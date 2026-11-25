package com.example.ui.components

import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

@Composable
fun rememberReducedMotion(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
}

private data class ParticleSeed(
    val xFraction: Float,
    val yOffset: Float,
    val speed: Float,
    val sizeDp: Float,
    val phase: Float,
    val colorIndex: Int
)

@Composable
fun FloatingAmbientParticles(
    effect: String,
    primaryColor: Color,
    secondaryColor: Color,
    modifier: Modifier = Modifier,
    count: Int = 22
) {
    val reducedMotion = rememberReducedMotion()
    val seeds = remember(count) {
        val rng = Random(42)
        List(count) {
            ParticleSeed(
                xFraction = rng.nextFloat(),
                yOffset = rng.nextFloat(),
                speed = 0.55f + rng.nextFloat() * 0.7f,
                sizeDp = 7f + rng.nextFloat() * 10f,
                phase = rng.nextFloat() * 6.28f,
                colorIndex = rng.nextInt(3)
            )
        }
    }

    val progress = if (reducedMotion) {
        0.35f
    } else {
        val infiniteTransition = rememberInfiniteTransition(label = "ambient_particles")
        val animated by infiniteTransition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 12000, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "particle_tick"
        )
        animated
    }

    val colors = remember(primaryColor, secondaryColor) {
        listOf(
            primaryColor.copy(alpha = 0.34f),
            secondaryColor.copy(alpha = 0.36f),
            Color(0xFFF59E0B).copy(alpha = 0.30f)
        )
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        if (w <= 0f || h <= 0f) return@Canvas

        seeds.forEach { seed ->
            val yNorm = if (effect == "petals" || effect == "confetti") {
                (seed.yOffset + progress * seed.speed) % 1f
            } else {
                1f - ((seed.yOffset + progress * seed.speed) % 1f)
            }
            val sway = sin((progress * 2f * PI + seed.phase).toDouble()).toFloat() * 22f
            val x = (seed.xFraction * w + sway).coerceIn(0f, w)
            val y = yNorm * h
            val r = seed.sizeDp * density * 0.5f
            val color = colors[seed.colorIndex]

            when (effect) {
                "hearts" -> drawHeart(Offset(x, y), r, color)
                "stars" -> drawStar(Offset(x, y), r, color)
                "petals" -> {
                    rotate(degrees = progress * 360f + seed.phase * 40f, pivot = Offset(x, y)) {
                        drawOval(
                            color = color,
                            topLeft = Offset(x - r, y - r * 0.55f),
                            size = Size(r * 2f, r * 1.1f)
                        )
                    }
                }
                "confetti" -> {
                    rotate(degrees = progress * 540f + seed.phase * 57f, pivot = Offset(x, y)) {
                        drawRect(
                            color = color.copy(alpha = 0.55f),
                            topLeft = Offset(x - r * 0.7f, y - r * 0.4f),
                            size = Size(r * 1.4f, r * 0.8f)
                        )
                    }
                }
                else -> drawSparkle(Offset(x, y), r, color)
            }
        }
    }
}

@Composable
fun ConfettiBurstOverlay(
    active: Boolean,
    modifier: Modifier = Modifier
) {
    if (!active) return
    val reducedMotion = rememberReducedMotion()
    val seeds = remember {
        val rng = Random(99)
        List(36) {
            ParticleSeed(
                xFraction = rng.nextFloat(),
                yOffset = rng.nextFloat(),
                speed = 0.8f + rng.nextFloat() * 1.2f,
                sizeDp = 6f + rng.nextFloat() * 8f,
                phase = rng.nextFloat() * 6.28f,
                colorIndex = rng.nextInt(5)
            )
        }
    }
    val palette = remember {
        listOf(
            Color(0xFFE11D48),
            Color(0xFF9333EA),
            Color(0xFFF59E0B),
            Color(0xFF10B981),
            Color(0xFFEC4899)
        )
    }
    val tick = if (reducedMotion) 0.5f else {
        val transition = rememberInfiniteTransition(label = "confetti")
        val anim by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(4500, easing = LinearEasing)),
            label = "confetti_tick"
        )
        anim
    }

    Canvas(modifier = modifier.fillMaxSize()) {
        val w = size.width
        val h = size.height
        seeds.forEach { s ->
            val y = ((s.yOffset + tick * s.speed) % 1f) * h
            val x = (s.xFraction * w + cos(tick * 6.28 + s.phase).toFloat() * 28f).coerceIn(0f, w)
            val sz = s.sizeDp * density
            rotate(tick * 720f + s.phase * 60f, pivot = Offset(x, y)) {
                drawRect(
                    color = palette[s.colorIndex].copy(alpha = 0.82f),
                    topLeft = Offset(x - sz / 2f, y - sz / 4f),
                    size = Size(sz, sz * 0.55f)
                )
            }
        }
    }
}

fun DrawScope.drawHeart(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        val topCurveHeight = radius * 0.35f
        moveTo(center.x, center.y + radius * 0.9f)
        cubicTo(
            center.x - radius * 1.4f, center.y + radius * 0.1f,
            center.x - radius * 0.9f, center.y - radius * 1.0f,
            center.x, center.y - topCurveHeight
        )
        cubicTo(
            center.x + radius * 0.9f, center.y - radius * 1.0f,
            center.x + radius * 1.4f, center.y + radius * 0.1f,
            center.x, center.y + radius * 0.9f
        )
        close()
    }
    drawPath(path, color = color)
}

fun DrawScope.drawSparkle(center: Offset, radius: Float, color: Color) {
    val path = Path().apply {
        moveTo(center.x, center.y - radius)
        quadraticTo(center.x, center.y, center.x + radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y + radius)
        quadraticTo(center.x, center.y, center.x - radius, center.y)
        quadraticTo(center.x, center.y, center.x, center.y - radius)
        close()
    }
    drawPath(path, color = color)
}

fun DrawScope.drawStar(center: Offset, radius: Float, color: Color) {
    val path = Path()
    val inner = radius * 0.45f
    for (i in 0 until 10) {
        val angle = (-PI / 2.0) + (i * PI / 5.0)
        val r = if (i % 2 == 0) radius else inner
        val px = center.x + (cos(angle) * r).toFloat()
        val py = center.y + (sin(angle) * r).toFloat()
        if (i == 0) path.moveTo(px, py) else path.lineTo(px, py)
    }
    path.close()
    drawPath(path, color = color)
}
