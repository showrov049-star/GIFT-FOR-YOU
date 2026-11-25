package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.data.model.AppNotification
import com.example.data.model.ReactionType
import com.example.data.model.Surprise
import com.example.data.model.SurpriseReply
import com.example.data.model.UserProfile
import com.example.ui.components.Base64ImageView
import com.example.ui.theme.PlumMuted
import com.example.ui.theme.RoseBlush
import com.example.ui.theme.RosePrimary
import com.example.ui.viewmodel.UiState
import java.text.SimpleDateFormat
import java.util.Locale

enum class SurpriseSortOption(val label: String) {
    NEWEST("Newest"),
    MOST_OPENED("Most Opened"),
    MOST_REPLIED("Most Replied"),
    TITLE_AZ("Title A-Z")
}

fun filterAndSortSurprises(
    surprises: List<Surprise>,
    statusFilter: String,
    query: String,
    sortOption: SurpriseSortOption
): List<Surprise> {
    val q = query.trim().lowercase()
    val filtered = surprises.filter { item ->
        val matchesStatus = statusFilter == "all" || item.status == statusFilter
        val matchesQuery = q.isEmpty() ||
            item.title.lowercase().contains(q) ||
            item.recipientName.lowercase().contains(q) ||
            item.id.lowercase().contains(q)
        matchesStatus && matchesQuery
    }
    return when (sortOption) {
        SurpriseSortOption.NEWEST -> filtered.sortedByDescending {
            it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L
        }
        SurpriseSortOption.MOST_OPENED -> filtered.sortedByDescending { it.openCount }
        SurpriseSortOption.MOST_REPLIED -> filtered.sortedByDescending { it.replyCount }
        SurpriseSortOption.TITLE_AZ -> filtered.sortedBy { it.title.lowercase() }
    }
}

@Composable
fun MySurprisesTab(
    surprisesState: UiState<List<Surprise>>,
    onCreateSurprise: () -> Unit,
    onEditSurprise: (Surprise) -> Unit,
    onPreviewSurprise: (Surprise) -> Unit,
    onShareSurprise: (Surprise) -> Unit,
    onDuplicateSurprise: (Surprise) -> Unit,
    onManagePrivacy: (Surprise) -> Unit,
    onDeleteSurprise: (Surprise) -> Unit
) {
    var statusFilter by remember { mutableStateOf("all") }
    var searchQuery by remember { mutableStateOf("") }
    var sortOption by remember { mutableStateOf(SurpriseSortOption.NEWEST) }

    val surprises = (surprisesState as? UiState.Success)?.data ?: emptyList()
    val filtered = remember(surprises, statusFilter, searchQuery, sortOption) {
        filterAndSortSurprises(surprises, statusFilter, searchQuery, sortOption)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it.take(80) },
            placeholder = { Text("Search by title, recipient, or code...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { searchQuery = "" }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear search")
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 16.dp, top = 12.dp)
                .testTag("surprises_search_input")
        )

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(listOf("all", "published", "opened", "replied", "draft", "expired")) { status ->
                FilterChip(
                    selected = statusFilter == status,
                    onClick = { statusFilter = status },
                    label = { Text(status.replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        LazyRow(
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(SurpriseSortOption.entries) { option ->
                FilterChip(
                    selected = sortOption == option,
                    onClick = { sortOption = option },
                    label = { Text("Sort: ${option.label}", style = MaterialTheme.typography.labelSmall) }
                )
            }
        }

        when (surprisesState) {
            is UiState.Loading -> LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(3) { SurpriseSkeletonCard() }
            }
            is UiState.Error -> Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
                Text(surprisesState.message, color = MaterialTheme.colorScheme.error)
            }
            is UiState.Success -> {
                if (filtered.isEmpty()) {
                    Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🎁", style = MaterialTheme.typography.displaySmall)
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = if (searchQuery.isNotBlank()) "No surprises match \"$searchQuery\"." else "No surprises match this filter.",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Spacer(Modifier.height(12.dp))
                            if (searchQuery.isNotBlank() || statusFilter != "all") {
                                OutlinedButton(onClick = {
                                    searchQuery = ""
                                    statusFilter = "all"
                                }) {
                                    Text("Clear Filters")
                                }
                            } else {
                                Button(onClick = onCreateSurprise, colors = ButtonDefaults.buttonColors(containerColor = RosePrimary)) {
                                    Text("Create Surprise")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(filtered, key = { it.id }) { surprise ->
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
                        item { Spacer(Modifier.height(80.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
fun RepliesInboxTab(
    repliesState: UiState<List<SurpriseReply>>,
    onMarkRead: (String) -> Unit
) {
    when (repliesState) {
        is UiState.Loading -> LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(3) { SurpriseSkeletonCard() }
        }
        is UiState.Error -> Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            Text(repliesState.message, color = MaterialTheme.colorScheme.error)
        }
        is UiState.Success -> {
            val replies = repliesState.data
            if (replies.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("💌", style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.height(8.dp))
                        Text("No replies yet", style = MaterialTheme.typography.titleMedium)
                        Text(
                            "When someone unwraps your surprise and sends a reaction, it will show up here.",
                            style = MaterialTheme.typography.bodySmall,
                            color = PlumMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(replies, key = { it.id }) { reply ->
                        val reaction = ReactionType.fromId(reply.reaction)
                        val dateLabel = reply.createdAt?.toDate()?.let {
                            SimpleDateFormat("MMM d, h:mm a", Locale.getDefault()).format(it)
                        } ?: "Just now"

                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (reply.readFlag) Color.White else RoseBlush
                            ),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (!reply.readFlag) onMarkRead(reply.id)
                            }
                        ) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Text(reaction.emoji, style = MaterialTheme.typography.headlineSmall)
                                        Spacer(Modifier.width(10.dp))
                                        Column {
                                            Text(reply.recipientName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                            Text("On \"${reply.surpriseTitle}\" • $dateLabel", style = MaterialTheme.typography.labelSmall, color = PlumMuted)
                                        }
                                    }
                                    if (!reply.readFlag) {
                                        Surface(shape = RoundedCornerShape(50), color = RosePrimary) {
                                            Text("NEW", color = Color.White, style = MaterialTheme.typography.labelSmall, modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp))
                                        }
                                    }
                                }
                                Text(reply.message, style = MaterialTheme.typography.bodyMedium)
                                if (!reply.photoBase64.isNullOrBlank()) {
                                    Base64ImageView(
                                        base64 = reply.photoBase64,
                                        contentDescription = "Recipient reply photo",
                                        modifier = Modifier.fillMaxWidth().height(160.dp).clip(RoundedCornerShape(14.dp))
                                    )
                                }
                                if (!reply.readFlag) {
                                    TextButton(onClick = { onMarkRead(reply.id) }, modifier = Modifier.align(Alignment.End)) {
                                        Text("Mark as Read")
                                    }
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@Composable
fun NotificationsTab(
    notificationsState: UiState<List<AppNotification>>,
    onMarkRead: (String) -> Unit,
    onMarkAllRead: () -> Unit
) {
    when (notificationsState) {
        is UiState.Loading -> LazyColumn(
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(3) { SurpriseSkeletonCard() }
        }
        is UiState.Error -> Box(Modifier.fillMaxSize().padding(20.dp), contentAlignment = Alignment.Center) {
            Text(notificationsState.message, color = MaterialTheme.colorScheme.error)
        }
        is UiState.Success -> {
            val list = notificationsState.data
            if (list.isEmpty()) {
                Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔔", style = MaterialTheme.typography.displaySmall)
                        Spacer(Modifier.height(8.dp))
                        Text("You're all caught up!", style = MaterialTheme.typography.titleMedium)
                    }
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (list.any { !it.readFlag }) {
                        item {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                                TextButton(onClick = onMarkAllRead) {
                                    Icon(Icons.Default.DoneAll, null, Modifier.size(16.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text("Mark All Read")
                                }
                            }
                        }
                    }
                    items(list, key = { it.id }) { notif ->
                        Surface(
                            shape = RoundedCornerShape(18.dp),
                            color = if (notif.readFlag) Color.White else RoseBlush,
                            shadowElevation = 2.dp,
                            modifier = Modifier.fillMaxWidth().clickable {
                                if (!notif.readFlag) onMarkRead(notif.id)
                            }
                        ) {
                            Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(if (notif.type == "replied") "💌" else "🎁", style = MaterialTheme.typography.headlineSmall)
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(notif.title, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
                                    Text(notif.message, style = MaterialTheme.typography.bodySmall, color = PlumMuted)
                                }
                            }
                        }
                    }
                    item { Spacer(Modifier.height(80.dp)) }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun PrivacyManageDialog(
    surprise: Surprise,
    onSave: (String, Long, Long, String) -> Unit,
    onDismiss: () -> Unit
) {
    var passcode by remember { mutableStateOf(surprise.secretPasscode) }
    var unlockAt by remember { mutableStateOf(surprise.unlockAtMillis) }
    var expiresAt by remember { mutableStateOf(surprise.expiresAtMillis) }
    var status by remember { mutableStateOf(surprise.status) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manage Surprise Privacy 🔒") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = passcode,
                    onValueChange = { passcode = it.take(40) },
                    label = { Text("Secret Passcode (blank = none)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("Access Status", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(selected = status == "published" || status == "opened" || status == "replied", onClick = { status = "published" }, label = { Text("Active") })
                    FilterChip(selected = status == "draft", onClick = { status = "draft" }, label = { Text("Pause (Draft)") })
                    FilterChip(selected = status == "expired", onClick = { status = "expired" }, label = { Text("Expire") })
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(selected = unlockAt == 0L, onClick = { unlockAt = 0L }, label = { Text("Unlock Now") })
                    FilterChip(selected = unlockAt > 0L, onClick = { unlockAt = System.currentTimeMillis() + 86_400_000L }, label = { Text("+24h Capsule") })
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(passcode, unlockAt, expiresAt, status) }) {
                Text("Save Privacy")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ProfilePrivacySettingsDialog(
    profile: UserProfile?,
    fallbackName: String,
    fallbackEmail: String,
    photoUrl: String?,
    onOpenSoundSettings: () -> Unit,
    onSave: (String, Boolean, Boolean, Boolean) -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    var displayName by remember(profile) { mutableStateOf(profile?.displayName ?: fallbackName) }
    var notifyOpen by remember(profile) { mutableStateOf(profile?.notifyOnOpen ?: true) }
    var notifyReply by remember(profile) { mutableStateOf(profile?.notifyOnReply ?: true) }
    var defaultSkip by remember(profile) { mutableStateOf(profile?.defaultAllowSkip ?: true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Profile & Privacy Settings") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!photoUrl.isNullOrBlank()) {
                        AsyncImage(
                            model = photoUrl,
                            contentDescription = "Google Profile Photo",
                            modifier = Modifier.size(48.dp).clip(CircleShape),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Surface(shape = CircleShape, color = RoseBlush, modifier = Modifier.size(48.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(displayName.take(1).uppercase(), fontWeight = FontWeight.Bold, color = RosePrimary)
                            }
                        }
                    }
                    Spacer(Modifier.width(12.dp))
                    Column {
                        Text(displayName, style = MaterialTheme.typography.titleSmall)
                        Text(profile?.email ?: fallbackEmail, style = MaterialTheme.typography.bodySmall, color = PlumMuted)
                    }
                }

                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it.take(100) },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Notify when gift opened", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = notifyOpen, onCheckedChange = { notifyOpen = it })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Notify on new replies", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = notifyReply, onCheckedChange = { notifyReply = it })
                }
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text("Allow game skip by default", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = defaultSkip, onCheckedChange = { defaultSkip = it })
                }

                OutlinedButton(
                    onClick = onOpenSoundSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Tune, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sound & Music Settings")
                }

                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.AutoMirrored.Filled.Logout, null, Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out")
                }
            }
        },
        confirmButton = {
            Button(onClick = { onSave(displayName, notifyOpen, notifyReply, defaultSkip) }) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
