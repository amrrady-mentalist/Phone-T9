package com.arady.phone.data

import android.content.Context

// Replaces the web app's localStorage entry "dialer_magic_conf" ({ apiUrl }).
class Prefs(context: Context) {
    private val store = context.getSharedPreferences("dialer_magic_conf", Context.MODE_PRIVATE)

    var apiUrl: String
        get() = store.getString("apiUrl", "") ?: ""
        set(value) {
            store.edit().putString("apiUrl", value).apply()
        }
}
