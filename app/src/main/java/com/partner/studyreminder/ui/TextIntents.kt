package com.partner.studyreminder.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import java.io.ByteArrayOutputStream

object TextIntents {
    fun fromIntent(context: Context, intent: Intent?): String? {
        if (intent == null) return null
        return when (intent.action) {
            Intent.ACTION_VIEW -> intent.data?.let { read(context, it) }
            Intent.ACTION_SEND -> {
                val stream = streamUri(intent)
                when {
                    stream != null -> read(context, stream)
                    !intent.getStringExtra(Intent.EXTRA_TEXT).isNullOrBlank() ->
                        intent.getStringExtra(Intent.EXTRA_TEXT)
                    else -> null
                }
            }
            Intent.ACTION_SEND_MULTIPLE -> {
                val list = streamList(intent)
                val first = list?.firstOrNull() ?: return null
                read(context, first)
            }
            else -> null
        }
    }

    fun read(context: Context, uri: Uri): String {
        context.contentResolver.openInputStream(uri).use { input ->
            if (input == null) error("打不开这个文件")
            val buffer = ByteArray(8192)
            val out = ByteArrayOutputStream()
            var total = 0
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                total += read
                if (total > 2_000_000) error("文件超过 2MB")
                out.write(buffer, 0, read)
            }
            return out.toByteArray().toString(Charsets.UTF_8).removePrefix("\uFEFF")
        }
    }

    private fun streamUri(intent: Intent): Uri? {
        return if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableExtra(Intent.EXTRA_STREAM)
        }
    }

    private fun streamList(intent: Intent): ArrayList<Uri>? {
        return if (Build.VERSION.SDK_INT >= 33) {
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM, Uri::class.java)
        } else {
            @Suppress("DEPRECATION")
            intent.getParcelableArrayListExtra(Intent.EXTRA_STREAM)
        }
    }
}
