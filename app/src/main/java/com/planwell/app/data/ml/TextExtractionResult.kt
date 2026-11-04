package com.planwell.app.data.ml

data class TextBlock(
    val text: String,
    val confidence: Float,
)

data class TextExtractionResult(
    val fullText: String,
    val blocks: List<TextBlock>,
) {
    companion object {
        val EMPTY = TextExtractionResult(fullText = "", blocks = emptyList())
    }
}
