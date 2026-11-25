package com.example.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.audio.GiftAudioEngine
import com.example.data.model.MiniGameType
import com.example.data.model.Surprise
import com.example.ui.components.drawHeart
import com.example.ui.components.drawSparkle
import com.example.ui.theme.SurpriseThemeSpec
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.random.Random

@Composable
fun MiniGameArenaHost(
    gameType: MiniGameType,
    surprise: Surprise,
    theme: SurpriseThemeSpec,
    audioEngine: GiftAudioEngine,
    gameIndex: Int,
    totalGames: Int,
    onGameWon: () -> Unit,
    onSkipGame: (() -> Unit)?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = theme.accent.copy(alpha = 0.14f)
        ) {
            Text(
                text = "Challenge ${gameIndex + 1} of $totalGames • ${surprise.gameDifficulty.replaceFirstChar { it.uppercase() }}",
                style = MaterialTheme.typography.labelLarge,
                color = theme.accent,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
            )
        }

        AnimatedContent(targetState = gameType, label = "mini_game_switch") { activeGame ->
            Card(
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.cardColors(containerColor = theme.cardSurface.copy(alpha = 0.96f)),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "${activeGame.emoji} ${activeGame.title}",
                        style = MaterialTheme.typography.titleLarge,
                        color = theme.textPrimary,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = activeGame.instruction,
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.textMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(14.dp))

                    key(activeGame.id, gameIndex) {
                        when (activeGame) {
                            MiniGameType.CATCH_HEARTS -> CatchTheHeartsGame(
                                difficulty = surprise.gameDifficulty,
                                theme = theme,
                                audioEngine = audioEngine,
                                onWin = onGameWon
                            )
                            MiniGameType.MEMORY_MATCH -> MemoryMatchGame(
                                difficulty = surprise.gameDifficulty,
                                theme = theme,
                                audioEngine = audioEngine,
                                onWin = onGameWon
                            )
                            MiniGameType.SCRATCH_TO_REVEAL -> ScratchToRevealGame(
                                surprise = surprise,
                                theme = theme,
                                audioEngine = audioEngine,
                                onWin = onGameWon
                            )
                            MiniGameType.LOVE_PUZZLE -> LovePuzzleGame(
                                difficulty = surprise.gameDifficulty,
                                theme = theme,
                                audioEngine = audioEngine,
                                onWin = onGameWon
                            )
                            MiniGameType.GUESS_SECRET_CODE -> GuessSecretCodeGame(
                                surprise = surprise,
                                theme = theme,
                                audioEngine = audioEngine,
                                onWin = onGameWon
                            )
                            MiniGameType.SPIN_THE_WHEEL -> SpinTheWheelGame(
                                theme = theme,
                                audioEngine = audioEngine,
                                onWin = onGameWon
                            )
                            MiniGameType.POP_BALLOONS -> PopBalloonsGame(
                                difficulty = surprise.gameDifficulty,
                                theme = theme,
                                audioEngine = audioEngine,
                                onWin = onGameWon
                            )
                            MiniGameType.TAP_THE_HEART -> TapTheHeartGame(
                                difficulty = surprise.gameDifficulty,
                                theme = theme,
                                audioEngine = audioEngine,
                                onWin = onGameWon
                            )
                        }
                    }
                }
            }
        }

        if (onSkipGame != null) {
            TextButton(
                onClick = {
                    audioEngine.playButtonClick()
                    onSkipGame()
                },
                modifier = Modifier.testTag("skip_game_button")
            ) {
                Icon(Icons.Default.SkipNext, null, Modifier.size(18.dp), tint = theme.textMuted)
                Spacer(Modifier.width(4.dp))
                Text("Skip to Surprise", color = theme.textMuted)
            }
        }
    }
}

private data class FallingHeart(
    val id: Int,
    var xFraction: Float,
    var yFraction: Float,
    val speed: Float,
    val isGolden: Boolean
)

@Composable
fun CatchTheHeartsGame(
    difficulty: String,
    theme: SurpriseThemeSpec,
    audioEngine: GiftAudioEngine,
    onWin: () -> Unit
) {
    val targetScore = when (difficulty) {
        "easy" -> 7
        "hard" -> 14
        else -> 10
    }
    val durationSec = when (difficulty) {
        "easy" -> 45
        "hard" -> 32
        else -> 38
    }

    var started by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var missed by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableIntStateOf(durationSec) }
    var failed by remember { mutableStateOf(false) }
    var won by remember { mutableStateOf(false) }
    var winDispatched by remember { mutableStateOf(false) }
    var basketX by remember { mutableFloatStateOf(0.5f) }
    val hearts = remember { mutableStateListOf<FallingHeart>() }
    var nextId by remember { mutableIntStateOf(1) }

    fun restart() {
        hearts.clear()
        score = 0
        missed = 0
        timeLeft = durationSec
        failed = false
        won = false
        paused = false
        winDispatched = false
        started = true
    }

    LaunchedEffect(started, paused, won, failed) {
        if (!started || paused || won || failed) return@LaunchedEffect
        var tickMs = 0L
        while (started && !paused && !won && !failed) {
            delay(32L)
            tickMs += 32L
            if (tickMs % 1000L < 32L) {
                timeLeft = (timeLeft - 1).coerceAtLeast(0)
                if (timeLeft == 0 && score < targetScore) {
                    failed = true
                    audioEngine.playGameFailure()
                    break
                }
            }
            val spawnInterval = if (score > targetScore / 2) 440L else 540L
            if (tickMs % spawnInterval < 32L && hearts.size < 9) {
                val baseSpeed = when (difficulty) {
                    "hard" -> 0.0135f
                    "easy" -> 0.0085f
                    else -> 0.011f
                } + (score * 0.00035f)
                hearts.add(
                    FallingHeart(
                        id = nextId++,
                        xFraction = 0.10f + Random.nextFloat() * 0.80f,
                        yFraction = -0.05f,
                        speed = baseSpeed + Random.nextFloat() * 0.004f,
                        isGolden = Random.nextFloat() < 0.22f
                    )
                )
            }
            val iter = hearts.listIterator()
            while (iter.hasNext()) {
                val h = iter.next()
                h.yFraction += h.speed
                if (h.yFraction in 0.76f..0.93f && abs(h.xFraction - basketX) < 0.15f) {
                    iter.remove()
                    score = (score + if (h.isGolden) 2 else 1).coerceAtMost(targetScore)
                    audioEngine.playPopSound()
                    if (score >= targetScore && !won) {
                        won = true
                        audioEngine.playSuccessFanfare()
                        break
                    }
                } else if (h.yFraction > 1.04f) {
                    iter.remove()
                    missed++
                }
            }
        }
    }

    LaunchedEffect(won) {
        if (won && !winDispatched) {
            winDispatched = true
            delay(900L)
            onWin()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Caught: $score / $targetScore ❤️", style = MaterialTheme.typography.labelLarge, color = theme.accent)
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (missed > 0) {
                    Text("Missed: $missed", style = MaterialTheme.typography.labelSmall, color = theme.textMuted)
                    Spacer(Modifier.width(8.dp))
                }
                Text("Time: ${timeLeft}s", style = MaterialTheme.typography.labelLarge, color = theme.textMuted)
                if (started && !won && !failed) {
                    IconButton(onClick = { paused = !paused }, modifier = Modifier.size(32.dp)) {
                        Icon(if (paused) Icons.Default.PlayArrow else Icons.Default.Pause, "Pause or resume", tint = theme.accent)
                    }
                    IconButton(onClick = { restart() }, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Refresh, "Restart game", tint = theme.textMuted)
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (score.toFloat() / targetScore).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(8.dp)),
            color = theme.accent
        )
        Spacer(Modifier.height(10.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(290.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.verticalGradient(listOf(theme.bgTop, theme.bgBottom)))
                .border(1.dp, theme.accent.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
                .pointerInput(started, paused, won, failed) {
                    detectHorizontalDragGestures { change, dragAmount ->
                        change.consume()
                        if (started && !paused && !won && !failed && size.width > 0) {
                            basketX = (basketX + dragAmount / size.width.toFloat()).coerceIn(0.12f, 0.88f)
                        }
                    }
                }
                .pointerInput(started, paused, won, failed) {
                    detectTapGestures { offset ->
                        if (!started || paused || won || failed || size.width == 0 || size.height == 0) return@detectTapGestures
                        val tapX = offset.x / size.width
                        val tapY = offset.y / size.height
                        val hit = hearts.firstOrNull { hypot(it.xFraction - tapX, it.yFraction - tapY) < 0.14f }
                        if (hit != null) {
                            hearts.remove(hit)
                            score = (score + if (hit.isGolden) 2 else 1).coerceAtMost(targetScore)
                            audioEngine.playPopSound()
                            if (score >= targetScore && !won) {
                                won = true
                                audioEngine.playSuccessFanfare()
                            }
                        } else {
                            basketX = tapX.coerceIn(0.12f, 0.88f)
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                hearts.forEach { h ->
                    val cx = h.xFraction * size.width
                    val cy = h.yFraction * size.height
                    drawHeart(
                        center = Offset(cx, cy),
                        radius = 22f,
                        color = if (h.isGolden) Color(0xFFF59E0B) else theme.accent
                    )
                    if (h.isGolden) {
                        drawSparkle(Offset(cx, cy - 20f), 10f, Color(0xFFFEF08A))
                    }
                }
                val bw = size.width * 0.25f
                val bh = 38f
                val bx = (basketX * size.width - bw / 2f).coerceIn(8f, size.width - bw - 8f)
                val by = size.height - 56f
                drawRoundRect(
                    color = theme.accent,
                    topLeft = Offset(bx, by),
                    size = Size(bw, bh),
                    cornerRadius = CornerRadius(20f, 20f)
                )
                drawRoundRect(
                    color = Color.White.copy(alpha = 0.35f),
                    topLeft = Offset(bx + 10f, by + 6f),
                    size = Size(bw - 20f, 8f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
            }

            if (!started) {
                Button(
                    onClick = {
                        audioEngine.playButtonClick()
                        restart()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                    modifier = Modifier.testTag("start_catch_hearts")
                ) {
                    Text("Start Game")
                }
            } else if (paused && !won && !failed) {
                Surface(shape = RoundedCornerShape(18.dp), color = Color.White.copy(alpha = 0.95f)) {
                    Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Game Paused", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { paused = false }, colors = ButtonDefaults.buttonColors(containerColor = theme.accent)) {
                            Text("Resume")
                        }
                    }
                }
            } else if (failed) {
                Surface(shape = RoundedCornerShape(18.dp), color = Color.White.copy(alpha = 0.95f)) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("Almost there! Try again 💕", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { restart() }, colors = ButtonDefaults.buttonColors(containerColor = theme.accent)) {
                            Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Retry")
                        }
                    }
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = won && !paused && !failed,
                enter = fadeIn() + scaleIn()
            ) {
                Surface(shape = RoundedCornerShape(20.dp), color = Color.White.copy(alpha = 0.96f)) {
                    Text(
                        text = "🎉 Sweet! You caught all the hearts!",
                        style = MaterialTheme.typography.titleMedium,
                        color = theme.accent,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        }
    }
}

private data class MemoryCard(
    val id: Int,
    val symbol: String,
    val revealed: Boolean = false,
    val matched: Boolean = false
)

@Composable
fun MemoryMatchGame(
    difficulty: String,
    theme: SurpriseThemeSpec,
    audioEngine: GiftAudioEngine,
    onWin: () -> Unit
) {
    val pairCount = when (difficulty) {
        "easy" -> 3
        "hard" -> 6
        else -> 4
    }
    val allSymbols = listOf("❤️", "🎁", "🌸", "⭐", "🎀", "🍰")
    fun buildDeck(): List<MemoryCard> {
        val chosen = allSymbols.take(pairCount)
        return (chosen + chosen).shuffled().mapIndexed { idx, s -> MemoryCard(idx, s) }
    }

    var cards by remember(pairCount) { mutableStateOf(buildDeck()) }
    var selectedIndices by remember { mutableStateOf<List<Int>>(emptyList()) }
    var moves by remember { mutableIntStateOf(0) }
    var elapsedSec by remember { mutableIntStateOf(0) }
    var locked by remember { mutableStateOf(false) }
    var mismatchPair by remember { mutableStateOf<Set<Int>>(emptySet()) }
    var winDispatched by remember { mutableStateOf(false) }
    val allMatched = cards.isNotEmpty() && cards.all { it.matched }

    LaunchedEffect(allMatched) {
        if (!allMatched) {
            while (!allMatched) {
                delay(1000L)
                elapsedSec++
            }
        }
    }

    LaunchedEffect(selectedIndices) {
        if (selectedIndices.size == 2) {
            locked = true
            val idx1 = selectedIndices[0]
            val idx2 = selectedIndices[1]
            val first = cards[idx1]
            val second = cards[idx2]
            delay(500L)
            if (first.symbol == second.symbol) {
                audioEngine.playCardMatch()
                cards = cards.mapIndexed { i, c ->
                    if (i == idx1 || i == idx2) c.copy(matched = true) else c
                }
            } else {
                mismatchPair = setOf(idx1, idx2)
                audioEngine.playCardMismatch()
                delay(240L)
                mismatchPair = emptySet()
                cards = cards.mapIndexed { i, c ->
                    if (i == idx1 || i == idx2) c.copy(revealed = false) else c
                }
            }
            selectedIndices = emptyList()
            locked = false
        }
    }

    LaunchedEffect(allMatched) {
        if (allMatched && !winDispatched) {
            winDispatched = true
            audioEngine.playSuccessFanfare()
            delay(850L)
            onWin()
        }
    }

    val columns = 4
    val rows = (cards.size + columns - 1) / columns

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Pairs: ${cards.count { it.matched } / 2} / $pairCount", color = theme.accent, style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Moves: $moves • ${elapsedSec}s", color = theme.textMuted, style = MaterialTheme.typography.labelLarge)
                IconButton(
                    onClick = {
                        cards = buildDeck()
                        selectedIndices = emptyList()
                        moves = 0
                        elapsedSec = 0
                        locked = false
                        mismatchPair = emptySet()
                        winDispatched = false
                    },
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(Icons.Default.Refresh, "Restart memory match", tint = theme.textMuted)
                }
            }
        }
        Spacer(Modifier.height(10.dp))

        for (r in 0 until rows) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                for (c in 0 until columns) {
                    val idx = r * columns + c
                    if (idx < cards.size) {
                        val card = cards[idx]
                        val faceUp = card.revealed || card.matched
                        val isMismatch = mismatchPair.contains(idx)
                        val flipScale by animateFloatAsState(
                            targetValue = if (faceUp) 1f else 0.94f,
                            animationSpec = spring(),
                            label = "card_flip"
                        )
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .height(72.dp)
                                .graphicsLayer {
                                    scaleX = flipScale
                                    scaleY = flipScale
                                }
                                .clickable(enabled = !locked && !faceUp && !allMatched && selectedIndices.size < 2 && !selectedIndices.contains(idx)) {
                                    audioEngine.playCardFlip()
                                    moves++
                                    cards = cards.mapIndexed { i, item ->
                                        if (i == idx) item.copy(revealed = true) else item
                                    }
                                    selectedIndices = selectedIndices + idx
                                },
                            shape = RoundedCornerShape(16.dp),
                            color = when {
                                card.matched -> Color(0xFFD1FAE5)
                                isMismatch -> Color(0xFFFEE2E2)
                                faceUp -> Color.White
                                else -> theme.accent.copy(alpha = 0.18f)
                            },
                            shadowElevation = if (faceUp) 4.dp else 1.dp
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = if (faceUp) card.symbol else "✨",
                                    style = MaterialTheme.typography.headlineSmall
                                )
                            }
                        }
                    } else {
                        Spacer(Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun ScratchToRevealGame(
    surprise: Surprise,
    theme: SurpriseThemeSpec,
    audioEngine: GiftAudioEngine,
    onWin: () -> Unit
) {
    val gridCols = 14
    val gridRows = 9
    val scratched = remember { mutableStateListOf<Boolean>().apply { repeat(gridCols * gridRows) { add(false) } } }
    val progress = scratched.count { it }.toFloat() / (gridCols * gridRows).toFloat()
    val threshold = 0.55f
    var won by remember { mutableStateOf(false) }
    var winDispatched by remember { mutableStateOf(false) }

    fun scratchAt(x: Float, y: Float, width: Int, height: Int) {
        if (won || width <= 0 || height <= 0) return
        val cx = ((x / width) * gridCols).toInt().coerceIn(0, gridCols - 1)
        val cy = ((y / height) * gridRows).toInt().coerceIn(0, gridRows - 1)
        var changed = false
        for (dy in -1..1) {
            for (dx in -1..1) {
                val nx = cx + dx
                val ny = cy + dy
                if (nx in 0 until gridCols && ny in 0 until gridRows) {
                    val idx = ny * gridCols + nx
                    if (!scratched[idx]) {
                        scratched[idx] = true
                        changed = true
                    }
                }
            }
        }
        if (changed) audioEngine.playScratchTick()
    }

    LaunchedEffect(progress) {
        if (progress >= threshold && !won) {
            won = true
            audioEngine.playSuccessFanfare()
        }
    }

    LaunchedEffect(won) {
        if (won && !winDispatched) {
            winDispatched = true
            delay(950L)
            onWin()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Scratched: ${(progress * 100).toInt()}% (Goal: 55%)",
                style = MaterialTheme.typography.labelLarge,
                color = theme.accent
            )
            IconButton(
                onClick = {
                    for (i in scratched.indices) scratched[i] = false
                    won = false
                    winDispatched = false
                },
                enabled = !won,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(Icons.Default.Refresh, "Reset scratch card", tint = theme.textMuted)
            }
        }
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (progress / threshold).coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(RoundedCornerShape(8.dp)),
            color = theme.accent
        )
        Spacer(Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(215.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.linearGradient(listOf(theme.bgTop, theme.bgBottom)))
                .border(1.5.dp, theme.accent.copy(alpha = 0.35f), RoundedCornerShape(22.dp))
                .pointerInput(won) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        scratchAt(change.position.x, change.position.y, size.width, size.height)
                    }
                }
                .pointerInput(won) {
                    detectTapGestures { offset ->
                        scratchAt(offset.x, offset.y, size.width, size.height)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("🎁 Surprise Unlocked For", style = MaterialTheme.typography.labelLarge, color = theme.textMuted)
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "${surprise.recipientName} ❤️",
                    style = MaterialTheme.typography.displaySmall,
                    color = theme.accent,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "With love from ${surprise.senderName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = theme.textPrimary
                )
            }

            if (!won) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val cellW = size.width / gridCols
                    val cellH = size.height / gridRows
                    for (r in 0 until gridRows) {
                        for (c in 0 until gridCols) {
                            if (!scratched[r * gridCols + c]) {
                                val foilTint = if ((r + c) % 2 == 0) Color(0xFFE5D7E4) else Color(0xFFDCD0DE)
                                drawRect(
                                    color = foilTint,
                                    topLeft = Offset(c * cellW, r * cellH),
                                    size = Size(cellW + 1f, cellH + 1f)
                                )
                                if ((r + c) % 3 == 0) {
                                    drawSparkle(
                                        Offset(c * cellW + cellW / 2f, r * cellH + cellH / 2f),
                                        6f,
                                        Color.White.copy(alpha = 0.65f)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(6.dp))
        Text("Tap or rub your finger across the shimmer foil!", style = MaterialTheme.typography.bodySmall, color = theme.textMuted)
    }
}

@Composable
fun LovePuzzleGame(
    difficulty: String,
    theme: SurpriseThemeSpec,
    audioEngine: GiftAudioEngine,
    onWin: () -> Unit
) {
    val solved = remember { listOf(1, 2, 3, 4, 5, 6, 7, 8, 0) }
    fun generateScrambled(): List<Int> {
        val state = solved.toMutableList()
        var blank = 8
        var prevBlank = -1
        val steps = when (difficulty) {
            "easy" -> 8
            "hard" -> 28
            else -> 16
        }
        repeat(steps) {
            val r = blank / 3
            val c = blank % 3
            val neighbors = buildList {
                if (r > 0 && blank - 3 != prevBlank) add(blank - 3)
                if (r < 2 && blank + 3 != prevBlank) add(blank + 3)
                if (c > 0 && blank - 1 != prevBlank) add(blank - 1)
                if (c < 2 && blank + 1 != prevBlank) add(blank + 1)
            }
            val pick = neighbors.random()
            state[blank] = state[pick]
            state[pick] = 0
            prevBlank = blank
            blank = pick
        }
        return if (state == solved) {
            state[8] = state[7]
            state[7] = 0
            state.toList()
        } else {
            state.toList()
        }
    }

    var tiles by remember(difficulty) { mutableStateOf(generateScrambled()) }
    var moves by remember { mutableIntStateOf(0) }
    var hintedTile by remember { mutableStateOf<Int?>(null) }
    var winDispatched by remember { mutableStateOf(false) }
    val isSolved = tiles == solved
    val emojis = listOf("", "💖", "🌹", "✨", "🎀", "❤️", "🧁", "💌", "🎁")

    LaunchedEffect(isSolved) {
        if (isSolved && !winDispatched) {
            winDispatched = true
            audioEngine.playSuccessFanfare()
            delay(850L)
            onWin()
        }
    }

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("In place: ${tiles.withIndex().count { (i, v) -> v != 0 && v == i + 1 }} / 8", style = MaterialTheme.typography.labelLarge, color = theme.accent)
            Text("Moves: $moves", style = MaterialTheme.typography.labelLarge, color = theme.textMuted)
        }
        Spacer(Modifier.height(8.dp))

        for (r in 0 until 3) {
            Row(
                modifier = Modifier.padding(vertical = 3.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                for (c in 0 until 3) {
                    val idx = r * 3 + c
                    val value = tiles[idx]
                    val blankIdx = tiles.indexOf(0)
                    val br = blankIdx / 3
                    val bc = blankIdx % 3
                    val isAdjacent = (abs(r - br) + abs(c - bc)) == 1
                    val isHinted = hintedTile == value && value != 0

                    Surface(
                        modifier = Modifier
                            .size(74.dp)
                            .border(
                                width = if (isHinted) 2.dp else 0.dp,
                                color = if (isHinted) theme.accent else Color.Transparent,
                                shape = RoundedCornerShape(16.dp)
                            )
                            .clickable(enabled = value != 0 && isAdjacent && !isSolved) {
                                audioEngine.playPopSound()
                                moves++
                                hintedTile = null
                                val next = tiles.toMutableList()
                                next[blankIdx] = value
                                next[idx] = 0
                                tiles = next
                            },
                        shape = RoundedCornerShape(16.dp),
                        color = when {
                            value == 0 -> theme.bgBottom.copy(alpha = 0.4f)
                            value == idx + 1 -> theme.accent.copy(alpha = 0.18f)
                            else -> Color.White
                        },
                        shadowElevation = if (value == 0) 0.dp else 4.dp
                    ) {
                        if (value != 0) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Text(emojis[value], style = MaterialTheme.typography.titleLarge)
                                Text("$value", style = MaterialTheme.typography.labelSmall, color = theme.textMuted)
                            }
                        }
                    }
                }
            }
        }
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(
                onClick = {
                    val blankIdx = tiles.indexOf(0)
                    val br = blankIdx / 3
                    val bc = blankIdx % 3
                    val candidates = buildList {
                        if (br > 0) add(blankIdx - 3)
                        if (br < 2) add(blankIdx + 3)
                        if (bc > 0) add(blankIdx - 1)
                        if (bc < 2) add(blankIdx + 1)
                    }
                    // Prefer moving a tile closer to its target position (blankIdx + 1)
                    val bestIdx = candidates.firstOrNull { tiles[it] == blankIdx + 1 } ?: candidates.first()
                    hintedTile = tiles[bestIdx]
                    audioEngine.playButtonClick()
                },
                enabled = !isSolved
            ) {
                Icon(Icons.Default.Lightbulb, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Hint")
            }
            OutlinedButton(
                onClick = {
                    tiles = generateScrambled()
                    moves = 0
                    hintedTile = null
                    winDispatched = false
                },
                enabled = !isSolved
            ) {
                Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                Spacer(Modifier.width(4.dp))
                Text("Shuffle")
            }
        }
    }
}
