package com.arady.phone.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PressBox(
    normal: Color,
    pressed: Color,
    modifier: Modifier = Modifier,
    shape: Shape = CircleShape,
    contentAlignment: Alignment = Alignment.Center,
    onLongClick: (() -> Unit)? = null,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    val isPressed by source.collectIsPressedAsState()
    Box(
        modifier = modifier
            .clip(shape)
            .background(if (isPressed) pressed else normal)
            .combinedClickable(
                interactionSource = source,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
        contentAlignment = contentAlignment,
        content = content,
    )
}

@Composable
fun TapBox(
    modifier: Modifier = Modifier,
    contentAlignment: Alignment = Alignment.TopStart,
    onClick: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    val source = remember { MutableInteractionSource() }
    Box(
        modifier = modifier.clickable(
            interactionSource = source,
            indication = null,
            onClick = onClick,
        ),
        contentAlignment = contentAlignment,
        content = content,
    )
}
