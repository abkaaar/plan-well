package com.planwell.app.domain.usecase.opportunity

import com.planwell.app.data.attachment.OpportunityAttachmentStore
import com.planwell.app.domain.capture.ParsedSharedContent
import com.planwell.app.domain.capture.SharedContentParser
import com.planwell.app.domain.model.AttachmentKind
import com.planwell.app.domain.model.CaptureMethod
import com.planwell.app.domain.model.OpportunityAttachment
import com.planwell.app.domain.model.OpportunityStatus
import com.planwell.app.domain.model.OpportunityType
import com.planwell.app.domain.model.SourcePlatform
import com.planwell.app.domain.repository.OpportunityRepository
import java.io.InputStream
import javax.inject.Inject

data class CaptureResult(
    val opportunityId: Long,
    val hasImage: Boolean,
    val parsed: ParsedSharedContent,
)

/**
 * Creates an inbox opportunity from share / paste / image capture with inferred metadata.
 */
class CaptureSharedContentUseCase @Inject constructor(
    private val repository: OpportunityRepository,
    private val attachmentStore: OpportunityAttachmentStore,
) {
    suspend fun fromText(
        subject: String?,
        text: String?,
        captureMethod: CaptureMethod = CaptureMethod.SHARE,
    ): CaptureResult {
        val parsed = SharedContentParser.parse(subject, text)
        val id = createOpportunity(parsed, captureMethod)
        return CaptureResult(opportunityId = id, hasImage = false, parsed = parsed)
    }

    suspend fun fromImage(
        displayName: String?,
        mimeType: String?,
        input: InputStream,
        subject: String? = null,
        text: String? = null,
        captureMethod: CaptureMethod = CaptureMethod.SHARE,
    ): CaptureResult {
        val parsed = SharedContentParser.parse(
            subject = subject,
            text = text,
            fallbackTitle = displayName
                ?.substringBeforeLast('.')
                ?.takeIf { it.isNotBlank() }
                ?: "Shared image",
        )
        val id = createOpportunity(parsed, captureMethod)
        val relative = attachmentStore.save(id, displayName, input)
        repository.addAttachmentMetadata(
            OpportunityAttachment(
                opportunityId = id,
                relativePath = relative,
                mimeType = mimeType ?: "image/*",
                displayName = displayName,
                kind = AttachmentKind.IMAGE,
            ),
        )
        return CaptureResult(opportunityId = id, hasImage = true, parsed = parsed)
    }

    suspend fun fromPaste(raw: String): CaptureResult =
        fromText(subject = null, text = raw, captureMethod = CaptureMethod.PASTE)

    private suspend fun createOpportunity(
        parsed: ParsedSharedContent,
        captureMethod: CaptureMethod,
    ): Long {
        val now = System.currentTimeMillis()
        return repository.createOpportunity(
            newCapturedOpportunity(
                title = parsed.title,
                description = parsed.description,
                type = OpportunityType.OPPORTUNITY,
                sourceUrl = parsed.sourceUrl,
                sourcePlatform = parsed.sourcePlatform,
                captureMethod = captureMethod,
            ).copy(
                status = OpportunityStatus.NEEDS_REVIEW,
                createdAtMs = now,
                updatedAtMs = now,
            ),
        )
    }
}

object SourcePlatformInference {
    fun fromUrl(url: String?): SourcePlatform = SharedContentParser.inferSourcePlatform(url)
}
