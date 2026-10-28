package com.arady.phone.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.Dp

// The original app used inline SVG icons on a 24 x 24 grid. The same path data is drawn here directly,
// so no icon library is needed.
class IconPart(val pathData: String, val filled: Boolean)

object PhoneIcons {
    val ThreeDots = listOf(
        IconPart(
            "M12 8c1.1 0 2-.9 2-2s-.9-2-2-2-2 .9-2 2 .9 2 2 2zm0 2c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2zm0 6c-1.1 0-2 .9-2 2s.9 2 2 2 2-.9 2-2-.9-2-2-2z",
            true,
        ),
    )

    val Call = listOf(
        IconPart(
            "M6.62 10.79a15.149 15.149 0 0 0 6.59 6.59l2.2-2.2c.27-.27.67-.36 1.02-.24 1.12.37 2.33.57 3.57.57.55 0 1 .45 1 1V20c0 .55-.45 1-1 1-9.39 0-17-7.61-17-17 0-.55.45-1 1-1h3.5c.55 0 1 .45 1 1 0 1.25.2 2.45.57 3.57.11.35.03.74-.25 1.02l-2.2 2.2z",
            true,
        ),
    )

    val Backspace = listOf(
        IconPart("M21 4H8l-7 8 7 8h13a2 2 0 0 0 2-2V6a2 2 0 0 0-2-2z", false),
        IconPart("M18 9L12 15", false),
        IconPart("M12 9L18 15", false),
    )

    val Recents = listOf(
        IconPart("M22 12A10 10 0 1 1 2 12A10 10 0 1 1 22 12Z", false),
        IconPart("M12 6L12 12L16 14", false),
    )

    val Keypad = listOf(
        IconPart("M3 3H10V12H3Z", false),
        IconPart("M14 3H21V8H14Z", false),
        IconPart("M14 12H21V21H14Z", false),
        IconPart("M3 16H10V21H3Z", false),
    )

    val People = listOf(
        IconPart("M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2", false),
        IconPart("M13 7A4 4 0 1 1 5 7A4 4 0 1 1 13 7Z", false),
        IconPart("M23 21v-2a4 4 0 0 0-3-3.87", false),
        IconPart("M16 3.13a4 4 0 0 1 0 7.75", false),
    )

    val Back = listOf(
        IconPart("M19 12L5 12", false),
        IconPart("M12 19L5 12L12 5", false),
    )

    val Edit = listOf(
        IconPart("M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7", false),
        IconPart("M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z", false),
    )

    val Message = listOf(
        IconPart("M21 15a2 2 0 0 1-2 2H7l-4 4V5a2 2 0 0 1 2-2h14a2 2 0 0 1 2 2z", false),
    )

    val Star = listOf(
        IconPart("M12 2L15.09 8.26L22 9.27L17 14.14L18.18 21.02L12 17.77L5.82 21.02L7 14.14L2 9.27L8.91 8.26Z", false),
    )
}

// Draws stroke parts with a 2 unit line (like the SVGs) and filled parts solid.
// fillUnder paints stroke parts with a solid colour first (used for the "favorited" star).
@Composable
fun SvgIcon(
    parts: List<IconPart>,
    tint: Color,
    size: Dp,
    modifier: Modifier = Modifier,
    fillUnder: Color? = null,
) {
    val paths = remember(parts) { parts.map { PathParser().parsePathString(it.pathData).toPath() } }
    Canvas(modifier.size(size)) {
        val factor = this.size.width / 24f
        scale(factor, Offset.Zero) {
            parts.forEachIndexed { i, part ->
                val path = paths[i]
                if (part.filled) {
                    drawPath(path, tint, style = Fill)
                } else {
                    if (fillUnder != null) drawPath(path, fillUnder, style = Fill)
                    drawPath(path, tint, style = Stroke(width = 2f))
                }
            }
        }
    }
}
