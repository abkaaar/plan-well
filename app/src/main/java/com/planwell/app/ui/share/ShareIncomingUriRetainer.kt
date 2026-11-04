package com.planwell.app.ui.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.planwell.app.domain.share.SharePayload
import java.io.File
import java.util.UUID

/**
 * Keeps share image URIs readable after the sending app's grant ends:
 * try persistable read permission, otherwise copy bytes into app cache.
 */
class ShareIncomingUriRetainer(
    private val context: Context,
) {
    fun retain(payload: SharePayload, intentFlags: Int): SharePayload {
        return when (payload) {
            is SharePayload.Text -> payload
            is SharePayload.Image -> SharePayload.Image(retainUri(payload.uri, intentFlags))
            is SharePayload.MultiImage -> SharePayload.MultiImage(
                payload.uris.map { retainUri(it, intentFlags) },
            )
        }
    }

    private fun retainUri(uri: Uri, intentFlags: Int): Uri {
        if (tryTakePersistable(uri, intentFlags)) {
            return uri
        }
        return copyToCache(uri)
    }

    private fun tryTakePersistable(uri: Uri, intentFlags: Int): Boolean {
        if (intentFlags and Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION == 0) {
            return false
        }
        if (intentFlags and Intent.FLAG_GRANT_READ_URI_PERMISSION == 0) {
            return false
        }
        return runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        }.isSuccess
    }

    private fun copyToCache(uri: Uri): Uri {
        val dir = File(context.cacheDir, CACHE_DIR).apply { mkdirs() }
        val ext = guessExtension(uri)
        val outFile = File(dir, "${UUID.randomUUID()}$ext")
        context.contentResolver.openInputStream(uri)?.use { input ->
            outFile.outputStream().use { output -> input.copyTo(output) }
        } ?: error("Could not read shared image")
        return Uri.fromFile(outFile)
    }

    private fun guessExtension(uri: Uri): String {
        val mime = context.contentResolver.getType(uri)?.lowercase().orEmpty()
        return when {
            mime.contains("png") -> ".png"
            mime.contains("webp") -> ".webp"
            mime.contains("gif") -> ".gif"
            mime.contains("jpeg") || mime.contains("jpg") -> ".jpg"
            else -> uri.lastPathSegment
                ?.substringAfterLast('.', missingDelimiterValue = "")
                ?.takeIf { it.length in 2..4 }
                ?.let { ".$it" }
                ?: ".img"
        }
    }

    companion object {
        const val CACHE_DIR = "share_incoming"
    }
}
