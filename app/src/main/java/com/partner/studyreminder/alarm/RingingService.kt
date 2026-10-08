package com.partner.studyreminder.alarm

import android.app.Service
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.partner.studyreminder.ui.ReminderActivity

class RingingService : Service() {
    private var player: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val handler = Handler(Looper.getMainLooper())
    private var foreground = false
    private val quiet = Runnable { quietDown() }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent == null || intent.action == ACTION_STOP) {
            AlarmActions.stop(this)
            return START_NOT_STICKY
        }
        val title = intent.getStringExtra(AlarmContract.EXTRA_TITLE) ?: "学习提醒"
        val note = intent.getStringExtra(AlarmContract.EXTRA_NOTE).orEmpty()
        val kind = intent.getStringExtra(AlarmContract.EXTRA_KIND) ?: AlarmContract.KIND_START
        val whenLabel = intent.getStringExtra(AlarmContract.EXTRA_WHEN).orEmpty()
        val notification = NotificationHelper.build(this, title, note, kind, whenLabel, ongoing = true)
        try {
            if (Build.VERSION.SDK_INT >= 34) {
                startForeground(
                    AlarmContract.NOTIF_ID,
                    notification,
                    android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE,
                )
            } else {
                startForeground(AlarmContract.NOTIF_ID, notification)
            }
            foreground = true
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed", e)
            NotificationHelper.playFallbackSound(this)
            stopSelf()
            return START_NOT_STICKY
        }
        acquireWakeLock()
        raiseSilentAlarmVolume()
        startSound()
        startVibration()
        openScreen(title, note, kind, whenLabel)
        handler.removeCallbacks(quiet)
        handler.postDelayed(quiet, RING_MS)
        return START_NOT_STICKY
    }

    private fun openScreen(title: String, note: String, kind: String, whenLabel: String) {
        val ui = Intent(this, ReminderActivity::class.java).apply {
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(AlarmContract.EXTRA_TITLE, title)
            putExtra(AlarmContract.EXTRA_NOTE, note)
            putExtra(AlarmContract.EXTRA_KIND, kind)
            putExtra(AlarmContract.EXTRA_WHEN, whenLabel)
        }
        try {
            startActivity(ui)
        } catch (e: Exception) {
            Log.w(TAG, "full screen activity blocked", e)
        }
    }

    private fun startSound() {
        stopSound()
        val uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        if (uri == null) {
            NotificationHelper.playFallbackSound(this)
            return
        }
        val attributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        try {
            val media = MediaPlayer()
            media.setAudioAttributes(attributes)
            media.setDataSource(this, uri)
            media.isLooping = true
            media.setWakeMode(applicationContext, PowerManager.PARTIAL_WAKE_LOCK)
            media.prepare()
            media.start()
            player = media
            requestFocus(attributes)
        } catch (e: Exception) {
            Log.e(TAG, "media player failed", e)
            NotificationHelper.playFallbackSound(this)
        }
    }

    private fun requestFocus(attributes: AudioAttributes) {
        val audio = getSystemService(AudioManager::class.java) ?: return
        if (Build.VERSION.SDK_INT >= 26) {
            val request = android.media.AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
                .setAudioAttributes(attributes)
                .build()
            audio.requestAudioFocus(request)
        } else {
            @Suppress("DEPRECATION")
            audio.requestAudioFocus(null, AudioManager.STREAM_ALARM, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        }
    }

    private fun raiseSilentAlarmVolume() {
        val audio = getSystemService(AudioManager::class.java) ?: return
        try {
            val max = audio.getStreamMaxVolume(AudioManager.STREAM_ALARM)
            val current = audio.getStreamVolume(AudioManager.STREAM_ALARM)
            if (current == 0 && max > 0) {
                audio.setStreamVolume(AudioManager.STREAM_ALARM, (max * 0.6f).toInt().coerceAtLeast(1), 0)
            }
        } catch (e: Exception) {
            Log.w(TAG, "could not raise alarm volume", e)
        }
    }

    private fun startVibration() {
        vibrator = if (Build.VERSION.SDK_INT >= 31) {
            getSystemService(VibratorManager::class.java)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
        val pattern = longArrayOf(0, 700, 400, 700, 400, 1000)
        try {
            if (Build.VERSION.SDK_INT >= 26) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                @Suppress("DEPRECATION")
                vibrator?.vibrate(pattern, 0)
            }
        } catch (e: Exception) {
            Log.w(TAG, "vibrate failed", e)
        }
    }

    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val power = getSystemService(PowerManager::class.java) ?: return
        wakeLock = power.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "studyreminder:ring").apply {
            setReferenceCounted(false)
            acquire(RING_MS + 5_000L)
        }
    }

    private fun quietDown() {
        stopSound()
        vibrator?.cancel()
        releaseWakeLock()
        if (foreground) {
            stopForeground(STOP_FOREGROUND_DETACH)
            foreground = false
        }
        stopSelf()
    }

    private fun stopSound() {
        val media = player ?: return
        player = null
        try {
            if (media.isPlaying) media.stop()
        } catch (_: Exception) {
        }
        media.release()
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) wakeLock?.release()
        } catch (_: Exception) {
        }
        wakeLock = null
    }

    override fun onDestroy() {
        handler.removeCallbacks(quiet)
        stopSound()
        vibrator?.cancel()
        releaseWakeLock()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "StudyReminder"
        private const val RING_MS = 120_000L
        const val ACTION_START = "com.partner.studyreminder.action.RING"
        const val ACTION_STOP = "com.partner.studyreminder.action.RING_STOP"
    }
}
