package com.arady.phone.data

import android.content.ContentResolver
import android.os.Bundle
import android.provider.CallLog
import com.arady.phone.CallType
import com.arady.phone.RecentCall
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class TimedCall(val timestamp: Long, val call: RecentCall)

class CallLogRepository(private val resolver: ContentResolver) {

    fun load(contacts: List<Contact>): List<TimedCall> {
        val calls = ArrayList<TimedCall>()
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

        val nowCal = Calendar.getInstance()
        val todayYear = nowCal.get(Calendar.YEAR)
        val todayDay = nowCal.get(Calendar.DAY_OF_YEAR)

        val cursor = try {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val args = Bundle().apply {
                    putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(CallLog.Calls.DATE))
                    putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
                    putInt(ContentResolver.QUERY_ARG_LIMIT, 300)
                }
                resolver.query(CallLog.Calls.CONTENT_URI, null, args, null)
            } else {
                resolver.query(CallLog.Calls.CONTENT_URI, null, null, null, "${CallLog.Calls.DATE} DESC")
            }
        } catch (_: Exception) {
            try {
                resolver.query(CallLog.Calls.CONTENT_URI, null, null, null, "${CallLog.Calls.DATE} DESC")
            } catch (_: Exception) {
                null
            }
        } ?: return emptyList()

        cursor.use {
            val idCol = it.getColumnIndex(CallLog.Calls._ID)
            val numCol = it.getColumnIndex(CallLog.Calls.NUMBER)
            val nameCol = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val typeCol = it.getColumnIndex(CallLog.Calls.TYPE)
            val dateCol = it.getColumnIndex(CallLog.Calls.DATE)
            val locCol = it.getColumnIndex(CallLog.Calls.GEOCODED_LOCATION)

            var fallbackId = 0L
            while (it.moveToNext()) {
                val id = if (idCol >= 0) it.getLong(idCol).toString() else (++fallbackId).toString()
                val rawNumber = if (numCol >= 0) it.getString(numCol).orEmpty() else ""
                val cachedName = if (nameCol >= 0) it.getString(nameCol) else null
                val rawType = if (typeCol >= 0) it.getInt(typeCol) else CallLog.Calls.INCOMING_TYPE
                val epochMillis = if (dateCol >= 0) it.getLong(dateCol) else System.currentTimeMillis()
                val location = (if (locCol >= 0) it.getString(locCol) else null) ?: "Unknown location"

                val callType = when (rawType) {
                    CallLog.Calls.MISSED_TYPE,
                    CallLog.Calls.REJECTED_TYPE,
                    CallLog.Calls.BLOCKED_TYPE -> CallType.MISSED
                    CallLog.Calls.OUTGOING_TYPE -> CallType.OUTGOING
                    else -> CallType.INCOMING
                }

                val displayName = if (!cachedName.isNullOrBlank()) {
                    cachedName
                } else {
                    val clean = ContactsRepository.cleanNumber(rawNumber)
                    val matchedContact = contacts.firstOrNull { c ->
                        c.phones.any { p ->
                            p == clean || clean.endsWith(p) || p.endsWith(clean)
                        }
                    }
                    matchedContact?.name ?: if (rawNumber.isNotBlank()) rawNumber else "Unknown"
                }

                // Format time string
                val callCal = Calendar.getInstance().apply { timeInMillis = epochMillis }
                val callYear = callCal.get(Calendar.YEAR)
                val callDay = callCal.get(Calendar.DAY_OF_YEAR)

                val formattedTime = when {
                    callYear == todayYear && callDay == todayDay -> timeFormat.format(Date(epochMillis)).lowercase()
                    callYear == todayYear && callDay == todayDay - 1 -> "Yesterday"
                    else -> dateFormat.format(Date(epochMillis))
                }

                val isSpam = rawType == CallLog.Calls.BLOCKED_TYPE
                val tag = if (isSpam) "Spam / Blocked" else null

                calls.add(
                    TimedCall(epochMillis, RecentCall(
                        id = "sys-$id",
                        name = displayName,
                        number = rawNumber,
                        type = callType,
                        location = if (location.isNotBlank()) location else "Unknown location",
                        tag = tag,
                        time = formattedTime,
                        isSpam = isSpam,
                    ))
                )
            }
        }

        return calls
    }

    companion object {
        fun formatTime(epochMillis: Long): String {
            val now = Calendar.getInstance()
            val c = Calendar.getInstance().apply { timeInMillis = epochMillis }
            val sameYear = c.get(Calendar.YEAR) == now.get(Calendar.YEAR)
            val day = c.get(Calendar.DAY_OF_YEAR)
            val today = now.get(Calendar.DAY_OF_YEAR)
            return when {
                sameYear && day == today -> SimpleDateFormat("h:mm a", Locale.getDefault()).format(Date(epochMillis)).lowercase()
                sameYear && day == today - 1 -> "Yesterday"
                else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(epochMillis))
            }
        }
    }
}
