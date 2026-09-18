package com.vellum.notes.editor

import kotlin.math.min

object Spellcheck {

    fun wordsOf(text: String): List<String> =
        Regex("[A-Za-z']+").findAll(text)
            .map { it.value }
            .filter { it.any { c -> c.isLetter() } }
            .toList()

    fun unknownWords(text: String, dictionary: Set<String>): List<String> {
        val seen = LinkedHashSet<String>()
        for (w in wordsOf(text)) {
            val lower = w.lowercase()
            if (lower !in dictionary) seen += w
        }
        return seen.toList()
    }

    fun suggestions(word: String, dictionary: Set<String>, limit: Int = 3): List<String> {
        val lower = word.lowercase()
        if (lower in dictionary) return emptyList()
        val scored = ArrayList<Pair<Int, String>>()
        for (cand in dictionary) {
            if (cand.length < lower.length - 2 || cand.length > lower.length + 2) continue
            if (cand.isNotEmpty() && lower.isNotEmpty() && cand[0] != lower[0]) continue
            val d = levenshtein(lower, cand, 2)
            if (d <= 2) scored += d to cand
        }
        return scored.sortedWith(compareBy({ it.first }, { it.second }))
            .take(limit)
            .map { preserveCase(word, it.second) }
    }

    fun preserveCase(original: String, suggestion: String): String {
        if (original.isEmpty()) return suggestion
        if (original[0].isUpperCase()) {
            return suggestion.replaceFirstChar { it.uppercase() }
        }
        return suggestion
    }

    fun levenshtein(a: String, b: String, cap: Int): Int {
        if (a == b) return 0
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        var prev = IntArray(b.length + 1) { it }
        var curr = IntArray(b.length + 1)
        for (i in 1..a.length) {
            curr[0] = i
            var rowMin = curr[0]
            for (j in 1..b.length) {
                curr[j] = min(
                    min(prev[j] + 1, curr[j - 1] + 1),
                    prev[j - 1] + if (a[i - 1] == b[j - 1]) 0 else 1,
                )
                if (curr[j] < rowMin) rowMin = curr[j]
            }
            if (rowMin > cap) return cap + 1
            val tmp = prev
            prev = curr
            curr = tmp
        }
        return prev[b.length]
    }

    fun loadAssetLines(read: () -> String): Set<String> =
        read().lineSequence()
            .map { it.trim().lowercase() }
            .filter { it.isNotEmpty() }
            .toHashSet()
}
