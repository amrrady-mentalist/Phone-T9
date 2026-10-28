package com.arady.phone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arady.phone.PhoneViewModel
import com.arady.phone.t9.MatchType

// "Smart Results Engine": the secret popup behind the Recents tab.
@Composable
fun PeekOverlay(vm: PhoneViewModel) {
    if (!vm.peekVisible) return
    BackHandler { vm.closePeek() }

    TapBox(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .padding(24.dp),
        contentAlignment = Alignment.Center,
        onClick = { vm.closePeek() },
    ) {
        // taps on the card itself must not close the popup
        TapBox(
            modifier = Modifier
                .widthIn(max = 330.dp)
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(PhoneColors.PeekCard)
                .border(1.dp, PhoneColors.PeekCardBorder, RoundedCornerShape(24.dp))
                .padding(20.dp),
            onClick = {},
        ) {
            Column {
                Text(
                    "SMART RESULTS ENGINE",
                    color = PhoneColors.TextSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.2.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 280.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (vm.peekItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(14.dp))
                                .background(PhoneColors.PeekItem)
                                .padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                "No Match Found",
                                color = PhoneColors.TextSecondary,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                    }
                    for (item in vm.peekItems) {
                        PressBox(
                            normal = PhoneColors.PeekItem,
                            pressed = PhoneColors.ActionCircleActive,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(14.dp),
                            onClick = { vm.pickPeekWord(item.word) },
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(
                                    item.word,
                                    color = PhoneColors.TextMain,
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                when (item.type) {
                                    MatchType.SUGGEST -> Tag("NEXT", PhoneColors.CallGreen)
                                    MatchType.CORRECTION -> Tag("FIX", PhoneColors.Yellow)
                                    MatchType.EXACT -> {}
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Tag(label: String, color: Color) {
    Text(
        label,
        color = color,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color.copy(alpha = 0.18f), RoundedCornerShape(6.dp))
            .padding(horizontal = 7.dp, vertical = 3.dp),
    )
}

// "Settings" dialog behind the three-dot menu with Light / Dark theme selector and API URL setup.
@Composable
fun SettingsOverlay(vm: PhoneViewModel) {
    if (!vm.settingsVisible) return
    BackHandler { vm.closeSettings() }

    var text by remember { mutableStateOf(vm.apiUrl) }

    TapBox(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0x99000000))
            .imePadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center,
        onClick = { vm.closeSettings() },
    ) {
        TapBox(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .shadow(16.dp, RoundedCornerShape(24.dp))
                .clip(RoundedCornerShape(24.dp))
                .background(PhoneColors.SettingsCard)
                .border(1.dp, PhoneColors.cardBorder, RoundedCornerShape(24.dp))
                .padding(22.dp),
            onClick = {},
        ) {
            Column {
                Text(
                    "Settings",
                    color = PhoneColors.TextMain,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                )

                Spacer(Modifier.height(18.dp))

                // Theme Mode Selector [ System | Light | Dark ]
                Text(
                    "Appearance",
                    color = PhoneColors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(CircleShape)
                        .background(PhoneColors.SearchBg)
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    val modes = listOf("system" to "System", "light" to "Light", "dark" to "Dark")
                    for ((modeKey, modeTitle) in modes) {
                        val isSelected = vm.themeMode == modeKey
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(CircleShape)
                                .background(if (isSelected) PhoneColors.SurfaceElevated else Color.Transparent)
                                .clickable { vm.applyThemeMode(modeKey) }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = modeTitle,
                                color = if (isSelected) PhoneColors.TextMain else PhoneColors.TextSecondary,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                // Target Public Routing URL
                Text(
                    "Target Public Routing URL",
                    color = PhoneColors.TextSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Spacer(Modifier.height(8.dp))
                BasicTextField(
                    value = text,
                    onValueChange = { text = it },
                    singleLine = true,
                    textStyle = TextStyle(color = PhoneColors.TextMain, fontSize = 14.sp),
                    cursorBrush = SolidColor(PhoneColors.CallGreen),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(PhoneColors.SearchBg)
                        .border(1.dp, PhoneColors.cardBorder, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    decorationBox = { inner ->
                        Box {
                            if (text.isEmpty()) {
                                Text("https://endpoint.com/api", color = PhoneColors.TextSecondary, fontSize = 14.sp)
                            }
                            inner()
                        }
                    },
                )

                Spacer(Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.End),
                ) {
                    PressBox(
                        normal = PhoneColors.ActionCircle,
                        pressed = PhoneColors.ActionCircleActive,
                        shape = CircleShape,
                        onClick = { vm.closeSettings() },
                    ) {
                        Text(
                            "Cancel",
                            color = PhoneColors.TextMain,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                        )
                    }
                    PressBox(
                        normal = PhoneColors.CallGreen,
                        pressed = PhoneColors.CallGreenActive,
                        shape = CircleShape,
                        onClick = { vm.saveApiUrl(text) },
                    ) {
                        Text(
                            "Save",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 20.dp, vertical = 9.dp),
                        )
                    }
                }
            }
        }
    }
}
