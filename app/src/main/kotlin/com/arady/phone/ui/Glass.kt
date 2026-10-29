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
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
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
    // how much of fallbackBase to lay under the blur (heavier = text behind is harder to see)
    val baseAlpha: Float = 1f,
)

private fun Color.boost(extra: Float): Color = copy(alpha = (alpha + extra).coerceAtMost(1f))

fun GlassStyle.pressed(): GlassStyle = GlassStyle(
    blur = blur,
    tintTop = tintTop.boost(0.14f),
    tintBottom = tintBottom.boost(0.14f),
    fallbackBase = fallbackBase,
    sheen = sheen.boost(0.08f),
    borderTop = borderTop,
    borderBottom = borderBottom,
    baseAlpha = baseAlpha,
)

// A glass surface. Pass a GlassSource to blur what is behind it; pass null for a plain tinted glass
// surface (used for small things that sit on top of another glass panel).
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

    if (canBlur) {
        val effect = remember(blurPx) { BlurEffect(blurPx, blurPx, TileMode.Clamp) }
        SideEffect { blurLayer.renderEffect = effect }
    }

    return this
        .onGloballyPositioned { pos = it.positionInRoot() }
        .clip(shape)
        .drawBehind {
            if (source != null) {
                val w = size.width.roundToInt()
                val h = size.height.roundToInt()
                // Base layer: hides the sharp content behind the panel even if the blur is unavailable.
                drawRect(style.fallbackBase.copy(alpha = if (canBlur) style.baseAlpha else 1f))
                if (canBlur && w > 0 && h > 0) {
                    blurLayer.record(this, layoutDirection, IntSize(w, h)) {
                        translate(left = source.origin.x - pos.x, top = source.origin.y - pos.y) {
                            drawLayer(source.layer)
                        }
                    }
                    drawLayer(blurLayer)
                }
            }
            drawRect(Brush.verticalGradient(listOf(style.tintTop, style.tintBottom)))
            drawRect(
                Brush.verticalGradient(
                    colors = listOf(style.sheen, Color.Transparent),
                    startY = 0f,
                    endY = size.height * 0.5f,
                ),
            )
        }
        .border(
            1.dp,
            Brush.linearGradient(listOf(style.borderTop, style.borderBottom)),
            shape,
        )
}

// Glass button with press feedback (slight shrink + brighter tint).
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
    Box(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
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

// Soft colour glows behind everything so the glass has something to refract (dark: indigo/violet).
@Composable
fun Modifier.ambientGlow(): Modifier {
    val dark = PhoneColors.isDark
    return this.drawBehind {
        val w = size.width
        val h = size.height
        val first = if (dark) Color(0xFF3B4CCA).copy(alpha = 0.22f) else Color(0xFFB9C3FF).copy(alpha = 0.40f)
        val second = if (dark) Color(0xFF7A3FD0).copy(alpha = 0.15f) else Color(0xFFE6C9FF).copy(alpha = 0.32f)
        drawRect(
            Brush.radialGradient(
                colors = listOf(first, Color.Transparent),
                center = Offset(w * 0.15f, h * 0.10f),
                radius = w * 0.95f,
            ),
        )
        drawRect(
            Brush.radialGradient(
                colors = listOf(second, Color.Transparent),
                center = Offset(w * 0.95f, h * 0.82f),
                radius = w * 0.95f,
            ),
        )
    }
}

// Overflow menu rendered as a glass card (replaces the system DropdownMenu, which is a separate window
// and cannot blur the screen behind it). Place it LAST inside the screen's root Box so it sits on top.
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
                    .glass(RoundedCornerShape(26.dp), PhoneGlass.card, glass)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
                    .padding(8.dp),
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
        pressed = if (PhoneColors.isDark) Color(0x33A9B6FF) else Color(0x14000000),
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
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
// The two glass palettes. Dark mode follows the ColorOS 17 screenshots: periwinkle / indigo glass
// with a bright upper-left edge light. Tweak the numbers here to taste.
// ---------------------------------------------------------------------------------------------

private fun white(a: Float) = Color.White.copy(alpha = a)
private fun black(a: Float) = Color.Black.copy(alpha = a)

private object DarkGlass {
    val sheet = GlassStyle(
        blur = 64.dp,
        tintTop = Color(0xFF3D4A8F).copy(alpha = 0.42f),
        tintBottom = Color(0xFF141A45).copy(alpha = 0.60f),
        fallbackBase = Color(0xFF0F1230),
        sheen = white(0.10f),
        borderTop = white(0.32f),
        borderBottom = white(0.05f),
    )
    val floatingPill = GlassStyle(
        blur = 52.dp,
        tintTop = Color(0xFF4A58A8).copy(alpha = 0.50f),
        tintBottom = Color(0xFF1B2255).copy(alpha = 0.62f),
        fallbackBase = Color(0xFF161A40),
        sheen = white(0.12f),
        borderTop = white(0.38f),
        borderBottom = white(0.06f),
    )
    val dockedPill = GlassStyle(
        blur = 0.dp,
        tintTop = Color(0xFF8C9BFF).copy(alpha = 0.20f),
        tintBottom = Color(0xFF4A5AC0).copy(alpha = 0.22f),
        fallbackBase = Color.Transparent,
        sheen = white(0.12f),
        borderTop = white(0.30f),
        borderBottom = white(0.05f),
    )
    val key = GlassStyle(
        blur = 0.dp,
        tintTop = Color(0xFF8C9BFF).copy(alpha = 0.26f),
        tintBottom = Color(0xFF4455BB).copy(alpha = 0.28f),
        fallbackBase = Color.Transparent,
        sheen = white(0.16f),
        borderTop = white(0.34f),
        borderBottom = white(0.06f),
    )
    val circle = GlassStyle(
        blur = 0.dp,
        tintTop = Color(0xFF7080E0).copy(alpha = 0.30f),
        tintBottom = Color(0xFF3A48A0).copy(alpha = 0.34f),
        fallbackBase = Color.Transparent,
        sheen = white(0.12f),
        borderTop = white(0.30f),
        borderBottom = white(0.05f),
    )
    val track = GlassStyle(
        blur = 0.dp,
        tintTop = white(0.10f),
        tintBottom = white(0.06f),
        fallbackBase = Color.Transparent,
        sheen = white(0.05f),
        borderTop = white(0.20f),
        borderBottom = white(0.04f),
    )
    val selected = GlassStyle(
        blur = 0.dp,
        tintTop = Color(0xFFA9B6FF).copy(alpha = 0.55f),
        tintBottom = Color(0xFF6272E0).copy(alpha = 0.55f),
        fallbackBase = Color.Transparent,
        sheen = white(0.20f),
        borderTop = white(0.45f),
        borderBottom = white(0.10f),
    )
    val card = GlassStyle(
        blur = 60.dp,
        tintTop = Color(0xFF3A4590).copy(alpha = 0.55f),
        tintBottom = Color(0xFF161B48).copy(alpha = 0.70f),
        fallbackBase = Color(0xFF12163A),
        sheen = white(0.08f),
        borderTop = white(0.30f),
        borderBottom = white(0.05f),
    )
    val item = GlassStyle(
        blur = 0.dp,
        tintTop = white(0.12f),
        tintBottom = white(0.06f),
        fallbackBase = Color.Transparent,
        sheen = white(0.08f),
        borderTop = white(0.22f),
        borderBottom = white(0.04f),
    )
}

private object LightGlass {
    val sheet = GlassStyle(
        blur = 64.dp,
        tintTop = white(0.68f),
        tintBottom = Color(0xFFF1EFFA).copy(alpha = 0.60f),
        fallbackBase = Color(0xFFF4F3FA),
        sheen = white(0.60f),
        borderTop = white(0.95f),
        borderBottom = black(0.06f),
    )
    val floatingPill = GlassStyle(
        blur = 52.dp,
        tintTop = white(0.62f),
        tintBottom = white(0.50f),
        fallbackBase = Color(0xFFF4F3FA),
        sheen = white(0.60f),
        borderTop = white(0.90f),
        borderBottom = black(0.06f),
    )
    val dockedPill = GlassStyle(
        blur = 0.dp,
        tintTop = white(0.55f),
        tintBottom = white(0.35f),
        fallbackBase = Color.Transparent,
        sheen = white(0.60f),
        borderTop = white(0.90f),
        borderBottom = black(0.05f),
    )
    val key = GlassStyle(
        blur = 0.dp,
        tintTop = white(0.78f),
        tintBottom = white(0.46f),
        fallbackBase = Color.Transparent,
        sheen = white(0.80f),
        borderTop = white(0.95f),
        borderBottom = black(0.05f),
    )
    val circle = GlassStyle(
        blur = 0.dp,
        tintTop = white(0.62f),
        tintBottom = white(0.40f),
        fallbackBase = Color.Transparent,
        sheen = white(0.60f),
        borderTop = white(0.90f),
        borderBottom = black(0.05f),
    )
    val track = GlassStyle(
        blur = 0.dp,
        tintTop = black(0.06f),
        tintBottom = black(0.04f),
        fallbackBase = Color.Transparent,
        sheen = white(0.20f),
        borderTop = black(0.05f),
        borderBottom = white(0.40f),
    )
    val selected = GlassStyle(
        blur = 0.dp,
        tintTop = white(0.98f),
        tintBottom = white(0.86f),
        fallbackBase = Color.Transparent,
        sheen = white(0.70f),
        borderTop = white(1f),
        borderBottom = black(0.06f),
    )
    val card = GlassStyle(
        blur = 60.dp,
        tintTop = white(0.80f),
        tintBottom = white(0.68f),
        fallbackBase = Color(0xFFFAFAFD),
        sheen = white(0.60f),
        borderTop = white(0.95f),
        borderBottom = black(0.06f),
    )
    val item = GlassStyle(
        blur = 0.dp,
        tintTop = white(0.55f),
        tintBottom = white(0.36f),
        fallbackBase = Color.Transparent,
        sheen = white(0.60f),
        borderTop = white(0.90f),
        borderBottom = black(0.05f),
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
    val item: GlassStyle @Composable get() = if (PhoneColors.isDark) DarkGlass.item else LightGlass.item
}
