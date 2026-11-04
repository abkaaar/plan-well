package com.planwell.app.domain.model

data class ScheduledCall(
    val id: Long = 0,
    val contactId: String? = null,
    val displayName: String,
    val phoneNumber: String,
    val scheduledAtMs: Long,
    val notes: String? = null,
    val isCompleted: Boolean = false,
    val createdAtMs: Long = 0,
    val updatedAtMs: Long = 0,
)
