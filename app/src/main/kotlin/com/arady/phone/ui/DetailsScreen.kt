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
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.draw.clip
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
            .ambientGlow()
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
                        if (!valid) return@detectHorizontalDragGestures
                        total += delta
                        dragX = if (startX <= edgePx) total.coerceAtLeast(0f) else total.coerceAtMost(0f)
                        if (abs(total) > closePx) {
                            valid = false
                            dragX = 0f
                            vm.closeContact()
                        }
                    },
                    onDragEnd = { dragX = 0f },
                    onDragCancel = { dragX = 0f },
                )
            }
            .systemBarsPadding(),
    ) {
        // top bar: back arrow (left), favorite + edit (right)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TapBox(
                modifier = Modifier
                    .clip(CircleShape)
                    .padding(8.dp),
                contentAlignment = Alignment.Center,
                onClick = { vm.closeContact() },
            ) {
                SvgIcon(PhoneIcons.Back, PhoneColors.TextMain, 24.dp)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TapBox(
                    modifier = Modifier
                        .clip(CircleShape)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                    onClick = {
                        favorite = !favorite
                        vm.trackAction("favorite_toggle")
                    },
                ) {
                    SvgIcon(
                        PhoneIcons.Star,
                        if (favorite) PhoneColors.Yellow else PhoneColors.TextMain,
                        22.dp,
                        fillUnder = if (favorite) PhoneColors.Yellow else null,
                    )
                }
                TapBox(
                    modifier = Modifier
                        .clip(CircleShape)
                        .padding(8.dp),
                    contentAlignment = Alignment.Center,
                    onClick = {
                        vm.trackAction("edit")
                        val uri = Uri.withAppendedPath(ContactsContract.Contacts.CONTENT_URI, contact.id.toString())
                        launchSafely(context, Intent(Intent.ACTION_EDIT, uri))
                    },
                ) {
                    SvgIcon(PhoneIcons.Edit, PhoneColors.TextMain, 22.dp)
                }
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(16.dp))

            // Large Avatar Circle
            val avatarColor = getAvatarColor(contact.name)
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .background(avatarColor),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = initialOf(contact.name),
                    color = Color.White,
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                )
            }

            Spacer(Modifier.height(14.dp))

            Text(
                text = contact.name,
                color = PhoneColors.TextMain,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 24.dp),
            )

            Spacer(Modifier.height(24.dp))

            // Quick Actions Row (Call, Text, Video, Share)
            val primaryPhone = contact.phones.firstOrNull() ?: ""
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
            ) {
                ActionCircleBtn(PhoneIcons.Call, "Call") {
                    vm.trackAction("call")
                    if (primaryPhone.isNotEmpty()) {
                        launchSafely(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$primaryPhone")))
                    }
                }
                ActionCircleBtn(PhoneIcons.Message, "Text") {
                    vm.trackAction("text")
                    if (primaryPhone.isNotEmpty()) {
                        launchSafely(context, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$primaryPhone")))
                    }
                }
                ActionCircleBtn(PhoneIcons.Video, "Video") {
                    vm.trackAction("video")
                }
                ActionCircleBtn(PhoneIcons.Share, "Share") {
                    vm.trackAction("share")
                    if (primaryPhone.isNotEmpty()) {
                        val shareIntent = Intent(Intent.ACTION_SEND).apply {
                            type = "text/plain"
                            putExtra(Intent.EXTRA_TEXT, "${contact.name}: $primaryPhone")
                        }
                        launchSafely(context, Intent.createChooser(shareIntent, "Share Contact"))
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // Contact Numbers Card
            if (contact.phones.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .glass(RoundedCornerShape(26.dp), PhoneGlass.card)
                        .padding(horizontal = 18.dp, vertical = 12.dp),
                ) {
                    Text(
                        text = "Phone Numbers",
                        color = PhoneColors.TextSecondary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                    for (phone in contact.phones) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(
                                    text = phone,
                                    color = PhoneColors.TextMain,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Medium,
                                )
                                Text(
                                    text = "Mobile",
                                    color = PhoneColors.TextSecondary,
                                    fontSize = 12.sp,
                                )
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                GlassButton(
                                    shape = CircleShape,
                                    style = PhoneGlass.circle,
                                    modifier = Modifier.size(38.dp),
                                    onClick = {
                                        vm.trackAction("call")
                                        launchSafely(context, Intent(Intent.ACTION_DIAL, Uri.parse("tel:$phone")))
                                    },
                                ) {
                                    SvgIcon(PhoneIcons.Call, PhoneColors.CallGreen, 18.dp)
                                }
                                GlassButton(
                                    shape = CircleShape,
                                    style = PhoneGlass.circle,
                                    modifier = Modifier.size(38.dp),
                                    onClick = {
                                        vm.trackAction("text")
                                        launchSafely(context, Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:$phone")))
                                    },
                                ) {
                                    SvgIcon(PhoneIcons.Message, PhoneColors.TextMain, 18.dp)
                                }
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(40.dp))
        }
    }
}

@Composable
private fun ActionCircleBtn(icon: List<IconPart>, label: String, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        GlassButton(
            shape = CircleShape,
            style = PhoneGlass.circle,
            modifier = Modifier.size(52.dp),
            onClick = onClick,
        ) {
            SvgIcon(icon, PhoneColors.TextMain, 22.dp)
        }
        Text(
            text = label,
            color = PhoneColors.TextSecondary,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
        )
    }
}
