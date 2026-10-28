package com.arady.phone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arady.phone.PhoneViewModel
import com.arady.phone.Tab

@Composable
fun PhoneApp(vm: PhoneViewModel, requestContactsPermission: () -> Unit) {
    Box(
        Modifier
            .fillMaxSize()
            .background(PhoneColors.BgMain),
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .padding(bottom = 8.dp),
        ) {
            // top action bar with the three dots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
                    .padding(top = 0.dp, start = 20.dp, end = 20.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                TapBox(
                    modifier = Modifier.padding(8.dp),
                    contentAlignment = Alignment.Center,
                    onClick = { vm.openSettings() },
                ) {
                    SvgIcon(PhoneIcons.ThreeDots, Color.White, 24.dp)
                }
            }

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth(),
            ) {
                when (vm.tab) {
                    Tab.KEYPAD -> KeypadScreen(vm)
                    Tab.CONTACTS -> ContactsScreen(vm, requestContactsPermission)
                }
            }

            // bottom navigation: Recents (secret trigger), Keypad, Contacts
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(PhoneColors.NavBorder),
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.Black)
                    .padding(top = 8.dp, bottom = 4.dp),
            ) {
                NavItem(Modifier.weight(1f), PhoneIcons.Recents, "Recents", active = false) { vm.openPeek() }
                NavItem(Modifier.weight(1f), PhoneIcons.Keypad, "Keypad", active = vm.tab == Tab.KEYPAD) {
                    vm.tab = Tab.KEYPAD
                }
                NavItem(Modifier.weight(1f), PhoneIcons.People, "Contacts", active = vm.tab == Tab.CONTACTS) {
                    vm.tab = Tab.CONTACTS
                }
            }
        }

        DetailsOverlay(vm)
        PeekOverlay(vm)
        SettingsOverlay(vm)
    }
}

@Composable
private fun NavItem(
    modifier: Modifier,
    icon: List<IconPart>,
    label: String,
    active: Boolean,
    onClick: () -> Unit,
) {
    val tint = if (active) Color.White else PhoneColors.TextMuted
    TapBox(modifier = modifier, contentAlignment = Alignment.Center, onClick = onClick) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            SvgIcon(icon, tint, 22.dp)
            Text(label, color = tint, fontSize = 11.sp)
        }
    }
}
