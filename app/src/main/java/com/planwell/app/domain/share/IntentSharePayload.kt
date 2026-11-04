package com.planwell.app.domain.share

import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat
import com.planwell.app.domain.capture.ShareIntentExtras

/**
 * Maps a share [Intent] into a typed [SharePayload], or null when unrecognized / empty.
 *
 * When both a stream and text are present, prefers [SharePayload.Image] (common for
 * image + caption shares from Instagram, Chrome, etc.).
 */
fun Intent.toSharePayload(): SharePayload? {
    return when (action) {
        Intent.ACTION_SEND -> parseSend()
        Intent.ACTION_SEND_MULTIPLE -> parseSendMultiple()
        else -> null
    }
}

private fun Intent.parseSend(): SharePayload? {
    val streamUri = singleStreamUri()
    // Prefer image when a stream is present (covers image+caption shares).
    if (streamUri != null) {
        return SharePayload.Image(streamUri)
    }
    // Image MIME without a readable stream is unrecognized for this phase.
    if (ShareIntentExtras.isImageMime(type)) {
        return null
    }
    return parseTextPayload()
}

private fun Intent.parseSendMultiple(): SharePayload? {
    val uris = multipleStreamUris()
    if (uris.isEmpty()) return parseTextPayload()
    return if (uris.size == 1) {
        SharePayload.Image(uris.first())
    } else {
        SharePayload.MultiImage(uris)
    }
}

private fun Intent.parseTextPayload(): SharePayload.Text? {
    val text = getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }
    val subject = getStringExtra(Intent.EXTRA_SUBJECT)?.takeIf { it.isNotBlank() }
    if (text == null && subject == null) return null
    // Subject-only shares still yield Text with subject as body and no separate subject.
    return if (text != null) {
        SharePayload.Text(text = text, subject = subject)
    } else {
        SharePayload.Text(text = subject.orEmpty(), subject = null)
    }
}

private fun Intent.singleStreamUri(): Uri? {
    IntentCompat.getParcelableExtra(this, Intent.EXTRA_STREAM, Uri::class.java)?.let { return it }
    clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri?.let { return it }
    return null
}

private fun Intent.multipleStreamUris(): List<Uri> {
    val fromExtra = IntentCompat.getParcelableArrayListExtra(this, Intent.EXTRA_STREAM, Uri::class.java)
        .orEmpty()
        .filterNotNull()
    if (fromExtra.isNotEmpty()) return fromExtra

    val clip = clipData ?: return emptyList()
    return buildList {
        for (i in 0 until clip.itemCount) {
            clip.getItemAt(i).uri?.let { add(it) }
        }
    }
}
