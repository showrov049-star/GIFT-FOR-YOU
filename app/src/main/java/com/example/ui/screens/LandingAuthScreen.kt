package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Games
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.CategoryCatalog
import com.example.ui.components.FloatingAmbientParticles
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.LavenderSoft
import com.example.ui.theme.PlumInk
import com.example.ui.theme.PlumMuted
import com.example.ui.theme.RoseBlush
import com.example.ui.theme.RosePrimary
import com.example.ui.theme.RoseSoft

@Composable
fun LandingAuthScreen(
    isSigningIn: Boolean,
    authError: String?,
    onNavigateToLogin: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onOpenGiftCodeClick: () -> Unit
) {

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(RoseBlush, Color(0xFFFDF8FF), Color(0xFFFFFBF7))
                )
            )
    ) {
        FloatingAmbientParticles(
            effect = "hearts",
            primaryColor = RosePrimary,
            secondaryColor = LavenderPrimary,
            count = 18
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // Top Brand Pill
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = CircleShape,
                            color = RosePrimary,
                            modifier = Modifier.size(38.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.CardGiftcard, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = stringResource(R.string.app_name),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = PlumInk
                            )
                            Text(
                                text = stringResource(R.string.app_tagline),
                                style = MaterialTheme.typography.labelSmall,
                                color = PlumMuted
                            )
                        }
                    }

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TextButton(
                            onClick = onNavigateToLogin,
                            modifier = Modifier.testTag("top_sign_in_button")
                        ) {
                            Text("Sign In", color = RosePrimary, fontWeight = FontWeight.SemiBold)
                        }
                        FilledTonalButton(
                            onClick = onOpenGiftCodeClick,
                            modifier = Modifier.testTag("top_open_gift_button")
                        ) {
                            Icon(Icons.Default.Redeem, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Open Gift")
                        }
                    }
                }
            }

            // Hero Card
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                        .shadow(14.dp, RoundedCornerShape(30.dp), spotColor = RoseSoft.copy(alpha = 0.4f)),
                    shape = RoundedCornerShape(30.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Image(
                            painter = painterResource(id = R.drawable.img_hero_gift),
                            contentDescription = "Glowing romantic gift box illustration",
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(190.dp)
                                .clip(RoundedCornerShape(22.dp)),
                            contentScale = ContentScale.Crop
                        )

                        Spacer(Modifier.height(18.dp))

                        Surface(
                            shape = RoundedCornerShape(50),
                            color = LavenderSoft
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.AutoAwesome, null, tint = LavenderPrimary, modifier = Modifier.size(14.dp))
                                Spacer(Modifier.width(5.dp))
                                Text(
                                    text = "Personalized Surprises + Mini-Games",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = LavenderPrimary
                                )
                            }
                        }

                        Spacer(Modifier.height(12.dp))

                        Text(
                            text = "Make Someone's Day Extra Special ❤️",
                            style = MaterialTheme.typography.displaySmall,
                            color = PlumInk,
                            textAlign = TextAlign.Center
                        )

                        Spacer(Modifier.height(8.dp))

                        Text(
                            text = stringResource(R.string.hero_subheading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = PlumMuted,
                            textAlign = TextAlign.Center
                        )

                        if (!authError.isNullOrBlank()) {
                            Spacer(Modifier.height(10.dp))
                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = MaterialTheme.colorScheme.errorContainer,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = authError,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    modifier = Modifier.padding(12.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = onNavigateToRegister,
                            enabled = !isSigningIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("create_account_cta_button"),
                            shape = RoundedCornerShape(18.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = RosePrimary)
                        ) {
                            Icon(Icons.Default.Favorite, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.cta_create_surprise),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold
                            )
                        }

                        Spacer(Modifier.height(10.dp))

                        OutlinedButton(
                            onClick = onNavigateToLogin,
                            enabled = !isSigningIn,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("sign_in_cta_button"),
                            shape = RoundedCornerShape(18.dp)
                        ) {
                            Text("Sign In to My Account", color = RosePrimary, fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(Modifier.height(4.dp))

                        TextButton(
                            onClick = onOpenGiftCodeClick,
                            modifier = Modifier.testTag("open_gift_code_button")
                        ) {
                            Icon(Icons.Default.Redeem, null, Modifier.size(18.dp), tint = LavenderPrimary)
                            Spacer(Modifier.width(8.dp))
                            Text("Received a Gift? Open With Code", color = LavenderPrimary)
                        }
                    }
                }
            }

            // Section A: How It Works
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "How It Works",
                        style = MaterialTheme.typography.headlineMedium,
                        color = PlumInk
                    )
                    val steps = listOf(
                        "1. Choose a Category" to "Birthday, Love Letter, Friendship & 13 more themes",
                        "2. Craft Your Surprise" to "Rich letter, polaroid photos, voice note & music",
                        "3. Add Fun Mini-Games" to "8 interactive games to unlock the gift box",
                        "4. Share Unique Link or QR" to "Send via WhatsApp, link, or scannable QR card",
                        "5. Make Someone Smile" to "Watch reactions & heartfelt replies arrive live"
                    )
                    steps.forEachIndexed { idx, (title, desc) ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = Color.White,
                            shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = CircleShape,
                                    color = RoseBlush,
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Text("${idx + 1}", fontWeight = FontWeight.Bold, color = RosePrimary)
                                    }
                                }
                                Spacer(Modifier.width(12.dp))
                                Column {
                                    Text(title, style = MaterialTheme.typography.titleSmall, color = PlumInk)
                                    Text(desc, style = MaterialTheme.typography.bodySmall, color = PlumMuted)
                                }
                            }
                        }
                    }
                }
            }

            // Section B: Popular Categories
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Popular Categories",
                        style = MaterialTheme.typography.headlineMedium,
                        color = PlumInk
                    )
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(CategoryCatalog.all.take(8)) { cat ->
                            Surface(
                                shape = RoundedCornerShape(22.dp),
                                color = Color.White,
                                shadowElevation = 4.dp,
                                modifier = Modifier
                                    .width(156.dp)
                                    .border(1.dp, Color(cat.accentHex).copy(alpha = 0.25f), RoundedCornerShape(22.dp))
                                    .clickable { onNavigateToRegister() }
                            ) {
                                Column(modifier = Modifier.padding(14.dp)) {
                                    Text(cat.emoji, style = MaterialTheme.typography.headlineMedium)
                                    Spacer(Modifier.height(6.dp))
                                    Text(cat.name, style = MaterialTheme.typography.titleSmall, color = PlumInk, maxLines = 1)
                                    Spacer(Modifier.height(2.dp))
                                    Text(cat.subtitle, style = MaterialTheme.typography.bodySmall, color = PlumMuted, maxLines = 2)
                                }
                            }
                        }
                    }
                }
            }

            // Section C: Why Gift For You?
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "Why Gift For You?",
                        style = MaterialTheme.typography.headlineMedium,
                        color = PlumInk
                    )
                    FeatureHighlightRow(Icons.Default.Favorite, "Personalized Gifts", "Tailored themes, typography, envelopes, and music.")
                    FeatureHighlightRow(Icons.Default.Games, "Interactive Mini-Games", "Playful challenges make unwrapping unforgettable.")
                    FeatureHighlightRow(Icons.Default.PhotoLibrary, "Beautiful Memories", "Polaroid photo slideshows & warm voice notes.")
                    FeatureHighlightRow(Icons.Default.Share, "Effortless Sharing", "Unique links, QR codes & WhatsApp ready.")
                    FeatureHighlightRow(Icons.Default.MarkEmailUnread, "Personal Replies", "Recipients send reactions & notes without signing up.")
                }
            }

            // Section D: Final CTA
            item {
                Surface(
                    shape = RoundedCornerShape(26.dp),
                    color = RosePrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "Someone deserves a surprise today!",
                            style = MaterialTheme.typography.headlineMedium,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onNavigateToRegister,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = RosePrimary),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Create a Surprise", fontWeight = FontWeight.Bold)
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(18.dp))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
fun GiftCodeDialog(
    onDismiss: () -> Unit,
    onOpenGiftCode: (String) -> Unit
) {
    var giftCodeInput by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Open Your Surprise 🎁") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "Paste the Surprise Link or enter the Gift Code (e.g. gfy_...) shared with you:",
                    style = MaterialTheme.typography.bodyMedium
                )
                OutlinedTextField(
                    value = giftCodeInput,
                    onValueChange = { giftCodeInput = it },
                    label = { Text("Gift Code or Link") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("gift_code_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (giftCodeInput.isNotBlank()) {
                        onDismiss()
                        onOpenGiftCode(giftCodeInput.trim())
                    }
                },
                enabled = giftCodeInput.isNotBlank()
            ) {
                Text("Unwrap Gift")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun FeatureHighlightRow(
    icon: ImageVector,
    title: String,
    subtitle: String
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.92f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = LavenderSoft, modifier = Modifier.size(40.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(icon, null, tint = LavenderPrimary, modifier = Modifier.size(20.dp))
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall, color = PlumInk)
                Text(subtitle, style = MaterialTheme.typography.bodySmall, color = PlumMuted)
            }
        }
    }
}
