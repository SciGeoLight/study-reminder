package com.partner.studyreminder.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import kotlin.math.max

/**
 * Copies todo photos into app-private storage so they survive the gallery item
 * being deleted. Sync never reads or writes these files.
 */
object TodoImages {
    const val MAX_COUNT = 8
    private const val MAX_EDGE = 1600
    private const val JPEG_QUALITY = 82

    fun root(filesDir: File): File = File(filesDir, "todo-images")

    fun directory(filesDir: File, todoId: String): File = File(root(filesDir), todoId)

    fun file(filesDir: File, todoId: String, name: String): File = File(directory(filesDir, todoId), name)

    fun importUri(context: Context, filesDir: File, todoId: String, uri: Uri): String? {
        val bitmap = decode(open = { context.contentResolver.openInputStream(uri) }, maxEdge = MAX_EDGE) ?: return null
        return write(filesDir, todoId, bitmap)
    }

    fun importFile(filesDir: File, todoId: String, source: File): String? {
        val bitmap = decode(open = { source.inputStream() }, maxEdge = MAX_EDGE) ?: return null
        val name = write(filesDir, todoId, bitmap)
        source.delete()
        return name
    }

    fun delete(filesDir: File, todoId: String, name: String) {
        if (!safeName(name)) return
        file(filesDir, todoId, name).delete()
    }

    fun deleteAll(filesDir: File, todoId: String) {
        directory(filesDir, todoId).deleteRecursively()
    }

    fun thumb(file: File, edge: Int): Bitmap? {
        if (!file.exists()) return null
        return decode(open = { file.inputStream() }, maxEdge = edge)
    }

    fun thumb(context: Context, uri: Uri, edge: Int): Bitmap? {
        return decode(open = { context.contentResolver.openInputStream(uri) }, maxEdge = edge)
    }

    private fun write(filesDir: File, todoId: String, bitmap: Bitmap): String? {
        val dir = directory(filesDir, todoId)
        if (!dir.exists() && !dir.mkdirs()) return null
        val name = UUID.randomUUID().toString() + ".jpg"
        val dest = File(dir, name)
        val ok = runCatching {
            FileOutputStream(dest).use { out ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, JPEG_QUALITY, out)
            }
        }.getOrDefault(false)
        bitmap.recycle()
        if (!ok || dest.length() == 0L) {
            dest.delete()
            return null
        }
        return name
    }

    private fun decode(open: () -> java.io.InputStream?, maxEdge: Int): Bitmap? {
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        open()?.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) return null
        val sample = sampleSize(bounds.outWidth, bounds.outHeight, maxEdge)
        val opts = BitmapFactory.Options().apply {
            inSampleSize = sample
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val decoded = open()?.use { BitmapFactory.decodeStream(it, null, opts) } ?: return null
        val oriented = runCatching { applyExif(open, decoded) }.getOrDefault(decoded)
        return scaleDown(oriented, maxEdge)
    }

    private fun applyExif(open: () -> java.io.InputStream?, bitmap: Bitmap): Bitmap {
        val rotation = open()?.use { stream ->
            val exif = ExifInterface(stream)
            when (exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)) {
                ExifInterface.ORIENTATION_ROTATE_90 -> 90
                ExifInterface.ORIENTATION_ROTATE_180 -> 180
                ExifInterface.ORIENTATION_ROTATE_270 -> 270
                else -> 0
            }
        } ?: 0
        if (rotation == 0) return bitmap
        val matrix = Matrix().apply { postRotate(rotation.toFloat()) }
        val turned = Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        if (turned != bitmap) bitmap.recycle()
        return turned
    }

    private fun scaleDown(bitmap: Bitmap, maxEdge: Int): Bitmap {
        val edge = max(bitmap.width, bitmap.height)
        if (edge <= maxEdge || edge == 0) return bitmap
        val scale = maxEdge.toFloat() / edge
        val width = (bitmap.width * scale).toInt().coerceAtLeast(1)
        val height = (bitmap.height * scale).toInt().coerceAtLeast(1)
        val scaled = Bitmap.createScaledBitmap(bitmap, width, height, true)
        if (scaled != bitmap) bitmap.recycle()
        return scaled
    }

    private fun sampleSize(width: Int, height: Int, maxEdge: Int): Int {
        var sample = 1
        val edge = max(width, height)
        while (edge / sample > maxEdge * 2) sample *= 2
        return sample
    }

    fun safeName(name: String): Boolean = name.matches(Regex("[A-Za-z0-9._-]{1,80}"))
}
