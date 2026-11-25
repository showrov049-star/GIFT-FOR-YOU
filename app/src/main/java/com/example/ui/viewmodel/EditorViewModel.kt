package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.CategoryCatalog
import com.example.data.model.Surprise
import com.example.data.repository.GiftRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class EditorUiState(
    val currentStep: Int = 0,
    val draft: Surprise = Surprise(),
    val isSaving: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val autosaveStatus: String? = null,
    val publishedId: String? = null,
    val errorMessage: String? = null,
    val statusBanner: String? = null
)

class EditorViewModel(
    private val repository: GiftRepository,
    private val creatorUid: String,
    private val defaultSenderName: String
) : ViewModel() {

    private var baselineDraft: Surprise = Surprise(
        creatorUid = creatorUid,
        senderName = defaultSenderName.ifBlank { "Someone Special" },
        title = CategoryCatalog.all.first().defaultTitle,
        introMessage = CategoryCatalog.all.first().defaultIntro,
        mainMessage = CategoryCatalog.all.first().messageTemplate,
        selectedGames = CategoryCatalog.all.first().suggestedGames
    )

    private var autosaveJob: Job? = null

    private val _state = MutableStateFlow(EditorUiState(draft = baselineDraft))
    val state: StateFlow<EditorUiState> = _state.asStateFlow()

    fun startNewWithCategory(categoryId: String) {
        autosaveJob?.cancel()
        val cat = CategoryCatalog.find(categoryId)
        val initial = Surprise(
            creatorUid = creatorUid,
            categoryId = cat.id,
            title = cat.defaultTitle,
            senderName = defaultSenderName.ifBlank { "Someone Special" },
            introMessage = cat.defaultIntro,
            mainMessage = cat.messageTemplate,
            themeId = cat.defaultThemeId,
            animationEffect = cat.defaultAnimation,
            selectedGames = cat.suggestedGames
        )
        baselineDraft = initial
        _state.value = EditorUiState(
            currentStep = 0,
            draft = initial,
            hasUnsavedChanges = false,
            autosaveStatus = null
        )
    }

    fun startEditing(existing: Surprise) {
        autosaveJob?.cancel()
        baselineDraft = existing
        _state.value = EditorUiState(
            currentStep = 0,
            draft = existing,
            hasUnsavedChanges = false,
            autosaveStatus = null
        )
    }

    fun generateRandomSurprise() {
        val cat = CategoryCatalog.randomTemplate()
        _state.update { current ->
            val updated = current.draft.copy(
                categoryId = cat.id,
                title = cat.defaultTitle,
                introMessage = cat.defaultIntro,
                mainMessage = cat.messageTemplate,
                themeId = cat.defaultThemeId,
                animationEffect = cat.defaultAnimation,
                selectedGames = cat.suggestedGames
            )
            current.copy(
                draft = updated,
                hasUnsavedChanges = updated != baselineDraft,
                statusBanner = "Generated a ${cat.emoji} ${cat.name} template!"
            )
        }
        scheduleAutoSave()
    }

    fun setStep(step: Int) {
        _state.update { it.copy(currentStep = step.coerceIn(0, 7), errorMessage = null) }
    }

    fun updateDraft(transform: (Surprise) -> Surprise) {
        _state.update { current ->
            val updated = transform(current.draft)
            current.copy(
                draft = updated,
                hasUnsavedChanges = updated != baselineDraft,
                errorMessage = null
            )
        }
        scheduleAutoSave()
    }

    fun applyCategoryTemplate(categoryId: String, overwriteMessage: Boolean) {
        val cat = CategoryCatalog.find(categoryId)
        _state.update { current ->
            val updated = current.draft.copy(
                categoryId = cat.id,
                themeId = cat.defaultThemeId,
                animationEffect = cat.defaultAnimation,
                selectedGames = if (current.draft.selectedGames.isEmpty()) cat.suggestedGames else current.draft.selectedGames,
                title = if (overwriteMessage || current.draft.title.isBlank()) cat.defaultTitle else current.draft.title,
                introMessage = if (overwriteMessage || current.draft.introMessage.isBlank()) cat.defaultIntro else current.draft.introMessage,
                mainMessage = if (overwriteMessage || current.draft.mainMessage.isBlank()) cat.messageTemplate else current.draft.mainMessage
            )
            current.copy(
                draft = updated,
                hasUnsavedChanges = updated != baselineDraft
            )
        }
        scheduleAutoSave()
    }

    fun addPhotoBase64(base64: String, caption: String = "") {
        _state.update { current ->
            if (current.draft.photos.size >= 4) {
                current.copy(errorMessage = "Maximum 4 keepsake photos per surprise.")
            } else {
                val updated = current.draft.copy(
                    photos = current.draft.photos + base64,
                    photoCaptions = current.draft.photoCaptions + caption.ifBlank { "Sweet Memory #${current.draft.photos.size + 1}" }
                )
                current.copy(
                    draft = updated,
                    hasUnsavedChanges = updated != baselineDraft,
                    errorMessage = null
                )
            }
        }
        scheduleAutoSave()
    }

    fun updatePhotoCaption(index: Int, caption: String) {
        _state.update { current ->
            val captions = current.draft.photoCaptions.toMutableList()
            while (captions.size <= index) captions.add("")
            captions[index] = caption.take(200)
            val updated = current.draft.copy(photoCaptions = captions)
            current.copy(
                draft = updated,
                hasUnsavedChanges = updated != baselineDraft
            )
        }
        scheduleAutoSave()
    }

    fun movePhotoUp(index: Int) {
        if (index <= 0) return
        _state.update { current ->
            val photos = current.draft.photos.toMutableList()
            val captions = current.draft.photoCaptions.toMutableList()
            while (captions.size < photos.size) captions.add("")
            photos.swap(index, index - 1)
            captions.swap(index, index - 1)
            val updated = current.draft.copy(photos = photos, photoCaptions = captions)
            current.copy(
                draft = updated,
                hasUnsavedChanges = updated != baselineDraft
            )
        }
        scheduleAutoSave()
    }

    fun removePhoto(index: Int) {
        _state.update { current ->
            val photos = current.draft.photos.toMutableList()
            val captions = current.draft.photoCaptions.toMutableList()
            if (index in photos.indices) photos.removeAt(index)
            if (index in captions.indices) captions.removeAt(index)
            val updated = current.draft.copy(photos = photos, photoCaptions = captions)
            current.copy(
                draft = updated,
                hasUnsavedChanges = updated != baselineDraft
            )
        }
        scheduleAutoSave()
    }

    fun toggleGame(gameId: String) {
        _state.update { current ->
            val list = current.draft.selectedGames.toMutableList()
            if (list.contains(gameId)) {
                list.remove(gameId)
            } else if (list.size < 8) {
                list.add(gameId)
            }
            val updated = current.draft.copy(selectedGames = list)
            current.copy(
                draft = updated,
                hasUnsavedChanges = updated != baselineDraft
            )
        }
        scheduleAutoSave()
    }

    fun reportError(message: String) {
        _state.update { it.copy(errorMessage = message) }
    }

    private fun scheduleAutoSave() {
        autosaveJob?.cancel()
        autosaveJob = viewModelScope.launch {
            delay(2500L)
            val snapshot = _state.value
            val d = snapshot.draft
            if (
                !snapshot.isSaving &&
                snapshot.hasUnsavedChanges &&
                d.status == "draft" &&
                d.title.isNotBlank() &&
                d.recipientName.isNotBlank() &&
                d.mainMessage.isNotBlank()
            ) {
                _state.update { it.copy(autosaveStatus = "Saving draft...") }
                repository.saveSurprise(d, publishNow = false)
                    .onSuccess { savedId ->
                        val syncedSaved = d.copy(id = savedId, status = "draft")
                        baselineDraft = syncedSaved
                        _state.update { curr ->
                            val latestWithId = curr.draft.copy(id = savedId)
                            curr.copy(
                                draft = latestWithId,
                                hasUnsavedChanges = latestWithId != baselineDraft,
                                autosaveStatus = "Draft autosaved"
                            )
                        }
                    }
                    .onFailure {
                        _state.update { it.copy(autosaveStatus = "Autosave paused") }
                    }
            }
        }
    }

    fun saveSurprise(publishNow: Boolean, onSaved: (String) -> Unit = {}) {
        val draft = _state.value.draft
        if (draft.title.isBlank()) {
            _state.update { it.copy(errorMessage = "Please enter a Surprise Title in Step 1.") }
            return
        }
        if (draft.recipientName.isBlank()) {
            _state.update { it.copy(errorMessage = "Please enter the Recipient Name in Step 1.") }
            return
        }
        if (draft.mainMessage.isBlank()) {
            _state.update { it.copy(errorMessage = "Please write your main letter message in Step 2.") }
            return
        }

        autosaveJob?.cancel()
        viewModelScope.launch {
            _state.update { it.copy(isSaving = true, errorMessage = null, autosaveStatus = if (publishNow) null else "Saving...") }
            repository.saveSurprise(draft, publishNow)
                .onSuccess { savedId ->
                    val nextStatus = if (publishNow) {
                        if (draft.status == "draft" || draft.status.isBlank()) "published" else draft.status
                    } else {
                        "draft"
                    }
                    val savedSnapshot = draft.copy(id = savedId, status = nextStatus)
                    baselineDraft = savedSnapshot
                    _state.update { curr ->
                        val updatedLatest = curr.draft.copy(id = savedId, status = nextStatus)
                        curr.copy(
                            isSaving = false,
                            draft = updatedLatest,
                            hasUnsavedChanges = updatedLatest != baselineDraft,
                            autosaveStatus = if (publishNow) "Published" else "Saved as draft",
                            publishedId = if (publishNow) savedId else null,
                            statusBanner = if (publishNow) "Surprise published!" else "Saved as draft!"
                        )
                    }
                    onSaved(savedId)
                }
                .onFailure { err ->
                    _state.update {
                        it.copy(
                            isSaving = false,
                            autosaveStatus = null,
                            errorMessage = err.localizedMessage ?: "Failed to save surprise."
                        )
                    }
                }
        }
    }

    fun dismissPublishedDialog() {
        _state.update { it.copy(publishedId = null) }
    }

    fun clearBanner() {
        _state.update { it.copy(statusBanner = null, errorMessage = null) }
    }

    private fun <T> MutableList<T>.swap(i: Int, j: Int) {
        val tmp = this[i]
        this[i] = this[j]
        this[j] = tmp
    }
}
