package com.arady.phone.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

data class PhoneColorScheme(
    val isDark: Boolean,
    val bgMain: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val keypadBg: Color,
    val keyBg: Color,
    val keyBgPressed: Color,
    val keyText: Color,
    val keySubtext: Color,
    val callGreen: Color,
    val callGreenPressed: Color,
    val pillNavBg: Color,
    val pillNavBorder: Color,
    val pillNavActive: Color,
    val topToggleBg: Color,
    val topToggleSelected: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val divider: Color,
    val searchBg: Color,
    val orangeAlert: Color,
    val cardBg: Color,
    val cardBorder: Color,
    val actionCircle: Color,
    val actionCircleActive: Color,
    val handleColor: Color,
)

val LightPhoneColors = PhoneColorScheme(
    isDark = false,
    bgMain = Color(0xFFF5F6F8),
    surface = Color(0xFFFFFFFF),
    surfaceElevated = Color(0xFFFFFFFF),
    keypadBg = Color(0xFFF7F8FA),
    keyBg = Color(0xFFF1F3F6),
    keyBgPressed = Color(0xFFE2E5EB),
    keyText = Color(0xFF111318),
    keySubtext = Color(0xFF80848F),
    callGreen = Color(0xFF00D26A),
    callGreenPressed = Color(0xFF00B55B),
    pillNavBg = Color(0xFFFFFFFF),
    pillNavBorder = Color(0xFFE2E4E8),
    pillNavActive = Color(0xFFE9EDF5),
    topToggleBg = Color(0xFFEAECEF),
    topToggleSelected = Color(0xFFFFFFFF),
    textPrimary = Color(0xFF111318),
    textSecondary = Color(0xFF6B707B),
    textMuted = Color(0xFF8E939E),
    divider = Color(0xFFEAECEF),
    searchBg = Color(0xFFEAECEF),
    orangeAlert = Color(0xFFFF6D00),
    cardBg = Color(0xFFFFFFFF),
    cardBorder = Color(0xFFE2E4E8),
    actionCircle = Color(0xFFF0F2F5),
    actionCircleActive = Color(0xFFE0E3E8),
    handleColor = Color(0xFFD0D3DB),
)

val DarkPhoneColors = PhoneColorScheme(
    isDark = true,
    bgMain = Color(0xFF000000),
    surface = Color(0xFF121418),
    surfaceElevated = Color(0xFF16181F),
    keypadBg = Color(0xFF16181E),
    keyBg = Color(0xFF252832),
    keyBgPressed = Color(0xFF353946),
    keyText = Color(0xFFFFFFFF),
    keySubtext = Color(0xFF868C9C),
    callGreen = Color(0xFF00D26A),
    callGreenPressed = Color(0xFF00B55B),
    pillNavBg = Color(0xFF1C1F27),
    pillNavBorder = Color(0xFF2A2E3B),
    pillNavActive = Color(0xFF2C313E),
    topToggleBg = Color(0xFF1A1C23),
    topToggleSelected = Color(0xFF282D3A),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFF8A90A0),
    textMuted = Color(0xFF636875),
    divider = Color(0xFF1C1E26),
    searchBg = Color(0xFF1B1D25),
    orangeAlert = Color(0xFFFF7A00),
    cardBg = Color(0xFF1A1C23),
    cardBorder = Color(0xFF282C38),
    actionCircle = Color(0xFF252832),
    actionCircleActive = Color(0xFF383C4A),
    handleColor = Color(0xFF404452),
)

val LocalPhoneColors = staticCompositionLocalOf { DarkPhoneColors }

// Dynamic accessor object preserved for full backward compatibility
object PhoneColors {
    val current: PhoneColorScheme
        @Composable
        get() = LocalPhoneColors.current

    val isDark: Boolean @Composable get() = current.isDark
    val BgMain: Color @Composable get() = current.bgMain
    val Surface: Color @Composable get() = current.surface
    val SurfaceElevated: Color @Composable get() = current.surfaceElevated
    val SurfaceKey: Color @Composable get() = current.keyBg
    val SurfaceKeyActive: Color @Composable get() = current.keyBgPressed
    val TextMain: Color @Composable get() = current.textPrimary
    val TextMuted: Color @Composable get() = current.textMuted
    val TextSecondary: Color @Composable get() = current.textSecondary
    val CallGreen: Color @Composable get() = current.callGreen
    val CallGreenActive: Color @Composable get() = current.callGreenPressed
    val AndroidBlue: Color = Color(0xFF3498DB)
    val Yellow: Color = Color(0xFFF1C40F)
    val OrangeAlert: Color @Composable get() = current.orangeAlert
    val Divider: Color @Composable get() = current.divider
    val SearchBg: Color @Composable get() = current.searchBg
    val AvatarSmall: Color @Composable get() = if (current.isDark) Color(0xFF2B2B2D) else Color(0xFFE2E4E9)
    val AvatarLarge: Color @Composable get() = if (current.isDark) Color(0xFF34495E) else Color(0xFFD6DBE4)
    val PeekCard: Color @Composable get() = current.cardBg
    val PeekCardBorder: Color @Composable get() = current.cardBorder
    val cardBorder: Color @Composable get() = current.cardBorder
    val PeekItem: Color @Composable get() = current.actionCircle
    val DetailsBg: Color @Composable get() = current.bgMain
    val DetailsCard: Color @Composable get() = current.cardBg
    val DetailsCardBorder: Color @Composable get() = current.cardBorder
    val ActionCircle: Color @Composable get() = current.actionCircle
    val ActionCircleActive: Color @Composable get() = current.actionCircleActive
    val SettingsCard: Color @Composable get() = current.cardBg
    val NavBorder: Color @Composable get() = current.pillNavBorder
    val PillNavBg: Color @Composable get() = current.pillNavBg
    val PillNavBorder: Color @Composable get() = current.pillNavBorder
    val PillNavActive: Color @Composable get() = current.pillNavActive
    val TopToggleBg: Color @Composable get() = current.topToggleBg
    val TopToggleSelected: Color @Composable get() = current.topToggleSelected
    val HandleColor: Color @Composable get() = current.handleColor
}

val AvatarAccentColors = listOf(
    Color(0xFF2979FF),
    Color(0xFF00A3FF),
    Color(0xFF9C27B0),
    Color(0xFFFF7043),
    Color(0xFF26A69A),
    Color(0xFF5C6BC0),
    Color(0xFFE91E63),
    Color(0xFF78909C),
)

fun getAvatarColor(key: String): Color {
    val hash = key.fold(0) { acc, c -> (acc * 31 + c.code) and 0x7fffffff }
    return AvatarAccentColors[hash % AvatarAccentColors.size]
}

@Composable
fun PhoneTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val phoneColors = if (darkTheme) DarkPhoneColors else LightPhoneColors
    val materialColorScheme = if (darkTheme) {
        darkColorScheme(
            primary = phoneColors.callGreen,
            background = phoneColors.bgMain,
            surface = phoneColors.surface,
            onBackground = phoneColors.textPrimary,
            onSurface = phoneColors.textPrimary,
        )
    } else {
        lightColorScheme(
            primary = phoneColors.callGreen,
            background = phoneColors.bgMain,
            surface = phoneColors.surface,
            onBackground = phoneColors.textPrimary,
            onSurface = phoneColors.textPrimary,
        )
    }

    CompositionLocalProvider(LocalPhoneColors provides phoneColors) {
        MaterialTheme(
            colorScheme = materialColorScheme,
            content = content,
        )
    }
}
