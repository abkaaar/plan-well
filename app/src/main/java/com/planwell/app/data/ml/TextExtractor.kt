package com.planwell.app.data.ml

import android.graphics.Bitmap
import android.util.Log
import androidx.annotation.VisibleForTesting
import com.google.android.gms.tasks.Task
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.Text
import com.planwell.app.BuildConfig
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * On-device ML Kit Latin text recognition. Never throws — corrupt or failed
 * recognition yields [TextExtractionResult.EMPTY].
 */
@Singleton
class TextExtractor private constructor(
    private val process: (InputImage) -> Task<Text>,
) {
    @Inject
    constructor(managed: ManagedTextRecognizer) : this(managed::process)

    suspend fun extract(bitmap: Bitmap): TextExtractionResult {
        return try {
            val image = InputImage.fromBitmap(bitmap, 0)
            val visionText = awaitText(image) ?: return TextExtractionResult.EMPTY
            val result = mapVisionText(visionText)
            if (BuildConfig.DEBUG) {
                Log.d(
                    TAG,
                    "extracted ${result.blocks.size} blocks, " +
                        "${result.fullText.length} chars:\n${result.fullText}",
                )
            }
            result
        } catch (_: Exception) {
            TextExtractionResult.EMPTY
        }
    }

    private suspend fun awaitText(image: InputImage): Text? =
        suspendCancellableCoroutine { cont ->
            val task = process(image)
            task.addOnSuccessListener { text ->
                if (cont.isActive) cont.resume(text)
            }
            task.addOnFailureListener {
                if (cont.isActive) cont.resume(null)
            }
            cont.invokeOnCancellation {
                runCatching {
                    val cancel = task.javaClass.methods.find {
                        it.name == "cancel" && it.parameterCount == 0
                    }
                    cancel?.invoke(task)
                }
            }
        }

    companion object {
        const val ENGINE_ID = "MLKIT_LATIN_V2"
        private const val TAG = "TextExtractor"

        @VisibleForTesting
        fun createForTest(process: (InputImage) -> Task<Text>): TextExtractor =
            TextExtractor(process)

        /** Preserves ML Kit block order; confidence is the mean of line confidences. */
        @VisibleForTesting
        internal fun mapVisionText(visionText: Text): TextExtractionResult {
            val blocks = visionText.textBlocks.map { block ->
                TextBlock(
                    text = block.text,
                    confidence = meanLineConfidence(block),
                )
            }
            return TextExtractionResult(
                fullText = visionText.text.trim(),
                blocks = blocks,
            )
        }

        @VisibleForTesting
        internal fun mapBlocks(
            fullText: String,
            blocks: List<Pair<String, List<Float>>>,
        ): TextExtractionResult {
            return TextExtractionResult(
                fullText = fullText.trim(),
                blocks = blocks.map { (text, lineConfidences) ->
                    TextBlock(text = text, confidence = meanConfidence(lineConfidences))
                },
            )
        }

        private fun meanLineConfidence(block: Text.TextBlock): Float {
            val confidences = block.lines.mapNotNull { line ->
                runCatching { line.confidence }.getOrNull()?.takeIf { it in 0f..1f }
            }
            return meanConfidence(confidences)
        }

        private fun meanConfidence(confidences: List<Float>): Float {
            if (confidences.isEmpty()) return 0f
            return confidences.average().toFloat()
        }
    }
}
