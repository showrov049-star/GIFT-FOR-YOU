package com.example.ui.components

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.data.model.Surprise
import com.example.data.util.ImageUtils
import com.example.data.util.QrCodeGenerator

@Composable
fun QrCodeView(
    payload: String,
    modifier: Modifier = Modifier
) {
    val matrix = remember(payload) { QrCodeGenerator.encode(payload) }
    Surface(
        modifier = modifier
            .size(180.dp)
            .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(18.dp)),
        shape = RoundedCornerShape(18.dp),
        color = Color.White
    ) {
        Canvas(
            modifier = Modifier
                .size(180.dp)
                .padding(14.dp)
        ) {
            val cellSize = size.width / matrix.size.toFloat()
            for (y in 0 until matrix.size) {
                for (x in 0 until matrix.size) {
                    if (matrix.modules[y][x]) {
                        drawRect(
                            color = Color(0xFF2D1623),
                            topLeft = Offset(x * cellSize, y * cellSize),
                            size = Size(cellSize + 0.5f, cellSize + 0.5f)
                        )
                    }
                }
            }
        }
    }
}

fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, text))
    Toast.makeText(context, "$label copied!", Toast.LENGTH_SHORT).show()
}

fun shareOnWhatsApp(context: Context, surprise: Surprise) {
    val msg = "Hey ${surprise.recipientName}! I made a special digital surprise for you on Gift For You ❤️\nOpen link: ${surprise.shareLink()}\nOr enter Gift Code: ${surprise.id}"
    val waIntent = Intent(Intent.ACTION_VIEW).apply {
        data = Uri.parse("https://wa.me/?text=${Uri.encode(msg)}")
    }
    runCatching {
        context.startActivity(waIntent)
    }.onFailure {
        shareSystem(context, surprise)
    }
}

fun shareSystem(context: Context, surprise: Surprise) {
    val msg = "Hey ${surprise.recipientName}! I created a digital surprise for you ❤️\n\nOpen: ${surprise.shareLink()}\nGift Code: ${surprise.id}"
    val sendIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_SUBJECT, surprise.title)
        putExtra(Intent.EXTRA_TEXT, msg)
    }
    context.startActivity(Intent.createChooser(sendIntent, "Share Surprise"))
}

@Composable
fun ShareSurpriseDialog(
    surprise: Surprise,
    isNewlyPublished: Boolean,
    onPreviewGift: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val shareUrl = surprise.shareLink()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Published",
                    tint = Color(0xFF10B981)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = if (isNewlyPublished) "Your Surprise is Live!" else "Share Surprise",
                    style = MaterialTheme.typography.titleLarge
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Send this unique link or Gift Code to ${surprise.recipientName} — no login needed to open!",
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )

                QrCodeView(payload = shareUrl)

                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Gift Code: ${surprise.id}",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            text = shareUrl,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { copyToClipboard(context, "Surprise link", shareUrl) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy Link")
                    }
                    OutlinedButton(
                        onClick = { copyToClipboard(context, "Gift code", surprise.id) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Copy Code")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { shareOnWhatsApp(context, surprise) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF16A34A)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("WhatsApp", fontWeight = FontWeight.SemiBold)
                    }
                    FilledTonalButton(
                        onClick = { shareSystem(context, surprise) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Share")
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { ImageUtils.exportSurpriseCardAndShare(context, surprise) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Letter Card")
                    }
                    FilledTonalButton(
                        onClick = onPreviewGift,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Visibility, null, Modifier.size(16.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("Preview")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )
}
