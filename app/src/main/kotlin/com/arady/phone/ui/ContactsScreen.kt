package com.arady.phone.ui

import android.content.Intent
import android.provider.ContactsContract
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arady.phone.PhoneViewModel
import com.arady.phone.Tab
import com.arady.phone.data.Contact
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed interface ListRow {
    class Header(val letter: String) : ListRow
    class Item(val contact: Contact) : ListRow
}

private val LETTER_GROUP = Regex("[A-Za-z0-9\\u0410-\\u042F\\u0430-\\u044F\\u0600-\\u06FF]")

private fun buildRows(contacts: List<Contact>, query: String): List<ListRow> {
    val q = query.lowercase().trim()
    val rows = ArrayList<ListRow>()
    var currentLetter = ""
    for (c in contacts) {
        if (q.isNotEmpty() && !c.name.lowercase().contains(q) && c.phones.none { it.contains(q) }) continue
        var letter = if (c.name.isEmpty()) "#" else c.name.substring(0, 1).uppercase()
        if (!LETTER_GROUP.matches(letter)) letter = "#"
        if (letter != currentLetter) {
            currentLetter = letter
            rows.add(ListRow.Header(letter))
        }
        rows.add(ListRow.Item(c))
    }
    return rows
}

@Composable
fun ContactsScreen(vm: PhoneViewModel, requestPermission: () -> Unit) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val searchFocusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    val glass = rememberGlassSource()

    Box(Modifier.fillMaxSize()) {
        Box(
            Modifier
                .fillMaxSize()
                .background(PhoneColors.BgMain)
                .ambientGlow()
                .glassSource(glass)
        ) {
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // Contacts Top Bar matching screenshots
            if (!vm.isSearchingContacts) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .height(52.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column {
                        Text(
                            text = "Contacts",
                            color = PhoneColors.TextMain,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = "${vm.contacts.size} contacts",
                            color = PhoneColors.TextSecondary,
                            fontSize = 12.sp,
                        )
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        GlassButton(
                            shape = CircleShape,
                            style = PhoneGlass.circle,
                            modifier = Modifier.size(38.dp),
                            onClick = {
                                vm.isSearchingContacts = true
                                scope.launch {
                                    delay(120)
                                    searchFocusRequester.requestFocus()
                                    keyboardController?.show()
                                }
                            },
                        ) {
                            SvgIcon(PhoneIcons.Search, PhoneColors.TextMain, 20.dp)
                        }

                        GlassButton(
                            shape = CircleShape,
                            style = PhoneGlass.circle,
                            modifier = Modifier.size(38.dp),
                            onClick = { menuExpanded = true },
                        ) {
                            SvgIcon(PhoneIcons.ThreeDots, PhoneColors.TextMain, 20.dp)
                        }
                    }
                }
            } else {
                // Search Mode Bar (Screenshots 8 & 9)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(
                        modifier = Modifier
                            .weight(1f)
                            .glass(CircleShape, PhoneGlass.track)
                            .padding(horizontal = 14.dp, vertical = 9.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        // Clicking magnifying glass dismisses the keyboard automatically
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clickable {
                                    keyboardController?.hide()
                                    focusManager.clearFocus()
                                },
                            contentAlignment = Alignment.Center,
                        ) {
                            SvgIcon(PhoneIcons.Search, PhoneColors.TextSecondary, 18.dp)
                        }
                        Spacer(Modifier.width(8.dp))
                        BasicTextField(
                            value = vm.contactsSearch,
                            onValueChange = { vm.contactsSearch = it },
                            singleLine = true,
                            textStyle = TextStyle(color = PhoneColors.TextMain, fontSize = 15.sp),
                            cursorBrush = SolidColor(PhoneColors.CallGreen),
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(onSearch = {
                                keyboardController?.hide()
                                focusManager.clearFocus()
                            }),
                            modifier = Modifier
                                .weight(1f)
                                .focusRequester(searchFocusRequester),
                            decorationBox = { inner ->
                                Box {
                                    if (vm.contactsSearch.isEmpty()) {
                                        Text("Search contacts", color = PhoneColors.TextSecondary, fontSize = 15.sp)
                                    }
                                    inner()
                                }
                            },
                        )
                        if (vm.contactsSearch.isNotEmpty()) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .clickable { vm.contactsSearch = "" },
                                contentAlignment = Alignment.Center,
                            ) {
                                SvgIcon(PhoneIcons.Close, PhoneColors.TextSecondary, 16.dp)
                            }
                        }
                    }

                    Spacer(Modifier.width(12.dp))

                    Text(
                        text = "Cancel",
                        color = PhoneColors.CallGreen,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable {
                                vm.isSearchingContacts = false
                                vm.contactsSearch = ""
                                focusManager.clearFocus()
                                keyboardController?.hide()
                            }
                            .padding(horizontal = 4.dp, vertical = 6.dp),
                    )
                }
            }

            // Contacts List / Permission Request
            if (!vm.contactsPermissionGranted) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 48.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        "Allow access to your contacts to see and search them.",
                        color = PhoneColors.TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(18.dp))
                    GlossButton(
                        shape = CircleShape,
                        onClick = requestPermission,
                    ) {
                        Text(
                            "Allow Contacts Access",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        )
                    }
                }
            } else {
                val rows = remember(vm.contacts, vm.contactsSearch) { buildRows(vm.contacts, vm.contactsSearch) }
                if (rows.isEmpty()) {
                    Text(
                        if (vm.contacts.isEmpty()) "No contacts found." else "No results.",
                        color = PhoneColors.TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(48.dp),
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                    ) {
                        items(rows) { row ->
                            when (row) {
                                is ListRow.Header -> {
                                    Text(
                                        text = row.letter,
                                        color = PhoneColors.CallGreen,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(start = 4.dp, top = 16.dp, bottom = 6.dp),
                                    )
                                }
                                is ListRow.Item -> {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(12.dp))
                                            .clickable { vm.openContact(row.contact) }
                                            .padding(vertical = 9.dp, horizontal = 4.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                    ) {
                                        val avatarColor = getAvatarColor(row.contact.name)
                                        Box(
                                            modifier = Modifier
                                                .size(42.dp)
                                                .clip(CircleShape)
                                                .background(avatarColor),
                                            contentAlignment = Alignment.Center,
                                        ) {
                                            Text(
                                                text = initialOf(row.contact.name),
                                                color = Color.White,
                                                fontSize = 16.sp,
                                                fontWeight = FontWeight.SemiBold,
                                            )
                                        }

                                        Spacer(Modifier.width(14.dp))

                                        Text(
                                            text = row.contact.name,
                                            color = PhoneColors.TextMain,
                                            fontSize = 16.sp,
                                            fontWeight = FontWeight.Medium,
                                            modifier = Modifier.weight(1f),
                                        )
                                    }
                                }
                            }
                        }

                        item {
                            Spacer(Modifier.height(100.dp))
                        }
                    }
                }
            }
        }

        }

        // Bottom Coordinated Floating Row (Side-by-side: Pill Nav + Green + FAB)
        // Guaranteed never to overlap on any screen width!
        Row(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp, start = 16.dp, end = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            FloatingPillNav(
                currentTab = vm.tab,
                onCallsClick = {
                    vm.tab = Tab.KEYPAD
                },
                onContactsClick = {
                    vm.tab = Tab.CONTACTS
                },
                glass = glass,
            )

            // Floating Action Button (+) on the right
            GlossButton(
                shape = CircleShape,
                modifier = Modifier.size(56.dp),
                onClick = {
                    try {
                        val intent = Intent(Intent.ACTION_INSERT, ContactsContract.Contacts.CONTENT_URI)
                        context.startActivity(intent)
                    } catch (_: Exception) {
                    }
                },
            ) {
                SvgIcon(PhoneIcons.Plus, Color.White, 24.dp)
            }
        }

        GlassMenu(expanded = menuExpanded, onDismiss = { menuExpanded = false }, glass = glass) {
            GlassMenuItem("Settings") {
                menuExpanded = false
                vm.openSettings()
            }
            val currentLabel = when (vm.themeMode) {
                "light" -> "Dark Mode"
                "dark" -> "System Mode"
                else -> "Light Mode"
            }
            GlassMenuItem("Switch to $currentLabel") {
                menuExpanded = false
                val nextMode = when (vm.themeMode) {
                    "light" -> "dark"
                    "dark" -> "system"
                    else -> "light"
                }
                vm.applyThemeMode(nextMode)
            }
            GlassMenuItem("Refresh Contacts", color = PhoneColors.TextSecondary) {
                menuExpanded = false
                vm.reloadContacts()
            }
        }
    }
}
