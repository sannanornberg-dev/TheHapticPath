package com.hapticpath.app

data class SessionSummary(
    val durationSeconds: Long = 0L,
    val totalErrors: Int = 0,
    val errorSentences: List<String> = emptyList()
)