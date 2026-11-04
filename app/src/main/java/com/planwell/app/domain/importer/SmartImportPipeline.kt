package com.planwell.app.domain.importer

import android.content.Context
import android.net.Uri
import com.planwell.app.data.ml.TextExtractor
import com.planwell.app.data.ml.loadBitmap
import com.planwell.app.data.net.NetworkConnectivity
import com.planwell.app.data.net.OgMetadataExtractor
import com.planwell.app.data.preferences.UserPreferencesStore
import com.planwell.app.data.storage.ImageStore
import com.planwell.app.domain.extract.EntityExtractor
import com.planwell.app.domain.extract.EntityType
import com.planwell.app.domain.share.SharePayload
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SmartImportPipeline @Inject constructor(
    @ApplicationContext private val context: Context,
    private val textExtractor: TextExtractor,
    private val entityExtractor: EntityExtractor,
    private val entryBuilder: EntryBuilder,
    private val imageStore: ImageStore,
    private val ogMetadataExtractor: OgMetadataExtractor,
    private val preferences: UserPreferencesStore,
    private val connectivity: NetworkConnectivity,
) {
    fun process(payload: SharePayload): Flow<ImportStage> = flow {
        try {
            val referenceTime = System.currentTimeMillis()
            var sourceImagePath: String? = null

            val rawText = when (payload) {
                is SharePayload.Text -> {
                    emit(ImportStage.Progress("Reading text…"))
                    buildTextBody(payload.subject, payload.text)
                }
                is SharePayload.Image -> {
                    emit(ImportStage.Progress("Reading image…"))
                    sourceImagePath = imageStore.persistSharedImage(payload.uri)
                    ocrUri(payload.uri)
                }
                is SharePayload.MultiImage -> {
                    emit(ImportStage.Progress("Reading image…"))
                    if (payload.uris.isNotEmpty()) {
                        sourceImagePath = imageStore.persistSharedImage(payload.uris.first())
                    }
                    payload.uris.mapNotNull { uri ->
                        ocrUri(uri).takeIf { it.isNotBlank() }
                    }.joinToString("\n\n")
                }
            }

            if (rawText.isBlank()) {
                emit(ImportStage.Failed("Could not read any text from the share"))
                return@flow
            }

            var workingText = rawText

            val earlyUrl = extractFirstUrl(workingText)
            if (earlyUrl != null &&
                preferences.fetchLinkPreviewsOnce() &&
                connectivity.isOnline()
            ) {
                emit(ImportStage.Progress("Fetching link preview…"))
                val og = runCatching { ogMetadataExtractor.fetch(earlyUrl) }.getOrNull()
                if (og != null) {
                    workingText = mergeOg(workingText, og.title, og.description)
                }
            }

            emit(ImportStage.Progress("Finding dates…"))
            val entities = entityExtractor.extract(workingText, referenceTime)

            // If no URL found early but entities found one, try OG once
            if (earlyUrl == null) {
                val entityUrl = entities.firstOrNull { it.type == EntityType.URL }
                    ?.normalised
                    ?: entities.firstOrNull { it.type == EntityType.URL }?.rawText
                if (entityUrl != null &&
                    preferences.fetchLinkPreviewsOnce() &&
                    connectivity.isOnline()
                ) {
                    emit(ImportStage.Progress("Fetching link preview…"))
                    val og = runCatching { ogMetadataExtractor.fetch(entityUrl) }.getOrNull()
                    if (og != null) {
                        workingText = mergeOg(workingText, og.title, og.description)
                    }
                }
            }

            emit(ImportStage.Progress("Preparing entry…"))
            val finalEntities = if (workingText != rawText) {
                entityExtractor.extract(workingText, referenceTime)
            } else {
                entities
            }
            val entry = entryBuilder.build(
                text = workingText,
                entities = finalEntities,
                sourceImagePath = sourceImagePath,
                referenceTime = referenceTime,
            )
            emit(ImportStage.Ready(entry))
        } catch (e: Exception) {
            emit(ImportStage.Failed(e.message ?: "Import failed"))
        }
    }.flowOn(Dispatchers.Default)

    suspend fun processOnce(payload: SharePayload): ImportedEntry {
        var result: ImportedEntry? = null
        var error: String? = null
        process(payload).collect { stage ->
            when (stage) {
                is ImportStage.Ready -> result = stage.entry
                is ImportStage.Failed -> error = stage.message
                is ImportStage.Progress -> Unit
            }
        }
        error?.let { throw IllegalStateException(it) }
        return result ?: throw IllegalStateException("Import produced no entry")
    }

    private suspend fun ocrUri(uri: Uri): String = withContext(Dispatchers.IO) {
        val bitmap = context.contentResolver.loadBitmap(uri) ?: return@withContext ""
        try {
            textExtractor.extract(bitmap).fullText
        } finally {
            if (!bitmap.isRecycled) bitmap.recycle()
        }
    }

    private fun buildTextBody(subject: String?, text: String): String {
        val body = text.trim()
        val sub = subject?.trim().orEmpty()
        return when {
            sub.isNotEmpty() && body.isNotEmpty() && !body.contains(sub) -> "$sub\n$body"
            body.isNotEmpty() -> body
            else -> sub
        }
    }

    private fun extractFirstUrl(text: String): String? {
        val matcher = URL_HINT.matcher(text)
        return if (matcher.find()) matcher.group()?.trimEnd('.', ',', ';', ':', ')') else null
    }

    private fun mergeOg(text: String, title: String?, description: String?): String {
        val parts = mutableListOf<String>()
        title?.takeIf { it.isNotBlank() }?.let { parts += it }
        description?.takeIf { it.isNotBlank() }?.let { parts += it }
        if (parts.isEmpty()) return text
        val ogBlock = parts.joinToString("\n")
        return if (text.contains(ogBlock)) text else "$ogBlock\n\n$text"
    }

    companion object {
        private val URL_HINT = java.util.regex.Pattern.compile(
            """(?i)\b((?:https?://|www\.)[^\s<>"']+|[\w.-]+\.(?:com|org|net|edu|io|co|uk|gov|ng)(?:/[^\s<>"']*)?)""",
        )
    }
}
