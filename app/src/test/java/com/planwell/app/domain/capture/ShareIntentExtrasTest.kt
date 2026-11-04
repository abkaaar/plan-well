package com.planwell.app.domain.capture

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ShareIntentExtrasTest {

    @Test
    fun detectsImageMimes() {
        assertTrue(ShareIntentExtras.isImageMime("image/jpeg"))
        assertTrue(ShareIntentExtras.isImageMime("image/png"))
        assertTrue(ShareIntentExtras.isImageMime("IMAGE/WEBP"))
        assertFalse(ShareIntentExtras.isImageMime("text/plain"))
        assertFalse(ShareIntentExtras.isImageMime(null))
    }

    @Test
    fun failClosedWhenImageStreamMissing() {
        assertTrue(
            ShareIntentExtras.shouldFailClosedOnMissingImageStream(
                mimeType = "image/jpeg",
                hasStreamUri = true,
                streamOpened = false,
            ),
        )
        assertTrue(
            ShareIntentExtras.shouldFailClosedOnMissingImageStream(
                mimeType = null,
                hasStreamUri = true,
                streamOpened = false,
            ),
        )
        assertFalse(
            ShareIntentExtras.shouldFailClosedOnMissingImageStream(
                mimeType = "image/jpeg",
                hasStreamUri = true,
                streamOpened = true,
            ),
        )
        assertFalse(
            ShareIntentExtras.shouldFailClosedOnMissingImageStream(
                mimeType = "text/plain",
                hasStreamUri = false,
                streamOpened = false,
            ),
        )
    }
}
