package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppNotification
import com.example.data.model.Surprise
import com.example.data.model.SurpriseReply
import com.example.data.model.UserProfile
import com.example.data.repository.GiftRepository
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

class DashboardViewModel(
    private val repository: GiftRepository,
    private val currentUserId: String
) : ViewModel() {

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { error ->
            val fsErr = GiftRepository.unwrapFirestoreError(error)
            if (fsErr == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing profile", fsErr ?: error)
            emit(null)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), null)

    val surprisesState: StateFlow<UiState<List<Surprise>>> = repository.observeMySurprises(currentUserId)
        .map<List<Surprise>, UiState<List<Surprise>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing surprises", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load surprises"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val repliesState: StateFlow<UiState<List<SurpriseReply>>> = repository.observeMyReplies(currentUserId)
        .map<List<SurpriseReply>, UiState<List<SurpriseReply>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing replies", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load replies"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    val notificationsState: StateFlow<UiState<List<AppNotification>>> = repository.observeMyNotifications(currentUserId)
        .map<List<AppNotification>, UiState<List<AppNotification>>> { UiState.Success(it) }
        .catch { error ->
            val firestoreError = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (firestoreError == null && error is CancellationException) throw error
            val rootError = firestoreError ?: error
            Log.w(TAG, "Error observing notifications", rootError)
            emit(UiState.Error(rootError.message ?: "Failed to load notifications"))
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000L), UiState.Loading)

    private val _toastMessage = MutableStateFlow<String?>(null)
    val toastMessage: StateFlow<String?> = _toastMessage.asStateFlow()

    fun clearToast() {
        _toastMessage.value = null
    }

    fun duplicateSurprise(surprise: Surprise) {
        viewModelScope.launch {
            repository.duplicateSurprise(surprise)
                .onSuccess { _toastMessage.value = "Duplicated as a new draft!" }
                .onFailure { _toastMessage.value = it.localizedMessage ?: "Failed to duplicate surprise" }
        }
    }

    fun deleteSurprise(surpriseId: String) {
        viewModelScope.launch {
            repository.deleteSurprise(surpriseId)
                .onSuccess { _toastMessage.value = "Surprise deleted." }
                .onFailure { _toastMessage.value = it.localizedMessage ?: "Failed to delete surprise" }
        }
    }

    fun updatePrivacy(
        surpriseId: String,
        passcode: String,
        unlockAtMillis: Long,
        expiresAtMillis: Long,
        status: String
    ) {
        viewModelScope.launch {
            repository.updateSurprisePrivacy(surpriseId, passcode, unlockAtMillis, expiresAtMillis, status)
                .onSuccess { _toastMessage.value = "Privacy & lock settings updated!" }
                .onFailure { _toastMessage.value = it.localizedMessage ?: "Failed to update privacy settings" }
        }
    }

    fun markReplyRead(replyId: String) {
        viewModelScope.launch {
            repository.markReplyRead(replyId)
        }
    }

    fun markNotificationRead(notificationId: String) {
        viewModelScope.launch {
            repository.markNotificationRead(notificationId)
        }
    }

    fun markAllNotificationsRead(notifications: List<AppNotification>) {
        viewModelScope.launch {
            repository.markAllNotificationsRead(notifications)
                .onSuccess { _toastMessage.value = "All notifications marked as read" }
        }
    }

    fun updateProfileSettings(
        displayName: String,
        notifyOnOpen: Boolean,
        notifyOnReply: Boolean,
        defaultAllowSkip: Boolean
    ) {
        viewModelScope.launch {
            repository.updateProfilePreferences(displayName, notifyOnOpen, notifyOnReply, defaultAllowSkip)
                .onSuccess { _toastMessage.value = "Profile & privacy settings saved!" }
                .onFailure { _toastMessage.value = it.localizedMessage ?: "Failed to save settings" }
        }
    }

    private companion object {
        const val TAG = "DashboardVM"
    }
}
