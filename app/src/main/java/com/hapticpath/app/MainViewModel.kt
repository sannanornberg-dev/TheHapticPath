package com.hapticpath.app

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed interface CurrentScreen {
    data object Session : CurrentScreen
    data object Summary : CurrentScreen
}

sealed interface SessionState {
    data object Idle : SessionState
    data object Recording : SessionState
    data class Error(val message: String) : SessionState
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // --- Services & Helpers ---
    private val downloader = ModelDownloader(application.applicationContext)
    private val rulePreferences by lazy { RulePreferences(getApplication()) }
    private val hapticManager by lazy {
        HapticFeedbackManager(getApplication<Application>().applicationContext)
    }
    private val ruleEngine by lazy { RuleEngine() }
    private var audioRecorder: AudioRecorder? = null

    // --- Navigering & UI-tillstånd ---
    private val _currentScreen = MutableStateFlow<CurrentScreen>(CurrentScreen.Session)
    val currentScreen: StateFlow<CurrentScreen> = _currentScreen.asStateFlow()

    private val _downloadState = MutableStateFlow<DownloadState>(DownloadState.Checking)
    val downloadState: StateFlow<DownloadState> = _downloadState.asStateFlow()

    private val _sessionState = MutableStateFlow<SessionState>(SessionState.Idle)
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    // --- Audio & Transkribering ---
    private val _rmsVolume = MutableStateFlow(0f)
    val rmsVolume: StateFlow<Float> = _rmsVolume.asStateFlow()

    private val _annotatedWords = MutableStateFlow<List<AnnotatedWord>>(emptyList())
    val annotatedWords: StateFlow<List<AnnotatedWord>> = _annotatedWords.asStateFlow()

    private val _errorCount = MutableStateFlow(0)
    val errorCount: StateFlow<Int> = _errorCount.asStateFlow()

    // --- Inställningar ---
    private val _biffRuleState = MutableStateFlow(true)
    val biffRuleState: StateFlow<Boolean> = _biffRuleState.asStateFlow()

    // --- Sessionssammanfattning ---
    private val _sessionSummary = MutableStateFlow(SessionSummary())
    val sessionSummary: StateFlow<SessionSummary> = _sessionSummary.asStateFlow()

    private val errorSentencesList = mutableListOf<String>()
    private var sessionStartTimeMs: Long = 0L

    init {
        initModelCheck()
        _biffRuleState.value = rulePreferences.isBiffRuleEnabled
    }

    // --- Inställningslogik ---
    fun setBiffRuleEnabled(enabled: Boolean) {
        rulePreferences.isBiffRuleEnabled = enabled
        _biffRuleState.value = enabled
    }

    fun triggerTestHaptic() {
        hapticManager.triggerPatternInterruption()
    }

    // --- Modellnedladdning (Moment 3.1 - Hårdvaruanpassad) ---
    fun initModelCheck() {
        viewModelScope.launch {
            _downloadState.value = DownloadState.Checking

            val isAlreadyDownloaded = withContext(Dispatchers.IO) {
                downloader.isModelDownloaded()
            }

            if (isAlreadyDownloaded) {
                _downloadState.value = DownloadState.Completed
            } else {
                val success = withContext(Dispatchers.IO) {
                    downloader.downloadModel { progress, downloadedMb, totalMb, modelName ->
                        _downloadState.value = DownloadState.Downloading(
                            progress = progress,
                            downloadedMb = downloadedMb,
                            totalMb = totalMb,
                            modelName = modelName
                        )
                    }
                }

                if (success) {
                    _downloadState.value = DownloadState.Completed
                } else {
                    _downloadState.value = DownloadState.Error(
                        "Kunde inte ladda ner modellfilen. Kontrollera anslutningen."
                    )
                }
            }
        }
    }

    // --- Sessionskontroll & AudioRecorder ---
    fun startSession() {
        if (_sessionState.value is SessionState.Recording) return

        viewModelScope.launch(Dispatchers.Main) {
            try {
                // Nollställ data för ny session
                _annotatedWords.value = emptyList()
                _errorCount.value = 0
                errorSentencesList.clear()
                sessionStartTimeMs = System.currentTimeMillis()

                _sessionState.value = SessionState.Recording
                _currentScreen.value = CurrentScreen.Session

                audioRecorder = AudioRecorder()
                audioRecorder?.startRecording(
                    onBufferReady = { _ ->
                        // WhisperEngine C++ inferens (Kopplas i Moment 3.6)
                    },
                    onRmsUpdated = { rms ->
                        // Tvinga uppdateringen till Dispatchers.Main så Compose UI-loopen fångar den direkt
                        viewModelScope.launch(Dispatchers.Main) {
                            _rmsVolume.value = rms
                        }
                    }
                )
            } catch (e: Exception) {
                _sessionState.value = SessionState.Error("Inspelning misslyckades: ${e.localizedMessage}")
                stopSession()
            }
        }
    }

    fun stopSession() {
        viewModelScope.launch(Dispatchers.Main) {
            try {
                audioRecorder?.stopRecording()
                audioRecorder = null
                _sessionState.value = SessionState.Idle
                _rmsVolume.value = 0f

                // Beräkna träningstid & sammanställ data för skärm 4
                val durationSec = if (sessionStartTimeMs > 0L) {
                    (System.currentTimeMillis() - sessionStartTimeMs) / 1000
                } else 0L

                _sessionSummary.value = SessionSummary(
                    durationSeconds = durationSec,
                    totalErrors = _errorCount.value,
                    errorSentences = errorSentencesList.toList()
                )

                // Växla vy till sammankopplad sammanfattningsskärm
                _currentScreen.value = CurrentScreen.Summary

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // --- Regelanalys & Transkriberingshantering ---
    fun processTranscribedText(fullSentence: String) {
        if (fullSentence.isBlank()) return

        Log.d("MainViewModel", "processTranscribedText indata: '$fullSentence'")

        viewModelScope.launch(Dispatchers.Default) {
            val rawWords = fullSentence.trim().split("\\s+".toRegex())

            val result = if (_biffRuleState.value) {
                ruleEngine.analyze(fullSentence)
            } else {
                RuleEngine.RuleAnalysisResult(hasError = false, matchedText = "")
            }

            val triggerTime = System.currentTimeMillis()

            if (result.hasError) {
                Log.w("MainViewModel", "Regelfel identifierat: '${result.matchedText}'. Triggar haptik.")
                hapticManager.triggerPatternInterruption()
                _errorCount.value += 1
                synchronized(errorSentencesList) {
                    errorSentencesList.add(fullSentence)
                }
            }

            val matchedTokens = if (result.hasError && result.matchedText.isNotBlank()) {
                result.matchedText.lowercase()
                    .split("\\s+".toRegex())
                    .map { cleanWordString(it) }
                    .filter { it.isNotBlank() }
            } else {
                emptyList()
            }

            val newWords = rawWords.map { word ->
                val cleanWord = cleanWordString(word)
                val isErrorWord = result.hasError && matchedTokens.contains(cleanWord)

                AnnotatedWord(
                    text = word,
                    isError = isErrorWord,
                    timestampMs = triggerTime
                )
            }

            withContext(Dispatchers.Main) {
                _annotatedWords.value = _annotatedWords.value + newWords
            }
        }
    }

    private fun cleanWordString(input: String): String {
        return input.lowercase().replace(Regex("[^a-zåäö]"), "")
    }

    fun testInjectSentence(sentence: String) {
        processTranscribedText(sentence)
    }

    override fun onCleared() {
        super.onCleared()
        stopSession()
    }
}