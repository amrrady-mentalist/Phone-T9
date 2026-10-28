package com.arady.phone.data

import android.content.ContentResolver
import android.provider.ContactsContract
import java.text.Collator
import java.util.Locale

// Reads the phone's own contacts (no more .vcf / .csv import).
// Numbers are cleaned the same way the web app cleaned them: spaces, dashes and brackets removed.
class ContactsRepository(private val resolver: ContentResolver) {

    fun load(): List<Contact> {
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.CONTACT_ID,
            ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
            ContactsContract.CommonDataKinds.Phone.NUMBER,
        )

        val names = LinkedHashMap<Long, String>()
        val lookups = HashMap<Long, String>()
        val numbers = LinkedHashMap<Long, LinkedHashSet<String>>()

        val cursor = resolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            projection,
            null,
            null,
            null,
        ) ?: return emptyList()

        cursor.use {
            val idCol = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.CONTACT_ID)
            val keyCol = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.LOOKUP_KEY)
            val nameCol = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
            val numCol = it.getColumnIndexOrThrow(ContactsContract.CommonDataKinds.Phone.NUMBER)
            while (it.moveToNext()) {
                val id = it.getLong(idCol)
                val name = (it.getString(nameCol) ?: "").trim()
                val phone = cleanNumber(it.getString(numCol) ?: "")
                if (name.isEmpty() || phone.isEmpty()) continue
                names.putIfAbsent(id, name)
                lookups.putIfAbsent(id, it.getString(keyCol) ?: "")
                numbers.getOrPut(id) { LinkedHashSet() }.add(phone)
            }
        }

        val contacts = names.map { (id, name) ->
            Contact(id, lookups[id].orEmpty(), name, numbers[id].orEmpty().toList())
        }

        // The web app sorted with localeCompare(["ar", "en"]).
        val collator = Collator.getInstance(Locale("ar"))
        return contacts.sortedWith(Comparator { a, b -> collator.compare(a.name, b.name) })
    }

    companion object {
        private val NOISE = Regex("[\\s\\-()]")

        fun cleanNumber(raw: String): String = raw.replace(NOISE, "").trim()
    }
}
