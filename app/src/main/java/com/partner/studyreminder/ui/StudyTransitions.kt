package com.partner.studyreminder.ui

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.activity.ComponentActivity
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResultLauncher
import com.partner.studyreminder.R

/** Push and rise transitions. 320ms, PathInterpolator(0.2, 0.8, 0.2, 1). */
internal fun ComponentActivity.installPush() {
    installTransition(
        openEnter = R.anim.study_slide_in_right,
        openExit = R.anim.study_slide_out_left,
        closeEnter = R.anim.study_slide_in_left,
        closeExit = R.anim.study_slide_out_right,
    )
}

/** Modal pages, such as import preview, rise from the bottom. */
internal fun ComponentActivity.installRise() {
    installTransition(
        openEnter = R.anim.study_slide_in_bottom,
        openExit = R.anim.study_hold,
        closeEnter = R.anim.study_hold,
        closeExit = R.anim.study_slide_out_bottom,
    )
}

internal fun ComponentActivity.launchPush(intent: Intent) {
    startActivity(intent)
    if (Build.VERSION.SDK_INT < 34) {
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.study_slide_in_right, R.anim.study_slide_out_left)
    }
}

internal fun ComponentActivity.launchRise(launcher: ActivityResultLauncher<Intent>, intent: Intent) {
    launcher.launch(intent)
    if (Build.VERSION.SDK_INT < 34) {
        @Suppress("DEPRECATION")
        overridePendingTransition(R.anim.study_slide_in_bottom, R.anim.study_hold)
    }
}

internal fun ComponentActivity.finishPush() {
    finishWith(
        enter = R.anim.study_slide_in_left,
        exit = R.anim.study_slide_out_right,
    )
}

internal fun ComponentActivity.finishRise() {
    finishWith(
        enter = R.anim.study_hold,
        exit = R.anim.study_slide_out_bottom,
    )
}

private fun ComponentActivity.installTransition(
    openEnter: Int,
    openExit: Int,
    closeEnter: Int,
    closeExit: Int,
) {
    if (Build.VERSION.SDK_INT >= 34) {
        overrideActivityTransition(Activity.OVERRIDE_TRANSITION_OPEN, openEnter, openExit, Color.TRANSPARENT)
        overrideActivityTransition(Activity.OVERRIDE_TRANSITION_CLOSE, closeEnter, closeExit, Color.TRANSPARENT)
        return
    }
    onBackPressedDispatcher.addCallback(
        this,
        object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                isEnabled = false
                finish()
                @Suppress("DEPRECATION")
                overridePendingTransition(closeEnter, closeExit)
            }
        },
    )
}

private fun ComponentActivity.finishWith(enter: Int, exit: Int) {
    finish()
    if (Build.VERSION.SDK_INT < 34) {
        @Suppress("DEPRECATION")
        overridePendingTransition(enter, exit)
    }
}
