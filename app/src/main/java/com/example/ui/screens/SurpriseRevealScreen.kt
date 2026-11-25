package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.AddPhotoAlternate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.Slideshow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.ZoomIn
import androidx.compose.material.icons.filled.ZoomOut
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.data.audio.GiftAudioEngine
import com.example.data.audio.SoundSettingsStore
import com.example.data.audio.VoiceNoteHelper
import com.example.data.model.CategoryCatalog
import com.example.data.model.MiniGameType
import com.example.data.model.ReactionType
import com.example.data.model.Surprise
import com.example.data.util.ImageUtils
import com.example.ui.components.Base64ImageView
import com.example.ui.components.ConfettiBurstOverlay
import com.example.ui.components.FloatingAmbientParticles
import com.example.ui.components.InteractiveEnvelopeView
import com.example.ui.components.InteractiveGiftBoxView
import com.example.ui.components.SoundSettingsDialog
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.PoppinsFontFamily
import com.example.ui.theme.SurpriseThemeSpec
import com.example.ui.theme.findThemeSpec
import com.example.ui.viewmodel.RevealStage
import com.example.ui.viewmodel.RevealViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun SurpriseRevealScreen(
    viewModel: RevealViewModel,
    onClose: () -> Unit
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val audioEngine = remember { GiftAudioEngine() }
    val voiceHelper = remember { VoiceNoteHelper(context) }
    val soundSettings by SoundSettingsStore.settings.collectAsStateWithLifecycle()
    var showSoundDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        SoundSettingsStore.loadIfNeeded(context)
    }

    LaunchedEffect(soundSettings) {
        audioEngine.applySettings(soundSettings)
    }

    BackHandler { onClose() }

    DisposableEffect(Unit) {
        onDispose {
            audioEngine.release()
            voiceHelper.release()
        }
    }

    val surprise = uiState.surprise
    val theme = if (surprise != null) {
        findThemeSpec(surprise.themeId, surprise.customBgColorHex)
    } else {
        findThemeSpec("blush_rose")
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(theme.bgTop, theme.bgBottom)))
    ) {
        FloatingAmbientParticles(
            effect = surprise?.animationEffect ?: "hearts",
            primaryColor = theme.accent,
            secondaryColor = theme.secondaryAccent
        )

        if (uiState.stage == RevealStage.GAME_SUCCESS || uiState.stage == RevealStage.GIFT_BOX) {
            ConfettiBurstOverlay(active = true)
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top bar with optional Preview banner & Sound controls
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onClose) {
                    Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = theme.textPrimary)
                }

                if (uiState.isPreviewMode) {
                    Surface(shape = RoundedCornerShape(50), color = theme.accent) {
                        Text(
                            text = "PREVIEW MODE",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp)
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { showSoundDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Sound settings",
                            tint = theme.accent
                        )
                    }
                    IconButton(onClick = {
                        scope.launch {
                            SoundSettingsStore.update(context) { it.copy(mutedAll = !it.mutedAll) }
                        }
                    }) {
                        Icon(
                            imageVector = if (soundSettings.mutedAll) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                            contentDescription = "Toggle audio mute",
                            tint = theme.accent
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(targetState = uiState.stage, label = "reveal_stage") { stage ->
                    when (stage) {
                        RevealStage.LOADING -> {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator(color = theme.accent)
                                Spacer(Modifier.height(12.dp))
                                Text("Preparing your surprise...", color = theme.textPrimary)
                            }
                        }

                        RevealStage.NOT_FOUND -> {
                            RevealStatusCard(
                                emoji = "💌",
                                title = "Surprise Not Found",
                                body = uiState.errorMessage ?: "Please double-check the Gift Code or make sure the surprise has been published.",
                                theme = theme,
                                actionText = "Go Back",
                                onAction = onClose,
                                secondaryActionText = if (viewModel.lastRequestedCode != null && !uiState.isPreviewMode) "Try Again" else null,
                                onSecondaryAction = if (viewModel.lastRequestedCode != null && !uiState.isPreviewMode) {
                                    { viewModel.retryLoad() }
                                } else null
                            )
                        }

                        RevealStage.EXPIRED -> {
                            RevealStatusCard(
                                emoji = "⏳",
                                title = "This Surprise Has Expired",
                                body = "The creator set an expiration time for this gift link.",
                                theme = theme,
                                actionText = "Close",
                                onAction = onClose
                            )
                        }

                        RevealStage.TIME_CAPSULE_LOCKED -> {
                            val dateStr = surprise?.unlockAtMillis?.let {
                                SimpleDateFormat("MMM d, yyyy 'at' h:mm a", Locale.getDefault()).format(Date(it))
                            } ?: "soon"
                            RevealStatusCard(
                                emoji = "🕰️",
                                title = "Time Capsule Sealed!",
                                body = "This surprise is scheduled to unlock on $dateStr.",
                                theme = theme,
                                actionText = "Okay",
                                onAction = onClose
                            )
                        }

                        RevealStage.PASSCODE_GATE -> {
                            PasscodeGateView(
                                recipientName = surprise?.recipientName ?: "Friend",
                                errorMsg = uiState.passcodeError,
                                theme = theme,
                                onSubmit = { viewModel.verifyPasscode(it) }
                            )
                        }

                        RevealStage.WELCOME -> {
                            val s = surprise!!
                            val cat = CategoryCatalog.find(s.categoryId)
                            Column(
                                modifier = Modifier
                                    .widthIn(max = 560.dp)
                                    .padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Surface(shape = RoundedCornerShape(50), color = theme.accent.copy(alpha = 0.14f)) {
                                    Text(
                                        text = "${cat.emoji} ${cat.name}",
                                        style = MaterialTheme.typography.labelLarge,
                                        color = theme.accent,
                                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                                    )
                                }
                                Spacer(Modifier.height(18.dp))
                                Image(
                                    painter = painterResource(id = R.drawable.img_hero_gift),
                                    contentDescription = "Gift welcome",
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(190.dp)
                                        .clip(RoundedCornerShape(24.dp)),
                                    contentScale = ContentScale.Crop
                                )
                                Spacer(Modifier.height(22.dp))
                                Text(
                                    text = "Hey ${s.recipientName}! Someone has prepared something special for you. ❤️",
                                    style = MaterialTheme.typography.displaySmall,
                                    color = theme.textPrimary,
                                    textAlign = TextAlign.Center
                                )
                                if (s.introMessage.isNotBlank()) {
                                    Spacer(Modifier.height(10.dp))
                                    Text(
                                        text = s.introMessage,
                                        style = MaterialTheme.typography.bodyLarge,
                                        color = theme.textMuted,
                                        textAlign = TextAlign.Center
                                    )
                                }
                                Spacer(Modifier.height(26.dp))
                                Button(
                                    onClick = {
                                        if (s.musicTrackId != "none") {
                                            audioEngine.startMusic(s.musicTrackId, s.musicVolume.toFloat())
                                        }
                                        audioEngine.playSparkleChime()
                                        viewModel.onBeginExperience()
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(54.dp)
                                        .testTag("begin_surprise_button")
                                ) {
                                    Text("Let's Begin ❤️", style = MaterialTheme.typography.titleMedium)
                                }
                            }
                        }

                        RevealStage.GAME_INTRO -> {
                            val s = surprise!!
                            val firstGame = MiniGameType.fromId(s.selectedGames.firstOrNull() ?: "catch_hearts")
                            RevealStatusCard(
                                emoji = firstGame.emoji,
                                title = "But first, a tiny challenge!",
                                body = "Complete ${s.selectedGames.size} playful mini-game${if (s.selectedGames.size > 1) "s" else ""} to unlock your surprise from ${s.senderName}.",
                                theme = theme,
                                actionText = "Play Now",
                                onAction = { viewModel.startGames() },
                                secondaryActionText = if (!s.gamesMandatory || s.allowSkip) "Skip to Gift" else null,
                                onSecondaryAction = { viewModel.skipGamesToGift() }
                            )
                        }

                        RevealStage.PLAYING_GAME -> {
                            val s = surprise!!
                            val gameId = s.selectedGames.getOrElse(uiState.currentGameIndex) { "catch_hearts" }
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState()),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                MiniGameArenaHost(
                                    gameType = MiniGameType.fromId(gameId),
                                    surprise = s,
                                    theme = theme,
                                    audioEngine = audioEngine,
                                    gameIndex = uiState.currentGameIndex,
                                    totalGames = s.selectedGames.size,
                                    onGameWon = { viewModel.onSingleGameWon() },
                                    onSkipGame = if (!s.gamesMandatory || s.allowSkip) {
                                        { viewModel.skipGamesToGift() }
                                    } else null
                                )
                            }
                        }

                        RevealStage.GAME_SUCCESS -> {
                            RevealStatusCard(
                                emoji = "🎉",
                                title = "Yay! You did it!",
                                body = "Your surprise is waiting for you inside the gift box!",
                                theme = theme,
                                actionText = "See My Gift Box 🎁",
                                onAction = { viewModel.proceedToGiftBox() }
                            )
                        }

                        RevealStage.GIFT_BOX -> {
                            val s = surprise!!
                            // 0 = idle floating, 1 = shaking, 2 = opened
                            var boxPhase by remember { mutableIntStateOf(0) }
                            LaunchedEffect(boxPhase) {
                                if (boxPhase == 1) {
                                    audioEngine.playBoxShake()
                                    delay(420L)
                                    boxPhase = 2
                                    audioEngine.playBoxOpen()
                                    delay(1450L)
                                    viewModel.proceedToEnvelope()
                                }
                            }
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = when (boxPhase) {
                                        2 -> "✨ Surprise Unlocked! ✨"
                                        1 -> "Opening Your Gift..."
                                        else -> "Tap the Gift Box to Open!"
                                    },
                                    style = MaterialTheme.typography.headlineMedium,
                                    color = theme.textPrimary,
                                    textAlign = TextAlign.Center
                                )
                                Spacer(Modifier.height(18.dp))
                                InteractiveGiftBoxView(
                                    giftBoxStyle = s.giftBoxStyle,
                                    isShaking = boxPhase == 1,
                                    isOpened = boxPhase == 2,
                                    onTapBox = { if (boxPhase == 0) boxPhase = 1 },
                                    modifier = Modifier.testTag("interactive_gift_box")
                                )
                                Spacer(Modifier.height(18.dp))
                                if (boxPhase == 0) {
                                    Button(
                                        onClick = { boxPhase = 1 },
                                        colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
                                    ) {
                                        Text("Unwrap Present 🎁")
                                    }
                                    Spacer(Modifier.height(4.dp))
                                    TextButton(onClick = { viewModel.proceedToEnvelope() }) {
                                        Icon(Icons.Default.SkipNext, null, Modifier.size(16.dp), tint = theme.textMuted)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Skip Animation", color = theme.textMuted)
                                    }
                                }
                            }
                        }

                        RevealStage.ENVELOPE_LETTER -> {
                            val s = surprise!!
                            var envOpened by remember { mutableStateOf(false) }
                            var typedLength by remember { mutableIntStateOf(0) }
                            var revealAll by remember { mutableStateOf(false) }

                            LaunchedEffect(envOpened, revealAll) {
                                if (envOpened) {
                                    if (typedLength == 0) audioEngine.playEnvelopeOpen()
                                    val fullText = s.mainMessage
                                    if (revealAll) {
                                        typedLength = fullText.length
                                    } else {
                                        for (i in (typedLength + 1)..fullText.length) {
                                            if (revealAll) {
                                                typedLength = fullText.length
                                                break
                                            }
                                            typedLength = i
                                            delay(16L)
                                        }
                                    }
                                }
                            }

                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                InteractiveEnvelopeView(
                                    envelopeStyle = s.envelopeStyle,
                                    recipientName = s.recipientName,
                                    title = s.title,
                                    isOpened = envOpened,
                                    onTapEnvelope = { envOpened = true },
                                    modifier = Modifier.testTag("interactive_envelope")
                                )

                                if (!envOpened) {
                                    Button(
                                        onClick = { envOpened = true },
                                        colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
                                    ) {
                                        Text("Open Sealed Letter 💌")
                                    }
                                    TextButton(onClick = { viewModel.proceedToFinalExperience() }) {
                                        Icon(Icons.Default.SkipNext, null, Modifier.size(16.dp), tint = theme.textMuted)
                                        Spacer(Modifier.width(4.dp))
                                        Text("Skip to Memories & Reply", color = theme.textMuted)
                                    }
                                } else {
                                    Card(
                                        shape = RoundedCornerShape(24.dp),
                                        colors = CardDefaults.cardColors(containerColor = theme.cardSurface),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .widthIn(max = 560.dp)
                                            .clickable { revealAll = true }
                                    ) {
                                        Column(modifier = Modifier.padding(22.dp)) {
                                            Text(
                                                text = "Dear ${s.recipientName},",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = theme.accent
                                            )
                                            Spacer(Modifier.height(10.dp))
                                            val fontFamily = when (s.fontStyle) {
                                                "poppins" -> PoppinsFontFamily
                                                "inter" -> InterFontFamily
                                                else -> PlayfairDisplayFontFamily
                                            }
                                            val align = when (s.textAlignment) {
                                                "start" -> TextAlign.Start
                                                "end" -> TextAlign.End
                                                else -> TextAlign.Center
                                            }
                                            Text(
                                                text = s.mainMessage.take(typedLength),
                                                style = MaterialTheme.typography.bodyLarge.copy(
                                                    fontFamily = fontFamily,
                                                    fontWeight = if (s.boldBody) FontWeight.Bold else FontWeight.Normal,
                                                    fontStyle = if (s.italicBody) FontStyle.Italic else FontStyle.Normal
                                                ),
                                                color = theme.textPrimary,
                                                textAlign = align,
                                                modifier = Modifier.fillMaxWidth()
                                            )
                                            if (typedLength < s.mainMessage.length) {
                                                Spacer(Modifier.height(6.dp))
                                                Text(
                                                    text = "Tap letter to reveal all",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = theme.textMuted,
                                                    modifier = Modifier.align(Alignment.CenterHorizontally)
                                                )
                                            }
                                            Spacer(Modifier.height(14.dp))
                                            Text(
                                                text = "— With love, ${s.senderName}",
                                                style = MaterialTheme.typography.titleSmall,
                                                color = theme.accent,
                                                modifier = Modifier.align(Alignment.End)
                                            )
                                        }
                                    }

                                    Button(
                                        onClick = {
                                            audioEngine.playButtonClick()
                                            viewModel.proceedToFinalExperience()
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                                        modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp).height(52.dp)
                                    ) {
                                        Text("Continue to Memories & Reply ❤️")
                                    }
                                }
                            }
                        }

                        RevealStage.FINAL_EXPERIENCE -> {
                            FinalExperienceContent(
                                surprise = surprise!!,
                                theme = theme,
                                audioEngine = audioEngine,
                                voiceHelper = voiceHelper,
                                replySending = uiState.replySending,
                                replySent = uiState.replySent,
                                errorMessage = uiState.errorMessage,
                                onSendReply = { name, reaction, msg, emoji, photo ->
                                    viewModel.sendReply(name, reaction, msg, emoji, photo)
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    if (showSoundDialog) {
        SoundSettingsDialog(
            audioEngine = audioEngine,
            onDismiss = { showSoundDialog = false }
        )
    }
}

@Composable
private fun PasscodeGateView(
    recipientName: String,
    errorMsg: String?,
    theme: SurpriseThemeSpec,
    onSubmit: (String) -> Unit
) {
    var code by remember { mutableStateOf("") }
    Card(
        shape = RoundedCornerShape(26.dp),
        colors = CardDefaults.cardColors(containerColor = theme.cardSurface),
        modifier = Modifier.padding(24.dp).widthIn(max = 440.dp)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(shape = CircleShape, color = theme.accent.copy(alpha = 0.14f), modifier = Modifier.size(56.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Lock, null, tint = theme.accent)
                }
            }
            Text("Secret Passcode Required", style = MaterialTheme.typography.titleLarge, color = theme.textPrimary)
            Text(
                text = "Hey $recipientName, enter the secret passcode from the sender to unlock your gift.",
                style = MaterialTheme.typography.bodyMedium,
                color = theme.textMuted,
                textAlign = TextAlign.Center
            )
            OutlinedTextField(
                value = code,
                onValueChange = { code = it },
                label = { Text("Passcode") },
                singleLine = true,
                isError = errorMsg != null,
                modifier = Modifier.fillMaxWidth()
            )
            if (errorMsg != null) {
                Text(errorMsg, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
            Button(
                onClick = { onSubmit(code) },
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Unlock Surprise")
            }
        }
    }
}

@Composable
private fun RevealStatusCard(
    emoji: String,
    title: String,
    body: String,
    theme: SurpriseThemeSpec,
    actionText: String,
    onAction: () -> Unit,
    secondaryActionText: String? = null,
    onSecondaryAction: (() -> Unit)? = null
) {
    Card(
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.cardColors(containerColor = theme.cardSurface.copy(alpha = 0.96f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        modifier = Modifier.padding(24.dp).widthIn(max = 460.dp)
    ) {
        Column(
            modifier = Modifier.padding(26.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(emoji, style = MaterialTheme.typography.displayLarge)
            Text(title, style = MaterialTheme.typography.headlineMedium, color = theme.textPrimary, textAlign = TextAlign.Center)
            Text(body, style = MaterialTheme.typography.bodyMedium, color = theme.textMuted, textAlign = TextAlign.Center)
            Spacer(Modifier.height(4.dp))
            Button(
                onClick = onAction,
                colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                Text(actionText)
            }
            if (secondaryActionText != null && onSecondaryAction != null) {
                TextButton(onClick = onSecondaryAction) {
                    Text(secondaryActionText, color = theme.textMuted)
                }
            }
        }
    }
}

@Composable
private fun FinalExperienceContent(
    surprise: Surprise,
    theme: SurpriseThemeSpec,
    audioEngine: GiftAudioEngine,
    voiceHelper: VoiceNoteHelper,
    replySending: Boolean,
    replySent: Boolean,
    errorMessage: String?,
    onSendReply: (String, ReactionType, String, String, String?) -> Unit
) {
    val context = LocalContext.current
    var lightboxIndex by remember { mutableStateOf<Int?>(null) }
    var lightboxZoomed by remember { mutableStateOf(false) }
    var slideshowPlaying by remember { mutableStateOf(false) }
    val galleryListState = rememberLazyListState()

    var playingVoice by remember { mutableStateOf(false) }
    var selectedReaction by remember { mutableStateOf(ReactionType.LOVED_IT) }
    var replyName by remember { mutableStateOf(surprise.recipientName) }
    var replyText by remember { mutableStateOf("") }
    var replyValidationError by remember { mutableStateOf<String?>(null) }
    var selectedEmoji by remember { mutableStateOf("❤️") }
    var replyPhotoBase64 by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(replySent) {
        if (replySent) {
            audioEngine.playReplySent()
        }
    }

    LaunchedEffect(slideshowPlaying, surprise.photos.size) {
        if (!slideshowPlaying || surprise.photos.size <= 1) return@LaunchedEffect
        var idx = 0
        while (slideshowPlaying) {
            delay(2800L)
            idx = (idx + 1) % surprise.photos.size
            galleryListState.animateScrollToItem(idx)
        }
    }

    val replyPhotoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            replyPhotoBase64 = ImageUtils.compressUriToBase64(context, uri, maxDim = 520, maxBytes = 120_000)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // Countdown Banner if eventDateMillis set
        if (surprise.eventDateMillis > 0L) {
            val diffDays = ((surprise.eventDateMillis - System.currentTimeMillis()) / 86_400_000L)
            val countdownLabel = when {
                diffDays > 0 -> "$diffDays day${if (diffDays == 1L) "" else "s"} until your special day! 🎉"
                diffDays == 0L -> "Today is the special day! 🎉"
                else -> "${-diffDays} day${if (diffDays == -1L) "" else "s"} of cherished celebration ❤️"
            }
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = theme.accent.copy(alpha = 0.14f),
                modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp)
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Schedule, null, tint = theme.accent)
                    Spacer(Modifier.width(8.dp))
                    Text(countdownLabel, style = MaterialTheme.typography.titleSmall, color = theme.accent)
                }
            }
        }

        // Polaroid Memories Gallery with Slideshow & Lightbox
        if (surprise.photos.isNotEmpty()) {
            Row(
                modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Cherished Memories 📸", style = MaterialTheme.typography.headlineSmall, color = theme.textPrimary)
                if (surprise.photos.size > 1) {
                    FilledTonalButton(onClick = { slideshowPlaying = !slideshowPlaying }) {
                        Icon(if (slideshowPlaying) Icons.Default.Pause else Icons.Default.Slideshow, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text(if (slideshowPlaying) "Pause" else "Slideshow")
                    }
                }
            }

            LazyRow(
                state = galleryListState,
                contentPadding = PaddingValues(horizontal = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp)
            ) {
                itemsIndexed(surprise.photos) { idx, base64 ->
                    val caption = surprise.photoCaptions.getOrElse(idx) { "Memory #${idx + 1}" }
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        shadowElevation = 6.dp,
                        modifier = Modifier
                            .width(280.dp)
                            .clickable {
                                lightboxZoomed = false
                                lightboxIndex = idx
                            }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Base64ImageView(
                                base64 = base64,
                                contentDescription = caption,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(210.dp)
                                    .clip(RoundedCornerShape(12.dp))
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                text = caption,
                                style = MaterialTheme.typography.bodyMedium,
                                fontStyle = FontStyle.Italic,
                                color = Color(0xFF2D1623),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                            Text(
                                text = "${idx + 1} of ${surprise.photos.size} • Tap to zoom",
                                style = MaterialTheme.typography.labelSmall,
                                color = theme.textMuted,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }
        } else {
            Image(
                painter = painterResource(id = R.drawable.img_memories_polaroid),
                contentDescription = "Polaroid keepsake memories",
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 460.dp)
                    .height(180.dp)
                    .clip(RoundedCornerShape(22.dp)),
                contentScale = ContentScale.Crop
            )
        }

        // Voice Note Card (pauses background music automatically while playing)
        if (!surprise.voiceNoteAudioBase64.isNullOrBlank() || !surprise.voiceNoteTranscript.isNullOrBlank()) {
            Card(
                shape = RoundedCornerShape(22.dp),
                colors = CardDefaults.cardColors(containerColor = theme.cardSurface),
                modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.GraphicEq, null, tint = theme.accent)
                        Spacer(Modifier.width(8.dp))
                        Text("Voice Message from ${surprise.senderName}", style = MaterialTheme.typography.titleMedium, color = theme.textPrimary)
                    }
                    if (!surprise.voiceNoteAudioBase64.isNullOrBlank()) {
                        FilledTonalButton(
                            onClick = {
                                if (playingVoice) {
                                    voiceHelper.stopPlayback()
                                    playingVoice = false
                                    audioEngine.resumeMusic()
                                } else {
                                    audioEngine.pauseMusic()
                                    playingVoice = true
                                    voiceHelper.playBase64Audio(surprise.voiceNoteAudioBase64) {
                                        playingVoice = false
                                        audioEngine.resumeMusic()
                                    }
                                }
                            }
                        ) {
                            Icon(if (playingVoice) Icons.Default.Stop else Icons.Default.PlayArrow, null)
                            Spacer(Modifier.width(6.dp))
                            Text(if (playingVoice) "Stop Voice Note" else "Play Voice Note (${surprise.voiceDurationSec}s)")
                        }
                    }
                    if (!surprise.voiceNoteTranscript.isNullOrBlank()) {
                        Text(
                            text = "\"${surprise.voiceNoteTranscript}\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontStyle = FontStyle.Italic,
                            color = theme.textMuted
                        )
                    }
                }
            }
        }

        // Stage 9: Final Emotional Closing Quote + Download Card
        Card(
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = theme.cardSurface.copy(alpha = 0.94f)),
            modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp)
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text("✨", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = stringResource(R.string.closing_emotional_quote),
                    style = MaterialTheme.typography.headlineMedium,
                    color = theme.textPrimary,
                    textAlign = TextAlign.Center
                )
                Text(
                    text = "With love always, ${surprise.senderName} ❤️",
                    style = MaterialTheme.typography.titleMedium,
                    color = theme.accent
                )
                OutlinedButton(onClick = { ImageUtils.exportSurpriseCardAndShare(context, surprise) }) {
                    Icon(Icons.Default.Download, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(6.dp))
                    Text("Save Keepsake Letter Card")
                }
            }
        }

        // Reply Section
        Card(
            shape = RoundedCornerShape(26.dp),
            colors = CardDefaults.cardColors(containerColor = theme.cardSurface),
            elevation = CardDefaults.cardElevation(defaultElevation = 6.dp),
            modifier = Modifier.fillMaxWidth().widthIn(max = 560.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Did this surprise make you smile? ❤️",
                    style = MaterialTheme.typography.titleLarge,
                    color = theme.textPrimary
                )

                if (replySent) {
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = Color(0xFFD1FAE5),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.CheckCircle, null, tint = Color(0xFF059669))
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = "Your reply has been delivered! ❤️",
                                style = MaterialTheme.typography.titleSmall,
                                color = Color(0xFF065F46)
                            )
                        }
                    }
                } else {
                    // Reactions row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        ReactionType.entries.forEach { reaction ->
                            val selected = reaction == selectedReaction
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = if (selected) theme.accent.copy(alpha = 0.16f) else theme.bgTop,
                                modifier = Modifier
                                    .weight(1f)
                                    .clickable {
                                        selectedReaction = reaction
                                        selectedEmoji = reaction.emoji
                                    }
                            ) {
                                Column(
                                    modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(reaction.emoji, style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        text = reaction.label.split(" ").first(),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (selected) theme.accent else theme.textMuted,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = replyName,
                        onValueChange = {
                            replyName = it.take(80)
                            replyValidationError = null
                        },
                        label = { Text("Your Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = replyText,
                        onValueChange = {
                            replyText = it.take(1500)
                            replyValidationError = null
                        },
                        label = { Text("Write a sweet reply...") },
                        supportingText = { Text("${replyText.length} / 1500") },
                        minLines = 3,
                        modifier = Modifier.fillMaxWidth().testTag("recipient_reply_input")
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            OutlinedButton(onClick = {
                                replyPhotoPicker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                            }) {
                                Icon(Icons.Default.AddPhotoAlternate, null, Modifier.size(18.dp))
                                Spacer(Modifier.width(6.dp))
                                Text(if (replyPhotoBase64 != null) "Change Photo" else "Add Photo")
                            }
                            if (replyPhotoBase64 != null) {
                                IconButton(onClick = { replyPhotoBase64 = null }) {
                                    Icon(Icons.Default.Close, "Remove attached photo", tint = MaterialTheme.colorScheme.error)
                                }
                            }
                        }
                        Text("Mood: $selectedEmoji", style = MaterialTheme.typography.labelLarge, color = theme.accent)
                    }

                    val shownError = replyValidationError ?: errorMessage
                    if (!shownError.isNullOrBlank()) {
                        Text(shownError, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                    }

                    Button(
                        onClick = {
                            if (replyName.isBlank()) {
                                replyValidationError = "Please enter your name."
                                return@Button
                            }
                            if (replyText.isBlank()) {
                                replyValidationError = "Please write a short message before sending."
                                return@Button
                            }
                            onSendReply(replyName.trim(), selectedReaction, replyText.trim(), selectedEmoji, replyPhotoBase64)
                        },
                        enabled = !replySending && !replySent,
                        colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("send_reply_button")
                    ) {
                        if (replySending) {
                            CircularProgressIndicator(modifier = Modifier.size(18.dp), color = Color.White, strokeWidth = 2.dp)
                            Spacer(Modifier.width(8.dp))
                            Text("Delivering...")
                        } else {
                            Icon(Icons.AutoMirrored.Filled.Send, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Send My Reply")
                        }
                    }
                }
            }
        }
    }

    val activeIdx = lightboxIndex
    if (activeIdx != null && activeIdx in surprise.photos.indices) {
        val currentBase64 = surprise.photos[activeIdx]
        val currentCaption = surprise.photoCaptions.getOrElse(activeIdx) { "Memory #${activeIdx + 1}" }
        AlertDialog(
            onDismissRequest = { lightboxIndex = null },
            title = {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${activeIdx + 1}/${surprise.photos.size} • $currentCaption",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    IconButton(onClick = { lightboxZoomed = !lightboxZoomed }) {
                        Icon(
                            imageVector = if (lightboxZoomed) Icons.Default.ZoomOut else Icons.Default.ZoomIn,
                            contentDescription = "Toggle photo zoom"
                        )
                    }
                }
            },
            text = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(320.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable { lightboxZoomed = !lightboxZoomed },
                        contentAlignment = Alignment.Center
                    ) {
                        Base64ImageView(
                            base64 = currentBase64,
                            contentDescription = currentCaption,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer {
                                    val s = if (lightboxZoomed) 1.85f else 1f
                                    scaleX = s
                                    scaleY = s
                                },
                            contentScale = ContentScale.Fit
                        )
                    }
                    if (surprise.photos.size > 1) {
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            OutlinedButton(
                                onClick = {
                                    lightboxZoomed = false
                                    lightboxIndex = (activeIdx - 1 + surprise.photos.size) % surprise.photos.size
                                }
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Prev")
                            }
                            OutlinedButton(
                                onClick = {
                                    lightboxZoomed = false
                                    lightboxIndex = (activeIdx + 1) % surprise.photos.size
                                }
                            ) {
                                Text("Next")
                                Spacer(Modifier.width(4.dp))
                                Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(16.dp))
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { lightboxIndex = null }) {
                    Text("Close")
                }
            }
        )
    }
}
