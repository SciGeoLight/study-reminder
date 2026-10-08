package com.partner.studyreminder.ui

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log

object OemSettings {
    private data class Comp(val pkg: String, val cls: String)

    private val XIAOMI = listOf(
        Comp("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
        Comp("com.miui.securitycenter", "com.miui.powercenter.PowerSettings"),
    )
    private val HUAWEI = listOf(
        Comp("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
        Comp("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity"),
        Comp("com.huawei.systemmanager", "com.huawei.systemmanager.optimize.process.ProtectActivity"),
    )
    private val HONOR = listOf(
        Comp("com.hihonor.systemmanager", "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
        Comp("com.hihonor.systemmanager", "com.hihonor.systemmanager.optimize.process.ProtectActivity"),
        Comp("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
    )
    private val OPPO = listOf(
        Comp("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
        Comp("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
        Comp("com.oplus.safecenter", "com.oplus.safecenter.permission.startup.StartupAppListActivity"),
        Comp("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity"),
        Comp("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity"),
    )
    private val VIVO = listOf(
        Comp("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
        Comp("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
        Comp("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity"),
        Comp("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.PurviewTabActivity"),
    )
    private val SAMSUNG = listOf(
        Comp("com.samsung.android.lool", "com.samsung.android.sm.battery.ui.BatteryActivity"),
        Comp("com.samsung.android.sm", "com.samsung.android.sm.battery.ui.BatteryActivity"),
        Comp("com.samsung.android.sm", "com.samsung.android.sm.ui.battery.BatteryActivity"),
        Comp("com.samsung.android.lool", "com.samsung.android.sm.ui.battery.BatteryActivity"),
        Comp("com.samsung.android.sm_cn", "com.samsung.android.sm.ui.battery.BatteryActivity"),
    )

    fun advice(): String {
        val key = (Build.MANUFACTURER + " " + Build.BRAND).lowercase()
        return when {
            key.contains("xiaomi") || key.contains("redmi") || key.contains("poco") ->
                "小米澎湃 OS 3（Android 17）：自启动选允许，省电策略选无限制，并打开悬浮通知和锁屏通知。从最近任务划掉没关系，但不要点「强行停止」。"
            key.contains("honor") ->
                "荣耀：应用启动管理里打开允许自启动、允许关联启动、允许后台活动。"
            key.contains("huawei") ->
                "华为 / 鸿蒙：应用启动管理里关闭自动管理，改为允许自启动、允许关联启动、允许后台活动。"
            key.contains("oppo") || key.contains("realme") || key.contains("oneplus") ->
                "OPPO / 真我 / 一加：自启动允许，耗电保护选不限制，并允许后台弹出界面。"
            key.contains("vivo") || key.contains("iqoo") ->
                "vivo / iQOO：后台高耗电允许，自启动允许。权限里打开「后台弹出界面」。"
            key.contains("samsung") ->
                "三星：电池里把「学习提醒」设为不受限制，不要让睡眠应用冻结它。"
            else ->
                "国产手机请到系统设置里允许自启动，并把电池策略设为无限制。不要强行停止本应用。"
        }
    }

    /** @return true when a vendor page actually opened. */
    fun openAutostart(activity: Activity): Boolean {
        for (candidate in candidates()) {
            val intent = Intent().setComponent(ComponentName(candidate.pkg, candidate.cls))
            val resolved = activity.packageManager.resolveActivity(intent, 0) != null
            if (!resolved) continue
            try {
                activity.startActivity(intent)
                return true
            } catch (e: Exception) {
                Log.w("StudyReminder", "oem page failed ${candidate.pkg}", e)
            }
        }
        openAppDetails(activity)
        return false
    }

    fun openAppDetails(context: Context) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    }

    private fun candidates(): List<Comp> {
        val key = (Build.MANUFACTURER + " " + Build.BRAND).lowercase()
        val picked = mutableListOf<Comp>()
        if (key.contains("xiaomi") || key.contains("redmi") || key.contains("poco")) picked += XIAOMI
        if (key.contains("honor")) picked += HONOR
        if (key.contains("huawei")) picked += HUAWEI
        if (key.contains("oppo") || key.contains("realme") || key.contains("oneplus")) picked += OPPO
        if (key.contains("vivo") || key.contains("iqoo")) picked += VIVO
        if (key.contains("samsung")) picked += SAMSUNG
        if (picked.isEmpty()) {
            picked += XIAOMI + HUAWEI + HONOR + OPPO + VIVO + SAMSUNG
        }
        return picked
    }
}
