package com.planwell.app.ui.share

import com.planwell.app.domain.classify.EntryKind
import com.planwell.app.domain.importer.ImportedEntry

sealed class ShareImportState {
    data class Loading(val stage: String) : ShareImportState()
    data class Review(val draft: EditableImportDraft) : ShareImportState()
    data object Saving : ShareImportState()
    data class Saved(val taskId: Long) : ShareImportState()
    data class Error(val message: String, val retryable: Boolean) : ShareImportState()
}

data class EditableImportDraft(
    val kind: EntryKind,
    val title: String,
    val notes: String,
    val dueOrStartMs: Long?,
    val timeWasExplicit: Boolean,
    val phone: String?,
    val address: String?,
    val url: String?,
    val money: String?,
    val sourceImagePath: String?,
    val confidence: Float,
) {
    fun toImportedEntry(): ImportedEntry = ImportedEntry(
        kind = kind,
        title = title,
        notes = notes,
        dueOrStartMs = dueOrStartMs,
        timeWasExplicit = timeWasExplicit,
        phone = phone,
        address = address,
        url = url,
        money = money,
        sourceImagePath = sourceImagePath,
        confidence = confidence,
    )

    companion object {
        fun from(entry: ImportedEntry): EditableImportDraft = EditableImportDraft(
            kind = entry.kind,
            title = entry.title,
            notes = entry.notes,
            dueOrStartMs = entry.dueOrStartMs,
            timeWasExplicit = entry.timeWasExplicit,
            phone = entry.phone,
            address = entry.address,
            url = entry.url,
            money = entry.money,
            sourceImagePath = entry.sourceImagePath,
            confidence = entry.confidence,
        )
    }
}
