package com.example.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.audio.GiftAudioEngine
import com.example.data.audio.SoundSettingsStore
import com.example.data.audio.VoiceNoteHelper
import com.example.data.model.CategoryCatalog
import com.example.data.model.MiniGameType
import com.example.data.model.MusicCatalog
import com.example.data.model.Surprise
import com.example.data.util.ImageUtils
import com.example.ui.components.Base64ImageView
import com.example.ui.components.SoundSettingsDialog
import com.example.ui.theme.RoseBlush
import com.example.ui.theme.RosePrimary
import com.example.ui.theme.SurpriseThemes
import com.example.ui.viewmodel.EditorViewModel

@Composable
fun EditorStep3Photos(draft: Surprise, viewModel: EditorViewModel) {
    val context = LocalContext.current
    var pendingDeletePhotoIdx by remember { mutableStateOf<Int?>(null) }
    val photoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val base64 = ImageUtils.compressUriToBase64(context, uri)
            if (base64 != null) {
                viewModel.addPhotoBase64(base64)
            } else {
                viewModel.reportError("Could not process the selected photo. Please try a different image.")
            }
        }
    }

    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Photos & Polaroid Memories 📸", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Add up to 4 special photos using the Android Photo Picker. Recipients can zoom & browse them as Polaroid cards!",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Button(
                onClick = {
                    photoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                },
                enabled = draft.photos.size < 4,
                colors = ButtonDefaults.buttonColors(containerColor = RosePrimary),
                modifier = Modifier.fillMaxWidth().testTag("add_photo_button")
            ) {
                Icon(Icons.Default.AddPhotoAlternate, null, Modifier.size(18.dp))
                Spacer(Modifier.width(8.dp))
                Text(if (draft.photos.size < 4) "Pick a Photo (${draft.photos.size}/4)" else "4 Photos Added (Max)")
            }

            draft.photos.forEachIndexed { idx, base64 ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = RoseBlush,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Base64ImageView(
                                base64 = base64,
                                contentDescription = "Selected photo ${idx + 1}",
                                modifier = Modifier.size(72.dp).clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text("Memory #${idx + 1}", style = MaterialTheme.typography.labelLarge)
                                Row {
                                    if (idx > 0) {
                                        IconButton(onClick = { viewModel.movePhotoUp(idx) }) {
                                            Icon(Icons.Default.ArrowUpward, "Move up", Modifier.size(18.dp))
                                        }
                                    }
                                    IconButton(onClick = { pendingDeletePhotoIdx = idx }) {
                                        Icon(Icons.Default.Delete, "Remove photo", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(18.dp))
                                    }
                                }
                            }
                        }
                        OutlinedTextField(
                            value = draft.photoCaptions.getOrElse(idx) { "" },
                            onValueChange = { viewModel.updatePhotoCaption(idx, it) },
                            label = { Text("Polaroid Caption") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            }
        }
    }

    if (pendingDeletePhotoIdx != null) {
        val targetIdx = pendingDeletePhotoIdx!!
        AlertDialog(
            onDismissRequest = { pendingDeletePhotoIdx = null },
            title = { Text("Remove Photo?") },
            text = { Text("Remove Memory #${targetIdx + 1} from this surprise?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.removePhoto(targetIdx)
                        pendingDeletePhotoIdx = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { pendingDeletePhotoIdx = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun EditorStep4VoiceNote(draft: Surprise, viewModel: EditorViewModel) {
    val context = LocalContext.current
    val voiceHelper = remember { VoiceNoteHelper(context) }
    var isRecording by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }
    var micStatus by remember { mutableStateOf<String?>(null) }
    var showDeleteVoiceDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        onDispose { voiceHelper.release() }
    }

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            if (voiceHelper.startRecording()) {
                isRecording = true
                micStatus = "Recording your voice note... Tap Stop when ready!"
            } else {
                micStatus = "Microphone unavailable on this device."
            }
        } else {
            micStatus = "Microphone permission denied. You can still type a voice transcript note below."
        }
    }

    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Voice Message 🎙️", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Record a short warm voice greeting (up to 45s) or attach a sweet spoken note transcript.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                Button(
                    onClick = {
                        if (isRecording) {
                            val (b64, dur) = voiceHelper.stopRecording()
                            isRecording = false
                            if (b64 != null) {
                                viewModel.updateDraft { it.copy(voiceNoteAudioBase64 = b64, voiceDurationSec = dur) }
                                micStatus = "Recorded ${dur}s voice note!"
                            } else {
                                micStatus = "Recording was too short or failed. Please try again."
                            }
                        } else {
                            val hasPerm = ContextCompat.checkSelfPermission(
                                context, Manifest.permission.RECORD_AUDIO
                            ) == PackageManager.PERMISSION_GRANTED
                            if (hasPerm) {
                                if (voiceHelper.startRecording()) {
                                    isRecording = true
                                    micStatus = "Recording..."
                                } else {
                                    micStatus = "Microphone unavailable."
                                }
                            } else {
                                micPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isRecording) MaterialTheme.colorScheme.error else RosePrimary
                    ),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(if (isRecording) Icons.Default.Stop else Icons.Default.Mic, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(if (isRecording) "Stop" else "Record Voice")
                }

                if (!draft.voiceNoteAudioBase64.isNullOrBlank()) {
                    FilledTonalButton(onClick = {
                        if (isPlaying) {
                            voiceHelper.stopPlayback()
                            isPlaying = false
                        } else {
                            isPlaying = true
                            voiceHelper.playBase64Audio(draft.voiceNoteAudioBase64) { isPlaying = false }
                        }
                    }) {
                        Icon(if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow, null)
                        Spacer(Modifier.width(4.dp))
                        Text(if (isPlaying) "Stop" else "${draft.voiceDurationSec}s")
                    }
                    IconButton(onClick = { showDeleteVoiceDialog = true }) {
                        Icon(Icons.Default.Delete, "Remove voice note", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            if (micStatus != null) {
                Text(micStatus!!, style = MaterialTheme.typography.bodySmall, color = RosePrimary)
            }

            OutlinedTextField(
                value = draft.voiceNoteTranscript ?: "",
                onValueChange = { viewModel.updateDraft { d -> d.copy(voiceNoteTranscript = it.take(500)) } },
                label = { Text("Voice Note Caption / Spoken Quote (Optional)") },
                supportingText = { Text("${(draft.voiceNoteTranscript ?: "").length}/500") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (showDeleteVoiceDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteVoiceDialog = false },
            title = { Text("Remove Voice Note?") },
            text = { Text("Delete the recorded voice note from this surprise?") },
            confirmButton = {
                Button(
                    onClick = {
                        voiceHelper.stopPlayback()
                        isPlaying = false
                        viewModel.updateDraft { it.copy(voiceNoteAudioBase64 = null, voiceDurationSec = 0L) }
                        micStatus = null
                        showDeleteVoiceDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Remove")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteVoiceDialog = false }) { Text("Cancel") }
            }
        )
    }
}

@Composable
fun EditorStep5Music(draft: Surprise, viewModel: EditorViewModel) {
    val context = LocalContext.current
    val audioEngine = remember { GiftAudioEngine() }
    val soundSettings by SoundSettingsStore.settings.collectAsStateWithLifecycle()
    var previewingId by remember { mutableStateOf<String?>(null) }
    var showSoundDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        SoundSettingsStore.loadIfNeeded(context)
    }

    LaunchedEffect(soundSettings) {
        audioEngine.applySettings(soundSettings)
    }

    DisposableEffect(Unit) {
        onDispose { audioEngine.release() }
    }

    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Background Music 🎵", style = MaterialTheme.typography.titleLarge)
                IconButton(onClick = { showSoundDialog = true }) {
                    Icon(Icons.Default.Tune, contentDescription = "Global sound settings", tint = RosePrimary)
                }
            }
            Text(
                text = "Plays softly only after the recipient taps \"Let's Begin\".",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            MusicCatalog.tracks.forEach { track ->
                val selected = draft.musicTrackId == track.id
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (selected) RoseBlush else Color(0xFFF9F9FB),
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, if (selected) RosePrimary else Color.Transparent, RoundedCornerShape(16.dp))
                        .clickable { viewModel.updateDraft { it.copy(musicTrackId = track.id) } }
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Default.MusicNote, null, tint = if (selected) RosePrimary else Color.Gray)
                            Spacer(Modifier.width(10.dp))
                            Column {
                                Text(track.title, style = MaterialTheme.typography.titleSmall)
                                Text(track.mood, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        if (track.notesHz.isNotEmpty()) {
                            IconButton(onClick = {
                                if (previewingId == track.id) {
                                    audioEngine.stopMusic()
                                    previewingId = null
                                } else {
                                    audioEngine.applySettings(soundSettings)
                                    audioEngine.startMusic(track.id, draft.musicVolume.toFloat())
                                    previewingId = track.id
                                }
                            }) {
                                Icon(
                                    imageVector = if (previewingId == track.id) Icons.Default.Stop else Icons.Default.PlayArrow,
                                    contentDescription = "Preview track",
                                    tint = RosePrimary
                                )
                            }
                        }
                    }
                }
            }

            Spacer(Modifier.height(4.dp))
            Text("Gift Track Volume: ${(draft.musicVolume * 100).toInt()}%", style = MaterialTheme.typography.labelLarge)
            Slider(
                value = draft.musicVolume.toFloat(),
                onValueChange = { vol ->
                    viewModel.updateDraft { it.copy(musicVolume = vol.toDouble()) }
                    audioEngine.setVolume(vol)
                }
            )
        }
    }

    if (showSoundDialog) {
        SoundSettingsDialog(
            audioEngine = audioEngine,
            onDismiss = { showSoundDialog = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorStep6VisualStyle(draft: Surprise, viewModel: EditorViewModel) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Theme Palette & Animations ✨", style = MaterialTheme.typography.titleLarge)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SurpriseThemes.take(3).forEach { t ->
                    ThemeSwatchChip(t.id, t.name, t.accent, draft.themeId == t.id, Modifier.weight(1f)) {
                        viewModel.updateDraft { it.copy(themeId = t.id) }
                    }
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SurpriseThemes.drop(3).forEach { t ->
                    ThemeSwatchChip(t.id, t.name, t.accent, draft.themeId == t.id, Modifier.weight(1f)) {
                        viewModel.updateDraft { it.copy(themeId = t.id) }
                    }
                }
            }

            Text("Floating Ambient Effect", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(
                    "hearts" to "❤️ Hearts",
                    "sparkles" to "✨ Sparkles",
                    "petals" to "🌸 Petals",
                    "confetti" to "🎉 Confetti",
                    "stars" to "⭐ Stars"
                ).forEach { (id, label) ->
                    FilterChip(
                        selected = draft.animationEffect == id,
                        onClick = { viewModel.updateDraft { it.copy(animationEffect = id) } },
                        label = { Text(label) }
                    )
                }
            }

            Text("Gift Box Design", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("classic_rose" to "Rose Ribbon", "lavender_silk" to "Lavender Silk", "golden_royal" to "Golden Royal").forEach { (id, label) ->
                    FilterChip(
                        selected = draft.giftBoxStyle == id,
                        onClick = { viewModel.updateDraft { it.copy(giftBoxStyle = id) } },
                        label = { Text(label) }
                    )
                }
            }

            Text("Envelope Design", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("wax_seal" to "Wax Seal ❤️", "ribbon_fold" to "Ribbon 🎀", "starry_mail" to "Starry ✨").forEach { (id, label) ->
                    FilterChip(
                        selected = draft.envelopeStyle == id,
                        onClick = { viewModel.updateDraft { it.copy(envelopeStyle = id) } },
                        label = { Text(label) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeSwatchChip(
    id: String,
    name: String,
    color: Color,
    selected: Boolean,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = if (selected) color.copy(alpha = 0.16f) else Color(0xFFF9F9FB),
        modifier = modifier
            .border(1.5.dp, if (selected) color else Color.Transparent, RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                if (selected) Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
            }
            Spacer(Modifier.height(4.dp))
            Text(name, style = MaterialTheme.typography.labelSmall, maxLines = 1)
        }
    }
}

@Composable
fun EditorStep7Games(draft: Surprise, viewModel: EditorViewModel) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("Mini-Game Challenges 🎮", style = MaterialTheme.typography.titleLarge)
            Text(
                text = "Select one or more interactive games the recipient plays to unlock the gift box.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            MiniGameType.entries.forEach { game ->
                val checked = draft.selectedGames.contains(game.id)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (checked) RoseBlush else Color(0xFFF9F9FB),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { viewModel.toggleGame(game.id) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = checked, onCheckedChange = { viewModel.toggleGame(game.id) })
                        Spacer(Modifier.width(6.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("${game.emoji} ${game.title}", style = MaterialTheme.typography.titleSmall)
                            Text(game.shortDesc, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            if (draft.selectedGames.contains(MiniGameType.GUESS_SECRET_CODE.id)) {
                OutlinedTextField(
                    value = draft.secretCodeClue,
                    onValueChange = { viewModel.updateDraft { d -> d.copy(secretCodeClue = it.take(200)) } },
                    label = { Text("Secret Code Clue") },
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = draft.secretCodeAnswer,
                    onValueChange = { viewModel.updateDraft { d -> d.copy(secretCodeAnswer = it.take(80)) } },
                    label = { Text("Expected Answer") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Text("Game Difficulty", style = MaterialTheme.typography.titleSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("easy" to "Easy", "medium" to "Medium", "hard" to "Hard").forEach { (id, label) ->
                    FilterChip(
                        selected = draft.gameDifficulty == id,
                        onClick = { viewModel.updateDraft { it.copy(gameDifficulty = id) } },
                        label = { Text(label) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Allow recipient to skip games", style = MaterialTheme.typography.bodyMedium)
                Switch(
                    checked = draft.allowSkip,
                    onCheckedChange = { allow -> viewModel.updateDraft { it.copy(allowSkip = allow, gamesMandatory = !allow) } }
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun EditorStep8Publish(
    draft: Surprise,
    isSaving: Boolean,
    viewModel: EditorViewModel,
    onPreview: () -> Unit
) {
    val category = CategoryCatalog.find(draft.categoryId)
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Privacy, Extras & Publish 🚀", style = MaterialTheme.typography.titleLarge)

            Surface(shape = RoundedCornerShape(16.dp), color = RoseBlush, modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("${category.emoji} ${draft.title}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text("For ${draft.recipientName.ifBlank { "Recipient" }} • From ${draft.senderName}", style = MaterialTheme.typography.bodySmall)
                    Text("Games: ${draft.selectedGames.size} • Photos: ${draft.photos.size} • Music: ${MusicCatalog.find(draft.musicTrackId).title}", style = MaterialTheme.typography.labelSmall, color = RosePrimary)
                }
            }

            OutlinedTextField(
                value = draft.secretPasscode,
                onValueChange = { viewModel.updateDraft { d -> d.copy(secretPasscode = it.take(40)) } },
                label = { Text("Optional Gift Passcode (Leave blank for open access)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )

            Text("Time Capsule Unlock", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = draft.unlockAtMillis == 0L,
                    onClick = { viewModel.updateDraft { it.copy(unlockAtMillis = 0L) } },
                    label = { Text("Unlock Now") }
                )
                FilterChip(
                    selected = draft.unlockAtMillis > 0L,
                    onClick = { viewModel.updateDraft { it.copy(unlockAtMillis = System.currentTimeMillis() + 86_400_000L) } },
                    label = { Text("In 24 Hours") }
                )
            }

            Text("Special Day Countdown Banner", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = draft.eventDateMillis == 0L,
                    onClick = { viewModel.updateDraft { it.copy(eventDateMillis = 0L) } },
                    label = { Text("None") }
                )
                FilterChip(
                    selected = draft.eventDateMillis > 0L,
                    onClick = { viewModel.updateDraft { it.copy(eventDateMillis = System.currentTimeMillis() + 3L * 86_400_000L) } },
                    label = { Text("3-Day Countdown") }
                )
            }

            Text("Link Expiration", style = MaterialTheme.typography.titleSmall)
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = draft.expiresAtMillis == 0L,
                    onClick = { viewModel.updateDraft { it.copy(expiresAtMillis = 0L) } },
                    label = { Text("Never Expires") }
                )
                FilterChip(
                    selected = draft.expiresAtMillis > 0L,
                    onClick = { viewModel.updateDraft { it.copy(expiresAtMillis = System.currentTimeMillis() + 30L * 86_400_000L) } },
                    label = { Text("30 Days") }
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedButton(
                    onClick = { viewModel.saveSurprise(publishNow = false) },
                    enabled = !isSaving,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Save, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save Draft")
                }
                FilledTonalButton(
                    onClick = onPreview,
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Visibility, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Preview Gift")
                }
            }
        }
    }
}
