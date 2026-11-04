package com.planwell.app.domain.usecase.opportunity

import android.content.Context
import android.net.Uri
import com.planwell.app.data.attachment.OpportunityAttachmentStore
import com.planwell.app.data.ml.TextExtractor
import com.planwell.app.data.ml.loadBitmap
import com.planwell.app.domain.extraction.OpportunityTextParser
import com.planwell.app.domain.model.AttachmentKind
import com.planwell.app.domain.model.ExtractionCandidateKind
import com.planwell.app.domain.model.ExtractionStatus
import com.planwell.app.domain.model.OpportunityAttachment
import com.planwell.app.domain.model.OpportunityCandidate
import com.planwell.app.domain.model.OpportunityDate
import com.planwell.app.domain.model.OpportunityDateType
import com.planwell.app.domain.repository.OpportunityRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.InputStream
import javax.inject.Inject

data class ExtractionRunResult(
    val status: ExtractionStatus,
    val errorMessage: String? = null,
)

/**
 * Runs on-device OCR + heuristic parsing. Failures set FAILED status but never
 * delete the opportunity or its attachments.
 */
class RunOpportunityExtractionUseCase @Inject constructor(
    private val repository: OpportunityRepository,
    private val attachmentStore: OpportunityAttachmentStore,
    private val textExtractor: TextExtractor,
    @ApplicationContext private val context: Context,
) {
    suspend operator fun invoke(opportunityId: Long): ExtractionRunResult {
        require(opportunityId > 0) { "Invalid opportunity" }
        val opportunity = repository.getOpportunity(opportunityId)
            ?: throw IllegalArgumentException("Opportunity not found")

        val image = opportunity.attachments.firstOrNull {
            it.kind == AttachmentKind.IMAGE ||
                it.mimeType?.startsWith("image/") == true
        }
        if (image == null) {
            repository.setExtractionState(
                opportunityId = opportunityId,
                status = ExtractionStatus.SKIPPED,
                error = "No image attachment to extract from",
                rawText = null,
                engine = TextExtractor.ENGINE_ID,
            )
            return ExtractionRunResult(ExtractionStatus.SKIPPED, "No image attachment")
        }

        repository.setExtractionState(
            opportunityId = opportunityId,
            status = ExtractionStatus.PENDING,
            error = null,
            rawText = null,
            engine = TextExtractor.ENGINE_ID,
        )

        return try {
            val file = attachmentStore.resolve(image.relativePath)
            val bitmap = context.contentResolver.loadBitmap(Uri.fromFile(file))
            if (bitmap == null) {
                repository.setExtractionState(
                    opportunityId = opportunityId,
                    status = ExtractionStatus.FAILED,
                    error = "No text found in image",
                    rawText = "",
                    engine = TextExtractor.ENGINE_ID,
                )
                return ExtractionRunResult(ExtractionStatus.FAILED, "No text found in image")
            }

            val extraction = try {
                textExtractor.extract(bitmap)
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
            val rawText = extraction.fullText
            if (rawText.isBlank()) {
                repository.setExtractionState(
                    opportunityId = opportunityId,
                    status = ExtractionStatus.FAILED,
                    error = "No text found in image",
                    rawText = "",
                    engine = TextExtractor.ENGINE_ID,
                )
                return ExtractionRunResult(ExtractionStatus.FAILED, "No text found in image")
            }

            val parsed = OpportunityTextParser.parse(rawText)
            repository.replacePendingCandidates(
                opportunityId,
                parsed.textCandidates.map {
                    OpportunityCandidate(
                        opportunityId = opportunityId,
                        kind = it.kind,
                        valueText = it.valueText,
                        sourceSnippet = it.sourceSnippet,
                    )
                },
            )

            // Replace only unconfirmed extracted dates of the same provenance run:
            // delete unconfirmed dates that have extractedDateMs/label from prior OCR,
            // then insert new candidates. Confirmed dates stay.
            opportunity.dates
                .filter { !it.isUserConfirmed }
                .forEach { repository.deleteDate(it.id) }

            parsed.dateCandidates.forEach { date ->
                repository.upsertDate(
                    OpportunityDate(
                        opportunityId = opportunityId,
                        type = date.type,
                        label = date.label,
                        dateMs = null,
                        extractedDateMs = date.extractedDateMs,
                        isUserConfirmed = false,
                    ),
                )
            }

            repository.setExtractionState(
                opportunityId = opportunityId,
                status = ExtractionStatus.SUCCEEDED,
                error = null,
                rawText = rawText,
                engine = TextExtractor.ENGINE_ID,
            )
            ExtractionRunResult(ExtractionStatus.SUCCEEDED)
        } catch (e: Exception) {
            repository.setExtractionState(
                opportunityId = opportunityId,
                status = ExtractionStatus.FAILED,
                error = (e.message ?: "Extraction failed").take(200),
                rawText = null,
                engine = TextExtractor.ENGINE_ID,
            )
            ExtractionRunResult(
                ExtractionStatus.FAILED,
                e.message ?: "Extraction failed",
            )
        }
    }
}

data class ExtractionReviewDecision(
    val candidateId: Long,
    val accept: Boolean,
    /** Edited value when accepting text candidates. */
    val editedValue: String? = null,
)

data class DateReviewDecision(
    val dateId: Long,
    val accept: Boolean,
    /** Confirmed / corrected epoch millis when accepting. */
    val confirmedDateMs: Long? = null,
)

class ApplyExtractionReviewUseCase @Inject constructor(
    private val repository: OpportunityRepository,
) {
    suspend operator fun invoke(
        opportunityId: Long,
        textDecisions: List<ExtractionReviewDecision>,
        dateDecisions: List<DateReviewDecision>,
    ) {
        require(opportunityId > 0) { "Invalid opportunity" }
        val opportunity = repository.getOpportunity(opportunityId)
            ?: throw IllegalArgumentException("Opportunity not found")

        var title = opportunity.title
        var description = opportunity.description
        var sourceUrl = opportunity.sourceUrl

        for (decision in textDecisions) {
            val candidate = repository.getCandidate(decision.candidateId) ?: continue
            if (candidate.opportunityId != opportunityId) continue
            if (!decision.accept) {
                repository.updateCandidate(candidate.copy(isRejected = true, isApplied = false))
                continue
            }
            val value = decision.editedValue?.trim()?.takeIf { it.isNotEmpty() }
                ?: candidate.valueText.trim()
            when (candidate.kind) {
                ExtractionCandidateKind.TITLE -> title = value
                ExtractionCandidateKind.URL -> sourceUrl = value
                ExtractionCandidateKind.ORGANIZATION -> {
                    description = mergeNote(description, "Organization: $value")
                }
                ExtractionCandidateKind.ELIGIBILITY -> {
                    description = mergeNote(description, "Eligibility: $value")
                }
            }
            repository.updateCandidate(
                candidate.copy(
                    valueText = value,
                    isApplied = true,
                    isRejected = false,
                ),
            )
        }

        for (decision in dateDecisions) {
            val date = opportunity.dates.firstOrNull { it.id == decision.dateId } ?: continue
            if (!decision.accept) {
                repository.deleteDate(date.id)
                continue
            }
            val ms = decision.confirmedDateMs
                ?: date.extractedDateMs
                ?: continue // cannot confirm without a concrete date
            // Ambiguous dates with null extractedDateMs require user-picked confirmedDateMs
            repository.confirmDate(date.id, ms)
            if (date.type == OpportunityDateType.OTHER && decision.confirmedDateMs != null) {
                repository.upsertDate(
                    date.copy(
                        dateMs = ms,
                        isUserConfirmed = true,
                    ),
                )
            }
        }

        repository.updateOpportunity(
            opportunity.copy(
                title = title,
                description = description,
                sourceUrl = sourceUrl,
            ),
        )
    }

    private fun mergeNote(existing: String?, addition: String): String {
        val base = existing?.trim().orEmpty()
        return if (base.isEmpty()) addition else "$base\n\n$addition"
    }
}

class AttachOpportunityImageUseCase @Inject constructor(
    private val repository: OpportunityRepository,
    private val attachmentStore: OpportunityAttachmentStore,
) {
    suspend operator fun invoke(
        opportunityId: Long,
        displayName: String?,
        mimeType: String?,
        input: InputStream,
    ): Long {
        require(opportunityId > 0) { "Invalid opportunity" }
        repository.getOpportunity(opportunityId)
            ?: throw IllegalArgumentException("Opportunity not found")
        val relative = attachmentStore.save(opportunityId, displayName, input)
        return repository.addAttachmentMetadata(
            OpportunityAttachment(
                opportunityId = opportunityId,
                relativePath = relative,
                mimeType = mimeType ?: "image/*",
                displayName = displayName,
                kind = AttachmentKind.IMAGE,
            ),
        )
    }
}
