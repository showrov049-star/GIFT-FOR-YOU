package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.data.model.AppNotification
import com.example.data.model.ReactionType
import com.example.data.model.Surprise
import com.example.data.model.SurpriseReply
import com.example.data.model.UserProfile
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import java.util.UUID

class GiftRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String {
        return auth.currentUser?.uid?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("User must be signed in with Google before accessing creator data.")
    }

    suspend fun ensureUserProfile(
        user: FirebaseUser,
        preferredDisplayName: String? = null
    ): Result<Unit> {
        val docPath = "users/${user.uid}"
        return try {
            val ref = db.collection("users").document(user.uid)
            val snap = ref.get().await()
            val customName = preferredDisplayName?.trim()?.takeIf { it.isNotBlank() }
            val safeName = (customName ?: user.displayName?.takeIf { it.isNotBlank() } ?: "Gift Creator").take(100)
            val safeEmail = (user.email ?: "").take(150)
            val safePhoto = user.photoUrl?.toString()?.take(500)
            if (!snap.exists()) {
                val payload = mapOf(
                    "uid" to user.uid,
                    "displayName" to safeName,
                    "email" to safeEmail,
                    "photoUrl" to safePhoto,
                    "notifyOnOpen" to true,
                    "notifyOnReply" to true,
                    "defaultAllowSkip" to true,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                ).filterValues { it != null }
                ref.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any>(
                    "email" to safeEmail,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                if (customName != null) {
                    updates["displayName"] = customName.take(100)
                } else {
                    val existingName = snap.getString("displayName")
                    if (existingName.isNullOrBlank()) {
                        updates["displayName"] = safeName
                    }
                }
                if (!safePhoto.isNullOrBlank()) {
                    updates["photoUrl"] = safePhoto
                }
                ref.update(updates).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val uid = requireUserId()
        if (uid != userId) throw IllegalStateException("Mismatched user profile request.")
        val path = "users/$uid"
        emitAll(
            db.collection("users").document(uid)
                .snapshots()
                .map { snap -> snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE) }
                .catch { error ->
                    val fsErr = unwrapFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: error, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun updateProfilePreferences(
        displayName: String,
        notifyOnOpen: Boolean,
        notifyOnReply: Boolean,
        defaultAllowSkip: Boolean
    ): Result<Unit> {
        var path = "users"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            val trimmed = displayName.trim().ifBlank { "Gift Creator" }.take(100)
            db.collection("users").document(uid).update(
                mapOf(
                    "displayName" to trimmed,
                    "notifyOnOpen" to notifyOnOpen,
                    "notifyOnReply" to notifyOnReply,
                    "defaultAllowSkip" to defaultAllowSkip,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    fun observeMySurprises(userId: String): Flow<List<Surprise>> = flow {
        val uid = requireUserId()
        if (uid != userId) throw IllegalStateException("Mismatched user ID.")
        val path = "surprises"
        emitAll(
            db.collection("surprises")
                .whereEqualTo("creatorUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(Surprise::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.updatedAt ?: it.createdAt ?: com.google.firebase.Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeMyReplies(userId: String): Flow<List<SurpriseReply>> = flow {
        val uid = requireUserId()
        if (uid != userId) throw IllegalStateException("Mismatched user ID.")
        val path = "replies"
        emitAll(
            db.collection("replies")
                .whereEqualTo("creatorUid", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(SurpriseReply::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: com.google.firebase.Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    fun observeMyNotifications(userId: String): Flow<List<AppNotification>> = flow {
        val uid = requireUserId()
        if (uid != userId) throw IllegalStateException("Mismatched user ID.")
        val path = "notifications"
        emitAll(
            db.collection("notifications")
                .whereEqualTo("userId", uid)
                .snapshots()
                .map { snap ->
                    snap.toObjects(AppNotification::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                        .sortedByDescending { it.createdAt ?: com.google.firebase.Timestamp(0, 0) }
                }
                .catch { error ->
                    val fsErr = unwrapFirestoreError(error)
                    if (fsErr == null && error is CancellationException) throw error
                    if (auth.currentUser?.uid == uid) {
                        handleFirestoreError(fsErr ?: error, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveSurprise(draft: Surprise, publishNow: Boolean): Result<String> {
        var docPath = "surprises"
        return try {
            val uid = requireUserId()
            val isNew = draft.id.isBlank()
            val surpriseId = if (isNew) generateSurpriseId() else draft.id
            docPath = "surprises/$surpriseId"
            val docRef = db.collection("surprises").document(surpriseId)

            val nextStatus = when {
                publishNow && draft.status == "draft" -> "published"
                publishNow && draft.status.isBlank() -> "published"
                publishNow -> draft.status.ifBlank { "published" }
                else -> "draft"
            }

            val safePhotos = draft.photos.take(4).map { it.take(215000) }
            val safeCaptions = draft.photoCaptions.take(4).map { it.take(200) }
            val safeGames = draft.selectedGames.take(8).map { it.take(40) }

            if (isNew) {
                val payload = mutableMapOf<String, Any?>(
                    "creatorUid" to uid,
                    "title" to draft.title.trim().ifBlank { "A Special Surprise" }.take(120),
                    "categoryId" to draft.categoryId.ifBlank { "birthday" }.take(50),
                    "recipientName" to draft.recipientName.trim().ifBlank { "Friend" }.take(80),
                    "senderName" to draft.senderName.trim().ifBlank { "Someone Special" }.take(80),
                    "introMessage" to draft.introMessage.trim().take(300),
                    "mainMessage" to draft.mainMessage.trim().ifBlank { "Wishing you happiness today and always!" }.take(5000),
                    "fontStyle" to draft.fontStyle,
                    "textAlignment" to draft.textAlignment,
                    "textColorHex" to draft.textColorHex.ifBlank { "#2D1623" }.take(12),
                    "boldBody" to draft.boldBody,
                    "italicBody" to draft.italicBody,
                    "photos" to safePhotos,
                    "photoCaptions" to safeCaptions,
                    "voiceNoteAudioBase64" to draft.voiceNoteAudioBase64?.takeIf { it.isNotBlank() }?.take(245000),
                    "voiceNoteTranscript" to draft.voiceNoteTranscript?.takeIf { it.isNotBlank() }?.take(500),
                    "voiceDurationSec" to draft.voiceDurationSec.coerceIn(0L, 120L),
                    "musicTrackId" to draft.musicTrackId.take(50),
                    "musicVolume" to draft.musicVolume.coerceIn(0.0, 1.0),
                    "themeId" to draft.themeId.ifBlank { "blush_rose" }.take(50),
                    "customBgColorHex" to draft.customBgColorHex.take(12),
                    "cardStyle" to draft.cardStyle,
                    "animationEffect" to draft.animationEffect,
                    "envelopeStyle" to draft.envelopeStyle,
                    "giftBoxStyle" to draft.giftBoxStyle,
                    "selectedGames" to safeGames,
                    "gameDifficulty" to draft.gameDifficulty,
                    "gamesMandatory" to draft.gamesMandatory,
                    "allowSkip" to draft.allowSkip,
                    "secretCodeClue" to draft.secretCodeClue.trim().take(200),
                    "secretCodeAnswer" to draft.secretCodeAnswer.trim().take(80),
                    "secretPasscode" to draft.secretPasscode.trim().take(40),
                    "unlockAtMillis" to draft.unlockAtMillis.coerceAtLeast(0L),
                    "eventDateMillis" to draft.eventDateMillis.coerceAtLeast(0L),
                    "expiresAtMillis" to draft.expiresAtMillis.coerceAtLeast(0L),
                    "status" to nextStatus,
                    "openCount" to 0L,
                    "replyCount" to 0L,
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "publishedAt" to if (nextStatus == "published") FieldValue.serverTimestamp() else null
                ).filterValues { it != null } as Map<String, Any>
                docRef.set(payload).await()
            } else {
                val updates = mutableMapOf<String, Any?>(
                    "title" to draft.title.trim().ifBlank { "A Special Surprise" }.take(120),
                    "categoryId" to draft.categoryId.ifBlank { "birthday" }.take(50),
                    "recipientName" to draft.recipientName.trim().ifBlank { "Friend" }.take(80),
                    "senderName" to draft.senderName.trim().ifBlank { "Someone Special" }.take(80),
                    "introMessage" to draft.introMessage.trim().take(300),
                    "mainMessage" to draft.mainMessage.trim().ifBlank { "Wishing you happiness today and always!" }.take(5000),
                    "fontStyle" to draft.fontStyle,
                    "textAlignment" to draft.textAlignment,
                    "textColorHex" to draft.textColorHex.ifBlank { "#2D1623" }.take(12),
                    "boldBody" to draft.boldBody,
                    "italicBody" to draft.italicBody,
                    "photos" to safePhotos,
                    "photoCaptions" to safeCaptions,
                    "voiceNoteAudioBase64" to draft.voiceNoteAudioBase64?.takeIf { it.isNotBlank() }?.take(245000),
                    "voiceNoteTranscript" to draft.voiceNoteTranscript?.takeIf { it.isNotBlank() }?.take(500),
                    "voiceDurationSec" to draft.voiceDurationSec.coerceIn(0L, 120L),
                    "musicTrackId" to draft.musicTrackId.take(50),
                    "musicVolume" to draft.musicVolume.coerceIn(0.0, 1.0),
                    "themeId" to draft.themeId.ifBlank { "blush_rose" }.take(50),
                    "customBgColorHex" to draft.customBgColorHex.take(12),
                    "cardStyle" to draft.cardStyle,
                    "animationEffect" to draft.animationEffect,
                    "envelopeStyle" to draft.envelopeStyle,
                    "giftBoxStyle" to draft.giftBoxStyle,
                    "selectedGames" to safeGames,
                    "gameDifficulty" to draft.gameDifficulty,
                    "gamesMandatory" to draft.gamesMandatory,
                    "allowSkip" to draft.allowSkip,
                    "secretCodeClue" to draft.secretCodeClue.trim().take(200),
                    "secretCodeAnswer" to draft.secretCodeAnswer.trim().take(80),
                    "secretPasscode" to draft.secretPasscode.trim().take(40),
                    "unlockAtMillis" to draft.unlockAtMillis.coerceAtLeast(0L),
                    "eventDateMillis" to draft.eventDateMillis.coerceAtLeast(0L),
                    "expiresAtMillis" to draft.expiresAtMillis.coerceAtLeast(0L),
                    "status" to nextStatus,
                    "updatedAt" to FieldValue.serverTimestamp(),
                    "publishedAt" to if (nextStatus == "published" && draft.publishedAt == null) FieldValue.serverTimestamp() else null
                ).filterValues { it != null } as Map<String, Any>
                docRef.update(updates).await()
            }
            Result.success(surpriseId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, docPath)
            Result.failure(e)
        }
    }

    suspend fun duplicateSurprise(source: Surprise): Result<String> {
        val copy = source.copy(
            id = "",
            title = "${source.title.take(105)} (Copy)",
            status = "draft",
            openCount = 0L,
            replyCount = 0L,
            createdAt = null,
            updatedAt = null,
            publishedAt = null
        )
        return saveSurprise(copy, publishNow = false)
    }

    suspend fun deleteSurprise(surpriseId: String): Result<Unit> {
        val path = "surprises/$surpriseId"
        return try {
            requireUserId()
            require(surpriseId.isNotBlank()) { "Surprise ID must not be blank." }
            db.collection("surprises").document(surpriseId).delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun updateSurprisePrivacy(
        surpriseId: String,
        secretPasscode: String,
        unlockAtMillis: Long,
        expiresAtMillis: Long,
        status: String
    ): Result<Unit> {
        val path = "surprises/$surpriseId"
        return try {
            requireUserId()
            db.collection("surprises").document(surpriseId).update(
                mapOf(
                    "secretPasscode" to secretPasscode.trim().take(40),
                    "unlockAtMillis" to unlockAtMillis.coerceAtLeast(0L),
                    "expiresAtMillis" to expiresAtMillis.coerceAtLeast(0L),
                    "status" to status,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markReplyRead(replyId: String): Result<Unit> {
        val path = "replies/$replyId"
        return try {
            requireUserId()
            db.collection("replies").document(replyId).update("readFlag", true).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markNotificationRead(notificationId: String): Result<Unit> {
        val path = "notifications/$notificationId"
        return try {
            requireUserId()
            db.collection("notifications").document(notificationId).update("readFlag", true).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun markAllNotificationsRead(notifications: List<AppNotification>): Result<Unit> {
        return try {
            requireUserId()
            val unread = notifications.filter { !it.readFlag }
            if (unread.isEmpty()) return Result.success(Unit)
            val batch = db.batch()
            unread.forEach { n ->
                batch.update(db.collection("notifications").document(n.id), "readFlag", true)
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, "notifications")
            Result.failure(e)
        }
    }

    // --- PUBLIC RECIPIENT FLOWS (No login required!) ---

    suspend fun fetchPublishedSurprise(rawCodeOrUrl: String): Result<Surprise?> {
        val cleanId = extractSurpriseId(rawCodeOrUrl)
        val path = "surprises/$cleanId"
        return try {
            require(cleanId.isNotBlank() && cleanId.matches(Regex("^[a-zA-Z0-9_\\-]+$"))) {
                "Please enter a valid surprise code or link."
            }
            val snap = db.collection("surprises").document(cleanId).get().await()
            if (!snap.exists()) {
                Result.success(null)
            } else {
                val item = snap.toObject(Surprise::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                Result.success(item)
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.GET, path)
            Result.failure(e)
        }
    }

    suspend fun recordSurpriseOpen(surprise: Surprise, sessionId: String): Result<Unit> {
        val path = "surprises/${surprise.id}"
        return try {
            val nextStatus = if (surprise.status == "replied") "replied" else "opened"
            val docRef = db.collection("surprises").document(surprise.id)
            docRef.update(
                mapOf(
                    "openCount" to surprise.openCount + 1L,
                    "status" to nextStatus,
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val evtId = "evt_${UUID.randomUUID().toString().replace("-", "").take(14)}"
            db.collection("surpriseEvents").document(evtId).set(
                mapOf(
                    "surpriseId" to surprise.id,
                    "creatorUid" to surprise.creatorUid,
                    "eventType" to "opened",
                    "anonymousSessionId" to sessionId.take(64),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val notifId = "ntf_${UUID.randomUUID().toString().replace("-", "").take(14)}"
            db.collection("notifications").document(notifId).set(
                mapOf(
                    "userId" to surprise.creatorUid,
                    "surpriseId" to surprise.id,
                    "type" to "opened",
                    "title" to "${surprise.recipientName} opened your surprise!",
                    "message" to "\"${surprise.title.take(50)}\" was just opened.",
                    "readFlag" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun recordGameCompleted(surprise: Surprise, sessionId: String): Result<Unit> {
        val evtId = "evt_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        return try {
            db.collection("surpriseEvents").document(evtId).set(
                mapOf(
                    "surpriseId" to surprise.id,
                    "creatorUid" to surprise.creatorUid,
                    "eventType" to "game_completed",
                    "anonymousSessionId" to sessionId.take(64),
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "surpriseEvents/$evtId")
            Result.failure(e)
        }
    }

    suspend fun submitRecipientReply(
        surprise: Surprise,
        recipientName: String,
        reaction: ReactionType,
        message: String,
        emojiTag: String,
        photoBase64: String?
    ): Result<Unit> {
        val replyId = "rep_${UUID.randomUUID().toString().replace("-", "").take(14)}"
        return try {
            val cleanMsg = message.trim().ifBlank { "${reaction.emoji} ${reaction.label}!" }.take(1500)
            val cleanName = recipientName.trim().ifBlank { surprise.recipientName }.take(80)

            val replyPayload = mutableMapOf<String, Any?>(
                "surpriseId" to surprise.id,
                "creatorUid" to surprise.creatorUid,
                "surpriseTitle" to surprise.title.take(120),
                "recipientName" to cleanName,
                "reaction" to reaction.id,
                "message" to cleanMsg,
                "emojiTag" to emojiTag.ifBlank { reaction.emoji }.take(20),
                "photoBase64" to photoBase64?.takeIf { it.isNotBlank() }?.take(215000),
                "readFlag" to false,
                "createdAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null } as Map<String, Any>

            db.collection("replies").document(replyId).set(replyPayload).await()

            db.collection("surprises").document(surprise.id).update(
                mapOf(
                    "replyCount" to surprise.replyCount + 1L,
                    "status" to "replied",
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()

            val notifId = "ntf_${UUID.randomUUID().toString().replace("-", "").take(14)}"
            db.collection("notifications").document(notifId).set(
                mapOf(
                    "userId" to surprise.creatorUid,
                    "surpriseId" to surprise.id,
                    "type" to "replied",
                    "title" to "${reaction.emoji} Reply from $cleanName!",
                    "message" to cleanMsg.take(140),
                    "readFlag" to false,
                    "createdAt" to FieldValue.serverTimestamp()
                )
            ).await()

            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, "replies/$replyId")
            Result.failure(e)
        }
    }

    companion object {
        fun generateSurpriseId(): String =
            "gfy_" + UUID.randomUUID().toString().replace("-", "").take(12)

        fun extractSurpriseId(input: String): String {
            val trimmed = input.trim()
            return when {
                trimmed.contains("/s/") -> trimmed.substringAfterLast("/s/").substringBefore("?").trim()
                trimmed.contains("giftforyou://s/") -> trimmed.substringAfterLast("giftforyou://s/").substringBefore("?").trim()
                else -> trimmed
            }
        }

        fun unwrapFirestoreError(error: Throwable): FirebaseFirestoreException? =
            generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
    }
}
