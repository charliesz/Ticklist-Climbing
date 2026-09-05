package com.charlie.ticklist.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.text.ClickableText
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.sp

private val urlPattern = Regex(
    """https?://[^\s<>"']+"""
)

@Composable
fun ClickableNotes(
    notes: String,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current

    val annotatedNotes = buildAnnotatedNotes(notes)

    ClickableText(
        text = annotatedNotes,
        modifier = modifier,
        style = TextStyle(
            fontSize = 14.sp,
            color = Color.Unspecified
        ),
        onClick = { offset ->
            val annotation = annotatedNotes
                .getStringAnnotations(
                    tag = "URL",
                    start = offset,
                    end = offset
                )
                .firstOrNull()

            if (annotation != null) {
                val intent = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse(annotation.item)
                )

                context.startActivity(intent)
            }
        }
    )
}

private fun buildAnnotatedNotes(
    notes: String
): AnnotatedString {
    return AnnotatedString.Builder().apply {
        var currentIndex = 0

        urlPattern.findAll(notes).forEach { match ->
            val start = match.range.first
            val endExclusive = match.range.last + 1

            if (start > currentIndex) {
                append(
                    notes.substring(
                        currentIndex,
                        start
                    )
                )
            }

            val rawUrl = match.value
            val trailingCharacters = rawUrl
                .takeLastWhile {
                    it == '.' ||
                            it == ',' ||
                            it == ';' ||
                            it == ':' ||
                            it == ')' ||
                            it == ']'
                }

            val url = if (trailingCharacters.isEmpty()) {
                rawUrl
            } else {
                rawUrl.dropLast(
                    trailingCharacters.length
                )
            }

            val urlStart = length

            withStyle(
                style = SpanStyle(
                    color = Color(0xFF4A90E2),
                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                )
            ) {
                append(url)
            }

            addStringAnnotation(
                tag = "URL",
                annotation = url,
                start = urlStart,
                end = length
            )

            if (trailingCharacters.isNotEmpty()) {
                append(trailingCharacters)
            }

            currentIndex = endExclusive
        }

        if (currentIndex < notes.length) {
            append(notes.substring(currentIndex))
        }
    }.toAnnotatedString()
}
