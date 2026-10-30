package com.arady.phone.ui

import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.layer.*
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------------------------
// Frosted glass, ColorOS 17 style.
//
// How the blur works (no extra library): the content that sits BEHIND the glass is drawn through
// Modifier.glassSource(), which records it into a GraphicsLayer. A glass panel then draws a second copy
// of that layer, shifted so it lines up with the content under the panel, with a blur RenderEffect on it,
// and puts the tint, sheen and edge light on top. Blur needs Android 12 (API 31) or newer; below that
// the panel falls back to a more opaque tint so it still looks like glass.
// ---------------------------------------------------------------------------------------------

class GlassSource internal constructor(val layer: GraphicsLayer) {
    // Where the source sits on screen, so panels can line their copy of it up.
    var origin by mutableStateOf(Offset.Zero)
}

@Composable
fun rememberGlassSource(): GlassSource {
    val layer = rememberGraphicsLayer()
    return remember(layer) { GlassSource(layer) }
}

// Put this on the container that holds everything the glass panels should blur.
// Glass panels must be SIBLINGS drawn after this container, never inside it.
fun Modifier.glassSource(source: GlassSource): Modifier = this
    .onGloballyPositioned { source.origin = it.positionInRoot() }
    .drawWithContent {
        val w = size.width.roundToInt()
        val h = size.height.roundToInt()
        if (w > 0 && h > 0) {
            source.layer.record(this, layoutDirection, IntSize(w, h)) {
                this@drawWithContent.drawContent()
            }
            drawLayer(source.layer)
        } else {
            drawContent()
        }
    }

@Immutable
class GlassStyle(
    val blur: Dp,
    val tintTop: Color,
    val tintBottom: Color,
    val fallbackBase: Color,
    val sheen: Color,
    val borderTop: Color,
    val borderBottom: Color,
    // how much of fallbackBase to lay under the blur (translucent so underlying content is visible)
    val baseAlpha: Float = 0.45f,
    // soft coloured light inside the panel (creates physical refractive liquid glass depth)
    val glow: Float = 0f,
)

private fun Color.boost(extra: Float): Color = copy(alpha = (alpha + extra).coerceAtMost(1f))

fun GlassStyle.pressed(): GlassStyle = GlassStyle(
    blur = blur,
    tintTop = tintTop.boost(0.18f),
    tintBottom = tintBottom.boost(0.18f),
    fallbackBase = fallbackBase,
    sheen = sheen.boost(0.12f),
    borderTop = borderTop.boost(0.15f),
    borderBottom = borderBottom,
    baseAlpha = (baseAlpha + 0.10f).coerceAtMost(1f),
    glow = glow,
)

// A glass surface. Blurs what is behind it when source is passed; otherwise renders a luminous glass panel.
@Composable
fun Modifier.glass(
    shape: Shape,
    style: GlassStyle,
    source: GlassSource? = null,
): Modifier {
    val density = LocalDensity.current
    val blurPx = with(density) { style.blur.toPx() }
    val blurLayer = rememberGraphicsLayer()
    val canBlur = source != null && blurPx > 0f && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    var pos by remember { mutableStateOf(Offset.Zero) }

    val effect = remember(blurPx) { BlurEffect(blurPx, blurPx, TileMode.Clamp) }

    return this
        .onGloballyPositioned { pos = it.positionInRoot() }
        .graphicsLayer {
            this.shape = shape
            this.clip = true
        }
        .drawWithContent {
            val outline = shape.createOutline(size, layoutDirection, this)
            val path = Path().apply { addOutline(outline) }

            // Everything is clipped strictly inside the shape to prevent square corners leaking
            clipPath(path) {
                // 1. Blurred background content (draw first so it sits beneath the glass tints)
                if (source != null) {
                    val w = size.width.roundToInt()
                    val h = size.height.roundToInt()
                    if (canBlur && w > 0 && h > 0) {
                        blurLayer.renderEffect = effect
                        blurLayer.record(this, layoutDirection, IntSize(w, h)) {
                            translate(left = source.origin.x - pos.x, top = source.origin.y - pos.y) {
                                drawLayer(source.layer)
                            }
                        }
                        drawLayer(blurLayer)
                    }
                    // Translucent base filter over blurred layer
                    if (style.fallbackBase != Color.Transparent) {
                        val effectiveAlpha = if (canBlur) style.baseAlpha else (style.baseAlpha + 0.04f).coerceAtMost(0.96f)
                        drawRect(style.fallbackBase.copy(alpha = effectiveAlpha))
                    }
                } else if (style.fallbackBase != Color.Transparent) {
                    drawRect(style.fallbackBase.copy(alpha = style.baseAlpha))
                }

                // 2. Translucent Glass Tint
                drawRect(Brush.verticalGradient(listOf(style.tintTop, style.tintBottom)))

                // 3. Curvature Top Sheen (clean specular optical reflection right at the top curved lip)
                val sheenHeight = minOf(size.height * 0.16f, with(density) { 24.dp.toPx() })
                if (sheenHeight > 0f && style.sheen.alpha > 0f) {
                    drawRect(
                        Brush.verticalGradient(
                            colors = listOf(
                                style.sheen,
                                style.sheen.copy(alpha = style.sheen.alpha * 0.25f),
                                Color.Transparent,
                            ),
                            startY = 0f,
                            endY = sheenHeight,
                        ),
                    )
                }
            }

            // Draw composable children content
            drawContent()
        }
        .border(
            1.2.dp,
            Brush.linearGradient(
                colors = listOf(style.borderTop, style.borderTop.copy(alpha = style.borderTop.alpha * 0.5f), style.borderBottom),
                start = Offset(0f, 0f),
                end = Offset(0f, Float.POSITIVE_INFINITY),
            ),
            shape,
        )
}

// Glass button with physical contact shadow and press feedback (slight shrink + brighter tint).
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun GlassButton(
    shape: Shape,
    modifier: Modifier = Modifier,
    style: GlassStyle = PhoneGlass.key,
    source: GlassSource? = null,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = tween(90),
        label = "glassPress",
    )
    val shadowAmbient = if (PhoneColors.isDark) Color.Black.copy(alpha = 0.50f) else Color(0x18000000)
    val shadowSpot = if (PhoneColors.isDark) Color.Black.copy(alpha = 0.70f) else Color(0x24000000)

    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(
                elevation = if (isPressed) 1.dp else 3.5.dp,
                shape = shape,
                ambientColor = shadowAmbient,
                spotColor = shadowSpot,
            )
            .glass(shape, if (isPressed) style.pressed() else style, source)
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

// The glossy green call / action button.
@Composable
fun GlossButton(
    shape: Shape,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val interaction = remember { MutableInteractionSource() }
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.96f else 1f,
        animationSpec = tween(90),
        label = "glossPress",
    )
    val top = if (isPressed) Color(0xFF1BCB78) else Color(0xFF45F29C)
    val bottom = if (isPressed) Color(0xFF00A455) else Color(0xFF00C964)
    val glow = Color(0xFF00D26A)
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .shadow(10.dp, shape, ambientColor = glow, spotColor = glow)
            .clip(shape)
            .background(Brush.verticalGradient(listOf(top, bottom)))
            .drawBehind {
                drawRect(
                    Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.30f), Color.Transparent),
                        startY = 0f,
                        endY = size.height * 0.55f,
                    ),
                )
            }
            .border(
                1.dp,
                Brush.verticalGradient(listOf(Color.White.copy(alpha = 0.55f), Color.White.copy(alpha = 0.08f))),
                shape,
            )
            .clickable(interactionSource = interaction, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center,
        content = content,
    )
}

// Ambient background modifier: keeps clean neutral canvas for frosted glass
@Composable
fun Modifier.ambientGlow(): Modifier = this

// Overflow menu rendered as a pure frosted glass card.
// Place it LAST inside the screen's root Box so it blurs content beneath it.
@Composable
fun GlassMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    glass: GlassSource?,
    content: @Composable ColumnScope.() -> Unit,
) {
    if (expanded) BackHandler(onBack = onDismiss)
    AnimatedVisibility(
        visible = expanded,
        enter = fadeIn(tween(130)),
        exit = fadeOut(tween(100)),
    ) {
        val scrim = remember { MutableInteractionSource() }
        Box(
            Modifier
                .fillMaxSize()
                .clickable(interactionSource = scrim, indication = null, onClick = onDismiss),
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .statusBarsPadding()
                    .padding(top = 52.dp, end = 20.dp)
                    .widthIn(min = 210.dp, max = 270.dp)
                    .glass(RoundedCornerShape(22.dp), PhoneGlass.menu, glass)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .padding(6.dp),
                content = content,
            )
        }
    }
}

@Composable
fun GlassMenuItem(
    text: String,
    color: Color = PhoneColors.TextMain,
    onClick: () -> Unit,
) {
    PressBox(
        normal = Color.Transparent,
        pressed = if (PhoneColors.isDark) Color(0x22FFFFFF) else Color(0x12000000),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        contentAlignment = Alignment.CenterStart,
        onClick = onClick,
    ) {
        Text(
            text = text,
            color = color,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 13.dp),
        )
    }
}

// ---------------------------------------------------------------------------------------------
// The two glass palettes. Dark mode follows the ColorOS Liquid Glass: translucent slate/indigo
// with electric purple bottom-left caustic refraction and bright specular rim highlights.
// ---------------------------------------------------------------------------------------------

private fun white(a: Float) = Color.White.copy(alpha = a)
private fun black(a: Float) = Color.Black.copy(alpha = a)

private object DarkGlass {
    val sheet = GlassStyle(
        blur = 40.dp,
        tintTop = white(0.10f),
        tintBottom = white(0.03f),
        fallbackBase = Color(0xFF10131B),
        sheen = white(0.24f),
        borderTop = white(0.38f),
        borderBottom = white(0.08f),
        baseAlpha = 0.92f,
        glow = 0f,
    )
    val floatingPill = GlassStyle(
        blur = 36.dp,
        tintTop = white(0.15f),
        tintBottom = white(0.06f),
        fallbackBase = Color(0xFF131722),
        sheen = white(0.18f),
        borderTop = white(0.38f),
        borderBottom = white(0.08f),
        baseAlpha = 0.85f,
        glow = 0f,
    )
    val dockedPill = GlassStyle(
        blur = 0.dp,
        tintTop = white(0.16f),
        tintBottom = white(0.06f),
        fallbackBase = Color(0xFF1A1F2C),
        sheen = white(0.16f),
        borderTop = white(0.32f),
        borderBottom = white(0.06f),
        baseAlpha = 0.70f,
    )
    val key = GlassStyle(
        blur = 0.dp,
        tintTop = white(0.18f),
        tintBottom = white(0.06f),
        fallbackBase = Color(0xFF222938),
        sheen = white(0.20f),
        borderTop = white(0.42f),
        borderBottom = white(0.08f),
        baseAlpha = 0.75f,
    )
    val circle = GlassStyle(
        blur = 0.dp,
        tintTop = white(0.18f),
        tintBottom = white(0.08f),
        fallbackBase = Color(0xFF202634),
        sheen = white(0.18f),
        borderTop = white(0.35f),
        borderBottom = white(0.07f),
        baseAlpha = 0.70f,
    )
    val track = GlassStyle(
        blur = 0.dp,
        tintTop = white(0.12f),
        tintBottom = white(0.06f),
        fallbackBase = Color(0xFF151924),
        sheen = white(0.10f),
        borderTop = white(0.26f),
        borderBottom = white(0.06f),
        baseAlpha = 0.65f,
    )
    val selected = GlassStyle(
        blur = 0.dp,
        tintTop = white(0.28f),
        tintBottom = white(0.14f),
        fallbackBase = Color(0xFF2A3446),
        sheen = white(0.28f),
        borderTop = white(0.55f),
        borderBottom = white(0.12f),
        baseAlpha = 0.82f,
    )
    val card = GlassStyle(
        blur = 40.dp,
        tintTop = white(0.14f),
        tintBottom = white(0.06f),
        fallbackBase = Color(0xFF131722),
        sheen = white(0.18f),
        borderTop = white(0.36f),
        borderBottom = white(0.06f),
        baseAlpha = 0.88f,
        glow = 0f,
    )
    val menu = GlassStyle(
        blur = 40.dp,
        tintTop = white(0.14f),
        tintBottom = white(0.06f),
        fallbackBase = Color(0xFF141824),
        sheen = white(0.18f),
        borderTop = white(0.36f),
        borderBottom = white(0.07f),
        baseAlpha = 0.92f,
        glow = 0f,
    )
    val item = GlassStyle(
        blur = 0.dp,
        tintTop = white(0.14f),
        tintBottom = white(0.07f),
        fallbackBase = Color(0xFF1B202D),
        sheen = white(0.14f),
        borderTop = white(0.30f),
        borderBottom = white(0.06f),
        baseAlpha = 0.55f,
    )
}

private object LightGlass {
    val sheet = GlassStyle(
        blur = 40.dp,
        tintTop = white(0.85f),
        tintBottom = white(0.70f),
        fallbackBase = Color(0xFFF4F6FA),
        sheen = white(0.75f),
        borderTop = white(0.98f),
        borderBottom = black(0.06f),
        baseAlpha = 0.92f,
        glow = 0f,
    )
    val floatingPill = GlassStyle(
        blur = 36.dp,
        tintTop = white(0.85f),
        tintBottom = white(0.70f),
        fallbackBase = Color(0xFFF2F4F8),
        sheen = white(0.75f),
        borderTop = white(0.95f),
        borderBottom = black(0.06f),
        baseAlpha = 0.80f,
        glow = 0f,
    )
    val dockedPill = GlassStyle(
        blur = 0.dp,
        tintTop = white(0.70f),
        tintBottom = white(0.50f),
        fallbackBase = Color(0xFFE8ECF2),
        sheen = white(0.70f),
        borderTop = white(0.92f),
        borderBottom = black(0.05f),
        baseAlpha = 0.75f,
    )
    val key = GlassStyle(
        blur = 0.dp,
        tintTop = white(0.95f),
        tintBottom = white(0.80f),
        fallbackBase = Color(0xFFFFFFFF),
        sheen = white(0.85f),
        borderTop = white(0.98f),
        borderBottom = black(0.06f),
        baseAlpha = 0.88f,
    )
    val circle = GlassStyle(
        blur = 0.dp,
        tintTop = white(0.80f),
        tintBottom = white(0.60f),
        fallbackBase = Color(0xFFFFFFFF),
        sheen = white(0.70f),
        borderTop = white(0.95f),
        borderBottom = black(0.06f),
        baseAlpha = 0.80f,
    )
    val track = GlassStyle(
        blur = 0.dp,
        tintTop = black(0.07f),
        tintBottom = black(0.04f),
        fallbackBase = Color.Transparent,
        sheen = white(0.25f),
        borderTop = black(0.06f),
        borderBottom = white(0.50f),
        baseAlpha = 0f,
    )
    val selected = GlassStyle(
        blur = 0.dp,
        tintTop = white(1f),
        tintBottom = white(0.90f),
        fallbackBase = Color(0xFFFFFFFF),
        sheen = white(0.85f),
        borderTop = white(1f),
        borderBottom = black(0.08f),
        baseAlpha = 0.95f,
    )
    val card = GlassStyle(
        blur = 40.dp,
        tintTop = white(0.85f),
        tintBottom = white(0.72f),
        fallbackBase = Color(0xFFF7F8FC),
        sheen = white(0.70f),
        borderTop = white(0.96f),
        borderBottom = black(0.06f),
        baseAlpha = 0.90f,
        glow = 0f,
    )
    val menu = GlassStyle(
        blur = 40.dp,
        tintTop = white(0.88f),
        tintBottom = white(0.72f),
        fallbackBase = Color(0xFFF7F8FC),
        sheen = white(0.65f),
        borderTop = white(0.96f),
        borderBottom = black(0.06f),
        baseAlpha = 0.92f,
        glow = 0f,
    )
    val item = GlassStyle(
        blur = 0.dp,
        tintTop = white(0.65f),
        tintBottom = white(0.45f),
        fallbackBase = Color(0xFFFFFFFF),
        sheen = white(0.70f),
        borderTop = white(0.92f),
        borderBottom = black(0.05f),
        baseAlpha = 0.70f,
    )
}

object PhoneGlass {
    val sheet: GlassStyle @Composable get() = if (PhoneColors.isDark) DarkGlass.sheet else LightGlass.sheet
    val floatingPill: GlassStyle @Composable get() = if (PhoneColors.isDark) DarkGlass.floatingPill else LightGlass.floatingPill
    val dockedPill: GlassStyle @Composable get() = if (PhoneColors.isDark) DarkGlass.dockedPill else LightGlass.dockedPill
    val key: GlassStyle @Composable get() = if (PhoneColors.isDark) DarkGlass.key else LightGlass.key
    val circle: GlassStyle @Composable get() = if (PhoneColors.isDark) DarkGlass.circle else LightGlass.circle
    val track: GlassStyle @Composable get() = if (PhoneColors.isDark) DarkGlass.track else LightGlass.track
    val selected: GlassStyle @Composable get() = if (PhoneColors.isDark) DarkGlass.selected else LightGlass.selected
    val card: GlassStyle @Composable get() = if (PhoneColors.isDark) DarkGlass.card else LightGlass.card
    val menu: GlassStyle @Composable get() = if (PhoneColors.isDark) DarkGlass.menu else LightGlass.menu
    val item: GlassStyle @Composable get() = if (PhoneColors.isDark) DarkGlass.item else LightGlass.item
}
