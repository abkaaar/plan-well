package com.planwell.app.data.net

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.jsoup.Jsoup
import java.net.URI
import javax.inject.Inject
import javax.inject.Singleton

data class OgMetadata(
    val title: String?,
    val description: String?,
    val imageUrl: String?,
)

@Singleton
class OgMetadataExtractor @Inject constructor() {

    suspend fun fetch(url: String): OgMetadata? = withContext(Dispatchers.IO) {
        withTimeoutOrNull(TIMEOUT_MS) {
            runCatching {
                val absolute = normalizeUrl(url) ?: return@runCatching null
                val doc = Jsoup.connect(absolute)
                    .userAgent(USER_AGENT)
                    .timeout(TIMEOUT_MS.toInt())
                    .followRedirects(true)
                    .get()
                val title = doc.selectFirst("meta[property=og:title]")?.attr("content")
                    ?.trim()
                    ?.ifBlank { null }
                    ?: doc.title()?.trim()?.ifBlank { null }
                val description = doc.selectFirst("meta[property=og:description]")?.attr("content")
                    ?.trim()
                    ?.ifBlank { null }
                val imageUrl = doc.selectFirst("meta[property=og:image]")?.attr("content")
                    ?.trim()
                    ?.ifBlank { null }
                if (title == null && description == null && imageUrl == null) null
                else OgMetadata(title, description, imageUrl)
            }.getOrNull()
        }
    }

    private fun normalizeUrl(url: String): String? {
        val trimmed = url.trim()
        if (trimmed.isEmpty()) return null
        val withScheme = if (trimmed.startsWith("http://", true) || trimmed.startsWith("https://", true)) {
            trimmed
        } else {
            "https://$trimmed"
        }
        return runCatching { URI(withScheme).toString() }.getOrNull()
    }

    companion object {
        private const val TIMEOUT_MS = 5_000L
        private const val USER_AGENT = "PlanWell/1.0 (Android; link-preview)"
    }
}
