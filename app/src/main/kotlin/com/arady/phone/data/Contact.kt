package com.arady.phone.data

data class Contact(
    val id: Long,
    val lookupKey: String,
    val name: String,
    val phones: List<String>,
) {
    val primaryPhone: String
        get() = phones.firstOrNull().orEmpty()
}
