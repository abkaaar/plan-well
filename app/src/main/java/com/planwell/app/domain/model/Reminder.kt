package com.planwell.app.domain.model

enum class RepeatType {
    NONE,
    DAILY,
    WEEKLY,
}

data class Reminder(
    val id: Long = 0,
    val taskId: Long,
    val triggerAtMs: Long,
    val repeatType: RepeatType = RepeatType.NONE,
    val repeatIntervalMs: Long? = null,
    val isActive: Boolean = true,
)
