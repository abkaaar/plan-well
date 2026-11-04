package com.planwell.app.domain.extract

/**
 * Port for turning raw shared/OCR text into structured entities.
 * Downstream code must depend on this interface only — never the impl type.
 */
interface EntityExtractor {
    suspend fun extract(text: String, referenceTime: Long): List<ExtractedEntity>
}
