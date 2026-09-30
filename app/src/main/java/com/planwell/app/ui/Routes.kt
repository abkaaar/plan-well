package com.planwell.app.ui

object Routes {
    const val HOME = "home"
    const val ADD_TASK = "task/add"
    const val EDIT_TASK = "task/edit/{taskId}"
    const val TASK_DETAIL = "task/{taskId}"
    const val TRASH = "trash"
    const val CATEGORY = "category"
    const val SEARCH = "search"

    fun taskDetail(taskId: Long) = "task/$taskId"
    fun editTask(taskId: Long) = "task/edit/$taskId"
}
