package com.arady.phone.data

import android.content.ContentResolver
import android.provider.CallLog
import com.arady.phone.CallType
import com.arady.phone.RecentCall
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class CallLogRepository(private val resolver: ContentResolver) {

    fun load(contacts: List<Contact>): List<RecentCall> {
        val projection = arrayOf(
            CallLog.Calls._ID,
            CallLog.Calls.NUMBER,
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.TYPE,
            CallLog.Calls.DATE,
            CallLog.Calls.GEOCODED_LOCATION,
        )

        val calls = ArrayList<RecentCall>()
        val timeFormat = SimpleDateFormat("h:mm a", Locale.getDefault())
        val dateFormat = SimpleDateFormat("MMM d", Locale.getDefault())

        val nowCal = Calendar.getInstance()
        val todayYear = nowCal.get(Calendar.YEAR)
        val todayDay = nowCal.get(Calendar.DAY_OF_YEAR)

        val cursor = try {
            resolver.query(
                CallLog.Calls.CONTENT_URI,
                projection,
                null,
                null,
                "${CallLog.Calls.DATE} DESC LIMIT 150",
            )
        } catch (_: SecurityException) {
            null
        } ?: return emptyList()

        cursor.use {
            val idCol = it.getColumnIndexOrThrow(CallLog.Calls._ID)
            val numCol = it.getColumnIndexOrThrow(CallLog.Calls.NUMBER)
            val nameCol = it.getColumnIndexOrThrow(CallLog.Calls.CACHED_NAME)
            val typeCol = it.getColumnIndexOrThrow(CallLog.Calls.TYPE)
            val dateCol = it.getColumnIndexOrThrow(CallLog.Calls.DATE)
            val locCol = it.getColumnIndexOrThrow(CallLog.Calls.GEOCODED_LOCATION)

            while (it.moveToNext()) {
                val id = it.getLong(idCol).toString()
                val rawNumber = it.getString(numCol).orEmpty()
                val cachedName = it.getString(nameCol)
                val rawType = it.getInt(typeCol)
                val epochMillis = it.getLong(dateCol)
                val location = it.getString(locCol) ?: "Unknown location"

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
                    RecentCall(
                        id = id,
                        name = displayName,
                        number = rawNumber,
                        type = callType,
                        location = if (location.isNotBlank()) location else "Unknown location",
                        tag = tag,
                        time = formattedTime,
                        isSpam = isSpam,
                    )
                )
            }
        }

        return calls
    }
}
