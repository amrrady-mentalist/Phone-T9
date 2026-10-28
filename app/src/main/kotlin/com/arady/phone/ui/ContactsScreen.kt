package com.arady.phone.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arady.phone.PhoneViewModel
import com.arady.phone.data.Contact

private sealed interface ListRow {
    class Header(val letter: String) : ListRow
    class Item(val contact: Contact) : ListRow
}

// Same grouping rule as the web app: Latin, Cyrillic, Arabic letters and digits get their own section,
// everything else goes under "#".
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
    Column(Modifier.fillMaxSize()) {
        Text(
            "Contacts",
            color = Color.White,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(start = 24.dp, top = 12.dp, end = 24.dp, bottom = 8.dp),
        )

        BasicTextField(
            value = vm.contactsSearch,
            onValueChange = { vm.contactsSearch = it },
            singleLine = true,
            textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
            cursorBrush = SolidColor(Color.White),
            modifier = Modifier
                .padding(start = 24.dp, top = 4.dp, end = 24.dp, bottom = 12.dp)
                .fillMaxWidth()
                .background(PhoneColors.SearchBg, RoundedCornerShape(10.dp))
                .padding(horizontal = 12.dp, vertical = 8.dp),
            decorationBox = { inner ->
                Box {
                    if (vm.contactsSearch.isEmpty()) {
                        Text("Search", color = PhoneColors.TextMuted, fontSize = 15.sp)
                    }
                    inner()
                }
            },
        )

        if (!vm.contactsPermissionGranted) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    "Allow access to your contacts to see them here.",
                    color = PhoneColors.TextMuted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                )
                Box(Modifier.height(16.dp))
                PressBox(
                    normal = PhoneColors.CallGreen,
                    pressed = PhoneColors.CallGreenActive,
                    shape = RoundedCornerShape(8.dp),
                    onClick = requestPermission,
                ) {
                    Text(
                        "Allow Contacts Access",
                        color = Color.White,
                        fontSize = 15.sp,
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
                    color = PhoneColors.TextMuted,
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(40.dp),
                )
            } else {
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                ) {
                    items(rows) { row ->
                        when (row) {
                            is ListRow.Header -> Text(
                                row.letter,
                                color = PhoneColors.CallGreen,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(start = 4.dp, top = 14.dp, bottom = 6.dp),
                            )
                            is ListRow.Item -> Column {
                                PressBox(
                                    normal = Color.Transparent,
                                    pressed = Color(0x08FFFFFF),
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(0.dp),
                                    contentAlignment = Alignment.CenterStart,
                                    onClick = { vm.openContact(row.contact) },
                                ) {
                                    Text(
                                        row.contact.name,
                                        color = Color.White,
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 11.dp),
                                    )
                                }
                                Box(
                                    Modifier
                                        .fillMaxWidth()
                                        .height(1.dp)
                                        .background(PhoneColors.Divider),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
