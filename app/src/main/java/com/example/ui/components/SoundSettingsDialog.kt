package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.audio.GiftAudioEngine
import com.example.data.audio.SoundSettingsStore
import kotlinx.coroutines.launch

@Composable
fun SoundSettingsDialog(
    audioEngine: GiftAudioEngine,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val settings by SoundSettingsStore.settings.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        SoundSettingsStore.loadIfNeeded(context)
    }

    LaunchedEffect(settings) {
        audioEngine.applySettings(settings)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (settings.mutedAll) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text("Sound & Music Settings")
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Mute All Audio", style = MaterialTheme.typography.titleSmall)
                    Switch(
                        checked = settings.mutedAll,
                        onCheckedChange = { muted ->
                            scope.launch {
                                SoundSettingsStore.update(context) { it.copy(mutedAll = muted) }
                            }
                        }
                    )
                }

                Column {
                    Text(
                        text = "Master Volume: ${(settings.masterVolume * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = settings.masterVolume,
                        enabled = !settings.mutedAll,
                        onValueChange = { v ->
                            scope.launch { SoundSettingsStore.update(context) { it.copy(masterVolume = v) } }
                        }
                    )
                }

                Column {
                    Text(
                        text = "Background Music: ${(settings.musicVolume * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = settings.musicVolume,
                        enabled = !settings.mutedAll,
                        onValueChange = { v ->
                            scope.launch { SoundSettingsStore.update(context) { it.copy(musicVolume = v) } }
                        }
                    )
                }

                Column {
                    Text(
                        text = "Sound Effects: ${(settings.sfxVolume * 100).toInt()}%",
                        style = MaterialTheme.typography.labelLarge
                    )
                    Slider(
                        value = settings.sfxVolume,
                        enabled = !settings.mutedAll,
                        onValueChange = { v ->
                            scope.launch { SoundSettingsStore.update(context) { it.copy(sfxVolume = v) } }
                        }
                    )
                }

                FilledTonalButton(
                    onClick = {
                        audioEngine.applySettings(settings)
                        audioEngine.playSparkleChime()
                    },
                    enabled = !settings.mutedAll,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.MusicNote, null)
                    Spacer(Modifier.width(6.dp))
                    Text("Preview Sound")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
