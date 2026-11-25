package com.example.data.util

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.net.Uri
import android.util.Base64
import androidx.core.content.FileProvider
import com.example.data.model.CategoryCatalog
import com.example.data.model.Surprise
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

object ImageUtils {

    fun compressUriToBase64(context: Context, uri: Uri, maxDim: Int = 640, maxBytes: Int = 140_000): String? {
        return runCatching {
            val input = context.contentResolver.openInputStream(uri) ?: return null
            val original = input.use { BitmapFactory.decodeStream(it) } ?: return null
            val scale = minOf(
                maxDim.toFloat() / original.width.coerceAtLeast(1),
                maxDim.toFloat() / original.height.coerceAtLeast(1),
                1f
            )
            val targetW = (original.width * scale).toInt().coerceAtLeast(1)
            val targetH = (original.height * scale).toInt().coerceAtLeast(1)
            val resized = if (scale < 1f) {
                Bitmap.createScaledBitmap(original, targetW, targetH, true)
            } else original

            var quality = 82
            var outBytes: ByteArray
            do {
                val stream = ByteArrayOutputStream()
                resized.compress(Bitmap.CompressFormat.JPEG, quality, stream)
                outBytes = stream.toByteArray()
                quality -= 10
            } while (outBytes.size > maxBytes && quality >= 30)

            Base64.encodeToString(outBytes, Base64.NO_WRAP)
        }.getOrNull()
    }

    fun decodeBase64ToBitmap(base64: String): Bitmap? {
        if (base64.isBlank()) return null
        return runCatching {
            val bytes = Base64.decode(base64, Base64.DEFAULT)
            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        }.getOrNull()
    }

    fun exportSurpriseCardAndShare(context: Context, surprise: Surprise) {
        runCatching {
            val width = 1080
            val height = 1440
            val bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bmp)
            val category = CategoryCatalog.find(surprise.categoryId)

            val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.parseColor("#FFF1F5")
            }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), bgPaint)

            val cardPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = android.graphics.Color.WHITE
            }
            canvas.drawRoundRect(RectF(72f, 96f, width - 72f, height - 96f), 48f, 48f, cardPaint)

            val borderPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = 6f
                color = android.graphics.Color.parseColor("#FB7185")
            }
            canvas.drawRoundRect(RectF(96f, 120f, width - 96f, height - 120f), 40f, 40f, borderPaint)

            val badgePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 44f
                color = android.graphics.Color.parseColor("#E11D48")
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
            }
            canvas.drawText("${category.emoji} ${category.name}", width / 2f, 220f, badgePaint)

            val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 58f
                color = android.graphics.Color.parseColor("#2D1623")
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
            }
            canvas.drawText(surprise.title.take(32), width / 2f, 320f, titlePaint)

            val toPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 38f
                color = android.graphics.Color.parseColor("#6E4B5E")
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Dear ${surprise.recipientName},", width / 2f, 420f, toPaint)

            val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 36f
                color = android.graphics.Color.parseColor("#2D1623")
                textAlign = Paint.Align.CENTER
            }
            val words = surprise.mainMessage.split(" ")
            val lines = mutableListOf<String>()
            var current = ""
            for (word in words) {
                val candidate = if (current.isEmpty()) word else "$current $word"
                if (bodyPaint.measureText(candidate) <= width - 260f) {
                    current = candidate
                } else {
                    lines.add(current)
                    current = word
                }
            }
            if (current.isNotEmpty()) lines.add(current)
            var y = 520f
            lines.take(12).forEach { line ->
                canvas.drawText(line, width / 2f, y, bodyPaint)
                y += 54f
            }

            val fromPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 40f
                color = android.graphics.Color.parseColor("#E11D48")
                textAlign = Paint.Align.CENTER
                typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
            }
            canvas.drawText("With love, ${surprise.senderName}", width / 2f, height - 240f, fromPaint)

            val footerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                textSize = 28f
                color = android.graphics.Color.parseColor("#9F1239")
                textAlign = Paint.Align.CENTER
            }
            canvas.drawText("Gift For You • Code: ${surprise.id}", width / 2f, height - 165f, footerPaint)

            val dir = File(context.cacheDir, "cards").apply { mkdirs() }
            val file = File(dir, "gift_card_${surprise.id}.png")
            FileOutputStream(file).use { out ->
                bmp.compress(Bitmap.CompressFormat.PNG, 95, out)
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_TEXT, "A keepsake gift card for ${surprise.recipientName}! Open code: ${surprise.id}")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(Intent.createChooser(intent, "Save or Share Gift Card"))
        }
    }
}
