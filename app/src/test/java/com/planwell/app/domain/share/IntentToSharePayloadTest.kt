package com.planwell.app.domain.share

import android.content.Intent
import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IntentToSharePayloadTest {

    @Test
    fun textOnly() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "https://example.com/post")
        }

        val payload = intent.toSharePayload()

        assertEquals(
            SharePayload.Text(text = "https://example.com/post", subject = null),
            payload,
        )
    }

    @Test
    fun textPlusSubject() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "Internship deadline")
            putExtra(Intent.EXTRA_TEXT, "Apply by Friday")
        }

        val payload = intent.toSharePayload()

        assertEquals(
            SharePayload.Text(text = "Apply by Friday", subject = "Internship deadline"),
            payload,
        )
    }

    @Test
    fun singleImage() {
        val uri = Uri.parse("content://media/external/images/media/42")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/jpeg"
            putExtra(Intent.EXTRA_STREAM, uri)
        }

        val payload = intent.toSharePayload()

        assertEquals(SharePayload.Image(uri), payload)
    }

    @Test
    fun multipleImages() {
        val uris = arrayListOf(
            Uri.parse("content://media/external/images/media/1"),
            Uri.parse("content://media/external/images/media/2"),
        )
        val intent = Intent(Intent.ACTION_SEND_MULTIPLE).apply {
            type = "image/*"
            putParcelableArrayListExtra(Intent.EXTRA_STREAM, uris)
        }

        val payload = intent.toSharePayload()

        assertEquals(SharePayload.MultiImage(uris), payload)
    }

    @Test
    fun emptyIntent_returnsNull() {
        assertNull(Intent().toSharePayload())
    }

    @Test
    fun unknownAction_returnsNull() {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "hello")
        }
        assertNull(intent.toSharePayload())
    }

    @Test
    fun sendWithBlankText_returnsNull() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, "   ")
        }
        assertNull(intent.toSharePayload())
    }

    @Test
    fun imageMimeWithoutStream_returnsNull() {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/png"
        }
        assertNull(intent.toSharePayload())
    }

    @Test
    fun imagePlusCaption_prefersImage() {
        val uri = Uri.parse("content://media/external/images/media/9")
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "image/*"
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_TEXT, "Nice shot")
        }

        val payload = intent.toSharePayload()

        assertTrue(payload is SharePayload.Image)
        assertEquals(uri, (payload as SharePayload.Image).uri)
    }
}
