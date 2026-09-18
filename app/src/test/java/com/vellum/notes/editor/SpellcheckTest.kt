package com.vellum.notes.editor

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SpellcheckTest {

    private val dict = setOf("hello", "world", "note", "notes", "handwriting", "ink", "paper", "write", "this", "is", "a", "test")

    @Test
    fun knownWords_pass() {
        assertTrue(Spellcheck.unknownWords("Hello world, this is a test!", dict).isEmpty())
    }

    @Test
    fun typos_flaggedOnce_casePreserved() {
        assertEquals(listOf("Helo", "wrold"), Spellcheck.unknownWords("Helo Helo wrold", dict))
    }

    @Test
    fun suggestions_ranked() {
        val s = Spellcheck.suggestions("helo", dict)
        assertTrue("hello first, got $s", s.firstOrNull() == "hello")
        assertTrue(Spellcheck.suggestions("hello", dict).isEmpty())
    }

    @Test
    fun levenshtein_capped() {
        assertEquals(1, Spellcheck.levenshtein("hello", "helo", 2))
        assertEquals(0, Spellcheck.levenshtein("ink", "ink", 2))
        assertTrue(Spellcheck.levenshtein("handwriting", "zzz", 2) > 2)
    }
}
