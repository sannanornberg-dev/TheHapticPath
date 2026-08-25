package com.hapticpath.app

sealed interface DownloadState {
    data object Checking : DownloadState
    data class Downloading(
        val progress: Float,
        val downloadedMb: Long,
        val totalMb: Long,
        val modelName: String
    ) : DownloadState
    data class Error(val message: String) : DownloadState
    data object Success : DownloadState
    data object Completed : DownloadState
}