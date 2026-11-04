package com.planwell.app.domain.share

import android.net.Uri

sealed class SharePayload {
    data class Text(
        val text: String,
        val subject: String?,
    ) : SharePayload()

    data class Image(
        val uri: Uri,
    ) : SharePayload()

    data class MultiImage(
        val uris: List<Uri>,
    ) : SharePayload()
}
