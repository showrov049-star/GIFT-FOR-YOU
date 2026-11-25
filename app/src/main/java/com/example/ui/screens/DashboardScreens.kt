package com.example.ui.screens

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileCopy
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Redeem
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.audio.GiftAudioEngine
import com.example.data.audio.SoundSettingsStore
import com.example.data.model.AppNotification
import com.example.data.model.CategoryCatalog
import com.example.data.model.Surprise
import com.example.data.model.SurpriseReply
import com.example.ui.components.ShareSurpriseDialog
import com.example.ui.components.SoundSettingsDialog
import com.example.ui.components.copyToClipboard
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.LavenderPrimary
import com.example.ui.theme.LavenderSoft
import com.example.ui.theme.PlumInk
import com.example.ui.theme.PlumMuted
import com.example.ui.theme.RoseBlush
import com.example.ui.theme.RosePrimary
import com.example.ui.viewmodel.DashboardViewModel
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardShellScreen(
    viewModel: DashboardViewModel,
    userDisplayName: String,
    userEmail: String,
    userPhotoUrl: String?,
    onCreateSurprise: () -> Unit,
    onEditSurprise: (Surprise) -> Unit,
    onPreviewSurprise: (Surprise) -> Unit,
    onOpenGiftCode: (String) -> Unit,
    onSignOut: () -> Unit
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    var showProfileSheet by remember { mutableStateOf(false) }
    var showSoundDialog by remember { mutableStateOf(false) }
    var showOpenGiftDialog by remember { mutableStateOf(false) }
    var openGiftInput by remember { mutableStateOf("") }
    var shareTarget by remember { mutableStateOf<Surprise?>(null) }
    var privacyTarget by remember { mutableStateOf<Surprise?>(null) }
    var deleteTarget by remember { mutableStateOf<Surprise?>(null) }

    val context = LocalContext.current
    val soundPreviewEngine = remember { GiftAudioEngine() }

    LaunchedEffect(Unit) {
        SoundSettingsStore.loadIfNeeded(context)
    }
    DisposableEffect(Unit) {
        onDispose { soundPreviewEngine.release() }
    }

    val surprisesState by viewModel.surprisesState.collectAsStateWithLifecycle()
    val repliesState by viewModel.repliesState.collectAsStateWithLifecycle()
    val notificationsState by viewModel.notificationsState.collectAsStateWithLifecycle()
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val toastMsg by viewModel.toastMessage.collectAsStateWithLifecycle()

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(toastMsg) {
        if (!toastMsg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(toastMsg!!)
            viewModel.clearToast()
        }
    }

    val surprises = (surprisesState as? UiState.Success)?.data ?: emptyList()
    val replies = (repliesState as? UiState.Success)?.data ?: emptyList()
    val notifications = (notificationsState as? UiState.Success)?.data ?: emptyList()

    val unreadReplies = replies.count { !it.readFlag }
    val unreadNotifications = notifications.count { !it.readFlag }
    val activeName = userProfile?.displayName?.takeIf { it.isNotBlank() } ?: userDisplayName.ifBlank { "Friend" }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val useRail = maxWidth >= 600.dp

        Row(modifier = Modifier.fillMaxSize()) {
            if (useRail) {
                NavigationRail(
                    containerColor = Color.White,
                    modifier = Modifier.fillMaxHeight().statusBarsPadding()
                ) {
                    Spacer(Modifier.height(12.dp))
                    NavigationRailItem(
                        selected = selectedTab == 0,
                        onClick = { selectedTab = 0 },
                        icon = { Icon(Icons.Default.Home, "Home") },
                        label = { Text("Home") }
                    )
                    NavigationRailItem(
                        selected = selectedTab == 1,
                        onClick = { selectedTab = 1 },
                        icon = { Icon(Icons.Default.CardGiftcard, "Surprises") },
                        label = { Text("Surprises") }
                    )
                    NavigationRailItem(
                        selected = selectedTab == 2,
                        onClick = { selectedTab = 2 },
                        icon = {
                            BadgedBox(badge = { if (unreadReplies > 0) Badge { Text("$unreadReplies") } }) {
                                Icon(Icons.Default.MarkEmailUnread, "Replies")
                            }
                        },
                        label = { Text("Replies") }
                    )
                    NavigationRailItem(
                        selected = selectedTab == 3,
                        onClick = { selectedTab = 3 },
                        icon = {
                            BadgedBox(badge = { if (unreadNotifications > 0) Badge { Text("$unreadNotifications") } }) {
                                Icon(Icons.Default.Notifications, "Alerts")
                            }
                        },
                        label = { Text("Alerts") }
                    )
                }
            }

            Scaffold(
                containerColor = CreamBackground,
                snackbarHost = { SnackbarHost(snackbarHostState) },
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                topBar = {
                    TopAppBar(
                        title = {
                            Text(
                                text = when (selectedTab) {
                                    0 -> "Gift For You"
                                    1 -> "My Surprises"
                                    2 -> "Received Replies"
                                    else -> "Notifications"
                                },
                                style = MaterialTheme.typography.titleLarge
                            )
                        },
                        actions = {
                            IconButton(
                                onClick = { showOpenGiftDialog = true },
                                modifier = Modifier.testTag("dashboard_open_gift_button")
                            ) {
                                Icon(Icons.Default.Redeem, contentDescription = "Open Gift Code", tint = RosePrimary)
                            }
                            IconButton(
                                onClick = { showSoundDialog = true },
                                modifier = Modifier.testTag("dashboard_sound_settings_button")
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = "Sound Settings", tint = RosePrimary)
                            }
                            IconButton(
                                onClick = { showProfileSheet = true },
                                modifier = Modifier.testTag("profile_settings_button")
                            ) {
                                Icon(Icons.Default.Person, contentDescription = "Profile and Privacy Settings")
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
                    )
                },
                floatingActionButton = {
                    FloatingActionButton(
                        onClick = onCreateSurprise,
                        containerColor = RosePrimary,
                        contentColor = Color.White,
                        modifier = Modifier.testTag("fab_create_surprise")
                    ) {
                        Icon(Icons.Default.Add, contentDescription = "Create New Surprise")
                    }
                },
                bottomBar = {
                    if (!useRail) {
                        NavigationBar(
                            containerColor = Color.White,
                            modifier = Modifier.navigationBarsPadding()
                        ) {
                            NavigationBarItem(
                                selected = selectedTab == 0,
                                onClick = { selectedTab = 0 },
                                icon = { Icon(Icons.Default.Home, "Home") },
                                label = { Text("Home") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 1,
                                onClick = { selectedTab = 1 },
                                icon = { Icon(Icons.Default.CardGiftcard, "Surprises") },
                                label = { Text("Surprises") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 2,
                                onClick = { selectedTab = 2 },
                                icon = {
                                    BadgedBox(badge = { if (unreadReplies > 0) Badge { Text("$unreadReplies") } }) {
                                        Icon(Icons.Default.MarkEmailUnread, "Replies")
                                    }
                                },
                                label = { Text("Replies") }
                            )
                            NavigationBarItem(
                                selected = selectedTab == 3,
                                onClick = { selectedTab = 3 },
                                icon = {
                                    BadgedBox(badge = { if (unreadNotifications > 0) Badge { Text("$unreadNotifications") } }) {
                                        Icon(Icons.Default.Notifications, "Alerts")
                                    }
                                },
                                label = { Text("Alerts") }
                            )
                        }
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (selectedTab) {
                        0 -> DashboardHomeTab(
                            userName = activeName,
                            surprisesState = surprisesState,
                            replies = replies,
                            notifications = notifications,
                            onCreateSurprise = onCreateSurprise,
                            onNavigateTab = { selectedTab = it },
                            onOpenGiftCodeClick = { showOpenGiftDialog = true },
                            onEditSurprise = onEditSurprise,
                            onPreviewSurprise = onPreviewSurprise,
                            onShareSurprise = { shareTarget = it },
                            onDuplicateSurprise = { viewModel.duplicateSurprise(it) },
                            onManagePrivacy = { privacyTarget = it },
                            onDeleteSurprise = { deleteTarget = it }
                        )
                        1 -> MySurprisesTab(
                            surprisesState = surprisesState,
                            onCreateSurprise = onCreateSurprise,
                            onEditSurprise = onEditSurprise,
                            onPreviewSurprise = onPreviewSurprise,
                            onShareSurprise = { shareTarget = it },
                            onDuplicateSurprise = { viewModel.duplicateSurprise(it) },
                            onManagePrivacy = { privacyTarget = it },
                            onDeleteSurprise = { deleteTarget = it }
                        )
                        2 -> RepliesInboxTab(
                            repliesState = repliesState,
                            onMarkRead = { viewModel.markReplyRead(it) }
                        )
                        3 -> NotificationsTab(
                            notificationsState = notificationsState,
                            onMarkRead = { viewModel.markNotificationRead(it) },
                            onMarkAllRead = { viewModel.markAllNotificationsRead(notifications) }
                        )
                    }
                }
            }
        }
    }

    if (shareTarget != null) {
        ShareSurpriseDialog(
            surprise = shareTarget!!,
            isNewlyPublished = false,
            onPreviewGift = {
                val target = shareTarget!!
                shareTarget = null
                onPreviewSurprise(target)
            },
            onDismiss = { shareTarget = null }
        )
    }

    if (privacyTarget != null) {
        PrivacyManageDialog(
            surprise = privacyTarget!!,
            onSave = { pass, unlockAt, expiresAt, status ->
                viewModel.updatePrivacy(privacyTarget!!.id, pass, unlockAt, expiresAt, status)
                privacyTarget = null
            },
            onDismiss = { privacyTarget = null }
        )
    }

    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { deleteTarget = null },
            title = { Text("Delete Surprise?") },
            text = { Text("Are you sure you want to delete \"${deleteTarget!!.title}\"? Recipients will no longer be able to open its link.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSurprise(deleteTarget!!.id)
                        deleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { deleteTarget = null }) { Text("Cancel") }
            }
        )
    }

    if (showOpenGiftDialog) {
        AlertDialog(
            onDismissRequest = { showOpenGiftDialog = false },
            title = { Text("Open a Gift Code 🎁") },
            text = {
                OutlinedTextField(
                    value = openGiftInput,
                    onValueChange = { openGiftInput = it },
                    label = { Text("Paste Surprise Link or Code") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (openGiftInput.isNotBlank()) {
                            showOpenGiftDialog = false
                            onOpenGiftCode(openGiftInput.trim())
                            openGiftInput = ""
                        }
                    },
                    enabled = openGiftInput.isNotBlank()
                ) {
                    Text("Open Surprise")
                }
            },
            dismissButton = {
                TextButton(onClick = { showOpenGiftDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showProfileSheet) {
        ProfilePrivacySettingsDialog(
            profile = userProfile,
            fallbackName = activeName,
            fallbackEmail = userEmail,
            photoUrl = userPhotoUrl,
            onOpenSoundSettings = {
                showProfileSheet = false
                showSoundDialog = true
            },
            onSave = { name, onOpen, onReply, defaultSkip ->
                viewModel.updateProfileSettings(name, onOpen, onReply, defaultSkip)
                showProfileSheet = false
            },
            onSignOut = {
                showProfileSheet = false
                onSignOut()
            },
            onDismiss = { showProfileSheet = false }
        )
    }

    if (showSoundDialog) {
        SoundSettingsDialog(
            audioEngine = soundPreviewEngine,
            onDismiss = { showSoundDialog = false }
        )
    }
}

@Composable
private fun DashboardHomeTab(
    userName: String,
    surprisesState: UiState<List<Surprise>>,
    replies: List<SurpriseReply>,
    notifications: List<AppNotification>,
    onCreateSurprise: () -> Unit,
    onNavigateTab: (Int) -> Unit,
    onOpenGiftCodeClick: () -> Unit,
    onEditSurprise: (Surprise) -> Unit,
    onPreviewSurprise: (Surprise) -> Unit,
    onShareSurprise: (Surprise) -> Unit,
    onDuplicateSurprise: (Surprise) -> Unit,
    onManagePrivacy: (Surprise) -> Unit,
    onDeleteSurprise: (Surprise) -> Unit
) {
    val surprises = (surprisesState as? UiState.Success)?.data ?: emptyList()
    val totalOpens = surprises.sumOf { it.openCount }
    val totalReplies = replies.size
    val unreadReplies = replies.count { !it.readFlag }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header
        item {
            Card(
                shape = RoundedCornerShape(26.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(listOf(RosePrimary, LavenderPrimary))
                        )
                        .padding(20.dp)
                ) {
                    Text(
                        text = "Welcome back, $userName! ✨",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = "What happiness are we creating today?",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onCreateSurprise,
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = RosePrimary),
                            modifier = Modifier.testTag("home_create_surprise_button")
                        ) {
                            Icon(Icons.Default.Add, null, Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Create Surprise", fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick = onOpenGiftCodeClick,
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                        ) {
                            Text("Open Code")
                        }
                    }
                }
            }
        }

        // 4 Statistics Cards
        item {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("Total Surprises", "${surprises.size}", "🎁", RoseBlush, RosePrimary, Modifier.weight(1f))
                    StatCard("Total Gift Opens", "$totalOpens", "👀", LavenderSoft, LavenderPrimary, Modifier.weight(1f))
                }
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                    StatCard("Total Replies", "$totalReplies", "💌", Color(0xFFD1FAE5), Color(0xFF059669), Modifier.weight(1f))
                    StatCard("Unread Replies", "$unreadReplies", "✨", Color(0xFFFEF3C7), Color(0xFFD97706), Modifier.weight(1f))
                }
            }
        }

        // Quick Actions
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                item {
                    FilledTonalButton(onClick = onCreateSurprise) { Text("➕ New Surprise") }
                }
                item {
                    FilledTonalButton(onClick = { onNavigateTab(1) }) { Text("🎁 My Surprises") }
                }
                item {
                    FilledTonalButton(onClick = { onNavigateTab(2) }) { Text("💌 Replies ($totalReplies)") }
                }
                item {
                    FilledTonalButton(onClick = { onNavigateTab(3) }) { Text("🔔 Alerts") }
                }
            }
        }

        // My Recent Surprises
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("My Recent Surprises", style = MaterialTheme.typography.titleLarge, color = PlumInk)
                if (surprises.isNotEmpty()) {
                    TextButton(onClick = { onNavigateTab(1) }) { Text("See All") }
                }
            }
        }

        when (surprisesState) {
            is UiState.Loading -> {
                items(2) {
                    SurpriseSkeletonCard()
                }
            }
            is UiState.Error -> item {
                Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.errorContainer) {
                    Text(surprisesState.message, modifier = Modifier.padding(16.dp), color = MaterialTheme.colorScheme.onErrorContainer)
                }
            }
            is UiState.Success -> {
                if (surprises.isEmpty()) {
                    item {
                        Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
                            Column(
                                modifier = Modifier.fillMaxWidth().padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("🎁", style = MaterialTheme.typography.displaySmall)
                                Spacer(Modifier.height(8.dp))
                                Text("No surprises yet!", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Create your first personalized gift with mini-games, photos, and music.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = PlumMuted
                                )
                                Spacer(Modifier.height(12.dp))
                                Button(onClick = onCreateSurprise, colors = ButtonDefaults.buttonColors(containerColor = RosePrimary)) {
                                    Text("Create Your First Surprise")
                                }
                            }
                        }
                    }
                } else {
                    items(surprises.take(4), key = { it.id }) { surprise ->
                        SurpriseSummaryCard(
                            surprise = surprise,
                            onEdit = { onEditSurprise(surprise) },
                            onPreview = { onPreviewSurprise(surprise) },
                            onShare = { onShareSurprise(surprise) },
                            onDuplicate = { onDuplicateSurprise(surprise) },
                            onPrivacy = { onManagePrivacy(surprise) },
                            onDelete = { onDeleteSurprise(surprise) }
                        )
                    }
                }
            }
        }

        // Recent Activity
        if (notifications.isNotEmpty()) {
            item {
                Text("Recent Activity", style = MaterialTheme.typography.titleLarge, color = PlumInk)
            }
            items(notifications.take(3), key = { it.id }) { notif ->
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onNavigateTab(if (notif.type == "replied") 2 else 3) }
                ) {
                    Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text(if (notif.type == "replied") "💌" else "🎉", style = MaterialTheme.typography.titleLarge)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(notif.title, style = MaterialTheme.typography.titleSmall)
                            Text(notif.message, style = MaterialTheme.typography.bodySmall, color = PlumMuted, maxLines = 2)
                        }
                    }
                }
            }
        }

        item { Spacer(Modifier.height(72.dp)) }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    emoji: String,
    bgColor: Color,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Row(
            modifier = Modifier.padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(shape = CircleShape, color = bgColor, modifier = Modifier.size(42.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Text(emoji, style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.width(10.dp))
            Column {
                Text(value, style = MaterialTheme.typography.titleLarge, color = accentColor, fontWeight = FontWeight.Bold)
                Text(label, style = MaterialTheme.typography.labelSmall, color = PlumMuted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun SurpriseSkeletonCard() {
    val transition = rememberInfiniteTransition(label = "skeleton_pulse")
    val alpha by transition.animateFloat(
        initialValue = 0.35f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
        label = "skeleton_alpha"
    )
    val barColor = RoseBlush.copy(alpha = alpha)

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.size(46.dp).clip(RoundedCornerShape(14.dp)).background(barColor))
                Spacer(Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(modifier = Modifier.fillMaxWidth(0.62f).height(16.dp).clip(RoundedCornerShape(6.dp)).background(barColor))
                    Box(modifier = Modifier.fillMaxWidth(0.42f).height(12.dp).clip(RoundedCornerShape(6.dp)).background(barColor))
                }
            }
            Box(modifier = Modifier.fillMaxWidth().height(32.dp).clip(RoundedCornerShape(12.dp)).background(barColor))
        }
    }
}

@Composable
fun SurpriseSummaryCard(
    surprise: Surprise,
    onEdit: () -> Unit,
    onPreview: () -> Unit,
    onShare: () -> Unit,
    onDuplicate: () -> Unit,
    onPrivacy: () -> Unit,
    onDelete: () -> Unit
) {
    val context = LocalContext.current
    var menuExpanded by remember { mutableStateOf(false) }
    val category = CategoryCatalog.find(surprise.categoryId)
    val dateLabel = surprise.createdAt?.toDate()?.let {
        SimpleDateFormat("MMM d, yyyy", Locale.getDefault()).format(it)
    } ?: "Just now"

    val (statusBg, statusFg) = when (surprise.status) {
        "replied" -> Color(0xFFD1FAE5) to Color(0xFF065F46)
        "opened" -> Color(0xFFFEF3C7) to Color(0xFF92400E)
        "published" -> RoseBlush to RosePrimary
        "expired" -> Color(0xFFFEE2E2) to Color(0xFF991B1B)
        else -> LavenderSoft to LavenderPrimary
    }

    Card(
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = Color(category.accentHex).copy(alpha = 0.14f),
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(category.emoji, style = MaterialTheme.typography.titleLarge)
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(surprise.title, style = MaterialTheme.typography.titleMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text("For ${surprise.recipientName} • $dateLabel", style = MaterialTheme.typography.bodySmall, color = PlumMuted)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Surface(shape = RoundedCornerShape(50), color = statusBg) {
                    Text(
                        text = surprise.status.replaceFirstChar { it.uppercase() },
                        style = MaterialTheme.typography.labelSmall,
                        color = statusFg,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(RoseBlush)
                    .clickable { copyToClipboard(context, "Surprise link", surprise.shareLink()) }
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Code: ${surprise.id} • Opens: ${surprise.openCount} • Replies: ${surprise.replyCount}",
                    style = MaterialTheme.typography.labelMedium,
                    color = PlumInk,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Icon(Icons.Default.ContentCopy, "Copy link", tint = RosePrimary, modifier = Modifier.size(16.dp))
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onEdit,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Edit", maxLines = 1)
                }
                FilledTonalButton(
                    onClick = onPreview,
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Visibility, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Preview", maxLines = 1)
                }
                Button(
                    onClick = onShare,
                    colors = ButtonDefaults.buttonColors(containerColor = RosePrimary),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.QrCode2, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Share", maxLines = 1)
                }
                Box {
                    IconButton(onClick = { menuExpanded = true }) {
                        Icon(Icons.Default.MoreVert, contentDescription = "More surprise options")
                    }
                    DropdownMenu(
                        expanded = menuExpanded,
                        onDismissRequest = { menuExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Duplicate") },
                            leadingIcon = { Icon(Icons.Default.FileCopy, null) },
                            onClick = {
                                menuExpanded = false
                                onDuplicate()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Manage Privacy") },
                            leadingIcon = { Icon(Icons.Default.Lock, null) },
                            onClick = {
                                menuExpanded = false
                                onPrivacy()
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete", color = MaterialTheme.colorScheme.error) },
                            leadingIcon = { Icon(Icons.Default.Delete, null, tint = MaterialTheme.colorScheme.error) },
                            onClick = {
                                menuExpanded = false
                                onDelete()
                            }
                        )
                    }
                }
            }
        }
    }
}
