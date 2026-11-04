package com.planwell.app.domain.model

/**
 * Captured opportunity / saved item — distinct from [Task].
 *
 * Trash vs archive:
 * - Soft-delete (`isDeleted`) moves the item to Trash; [restore] clears the flag.
 * - [OpportunityStatus.ARCHIVED] hides it from the active inbox without trash.
 * - Permanent delete removes the row, cascading dates/attachments/task links,
 *   and deletes attachment files from app-private storage.
 *
 * Tasks remain independent: links are optional many-to-many.
 *
 * Relationship lifecycle:
 * - Soft-delete / archive / restore opportunity: does not move or delete tasks.
 * - Permanent delete opportunity: CASCADE removes link rows only; tasks remain.
 * - Soft-delete task: link remains; permanent delete task removes that link only.
 * - Never schedule reminders from unconfirmed extracted dates.
 */
enum class OpportunityStatus {
    NEEDS_REVIEW,
    ACTIVE,
    COMPLETED,
    ARCHIVED,
}

enum class OpportunityType {
    EVENT,
    DEADLINE,
    ANNOUNCEMENT,
    OPPORTUNITY,
    OTHER,
}

enum class SourcePlatform {
    INSTAGRAM,
    BROWSER,
    OTHER,
    UNKNOWN,
}

enum class CaptureMethod {
    SHARE,
    MANUAL,
    PASTE,
    UNKNOWN,
}

enum class OpportunityDateType {
    APPLICATION_DEADLINE,
    EVENT,
    OTHER,
}

enum class AttachmentKind {
    IMAGE,
    FILE,
    TEXT,
}

enum class ExtractionStatus {
    NONE,
    PENDING,
    SUCCEEDED,
    FAILED,
    SKIPPED,
}

enum class ExtractionCandidateKind {
    TITLE,
    ORGANIZATION,
    URL,
    ELIGIBILITY,
}

data class OpportunityDate(
    val id: Long = 0,
    val opportunityId: Long = 0,
    val type: OpportunityDateType,
    val label: String? = null,
    /** User-facing date once confirmed (or manually set). */
    val dateMs: Long? = null,
    /** Heuristic / OCR suggestion; not authoritative until [isUserConfirmed]. */
    val extractedDateMs: Long? = null,
    val isUserConfirmed: Boolean = false,
)

data class OpportunityCandidate(
    val id: Long = 0,
    val opportunityId: Long = 0,
    val kind: ExtractionCandidateKind,
    val valueText: String,
    val sourceSnippet: String? = null,
    val isRejected: Boolean = false,
    val isApplied: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis(),
)

data class OpportunityAttachment(
    val id: Long = 0,
    val opportunityId: Long = 0,
    /** Relative path under the app opportunities files directory. */
    val relativePath: String,
    val mimeType: String? = null,
    val displayName: String? = null,
    val kind: AttachmentKind = AttachmentKind.FILE,
    val createdAtMs: Long = System.currentTimeMillis(),
)

data class Opportunity(
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val type: OpportunityType = OpportunityType.OPPORTUNITY,
    val categoryId: Long? = null,
    val status: OpportunityStatus = OpportunityStatus.NEEDS_REVIEW,
    val sourceUrl: String? = null,
    val sourcePlatform: SourcePlatform = SourcePlatform.UNKNOWN,
    val captureMethod: CaptureMethod = CaptureMethod.UNKNOWN,
    val isDeleted: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis(),
    val extractionStatus: ExtractionStatus = ExtractionStatus.NONE,
    val extractionError: String? = null,
    val extractedRawText: String? = null,
    val lastExtractionAtMs: Long? = null,
    val extractionEngine: String? = null,
    val dates: List<OpportunityDate> = emptyList(),
    val attachments: List<OpportunityAttachment> = emptyList(),
    val candidates: List<OpportunityCandidate> = emptyList(),
    /** Task ids linked to this opportunity (ordinary tasks stay independent). */
    val linkedTaskIds: List<Long> = emptyList(),
)
