package com.arady.phone

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arady.phone.data.Contact
import com.arady.phone.data.ContactsRepository
import com.arady.phone.data.Prefs
import com.arady.phone.net.ApiClient
import com.arady.phone.t9.T9Engine
import com.arady.phone.t9.T9Match
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class Tab { KEYPAD, CONTACTS }

class PhoneViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    private val contactsRepository = ContactsRepository(app.contentResolver)

    // --- navigation / dialer ---
    var tab by mutableStateOf(Tab.KEYPAD)
    var digits by mutableStateOf("")
        private set
    var digitsFlash by mutableStateOf(false)
        private set

    // --- contacts ---
    var contacts by mutableStateOf<List<Contact>>(emptyList())
        private set
    var contactsPermissionGranted by mutableStateOf(false)
        private set
    var contactsSearch by mutableStateOf("")

    // --- details screen ---
    var detailsVisible by mutableStateOf(false)
        private set
    var detailsContact by mutableStateOf<Contact?>(null)
        private set

    // --- secret T9 popup ---
    var peekVisible by mutableStateOf(false)
        private set
    var peekItems by mutableStateOf<List<T9Match>>(emptyList())
        private set
    private var peekSequence = ""

    // --- settings ---
    var settingsVisible by mutableStateOf(false)
        private set
    var apiUrl by mutableStateOf(prefs.apiUrl)
        private set

    // The priority list works instantly; the full dictionary swaps in a moment later (about a second).
    @Volatile
    private var engine: T9Engine = T9Engine.buildPriorityOnly()
    var dictionaryReady by mutableStateOf(false)
        private set

    init {
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val built = getApplication<Application>().assets.open("words_alpha.txt")
                    .bufferedReader()
                    .useLines { lines -> T9Engine.build(lines) }
                engine = built
                dictionaryReady = true
            } catch (_: Exception) {
                // keep the priority-only engine
            }
        }
    }

    // ---------------- keypad ----------------

    fun typeKey(key: String) {
        digits += key
    }

    fun backspace() {
        if (digits.isNotEmpty()) digits = digits.dropLast(1)
    }

    private fun digitsOnly(): String = digits.filter { it in '0'..'9' }

    // Contacts whose number contains the typed digits (max 2), like the original suggestions area.
    fun suggestions(): List<Pair<Contact, String>> {
        val typed = digitsOnly()
        if (typed.isEmpty() || contacts.isEmpty()) return emptyList()
        val out = ArrayList<Pair<Contact, String>>()
        for (c in contacts) {
            val hit = c.phones.firstOrNull { it.contains(typed) } ?: continue
            out.add(c to hit)
            if (out.size == 2) break
        }
        return out
    }

    // ---------------- the secret ----------------

    // Hidden "Recents" tab: show the smart results for the digits on screen.
    fun openPeek() {
        val typed = digitsOnly()
        if (typed.isEmpty()) return
        peekSequence = typed
        peekItems = engine.matches(typed, 6)
        peekVisible = true
    }

    fun closePeek() {
        peekVisible = false
    }

    fun pickPeekWord(word: String) {
        peekVisible = false
        send(peekSequence, word)
    }

    // Green call button: flash the digits and send the best word.
    fun pressCall() {
        val typed = digitsOnly()
        if (typed.isEmpty()) return
        viewModelScope.launch {
            digitsFlash = true
            delay(1000)
            digitsFlash = false
        }
        val top = engine.topWord(typed)
        send(typed, top)
    }

    // ---------------- contacts ----------------

    fun onContactsPermission(granted: Boolean) {
        contactsPermissionGranted = granted
        if (granted) reloadContacts()
    }

    fun reloadContacts() {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) {
                try {
                    contactsRepository.load()
                } catch (_: Exception) {
                    emptyList()
                }
            }
            contacts = loaded
        }
    }

    fun openContact(contact: Contact) {
        detailsContact = contact
        detailsVisible = true
        send("contact_select", contact.name)
    }

    fun closeContact() {
        detailsVisible = false
    }

    // Events the web app also posted ("action_click" + one of the names below).
    fun trackAction(action: String) {
        send("action_click", action)
    }

    // ---------------- settings ----------------

    fun openSettings() {
        settingsVisible = true
    }

    fun closeSettings() {
        settingsVisible = false
    }

    fun saveApiUrl(url: String) {
        val clean = url.trim()
        apiUrl = clean
        prefs.apiUrl = clean
        settingsVisible = false
    }

    // ---------------- network ----------------

    // Same argument order as the web app's sendWordToApi(cleanSequence, wordToSend).
    private fun send(sequence: String, value: String) {
        ApiClient.send(apiUrl, sequence, value)
    }
}
