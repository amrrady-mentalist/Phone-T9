package com.arady.phone.ui

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import com.arady.phone.PhoneViewModel
import com.arady.phone.Tab

@Composable
fun PhoneApp(vm: PhoneViewModel, requestContactsPermission: () -> Unit) {
    val systemInDark = isSystemInDarkTheme()
    val isDark = when (vm.themeMode) {
        "light" -> false
        "dark" -> true
        else -> systemInDark
    }

    val context = LocalContext.current
    DisposableEffect(isDark) {
        val activity = context as? ComponentActivity
        activity?.enableEdgeToEdge(
            statusBarStyle = if (isDark) {
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
            },
            navigationBarStyle = if (isDark) {
                SystemBarStyle.dark(android.graphics.Color.TRANSPARENT)
            } else {
                SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
            },
        )
        onDispose {}
    }

    PhoneTheme(darkTheme = isDark) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(PhoneColors.BgMain)
                .systemBarsPadding(),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .fillMaxWidth(),
            ) {
                when (vm.tab) {
                    Tab.KEYPAD -> KeypadScreen(vm)
                    Tab.CONTACTS -> ContactsScreen(vm, requestContactsPermission)
                }
            }

            DetailsOverlay(vm)
            PeekOverlay(vm)
            SettingsOverlay(vm)
        }
    }
}
