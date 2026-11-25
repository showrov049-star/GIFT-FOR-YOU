package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.data.audio.GiftAudioEngine
import com.example.data.model.Surprise
import com.example.ui.components.drawHeart
import com.example.ui.theme.SurpriseThemeSpec
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun GuessSecretCodeGame(
    surprise: Surprise,
    theme: SurpriseThemeSpec,
    audioEngine: GiftAudioEngine,
    onWin: () -> Unit
) {
    val clue = surprise.secretCodeClue.ifBlank { "What makes someone smile when they open a gift?" }
    val expected = surprise.secretCodeAnswer.ifBlank { "smile" }
    var guess by remember { mutableStateOf("") }
    var attempts by remember { mutableIntStateOf(0) }
    var showHintManual by remember { mutableStateOf(false) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var unlocked by remember { mutableStateOf(false) }
    var winDispatched by remember { mutableStateOf(false) }

    fun submitGuess() {
        if (unlocked) return
        val cleaned = guess.trim()
        if (cleaned.isBlank()) {
            errorMsg = "Type your guess first!"
            return
        }
        if (cleaned.equals(expected.trim(), ignoreCase = true)) {
            unlocked = true
            errorMsg = null
        } else {
            attempts++
            audioEngine.playCardMismatch()
            errorMsg = "Not quite ($attempts ${if (attempts == 1) "try" else "tries"})! Give it another try 💕"
        }
    }

    LaunchedEffect(unlocked) {
        if (unlocked && !winDispatched) {
            winDispatched = true
            audioEngine.playSuccessFanfare()
            delay(850L)
            onWin()
        }
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(18.dp),
            color = theme.bgTop,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Personal Clue 🔐", style = MaterialTheme.typography.labelLarge, color = theme.accent)
                    if (!unlocked && !showHintManual) {
                        TextButton(onClick = {
                            audioEngine.playButtonClick()
                            showHintManual = true
                        }) {
                            Icon(Icons.Default.Lightbulb, null, Modifier.size(15.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Hint")
                        }
                    }
                }
                Spacer(Modifier.height(4.dp))
                Text(clue, style = MaterialTheme.typography.bodyLarge, color = theme.textPrimary)
                if (attempts >= 2 || showHintManual) {
                    Spacer(Modifier.height(6.dp))
                    val masked = expected.trim().mapIndexed { idx, ch ->
                        if (idx == 0 || ch == ' ') ch else '_'
                    }.joinToString(" ")
                    Text(
                        text = "Hint: $masked (${expected.trim().length} characters)",
                        style = MaterialTheme.typography.bodySmall,
                        color = theme.textMuted
                    )
                }
            }
        }

        OutlinedTextField(
            value = guess,
            onValueChange = {
                guess = it
                errorMsg = null
            },
            enabled = !unlocked,
            label = { Text("Enter secret word") },
            singleLine = true,
            isError = errorMsg != null,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { submitGuess() }),
            modifier = Modifier.fillMaxWidth()
        )

        if (errorMsg != null) {
            Text(errorMsg!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Button(
            onClick = { submitGuess() },
            enabled = !unlocked,
            colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Default.LockOpen, null, Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(if (unlocked) "Unlocked! 🎉" else "Unlock Gift")
        }
    }
}

@Composable
fun SpinTheWheelGame(
    theme: SurpriseThemeSpec,
    audioEngine: GiftAudioEngine,
    onWin: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val rotation = remember { Animatable(0f) }
    var spinning by remember { mutableStateOf(false) }
    var prizeText by remember { mutableStateOf<String?>(null) }
    var winDispatched by remember { mutableStateOf(false) }

    val slices = listOf(
        Triple("🤗", "Warm Hug", Color(0xFFFB7185)),
        Triple("✨", "Golden Wish", Color(0xFFFBBF24)),
        Triple("💖", "Pure Joy", Color(0xFFC084FC)),
        Triple("🌸", "Blessing", Color(0xFF34D399)),
        Triple("😊", "Big Smile", Color(0xFFF472B6)),
        Triple("🎁", "Gift Key", Color(0xFF60A5FA))
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Box(contentAlignment = Alignment.TopCenter, modifier = Modifier.size(238.dp)) {
            Canvas(
                modifier = Modifier
                    .size(214.dp)
                    .align(Alignment.Center)
                    .rotate(rotation.value)
            ) {
                val sweep = 360f / slices.size
                val radius = size.minDimension / 2f
                slices.forEachIndexed { idx, (emoji, _, color) ->
                    val startAngle = idx * sweep - 90f
                    drawArc(
                        color = color,
                        startAngle = startAngle,
                        sweepAngle = sweep,
                        useCenter = true
                    )
                    // Draw emoji centered in slice
                    val midAngleRad = ((startAngle + sweep / 2f) * PI / 180.0)
                    val tx = center.x + (cos(midAngleRad) * radius * 0.62f).toFloat()
                    val ty = center.y + (sin(midAngleRad) * radius * 0.62f).toFloat()
                    drawIntoCanvas { canvas ->
                        val paint = android.graphics.Paint().apply {
                            textSize = 42f
                            textAlign = android.graphics.Paint.Align.CENTER
                        }
                        canvas.nativeCanvas.drawText(emoji, tx, ty + 14f, paint)
                    }
                }
                drawCircle(color = Color.White, radius = size.minDimension * 0.15f)
            }
            // Pointer triangle at top
            Canvas(modifier = Modifier.size(28.dp)) {
                val p = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width, 0f)
                    lineTo(size.width / 2f, size.height)
                    close()
                }
                drawPath(p, color = theme.textPrimary)
            }
        }

        if (prizeText != null) {
            AnimatedVisibility(visible = true, enter = fadeIn() + scaleIn()) {
                Surface(shape = RoundedCornerShape(16.dp), color = theme.bgTop) {
                    Text(
                        text = "You landed on: $prizeText!",
                        style = MaterialTheme.typography.titleMedium,
                        color = theme.accent,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
            }
        }

        Button(
            onClick = {
                if (spinning || winDispatched) return@Button
                spinning = true
                prizeText = null
                scope.launch {
                    val chosenIndex = Random.nextInt(slices.size)
                    val sweep = 360f / slices.size
                    // Target center of chosenIndex aligns with -90deg (top pointer)
                    val desiredMod = (360f - (chosenIndex * sweep + sweep / 2f) + 360f) % 360f
                    val currentMod = ((rotation.value % 360f) + 360f) % 360f
                    val deltaMod = (desiredMod - currentMod + 360f) % 360f
                    val target = rotation.value + 1440f + deltaMod

                    val tickJob = launch {
                        repeat(14) { step ->
                            audioEngine.playWheelTick()
                            delay(120L + step * 16L)
                        }
                    }
                    rotation.animateTo(
                        targetValue = target,
                        animationSpec = tween(durationMillis = 2600, easing = FastOutSlowInEasing)
                    )
                    tickJob.cancel()

                    val wonSlice = slices[chosenIndex]
                    prizeText = "${wonSlice.first} ${wonSlice.second}"
                    audioEngine.playSuccessFanfare()
                    winDispatched = true
                    delay(1000L)
                    onWin()
                }
            },
            enabled = !spinning && !winDispatched,
            colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
        ) {
            Text(if (spinning) "Spinning..." else "Spin the Wheel 🎡")
        }
    }
}

private data class BalloonItem(
    val id: Int,
    val xFraction: Float,
    var yFraction: Float,
    val speed: Float,
    val color: Color
)

private data class PopBurst(
    val xFraction: Float,
    val yFraction: Float,
    val color: Color,
    var life: Int = 8
)

@Composable
fun PopBalloonsGame(
    difficulty: String,
    theme: SurpriseThemeSpec,
    audioEngine: GiftAudioEngine,
    onWin: () -> Unit
) {
    val target = when (difficulty) {
        "easy" -> 7
        "hard" -> 13
        else -> 10
    }
    var started by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var popped by remember { mutableIntStateOf(0) }
    var timeLeft by remember { mutableIntStateOf(30) }
    var failed by remember { mutableStateOf(false) }
    var won by remember { mutableStateOf(false) }
    var winDispatched by remember { mutableStateOf(false) }
    val balloons = remember { mutableStateListOf<BalloonItem>() }
    val bursts = remember { mutableStateListOf<PopBurst>() }
    var nextId by remember { mutableIntStateOf(1) }
    val palette = remember {
        listOf(Color(0xFFE11D48), Color(0xFF9333EA), Color(0xFFF59E0B), Color(0xFF10B981), Color(0xFFEC4899))
    }

    fun restart() {
        balloons.clear()
        bursts.clear()
        popped = 0
        timeLeft = 30
        failed = false
        won = false
        paused = false
        winDispatched = false
        started = true
    }

    LaunchedEffect(started, paused, won, failed) {
        if (!started || paused || won || failed) return@LaunchedEffect
        var elapsedMs = 0L
        while (started && !paused && !won && !failed) {
            delay(32L)
            elapsedMs += 32L
            if (elapsedMs % 1000L < 32L) {
                timeLeft = (timeLeft - 1).coerceAtLeast(0)
                if (timeLeft == 0 && popped < target) {
                    failed = true
                    audioEngine.playGameFailure()
                    break
                }
            }
            if (elapsedMs % 460L < 32L && balloons.size < 9) {
                val baseSpeed = when (difficulty) {
                    "hard" -> 0.011f
                    "easy" -> 0.007f
                    else -> 0.0085f
                }
                balloons.add(
                    BalloonItem(
                        id = nextId++,
                        xFraction = 0.12f + Random.nextFloat() * 0.76f,
                        yFraction = 1.08f,
                        speed = baseSpeed + Random.nextFloat() * 0.006f,
                        color = palette.random()
                    )
                )
            }
            val iter = balloons.listIterator()
            while (iter.hasNext()) {
                val b = iter.next()
                b.yFraction -= b.speed
                if (b.yFraction < -0.15f) iter.remove()
            }
            val burstIter = bursts.listIterator()
            while (burstIter.hasNext()) {
                val burst = burstIter.next()
                burst.life--
                if (burst.life <= 0) burstIter.remove()
            }
        }
    }

    LaunchedEffect(won) {
        if (won && !winDispatched) {
            winDispatched = true
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
            Text("Popped: $popped / $target 🎈", color = theme.accent, style = MaterialTheme.typography.labelLarge)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Time: ${timeLeft}s", color = theme.textMuted, style = MaterialTheme.typography.labelLarge)
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
            progress = { (popped.toFloat() / target).coerceIn(0f, 1f) },
            modifier = Modifier
                .fillMaxWidth()
                .height(8.dp)
                .clip(RoundedCornerShape(8.dp)),
            color = theme.accent
        )
        Spacer(Modifier.height(10.dp))

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(270.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.verticalGradient(listOf(theme.bgTop, theme.bgBottom)))
                .border(1.dp, theme.accent.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
                .pointerInput(started, paused, won, failed) {
                    detectTapGestures { offset ->
                        if (!started || paused || won || failed || size.width == 0 || size.height == 0) return@detectTapGestures
                        val tx = offset.x / size.width
                        val ty = offset.y / size.height
                        val hit = balloons.lastOrNull { hypot(it.xFraction - tx, it.yFraction - ty) < 0.15f }
                        if (hit != null) {
                            balloons.remove(hit)
                            bursts.add(PopBurst(hit.xFraction, hit.yFraction, hit.color))
                            popped = (popped + 1).coerceAtMost(target)
                            audioEngine.playPopSound()
                            if (popped >= target && !won) {
                                won = true
                                audioEngine.playSuccessFanfare()
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                balloons.forEach { b ->
                    val cx = b.xFraction * size.width
                    val cy = b.yFraction * size.height
                    drawLine(
                        color = Color.Gray.copy(alpha = 0.55f),
                        start = Offset(cx, cy + 36f),
                        end = Offset(cx, cy + 82f),
                        strokeWidth = 3f
                    )
                    drawOval(
                        color = b.color,
                        topLeft = Offset(cx - 30f, cy - 38f),
                        size = Size(60f, 76f)
                    )
                    drawOval(
                        color = Color.White.copy(alpha = 0.45f),
                        topLeft = Offset(cx - 18f, cy - 26f),
                        size = Size(14f, 22f)
                    )
                }
                bursts.forEach { burst ->
                    val cx = burst.xFraction * size.width
                    val cy = burst.yFraction * size.height
                    val radius = (9 - burst.life) * 8f
                    drawCircle(
                        color = burst.color.copy(alpha = (burst.life / 8f).coerceIn(0f, 1f)),
                        radius = radius,
                        center = Offset(cx, cy)
                    )
                }
            }

            if (!started) {
                Button(
                    onClick = {
                        audioEngine.playButtonClick()
                        restart()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
                ) {
                    Text("Start Popping 🎈")
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
                    Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Time's up! Give it another pop 🎈", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { restart() }, colors = ButtonDefaults.buttonColors(containerColor = theme.accent)) {
                            Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Try Again")
                        }
                    }
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = won && !paused && !failed,
                enter = fadeIn() + scaleIn()
            ) {
                Surface(shape = RoundedCornerShape(16.dp), color = Color.White.copy(alpha = 0.95f)) {
                    Text(
                        text = "🎉 Woohoo! All balloons popped!",
                        color = theme.accent,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TapTheHeartGame(
    difficulty: String,
    theme: SurpriseThemeSpec,
    audioEngine: GiftAudioEngine,
    onWin: () -> Unit
) {
    val target = when (difficulty) {
        "easy" -> 6
        "hard" -> 14
        else -> 10
    }
    var started by remember { mutableStateOf(false) }
    var paused by remember { mutableStateOf(false) }
    var score by remember { mutableIntStateOf(0) }
    var combo by remember { mutableIntStateOf(0) }
    var lastTapMs by remember { mutableStateOf(0L) }
    var timeLeft by remember { mutableIntStateOf(15) }
    var heartX by remember { mutableStateOf(0.5f) }
    var heartY by remember { mutableStateOf(0.5f) }
    var won by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    var winDispatched by remember { mutableStateOf(false) }

    fun restart() {
        score = 0
        combo = 0
        lastTapMs = 0L
        timeLeft = 15
        won = false
        failed = false
        paused = false
        winDispatched = false
        started = true
    }

    LaunchedEffect(started, paused, won, failed) {
        if (!started || paused || won || failed) return@LaunchedEffect
        while (timeLeft > 0 && !won && !paused) {
            delay(1000L)
            timeLeft--
            if (timeLeft == 0 && score < target) {
                failed = true
                audioEngine.playGameFailure()
            }
        }
    }

    LaunchedEffect(won) {
        if (won && !winDispatched) {
            winDispatched = true
            audioEngine.playSuccessFanfare()
            delay(850L)
            onWin()
        }
    }

    val heartPopScale by animateFloatAsState(
        targetValue = if (started && !won && !failed) 1f else 0.7f,
        animationSpec = spring(),
        label = "heart_pop"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Taps: $score / $target 💖", color = theme.accent, style = MaterialTheme.typography.labelLarge)
                if (combo >= 2) {
                    Spacer(Modifier.width(8.dp))
                    Surface(shape = RoundedCornerShape(50), color = theme.accent.copy(alpha = 0.16f)) {
                        Text(
                            text = "${combo}x Combo!",
                            style = MaterialTheme.typography.labelSmall,
                            color = theme.accent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Time: ${timeLeft}s", color = theme.textMuted, style = MaterialTheme.typography.labelLarge)
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
        Spacer(Modifier.height(8.dp))

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .clip(RoundedCornerShape(22.dp))
                .background(Brush.radialGradient(listOf(theme.bgTop, theme.bgBottom)))
                .border(1.dp, theme.accent.copy(alpha = 0.25f), RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            val areaW = constraints.maxWidth.toFloat()
            val areaH = constraints.maxHeight.toFloat()

            if (started && !paused && !won && !failed) {
                Surface(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .offset {
                            IntOffset(
                                ((heartX * (areaW - 180f)).roundToInt()).coerceAtLeast(16),
                                ((heartY * (areaH - 180f)).roundToInt()).coerceAtLeast(16)
                            )
                        }
                        .size(64.dp)
                        .scale(heartPopScale)
                        .clickable {
                            if (won || failed) return@clickable
                            val now = System.currentTimeMillis()
                            combo = if (lastTapMs > 0L && now - lastTapMs <= 1200L) combo + 1 else 1
                            lastTapMs = now
                            score = (score + 1).coerceAtMost(target)
                            audioEngine.playPopSound()
                            if (score >= target) {
                                won = true
                            } else {
                                heartX = 0.1f + Random.nextFloat() * 0.8f
                                heartY = 0.1f + Random.nextFloat() * 0.8f
                            }
                        },
                    shape = CircleShape,
                    color = theme.accent.copy(alpha = 0.16f)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Canvas(modifier = Modifier.size(36.dp)) {
                            drawHeart(Offset(size.width / 2f, size.height / 2f), 16f, theme.accent)
                        }
                    }
                }
            } else if (!started) {
                Button(
                    onClick = {
                        audioEngine.playButtonClick()
                        restart()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accent)
                ) {
                    Text("Start 15s Rush 💖")
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
                    Column(modifier = Modifier.padding(18.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("So close! Try again 💖", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = { restart() }, colors = ButtonDefaults.buttonColors(containerColor = theme.accent)) {
                            Icon(Icons.Default.Refresh, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Retry 15s Rush")
                        }
                    }
                }
            }
            androidx.compose.animation.AnimatedVisibility(
                visible = won && !paused && !failed,
                enter = fadeIn() + scaleIn()
            ) {
                Text(
                    text = "💖 Lightning reflexes! Gift unlocked!",
                    style = MaterialTheme.typography.titleMedium,
                    color = theme.accent,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(16.dp)
                )
            }
        }
    }
}
