package com.planwell.app.data.storage

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import com.planwell.app.data.ml.loadBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    suspend fun persistSharedImage(uri: Uri): String? = withContext(Dispatchers.IO) {
        runCatching {
            val bitmap = context.contentResolver.loadBitmap(uri) ?: return@runCatching null
            try {
                val dir = File(context.filesDir, IMPORTS_DIR).apply { mkdirs() }
                val out = File(dir, "${UUID.randomUUID()}.webp")
                out.outputStream().use { stream ->
                    val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        Bitmap.CompressFormat.WEBP_LOSSLESS
                    } else {
                        @Suppress("DEPRECATION")
                        Bitmap.CompressFormat.WEBP
                    }
                    val quality = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) 100 else 90
                    if (!bitmap.compress(format, quality, stream)) {
                        error("compress failed")
                    }
                }
                out.absolutePath
            } finally {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
        }.getOrNull()
    }

    companion object {
        const val IMPORTS_DIR = "imports"
    }
}
