package com.arady.phone.data

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

// Calls dialed inside this app never reach the system call log (the app does not place real calls),
// so they are remembered here and merged into the history list.
class AppCallStore(context: Context) {
    private val store = context.getSharedPreferences("app_calls", Context.MODE_PRIVATE)

    class Entry(val id: String, val name: String, val number: String, val timestamp: Long)

    fun load(): List<Entry> {
        val raw = store.getString("calls", null) ?: return emptyList()
        return try {
            val arr = JSONArray(raw)
            (0 until arr.length()).map {
                val o = arr.getJSONObject(it)
                Entry(o.getString("id"), o.getString("name"), o.getString("number"), o.getLong("ts"))
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun add(entry: Entry) {
        val all = (listOf(entry) + load()).take(MAX)
        val arr = JSONArray()
        for (e in all) {
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("name", e.name)
                    .put("number", e.number)
                    .put("ts", e.timestamp),
            )
        }
        store.edit().putString("calls", arr.toString()).apply()
    }

    companion object {
        const val MAX = 200
    }
}
