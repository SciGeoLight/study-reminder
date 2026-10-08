package com.partner.studyreminder.data

import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import java.io.FileOutputStream
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [35])
class BackgroundCacheTest {
    @Test(timeout = 20_000)
    fun activitiesShareOneDecodedBitmap() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        Backgrounds.clear(context)
        try {
            writeJpeg(context)
            val first = Backgrounds.load(context)
            val second = Backgrounds.load(context)
            assertTrue(first != null)
            assertSame(first, second)
            assertSame(first, Backgrounds.peek(context))
            Backgrounds.clear(context)
            assertNull(Backgrounds.peek(context))
            writeJpeg(context)
            val third = Backgrounds.load(context)
            assertTrue(third != null)
            assertNotSame(first, third)
        } finally {
            Backgrounds.clear(context)
        }
    }

    private fun writeJpeg(context: android.content.Context) {
        val bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888)
        bitmap.eraseColor(Color.BLUE)
        FileOutputStream(Backgrounds.file(context)).use {
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, it)
        }
        bitmap.recycle()
    }
}
