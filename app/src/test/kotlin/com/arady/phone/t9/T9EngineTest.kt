package com.arady.phone.t9

import java.io.File
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.BeforeClass
import org.junit.Test

// Compares the Kotlin engine with golden output produced by running the ORIGINAL JavaScript
// (getSmartT9Matches) against the full words_alpha.txt dictionary.
// golden.tsv columns: sequence, result count, md5 of the whole ranked list, first 25 results.
class T9EngineTest {

    companion object {
        lateinit var engine: T9Engine

        @JvmStatic
        @BeforeClass
        fun load() {
            val file = if (File("src/main/assets/words_alpha.txt").exists()) {
                File("src/main/assets/words_alpha.txt")
            } else {
                File("app/src/main/assets/words_alpha.txt")
            }
            engine = file.bufferedReader().useLines { lines -> T9Engine.build(lines.toList().asSequence()) }
        }

        private fun md5(text: String): String =
            MessageDigest.getInstance("MD5").digest(text.toByteArray())
                .joinToString("") { "%02x".format(it) }

        private fun tag(type: MatchType): Char = when (type) {
            MatchType.EXACT -> 'E'
            MatchType.SUGGEST -> 'S'
            MatchType.CORRECTION -> 'C'
        }
    }

    @Test
    fun matchesOriginalJavaScriptForEveryGoldenSequence() {
        val stream = javaClass.classLoader!!.getResourceAsStream("golden.tsv")!!
        val rows = stream.bufferedReader().readLines().filter { it.isNotBlank() }
        assertTrue(rows.size > 1000)

        val failures = ArrayList<String>()
        for (row in rows) {
            val cols = row.split('\t')
            val seq = cols[0]
            val expectedCount = cols[1].toInt()
            val expectedMd5 = cols[2]
            val expectedTop = if (cols.size > 3) cols[3] else ""

            val all = engine.matches(seq)
            val top = all.take(25).joinToString("|") { it.word + ":" + tag(it.type) }
            val fullHash = md5(all.joinToString(",") { it.word + ":" + it.type.name.lowercase() })

            if (all.size != expectedCount || top != expectedTop || fullHash != expectedMd5) {
                failures.add("seq='$seq' expected $expectedCount/$expectedTop got ${all.size}/$top")
            }
            // The limited call used by the UI must equal the head of the full list.
            val limited = engine.matches(seq, 6)
            assertEquals("limit mismatch for '$seq'", all.take(6), limited)
        }
        assertTrue(failures.take(10).joinToString("\n"), failures.isEmpty())
    }

    @Test
    fun priorityWordsWinTies() {
        assertEquals("phone", engine.matches("74663").first().word)
        assertEquals(listOf("cat", "act", "bat"), engine.matches("228").take(3).map { it.word })
    }

    @Test
    fun emptyAndSymbolOnlyInputsGiveNothing() {
        assertTrue(engine.matches("").isEmpty())
        assertTrue(engine.matches("*#").isEmpty())
        assertEquals("unknown", engine.topWord("0"))
    }

    @Test
    fun starAndHashAreIgnored() {
        assertEquals(engine.matches("23"), engine.matches("2*3#"))
    }
}
