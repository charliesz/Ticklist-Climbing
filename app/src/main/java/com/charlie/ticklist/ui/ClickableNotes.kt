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
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.sp

private val markdownLinkPattern = Regex(
    """\[([^\]]+)]\((https?://[^\s)]+)\)"""
)

private val directUrlPattern = Regex(
    """https?://[^\s<>"')\]]+"""
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
            color = Color.Unspecified,
            fontSize = 12.sp
        ),
        onClick = { offset ->
            val annotation = annotatedNotes
                .getStringAnnotations(
                    tag = "URL",
                    start = 0,
                    end = annotatedNotes.length
                )
                .firstOrNull { currentAnnotation ->
                    offset >= currentAnnotation.start &&
                            offset < currentAnnotation.end
                }

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
    val markdownLinks = markdownLinkPattern
        .findAll(notes)
        .map { match ->
            LinkMatch(
                sourceStart = match.range.first,
                sourceEndExclusive = match.range.last + 1,
                visibleText = match.groupValues[1],
                targetUrl = match.groupValues[2]
            )
        }
        .toList()

    val directLinks = directUrlPattern
        .findAll(notes)
        .filter { directMatch ->
            markdownLinks.none { markdownLink ->
                directMatch.range.first >= markdownLink.sourceStart &&
                        directMatch.range.last <
                        markdownLink.sourceEndExclusive
            }
        }
        .map { match ->
            val cleanedUrl = removeTrailingPunctuation(
                match.value
            )

            LinkMatch(
                sourceStart = match.range.first,
                sourceEndExclusive = match.range.last + 1,
                visibleText = cleanedUrl,
                targetUrl = cleanedUrl
            )
        }
        .toList()

    val links = (
            markdownLinks + directLinks
            ).sortedBy {
            it.sourceStart
        }

    return AnnotatedString.Builder().apply {
        var sourceIndex = 0

        for (link in links) {
            if (link.sourceStart > sourceIndex) {
                append(
                    notes.substring(
                        sourceIndex,
                        link.sourceStart
                    )
                )
            }

            val visibleLinkStart = length

            withStyle(
                style = SpanStyle(
                    color = Color(0xFF4A90E2),
                    textDecoration = TextDecoration.Underline
                )
            ) {
                append(link.visibleText)
            }

            addStringAnnotation(
                tag = "URL",
                annotation = link.targetUrl,
                start = visibleLinkStart,
                end = length
            )

            if (
                link.visibleText !=
                notes.substring(
                    link.sourceStart,
                    link.sourceEndExclusive
                )
            ) {
                val sourceText = notes.substring(
                    link.sourceStart,
                    link.sourceEndExclusive
                )

                val trailingText =
                    sourceText.removePrefix(
                        "[${link.visibleText}](${link.targetUrl})"
                    )

                if (
                    trailingText.isNotEmpty() &&
                    !sourceText.startsWith("[")
                ) {
                    append(trailingText)
                }
            }

            sourceIndex = link.sourceEndExclusive
        }

        if (sourceIndex < notes.length) {
            append(notes.substring(sourceIndex))
        }
    }.toAnnotatedString()
}

private fun removeTrailingPunctuation(
    url: String
): String {
    return url.trimEnd(
        '.',
        ',',
        ';',
        ':',
        ')',
        ']'
    )
}

private data class LinkMatch(
    val sourceStart: Int,
    val sourceEndExclusive: Int,
    val visibleText: String,
    val targetUrl: String
)
