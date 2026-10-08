package com.partner.studyreminder.ui

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.kyant.shapes.Capsule
import com.partner.studyreminder.ui.icons.StudyIcons
import com.partner.studyreminder.alarm.AlarmContract
import com.partner.studyreminder.alarm.FocusParams
import com.partner.studyreminder.alarm.IslandNotifications
import com.partner.studyreminder.data.Prefs
import com.partner.studyreminder.ui.glass.GlassIconButton
import com.partner.studyreminder.ui.glass.LiquidButton
import com.partner.studyreminder.ui.glass.LiquidPage
import com.partner.studyreminder.ui.glass.liquidGlass
import com.partner.studyreminder.ui.glass.squircle
import com.partner.studyreminder.ui.glass.studyColors
import com.partner.studyreminder.ui.theme.StudyTheme
import com.partner.studyreminder.ui.theme.edgeToEdge
import com.partner.studyreminder.ui.theme.toast

class PermissionActivity : ComponentActivity() {
    private var tick by mutableIntStateOf(0)

    private val requestNotifications = registerForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { tick++ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        edgeToEdge()
        setContent {
            StudyTheme {
                val refresh = tick
                PermissionScreen(
                    refreshKey = refresh,
                    advice = OemSettings.advice(),
                    rows = listOf(
                        PermRow("通知", "到点时要弹出悬浮通知。Android 13 及以上需要单独允许。", notificationsOn(), "去打开", { openNotifications() }),
                        PermRow(
                            title = "实时活动",
                            hint = "Android 16 及以上（含 Android 17）的实时活动。开启后，进行中的倒计时可以出现在状态栏。",
                            ok = promotedOn(),
                            action = "去打开",
                            onAction = { openPromoted() },
                        ),
                        PermRow(
                            title = "悬浮通知 / 锁屏通知",
                            hint = if (FocusParams.isXiaomi(Build.MANUFACTURER, Build.BRAND)) {
                                "小米澎湃 OS 请在通知管理里打开「悬浮通知」和「锁屏通知」，超级岛才会出现。"
                            } else {
                                "请允许悬浮通知，并允许锁屏时显示这条提醒。"
                            },
                            ok = headsChannelOn(),
                            action = "通知设置",
                            onAction = { openNotificationSettings() },
                        ),
                        PermRow("精确闹钟", "锁屏、省电时也按设定的分钟响。这是最重要的一项。", exactAlarmOn(), "去打开", { openExactAlarm() }),
                        PermRow("全屏提醒", "锁屏时直接弹出提醒页。Android 14 及以上需要允许。", fullScreenOn(), "去打开", { openFullScreen() }),
                        PermRow("忽略电池优化", "避免系统为了省电把闹钟推迟。请选「允许」。", batteryOn(), "去打开", { openBattery() }),
                        PermRow(
                            title = "自启动 / 后台",
                            hint = "系统检测不到这项。打开设置并允许「学习提醒」后，点「我已允许」。",
                            ok = Prefs.autostartConfirmed(this),
                            action = "去设置",
                            onAction = { openAutostart() },
                            secondary = "我已允许",
                            onSecondary = {
                                Prefs.setAutostartConfirmed(this, true)
                                tick++
                            },
                        ),
                    ),
                    doneLabel = if (Prefs.sawPermissions(this)) "完成" else "进入应用",
                    onBack = { finish() },
                    onDone = {
                        Prefs.setSawPermissions(this, true)
                        finish()
                    },
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        edgeToEdge()
        tick++
    }

    private fun notificationsOn(): Boolean {
        val manager = getSystemService(NotificationManager::class.java)
        val enabled = manager?.areNotificationsEnabled() == true
        if (!enabled) return false
        if (Build.VERSION.SDK_INT < 33) return true
        return ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
    }

    private fun exactAlarmOn(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return true
        val manager = getSystemService(AlarmManager::class.java) ?: return false
        return manager.canScheduleExactAlarms()
    }

    private fun fullScreenOn(): Boolean {
        if (Build.VERSION.SDK_INT < 34) return true
        val manager = getSystemService(NotificationManager::class.java) ?: return false
        return manager.canUseFullScreenIntent()
    }

    private fun batteryOn(): Boolean {
        val power = getSystemService(PowerManager::class.java) ?: return false
        return power.isIgnoringBatteryOptimizations(packageName)
    }

    private fun promotedOn(): Boolean = IslandNotifications.canPostPromoted(this)

    private fun headsChannelOn(): Boolean {
        if (!notificationsOn()) return false
        IslandNotifications.ensureChannels(this)
        val manager = getSystemService(NotificationManager::class.java) ?: return false
        val channel = manager.getNotificationChannel(AlarmContract.CHANNEL_HEADS) ?: return true
        return channel.importance >= NotificationManager.IMPORTANCE_HIGH
    }

    private fun openPromoted() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.BAKLAVA) {
            toast("实时活动需要 Android 16。", long = false)
            return
        }
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_PROMOTION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        startSafely(intent)
    }

    private fun openNotificationSettings() {
        IslandNotifications.ensureChannels(this)
        val channel = Intent(Settings.ACTION_CHANNEL_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            putExtra(Settings.EXTRA_CHANNEL_ID, AlarmContract.CHANNEL_HEADS)
        }
        if (!startSafely(channel, toastOnFail = false)) openNotifications()
    }

    private fun openNotifications() {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            requestNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            return
        }
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        }
        startSafely(intent)
    }

    private fun openExactAlarm() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            toast("这个系统版本不需要单独开精确闹钟。", long = false)
            return
        }
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            data = Uri.parse("package:$packageName")
        }
        startSafely(intent)
    }

    private fun openFullScreen() {
        if (Build.VERSION.SDK_INT < 34) {
            toast("这个系统版本不需要单独开全屏提醒。", long = false)
            return
        }
        val intent = Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT).apply {
            data = Uri.parse("package:$packageName")
        }
        startSafely(intent)
    }

    private fun openBattery() {
        val request = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        if (!startSafely(request, toastOnFail = false)) {
            startSafely(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
    }

    private fun openAutostart() {
        val opened = OemSettings.openAutostart(this)
        if (!opened) {
            toast("没找到厂商的自启动页，已打开应用详情。请在电池和自启动里允许本应用。")
        }
    }

    private fun startSafely(intent: Intent, toastOnFail: Boolean = true): Boolean {
        return try {
            startActivity(intent)
            true
        } catch (_: Exception) {
            if (toastOnFail) {
                toast("打不开系统设置，已改去应用详情。")
                OemSettings.openAppDetails(this)
            }
            false
        }
    }
}

private data class PermRow(
    val title: String,
    val hint: String,
    val ok: Boolean,
    val action: String,
    val onAction: () -> Unit,
    val secondary: String? = null,
    val onSecondary: (() -> Unit)? = null,
)

@Composable
private fun PermissionScreen(
    refreshKey: Int,
    advice: String,
    rows: List<PermRow>,
    doneLabel: String,
    onBack: () -> Unit,
    onDone: () -> Unit,
) {
    val colors = studyColors()
    val visibleRows = if (refreshKey >= 0) rows else emptyList()
    LiquidPage { backdrop ->
        Column(
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .windowInsetsPadding(WindowInsets.statusBars)
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
        ) {
            Spacer(Modifier.height(8.dp))
            GlassIconButton(onBack, backdrop) {
                Icon(StudyIcons.ChevronLeft, contentDescription = "返回", tint = colors.label)
            }
            Spacer(Modifier.height(12.dp))
            Text("权限", color = colors.label, fontSize = 40.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.6).sp)
            Spacer(Modifier.height(6.dp))
            Text(
                text = "可靠响铃需要下面几项。逐项打开，状态变成绿色后再用。导入之后，即使应用没打开，到点也会响。",
                color = colors.secondary,
                fontSize = 15.sp,
                lineHeight = 21.sp,
            )
            if (advice.isNotBlank()) {
                Spacer(Modifier.height(8.dp))
                Text(advice, color = colors.orange, fontSize = 14.sp, lineHeight = 20.sp)
            }
            Spacer(Modifier.height(18.dp))
            Column(
                Modifier
                    .fillMaxWidth()
                    .liquidGlass(backdrop, squircle(28.dp), colors.glass, blurRadius = 2.dp, refraction = 24.dp),
            ) {
                visibleRows.forEachIndexed { index, row ->
                    Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                row.title,
                                color = colors.label,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                            )
                            StatusPill(ok = row.ok, backdrop = backdrop)
                        }
                        Spacer(Modifier.height(4.dp))
                        Text(row.hint, color = colors.secondary, fontSize = 14.sp, lineHeight = 20.sp)
                        Spacer(Modifier.height(8.dp))
                        Row {
                            Text(
                                text = row.action,
                                color = colors.blue,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier
                                    .clickable(onClick = row.onAction)
                                    .padding(top = 4.dp, bottom = 4.dp, end = 16.dp),
                            )
                            if (row.secondary != null && row.onSecondary != null) {
                                Text(
                                    text = row.secondary,
                                    color = colors.blue,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier
                                        .clickable(onClick = row.onSecondary)
                                        .padding(vertical = 4.dp),
                                )
                            }
                        }
                    }
                    if (index != visibleRows.lastIndex) {
                        Box(
                            Modifier
                                .fillMaxWidth()
                                .padding(start = 16.dp)
                                .height(0.5.dp)
                                .background(colors.separator),
                        )
                    }
                }
            }
            Spacer(Modifier.height(22.dp))
            LiquidButton(
                onClick = onDone,
                backdrop = backdrop,
                modifier = Modifier.fillMaxWidth(),
                tint = colors.blue,
                height = 56.dp,
            ) {
                Text(doneLabel, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun StatusPill(ok: Boolean, backdrop: com.kyant.backdrop.Backdrop) {
    val colors = studyColors()
    val color = if (ok) colors.green else colors.red
    Text(
        text = if (ok) "已开启" else "未开启",
        color = color,
        fontSize = 13.sp,
        fontWeight = FontWeight.SemiBold,
        modifier = Modifier
            .liquidGlass(
                backdrop = backdrop,
                shape = Capsule(),
                surface = color.copy(alpha = 0.22f),
                blurRadius = 2.dp,
                refraction = 6.dp,
                chromatic = false,
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}
