package com.planwell.app.ui

object Routes {
    const val HOME = "home"
    const val ADD_TASK = "task/add?preset={preset}"
    const val ADD_TASK_BASE = "task/add"
    const val ADD_CALL = "call/add"
    const val EDIT_TASK = "task/edit/{taskId}"
    const val TASK_DETAIL = "task/{taskId}"
    const val TRASH = "trash"
    const val CATEGORY = "category"
    const val SEARCH = "search"
    const val SETTINGS = "settings"
    const val OPPORTUNITY_INBOX = "opportunity/inbox"
    const val ADD_OPPORTUNITY = "opportunity/add"
    const val EDIT_OPPORTUNITY = "opportunity/edit/{opportunityId}"
    const val OPPORTUNITY_DETAIL = "opportunity/{opportunityId}"
    const val OPPORTUNITY_EXTRACTION = "opportunity/{opportunityId}/extraction"

    fun addTask(preset: String? = null): String =
        if (preset.isNullOrBlank()) "task/add?preset=" else "task/add?preset=$preset"

    fun taskDetail(taskId: Long) = "task/$taskId"
    fun editTask(taskId: Long) = "task/edit/$taskId"
    fun opportunityDetail(opportunityId: Long) = "opportunity/$opportunityId"
    fun editOpportunity(opportunityId: Long) = "opportunity/edit/$opportunityId"
    fun opportunityExtraction(opportunityId: Long) = "opportunity/$opportunityId/extraction"
}
