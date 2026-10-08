package com.partner.studyreminder.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.partner.studyreminder.BuildConfig
import com.partner.studyreminder.data.Backgrounds
import com.partner.studyreminder.alarm.AlarmScheduler
import com.partner.studyreminder.data.Prefs
import com.partner.studyreminder.sync.PlanSync
import com.partner.studyreminder.sync.SyncScheduler
import com.partner.studyreminder.ui.glass.GlassIconButton
import com.partner.studyreminder.ui.glass.LiquidButton
import com.partner.studyreminder.ui.glass.LiquidPage
import com.partner.studyreminder.ui.glass.LiquidSlider
import com.partner.studyreminder.ui.glass.LiquidSwitch
import com.partner.studyreminder.ui.glass.liquidGlass
import com.partner.studyreminder.ui.glass.squircle
import com.partner.studyreminder.ui.glass.studyColors
import com.partner.studyreminder.ui.theme.StudyTheme
import com.partner.studyreminder.ui.theme.ThemeMode
import com.partner.studyreminder.ui.theme.edgeToEdge
import com.partner.studyreminder.ui.theme.toast

class SettingsForm {
    var island by mutableStateOf(true)
    var preOn by mutableStateOf(false)
    var minutes by mutableStateOf("5")
    var url by mutableStateOf("")
    var message by mutableStateOf("")
    var syncing by mutableStateOf(false)
}

class SettingsActivity : ComponentActivity() {
    private val form = SettingsForm()
    private var backgroundStamp by mutableLongStateOf(0L)
    private var applyingBackground by mutableStateOf(false)

    private val pickBackground = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@registerForActivityResult
        applyingBackground = true
        Thread {
            val failed = runCatching { Backgrounds.save(applicationContext, uri) }.exceptionOrNull()
            runOnUiThread {
                applyingBackground = false
                if (failed == null) Backgrounds.notifyChanged()
                if (isDestroyed) return@runOnUiThread
                if (failed != null) toast("这张图片打不开")
                else toast("已更换背景", long = false)
                backgroundStamp = Backgrounds.stamp(this)
            }
        }.start()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        backgroundStamp = Backgrounds.stamp(this)
        super.onCreate(savedInstanceState)
        edgeToEdge()
        form.island = Prefs.isIslandMode(this)
        val minutes = Prefs.preMinutes(this)
        form.preOn = minutes > 0
        form.minutes = if (minutes > 0) minutes.toString() else "5"
        form.url = Prefs.syncUrl(this)
        form.message = Prefs.syncMessage(this).ifBlank { "还没有同步过。留空就不会自动同步。" }
        setContent {
            StudyTheme {
                SettingsScreen(
                    form = form,
                    version = "学习提醒 ${BuildConfig.VERSION_NAME}（${BuildConfig.VERSION_CODE}）",
                    onBack = { finish() },
                    onSave = { save(showToast = true) },
                    onSync = { syncNow() },
                    backgroundStamp = backgroundStamp,
                    applyingBackground = applyingBackground,
                    onPickBackground = {
                        if (!applyingBackground) {
                            pickBackground.launch(
                                PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                            )
                        }
                    },
                    onClearBackground = {
                        Backgrounds.clear(this)
                        Backgrounds.notifyChanged()
                        backgroundStamp = 0L
                        toast("已恢复默认背景", long = false)
                    },
                    onTheme = { mode ->
                        Prefs.setTheme(this, mode)
                        ThemeMode.notifyChanged()
                        edgeToEdge()
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        edgeToEdge()
    }

    override fun onPause() {
        super.onPause()
        if (!form.syncing) save(showToast = false)
    }

    private fun save(showToast: Boolean) {
        val url = form.url.trim()
        val previousUrl = Prefs.syncUrl(this)
        val previousPre = Prefs.preMinutes(this)
        val pre = if (!form.preOn) {
            0
        } else {
            form.minutes.toIntOrNull()?.coerceIn(1, 180) ?: 5
        }
        Prefs.setReminderMode(this, if (form.island) Prefs.MODE_ISLAND else Prefs.MODE_FULLSCREEN)
        Prefs.setPreMinutes(this, pre)
        Prefs.setSyncUrl(this, url)
        if (pre != previousPre) AlarmScheduler.rescheduleAll(this)
        if (url != previousUrl) SyncScheduler.replace(this) else SyncScheduler.ensure(this)
        if (showToast) toast("已保存", long = false)
    }

    private fun syncNow() {
        save(showToast = false)
        val url = Prefs.syncUrl(this)
        if (url.isBlank()) {
            form.message = "没有填写网址，不会自动同步"
            toast("先填写网址", long = false)
            return
        }
        form.syncing = true
        Thread {
            val message = PlanSync.run(applicationContext)
            runOnUiThread {
                form.syncing = false
                if (isDestroyed) return@runOnUiThread
                form.message = message
                toast(message)
            }
        }.start()
    }
}

@Composable
private fun ModeRow(
    title: String,
    hint: String,
    selected: Boolean,
    backdrop: com.kyant.backdrop.Backdrop,
    onClick: () -> Unit,
) {
    val colors = studyColors()
    val plate = if (selected) {
        Modifier.liquidGlass(
            backdrop,
            squircle(18.dp),
            colors.glass,
            blurRadius = 4.dp,
            refraction = 12.dp,
            chromatic = false,
        )
    } else {
        Modifier
    }
    Row(
        plate
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = if (selected) 12.dp else 0.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, color = colors.label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(2.dp))
            Text(hint, color = colors.secondary, fontSize = 13.sp, lineHeight = 18.sp)
        }
        if (selected) {
            Spacer(Modifier.width(12.dp))
            Icon(Icons.Rounded.Check, contentDescription = "已选", tint = colors.blue)
        }
    }
}

@Composable
private fun SettingsScreen(
    form: SettingsForm,
    version: String,
    onBack: () -> Unit,
    onSave: () -> Unit,
    onSync: () -> Unit,
    backgroundStamp: Long,
    applyingBackground: Boolean,
    onPickBackground: () -> Unit,
    onClearBackground: () -> Unit,
    onTheme: (String) -> Unit,
) {
    val colors = studyColors()
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedTextColor = colors.label,
        unfocusedTextColor = colors.label,
        focusedBorderColor = colors.blue,
        unfocusedBorderColor = colors.separator,
        cursorColor = colors.blue,
        focusedLabelColor = colors.blue,
        unfocusedLabelColor = colors.secondary,
        focusedContainerColor = Color.Transparent,
        unfocusedContainerColor = Color.Transparent,
        disabledContainerColor = Color.Transparent,
        disabledTextColor = colors.tertiary,
        disabledBorderColor = colors.separator,
    )
    val context = LocalContext.current
    val background = remember(backgroundStamp) { Backgrounds.load(context) }
    LiquidPage { backdrop ->
        Column(
            Modifier
                .fillMaxSize()
                .imePadding()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp)
                .padding(bottom = 28.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            GlassIconButton(onBack, backdrop) {
                Icon(Icons.Rounded.ChevronLeft, contentDescription = "返回", tint = colors.label)
            }
            Spacer(Modifier.height(12.dp))
            Text("设置", color = colors.label, fontSize = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp)
            Spacer(Modifier.height(18.dp))
            val theme = Prefs.theme(context)
            Column(
                Modifier
                    .fillMaxWidth()
                    .liquidGlass(backdrop, squircle(28.dp), colors.glass, blurRadius = 2.dp, refraction = 24.dp)
                    .padding(16.dp),
            ) {
                Text("外观", color = colors.secondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                ModeRow(
                    title = "浅色",
                    hint = "始终使用浅色背景。",
                    selected = theme == Prefs.THEME_LIGHT,
                    backdrop = backdrop,
                    onClick = { onTheme(Prefs.THEME_LIGHT) },
                )
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.separator))
                ModeRow(
                    title = "深色",
                    hint = "始终使用深色背景。",
                    selected = theme == Prefs.THEME_DARK,
                    backdrop = backdrop,
                    onClick = { onTheme(Prefs.THEME_DARK) },
                )
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.separator))
                ModeRow(
                    title = "跟随系统",
                    hint = "默认。和手机的浅色或深色保持一致。",
                    selected = theme == Prefs.THEME_SYSTEM,
                    backdrop = backdrop,
                    onClick = { onTheme(Prefs.THEME_SYSTEM) },
                )
            }
            Spacer(Modifier.height(16.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .liquidGlass(backdrop, squircle(28.dp), colors.glass, blurRadius = 2.dp, refraction = 24.dp)
                    .padding(16.dp),
            ) {
                Text("提醒方式", color = colors.secondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(6.dp))
                ModeRow(
                    title = "超级岛 + 悬浮通知",
                    hint = "到点弹出横幅，进行中停在岛上并倒计时。默认用这个。",
                    selected = form.island,
                    backdrop = backdrop,
                    onClick = { form.island = true },
                )
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.separator))
                ModeRow(
                    title = "全屏响铃",
                    hint = "到点打开全屏提醒页，循环响铃，直到关掉或再提醒。",
                    selected = !form.island,
                    backdrop = backdrop,
                    onClick = { form.island = false },
                )
            }
            Spacer(Modifier.height(16.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .liquidGlass(backdrop, squircle(28.dp), colors.glass, blurRadius = 2.dp, refraction = 24.dp)
                    .padding(16.dp),
            ) {
                Text("背景", color = colors.secondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(8.dp))
                Text(
                    if (background == null) {
                        "现在是默认的彩色渐变。选一张照片后，液态玻璃会叠在照片上。"
                    } else {
                        "正在使用自定义背景。照片只存在这台手机上。"
                    },
                    color = colors.secondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
                if (background != null) {
                    Spacer(Modifier.height(12.dp))
                    Image(
                        bitmap = background,
                        contentDescription = "当前背景",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clip(squircle(18.dp)),
                    )
                }
                Spacer(Modifier.height(12.dp))
                LiquidButton(
                    onClick = onPickBackground,
                    backdrop = backdrop,
                    modifier = Modifier.fillMaxWidth(),
                    tint = colors.blue,
                    enabled = !applyingBackground,
                ) {
                    Text(
                        if (applyingBackground) "正在设置…" else "选择图片",
                        color = Color.White,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                if (background != null) {
                    Spacer(Modifier.height(10.dp))
                    LiquidButton(
                        onClick = onClearBackground,
                        backdrop = backdrop,
                        modifier = Modifier.fillMaxWidth(),
                        surface = colors.glass,
                        enabled = !applyingBackground,
                    ) {
                        Text("恢复默认", color = colors.label, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    }
                }
            }
            Spacer(Modifier.height(16.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .liquidGlass(backdrop, squircle(28.dp), colors.glass, blurRadius = 2.dp, refraction = 24.dp)
                    .padding(16.dp),
            ) {
                Text("提前提醒", color = colors.secondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("开始前再提醒一次", color = colors.label, fontSize = 17.sp)
                        Spacer(Modifier.height(2.dp))
                        Text("默认关闭。打开后，每个时间段开始前会再响一次。", color = colors.secondary, fontSize = 13.sp, lineHeight = 18.sp)
                    }
                    Spacer(Modifier.width(12.dp))
                    LiquidSwitch(
                        checked = form.preOn,
                        onCheckedChange = { form.preOn = it },
                        backdrop = backdrop,
                    )
                }
                Spacer(Modifier.height(12.dp))
                Box(Modifier.fillMaxWidth().height(0.5.dp).background(colors.separator))
                Spacer(Modifier.height(12.dp))
                val minutesValue = form.minutes.toIntOrNull()?.coerceIn(1, 180) ?: 5
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "$minutesValue",
                        color = colors.label.copy(alpha = if (form.preOn) 1f else 0.4f),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        " 分钟",
                        color = colors.secondary.copy(alpha = if (form.preOn) 1f else 0.4f),
                        fontSize = 17.sp,
                    )
                }
                Spacer(Modifier.height(8.dp))
                LiquidSlider(
                    value = minutesValue.toFloat(),
                    onValueChange = { next ->
                        if (form.preOn) form.minutes = next.toInt().coerceIn(1, 180).toString()
                    },
                    valueRange = 1f..180f,
                    backdrop = backdrop,
                    enabled = form.preOn,
                )
                Text("拖动玻璃滑块。范围 1 到 180 分钟。", color = colors.tertiary, fontSize = 13.sp)
            }
            Spacer(Modifier.height(16.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .liquidGlass(backdrop, squircle(28.dp), colors.glass, blurRadius = 2.dp, refraction = 24.dp)
                    .padding(16.dp),
            ) {
                Text("网址同步", color = colors.secondary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    value = form.url,
                    onValueChange = { form.url = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("计划网址，留空则不自动同步") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    minLines = 2,
                    colors = fieldColors,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    "网址返回纯文本计划或 .ics 都可以。填写后大约每 2 小时自动同步一次，有网才会跑。同步只更新从网址来的安排，手动添加的会留下来。",
                    color = colors.secondary,
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                )
                Spacer(Modifier.height(12.dp))
                LiquidButton(
                    onClick = onSync,
                    backdrop = backdrop,
                    modifier = Modifier.fillMaxWidth(),
                    surface = colors.glass,
                    enabled = !form.syncing,
                ) {
                    Text(
                        if (form.syncing) "同步中…" else "立即同步",
                        color = colors.label,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(form.message, color = colors.secondary, fontSize = 14.sp, lineHeight = 20.sp)
            }
            Spacer(Modifier.height(18.dp))
            LiquidButton(
                onClick = onSave,
                backdrop = backdrop,
                modifier = Modifier.fillMaxWidth(),
                tint = colors.blue,
                height = 56.dp,
            ) {
                Text("保存", color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
            Spacer(Modifier.height(16.dp))
            Text(version, color = colors.tertiary, fontSize = 13.sp, modifier = Modifier.fillMaxWidth())
        }
    }
}
