package com.partner.studyreminder.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import java.io.File
import java.io.FileOutputStream
import kotlin.math.max

/** A photo copied into app storage, scaled down so the glass backdrop stays light. */
object Backgrounds {
    private const val MAX_EDGE = 1600

    /** Bumped on the main thread after the file changes, so every open page reloads. */
    private val changes = mutableLongStateOf(0L)

    fun file(context: Context): File = File(context.applicationContext.filesDir, "background.jpg")

    fun changes(): Long = changes.longValue

    fun notifyChanged() {
        changes.longValue = changes.longValue + 1L
    }

    /** 0 when the default gradient is in use. */
    fun stamp(context: Context): Long {
        val file = file(context)
        return if (file.exists() && file.length() > 0L) file.lastModified() else 0L
    }

    fun save(context: Context, uri: Uri) {
        val app = context.applicationContext
        val raw = File(app.cacheDir, "background-incoming")
        app.contentResolver.openInputStream(uri).use { input ->
            requireNotNull(input) { "打不开图片" }
            raw.outputStream().use { input.copyTo(it) }
        }
        try {
            val bitmap = decodeOriented(raw, MAX_EDGE)
            FileOutputStream(file(app)).use { bitmap.compress(Bitmap.CompressFormat.JPEG, 86, it) }
            bitmap.recycle()
        } finally {
            raw.delete()
        }
    }

    fun clear(context: Context) {
        file(context).delete()
    }

    fun load(context: Context): ImageBitmap? {
        val file = file(context)
        if (!file.exists() || file.length() == 0L) return null
        val bitmap = BitmapFactory.decodeFile(file.absolutePath) ?: return null
        return bitmap.asImageBitmap()
    }

    private fun decodeOriented(file: File, maxEdge: Int): Bitmap {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.absolutePath, bounds)
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) error("图片无法解码")
        val options = BitmapFactory.Options().apply {
            inSampleSize = BackgroundScale.sampleSize(bounds.outWidth, bounds.outHeight, maxEdge)
        }
        val decoded = BitmapFactory.decodeFile(file.absolutePath, options) ?: error("图片无法解码")
        val orientation = runCatching {
            ExifInterface(file.absolutePath).getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL,
            )
        }.getOrDefault(ExifInterface.ORIENTATION_NORMAL)
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90f
            ExifInterface.ORIENTATION_ROTATE_180 -> 180f
            ExifInterface.ORIENTATION_ROTATE_270 -> 270f
            else -> 0f
        }
        val rotated = if (degrees == 0f) {
            decoded
        } else {
            val matrix = Matrix().apply { postRotate(degrees) }
            Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true).also {
                if (it != decoded) decoded.recycle()
            }
        }
        val longest = max(rotated.width, rotated.height)
        if (longest <= maxEdge) return rotated
        val scale = maxEdge.toFloat() / longest
        val width = (rotated.width * scale).toInt().coerceAtLeast(1)
        val height = (rotated.height * scale).toInt().coerceAtLeast(1)
        return Bitmap.createScaledBitmap(rotated, width, height, true).also {
            if (it != rotated) rotated.recycle()
        }
    }
}

object BackgroundScale {
    /** Power-of-two sample size so the longer edge stays at least [maxEdge] before a final scale. */
    fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        if (width <= 0 || height <= 0 || maxEdge <= 0) return 1
        var size = 1
        val longest = max(width, height)
        while (longest / (size * 2) >= maxEdge) size *= 2
        return size
    }
}
