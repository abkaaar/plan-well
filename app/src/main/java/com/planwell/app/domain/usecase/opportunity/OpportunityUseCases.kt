package com.planwell.app.domain.usecase.opportunity

import com.planwell.app.domain.model.CaptureMethod
import com.planwell.app.domain.model.Opportunity
import com.planwell.app.domain.model.OpportunityAttachment
import com.planwell.app.domain.model.OpportunityDate
import com.planwell.app.domain.model.OpportunityStatus
import com.planwell.app.domain.model.OpportunityType
import com.planwell.app.domain.model.SourcePlatform
import com.planwell.app.domain.repository.OpportunityRepository
import javax.inject.Inject

private const val TITLE_MAX = 200
private const val DESCRIPTION_MAX = 2_000
private const val URL_MAX = 2_000

internal fun validateOpportunityFields(
    title: String,
    description: String?,
    sourceUrl: String?,
) {
    val trimmedTitle = title.trim()
    require(trimmedTitle.isNotBlank()) { "Title is required" }
    require(trimmedTitle.length <= TITLE_MAX) { "Title max $TITLE_MAX characters" }
    description?.let {
        require(it.length <= DESCRIPTION_MAX) { "Description max $DESCRIPTION_MAX characters" }
    }
    sourceUrl?.let {
        require(it.length <= URL_MAX) { "Source URL max $URL_MAX characters" }
    }
}

class CreateOpportunityUseCase @Inject constructor(
    private val repository: OpportunityRepository,
) {
    suspend operator fun invoke(opportunity: Opportunity): Long {
        validateOpportunityFields(opportunity.title, opportunity.description, opportunity.sourceUrl)
        val now = System.currentTimeMillis()
        return repository.createOpportunity(
            opportunity.copy(
                title = opportunity.title.trim(),
                description = opportunity.description?.trim()?.ifBlank { null },
                sourceUrl = opportunity.sourceUrl?.trim()?.ifBlank { null },
                createdAtMs = now,
                updatedAtMs = now,
            ),
        )
    }
}

class UpdateOpportunityUseCase @Inject constructor(
    private val repository: OpportunityRepository,
) {
    suspend operator fun invoke(opportunity: Opportunity) {
        require(opportunity.id > 0) { "Invalid opportunity" }
        validateOpportunityFields(opportunity.title, opportunity.description, opportunity.sourceUrl)
        repository.updateOpportunity(
            opportunity.copy(
                title = opportunity.title.trim(),
                description = opportunity.description?.trim()?.ifBlank { null },
                sourceUrl = opportunity.sourceUrl?.trim()?.ifBlank { null },
            ),
        )
    }
}

class GetOpportunityUseCase @Inject constructor(
    private val repository: OpportunityRepository,
) {
    suspend operator fun invoke(id: Long): Opportunity? = repository.getOpportunity(id)
}

class ObserveOpportunityUseCase @Inject constructor(
    private val repository: OpportunityRepository,
) {
    operator fun invoke(id: Long) = repository.observeOpportunity(id)
}

class ObserveInboxOpportunitiesUseCase @Inject constructor(
    private val repository: OpportunityRepository,
) {
    operator fun invoke() = repository.observeInbox()
}

class ObserveArchivedOpportunitiesUseCase @Inject constructor(
    private val repository: OpportunityRepository,
) {
    operator fun invoke() = repository.observeArchived()
}

class ObserveDeletedOpportunitiesUseCase @Inject constructor(
    private val repository: OpportunityRepository,
) {
    operator fun invoke() = repository.observeDeleted()
}

class SoftDeleteOpportunityUseCase @Inject constructor(
    private val repository: OpportunityRepository,
) {
    suspend operator fun invoke(id: Long) {
        require(id > 0) { "Invalid opportunity" }
        repository.softDelete(id)
    }
}

class RestoreOpportunityUseCase @Inject constructor(
    private val repository: OpportunityRepository,
) {
    suspend operator fun invoke(id: Long) {
        require(id > 0) { "Invalid opportunity" }
        repository.restore(id)
    }
}

class ArchiveOpportunityUseCase @Inject constructor(
    private val repository: OpportunityRepository,
) {
    suspend operator fun invoke(id: Long) {
        require(id > 0) { "Invalid opportunity" }
        repository.archive(id)
    }
}

class UnarchiveOpportunityUseCase @Inject constructor(
    private val repository: OpportunityRepository,
) {
    suspend operator fun invoke(id: Long) {
        require(id > 0) { "Invalid opportunity" }
        repository.unarchive(id)
    }
}

class SetOpportunityStatusUseCase @Inject constructor(
    private val repository: OpportunityRepository,
) {
    suspend operator fun invoke(id: Long, status: OpportunityStatus) {
        require(id > 0) { "Invalid opportunity" }
        repository.setStatus(id, status)
    }
}

class PermanentlyDeleteOpportunityUseCase @Inject constructor(
    private val repository: OpportunityRepository,
) {
    suspend operator fun invoke(id: Long) {
        require(id > 0) { "Invalid opportunity" }
        repository.permanentlyDelete(id)
    }
}

class UpsertOpportunityDateUseCase @Inject constructor(
    private val repository: OpportunityRepository,
) {
    suspend operator fun invoke(date: OpportunityDate): Long {
        require(date.opportunityId > 0) { "Opportunity id required" }
        if (date.isUserConfirmed) {
            require(date.dateMs != null) { "Confirmed date requires dateMs" }
        }
        return repository.upsertDate(date)
    }
}

class ConfirmOpportunityDateUseCase @Inject constructor(
    private val repository: OpportunityRepository,
) {
    suspend operator fun invoke(dateId: Long, confirmedDateMs: Long) {
        require(dateId > 0) { "Invalid date" }
        repository.confirmDate(dateId, confirmedDateMs)
    }
}

class DeleteOpportunityDateUseCase @Inject constructor(
    private val repository: OpportunityRepository,
) {
    suspend operator fun invoke(dateId: Long) {
        require(dateId > 0) { "Invalid date" }
        repository.deleteDate(dateId)
    }
}

class LinkOpportunityTaskUseCase @Inject constructor(
    private val repository: OpportunityRepository,
) {
    suspend operator fun invoke(opportunityId: Long, taskId: Long) {
        require(opportunityId > 0) { "Invalid opportunity" }
        require(taskId > 0) { "Invalid task" }
        repository.linkTask(opportunityId, taskId)
    }
}

class UnlinkOpportunityTaskUseCase @Inject constructor(
    private val repository: OpportunityRepository,
) {
    suspend operator fun invoke(opportunityId: Long, taskId: Long) {
        require(opportunityId > 0) { "Invalid opportunity" }
        require(taskId > 0) { "Invalid task" }
        repository.unlinkTask(opportunityId, taskId)
    }
}

class AddOpportunityAttachmentUseCase @Inject constructor(
    private val repository: OpportunityRepository,
) {
    suspend operator fun invoke(attachment: OpportunityAttachment): Long {
        require(attachment.opportunityId > 0) { "Opportunity id required" }
        require(attachment.relativePath.isNotBlank()) { "Attachment path required" }
        return repository.addAttachmentMetadata(attachment)
    }
}

/** Convenience factory for share/manual capture defaults. */
fun newCapturedOpportunity(
    title: String,
    description: String? = null,
    type: OpportunityType = OpportunityType.OPPORTUNITY,
    sourceUrl: String? = null,
    sourcePlatform: SourcePlatform = SourcePlatform.UNKNOWN,
    captureMethod: CaptureMethod = CaptureMethod.MANUAL,
): Opportunity =
    Opportunity(
        title = title,
        description = description,
        type = type,
        sourceUrl = sourceUrl,
        sourcePlatform = sourcePlatform,
        captureMethod = captureMethod,
        status = OpportunityStatus.NEEDS_REVIEW,
    )
