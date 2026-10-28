package com.arady.phone.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.arady.phone.PhoneViewModel
import com.arady.phone.data.Contact
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun DetailsOverlay(vm: PhoneViewModel) {
    val contact = vm.detailsContact
    BackHandler(enabled = vm.detailsVisible) { vm.closeContact() }
    AnimatedVisibility(
        visible = vm.detailsVisible && contact != null,
        enter = slideInHorizontally(animationSpec = tween(250)) { it },
        exit = slideOutHorizontally(animationSpec = tween(250)) { it },
    ) {
        if (contact != null) DetailsContent(vm, contact)
    }
}

private fun launchSafely(context: Context, intent: Intent) {
    try {
        context.startActivity(intent)
    } catch (_: Exception) {
    }
}

@Composable
private fun DetailsContent(vm: PhoneViewModel, contact: Contact) {
    val context = LocalContext.current
    var favorite by remember(contact.id) { mutableStateOf(false) }

    // edge swipe to close: starts within 25dp of either edge, closes after dragging more than 75dp
    var dragX by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val edgePx = with(density) { 25.dp.toPx() }
    val closePx = with(density) { 75.dp.toPx() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .offset { IntOffset(dragX.roundToInt(), 0) }
            .background(PhoneColors.DetailsBg)
            .pointerInput(Unit) {
                var startX = 0f
                var valid = false
                var total = 0f
                detectHorizontalDragGestures(
                    onDragStart = { offset ->
                        startX = offset.x
                        valid = offset.x <= edgePx || offset.x >= size.width - edgePx
                        total = 0f
                    },
                    onHorizontalDrag = { _, delta ->
                        if (valid) {
                            total += delta
                            val fromLeft = startX <= edgePx && total > 0
                            val fromRight = startX >= size.width - edgePx && total < 0
                            dragX = if (fromLeft || fromRight) abs(total) else 0f
                        }
                    },
                    onDragEnd = {
                        if (valid && abs(total) > closePx) vm.closeContact() else dragX = 0f
                        valid = false
                    },
                    onDragCancel = {
                        dragX = 0f
                        valid = false
                    },
                )
            }
            .systemBarsPadding(),
    ) {
        // top bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TapBox(Modifier.padding(8.dp), Alignment.Center, onClick = { vm.closeContact() }) {
                SvgIcon(PhoneIcons.Back, Color.White, 24.dp)
            }
            TapBox(
                Modifier.padding(8.dp),
                Alignment.Center,
                onClick = {
                    vm.trackAction("edit_contact")
                    val uri = ContactsContract.Contacts.getLookupUri(contact.id, contact.lookupKey)
                    val intent = Intent(Intent.ACTION_EDIT).apply {
                        setDataAndType(uri, ContactsContract.Contacts.CONTENT_ITEM_TYPE)
                    }
                    launchSafely(context, intent)
                },
            ) {
                SvgIcon(PhoneIcons.Edit, Color.White, 22.dp)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 10.dp, bottom = 20.dp)
                    .size(100.dp)
                    .background(PhoneColors.AvatarLarge, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(initialOf(contact.name), color = Color.White, fontSize = 42.sp, fontWeight = FontWeight.Medium)
            }
            Text(
                contact.name,
                color = Color.White,
                fontSize = 24.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(bottom = 28.dp),
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(28.dp, Alignment.CenterHorizontally),
            ) {
                ActionItem("Call", PhoneIcons.Call) {
                    vm.trackAction("call_contact")
                    launchSafely(context, Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", contact.primaryPhone, null)))
                }
                ActionItem("Text", PhoneIcons.Message) {
                    vm.trackAction("message_contact")
                    launchSafely(context, Intent(Intent.ACTION_SENDTO, Uri.fromParts("smsto", contact.primaryPhone, null)))
                }
                ActionItem(
                    label = if (favorite) "Favorited" else "Favorite",
                    icon = PhoneIcons.Star,
                    tint = if (favorite) PhoneColors.Yellow else PhoneColors.AndroidBlue,
                    fillUnder = if (favorite) PhoneColors.Yellow else null,
                ) {
                    favorite = !favorite
                    vm.trackAction(if (favorite) "favorite_added" else "favorite_removed")
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(PhoneColors.DetailsCard, RoundedCornerShape(16.dp))
                    .border(1.dp, PhoneColors.DetailsCardBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                for (phone in contact.phones) {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Mobile", color = PhoneColors.AndroidBlue, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        Text(phone, color = Color.White, fontSize = 16.sp)
                    }
                }
            }
        }
    }
}

@Composable
private fun ActionItem(
    label: String,
    icon: List<IconPart>,
    tint: Color = PhoneColors.AndroidBlue,
    fillUnder: Color? = null,
    onClick: () -> Unit,
) {
    TapBox(
        modifier = Modifier.width(60.dp),
        contentAlignment = Alignment.TopCenter,
        onClick = onClick,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PressBox(
                normal = PhoneColors.ActionCircle,
                pressed = PhoneColors.ActionCircleActive,
                modifier = Modifier.size(48.dp),
                onClick = onClick,
            ) {
                SvgIcon(icon, tint, 20.dp, fillUnder = fillUnder)
            }
            Text(label, color = Color(0xFFE3E3E3), fontSize = 12.sp)
        }
    }
}
