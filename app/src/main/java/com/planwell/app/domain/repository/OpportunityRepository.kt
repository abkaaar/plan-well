package com.planwell.app.domain.repository

import com.planwell.app.domain.model.ExtractionStatus
import com.planwell.app.domain.model.Opportunity
import com.planwell.app.domain.model.OpportunityAttachment
import com.planwell.app.domain.model.OpportunityCandidate
import com.planwell.app.domain.model.OpportunityDate
import com.planwell.app.domain.model.OpportunityStatus
import kotlinx.coroutines.flow.Flow

interface OpportunityRepository {
    fun observeInbox(): Flow<List<Opportunity>>
    fun observeArchived(): Flow<List<Opportunity>>
    fun observeDeleted(): Flow<List<Opportunity>>
    fun observeOpportunity(id: Long): Flow<Opportunity?>
    suspend fun getOpportunity(id: Long): Opportunity?

    suspend fun createOpportunity(opportunity: Opportunity): Long
    suspend fun updateOpportunity(opportunity: Opportunity)

    suspend fun softDelete(id: Long)
    suspend fun restore(id: Long)
    suspend fun archive(id: Long)
    suspend fun unarchive(id: Long)
    suspend fun setStatus(id: Long, status: OpportunityStatus)
    suspend fun permanentlyDelete(id: Long)

    suspend fun upsertDate(date: OpportunityDate): Long
    suspend fun confirmDate(dateId: Long, confirmedDateMs: Long)
    suspend fun deleteDate(dateId: Long)

    suspend fun addAttachmentMetadata(attachment: OpportunityAttachment): Long
    suspend fun removeAttachment(attachmentId: Long)

    suspend fun replacePendingCandidates(opportunityId: Long, candidates: List<OpportunityCandidate>)
    suspend fun updateCandidate(candidate: OpportunityCandidate)
    suspend fun getCandidate(id: Long): OpportunityCandidate?

    suspend fun setExtractionState(
        opportunityId: Long,
        status: ExtractionStatus,
        error: String? = null,
        rawText: String? = null,
        engine: String? = null,
        atMs: Long = System.currentTimeMillis(),
    )

    suspend fun linkTask(opportunityId: Long, taskId: Long)
    suspend fun unlinkTask(opportunityId: Long, taskId: Long)
    fun observeLinkedTaskIds(opportunityId: Long): Flow<List<Long>>
    fun observeOpportunityIdsForTask(taskId: Long): Flow<List<Long>>
}
