package com.hapticpath.app

data class AnnotatedWord(
    val text: String,
    val isError: Boolean = false,
    val timestampMs: Long = System.currentTimeMillis()
)