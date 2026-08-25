package com.hapticpath.app

import android.util.Log

class RuleEngine {

    data class RuleAnalysisResult(
        val hasError: Boolean,
        val matchedText: String = ""
    )

    // Regex för BIFF-regeln (Bisats: Inte Före Finita verbet)
    // Fångar mönster som "jag inte vet", "hon inte kan", "det inte finns"
    private val biffPattern = Regex(
        "(?i)\\b(jag|du|han|hon|den|det|vi|ni|de)\\s+inte\\s+([a-zåäö]+)\\b"
    )

    fun analyze(text: String): RuleAnalysisResult {
        if (text.isBlank()) return RuleAnalysisResult(hasError = false)

        val match = biffPattern.find(text)
        return if (match != null) {
            val matchedSubstring = match.value
            Log.d("RuleEngine", "BIFF-regel bruten: '$matchedSubstring'")
            RuleAnalysisResult(hasError = true, matchedText = matchedSubstring)
        } else {
            RuleAnalysisResult(hasError = false)
        }
    }
}