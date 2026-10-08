package com.partner.studyreminder.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.Backdrop
import com.kyant.shapes.Capsule
import com.partner.studyreminder.ui.icons.StudyIcons
import com.partner.studyreminder.BuildConfig
import com.partner.studyreminder.alarm.AlarmScheduler
import com.partner.studyreminder.data.Backgrounds
import com.partner.studyreminder.data.Prefs
import com.partner.studyreminder.sync.PlanSync
import com.partner.studyreminder.sync.SyncScheduler
import com.partner.studyreminder.ui.glass.GlassIconButton
import com.partner.studyreminder.ui.glass.GlassTier
import com.partner.studyreminder.ui.glass.LiquidButton
import com.partner.studyreminder.ui.glass.LiquidPage
import com.partner.studyreminder.ui.glass.LiquidSlider
import com.partner.studyreminder.ui.glass.LiquidSwitch
import com.partner.studyreminder.ui.glass.glass
import com.partner.studyreminder.ui.glass.rememberSharedBackground
import com.partner.studyreminder.ui.glass.squircle
import com.partner.studyreminder.ui.glass.studyColors
import com.partner.studyreminder.ui.theme.StudyTheme
import com.partner.studyreminder.ui.theme.ThemeMode
import com.partner.studyreminder.ui.theme.edgeToEdge
import com.partner.studyreminder.ui.theme.toast
import kotlinx.coroutines.delay

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
        installPush()
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
                    onBack = { finishPush() },
                    onSave = { save() },
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
        if (!form.syncing) save()
    }

    private fun save() {
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
    }

    private fun syncNow() {
        save()
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
                val copy = syncStatusCopy(message, Prefs.syncSuccessAt(this), AlarmScheduler.upcomingCount(this))
                toast(copy.suggestion ?: copy.result)
            }
        }.start()
    }
}

private val LightPreview = listOf(Color(0xFFB9DCFF), Color(0xFFE7D4FF), Color(0xFFFFE0EC))
private val DarkPreview = listOf(Color(0xFF0B1020), Color(0xFF1A1440), Color(0xFF123044))

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
    val context = LocalContext.current
    val background by rememberSharedBackground(backgroundStamp)
    LaunchedEffect(form.island, form.preOn, form.minutes, form.url) {
        delay(500)
        if (!form.syncing) onSave()
    }
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
                Icon(StudyIcons.ChevronLeft, contentDescription = "返回", tint = colors.label)
            }
            Spacer(Modifier.height(12.dp))
            Text("设置", color = colors.label, fontSize = 34.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp)
            Spacer(Modifier.height(22.dp))
            val theme = Prefs.theme(context)
            SettingsGroup(
                title = "外观",
                footnote = "默认跟随系统。也可以固定浅色或深色。",
                backdrop = backdrop,
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ThemeThumb("浅色", theme == Prefs.THEME_LIGHT, light = true, split = false) {
                        onTheme(Prefs.THEME_LIGHT)
                    }
                    ThemeThumb("深色", theme == Prefs.THEME_DARK, light = false, split = false) {
                        onTheme(Prefs.THEME_DARK)
                    }
                    ThemeThumb("跟随系统", theme == Prefs.THEME_SYSTEM, light = true, split = true) {
                        onTheme(Prefs.THEME_SYSTEM)
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            SettingsGroup(
                title = "提醒方式",
                footnote = "超级岛到点弹出横幅，进行中停在岛上并倒计时。全屏响铃会打开提醒页，循环响铃，直到关掉或再提醒。",
                backdrop = backdrop,
            ) {
                SettingsIconRow(
                    icon = StudyIcons.Notifications,
                    tile = colors.blue,
                    title = "超级岛 + 悬浮通知",
                    onClick = { form.island = true },
                ) {
                    if (form.island) {
                        Icon(StudyIcons.Check, contentDescription = "已选", tint = colors.blue, modifier = Modifier.size(20.dp))
                    }
                }
                SettingsDivider()
                SettingsIconRow(
                    icon = StudyIcons.Alarm,
                    tile = colors.orange,
                    title = "全屏响铃",
                    onClick = { form.island = false },
                ) {
                    if (!form.island) {
                        Icon(StudyIcons.Check, contentDescription = "已选", tint = colors.blue, modifier = Modifier.size(20.dp))
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            val minutesValue = form.minutes.toIntOrNull()?.coerceIn(1, 180) ?: 5
            val lightPlate = colors.label.red < 0.5f
            val plate = if (lightPlate) Color.White else Color(0xFF1C1C1E)
            val digit = if (lightPlate) colors.label else Color.White
            val minuteLabel = if (lightPlate) colors.secondary else Color.White.copy(alpha = 0.72f)
            val digitAlpha = if (form.preOn) 1f else 0.45f
            SettingsGroup(
                title = "提前提醒",
                footnote = "默认关闭。打开后，每个时间段开始前会再响一次。范围 1 到 180 分钟。",
                backdrop = backdrop,
            ) {
                SettingsIconRow(
                    icon = StudyIcons.Alarm,
                    tile = colors.green,
                    title = "开始前再提醒一次",
                ) {
                    LiquidSwitch(
                        checked = form.preOn,
                        onCheckedChange = { form.preOn = it },
                        backdrop = backdrop,
                    )
                }
                SettingsDivider()
                Row(
                    Modifier
                        .padding(start = 16.dp, end = 16.dp, top = 10.dp, bottom = 4.dp)
                        .clip(Capsule())
                        .background(plate)
                        .padding(horizontal = 14.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "$minutesValue",
                        color = digit.copy(alpha = digitAlpha),
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        " 分钟",
                        color = minuteLabel.copy(alpha = minuteLabel.alpha * digitAlpha),
                        fontSize = 17.sp,
                    )
                }
                LiquidSlider(
                    value = minutesValue.toFloat(),
                    onValueChange = { next ->
                        if (form.preOn) form.minutes = next.toInt().coerceIn(1, 180).toString()
                    },
                    valueRange = 1f..180f,
                    backdrop = backdrop,
                    enabled = form.preOn,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, bottom = 12.dp),
                )
            }
            Spacer(Modifier.height(22.dp))
            SettingsGroup(
                title = "背景",
                footnote = if (background == null) {
                    "现在是默认的彩色渐变。选一张照片后，液态玻璃会叠在照片上。"
                } else {
                    "正在使用自定义背景。照片只存在这台手机上。"
                },
                backdrop = backdrop,
            ) {
                val preview = background
                if (preview != null) {
                    Image(
                        bitmap = preview,
                        contentDescription = "当前背景",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp)
                            .height(140.dp)
                            .clip(squircle(16.dp)),
                    )
                    SettingsDivider()
                }
                SettingsIconRow(
                    icon = StudyIcons.PhotoLibrary,
                    tile = colors.blue,
                    title = if (applyingBackground) "正在设置…" else "选择图片",
                    enabled = !applyingBackground,
                    onClick = onPickBackground,
                )
                if (background != null) {
                    SettingsDivider()
                    SettingsIconRow(
                        icon = StudyIcons.Close,
                        tile = Color(0xFF8E8E93),
                        title = "恢复默认",
                        enabled = !applyingBackground,
                        onClick = onClearBackground,
                    )
                }
            }
            Spacer(Modifier.height(22.dp))
            val copy = syncStatusCopy(
                form.message,
                Prefs.syncSuccessAt(context),
                AlarmScheduler.upcomingCount(context),
            )
            SettingsGroup(
                title = "同步",
                footnote = "网址返回纯文本计划或 .ics 都可以。填写后大约每 2 小时自动同步一次，有网才会跑。同步只更新从网址来的安排，手动添加的会留下来。",
                backdrop = backdrop,
            ) {
                BasicTextField(
                    value = form.url,
                    onValueChange = { form.url = it },
                    textStyle = TextStyle(color = colors.label, fontSize = 16.sp),
                    cursorBrush = SolidColor(colors.blue),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                    maxLines = 3,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    decorationBox = { inner ->
                        Box {
                            if (form.url.isEmpty()) {
                                Text("计划网址，留空则不自动同步", color = colors.tertiary, fontSize = 16.sp)
                            }
                            inner()
                        }
                    },
                )
                SettingsDivider()
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    SyncLine("上次成功", copy.lastSuccess)
                    SyncLine("结果", if (form.syncing) "同步中…" else copy.result)
                    if (!form.syncing && copy.suggestion != null) {
                        SyncLine("建议", copy.suggestion)
                    }
                    SyncLine("识别", copy.recognized)
                    SyncLine("已排闹钟", copy.scheduled)
                    if (copy.overflow != null) {
                        Text(copy.overflow, color = colors.orange, fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
                    }
                }
                LiquidButton(
                    onClick = onSync,
                    backdrop = backdrop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
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
            }
            Spacer(Modifier.height(22.dp))
            Text(version, color = colors.tertiary, fontSize = 13.sp, modifier = Modifier.padding(start = 16.dp))
        }
    }
}

@Composable
private fun SettingsGroup(
    title: String,
    footnote: String,
    backdrop: Backdrop,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val colors = studyColors()
    Text(
        title,
        color = colors.secondary,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier.padding(start = 16.dp, bottom = 6.dp),
    )
    Column(
        Modifier
            .fillMaxWidth()
            .glass(backdrop, GlassTier.Card, squircle(22.dp)),
        content = content,
    )
    Spacer(Modifier.height(6.dp))
    Text(
        footnote,
        color = colors.tertiary,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        modifier = Modifier.padding(horizontal = 16.dp),
    )
}

@Composable
private fun SettingsIconRow(
    icon: ImageVector,
    tile: Color,
    title: String,
    enabled: Boolean = true,
    onClick: (() -> Unit)? = null,
    trailing: @Composable () -> Unit = {},
) {
    val colors = studyColors()
    Row(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = null,
                        indication = null,
                        enabled = enabled,
                        onClick = onClick,
                    )
                } else {
                    Modifier
                },
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(30.dp)
                .clip(squircle(8.dp))
                .background(tile),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(12.dp))
        Text(
            title,
            color = colors.label,
            fontSize = 17.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.weight(1f),
        )
        trailing()
    }
}

@Composable
private fun SettingsDivider() {
    val colors = studyColors()
    Box(
        Modifier
            .fillMaxWidth()
            .padding(start = 58.dp)
            .height(0.5.dp)
            .background(colors.separator),
    )
}

@Composable
private fun RowScope.ThemeThumb(
    label: String,
    selected: Boolean,
    light: Boolean,
    split: Boolean,
    onClick: () -> Unit,
) {
    val colors = studyColors()
    val shape = squircle(16.dp)
    val stroke = if (selected) 2.dp else 1.dp
    val strokeColor = if (selected) colors.blue else colors.separator
    Column(
        Modifier
            .weight(1f)
            .clickable(interactionSource = null, indication = null, onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .height(72.dp)
                .border(stroke, strokeColor, shape)
                .clip(shape),
        ) {
            if (split) {
                Row(Modifier.fillMaxSize()) {
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Brush.verticalGradient(LightPreview)),
                    )
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .background(Brush.verticalGradient(DarkPreview)),
                    )
                }
            } else {
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(Brush.verticalGradient(if (light) LightPreview else DarkPreview)),
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            label,
            color = if (selected) colors.blue else colors.secondary,
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SyncLine(label: String, value: String) {
    val colors = studyColors()
    Row(Modifier.fillMaxWidth().padding(vertical = 2.dp), verticalAlignment = Alignment.Top) {
        Text(label, color = colors.secondary, fontSize = 15.sp, modifier = Modifier.width(76.dp))
        Text(value, color = colors.label, fontSize = 15.sp, lineHeight = 20.sp, modifier = Modifier.weight(1f))
    }
}
