package com.keephub.app.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit

/**
 * Parses markdown text containing **bold**, *italic*, and `code` tags into an [AnnotatedString]
 * with appropriate [SpanStyle] styling, stripping literal formatting markers.
 */
fun parseMarkdownToAnnotatedString(
    text: String,
    highlightBoldColor: Color? = null
): AnnotatedString {
    if (text.isBlank()) return AnnotatedString("")

    return buildAnnotatedString {
        val regex = Regex("""(\*\*(.+?)\*\*|\*([^*\n]+?)\*|`([^`\n]+?)`)""")
        var cursor = 0

        for (match in regex.findAll(text)) {
            val range = match.range
            if (range.first > cursor) {
                append(text.substring(cursor, range.first))
            }

            val fullMatch = match.value
            when {
                fullMatch.startsWith("**") && fullMatch.endsWith("**") && fullMatch.length >= 4 -> {
                    val inner = match.groupValues[2]
                    val start = length
                    append(inner)
                    addStyle(
                        style = SpanStyle(
                            fontWeight = FontWeight.Bold,
                            color = highlightBoldColor ?: Color.Unspecified
                        ),
                        start = start,
                        end = length
                    )
                }
                fullMatch.startsWith("*") && fullMatch.endsWith("*") && fullMatch.length >= 2 -> {
                    val inner = match.groupValues[3]
                    val start = length
                    append(inner)
                    addStyle(
                        style = SpanStyle(fontStyle = FontStyle.Italic),
                        start = start,
                        end = length
                    )
                }
                fullMatch.startsWith("`") && fullMatch.endsWith("`") && fullMatch.length >= 2 -> {
                    val inner = match.groupValues[4]
                    val start = length
                    append(inner)
                    addStyle(
                        style = SpanStyle(fontFamily = FontFamily.Monospace),
                        start = start,
                        end = length
                    )
                }
                else -> append(fullMatch)
            }
            cursor = range.last + 1
        }

        if (cursor < text.length) {
            append(text.substring(cursor))
        }
    }
}

/**
 * A lightweight Compose text element that formats markdown **bold**, *italic*, and `code` spans.
 */
@Composable
fun MarkdownText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    boldColor: Color? = null,
    lineHeight: TextUnit = TextUnit.Unspecified
) {
    val annotated = remember(text, boldColor) {
        parseMarkdownToAnnotatedString(text, boldColor)
    }
    Text(
        text = annotated,
        modifier = modifier,
        style = style,
        color = color,
        lineHeight = lineHeight
    )
}
