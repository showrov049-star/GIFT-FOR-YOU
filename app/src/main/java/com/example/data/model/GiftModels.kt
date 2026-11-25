package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    val uid: String = "",
    val displayName: String = "",
    val email: String = "",
    val photoUrl: String? = null,
    val notifyOnOpen: Boolean = true,
    val notifyOnReply: Boolean = true,
    val defaultAllowSkip: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class Surprise(
    @DocumentId val id: String = "",
    val creatorUid: String = "",
    val title: String = "",
    val categoryId: String = "birthday",
    val recipientName: String = "",
    val senderName: String = "",
    val introMessage: String = "",
    val mainMessage: String = "",
    val fontStyle: String = "playfair",
    val textAlignment: String = "center",
    val textColorHex: String = "#2D1623",
    val boldBody: Boolean = false,
    val italicBody: Boolean = false,
    val photos: List<String> = emptyList(),
    val photoCaptions: List<String> = emptyList(),
    val voiceNoteAudioBase64: String? = null,
    val voiceNoteTranscript: String? = null,
    val voiceDurationSec: Long = 0L,
    val musicTrackId: String = "music_box_lullaby",
    val musicVolume: Double = 0.75,
    val themeId: String = "blush_rose",
    val customBgColorHex: String = "#FFF1F5",
    val cardStyle: String = "glass",
    val animationEffect: String = "hearts",
    val envelopeStyle: String = "wax_seal",
    val giftBoxStyle: String = "classic_rose",
    val selectedGames: List<String> = listOf("catch_hearts"),
    val gameDifficulty: String = "medium",
    val gamesMandatory: Boolean = true,
    val allowSkip: Boolean = true,
    val secretCodeClue: String = "What is our favorite thing to do together?",
    val secretCodeAnswer: String = "smile",
    val secretPasscode: String = "",
    val unlockAtMillis: Long = 0L,
    val eventDateMillis: Long = 0L,
    val expiresAtMillis: Long = 0L,
    val status: String = "draft",
    val openCount: Long = 0L,
    val replyCount: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null,
    val publishedAt: Timestamp? = null
) {
    fun shareLink(): String = "https://giftforyou.app/s/$id"
}

@Keep
@IgnoreExtraProperties
data class SurpriseReply(
    @DocumentId val id: String = "",
    val surpriseId: String = "",
    val creatorUid: String = "",
    val surpriseTitle: String = "",
    val recipientName: String = "",
    val reaction: String = "loved_it",
    val message: String = "",
    val emojiTag: String = "❤️",
    val photoBase64: String? = null,
    val readFlag: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class AppNotification(
    @DocumentId val id: String = "",
    val userId: String = "",
    val surpriseId: String = "",
    val type: String = "opened",
    val title: String = "",
    val message: String = "",
    val readFlag: Boolean = false,
    @ServerTimestamp val createdAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class SurpriseEvent(
    @DocumentId val id: String = "",
    val surpriseId: String = "",
    val creatorUid: String = "",
    val eventType: String = "opened",
    val anonymousSessionId: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null
)

enum class MiniGameType(
    val id: String,
    val title: String,
    val emoji: String,
    val shortDesc: String,
    val instruction: String
) {
    CATCH_HEARTS(
        id = "catch_hearts",
        title = "Catch the Hearts",
        emoji = "❤️",
        shortDesc = "Slide the basket to catch falling hearts",
        instruction = "Move the basket left & right or tap hearts to catch 10 before time runs out!"
    ),
    MEMORY_MATCH(
        id = "memory_match",
        title = "Memory Match",
        emoji = "🧠",
        shortDesc = "Flip cards and pair matching symbols",
        instruction = "Tap two cards at a time to match all adorable gift pairs!"
    ),
    SCRATCH_TO_REVEAL(
        id = "scratch_to_reveal",
        title = "Scratch to Reveal",
        emoji = "✨",
        shortDesc = "Scratch off the shimmer foil card",
        instruction = "Rub your finger across the pastel shimmer card to uncover the hidden secret!"
    ),
    LOVE_PUZZLE(
        id = "love_puzzle",
        title = "Love Puzzle",
        emoji = "🧩",
        shortDesc = "Arrange the heart mosaic tiles",
        instruction = "Tap adjacent tiles to slide them into order and complete the heart mosaic!"
    ),
    GUESS_SECRET_CODE(
        id = "guess_secret_code",
        title = "Guess the Secret Code",
        emoji = "🔐",
        shortDesc = "Solve the personal clue",
        instruction = "Read the special clue left just for you and enter the secret word!"
    ),
    SPIN_THE_WHEEL(
        id = "spin_the_wheel",
        title = "Spin the Wheel",
        emoji = "🎡",
        shortDesc = "Spin for a sweet surprise perk",
        instruction = "Tap Spin the Wheel to land on a sweet gift blessing and unlock your surprise!"
    ),
    POP_BALLOONS(
        id = "pop_balloons",
        title = "Pop the Balloons",
        emoji = "🎈",
        shortDesc = "Pop floating pastel balloons",
        instruction = "Tap 10 floating balloons before they drift away to burst open your gift!"
    ),
    TAP_THE_HEART(
        id = "tap_the_heart",
        title = "Tap the Heart",
        emoji = "💖",
        shortDesc = "15-second sparkling heart reflex rush",
        instruction = "Tap the glowing hearts as fast as they appear in 15 seconds!"
    );

    companion object {
        fun fromId(raw: String): MiniGameType =
            entries.firstOrNull { it.id == raw } ?: CATCH_HEARTS
    }
}

enum class ReactionType(val id: String, val emoji: String, val label: String) {
    LOVED_IT("loved_it", "😍", "Loved it"),
    SO_CUTE("so_cute", "🥰", "So Cute"),
    MADE_ME_LAUGH("made_me_laugh", "😂", "Made Me Laugh"),
    EMOTIONAL("emotional", "🥺", "Emotional"),
    I_LOVE_IT("i_love_it", "❤️", "I Love It");

    companion object {
        fun fromId(raw: String): ReactionType =
            entries.firstOrNull { it.id == raw } ?: LOVED_IT
    }
}

data class MusicTrack(
    val id: String,
    val title: String,
    val mood: String,
    val notesHz: List<Double>,
    val tempoMs: Long
)

object MusicCatalog {
    val tracks = listOf(
        MusicTrack(
            id = "none",
            title = "No Background Music",
            mood = "Silent letter reading",
            notesHz = emptyList(),
            tempoMs = 500L
        ),
        MusicTrack(
            id = "music_box_lullaby",
            title = "Rose Music Box",
            mood = "Gentle & Romantic",
            notesHz = listOf(523.25, 659.25, 783.99, 659.25, 587.33, 698.46, 880.00, 698.46),
            tempoMs = 420L
        ),
        MusicTrack(
            id = "warm_piano",
            title = "Warm Sunset Keys",
            mood = "Nostalgic & Cozy",
            notesHz = listOf(392.00, 493.88, 587.33, 739.99, 659.25, 587.33, 493.88, 392.00),
            tempoMs = 480L
        ),
        MusicTrack(
            id = "starlight_harp",
            title = "Lavender Starlight",
            mood = "Dreamy & Calm",
            notesHz = listOf(440.00, 554.37, 659.25, 880.00, 783.99, 659.25, 554.37, 440.00),
            tempoMs = 450L
        ),
        MusicTrack(
            id = "cheerful_chimes",
            title = "Birthday Chimes",
            mood = "Playful & Bright",
            notesHz = listOf(523.25, 587.33, 659.25, 783.99, 1046.50, 783.99, 659.25, 523.25),
            tempoMs = 340L
        )
    )

    fun find(id: String): MusicTrack = tracks.firstOrNull { it.id == id } ?: tracks[1]
}

data class SurpriseCategory(
    val id: String,
    val name: String,
    val emoji: String,
    val subtitle: String,
    val accentHex: Long,
    val defaultThemeId: String,
    val defaultAnimation: String,
    val defaultTitle: String,
    val defaultIntro: String,
    val messageTemplate: String,
    val suggestedGames: List<String>
)

object CategoryCatalog {
    val all: List<SurpriseCategory> = listOf(
        SurpriseCategory(
            id = "birthday",
            name = "Birthday Surprise",
            emoji = "🎂",
            subtitle = "Confetti, wishes & unforgettable cake-day joy",
            accentHex = 0xFFE11D48,
            defaultThemeId = "blush_rose",
            defaultAnimation = "confetti",
            defaultTitle = "Happy Birthday to Someone Special!",
            defaultIntro = "Close your eyes, make a wish, and unlock your birthday surprise!",
            messageTemplate = "Happy Birthday! Every year you bring more warmth, laughter, and magic into the world. May today surround you with everything that makes your heart smile.",
            suggestedGames = listOf("pop_balloons", "spin_the_wheel")
        ),
        SurpriseCategory(
            id = "love_letter",
            name = "Love Letter",
            emoji = "❤️",
            subtitle = "Sweet devotion sealed in a wax-stamped letter",
            accentHex = 0xFFBE123C,
            defaultThemeId = "blush_rose",
            defaultAnimation = "hearts",
            defaultTitle = "A Love Note Written Just For You",
            defaultIntro = "I tucked a few words from my heart inside this little gift box.",
            messageTemplate = "Loving you is my favorite part of every single day. Thank you for being my safe place, my brightest laugh, and my sweetest adventure.",
            suggestedGames = listOf("catch_hearts", "scratch_to_reveal")
        ),
        SurpriseCategory(
            id = "friendship",
            name = "Friendship Surprise",
            emoji = "👫",
            subtitle = "Celebrate your ride-or-die bestie",
            accentHex = 0xFF9333EA,
            defaultThemeId = "lavender_dream",
            defaultAnimation = "sparkles",
            defaultTitle = "To My Favorite Human & Best Friend",
            defaultIntro = "Besties don't wait for a holiday to celebrate each other!",
            messageTemplate = "Thank you for every late-night talk, endless inside joke, and moment you showed up without being asked. Life is infinitely better with you in my corner.",
            suggestedGames = listOf("memory_match", "tap_the_heart")
        ),
        SurpriseCategory(
            id = "anniversary",
            name = "Anniversary",
            emoji = "💍",
            subtitle = "Milestone memories & timeless promises",
            accentHex = 0xFFD97706,
            defaultThemeId = "golden_sunshine",
            defaultAnimation = "petals",
            defaultTitle = "Happy Anniversary, My Forever",
            defaultIntro = "Another chapter of us — unlock this keepsake of our favorite moments.",
            messageTemplate = "Looking back at every mile we've walked together fills me with gratitude. Here is to all our shared sunrises, quiet evenings, and every tomorrow yet to come.",
            suggestedGames = listOf("love_puzzle", "guess_secret_code")
        ),
        SurpriseCategory(
            id = "valentines",
            name = "Valentine's Day",
            emoji = "🌹",
            subtitle = "Roses, hearts & playful romance",
            accentHex = 0xFFE11D48,
            defaultThemeId = "blush_rose",
            defaultAnimation = "hearts",
            defaultTitle = "Will You Be My Forever Valentine?",
            defaultIntro = "Before you claim your roses, catch a few hearts for me!",
            messageTemplate = "You still give me butterflies every single time you smile. Happy Valentine's Day to the person who holds my whole heart.",
            suggestedGames = listOf("catch_hearts", "love_puzzle")
        ),
        SurpriseCategory(
            id = "apology",
            name = "Say Sorry",
            emoji = "🥺",
            subtitle = "Gentle peace offering from the heart",
            accentHex = 0xFF7C3AED,
            defaultThemeId = "lavender_dream",
            defaultAnimation = "petals",
            defaultTitle = "I'm Truly Sorry — Please Forgive Me",
            defaultIntro = "I made this little peace offering because you mean so much to me.",
            messageTemplate = "I never want to be the reason your smile fades. I am genuinely sorry for my mistake, and I value us more than pride. Let's talk when you're ready.",
            suggestedGames = listOf("scratch_to_reveal", "tap_the_heart")
        ),
        SurpriseCategory(
            id = "thank_you",
            name = "Thank You",
            emoji = "💌",
            subtitle = "Warm gratitude wrapped with care",
            accentHex = 0xFF059669,
            defaultThemeId = "emerald_garden",
            defaultAnimation = "sparkles",
            defaultTitle = "A Heartfelt Thank You!",
            defaultIntro = "Words alone weren't enough, so I wrapped my gratitude in a surprise.",
            messageTemplate = "Thank you from the bottom of my heart for your kindness and generosity. What you did made a bigger difference than you will ever know.",
            suggestedGames = listOf("spin_the_wheel", "memory_match")
        ),
        SurpriseCategory(
            id = "missing_you",
            name = "Missing You",
            emoji = "💕",
            subtitle = "Bridge the miles with a digital hug",
            accentHex = 0xFFEC4899,
            defaultThemeId = "midnight_starlight",
            defaultAnimation = "stars",
            defaultTitle = "Sending a Big Hug Across the Miles",
            defaultIntro = "Count the stars while you unlock this reminder that you're on my mind.",
            messageTemplate = "Distance is only geography — you are right here in every thought and every song I hear. Counting down the days until I get to see you again.",
            suggestedGames = listOf("tap_the_heart", "guess_secret_code")
        ),
        SurpriseCategory(
            id = "graduation",
            name = "Graduation",
            emoji = "🎓",
            subtitle = "Cheers to hard work & bright horizons",
            accentHex = 0xFFD97706,
            defaultThemeId = "golden_sunshine",
            defaultAnimation = "confetti",
            defaultTitle = "Hats Off to You, Graduate!",
            defaultIntro = "You earned every bit of this celebration — ready to pop some balloons?",
            messageTemplate = "All those late nights, perseverance, and courage brought you right here. I am endlessly proud of who you are and excited for everything you'll achieve next!",
            suggestedGames = listOf("pop_balloons", "spin_the_wheel")
        ),
        SurpriseCategory(
            id = "congratulations",
            name = "Congratulations",
            emoji = "🎉",
            subtitle = "Celebrate big wins & proud milestones",
            accentHex = 0xFFEA580C,
            defaultThemeId = "peach_sunset",
            defaultAnimation = "confetti",
            defaultTitle = "Huge Congratulations to You!",
            defaultIntro = "A milestone this awesome deserves confetti and a mini celebration!",
            messageTemplate = "You did it!! Watching your hard work pay off is pure joy. Celebrate big today — you deserve every cheer coming your way!",
            suggestedGames = listOf("pop_balloons", "spin_the_wheel")
        ),
        SurpriseCategory(
            id = "family_love",
            name = "Family Love",
            emoji = "🏡",
            subtitle = "Cozy appreciation for Mom, Dad & family",
            accentHex = 0xFF059669,
            defaultThemeId = "emerald_garden",
            defaultAnimation = "petals",
            defaultTitle = "Home Is Wherever You Are",
            defaultIntro = "A warm little keepsake to remind you how deeply you are loved.",
            messageTemplate = "Thank you for being my roots, my warmth, and my forever cheerleader. No matter how busy life gets, I am so grateful to belong to this family.",
            suggestedGames = listOf("memory_match", "scratch_to_reveal")
        ),
        SurpriseCategory(
            id = "just_because",
            name = "Just Because",
            emoji = "🌸",
            subtitle = "Spontaneous kindness for no reason at all",
            accentHex = 0xFFDB2777,
            defaultThemeId = "blush_rose",
            defaultAnimation = "petals",
            defaultTitle = "A Little Surprise, Just Because!",
            defaultIntro = "No special occasion — I just wanted to put a smile on your face today.",
            messageTemplate = "Consider this your gentle reminder that you are appreciated, noticed, and deeply cared for — even on an ordinary Tuesday.",
            suggestedGames = listOf("scratch_to_reveal", "catch_hearts")
        ),
        SurpriseCategory(
            id = "good_morning",
            name = "Good Morning",
            emoji = "☀️",
            subtitle = "Start someone's morning with sunshine",
            accentHex = 0xFFF59E0B,
            defaultThemeId = "golden_sunshine",
            defaultAnimation = "sparkles",
            defaultTitle = "Rise & Shine, Sunshine!",
            defaultIntro = "Pour your coffee and open this cheerful morning boost!",
            messageTemplate = "Good morning! Take a deep breath, step into today knowing someone is rooting for you, and let good things find you effortlessly.",
            suggestedGames = listOf("tap_the_heart", "spin_the_wheel")
        ),
        SurpriseCategory(
            id = "good_night",
            name = "Good Night",
            emoji = "🌙",
            subtitle = "Starry whispers before sweet dreams",
            accentHex = 0xFF8B5CF6,
            defaultThemeId = "midnight_starlight",
            defaultAnimation = "stars",
            defaultTitle = "Sweet Dreams Under the Stars",
            defaultIntro = "Before you drift off to sleep, open your cozy goodnight note.",
            messageTemplate = "Let go of everything heavy from today. You did enough, you are enough, and tomorrow holds gentle light. Sleep peacefully.",
            suggestedGames = listOf("memory_match", "scratch_to_reveal")
        ),
        SurpriseCategory(
            id = "motivation",
            name = "Motivation",
            emoji = "✨",
            subtitle = "Spark courage before a big moment",
            accentHex = 0xFFEA580C,
            defaultThemeId = "peach_sunset",
            defaultAnimation = "sparkles",
            defaultTitle = "You've Got This — Believe It!",
            defaultIntro = "A pocketful of courage and sparkles waiting inside for you!",
            messageTemplate = "Whenever doubt tries to whisper, remember how many mountains you have already climbed. Walk in tall, trust your preparation, and shine!",
            suggestedGames = listOf("spin_the_wheel", "tap_the_heart")
        ),
        SurpriseCategory(
            id = "get_well_soon",
            name = "Get Well Soon",
            emoji = "🌻",
            subtitle = "Gentle sunshine & healing warm wishes",
            accentHex = 0xFF10B981,
            defaultThemeId = "emerald_garden",
            defaultAnimation = "petals",
            defaultTitle = "Sending Warm Healing Vibes",
            defaultIntro = "Rest cozy while you unwrap a little basket of sunshine and smiles.",
            messageTemplate = "Sending you gentle hugs, warm tea energy, and brighter mornings ahead. Take all the rest you need — we miss your bright spirit!",
            suggestedGames = listOf("scratch_to_reveal", "pop_balloons")
        )
    )

    fun find(id: String): SurpriseCategory = all.firstOrNull { it.id == id } ?: all.first()

    fun randomTemplate(): SurpriseCategory = all.random()
}
