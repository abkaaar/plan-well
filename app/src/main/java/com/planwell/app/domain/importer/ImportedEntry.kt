package com.planwell.app.domain.importer

import com.planwell.app.domain.classify.EntryKind

data class ImportedEntry(
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
)
