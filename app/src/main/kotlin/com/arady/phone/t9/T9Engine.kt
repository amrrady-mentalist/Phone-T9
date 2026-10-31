package com.arady.phone.t9

enum class MatchType { EXACT, SUGGEST, CORRECTION }

data class T9Match(val word: String, val type: MatchType)

// Pure Kotlin port of the web app's T9 engine (no Android imports, so it can be unit tested on the JVM).
//
// Behaviour is intentionally identical to the original JavaScript, including the order of ties:
// JS objects iterate "array index" keys (canonical numbers up to 4294967294) in ascending numeric order
// first, then every other key in insertion order. orderedKeys reproduces exactly that.
//
// Build one with T9Engine.build(...). An instance is read-only after build, so it is safe to share.
class T9Engine private constructor() {

    private val dictionary = LinkedHashMap<String, ArrayList<String>>()
    private val priorityIndex = HashMap<String, Int>()
    private val priorityOrder = ArrayList<String>() // distinct priority words, in list order
    // Frequency rank (0 = most common) of the common-word list. Empty = classic behaviour (identical to the web app).
    private val rankIndex = HashMap<String, Int>()
    private val rankedList = ArrayList<String>() // common words in rank order (ranked mode only)
    private var orderedKeys: Array<String> = emptyArray()

    val wordCount: Int
        get() = dictionary.values.sumOf { it.size }

    // Equivalent of the JS indexWordsIntoT9(). Priority words go to the front of their bucket (unshift),
    // dictionary words go to the back (push).
    private fun indexWords(words: Sequence<String>, lowPriority: Boolean) {
        for (raw in words) {
            val word = clean(raw)
            if (word.length <= 1) continue
            val seq = toSequence(word)
            val bucket = dictionary.getOrPut(seq) { ArrayList(2) }
            if (!bucket.contains(word)) {
                if (lowPriority) bucket.add(word) else bucket.add(0, word)
            }
        }
    }

    private fun finish() {
        val indexKeys = ArrayList<String>()
        val otherKeys = ArrayList<String>()
        for (key in dictionary.keys) {
            if (isArrayIndex(key)) indexKeys.add(key) else otherKeys.add(key)
        }
        // Digits 2-9 only, so no leading zeros: numeric order == (length, then lexicographic) order.
        indexKeys.sortWith(compareBy<String> { it.length }.thenBy { it })
        orderedKeys = (indexKeys + otherKeys).toTypedArray()

        // JS weight = 100000 - highPriorityWords.indexOf(word), i.e. the FIRST index of a duplicated word.
        PriorityWords.LIST.forEachIndexed { i, w ->
            val cleaned = clean(w)
            if (cleaned.length > 1 && !priorityIndex.containsKey(cleaned)) {
                priorityIndex[cleaned] = i
                priorityOrder.add(cleaned)
            }
        }
    }

    private fun weight(word: String): Int {
        val idx = priorityIndex[word]
        if (idx != null) return 100000 - idx
        val r = rankIndex[word] ?: return 100
        return 90000 - r // always below every priority word (>= 99800), above all unranked words
    }

    // Returns the ranked results for a digit sequence, exactly like getSmartT9Matches() in the original,
    // truncated to limit. Anything that is not a digit is ignored first (so '*' and '#' disappear).
    fun matches(input: String, limit: Int = Int.MAX_VALUE): List<T9Match> {
        val seq = input.filter { it in '0'..'9' }
        val n = seq.length
        if (n == 0 || limit <= 0) return emptyList()

        val results = ArrayList<T9Match>()

        // 1) Exact matches, sorted by weight (stable).
        val exact = dictionary[seq]
        if (exact != null) {
            for (w in exact.sortedByDescending { weight(it) }) {
                if (w == JS_QUIRK_WORD) continue
                // Ranked mode: rare dictionary words only fill in when fewer than 3 common words matched.
                if (rankIndex.isNotEmpty() && results.size >= 3 && !priorityIndex.containsKey(w) && !rankIndex.containsKey(w)) continue
                results.add(T9Match(w, MatchType.EXACT))
                if (results.size >= limit) return results
            }
        }
        val exactCount = results.size

        // 2) Predictions: longer sequences starting with the typed digits.
        //    Priority words come first (ordered by weight), then everything else in dictionary iteration order.
        for (w in priorityOrder) {
            val s = toSequence(w)
            if (w != JS_QUIRK_WORD && s.length > n && s.startsWith(seq) && !isBoringSuffix(w, n) &&
                (rankIndex.isEmpty() || s.length <= n + MAX_EXTRA)) {
                results.add(T9Match(w, MatchType.SUGGEST))
                if (results.size >= limit) return results
            }
        }
        if (rankIndex.isNotEmpty()) {
            // Ranked mode: predictions only from the common-word list, most common first, shorter words first.
            val cands = ArrayList<String>()
            for (key in orderedKeys) {
                if (key.length <= n || key.length > n + MAX_EXTRA || !key.startsWith(seq)) continue
                for (w in dictionary.getValue(key)) {
                    if (priorityIndex.containsKey(w) || !rankIndex.containsKey(w)) continue
                    if (w == JS_QUIRK_WORD || isBoringSuffix(w, n)) continue
                    cands.add(w)
                }
            }
            cands.sortWith(compareBy<String> { rankIndex.getValue(it) }.thenBy { it.length }.thenBy { it })
            for (w in cands) {
                results.add(T9Match(w, MatchType.SUGGEST))
                if (results.size >= limit) return results
            }
        }
        if (rankIndex.isEmpty()) scan@ for (key in orderedKeys) {
            if (key.length <= n || !key.startsWith(seq)) continue
            for (w in dictionary.getValue(key)) {
                if (priorityIndex.containsKey(w)) continue
                if (w == JS_QUIRK_WORD || isBoringSuffix(w, n)) continue
                results.add(T9Match(w, MatchType.SUGGEST))
                if (results.size >= limit) break@scan
            }
        }
        if (results.size >= limit) return results

        // Ranked mode: when fewer than 3 results, offer the closest common words that are within 1-2 letters
        // of the typed length: one wrong digit, one missing/extra digit, or extra digits typed after a shorter word.
        if (rankIndex.isNotEmpty() && results.size < 3) {
            val shown = HashSet<String>()
            for (r in results) shown.add(r.word)
            val near = ArrayList<String>()
            for (w in rankedList) {
                val wl = w.length
                if (wl < n - MAX_EXTRA || wl > n + 1 || shown.contains(w) || w == JS_QUIRK_WORD) continue
                val ws = toSequence(w)
                val ok = if (wl >= n - 1 && wl <= n + 1) withinOneEdit(seq, ws) else wl >= 4 && seq.startsWith(ws)
                if (ok) near.add(w)
            }
            near.sortWith(compareBy<String> { rankIndex.getValue(it) })
            for (w in near) {
                results.add(T9Match(w, MatchType.CORRECTION))
                if (results.size >= minOf(limit, 6) || results.size >= 6) break
            }
            if (results.size >= limit) return results.take(limit)
        }

        // 3) Corrections: only when nothing else was found. Same length, exactly one differing digit
        //    (for equal lengths that is the same thing as Levenshtein distance 1), best 3 by weight.
        if (results.isEmpty() && exactCount == 0) {
            val found = ArrayList<String>()
            for (key in orderedKeys) {
                if (key.length != n) continue
                var diff = 0
                for (i in 0 until n) {
                    if (key[i] != seq[i]) {
                        diff++
                        if (diff > 1) break
                    }
                }
                if (diff == 1) found.addAll(dictionary.getValue(key))
            }
            for (w in found.sortedByDescending { weight(it) }.filter { it != JS_QUIRK_WORD }.take(3)) {
                results.add(T9Match(w, MatchType.CORRECTION))
                if (results.size >= limit) break
            }
        }
        return results
    }

    // The word that the Call button sends: best match, or "unknown".
    fun topWord(input: String): String = matches(input, 1).firstOrNull()?.word ?: "unknown"

    companion object {
        private val BORING_SUFFIXES = setOf("er", "ly", "ing", "s", "est", "yl")

        // Quirk kept on purpose: the JS de-dupe used a plain object, where seen["constructor"] is already
        // truthy, so the original app can never show this one word. Keeping it makes results identical.
        private const val JS_QUIRK_WORD = "constructor"

        // Ranked mode never suggests words more than this many letters longer than what was typed.
        private const val MAX_EXTRA = 2

        private fun withinOneEdit(a: String, b: String): Boolean {
            val la = a.length
            val lb = b.length
            if (kotlin.math.abs(la - lb) > 1) return false
            var i = 0
            while (i < la && i < lb && a[i] == b[i]) i++
            if (i == la && i == lb) return true
            return when {
                la == lb -> a.substring(i + 1) == b.substring(i + 1)
                la > lb -> a.substring(i + 1) == b.substring(i)
                else -> a.substring(i) == b.substring(i + 1)
            }
        }

        private fun isBoringSuffix(word: String, typedLength: Int): Boolean =
            word.length > typedLength && word.substring(typedLength) in BORING_SUFFIXES

        private fun clean(raw: String): String {
            val sb = StringBuilder(raw.length)
            for (ch in raw.lowercase()) if (ch in 'a'..'z') sb.append(ch)
            return sb.toString()
        }

        private fun digitFor(c: Char): Char = when (c) {
            'a', 'b', 'c' -> '2'
            'd', 'e', 'f' -> '3'
            'g', 'h', 'i' -> '4'
            'j', 'k', 'l' -> '5'
            'm', 'n', 'o' -> '6'
            'p', 'q', 'r', 's' -> '7'
            't', 'u', 'v' -> '8'
            else -> '9' // w x y z (input is already limited to a-z)
        }

        fun toSequence(word: String): String {
            val chars = CharArray(word.length)
            for (i in word.indices) chars[i] = digitFor(word[i])
            return String(chars)
        }

        // JS "array index": canonical numeric string, value <= 2^32 - 2.
        private fun isArrayIndex(key: String): Boolean = when {
            key.length < 10 -> true
            key.length == 10 -> key.toLong() <= 4294967294L
            else -> false
        }

        // Priority list only (what the original app has before / without the big dictionary).
        fun buildPriorityOnly(): T9Engine = build(emptySequence())

        // Ranked build: priority list, then the common words (most common first), then the full dictionary
        // as a fallback so any word can still be an exact match.
        fun buildRanked(rankedWords: List<String>, dictionaryWords: Sequence<String>): T9Engine {
            val engine = T9Engine()
            engine.indexWords(PriorityWords.LIST.asSequence(), lowPriority = false)
            engine.indexWords(rankedWords.asSequence(), lowPriority = true)
            engine.indexWords(dictionaryWords, lowPriority = true)
            rankedWords.forEachIndexed { i, w ->
                val c = clean(w)
                if (c.length > 1 && !engine.rankIndex.containsKey(c)) {
                    engine.rankIndex[c] = i
                    engine.rankedList.add(c)
                }
            }
            engine.finish()
            return engine
        }

        // Priority list first, then the dictionary words (low priority), like the original boot sequence.
        fun build(dictionaryWords: Sequence<String>): T9Engine {
            val engine = T9Engine()
            engine.indexWords(PriorityWords.LIST.asSequence(), lowPriority = false)
            engine.indexWords(dictionaryWords, lowPriority = true)
            engine.finish()
            return engine
        }
    }
}
