package com.arady.phone.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arady.phone.CallFilter
import com.arady.phone.CallType
import com.arady.phone.PhoneViewModel
import com.arady.phone.RecentCall
import com.arady.phone.Tab
import com.arady.phone.data.Contact
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private class KeyDef(val value: String, val letters: String)

private val KEY_ROWS = listOf(
    listOf(KeyDef("1", "oo"), KeyDef("2", "abc"), KeyDef("3", "def")),
    listOf(KeyDef("4", "ghi"), KeyDef("5", "jkl"), KeyDef("6", "mno")),
    listOf(KeyDef("7", "pqrs"), KeyDef("8", "tuv"), KeyDef("9", "wxyz")),
    listOf(KeyDef("*", ","), KeyDef("0", "+"), KeyDef("#", ";")),
)

@Composable
fun KeypadScreen(
    vm: PhoneViewModel,
    requestCallLogPermission: () -> Unit,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current
    val searchFocusRequester = remember { FocusRequester() }
    val scope = rememberCoroutineScope()

    val glass = rememberGlassSource()

    Box(Modifier.fillMaxSize()) {
        // Everything inside this Box is what the glass panels blur.
        Box(
            Modifier
                .fillMaxSize()
                .background(PhoneColors.BgMain)
                .ambientGlow()
                .glassSource(glass)
        ) {
        // Main Calls Screen: Header + Calls List
        Column(Modifier.fillMaxSize().statusBarsPadding()) {
            // Top Bar matching screenshots
            CallsTopBar(
                vm = vm,
                searchFocusRequester = searchFocusRequester,
                onSearchClick = {
                    if (!vm.isSearchingCalls) {
                        vm.isSearchingCalls = true
                        vm.keypadVisible = false // Dismiss dialpad when searching
                        scope.launch {
                            delay(120)
                            searchFocusRequester.requestFocus()
                            keyboardController?.show()
                        }
                    } else {
                        // Dismiss search and keyboard automatically
                        vm.isSearchingCalls = false
                        vm.callsSearch = ""
                        focusManager.clearFocus()
                        keyboardController?.hide()
                    }
                },
                onCancelSearch = {
                    vm.isSearchingCalls = false
                    vm.callsSearch = ""
                    focusManager.clearFocus()
                    keyboardController?.hide()
                },
                onMenuClick = { menuExpanded = true },
            )

            // If call log permission is not yet granted, show prompt
            if (!vm.callLogPermissionGranted) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 36.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Allow access to call logs to view your actual call history and missed calls.",
                        color = PhoneColors.TextSecondary,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(16.dp))
                    GlossButton(
                        shape = CircleShape,
                        onClick = requestCallLogPermission,
                    ) {
                        Text(
                            text = "Allow Call Log Access",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 22.dp, vertical = 11.dp),
                        )
                    }
                }
            } else {
                // Actual Calls List
                val filteredCalls = remember(vm.recentCalls, vm.callFilter, vm.callsSearch) {
                    val q = vm.callsSearch.trim().lowercase()
                    vm.recentCalls.filter { call ->
                        val matchesFilter = when (vm.callFilter) {
                            CallFilter.ALL -> true
                            CallFilter.MISSED -> call.type == CallType.MISSED || call.isSpam
                        }
                        val matchesSearch = q.isEmpty() ||
                            call.name.lowercase().contains(q) ||
                            call.number.contains(q) ||
                            call.location.lowercase().contains(q)
                        matchesFilter && matchesSearch
                    }
                }

                if (filteredCalls.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .padding(horizontal = 24.dp, vertical = 40.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (vm.callsSearch.isNotEmpty()) {
                                "No calls found."
                            } else if (vm.callFilter == CallFilter.MISSED) {
                                "No missed calls."
                            } else {
                                "No call history."
                            },
                            color = PhoneColors.TextSecondary,
                            fontSize = 15.sp,
                            textAlign = TextAlign.Center,
                        )
                    }
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                    ) {
                        items(filteredCalls, key = { it.id }) { call ->
                            CallItemRow(
                                call = call,
                                onClick = { vm.dialCall(call.number, call.name) },
                                onInfoClick = {
                                    val contact = vm.contacts.firstOrNull { it.phones.any { p -> p.contains(call.number) } }
                                        ?: Contact(
                                            id = call.id.toLongOrNull() ?: 0L,
                                            lookupKey = call.number,
                                            name = call.name,
                                            phones = listOf(call.number),
                                        )
                                    vm.openContact(contact)
                                },
                            )
                        }

                        // Extra padding at bottom to clear bottom row
                        item {
                            Spacer(Modifier.height(if (vm.keypadVisible) 480.dp else 110.dp))
                        }
                    }
                }
            }
        }

        }

        // Bottom Coordinated Floating Bar when keypad is hidden (Side-by-side: Pill Nav + Dialpad FAB)
        // This ensures they NEVER overlap on any screen width!
        AnimatedVisibility(
            visible = !vm.keypadVisible,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(150)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(bottom = 20.dp, start = 16.dp, end = 16.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp),
            ) {
                FloatingPillNav(
                    currentTab = vm.tab,
                    onCallsClick = {
                        if (vm.digits.isNotEmpty()) {
                            vm.openPeek()
                        } else {
                            vm.tab = Tab.KEYPAD
                            vm.keypadVisible = true
                        }
                    },
                    onContactsClick = { vm.tab = Tab.CONTACTS },
                    glass = glass,
                )

                // Dialpad FAB
                GlossButton(
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp),
                    onClick = { vm.keypadVisible = true },
                ) {
                    SvgIcon(PhoneIcons.DialpadGrid, Color.White, 24.dp)
                }
            }
        }

        // Keypad Bottom Sheet (Screenshots 1, 2, 3, 4, 11)
        AnimatedVisibility(
            visible = vm.keypadVisible,
            enter = slideInVertically(tween(260)) { it } + fadeIn(tween(200)),
            exit = slideOutVertically(tween(220)) { it } + fadeOut(tween(150)),
            modifier = Modifier.align(Alignment.BottomCenter),
        ) {
            KeypadSheet(vm = vm, glass = glass)
        }

        // Overflow menu (glass card), last so it sits on top of everything
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
            GlassMenuItem("Refresh Call Log", color = PhoneColors.TextSecondary) {
                menuExpanded = false
                vm.reloadCallLogs()
            }
        }
    }
}

@Composable
private fun CallsTopBar(
    vm: PhoneViewModel,
    searchFocusRequester: FocusRequester,
    onSearchClick: () -> Unit,
    onCancelSearch: () -> Unit,
    onMenuClick: () -> Unit,
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusManager = LocalFocusManager.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            // Title "Calls" matching screenshots
            Text(
                text = "Calls",
                color = PhoneColors.TextMain,
                fontSize = 26.sp,
                fontWeight = FontWeight.Bold,
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                // Segmented Pill Toggle [ All | Missed ]
                Row(
                    modifier = Modifier
                        .height(34.dp)
                        .glass(CircleShape, PhoneGlass.track)
                        .padding(3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    val allSelected = vm.callFilter == CallFilter.ALL
                    Box(
                        modifier = Modifier
                            .then(if (allSelected) Modifier.glass(CircleShape, PhoneGlass.selected) else Modifier.clip(CircleShape))
                            .clickable { vm.callFilter = CallFilter.ALL }
                            .padding(horizontal = 14.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "All",
                            color = if (allSelected) PhoneColors.TextMain else PhoneColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (allSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }

                    val missedSelected = vm.callFilter == CallFilter.MISSED
                    Box(
                        modifier = Modifier
                            .then(if (missedSelected) Modifier.glass(CircleShape, PhoneGlass.selected) else Modifier.clip(CircleShape))
                            .clickable { vm.callFilter = CallFilter.MISSED }
                            .padding(horizontal = 12.dp, vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Missed",
                            color = if (missedSelected) PhoneColors.TextMain else PhoneColors.TextSecondary,
                            fontSize = 12.sp,
                            fontWeight = if (missedSelected) FontWeight.SemiBold else FontWeight.Normal,
                        )
                    }
                }

                // Search Icon Button
                GlassButton(
                    shape = CircleShape,
                    style = PhoneGlass.circle,
                    modifier = Modifier.size(36.dp),
                    onClick = onSearchClick,
                ) {
                    SvgIcon(PhoneIcons.Search, PhoneColors.TextMain, 20.dp)
                }

                // Three-dots Menu Button
                GlassButton(
                    shape = CircleShape,
                    style = PhoneGlass.circle,
                    modifier = Modifier.size(36.dp),
                    onClick = onMenuClick,
                ) {
                    SvgIcon(PhoneIcons.ThreeDots, PhoneColors.TextMain, 20.dp)
                }
            }
        }

        // Expandable search bar if search button clicked
        AnimatedVisibility(visible = vm.isSearchingCalls) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    modifier = Modifier
                        .weight(1f)
                        .glass(CircleShape, PhoneGlass.track)
                        .padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Clicking search icon dismisses the keyboard automatically
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
                        value = vm.callsSearch,
                        onValueChange = { vm.callsSearch = it },
                        singleLine = true,
                        textStyle = TextStyle(color = PhoneColors.TextMain, fontSize = 14.sp),
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
                            if (vm.callsSearch.isEmpty()) {
                                Text("Search calls", color = PhoneColors.TextSecondary, fontSize = 14.sp)
                            }
                            inner()
                        },
                    )
                    if (vm.callsSearch.isNotEmpty()) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clickable { vm.callsSearch = "" },
                            contentAlignment = Alignment.Center,
                        ) {
                            SvgIcon(PhoneIcons.Close, PhoneColors.TextSecondary, 16.dp)
                        }
                    }
                }

                Spacer(Modifier.width(10.dp))

                Text(
                    text = "Cancel",
                    color = PhoneColors.CallGreen,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable(onClick = onCancelSearch)
                        .padding(horizontal = 4.dp, vertical = 6.dp),
                )
            }
        }
    }
}

@Composable
private fun CallItemRow(
    call: RecentCall,
    onClick: () -> Unit,
    onInfoClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // Avatar Circle with dynamic initial or accent color
        val avatarColor = getAvatarColor(call.name)
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(avatarColor),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = initialOf(call.name),
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.SemiBold,
            )
        }

        Spacer(Modifier.width(14.dp))

        // Call Info Center
        Column(Modifier.weight(1f)) {
            Text(
                text = call.name,
                color = PhoneColors.TextMain,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )

            Spacer(Modifier.height(3.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(5.dp),
            ) {
                // Call Direction Icon
                val arrowIcon = when (call.type) {
                    CallType.INCOMING -> PhoneIcons.CallIncoming
                    CallType.OUTGOING -> PhoneIcons.CallOutgoing
                    CallType.MISSED -> PhoneIcons.CallMissed
                }
                val arrowTint = if (call.isSpam || call.type == CallType.MISSED) PhoneColors.OrangeAlert else PhoneColors.TextSecondary
                SvgIcon(arrowIcon, arrowTint, 12.dp)

                // VoLTE badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(3.dp))
                        .background(if (PhoneColors.isDark) Color(0xFF242730) else Color(0xFFE5E7EB))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = "VoLTE",
                        color = PhoneColors.TextSecondary,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Medium,
                    )
                }

                // Location / Number Label
                Text(
                    text = call.location,
                    color = PhoneColors.TextSecondary,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }

            // Spam / Warning tag
            if (call.tag != null) {
                Spacer(Modifier.height(2.dp))
                Text(
                    text = call.tag,
                    color = PhoneColors.OrangeAlert,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                )
            }
        }

        Spacer(Modifier.width(8.dp))

        // Time and Info (i) Icon Button
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = call.time,
                color = PhoneColors.TextSecondary,
                fontSize = 12.sp,
            )

            PressBox(
                normal = Color.Transparent,
                pressed = PhoneColors.ActionCircle,
                modifier = Modifier.size(34.dp),
                shape = CircleShape,
                onClick = onInfoClick,
            ) {
                SvgIcon(PhoneIcons.Info, PhoneColors.TextSecondary, 18.dp)
            }
        }
    }
}

@Composable
private fun KeypadSheet(vm: PhoneViewModel, glass: GlassSource) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .glass(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp), PhoneGlass.sheet, glass)
            .navigationBarsPadding()
            .padding(top = 10.dp, bottom = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // Drag Handle (collapses the keypad sheet)
        Box(
            modifier = Modifier
                .width(42.dp)
                .height(5.dp)
                .clip(CircleShape)
                .background(PhoneColors.HandleColor)
                .clickable { vm.keypadVisible = false }
        )

        // Suggestions Area (matches typed digits against contacts)
        val suggestions = vm.suggestions()
        if (suggestions.isNotEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 6.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                for ((contact, phone) in suggestions) {
                    GlassButton(
                        shape = RoundedCornerShape(16.dp),
                        style = PhoneGlass.item,
                        modifier = Modifier.fillMaxWidth(),
                        onClick = { vm.openContact(contact) },
                    ) {
                      Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterStart) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            val color = getAvatarColor(contact.name)
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(color),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    initialOf(contact.name),
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                )
                            }
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(
                                    contact.name,
                                    color = PhoneColors.TextMain,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(phone, color = PhoneColors.TextSecondary, fontSize = 11.sp)
                            }
                        }
                      }
                    }
                }
            }
        }

        // Typed Digits Display
        if (vm.digits.isNotEmpty()) {
            Text(
                text = vm.digits,
                color = if (vm.digitsFlash) PhoneColors.CallGreen else PhoneColors.TextMain,
                fontSize = 38.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 1.sp,
                textAlign = TextAlign.Center,
                maxLines = 1,
                softWrap = false,
                overflow = TextOverflow.Clip,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 4.dp),
            )
        } else {
            Spacer(Modifier.height(8.dp))
        }

        // 4 x 3 Grid of Stadium-Pill Shaped Keys (Screenshots 1, 4, 11)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            for (row in KEY_ROWS) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    for (key in row) {
                        GlassButton(
                            shape = RoundedCornerShape(22.dp),
                            style = PhoneGlass.key,
                            modifier = Modifier
                                .weight(1f)
                                .height(52.dp),
                            onClick = { vm.typeKey(key.value) },
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                            ) {
                                Text(
                                    text = key.value,
                                    color = PhoneColors.TextMain,
                                    fontSize = 25.sp,
                                    fontWeight = FontWeight.Medium,
                                    lineHeight = 26.sp,
                                )
                                if (key.letters.isNotEmpty()) {
                                    Text(
                                        text = key.letters.uppercase(),
                                        color = PhoneColors.TextSecondary,
                                        fontSize = 9.sp,
                                        letterSpacing = 1.sp,
                                        lineHeight = 10.sp,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Action Row below keys:
        // Left: Keypad hide pill | Center: Green Call Pill | Right: Backspace pill
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 28.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            // Left Pill: Toggle / Hide Keypad
            GlassButton(
                shape = RoundedCornerShape(20.dp),
                style = PhoneGlass.key,
                modifier = Modifier
                    .width(64.dp)
                    .height(48.dp),
                onClick = { vm.keypadVisible = false },
            ) {
                SvgIcon(PhoneIcons.DialpadGrid, PhoneColors.TextMain, 22.dp)
            }

            // Center Pill: Green Call Button (Wide Stadium Pill)
            GlossButton(
                shape = CircleShape,
                modifier = Modifier
                    .width(110.dp)
                    .height(54.dp),
                onClick = { vm.pressCall() },
            ) {
                SvgIcon(PhoneIcons.Call, Color.White, 26.dp)
            }

            // Right Pill: Backspace Button
            GlassButton(
                shape = RoundedCornerShape(20.dp),
                style = PhoneGlass.key,
                modifier = Modifier
                    .width(64.dp)
                    .height(48.dp),
                onClick = { vm.backspace() },
                onLongClick = { vm.clearDigits() },
            ) {
                SvgIcon(PhoneIcons.Backspace, PhoneColors.TextSecondary, 22.dp)
            }
        }

        // Docked Bottom Navigation Pill [ Calls | Contacts ]
        FloatingPillNav(
            currentTab = vm.tab,
            onCallsClick = {
                if (vm.digits.isNotEmpty()) {
                    // Secret trigger when digits are typed!
                    vm.openPeek()
                } else {
                    vm.tab = Tab.KEYPAD
                }
            },
            onContactsClick = { vm.tab = Tab.CONTACTS },
            docked = true,
            modifier = Modifier.padding(top = 2.dp, bottom = 4.dp),
        )
    }
}

@Composable
fun FloatingPillNav(
    currentTab: Tab,
    onCallsClick: () -> Unit,
    onContactsClick: () -> Unit,
    modifier: Modifier = Modifier,
    glass: GlassSource? = null,
    docked: Boolean = false,
) {
    Row(
        modifier = modifier
            .glass(CircleShape, if (docked) PhoneGlass.dockedPill else PhoneGlass.floatingPill, glass)
            .padding(horizontal = 6.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        val callsActive = currentTab == Tab.KEYPAD
        Row(
            modifier = Modifier
                .then(if (callsActive) Modifier.glass(CircleShape, PhoneGlass.selected) else Modifier.clip(CircleShape))
                .clickable(onClick = onCallsClick)
                .padding(horizontal = 18.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SvgIcon(
                PhoneIcons.Call,
                if (callsActive) PhoneColors.TextMain else PhoneColors.TextSecondary,
                16.dp,
            )
            Text(
                text = "Calls",
                color = if (callsActive) PhoneColors.TextMain else PhoneColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = if (callsActive) FontWeight.SemiBold else FontWeight.Normal,
            )
        }

        val contactsActive = currentTab == Tab.CONTACTS
        Row(
            modifier = Modifier
                .then(if (contactsActive) Modifier.glass(CircleShape, PhoneGlass.selected) else Modifier.clip(CircleShape))
                .clickable(onClick = onContactsClick)
                .padding(horizontal = 18.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            SvgIcon(
                PhoneIcons.People,
                if (contactsActive) PhoneColors.TextMain else PhoneColors.TextSecondary,
                16.dp,
            )
            Text(
                text = "Contacts",
                color = if (contactsActive) PhoneColors.TextMain else PhoneColors.TextSecondary,
                fontSize = 12.sp,
                fontWeight = if (contactsActive) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}

fun initialOf(name: String): String =
    if (name.isEmpty()) "?" else name.substring(0, 1).uppercase()
