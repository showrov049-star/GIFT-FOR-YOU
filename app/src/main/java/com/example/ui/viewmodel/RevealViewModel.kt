package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.ReactionType
import com.example.data.model.Surprise
import com.example.data.repository.GiftRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

enum class RevealStage {
    LOADING,
    NOT_FOUND,
    PASSCODE_GATE,
    TIME_CAPSULE_LOCKED,
    EXPIRED,
    WELCOME,
    GAME_INTRO,
    PLAYING_GAME,
    GAME_SUCCESS,
    GIFT_BOX,
    ENVELOPE_LETTER,
    FINAL_EXPERIENCE
}

data class RevealUiState(
    val stage: RevealStage = RevealStage.LOADING,
    val surprise: Surprise? = null,
    val isPreviewMode: Boolean = false,
    val currentGameIndex: Int = 0,
    val passcodeError: String? = null,
    val replySending: Boolean = false,
    val replySent: Boolean = false,
    val errorMessage: String? = null
)

class RevealViewModel(
    private val repository: GiftRepository
) : ViewModel() {

    private val _state = MutableStateFlow(RevealUiState())
    val state: StateFlow<RevealUiState> = _state.asStateFlow()

    private val sessionId = "sess_" + UUID.randomUUID().toString().replace("-", "").take(10)
    private var openRecorded = false
    var lastRequestedCode: String? = null
        private set

    fun loadPublishedSurprise(surpriseIdOrCode: String) {
        lastRequestedCode = surpriseIdOrCode
        openRecorded = false
        viewModelScope.launch {
            _state.value = RevealUiState(stage = RevealStage.LOADING)
            repository.fetchPublishedSurprise(surpriseIdOrCode)
                .onSuccess { item ->
                    if (item == null) {
                        _state.value = RevealUiState(stage = RevealStage.NOT_FOUND)
                    } else {
                        evaluateGatesAndStart(item, isPreview = false)
                    }
                }
                .onFailure { err ->
                    _state.value = RevealUiState(
                        stage = RevealStage.NOT_FOUND,
                        errorMessage = err.localizedMessage ?: "Could not load surprise."
                    )
                }
        }
    }

    fun retryLoad() {
        val code = lastRequestedCode ?: return
        loadPublishedSurprise(code)
    }

    fun loadPreviewSurprise(surprise: Surprise) {
        lastRequestedCode = null
        openRecorded = true // Do not increment openCount during creator preview
        evaluateGatesAndStart(surprise, isPreview = true)
    }

    private fun evaluateGatesAndStart(surprise: Surprise, isPreview: Boolean) {
        val now = System.currentTimeMillis()
        val initialStage = when {
            !isPreview && surprise.expiresAtMillis > 0L && now > surprise.expiresAtMillis -> RevealStage.EXPIRED
            !isPreview && surprise.unlockAtMillis > 0L && now < surprise.unlockAtMillis -> RevealStage.TIME_CAPSULE_LOCKED
            !isPreview && surprise.secretPasscode.isNotBlank() -> RevealStage.PASSCODE_GATE
            else -> RevealStage.WELCOME
        }
        _state.value = RevealUiState(
            stage = initialStage,
            surprise = surprise,
            isPreviewMode = isPreview
        )
    }

    fun verifyPasscode(entered: String) {
        val surprise = _state.value.surprise ?: return
        if (entered.trim().equals(surprise.secretPasscode.trim(), ignoreCase = true)) {
            val now = System.currentTimeMillis()
            val next = if (surprise.unlockAtMillis > 0L && now < surprise.unlockAtMillis) {
                RevealStage.TIME_CAPSULE_LOCKED
            } else {
                RevealStage.WELCOME
            }
            _state.update { it.copy(stage = next, passcodeError = null) }
        } else {
            _state.update { it.copy(passcodeError = "That passcode didn't match. Try again!") }
        }
    }

    fun onBeginExperience() {
        val current = _state.value
        val surprise = current.surprise ?: return

        if (!current.isPreviewMode && !openRecorded) {
            openRecorded = true
            viewModelScope.launch {
                repository.recordSurpriseOpen(surprise, sessionId)
            }
        }

        val hasGames = surprise.selectedGames.isNotEmpty()
        _state.update {
            it.copy(
                stage = if (hasGames) RevealStage.GAME_INTRO else RevealStage.GIFT_BOX,
                currentGameIndex = 0
            )
        }
    }

    fun startGames() {
        _state.update { it.copy(stage = RevealStage.PLAYING_GAME) }
    }

    fun skipGamesToGift() {
        _state.update { it.copy(stage = RevealStage.GIFT_BOX) }
    }

    fun onSingleGameWon() {
        val current = _state.value
        val surprise = current.surprise ?: return
        val nextIndex = current.currentGameIndex + 1
        if (nextIndex < surprise.selectedGames.size) {
            _state.update { it.copy(currentGameIndex = nextIndex) }
        } else {
            if (!current.isPreviewMode) {
                viewModelScope.launch {
                    repository.recordGameCompleted(surprise, sessionId)
                }
            }
            _state.update { it.copy(stage = RevealStage.GAME_SUCCESS) }
        }
    }

    fun proceedToGiftBox() {
        _state.update { it.copy(stage = RevealStage.GIFT_BOX) }
    }

    fun proceedToEnvelope() {
        _state.update { it.copy(stage = RevealStage.ENVELOPE_LETTER) }
    }

    fun proceedToFinalExperience() {
        _state.update { it.copy(stage = RevealStage.FINAL_EXPERIENCE) }
    }

    fun sendReply(
        recipientName: String,
        reaction: ReactionType,
        message: String,
        emojiTag: String,
        photoBase64: String?
    ) {
        val current = _state.value
        val surprise = current.surprise ?: return
        if (current.isPreviewMode) {
            _state.update { it.copy(replySent = true) }
            return
        }
        viewModelScope.launch {
            _state.update { it.copy(replySending = true, errorMessage = null) }
            repository.submitRecipientReply(
                surprise = surprise,
                recipientName = recipientName,
                reaction = reaction,
                message = message,
                emojiTag = emojiTag,
                photoBase64 = photoBase64
            ).onSuccess {
                _state.update { it.copy(replySending = false, replySent = true) }
            }.onFailure { err ->
                _state.update {
                    it.copy(
                        replySending = false,
                        errorMessage = err.localizedMessage ?: "Failed to send reply."
                    )
                }
            }
        }
    }
}
