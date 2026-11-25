package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.FormatAlignLeft
import androidx.compose.material.icons.automirrored.filled.FormatAlignRight
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.FormatAlignCenter
import androidx.compose.material.icons.filled.FormatBold
import androidx.compose.material.icons.filled.FormatItalic
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.model.CategoryCatalog
import com.example.data.model.Surprise
import com.example.ui.components.ShareSurpriseDialog
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.InterFontFamily
import com.example.ui.theme.PlayfairDisplayFontFamily
import com.example.ui.theme.PoppinsFontFamily
import com.example.ui.theme.RoseBlush
import com.example.ui.theme.RosePrimary
import com.example.ui.viewmodel.EditorViewModel

private val EditorStepTitles = listOf(
    "Basic Info",
    "Main Message",
    "Photos",
    "Voice Note",
    "Music",
    "Visual Style",
    "Mini-Games",
    "Preview & Publish"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditorScreen(
    viewModel: EditorViewModel,
    onBack: () -> Unit,
    onPreviewSurprise: (Surprise) -> Unit,
    onPublishedDone: () -> Unit
) {
    val uiState by viewModel.state.collectAsStateWithLifecycle()
    val draft = uiState.draft
    var showUnsavedDialog by remember { mutableStateOf(false) }

    val attemptClose = {
        if (uiState.hasUnsavedChanges) {
            showUnsavedDialog = true
        } else {
            onBack()
        }
    }

    BackHandler {
        if (uiState.currentStep > 0) {
            viewModel.setStep(uiState.currentStep - 1)
        } else {
            attemptClose()
        }
    }

    Scaffold(
        containerColor = CreamBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = EditorStepTitles[uiState.currentStep],
                            style = MaterialTheme.typography.titleMedium
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "Step ${uiState.currentStep + 1} of ${EditorStepTitles.size}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            val badgeText = uiState.autosaveStatus ?: if (uiState.hasUnsavedChanges) "Unsaved changes" else null
                            if (!badgeText.isNullOrBlank()) {
                                Surface(
                                    shape = RoundedCornerShape(50),
                                    color = if (uiState.hasUnsavedChanges && uiState.autosaveStatus != "Saving draft...") RoseBlush else Color(0xFFD1FAE5)
                                ) {
                                    Text(
                                        text = badgeText,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (uiState.hasUnsavedChanges && uiState.autosaveStatus != "Saving draft...") RosePrimary else Color(0xFF065F46),
                                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 1.dp)
                                    )
                                }
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = attemptClose) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Close editor")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.generateRandomSurprise() }) {
                        Icon(Icons.Default.Casino, contentDescription = "Random surprise template", tint = RosePrimary)
                    }
                    IconButton(onClick = { onPreviewSurprise(draft) }) {
                        Icon(Icons.Default.Visibility, contentDescription = "Preview surprise", tint = RosePrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (uiState.currentStep > 0) {
                        OutlinedButton(onClick = { viewModel.setStep(uiState.currentStep - 1) }) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Back")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { viewModel.saveSurprise(publishNow = false) },
                            enabled = !uiState.isSaving
                        ) {
                            Icon(Icons.Default.Save, null, Modifier.size(16.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save Draft")
                        }
                    }

                    if (uiState.currentStep < EditorStepTitles.lastIndex) {
                        Button(
                            onClick = { viewModel.setStep(uiState.currentStep + 1) },
                            colors = ButtonDefaults.buttonColors(containerColor = RosePrimary),
                            modifier = Modifier.testTag("editor_next_button")
                        ) {
                            Text("Next Step")
                            Spacer(Modifier.width(6.dp))
                            Icon(Icons.AutoMirrored.Filled.ArrowForward, null, Modifier.size(16.dp))
                        }
                    } else {
                        Button(
                            onClick = { viewModel.saveSurprise(publishNow = true) },
                            enabled = !uiState.isSaving,
                            colors = ButtonDefaults.buttonColors(containerColor = RosePrimary),
                            modifier = Modifier.testTag("publish_surprise_button")
                        ) {
                            Text(if (uiState.isSaving) "Publishing..." else "Publish Surprise 🎁")
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            LinearProgressIndicator(
                progress = { (uiState.currentStep + 1) / EditorStepTitles.size.toFloat() },
                modifier = Modifier.fillMaxWidth().height(4.dp),
                color = RosePrimary
            )

            LazyRow(
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(EditorStepTitles) { index, label ->
                    FilterChip(
                        selected = uiState.currentStep == index,
                        onClick = { viewModel.setStep(index) },
                        label = { Text("${index + 1}. $label", style = MaterialTheme.typography.labelSmall) }
                    )
                }
            }

            if (!uiState.errorMessage.isNullOrBlank()) {
                Surface(
                    color = MaterialTheme.colorScheme.errorContainer,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = uiState.errorMessage!!,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }
            if (!uiState.statusBanner.isNullOrBlank()) {
                Surface(
                    color = Color(0xFFD1FAE5),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(uiState.statusBanner!!, color = Color(0xFF065F46), style = MaterialTheme.typography.bodySmall)
                        TextButton(onClick = { viewModel.clearBanner() }) { Text("OK") }
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState()),
                contentAlignment = Alignment.TopCenter
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .widthIn(max = 640.dp)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    when (uiState.currentStep) {
                        0 -> EditorStep1BasicInfo(draft, viewModel)
                        1 -> EditorStep2Message(draft, viewModel)
                        2 -> EditorStep3Photos(draft, viewModel)
                        3 -> EditorStep4VoiceNote(draft, viewModel)
                        4 -> EditorStep5Music(draft, viewModel)
                        5 -> EditorStep6VisualStyle(draft, viewModel)
                        6 -> EditorStep7Games(draft, viewModel)
                        7 -> EditorStep8Publish(
                            draft = draft,
                            isSaving = uiState.isSaving,
                            viewModel = viewModel,
                            onPreview = { onPreviewSurprise(draft) }
                        )
                    }
                }
            }
        }
    }

    if (uiState.publishedId != null) {
        val publishedSurprise = draft.copy(id = uiState.publishedId!!)
        ShareSurpriseDialog(
            surprise = publishedSurprise,
            isNewlyPublished = true,
            onPreviewGift = {
                viewModel.dismissPublishedDialog()
                onPreviewSurprise(publishedSurprise)
            },
            onDismiss = {
                viewModel.dismissPublishedDialog()
                onPublishedDone()
            }
        )
    }

    if (showUnsavedDialog) {
        AlertDialog(
            onDismissRequest = { showUnsavedDialog = false },
            title = { Text("Unsaved Changes") },
            text = { Text("You have edits that haven't been saved yet. Would you like to save a draft before leaving?") },
            confirmButton = {
                Button(
                    onClick = {
                        showUnsavedDialog = false
                        viewModel.saveSurprise(publishNow = false) {
                            onBack()
                        }
                    },
                    enabled = !uiState.isSaving,
                    colors = ButtonDefaults.buttonColors(containerColor = RosePrimary)
                ) {
                    Text("Save Draft")
                }
            },
            dismissButton = {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    TextButton(onClick = { showUnsavedDialog = false }) {
                        Text("Keep Editing")
                    }
                    TextButton(onClick = {
                        showUnsavedDialog = false
                        onBack()
                    }) {
                        Text("Discard", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        )
    }
}

@Composable
private fun EditorStep1BasicInfo(draft: Surprise, viewModel: EditorViewModel) {
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text("Who is this surprise for? 🎁", style = MaterialTheme.typography.titleLarge)
            OutlinedTextField(
                value = draft.title,
                onValueChange = { viewModel.updateDraft { d -> d.copy(title = it.take(120)) } },
                label = { Text("Surprise Title") },
                supportingText = { Text("${draft.title.length}/120") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("editor_title_input")
            )
            OutlinedTextField(
                value = draft.recipientName,
                onValueChange = { viewModel.updateDraft { d -> d.copy(recipientName = it.take(80)) } },
                label = { Text("Recipient Name") },
                placeholder = { Text("e.g. Maya") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().testTag("editor_recipient_input")
            )
            OutlinedTextField(
                value = draft.senderName,
                onValueChange = { viewModel.updateDraft { d -> d.copy(senderName = it.take(80)) } },
                label = { Text("Your Name (Sender)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            OutlinedTextField(
                value = draft.introMessage,
                onValueChange = { viewModel.updateDraft { d -> d.copy(introMessage = it.take(300)) } },
                label = { Text("Short Intro Message") },
                placeholder = { Text("Shown on the welcome screen before unwrapping...") },
                supportingText = { Text("${draft.introMessage.length}/300") },
                minLines = 2,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    Text("Category & Theme Template", style = MaterialTheme.typography.titleMedium)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        itemsIndexed(CategoryCatalog.all) { _, cat ->
            val selected = draft.categoryId == cat.id
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = if (selected) RosePrimary else Color.White,
                shadowElevation = 2.dp,
                modifier = Modifier.clickable { viewModel.applyCategoryTemplate(cat.id, overwriteMessage = false) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(cat.emoji)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = cat.name,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}

@Composable
private fun EditorStep2Message(draft: Surprise, viewModel: EditorViewModel) {
    val category = CategoryCatalog.find(draft.categoryId)
    Card(shape = RoundedCornerShape(22.dp), colors = CardDefaults.cardColors(containerColor = Color.White)) {
        Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Personal Letter 💌", style = MaterialTheme.typography.titleLarge)
                TextButton(onClick = {
                    viewModel.updateDraft { it.copy(mainMessage = category.messageTemplate) }
                }) {
                    Icon(Icons.Default.AutoAwesome, null, Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("Use Template")
                }
            }

            // Rich formatting toolbar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(RoseBlush)
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row {
                    IconButton(onClick = { viewModel.updateDraft { it.copy(boldBody = !it.boldBody) } }) {
                        Icon(
                            Icons.Default.FormatBold,
                            contentDescription = "Bold",
                            tint = if (draft.boldBody) RosePrimary else Color.Gray
                        )
                    }
                    IconButton(onClick = { viewModel.updateDraft { it.copy(italicBody = !it.italicBody) } }) {
                        Icon(
                            Icons.Default.FormatItalic,
                            contentDescription = "Italic",
                            tint = if (draft.italicBody) RosePrimary else Color.Gray
                        )
                    }
                }
                Row {
                    IconButton(onClick = { viewModel.updateDraft { it.copy(textAlignment = "start") } }) {
                        Icon(Icons.AutoMirrored.Filled.FormatAlignLeft, "Align left", tint = if (draft.textAlignment == "start") RosePrimary else Color.Gray)
                    }
                    IconButton(onClick = { viewModel.updateDraft { it.copy(textAlignment = "center") } }) {
                        Icon(Icons.Default.FormatAlignCenter, "Align center", tint = if (draft.textAlignment == "center") RosePrimary else Color.Gray)
                    }
                    IconButton(onClick = { viewModel.updateDraft { it.copy(textAlignment = "end") } }) {
                        Icon(Icons.AutoMirrored.Filled.FormatAlignRight, "Align right", tint = if (draft.textAlignment == "end") RosePrimary else Color.Gray)
                    }
                }
            }

            // Font selection chips
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("playfair" to "Playfair", "poppins" to "Poppins", "inter" to "Inter").forEach { (id, label) ->
                    FilterChip(
                        selected = draft.fontStyle == id,
                        onClick = { viewModel.updateDraft { it.copy(fontStyle = id) } },
                        label = { Text(label) }
                    )
                }
            }

            // Emoji quick-insert bar
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                val emojis = listOf("❤️", "🎂", "✨", "🥰", "🌸", "🎉", "💌", "🥹", "🥂")
                itemsIndexed(emojis) { _, emoji ->
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = RoseBlush,
                        modifier = Modifier.clickable {
                            viewModel.updateDraft { it.copy(mainMessage = (it.mainMessage + " $emoji").take(5000)) }
                        }
                    ) {
                        Text(emoji, modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp))
                    }
                }
            }

            val fontFamily = when (draft.fontStyle) {
                "poppins" -> PoppinsFontFamily
                "inter" -> InterFontFamily
                else -> PlayfairDisplayFontFamily
            }
            val align = when (draft.textAlignment) {
                "start" -> TextAlign.Start
                "end" -> TextAlign.End
                else -> TextAlign.Center
            }
            OutlinedTextField(
                value = draft.mainMessage,
                onValueChange = { viewModel.updateDraft { d -> d.copy(mainMessage = it.take(5000)) } },
                label = { Text("Write your heartfelt message") },
                supportingText = { Text("${draft.mainMessage.length}/5000") },
                minLines = 6,
                textStyle = MaterialTheme.typography.bodyLarge.copy(
                    fontFamily = fontFamily,
                    fontWeight = if (draft.boldBody) FontWeight.Bold else FontWeight.Normal,
                    fontStyle = if (draft.italicBody) FontStyle.Italic else FontStyle.Normal,
                    textAlign = align
                ),
                modifier = Modifier.fillMaxWidth().testTag("editor_message_input")
            )
        }
    }
}
