package com.planwell.app.domain.capture

/**
 * Pure helpers for classifying share payloads (unit-testable without Android framework).
 */
object ShareIntentExtras {

    fun isImageMime(mimeType: String?): Boolean {
        val type = mimeType?.lowercase().orEmpty()
        return type.startsWith("image/")
    }

    /**
     * When the sender clearly shared an image (MIME or stream present) but the
     * stream cannot be opened, surface an error instead of silently saving text.
     */
    fun shouldFailClosedOnMissingImageStream(
        mimeType: String?,
        hasStreamUri: Boolean,
        streamOpened: Boolean,
    ): Boolean {
        if (!hasStreamUri) return false
        if (streamOpened) return false
        return isImageMime(mimeType) || hasStreamUri
    }
}
