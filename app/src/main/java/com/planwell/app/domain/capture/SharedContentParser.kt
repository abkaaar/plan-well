package com.planwell.app.domain.capture

import com.planwell.app.domain.model.SourcePlatform
import java.util.Locale
import java.util.regex.Pattern

data class ParsedSharedContent(
    val title: String,
    val description: String?,
    val sourceUrl: String?,
    val sourcePlatform: SourcePlatform,
)

/**
 * Pure parsing of shared text / subject into opportunity fields.
 * Does not touch the database or filesystem.
 */
object SharedContentParser {

    private val urlPattern = Pattern.compile(
        """(?i)\b((?:https?://|www\.)[^\s<>"']+)""",
    )

    fun parse(
        subject: String?,
        text: String?,
        fallbackTitle: String = "Shared item",
    ): ParsedSharedContent {
        val subjectTrim = subject?.trim()?.takeIf { it.isNotEmpty() }
        val textTrim = text?.trim()?.takeIf { it.isNotEmpty() }
        val combined = listOfNotNull(subjectTrim, textTrim).joinToString("\n")

        val url = extractFirstUrl(combined)
        val platform = inferSourcePlatform(url)

        val title = when {
            subjectTrim != null && !looksLikeUrl(subjectTrim) -> subjectTrim.take(200)
            textTrim != null -> {
                val firstLine = textTrim.lineSequence()
                    .map { it.trim() }
                    .firstOrNull { it.isNotEmpty() && !looksLikeUrl(it) }
                firstLine?.take(200)
                    ?: url?.let { hostLabel(it) }
                    ?: fallbackTitle
            }
            url != null -> hostLabel(url)
            else -> fallbackTitle
        }

        val description = textTrim?.let { body ->
            if (body == title || looksLikeUrl(body)) null else body.take(2_000)
        }

        return ParsedSharedContent(
            title = title.ifBlank { fallbackTitle },
            description = description,
            sourceUrl = url,
            sourcePlatform = platform,
        )
    }

    fun inferSourcePlatform(url: String?): SourcePlatform {
        if (url.isNullOrBlank()) return SourcePlatform.UNKNOWN
        val host = hostOf(url)?.lowercase(Locale.US) ?: return SourcePlatform.UNKNOWN
        return when {
            host.contains("instagram.com") || host.contains("instagr.am") ->
                SourcePlatform.INSTAGRAM
            host.contains("twitter.com") ||
                host.contains("x.com") ||
                host.contains("facebook.com") ||
                host.contains("fb.com") ||
                host.contains("tiktok.com") ||
                host.contains("linkedin.com") ->
                SourcePlatform.OTHER
            else -> SourcePlatform.BROWSER
        }
    }

    fun extractFirstUrl(text: String): String? {
        val matcher = urlPattern.matcher(text)
        if (!matcher.find()) return null
        var url = matcher.group(1) ?: return null
        while (url.endsWith(".") || url.endsWith(",") || url.endsWith(")")) {
            url = url.dropLast(1)
        }
        return if (url.startsWith("http", ignoreCase = true)) url else "https://$url"
    }

    private fun looksLikeUrl(value: String): Boolean =
        urlPattern.matcher(value).find() || value.contains("www.", ignoreCase = true)

    private fun hostOf(url: String): String? =
        try {
            val normalized = if (url.startsWith("http", ignoreCase = true)) url else "https://$url"
            java.net.URI(normalized).host
        } catch (_: Exception) {
            null
        }

    private fun hostLabel(url: String): String =
        hostOf(url)?.removePrefix("www.")?.take(80) ?: "Shared link"
}
