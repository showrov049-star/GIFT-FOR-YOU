package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.ui.components.FloatingAmbientParticles
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.LavenderSoft
import com.example.ui.theme.PlumInk
import com.example.ui.theme.PlumMuted
import com.example.ui.theme.RoseBlush
import com.example.ui.theme.RosePrimary
import com.example.ui.theme.RoseSoft

enum class AuthDestination {
    LANDING,
    LOGIN,
    REGISTER
}

@Composable
fun LoginScreen(
    isSigningIn: Boolean,
    authError: String?,
    onGoogleSignInClick: () -> Unit,
    onNavigateToRegister: () -> Unit,
    onOpenGiftCodeClick: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(enabled = !isSigningIn, onBack = onBack)

    AuthScaffoldShell(
        title = stringResource(R.string.auth_login_title),
        onBack = onBack,
        backTestTag = "login_back_button"
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .shadow(14.dp, RoundedCornerShape(28.dp), spotColor = RoseSoft.copy(alpha = 0.45f)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AuthBrandHeader(
                    badgeText = stringResource(R.string.auth_secure_badge),
                    heading = stringResource(R.string.auth_welcome_back),
                    subtitle = stringResource(R.string.auth_login_subtitle)
                )

                AuthErrorBanner(authError)

                Spacer(Modifier.height(22.dp))

                GoogleAuthActionButton(
                    text = if (isSigningIn) {
                        stringResource(R.string.auth_signing_in)
                    } else {
                        stringResource(R.string.sign_in_google)
                    },
                    isLoading = isSigningIn,
                    isPrimaryFilled = true,
                    testTag = "login_google_button",
                    onClick = onGoogleSignInClick
                )

                Spacer(Modifier.height(14.dp))

                AuthProviderInfoCard(
                    title = stringResource(R.string.auth_google_only_title),
                    body = stringResource(R.string.auth_google_only_note)
                )

                Spacer(Modifier.height(18.dp))
                HorizontalDivider(color = RoseBlush)
                Spacer(Modifier.height(14.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.auth_no_account_prompt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = PlumMuted
                    )
                    TextButton(
                        onClick = onNavigateToRegister,
                        enabled = !isSigningIn,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("go_to_register_button")
                    ) {
                        Text(
                            text = stringResource(R.string.auth_create_account),
                            color = RosePrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                TextButton(
                    onClick = onOpenGiftCodeClick,
                    enabled = !isSigningIn,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("login_open_gift_code_button")
                ) {
                    Icon(Icons.Default.Redeem, null, modifier = Modifier.size(16.dp), tint = LavenderPrimary)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = stringResource(R.string.auth_recipient_shortcut),
                        color = LavenderPrimary,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }
    }
}

@Composable
fun RegistrationScreen(
    isSigningIn: Boolean,
    authError: String?,
    onGoogleSignUpClick: (preferredDisplayName: String?) -> Unit,
    onNavigateToLogin: () -> Unit,
    onBack: () -> Unit
) {
    BackHandler(enabled = !isSigningIn, onBack = onBack)

    var preferredName by remember { mutableStateOf("") }

    AuthScaffoldShell(
        title = stringResource(R.string.auth_register_title),
        onBack = onBack,
        backTestTag = "register_back_button"
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = 500.dp)
                .shadow(14.dp, RoundedCornerShape(28.dp), spotColor = LavenderSoft.copy(alpha = 0.55f)),
            shape = RoundedCornerShape(28.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                AuthBrandHeader(
                    badgeText = stringResource(R.string.auth_creator_badge),
                    heading = stringResource(R.string.auth_create_account_heading),
                    subtitle = stringResource(R.string.auth_register_subtitle)
                )

                Spacer(Modifier.height(18.dp))

                OutlinedTextField(
                    value = preferredName,
                    onValueChange = { if (it.length <= 60) preferredName = it },
                    enabled = !isSigningIn,
                    singleLine = true,
                    label = { Text(stringResource(R.string.auth_display_name_label)) },
                    placeholder = { Text(stringResource(R.string.auth_display_name_placeholder)) },
                    leadingIcon = { Icon(Icons.Default.Person, null, tint = RosePrimary) },
                    supportingText = {
                        Text(
                            text = stringResource(R.string.auth_display_name_helper),
                            style = MaterialTheme.typography.labelSmall,
                            color = PlumMuted
                        )
                    },
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Words,
                        imeAction = ImeAction.Done
                    ),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("register_display_name_input")
                )

                Spacer(Modifier.height(12.dp))

                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = RoseBlush.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        RegistrationPerkItem(
                            icon = Icons.Default.CheckCircle,
                            text = stringResource(R.string.auth_perk_sync)
                        )
                        RegistrationPerkItem(
                            icon = Icons.Default.QrCode2,
                            text = stringResource(R.string.auth_perk_share)
                        )
                        RegistrationPerkItem(
                            icon = Icons.Default.NotificationsActive,
                            text = stringResource(R.string.auth_perk_replies)
                        )
                    }
                }

                AuthErrorBanner(authError)

                Spacer(Modifier.height(18.dp))

                GoogleAuthActionButton(
                    text = if (isSigningIn) {
                        stringResource(R.string.auth_creating_account)
                    } else {
                        stringResource(R.string.auth_signup_google)
                    },
                    isLoading = isSigningIn,
                    isPrimaryFilled = true,
                    testTag = "register_google_button",
                    onClick = {
                        onGoogleSignUpClick(preferredName.trim().takeIf { it.isNotBlank() })
                    }
                )

                Spacer(Modifier.height(12.dp))

                AuthProviderInfoCard(
                    title = stringResource(R.string.auth_google_only_title),
                    body = stringResource(R.string.auth_google_only_note)
                )

                Spacer(Modifier.height(16.dp))
                HorizontalDivider(color = RoseBlush)
                Spacer(Modifier.height(12.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = stringResource(R.string.auth_have_account_prompt),
                        style = MaterialTheme.typography.bodyMedium,
                        color = PlumMuted
                    )
                    TextButton(
                        onClick = onNavigateToLogin,
                        enabled = !isSigningIn,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("go_to_login_button")
                    ) {
                        Text(
                            text = stringResource(R.string.auth_sign_in_link),
                            color = RosePrimary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AuthScaffoldShell(
    title: String,
    onBack: () -> Unit,
    backTestTag: String,
    content: @Composable () -> Unit
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
            effect = "sparkles",
            primaryColor = RosePrimary,
            secondaryColor = LavenderPrimary,
            count = 14
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding(),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 500.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag(backTestTag)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = PlumInk
                        )
                    }
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = PlumInk
                    )
                }
            }

            item {
                content()
                Spacer(Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun AuthBrandHeader(
    badgeText: String,
    heading: String,
    subtitle: String
) {
    Surface(
        shape = CircleShape,
        color = RosePrimary,
        shadowElevation = 6.dp,
        modifier = Modifier.size(62.dp)
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.background(
                Brush.linearGradient(listOf(RosePrimary, LavenderPrimary))
            )
        ) {
            Icon(
                imageVector = Icons.Default.CardGiftcard,
                contentDescription = stringResource(R.string.app_name),
                tint = Color.White,
                modifier = Modifier.size(30.dp)
            )
        }
    }

    Spacer(Modifier.height(14.dp))

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
                text = badgeText,
                style = MaterialTheme.typography.labelMedium,
                color = LavenderPrimary
            )
        }
    }

    Spacer(Modifier.height(12.dp))

    Text(
        text = heading,
        style = MaterialTheme.typography.displaySmall,
        color = PlumInk,
        textAlign = TextAlign.Center
    )

    Spacer(Modifier.height(6.dp))

    Text(
        text = subtitle,
        style = MaterialTheme.typography.bodyMedium,
        color = PlumMuted,
        textAlign = TextAlign.Center
    )
}

@Composable
private fun AuthErrorBanner(authError: String?) {
    if (!authError.isNullOrBlank()) {
        Spacer(Modifier.height(14.dp))
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
}

@Composable
private fun GoogleAuthActionButton(
    text: String,
    isLoading: Boolean,
    isPrimaryFilled: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    if (isPrimaryFilled) {
        Button(
            onClick = onClick,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag(testTag),
            shape = RoundedCornerShape(18.dp),
            colors = ButtonDefaults.buttonColors(containerColor = RosePrimary)
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.5.dp
                )
                Spacer(Modifier.width(10.dp))
                Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            } else {
                Surface(
                    shape = CircleShape,
                    color = Color.White,
                    modifier = Modifier.size(26.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "G",
                            color = RosePrimary,
                            fontWeight = FontWeight.ExtraBold,
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                Spacer(Modifier.width(10.dp))
                Text(text, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            }
        }
    } else {
        OutlinedButton(
            onClick = onClick,
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag(testTag),
            shape = RoundedCornerShape(18.dp),
            border = BorderStroke(1.2.dp, RosePrimary.copy(alpha = 0.45f))
        ) {
            Icon(Icons.Default.Lock, null, tint = RosePrimary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text(text, color = RosePrimary, style = MaterialTheme.typography.titleSmall)
        }
    }
}

@Composable
private fun RegistrationPerkItem(icon: ImageVector, text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, null, tint = RosePrimary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(10.dp))
        Text(text, style = MaterialTheme.typography.bodySmall, color = PlumInk)
    }
}

@Composable
private fun AuthProviderInfoCard(title: String, body: String) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = LavenderSoft.copy(alpha = 0.55f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = LavenderPrimary,
                modifier = Modifier.size(18.dp).padding(top = 2.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = PlumInk
                )
                Text(
                    text = body,
                    style = MaterialTheme.typography.bodySmall,
                    color = PlumMuted
                )
            }
        }
    }
}
