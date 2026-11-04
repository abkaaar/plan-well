package com.planwell.app.data.ml

import android.content.ContentResolver
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import androidx.exifinterface.media.ExifInterface
import java.io.IOException
import kotlin.math.max
import kotlin.math.roundToInt

private const val MAX_LONG_EDGE = 2048

/**
 * Decodes [uri] to a bitmap, downsampling so the long edge is at most 2048px,
 * and correcting EXIF orientation. Returns null if the image is unreadable.
 */
fun ContentResolver.loadBitmap(uri: Uri): Bitmap? {
    return runCatching {
        val decoded = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            decodeWithImageDecoder(uri)
        } else {
            decodeLegacy(uri)
        } ?: return null
        applyExifOrientation(uri, decoded)
    }.getOrNull()
}

private fun ContentResolver.decodeWithImageDecoder(uri: Uri): Bitmap? {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) return null
    val source = ImageDecoder.createSource(this, uri)
    return ImageDecoder.decodeBitmap(source) { decoder, info, _ ->
        val width = info.size.width
        val height = info.size.height
        val longEdge = max(width, height)
        if (longEdge > MAX_LONG_EDGE) {
            val scale = MAX_LONG_EDGE.toFloat() / longEdge
            decoder.setTargetSize(
                (width * scale).roundToInt().coerceAtLeast(1),
                (height * scale).roundToInt().coerceAtLeast(1),
            )
        }
        // Software bitmap is safer for ML Kit InputImage.
        decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
    }
}

@Suppress("DEPRECATION")
private fun ContentResolver.decodeLegacy(uri: Uri): Bitmap? {
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        ?: return runCatching {
            MediaStore.Images.Media.getBitmap(this, uri)
        }.getOrNull()?.let { scaleDown(it) }

    val sample = calculateInSampleSize(bounds.outWidth, bounds.outHeight, MAX_LONG_EDGE)
    val opts = BitmapFactory.Options().apply { inSampleSize = sample }
    val bitmap = openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, opts) }
        ?: return null
    return scaleDown(bitmap)
}

private fun scaleDown(bitmap: Bitmap): Bitmap {
    val longEdge = max(bitmap.width, bitmap.height)
    if (longEdge <= MAX_LONG_EDGE) return bitmap
    val scale = MAX_LONG_EDGE.toFloat() / longEdge
    val w = (bitmap.width * scale).roundToInt().coerceAtLeast(1)
    val h = (bitmap.height * scale).roundToInt().coerceAtLeast(1)
    val scaled = Bitmap.createScaledBitmap(bitmap, w, h, true)
    if (scaled !== bitmap) bitmap.recycle()
    return scaled
}

private fun calculateInSampleSize(width: Int, height: Int, maxLongEdge: Int): Int {
    var sample = 1
    var longEdge = max(width, height)
    while (longEdge / (sample * 2) >= maxLongEdge) {
        sample *= 2
    }
    return sample.coerceAtLeast(1)
}

private fun ContentResolver.applyExifOrientation(uri: Uri, bitmap: Bitmap): Bitmap {
    // ImageDecoder already applies EXIF orientation on API 28+.
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) return bitmap

    val orientation = try {
        openInputStream(uri)?.use { stream ->
            ExifInterface(stream).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        } ?: ExifInterface.ORIENTATION_NORMAL
    } catch (_: IOException) {
        ExifInterface.ORIENTATION_NORMAL
    }

    val matrix = Matrix()
    when (orientation) {
        ExifInterface.ORIENTATION_FLIP_HORIZONTAL -> matrix.setScale(-1f, 1f)
        ExifInterface.ORIENTATION_ROTATE_180 -> matrix.setRotate(180f)
        ExifInterface.ORIENTATION_FLIP_VERTICAL -> {
            matrix.setRotate(180f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_TRANSPOSE -> {
            matrix.setRotate(90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_90 -> matrix.setRotate(90f)
        ExifInterface.ORIENTATION_TRANSVERSE -> {
            matrix.setRotate(-90f)
            matrix.postScale(-1f, 1f)
        }
        ExifInterface.ORIENTATION_ROTATE_270 -> matrix.setRotate(-90f)
        else -> return bitmap
    }
    val oriented = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    if (oriented !== bitmap) bitmap.recycle()
    return oriented
}
