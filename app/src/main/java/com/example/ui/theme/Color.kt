package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Core Brand Palette — Romantic Pink, Lavender, Cream & Gold
val RosePrimary = Color(0xFFE11D48)
val RoseSoft = Color(0xFFFB7185)
val RoseBlush = Color(0xFFFFF1F5)
val RoseContainer = Color(0xFFFFE4EC)
val RoseDeep = Color(0xFF9F1239)

val LavenderPrimary = Color(0xFF9333EA)
val LavenderSoft = Color(0xFFF3E8FF)
val LavenderMist = Color(0xFFE9D5FF)

val CreamBackground = Color(0xFFFFFBF7)
val CreamCard = Color(0xFFFFFFFF)
val SoftBorder = Color(0xFFF3DCE5)

val GoldAccent = Color(0xFFF59E0B)
val GoldSoft = Color(0xFFFEF3C7)

val PlumInk = Color(0xFF2D1623)
val PlumMuted = Color(0xFF6E4B5E)

val SuccessMint = Color(0xFF10B981)
val SuccessMintBg = Color(0xFFD1FAE5)

// Dark Mode Palette
val DarkPlumBg = Color(0xFF1A0E16)
val DarkPlumSurface = Color(0xFF261420)
val DarkPlumCard = Color(0xFF331B2B)
val DarkRosePrimary = Color(0xFFFF6B8B)
val DarkCreamText = Color(0xFFFFF0F5)

data class SurpriseThemeSpec(
    val id: String,
    val name: String,
    val bgTop: Color,
    val bgBottom: Color,
    val cardSurface: Color,
    val accent: Color,
    val secondaryAccent: Color,
    val textPrimary: Color,
    val textMuted: Color,
    val isDark: Boolean = false
)

val SurpriseThemes = listOf(
    SurpriseThemeSpec(
        id = "blush_rose",
        name = "Blush Rose",
        bgTop = Color(0xFFFFF0F5),
        bgBottom = Color(0xFFFFE4EC),
        cardSurface = Color(0xFFFFFFFF),
        accent = Color(0xFFE11D48),
        secondaryAccent = Color(0xFFF472B6),
        textPrimary = Color(0xFF2D1623),
        textMuted = Color(0xFF7A4B61)
    ),
    SurpriseThemeSpec(
        id = "lavender_dream",
        name = "Lavender Dream",
        bgTop = Color(0xFFF8F2FF),
        bgBottom = Color(0xFFEDE9FE),
        cardSurface = Color(0xFFFFFFFF),
        accent = Color(0xFF9333EA),
        secondaryAccent = Color(0xFFEC4899),
        textPrimary = Color(0xFF26163B),
        textMuted = Color(0xFF6B528A)
    ),
    SurpriseThemeSpec(
        id = "golden_sunshine",
        name = "Golden Glow",
        bgTop = Color(0xFFFFFBEB),
        bgBottom = Color(0xFFFEF3C7),
        cardSurface = Color(0xFFFFFFFF),
        accent = Color(0xFFD97706),
        secondaryAccent = Color(0xFFF43F5E),
        textPrimary = Color(0xFF3B2412),
        textMuted = Color(0xFF855830)
    ),
    SurpriseThemeSpec(
        id = "midnight_starlight",
        name = "Midnight Stars",
        bgTop = Color(0xFF17112B),
        bgBottom = Color(0xFF2B1947),
        cardSurface = Color(0xFF241B40),
        accent = Color(0xFFF472B6),
        secondaryAccent = Color(0xFFFBBF24),
        textPrimary = Color(0xFFFFF5FA),
        textMuted = Color(0xFFD8C6E8),
        isDark = true
    ),
    SurpriseThemeSpec(
        id = "emerald_garden",
        name = "Botanical Bloom",
        bgTop = Color(0xFFECFDF5),
        bgBottom = Color(0xFFD1FAE5),
        cardSurface = Color(0xFFFFFFFF),
        accent = Color(0xFF059669),
        secondaryAccent = Color(0xFFF43F5E),
        textPrimary = Color(0xFF113328),
        textMuted = Color(0xFF3E6B5C)
    ),
    SurpriseThemeSpec(
        id = "peach_sunset",
        name = "Peach Sunset",
        bgTop = Color(0xFFFFF7ED),
        bgBottom = Color(0xFFFFEDD5),
        cardSurface = Color(0xFFFFFFFF),
        accent = Color(0xFFEA580C),
        secondaryAccent = Color(0xFFDB2777),
        textPrimary = Color(0xFF381A0E),
        textMuted = Color(0xFF804B35)
    )
)

fun findThemeSpec(themeId: String, customHex: String = ""): SurpriseThemeSpec {
    val base = SurpriseThemes.firstOrNull { it.id == themeId } ?: SurpriseThemes.first()
    if (themeId == "custom" && customHex.isNotBlank()) {
        val parsed = runCatching { Color(android.graphics.Color.parseColor(customHex)) }.getOrNull()
        if (parsed != null) {
            return base.copy(
                id = "custom",
                name = "Custom Tint",
                bgTop = parsed.copy(alpha = 0.16f),
                bgBottom = parsed.copy(alpha = 0.30f),
                accent = parsed
            )
        }
    }
    return base
}
