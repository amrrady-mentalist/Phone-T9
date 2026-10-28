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

    val DialpadGrid = listOf(
        IconPart("M5 5h2v2H5zm6 0h2v2h-2zm6 0h2v2h-2zM5 11h2v2H5zm6 0h2v2h-2zm6 0h2v2h-2zM5 17h2v2H5zm6 0h2v2h-2zm6 0h2v2h-2z", true),
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

    val Search = listOf(
        IconPart("M10.5 18a7.5 7.5 0 1 1 0-15 7.5 7.5 0 0 1 0 15z", false),
        IconPart("M16 16l5 5", false),
    )

    val Info = listOf(
        IconPart("M12 22C6.477 22 2 17.523 2 12S6.477 2 12 2s10 4.477 10 10-4.477 10-10 10z", false),
        IconPart("M12 8h.01", false),
        IconPart("M12 11v6", false),
    )

    val Plus = listOf(
        IconPart("M12 5v14M5 12h14", false),
    )

    val CallIncoming = listOf(
        IconPart("M6 18L18 6M6 6v12h12", false),
    )

    val CallOutgoing = listOf(
        IconPart("M6 18L18 6M18 18V6H6", false),
    )

    val CallMissed = listOf(
        IconPart("M6 18L18 6M6 6v12h12", false),
    )

    val Close = listOf(
        IconPart("M18 6L6 18M6 6l12 12", false),
    )

    val Share = listOf(
        IconPart("M4 12v8a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2v-8", false),
        IconPart("M16 6l-4-4-4 4", false),
        IconPart("M12 2v13", false),
    )

    val Video = listOf(
        IconPart("M15 10l5-3v10l-5-3", false),
        IconPart("M3 6h12v12H3z", false),
    )
}

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
                    drawPath(path, tint, style = Stroke(width = 2.2f))
                }
            }
        }
    }
}
