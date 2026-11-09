package com.keephub.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.keephub.app.ui.components.parseMarkdownToAnnotatedString
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MarkdownTextTest {

    @Test
    fun testEmptyAndBlankStrings() {
        val empty = parseMarkdownToAnnotatedString("")
        assertEquals("", empty.text)

        val blank = parseMarkdownToAnnotatedString("   ")
        assertEquals("", blank.text)
    }

    @Test
    fun testPlainTextWithoutMarkdown() {
        val input = "Just a plain sentence with no special formatting."
        val result = parseMarkdownToAnnotatedString(input)
        assertEquals(input, result.text)
        assertTrue(result.spanStyles.isEmpty())
    }

    @Test
    fun testSingleBoldWord() {
        val input = "The **ephemeral** beauty of morning dew."
        val result = parseMarkdownToAnnotatedString(input)

        // Asterisks stripped in final text
        assertEquals("The ephemeral beauty of morning dew.", result.text)

        // One bold style span applied to "ephemeral"
        assertEquals(1, result.spanStyles.size)
        val span = result.spanStyles.first()
        assertEquals(4, span.start)
        assertEquals(13, span.end)
        assertEquals(FontWeight.Bold, span.item.fontWeight)
    }

    @Test
    fun testMultipleBoldSpansWithColor() {
        val input = "**First** word and **Second** word."
        val customColor = Color.Blue
        val result = parseMarkdownToAnnotatedString(input, highlightBoldColor = customColor)

        assertEquals("First word and Second word.", result.text)
        assertEquals(2, result.spanStyles.size)

        val firstSpan = result.spanStyles[0]
        assertEquals(0, firstSpan.start)
        assertEquals(5, firstSpan.end)
        assertEquals(FontWeight.Bold, firstSpan.item.fontWeight)
        assertEquals(customColor, firstSpan.item.color)

        val secondSpan = result.spanStyles[1]
        assertEquals(15, secondSpan.start)
        assertEquals(21, secondSpan.end)
        assertEquals(FontWeight.Bold, secondSpan.item.fontWeight)
        assertEquals(customColor, secondSpan.item.color)
    }

    @Test
    fun testStoryFormattingWithColonsAndPunctuation() {
        val story = "The student remained **resilient** throughout exams. **Takeaway:** Persistence pays."
        val result = parseMarkdownToAnnotatedString(story)

        assertEquals("The student remained resilient throughout exams. Takeaway: Persistence pays.", result.text)
        assertEquals(2, result.spanStyles.size)
        assertEquals(FontWeight.Bold, result.spanStyles[0].item.fontWeight)
        assertEquals(FontWeight.Bold, result.spanStyles[1].item.fontWeight)
    }
}
