package com.planwell.app.domain.model

data class Category(
    val id: Long = 0,
    val name: String,
    val colorHex: String = "#2563EB",
    val taskCount: Int = 0,
)

data class CompletionStats(
    val todayTotal: Int,
    val todayCompleted: Int,
) {
    val progress: Float
        get() = if (todayTotal == 0) 0f else todayCompleted.toFloat() / todayTotal.toFloat()
}

/** Preset colours for category create/edit (SRS FR-03.4 — ≥10). */
object CategoryColorPresets {
    val hexValues: List<String> = listOf(
        "#2563EB",
        "#10B981",
        "#F59E0B",
        "#EF4444",
        "#8B5CF6",
        "#EC4899",
        "#06B6D4",
        "#84CC16",
        "#F97316",
        "#64748B",
    ).map { it.uppercase() }
}
