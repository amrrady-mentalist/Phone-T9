package com.arady.phone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape

// A tappable box that swaps its background while pressed (the CSS ":active" rules of the web app).
// No ripple, to keep the flat look of the original.
@Composable
fun PressBox(
    normal: Color,
    pressed: Color,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    contentAlignment: Alignment = Alignment.Center,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (isPressed) pressed else normal)
            .clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = contentAlignment,
        content = content,
    )
}

// Plain tap without any visual feedback.
@Composable
fun TapBox(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = modifier.clickable(interactionSource = source, indication = null, onClick = onClick),
        contentAlignment = contentAlignment,
        content = content,
    )
}
