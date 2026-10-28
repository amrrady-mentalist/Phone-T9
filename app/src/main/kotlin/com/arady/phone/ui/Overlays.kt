package com.arady.phone.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
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
            .background(Color(0xBF000000))
            .padding(24.dp),
        contentAlignment = Alignment.Center,
        onClick = { vm.closePeek() },
    ) {
        // taps on the card itself must not close the popup
        TapBox(
            modifier = Modifier
                .widthIn(max = 310.dp)
                .fillMaxWidth()
                .background(PhoneColors.PeekCard, RoundedCornerShape(14.dp))
                .border(1.dp, PhoneColors.PeekCardBorder, RoundedCornerShape(14.dp))
                .padding(18.dp),
            onClick = {},
        ) {
            Column {
                Text(
                    "SMART RESULTS ENGINE",
                    color = PhoneColors.TextMuted,
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 14.dp),
                )
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    if (vm.peekItems.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(PhoneColors.PeekItem, RoundedCornerShape(10.dp))
                                .padding(14.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text("No Match Found", color = PhoneColors.TextMuted, fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                    for (item in vm.peekItems) {
                        PressBox(
                            normal = PhoneColors.PeekItem,
                            pressed = Color(0xFF3A3A3C),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            onClick = { vm.pickPeekWord(item.word) },
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                            ) {
                                Text(item.word, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
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
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}

// "API Endpoint Setup" dialog behind the three-dot menu.
@Composable
fun SettingsOverlay(vm: PhoneViewModel) {
    if (!vm.settingsVisible) return
    BackHandler { vm.closeSettings() }

    var text by remember { mutableStateOf(vm.apiUrl) }

    TapBox(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xD9000000))
            .imePadding()
            .padding(20.dp),
        contentAlignment = Alignment.Center,
        onClick = {},
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 360.dp)
                .fillMaxWidth()
                .background(PhoneColors.SettingsCard, RoundedCornerShape(16.dp))
                .padding(24.dp),
        ) {
            Text("API Endpoint Setup", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(16.dp))
            Text("Target Public Routing URL", color = PhoneColors.TextMuted, fontSize = 13.sp)
            Spacer(Modifier.height(6.dp))
            BasicTextField(
                value = text,
                onValueChange = { text = it },
                singleLine = true,
                textStyle = TextStyle(color = Color.White, fontSize = 14.sp),
                cursorBrush = SolidColor(Color.White),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFF111111), RoundedCornerShape(8.dp))
                    .border(1.dp, Color(0xFF444444), RoundedCornerShape(8.dp))
                    .padding(10.dp),
                decorationBox = { inner ->
                    Box {
                        if (text.isEmpty()) {
                            Text("https://endpoint.com/api", color = PhoneColors.TextMuted, fontSize = 14.sp)
                        }
                        inner()
                    }
                },
            )
            Spacer(Modifier.height(20.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.End),
            ) {
                PressBox(
                    normal = Color(0xFF444444),
                    pressed = Color(0xFF555555),
                    shape = RoundedCornerShape(8.dp),
                    onClick = { vm.closeSettings() },
                ) {
                    Text(
                        "Cancel",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
                PressBox(
                    normal = PhoneColors.CallGreen,
                    pressed = PhoneColors.CallGreenActive,
                    shape = RoundedCornerShape(8.dp),
                    onClick = { vm.saveApiUrl(text) },
                ) {
                    Text(
                        "Save",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                    )
                }
            }
        }
    }
}
