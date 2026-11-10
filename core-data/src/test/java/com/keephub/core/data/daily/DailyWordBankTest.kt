package com.keephub.core.data.daily

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDate

class DailyWordBankTest {

    @Test
    fun allLevels_haveSufficientCuratedWords() {
        for (level in VocabularyLevel.entries) {
            val words = DailyWordBank.words[level]
            assertNotNull("Words for level $level should exist", words)
            assertTrue("Level $level should have at least 25 words", (words?.size ?: 0) >= 25)
            // Verify every word has complete metadata
            words?.forEach { w ->
                assertTrue("Term should not be blank", w.term.isNotBlank())
                assertTrue("POS should not be blank", w.pos.isNotBlank())
                assertTrue("Definition should not be blank", w.definition.isNotBlank())
                assertTrue("Example should not be blank", w.example.isNotBlank())
                assertTrue("Mnemonic should not be blank", w.mnemonic.isNotBlank())
                assertEquals(level, w.level)
            }
        }
    }

    @Test
    fun getWordsForDate_returnsExactlyFiveWords() {
        val today = LocalDate.of(2026, 11, 10)
        for (level in VocabularyLevel.entries) {
            val dailyWords = DailyWordBank.getWordsForDate(level, today)
            assertEquals(5, dailyWords.size)
            // All 5 words should be distinct
            assertEquals(5, dailyWords.map { it.term }.distinct().size)
        }
    }

    @Test
    fun getWordsForDate_consecutiveDays_rotateWords() {
        val day1 = LocalDate.of(2026, 11, 10)
        val day2 = LocalDate.of(2026, 11, 11)

        val wordsDay1 = DailyWordBank.getWordsForDate(VocabularyLevel.INTERMEDIATE, day1).map { it.term }
        val wordsDay2 = DailyWordBank.getWordsForDate(VocabularyLevel.INTERMEDIATE, day2).map { it.term }

        // Starting sets should differ between consecutive days
        assertNotEquals(wordsDay1, wordsDay2)
    }

    @Test
    fun getWordsForDate_skipsExistingTerms() {
        val date = LocalDate.of(2026, 11, 10)
        val initialWords = DailyWordBank.getWordsForDate(VocabularyLevel.BEGINNER, date)
        val existing = setOf(initialWords[0].term.lowercase())

        val newWords = DailyWordBank.getWordsForDate(VocabularyLevel.BEGINNER, date, existingTerms = existing)
        assertEquals(5, newWords.size)
        assertFalse(
            "Should not include already existing word",
            newWords.any { it.term.equals(initialWords[0].term, ignoreCase = true) }
        )
    }

    @Test
    fun vocabularyLevel_fromCode_defaultsSafely() {
        assertEquals(VocabularyLevel.BEGINNER, VocabularyLevel.fromCode("BEGINNER"))
        assertEquals(VocabularyLevel.BEGINNER, VocabularyLevel.fromCode("beginner"))
        assertEquals(VocabularyLevel.ADVANCED, VocabularyLevel.fromCode("ADVANCED"))
        assertEquals(VocabularyLevel.INTERMEDIATE, VocabularyLevel.fromCode("unknown"))
        assertEquals(VocabularyLevel.INTERMEDIATE, VocabularyLevel.fromCode(null))
    }
}
