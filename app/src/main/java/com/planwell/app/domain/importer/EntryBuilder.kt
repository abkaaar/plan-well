package com.planwell.app.domain.importer

import com.planwell.app.domain.classify.IntentClassifier
import com.planwell.app.domain.extract.DateTimeRanker
import com.planwell.app.domain.extract.EntityType
import com.planwell.app.domain.extract.ExtractedEntity
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EntryBuilder @Inject constructor(
    private val titleExtractor: TitleExtractor,
    private val classifier: IntentClassifier,
) {
    fun build(
        text: String,
        entities: List<ExtractedEntity>,
        sourceImagePath: String? = null,
        referenceTime: Long = System.currentTimeMillis(),
    ): ImportedEntry {
        val classification = classifier.classify(text, entities)
        val ranked = DateTimeRanker.rankDateTimes(entities, referenceTime, text)
        val timeWasExplicit = ranked?.let {
            it.normalised == "time_explicit" || DateTimeRanker.hasExplicitTime(it.rawText)
        } ?: false

        return ImportedEntry(
            kind = classification.kind,
            title = titleExtractor.extract(text),
            notes = text.trim(),
            dueOrStartMs = ranked?.resolvedTimestampMs,
            timeWasExplicit = timeWasExplicit,
            phone = entities.firstOrNull { it.type == EntityType.PHONE }?.normalised
                ?: entities.firstOrNull { it.type == EntityType.PHONE }?.rawText,
            address = entities.firstOrNull { it.type == EntityType.ADDRESS }?.rawText,
            url = entities.firstOrNull { it.type == EntityType.URL }?.normalised
                ?: entities.firstOrNull { it.type == EntityType.URL }?.rawText,
            money = entities.firstOrNull { it.type == EntityType.MONEY }?.normalised
                ?: entities.firstOrNull { it.type == EntityType.MONEY }?.rawText,
            sourceImagePath = sourceImagePath,
            confidence = classification.confidence,
        )
    }
}
