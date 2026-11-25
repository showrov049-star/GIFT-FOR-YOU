package com.example.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.CategoryCatalog
import com.example.data.model.MiniGameType
import com.example.ui.theme.CreamBackground
import com.example.ui.theme.RoseBlush

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryGalleryScreen(
    onBack: () -> Unit,
    onCategorySelected: (String) -> Unit
) {
    Scaffold(
        containerColor = CreamBackground,
        topBar = {
            TopAppBar(
                title = { Text("Choose a Surprise Theme") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    FilledTonalButton(
                        onClick = { onCategorySelected(CategoryCatalog.randomTemplate().id) },
                        modifier = Modifier.padding(end = 10.dp).testTag("random_category_btn")
                    ) {
                        Icon(Icons.Default.Casino, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Surprise Me")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        }
    ) { innerPadding ->
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 164.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            items(CategoryCatalog.all, key = { it.id }) { category ->
                val accent = Color(category.accentHex)
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(22.dp))
                        .clickable { onCategorySelected(category.id) }
                        .testTag("category_card_${category.id}"),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                Brush.verticalGradient(
                                    listOf(accent.copy(alpha = 0.08f), Color.White)
                                )
                            )
                            .padding(14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(category.emoji, style = MaterialTheme.typography.headlineMedium)
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = accent.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = category.defaultAnimation.replaceFirstChar { it.uppercase() },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = category.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = category.subtitle,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2
                        )
                        Spacer(Modifier.height(8.dp))
                        val gameEmoji = MiniGameType.fromId(category.suggestedGames.firstOrNull() ?: "catch_hearts").emoji
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = RoseBlush
                        ) {
                            Text(
                                text = "$gameEmoji Suggested Game",
                                style = MaterialTheme.typography.labelSmall,
                                color = accent,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
