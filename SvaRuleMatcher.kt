package com.hapticpath.app

class SvaRuleMatcher {

    private val timeAdverbs = listOf("idag", "igår", "imorgon", "nu", "sedan", "då", "ibland", "ofta")
    private val pronouns = listOf("jag", "du", "han", "hon", "den", "det", "vi", "ni", "de")

    fun hasWordOrderError(text: String): Boolean {
        val words = text.lowercase().trim().split("\\s+".toRegex())
        if (words.size < 3) return false

        for (i in 0 until words.size - 1) {
            val currentWord = words[i].replace("[^a-zåäö]".toRegex(), "")
            val nextWord = words[i + 1].replace("[^a-zåäö]".toRegex(), "")

            if (timeAdverbs.contains(currentWord) && pronouns.contains(nextWord)) {
                return true
            }
        }
        return false
    }
}
