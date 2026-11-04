package com.planwell.app.data.repository

import com.planwell.app.data.attachment.OpportunityAttachmentStore
import com.planwell.app.data.local.dao.OpportunityAttachmentDao
import com.planwell.app.data.local.dao.OpportunityCandidateDao
import com.planwell.app.data.local.dao.OpportunityDao
import com.planwell.app.data.local.dao.OpportunityDateDao
import com.planwell.app.data.local.dao.OpportunityTaskLinkDao
import com.planwell.app.data.local.db.entity.OpportunityTaskLinkEntity
import com.planwell.app.data.mapper.toDomain
import com.planwell.app.data.mapper.toEntity
import com.planwell.app.domain.model.ExtractionStatus
import com.planwell.app.domain.model.Opportunity
import com.planwell.app.domain.model.OpportunityAttachment
import com.planwell.app.domain.model.OpportunityCandidate
import com.planwell.app.domain.model.OpportunityDate
import com.planwell.app.domain.model.OpportunityStatus
import com.planwell.app.domain.repository.OpportunityRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.mapLatest
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class OpportunityRepositoryImpl @Inject constructor(
    private val opportunityDao: OpportunityDao,
    private val dateDao: OpportunityDateDao,
    private val attachmentDao: OpportunityAttachmentDao,
    private val candidateDao: OpportunityCandidateDao,
    private val linkDao: OpportunityTaskLinkDao,
    private val attachmentStore: OpportunityAttachmentStore,
) : OpportunityRepository {

    override fun observeInbox(): Flow<List<Opportunity>> =
        opportunityDao.observeInbox().mapLatest { list -> list.mapNotNull { hydrate(it.id) } }

    override fun observeArchived(): Flow<List<Opportunity>> =
        opportunityDao.observeArchived().mapLatest { list -> list.mapNotNull { hydrate(it.id) } }

    override fun observeDeleted(): Flow<List<Opportunity>> =
        opportunityDao.observeDeleted().mapLatest { list -> list.mapNotNull { hydrate(it.id) } }

    override fun observeOpportunity(id: Long): Flow<Opportunity?> =
        combine(
            opportunityDao.observeById(id),
            dateDao.observeForOpportunity(id),
            attachmentDao.observeForOpportunity(id),
            candidateDao.observeForOpportunity(id),
            linkDao.observeTaskIds(id),
        ) { entity, dates, attachments, candidates, taskIds ->
            entity?.toDomain(
                dates = dates.map { it.toDomain() },
                attachments = attachments.map { it.toDomain() },
                candidates = candidates.map { it.toDomain() },
                linkedTaskIds = taskIds,
            )
        }

    override suspend fun getOpportunity(id: Long): Opportunity? = hydrate(id)

    override suspend fun createOpportunity(opportunity: Opportunity): Long {
        val id = opportunityDao.insert(opportunity.copy(id = 0).toEntity())
        opportunity.dates.forEach { date ->
            dateDao.insert(date.copy(id = 0, opportunityId = id).toEntity())
        }
        opportunity.attachments.forEach { attachment ->
            attachmentDao.insert(attachment.copy(id = 0, opportunityId = id).toEntity())
        }
        opportunity.candidates.forEach { candidate ->
            candidateDao.insert(candidate.copy(id = 0, opportunityId = id).toEntity())
        }
        opportunity.linkedTaskIds.forEach { taskId ->
            linkDao.insert(OpportunityTaskLinkEntity(opportunityId = id, taskId = taskId))
        }
        return id
    }

    override suspend fun updateOpportunity(opportunity: Opportunity) {
        opportunityDao.update(
            opportunity.copy(updatedAtMs = System.currentTimeMillis()).toEntity(),
        )
    }

    override suspend fun softDelete(id: Long) {
        opportunityDao.softDelete(id)
    }

    override suspend fun restore(id: Long) {
        opportunityDao.restore(id)
    }

    override suspend fun archive(id: Long) {
        opportunityDao.updateStatus(id, OpportunityStatus.ARCHIVED.name)
    }

    override suspend fun unarchive(id: Long) {
        opportunityDao.updateStatus(id, OpportunityStatus.ACTIVE.name)
    }

    override suspend fun setStatus(id: Long, status: OpportunityStatus) {
        opportunityDao.updateStatus(id, status.name)
    }

    override suspend fun permanentlyDelete(id: Long) {
        attachmentStore.deleteAllForOpportunity(id)
        opportunityDao.permanentlyDelete(id)
    }

    override suspend fun upsertDate(date: OpportunityDate): Long {
        return if (date.id == 0L) {
            dateDao.insert(date.toEntity())
        } else {
            dateDao.update(date.toEntity())
            date.id
        }
    }

    override suspend fun confirmDate(dateId: Long, confirmedDateMs: Long) {
        val existing = dateDao.getById(dateId) ?: return
        dateDao.update(
            existing.copy(
                dateMs = confirmedDateMs,
                isUserConfirmed = true,
            ),
        )
        touchOpportunity(existing.opportunityId)
    }

    override suspend fun deleteDate(dateId: Long) {
        dateDao.deleteById(dateId)
    }

    override suspend fun addAttachmentMetadata(attachment: OpportunityAttachment): Long =
        attachmentDao.insert(attachment.copy(id = 0).toEntity())

    override suspend fun removeAttachment(attachmentId: Long) {
        val existing = attachmentDao.getById(attachmentId) ?: return
        attachmentStore.delete(existing.relativePath)
        attachmentDao.deleteById(attachmentId)
    }

    override suspend fun replacePendingCandidates(
        opportunityId: Long,
        candidates: List<OpportunityCandidate>,
    ) {
        candidateDao.deletePendingForOpportunity(opportunityId)
        if (candidates.isNotEmpty()) {
            candidateDao.insertAll(
                candidates.map { it.copy(id = 0, opportunityId = opportunityId).toEntity() },
            )
        }
    }

    override suspend fun updateCandidate(candidate: OpportunityCandidate) {
        candidateDao.update(candidate.toEntity())
    }

    override suspend fun getCandidate(id: Long): OpportunityCandidate? =
        candidateDao.getById(id)?.toDomain()

    override suspend fun setExtractionState(
        opportunityId: Long,
        status: ExtractionStatus,
        error: String?,
        rawText: String?,
        engine: String?,
        atMs: Long,
    ) {
        val existing = opportunityDao.getById(opportunityId) ?: return
        opportunityDao.update(
            existing.copy(
                extractionStatus = status.name,
                extractionError = error,
                extractedRawText = rawText ?: existing.extractedRawText,
                lastExtractionAtMs = atMs,
                extractionEngine = engine ?: existing.extractionEngine,
                updatedAtMs = atMs,
            ),
        )
    }

    override suspend fun linkTask(opportunityId: Long, taskId: Long) {
        linkDao.insert(OpportunityTaskLinkEntity(opportunityId, taskId))
        touchOpportunity(opportunityId)
    }

    override suspend fun unlinkTask(opportunityId: Long, taskId: Long) {
        linkDao.delete(opportunityId, taskId)
        touchOpportunity(opportunityId)
    }

    override fun observeLinkedTaskIds(opportunityId: Long): Flow<List<Long>> =
        linkDao.observeTaskIds(opportunityId)

    override fun observeOpportunityIdsForTask(taskId: Long): Flow<List<Long>> =
        linkDao.observeOpportunityIds(taskId)

    private suspend fun hydrate(id: Long): Opportunity? {
        val entity = opportunityDao.getById(id) ?: return null
        val dates = dateDao.getForOpportunity(id).map { it.toDomain() }
        val attachments = attachmentDao.getForOpportunity(id).map { it.toDomain() }
        val candidates = candidateDao.getForOpportunity(id).map { it.toDomain() }
        val taskIds = linkDao.getTaskIds(id)
        return entity.toDomain(dates, attachments, candidates, taskIds)
    }

    private suspend fun touchOpportunity(id: Long) {
        val entity = opportunityDao.getById(id) ?: return
        opportunityDao.update(entity.copy(updatedAtMs = System.currentTimeMillis()))
    }
}
