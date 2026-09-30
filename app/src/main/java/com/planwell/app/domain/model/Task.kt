package com.planwell.app.domain.model

enum class Priority(val value: Int) {
    NONE(0),
    LOW(1),
    MEDIUM(2),
    HIGH(3);

    companion object {
        fun fromInt(value: Int): Priority =
            entries.firstOrNull { it.value == value } ?: NONE
    }
}

data class Task(
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val dueDateMs: Long? = null,
    val priority: Priority = Priority.NONE,
    val categoryId: Long? = null,
    val isCompleted: Boolean = false,
    val completedAtMs: Long? = null,
    val isDeleted: Boolean = false,
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis(),
)
