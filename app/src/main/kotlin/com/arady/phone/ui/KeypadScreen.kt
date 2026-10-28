package com.arady.phone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arady.phone.PhoneViewModel

private class KeyDef(val value: String, val letters: String)

private val KEY_ROWS = listOf(
    listOf(KeyDef("1", "oo"), KeyDef("2", "abc"), KeyDef("3", "def")),
    listOf(KeyDef("4", "ghi"), KeyDef("5", "jkl"), KeyDef("6", "mno")),
    listOf(KeyDef("7", "pqrs"), KeyDef("8", "tuv"), KeyDef("9", "wxyz")),
    listOf(KeyDef("*", ","), KeyDef("0", "+"), KeyDef("#", ";")),
)

@Composable
fun KeypadScreen(vm: PhoneViewModel) {
    Column(Modifier.fillMaxSize()) {
        // dialer header: suggestions + typed digits
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .heightIn(min = 120.dp)
                .padding(horizontal = 24.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            val suggestions = vm.suggestions()
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 110.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for ((contact, phone) in suggestions) {
                    PressBox(
                        normal = Color(0x0AFFFFFF),
                        pressed = Color(0x1AFFFFFF),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp),
                        contentAlignment = Alignment.CenterStart,
                        onClick = { vm.openContact(contact) },
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(34.dp)
                                    .background(PhoneColors.AvatarSmall, CircleShape),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    initialOf(contact.name),
                                    color = PhoneColors.TextMain,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Spacer(Modifier.size(12.dp))
                            Column {
                                Text(
                                    contact.name,
                                    color = PhoneColors.TextMain,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(phone, color = PhoneColors.TextMuted, fontSize = 12.sp)
                            }
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = vm.digits,
                color = if (vm.digitsFlash) PhoneColors.CallGreen else PhoneColors.TextMain,
                fontSize = 42.sp,
                fontWeight = FontWeight.Light,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 56.dp),
            )
        }

        // 3 x 4 key grid
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 44.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            for (row in KEY_ROWS) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    for (key in row) {
                        PressBox(
                            normal = PhoneColors.SurfaceKey,
                            pressed = PhoneColors.SurfaceKeyActive,
                            modifier = Modifier
                                .weight(1f)
                                .aspectRatio(1f),
                            onClick = { vm.typeKey(key.value) },
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    key.value,
                                    color = PhoneColors.TextMain,
                                    fontSize = 32.sp,
                                    lineHeight = 36.sp,
                                )
                                Text(
                                    key.letters.uppercase(),
                                    color = PhoneColors.TextMuted,
                                    fontSize = 10.sp,
                                    lineHeight = 11.sp,
                                    letterSpacing = 1.5.sp,
                                )
                            }
                        }
                    }
                }
            }
        }

        // call button + backspace
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 24.dp, bottom = 12.dp)
                .height(75.dp),
        ) {
            PressBox(
                normal = PhoneColors.CallGreen,
                pressed = PhoneColors.CallGreenActive,
                modifier = Modifier
                    .size(75.dp)
                    .align(Alignment.Center),
                onClick = { vm.pressCall() },
            ) {
                SvgIcon(PhoneIcons.Call, Color.White, 30.dp)
            }
            TapBox(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .padding(end = 56.dp)
                    .padding(12.dp),
                contentAlignment = Alignment.Center,
                onClick = { vm.backspace() },
            ) {
                SvgIcon(PhoneIcons.Backspace, PhoneColors.TextMuted, 28.dp)
            }
        }
    }
}

fun initialOf(name: String): String =
    if (name.isEmpty()) "?" else name.substring(0, 1).uppercase()
