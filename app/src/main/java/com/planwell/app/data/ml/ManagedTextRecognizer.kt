package com.planwell.app.data.ml

import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.Closeable

/**
 * Lazily creates the ML Kit [TextRecognizer] with
 * [TextRecognizerOptions.DEFAULT_OPTIONS] so unit tests that never OCR do not
 * require MlKitContext during Application onCreate.
 */
class ManagedTextRecognizer : Closeable {

    private val lock = Any()
    private var client: TextRecognizer? = null

    fun process(image: InputImage): Task<Text> = clientOrCreate().process(image)

    private fun clientOrCreate(): TextRecognizer = synchronized(lock) {
        client ?: TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS).also {
            client = it
        }
    }

    override fun close() {
        synchronized(lock) {
            client?.close()
            client = null
        }
    }
}
