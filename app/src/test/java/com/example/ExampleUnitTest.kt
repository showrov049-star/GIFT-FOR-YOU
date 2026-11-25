package com.example

import com.example.data.audio.SoundSettings
import com.example.data.model.CategoryCatalog
import com.example.data.model.MiniGameType
import com.example.data.model.Surprise
import com.example.data.repository.GiftRepository
import com.example.data.util.QrCodeGenerator
import com.example.ui.screens.SurpriseSortOption
import com.example.ui.screens.filterAndSortSurprises
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
    @Test
    fun categoryCatalog_hasAllSixteenCategories() {
        assertEquals(16, CategoryCatalog.all.size)
        assertEquals("Birthday Surprise", CategoryCatalog.find("birthday").name)
        assertEquals("Get Well Soon", CategoryCatalog.find("get_well_soon").name)
    }

    @Test
    fun miniGames_hasAllEightGames() {
        assertEquals(8, MiniGameType.entries.size)
        assertEquals(MiniGameType.SCRATCH_TO_REVEAL, MiniGameType.fromId("scratch_to_reveal"))
    }

    @Test
    fun extractSurpriseId_handlesUrlAndRawCode() {
        assertEquals("gfy_abc123", GiftRepository.extractSurpriseId("https://giftforyou.app/s/gfy_abc123"))
        assertEquals("gfy_abc123", GiftRepository.extractSurpriseId("giftforyou://s/gfy_abc123"))
        assertEquals("gfy_abc123", GiftRepository.extractSurpriseId("  gfy_abc123  "))
    }

    @Test
    fun qrCodeGenerator_producesSquareMatrix() {
        val qr = QrCodeGenerator.encode("https://giftforyou.app/s/gfy_test123456")
        assertTrue(qr.size >= 21)
        assertEquals(qr.size, qr.modules.size)
        assertEquals(qr.size, qr.modules[0].size)
    }

    @Test
    fun soundSettings_hasSensibleDefaults() {
        val defaults = SoundSettings()
        assertFalse(defaults.mutedAll)
        assertTrue(defaults.masterVolume in 0f..1f)
        assertTrue(defaults.musicVolume in 0f..1f)
        assertTrue(defaults.sfxVolume in 0f..1f)
    }

    @Test
    fun filterAndSortSurprises_filtersByStatusAndQueryAndSorts() {
        val items = listOf(
            Surprise(id = "gfy_1", title = "Zebra Birthday", recipientName = "Maya", status = "published", openCount = 2, replyCount = 1),
            Surprise(id = "gfy_2", title = "Anniversary Love", recipientName = "Alex", status = "replied", openCount = 10, replyCount = 4),
            Surprise(id = "gfy_3", title = "Draft Note", recipientName = "Maya", status = "draft", openCount = 0, replyCount = 0)
        )

        val mayaOnly = filterAndSortSurprises(items, "all", "maya", SurpriseSortOption.TITLE_AZ)
        assertEquals(listOf("gfy_3", "gfy_1"), mayaOnly.map { it.id })

        val mostOpened = filterAndSortSurprises(items, "all", "", SurpriseSortOption.MOST_OPENED)
        assertEquals(listOf("gfy_2", "gfy_1", "gfy_3"), mostOpened.map { it.id })

        val repliedOnly = filterAndSortSurprises(items, "replied", "", SurpriseSortOption.MOST_REPLIED)
        assertEquals(listOf("gfy_2"), repliedOnly.map { it.id })
    }
}
