package com.planwell.app.domain.extract

data class ExtractedEntity(
    val type: EntityType,
    val rawText: String,
    val start: Int,
    val end: Int,
    val resolvedTimestampMs: Long? = null,
    val normalised: String? = null,
)
