package com.planwell.app.domain.usecase.task

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import com.planwell.app.domain.repository.TaskRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

class ExportTasksUseCase @Inject constructor(
    private val repository: TaskRepository,
    @ApplicationContext private val context: Context,
) {
    suspend operator fun invoke(): Uri {
        val tasks = repository.observeActiveTasks().first()
        val array = JSONArray()
        tasks.forEach { task ->
            array.put(
                JSONObject().apply {
                    put("id", task.id)
                    put("title", task.title)
                    put("description", task.description)
                    put("dueDateMs", task.dueDateMs)
                    put("priority", task.priority.name)
                    put("categoryId", task.categoryId)
                    put("isCompleted", task.isCompleted)
                    put("completedAtMs", task.completedAtMs)
                    put("createdAtMs", task.createdAtMs)
                    put("updatedAtMs", task.updatedAtMs)
                },
            )
        }
        val body = JSONObject().put("exportedAtMs", System.currentTimeMillis()).put("tasks", array)
        val stamp = SimpleDateFormat("yyyyMMdd-HHmmss", Locale.US).format(Date())
        val fileName = "planwell-tasks-$stamp.json"
        val bytes = body.toString(2).toByteArray(Charsets.UTF_8)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val resolver = context.contentResolver
            val collection = MediaStore.Downloads.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
            val values = ContentValues().apply {
                put(MediaStore.MediaColumns.DISPLAY_NAME, fileName)
                put(MediaStore.MediaColumns.MIME_TYPE, "application/json")
                put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS)
                put(MediaStore.MediaColumns.IS_PENDING, 1)
            }
            val uri = resolver.insert(collection, values)
                ?: error("Could not create export file")
            resolver.openOutputStream(uri)?.use { it.write(bytes) }
                ?: error("Could not write export file")
            values.clear()
            values.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, values, null, null)
            return uri
        }

        val dir = context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: error("Downloads folder unavailable")
        val file = java.io.File(dir, fileName)
        file.writeBytes(bytes)
        return Uri.fromFile(file)
    }
}
