package com.planwell.app.data.mapper

import com.planwell.app.data.local.db.entity.OpportunityAttachmentEntity
import com.planwell.app.data.local.db.entity.OpportunityCandidateEntity
import com.planwell.app.data.local.db.entity.OpportunityDateEntity
import com.planwell.app.data.local.db.entity.OpportunityEntity
import com.planwell.app.domain.model.AttachmentKind
import com.planwell.app.domain.model.CaptureMethod
import com.planwell.app.domain.model.ExtractionCandidateKind
import com.planwell.app.domain.model.ExtractionStatus
import com.planwell.app.domain.model.Opportunity
import com.planwell.app.domain.model.OpportunityAttachment
import com.planwell.app.domain.model.OpportunityCandidate
import com.planwell.app.domain.model.OpportunityDate
import com.planwell.app.domain.model.OpportunityDateType
import com.planwell.app.domain.model.OpportunityStatus
import com.planwell.app.domain.model.OpportunityType
import com.planwell.app.domain.model.SourcePlatform

fun OpportunityEntity.toDomain(
    dates: List<OpportunityDate> = emptyList(),
    attachments: List<OpportunityAttachment> = emptyList(),
    candidates: List<OpportunityCandidate> = emptyList(),
    linkedTaskIds: List<Long> = emptyList(),
): Opportunity =
    Opportunity(
        id = id,
        title = title,
        description = description,
        type = OpportunityType.entries.firstOrNull { it.name == type } ?: OpportunityType.OTHER,
        categoryId = categoryId,
        status = OpportunityStatus.entries.firstOrNull { it.name == status }
            ?: OpportunityStatus.NEEDS_REVIEW,
        sourceUrl = sourceUrl,
        sourcePlatform = SourcePlatform.entries.firstOrNull { it.name == sourcePlatform }
            ?: SourcePlatform.UNKNOWN,
        captureMethod = CaptureMethod.entries.firstOrNull { it.name == captureMethod }
            ?: CaptureMethod.UNKNOWN,
        isDeleted = isDeleted,
        createdAtMs = createdAtMs,
        updatedAtMs = updatedAtMs,
        extractionStatus = ExtractionStatus.entries.firstOrNull { it.name == extractionStatus }
            ?: ExtractionStatus.NONE,
        extractionError = extractionError,
        extractedRawText = extractedRawText,
        lastExtractionAtMs = lastExtractionAtMs,
        extractionEngine = extractionEngine,
        dates = dates,
        attachments = attachments,
        candidates = candidates,
        linkedTaskIds = linkedTaskIds,
    )

fun Opportunity.toEntity(): OpportunityEntity =
    OpportunityEntity(
        id = id,
        title = title,
        description = description,
        type = type.name,
        categoryId = categoryId,
        status = status.name,
        sourceUrl = sourceUrl,
        sourcePlatform = sourcePlatform.name,
        captureMethod = captureMethod.name,
        isDeleted = isDeleted,
        createdAtMs = createdAtMs,
        updatedAtMs = updatedAtMs,
        extractionStatus = extractionStatus.name,
        extractionError = extractionError,
        extractedRawText = extractedRawText,
        lastExtractionAtMs = lastExtractionAtMs,
        extractionEngine = extractionEngine,
    )

fun OpportunityDateEntity.toDomain(): OpportunityDate =
    OpportunityDate(
        id = id,
        opportunityId = opportunityId,
        type = OpportunityDateType.entries.firstOrNull { it.name == type }
            ?: OpportunityDateType.OTHER,
        label = label,
        dateMs = dateMs,
        extractedDateMs = extractedDateMs,
        isUserConfirmed = isUserConfirmed,
    )

fun OpportunityDate.toEntity(): OpportunityDateEntity =
    OpportunityDateEntity(
        id = id,
        opportunityId = opportunityId,
        type = type.name,
        label = label,
        dateMs = dateMs,
        extractedDateMs = extractedDateMs,
        isUserConfirmed = isUserConfirmed,
    )

fun OpportunityAttachmentEntity.toDomain(): OpportunityAttachment =
    OpportunityAttachment(
        id = id,
        opportunityId = opportunityId,
        relativePath = relativePath,
        mimeType = mimeType,
        displayName = displayName,
        kind = AttachmentKind.entries.firstOrNull { it.name == kind } ?: AttachmentKind.FILE,
        createdAtMs = createdAtMs,
    )

fun OpportunityAttachment.toEntity(): OpportunityAttachmentEntity =
    OpportunityAttachmentEntity(
        id = id,
        opportunityId = opportunityId,
        relativePath = relativePath,
        mimeType = mimeType,
        displayName = displayName,
        kind = kind.name,
        createdAtMs = createdAtMs,
    )

fun OpportunityCandidateEntity.toDomain(): OpportunityCandidate =
    OpportunityCandidate(
        id = id,
        opportunityId = opportunityId,
        kind = ExtractionCandidateKind.entries.firstOrNull { it.name == kind }
            ?: ExtractionCandidateKind.TITLE,
        valueText = valueText,
        sourceSnippet = sourceSnippet,
        isRejected = isRejected,
        isApplied = isApplied,
        createdAtMs = createdAtMs,
    )

fun OpportunityCandidate.toEntity(): OpportunityCandidateEntity =
    OpportunityCandidateEntity(
        id = id,
        opportunityId = opportunityId,
        kind = kind.name,
        valueText = valueText,
        sourceSnippet = sourceSnippet,
        isRejected = isRejected,
        isApplied = isApplied,
        createdAtMs = createdAtMs,
    )
