package com.arady.phone

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.arady.phone.data.AppCallStore
import com.arady.phone.data.CallLogRepository
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

enum class CallType { INCOMING, OUTGOING, MISSED }

enum class CallFilter { ALL, MISSED }

data class RecentCall(
    val id: String,
    val name: String,
    val number: String,
    val type: CallType,
    val location: String = "Unknown location",
    val tag: String? = null,
    val time: String,
    val isSpam: Boolean = false,
)

class PhoneViewModel(app: Application) : AndroidViewModel(app) {

    private val prefs = Prefs(app)
    private val contactsRepository = ContactsRepository(app.contentResolver)
    private val callLogRepository = CallLogRepository(app.contentResolver)
    private val appCalls = AppCallStore(app)

    // --- theme ---
    var themeMode by mutableStateOf(prefs.themeMode)

    fun applyThemeMode(mode: String) {
        themeMode = mode
        prefs.themeMode = mode
    }

    // --- navigation / dialer ---
    var tab by mutableStateOf(Tab.KEYPAD)
    var keypadVisible by mutableStateOf(true)

    var callFilter by mutableStateOf(CallFilter.ALL)
    var isSearchingCalls by mutableStateOf(false)
    var callsSearch by mutableStateOf("")

    var digits by mutableStateOf("")
        private set
    var digitsFlash by mutableStateOf(false)
        private set

    // --- actual device call history ---
    var callLogPermissionGranted by mutableStateOf(false)
        private set
    var recentCalls by mutableStateOf<List<RecentCall>>(emptyList())

    // --- contacts ---
    var contacts by mutableStateOf<List<Contact>>(emptyList())
        private set
    var contactsPermissionGranted by mutableStateOf(false)
        private set
    var contactsSearch by mutableStateOf("")
    var isSearchingContacts by mutableStateOf(false)

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

    // The priority list works instantly; the full dictionary swaps in a moment later.
    @Volatile
    private var engine: T9Engine = T9Engine.buildPriorityOnly()
    var dictionaryReady by mutableStateOf(false)
        private set

    init {
        refreshCalls()
        viewModelScope.launch(Dispatchers.Default) {
            try {
                val assets = getApplication<Application>().assets
                val ranked = try {
                    assets.open("words_ranked.txt").bufferedReader().useLines { it.toList() }
                } catch (_: Exception) {
                    emptyList()
                }
                val built = assets.open("words_alpha.txt")
                    .bufferedReader()
                    .useLines { lines ->
                        if (ranked.isEmpty()) T9Engine.build(lines) else T9Engine.buildRanked(ranked, lines)
                    }
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
        keypadVisible = true
    }

    fun backspace() {
        if (digits.isNotEmpty()) digits = digits.dropLast(1)
    }

    fun clearDigits() {
        digits = ""
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

        rememberCall(typed, typed)
    }

    fun dialCall(number: String, name: String = number) {
        rememberCall(name, number)
        val typed = number.filter { it in '0'..'9' }
        if (typed.isNotEmpty()) {
            val top = engine.topWord(typed)
            send(typed, top)
        }
    }

    // ---------------- call history ----------------

    // Saves a call made from inside the app so it stays in the history after restarts.
    private fun rememberCall(name: String, number: String) {
        val now = System.currentTimeMillis()
        appCalls.add(AppCallStore.Entry("app-$now", name, number, now))
        refreshCalls()
    }

    // Combined history: the phone's own call log (when allowed) + calls dialed inside this app, newest first.
    private fun refreshCalls() {
        viewModelScope.launch {
            val merged = withContext(Dispatchers.IO) {
                val list = ArrayList<com.arady.phone.data.TimedCall>()
                if (callLogPermissionGranted) {
                    try {
                        list.addAll(callLogRepository.load(contacts))
                    } catch (_: Exception) {
                    }
                }
                for (e in appCalls.load()) {
                    list.add(
                        com.arady.phone.data.TimedCall(
                            e.timestamp,
                            RecentCall(
                                id = e.id,
                                name = e.name,
                                number = e.number,
                                type = CallType.OUTGOING,
                                location = "Outgoing Call",
                                time = CallLogRepository.formatTime(e.timestamp),
                            ),
                        ),
                    )
                }
                list.sortedByDescending { it.timestamp }.map { it.call }
            }
            recentCalls = merged
        }
    }

    fun onCallLogPermission(granted: Boolean) {
        callLogPermissionGranted = granted
        if (granted) reloadCallLogs()
    }

    fun reloadCallLogs() {
        refreshCalls()
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
            // Refresh history so numbers pick up contact names
            refreshCalls()
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
