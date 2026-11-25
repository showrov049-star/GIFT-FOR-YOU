package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.util.ImageUtils

@Composable
fun Base64ImageView(
    base64: String,
    contentDescription: String,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    val imageBitmap = remember(base64) {
        ImageUtils.decodeBase64ToBitmap(base64)?.asImageBitmap()
    }
    if (imageBitmap != null) {
        Image(
            bitmap = imageBitmap,
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
    } else {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Favorite,
                contentDescription = contentDescription,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun InteractiveGiftBoxView(
    giftBoxStyle: String,
    isShaking: Boolean = false,
    isOpened: Boolean,
    onTapBox: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reducedMotion = rememberReducedMotion()
    val infinite = rememberInfiniteTransition(label = "gift_box_float")
    val bobY by infinite.animateFloat(
        initialValue = if (reducedMotion || isOpened) 0f else -8f,
        targetValue = if (reducedMotion || isOpened) 0f else 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob"
    )
    val idleWiggle by infinite.animateFloat(
        initialValue = if (reducedMotion || isOpened) 0f else -2.2f,
        targetValue = if (reducedMotion || isOpened) 0f else 2.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(720, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "wiggle"
    )
    val shakeWiggle by infinite.animateFloat(
        initialValue = -7f,
        targetValue = 7f,
        animationSpec = infiniteRepeatable(
            animation = tween(85, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake_wiggle"
    )

    val boxScale by animateFloatAsState(
        targetValue = when {
            isOpened -> 1.04f
            isShaking -> 1.07f
            else -> 1f
        },
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "box_scale"
    )
    val lidLift by animateDpAsState(
        targetValue = if (isOpened) (-64).dp else 0.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "lid_lift"
    )
    val lidRotate by animateFloatAsState(
        targetValue = if (isOpened) -18f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "lid_rot"
    )
    val glowAlpha by animateFloatAsState(
        targetValue = if (isOpened) 1f else if (isShaking) 0.55f else 0.25f,
        animationSpec = tween(550),
        label = "glow_alpha"
    )

    val (boxColor, boxDark, ribbonColor) = when (giftBoxStyle) {
        "lavender_silk" -> Triple(Color(0xFFC084FC), Color(0xFF9333EA), Color(0xFFFDF2F8))
        "golden_royal" -> Triple(Color(0xFFFBBF24), Color(0xFFD97706), Color(0xFFBE123C))
        else -> Triple(Color(0xFFFB7185), Color(0xFFE11D48), Color(0xFFFEF3C7))
    }

    Box(
        modifier = modifier
            .size(240.dp)
            .scale(boxScale)
            .offset(y = bobY.dp)
            .rotate(if (isShaking && !isOpened && !reducedMotion) shakeWiggle else idleWiggle)
            .semantics { contentDescription = "Tap the gift box to unwrap your surprise" }
            .clickable(enabled = !isShaking && !isOpened, onClick = onTapBox),
        contentAlignment = Alignment.Center
    ) {
        // Radiant glow behind gift box
        Box(
            modifier = Modifier
                .size(220.dp)
                .alpha(glowAlpha)
                .background(
                    Brush.radialGradient(
                        colors = listOf(Color(0xFFFDE68A), Color(0xFFFBCFE8).copy(alpha = 0.45f), Color.Transparent)
                    ),
                    shape = CircleShape
                )
        )

        Canvas(modifier = Modifier.size(190.dp)) {
            val w = size.width
            val h = size.height
            val bodyTop = h * 0.38f
            val bodyH = h * 0.54f
            val bodyW = w * 0.78f
            val bodyLeft = (w - bodyW) / 2f

            // Light burst emerging from inside when opened
            if (isOpened) {
                val beam = Path().apply {
                    moveTo(w * 0.24f, bodyTop)
                    lineTo(w * 0.04f, -h * 0.08f)
                    lineTo(w * 0.96f, -h * 0.08f)
                    lineTo(w * 0.76f, bodyTop)
                    close()
                }
                drawPath(
                    beam,
                    brush = Brush.verticalGradient(
                        listOf(Color(0xFFFEF08A).copy(alpha = 0.12f), Color(0xFFFDE047).copy(alpha = 0.85f))
                    )
                )
            }

            // Box body
            drawRoundRect(
                brush = Brush.verticalGradient(listOf(boxColor, boxDark)),
                topLeft = Offset(bodyLeft, bodyTop),
                size = Size(bodyW, bodyH),
                cornerRadius = CornerRadius(18f, 18f)
            )
            // Vertical ribbon
            drawRect(
                color = ribbonColor,
                topLeft = Offset(w * 0.44f, bodyTop),
                size = Size(w * 0.12f, bodyH)
            )
            // Horizontal ribbon
            drawRect(
                color = ribbonColor,
                topLeft = Offset(bodyLeft, bodyTop + bodyH * 0.42f),
                size = Size(bodyW, h * 0.09f)
            )
        }

        // Animated Lid + Ribbon Bow
        Canvas(
            modifier = Modifier
                .size(190.dp)
                .offset(y = lidLift)
                .rotate(lidRotate)
        ) {
            val w = size.width
            val h = size.height
            val lidW = w * 0.86f
            val lidLeft = (w - lidW) / 2f
            val lidTop = h * 0.24f
            val lidH = h * 0.16f

            drawRoundRect(
                brush = Brush.verticalGradient(listOf(boxColor.copy(alpha = 0.95f), boxDark)),
                topLeft = Offset(lidLeft, lidTop),
                size = Size(lidW, lidH),
                cornerRadius = CornerRadius(16f, 16f)
            )
            drawRect(
                color = ribbonColor,
                topLeft = Offset(w * 0.44f, lidTop),
                size = Size(w * 0.12f, lidH)
            )

            // Bow loops
            drawOval(
                color = ribbonColor,
                topLeft = Offset(w * 0.27f, lidTop - h * 0.11f),
                size = Size(w * 0.22f, h * 0.13f)
            )
            drawOval(
                color = ribbonColor,
                topLeft = Offset(w * 0.51f, lidTop - h * 0.11f),
                size = Size(w * 0.22f, h * 0.13f)
            )
            drawCircle(
                color = boxDark,
                radius = w * 0.055f,
                center = Offset(w * 0.50f, lidTop - h * 0.03f)
            )
        }
    }
}

@Composable
fun InteractiveEnvelopeView(
    envelopeStyle: String,
    recipientName: String,
    title: String,
    isOpened: Boolean,
    onTapEnvelope: () -> Unit,
    modifier: Modifier = Modifier
) {
    val flapScale by animateFloatAsState(
        targetValue = if (isOpened) -0.92f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "flap"
    )
    val letterLift by animateDpAsState(
        targetValue = if (isOpened) (-28).dp else 6.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "letter_lift"
    )

    val (envColor, sealColor, sealText) = when (envelopeStyle) {
        "ribbon_fold" -> Triple(Color(0xFFFDF2F8), Color(0xFF9333EA), "🎀")
        "starry_mail" -> Triple(Color(0xFFEDE9FE), Color(0xFFF59E0B), "✨")
        else -> Triple(Color(0xFFFFF8F3), Color(0xFFE11D48), "❤️")
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(255.dp)
            .clickable(enabled = !isOpened, onClick = onTapEnvelope),
        contentAlignment = Alignment.Center
    ) {
        // Rising letter card
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.84f)
                .height(175.dp)
                .offset(y = letterLift)
                .shadow(10.dp, RoundedCornerShape(20.dp)),
            shape = RoundedCornerShape(20.dp),
            color = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(18.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Icon(
                    imageVector = if (isOpened) Icons.Default.MarkEmailRead else Icons.Default.CardGiftcard,
                    contentDescription = "Envelope letter",
                    tint = sealColor,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = "For $recipientName",
                    style = MaterialTheme.typography.labelLarge,
                    color = sealColor
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )
            }
        }

        // Envelope pocket & flap
        Box(
            modifier = Modifier
                .fillMaxWidth(0.92f)
                .height(160.dp)
                .align(Alignment.BottomCenter)
                .clip(RoundedCornerShape(22.dp))
                .background(envColor)
                .border(1.5.dp, sealColor.copy(alpha = 0.25f), RoundedCornerShape(22.dp)),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(scaleX = 1f, scaleY = flapScale)
            ) {
                val path = Path().apply {
                    moveTo(0f, 0f)
                    lineTo(size.width / 2f, size.height * 0.55f)
                    lineTo(size.width, 0f)
                    close()
                }
                drawPath(path, color = sealColor.copy(alpha = 0.14f))
            }

            if (!isOpened) {
                Surface(
                    shape = CircleShape,
                    color = sealColor,
                    shadowElevation = 6.dp,
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(sealText, style = MaterialTheme.typography.titleLarge)
                    }
                }
            }
        }
    }
}
