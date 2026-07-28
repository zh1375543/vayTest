package com.vaycore.finance.util.deivce

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.Point
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.StatFs
import android.os.SystemClock
import android.provider.Settings
import android.text.TextUtils
import android.view.WindowManager
import com.vaycore.finance.app.App
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import kotlin.math.pow
import kotlin.math.sqrt

/** Reads device integrity, display, memory, and storage information. */
object DeviceEnvironmentReader {

    fun isRoot(): Int = if (isDeviceRooted()) 1 else 0

    fun isDeviceRooted(): Boolean = hasTestKeys() || hasSuBinary()

    /** Reports whether the current device exhibits emulator characteristics. */
    fun isEmulator(): Int {
        val isEmulator =
            Build.FINGERPRINT.startsWith("generic") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for x86") ||
                Build.MANUFACTURER.contains("Genymotion") ||
                Build.BRAND.startsWith("generic")
        if (isEmulator) return 1

        val intent = Intent(Intent.ACTION_DIAL).apply { data = Uri.parse("tel:123456") }
        return if (intent.resolveActivity(App.appContext.packageManager) == null) 1 else 0
    }

    fun isAppDebug(): Int {
        val enabled = Settings.Secure.getInt(
            App.appContext.contentResolver,
            Settings.Secure.ADB_ENABLED,
            0
        ) > 0
        return if (enabled) 1 else 0
    }

    fun isAirplaneModeOn(): Int {
        val enabled = Settings.Global.getInt(
            App.appContext.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON,
            0
        ) != 0
        return if (enabled) 1 else 0
    }

    fun getPhoneMode(): Int {
        val audio = App.appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audio.ringerMode
    }

    fun getBootTime(): Long = System.currentTimeMillis() - SystemClock.elapsedRealtime()

    fun getResolutions(): String {
        val point = Point()
        @Suppress("DEPRECATION")
        (App.appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            .defaultDisplay
            .getRealSize(point)
        return "${point.x}*${point.y}"
    }

    fun getScreenSizeInches(): String {
        val point = Point()
        @Suppress("DEPRECATION")
        (App.appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager)
            .defaultDisplay
            .getRealSize(point)

        val metrics = App.appContext.resources.displayMetrics
        val x = (point.x / metrics.xdpi).toDouble().pow(2.0)
        val y = (point.y / metrics.ydpi).toDouble().pow(2.0)
        return sqrt(x + y).toString()
    }

    fun getBrightness(): Int = Settings.System.getInt(
        App.appContext.contentResolver,
        Settings.System.SCREEN_BRIGHTNESS,
        255
    )

    fun getScreenDensity(): Float = Resources.getSystem().displayMetrics.density

    fun getScreenDensityDpi(): Int = Resources.getSystem().displayMetrics.densityDpi

    fun isTabletDevice(): Int {
        val isTablet = App.appContext.resources.configuration.screenLayout and
            Configuration.SCREENLAYOUT_SIZE_MASK >= Configuration.SCREENLAYOUT_SIZE_LARGE
        return if (isTablet) 1 else 0
    }

    fun getScreenSizeOfDevice2(): Double = try {
        val metrics = Resources.getSystem().displayMetrics
        val width = metrics.widthPixels / metrics.xdpi
        val height = metrics.heightPixels / metrics.ydpi
        sqrt((width * width + height * height).toDouble())
    } catch (_: Exception) {
        0.0
    }

    fun getTotalMemory(): Long = runCatching {
        BufferedReader(FileReader("/proc/meminfo")).use { reader ->
            reader.readLine().split("\\s+".toRegex())[1].toLong() * 1024
        }
    }.getOrDefault(0)

    fun getAvailMemory(): Long {
        val activityManager = App.appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return ActivityManager.MemoryInfo().also(activityManager::getMemoryInfo).availMem
    }

    fun getFsTotalSize(anyPathInFs: String?): Long {
        if (TextUtils.isEmpty(anyPathInFs)) return 0
        val statFs = StatFs(anyPathInFs)
        return statFs.blockSizeLong * statFs.blockCountLong
    }

    fun getInternalTotalSize(): Long = getFsTotalSize(Environment.getDataDirectory().absolutePath)

    fun getInternalAvailableSize(): Long = getFsAvailableSize(Environment.getDataDirectory().absolutePath)

    fun getFsAvailableSize(anyPathInFs: String?): Long {
        if (TextUtils.isEmpty(anyPathInFs)) return 0
        val statFs = StatFs(anyPathInFs)
        return statFs.blockSizeLong * statFs.availableBlocksLong
    }

    private fun hasTestKeys(): Boolean = Build.TAGS?.contains("test-keys") == true

    private fun hasSuBinary(): Boolean = arrayOf(
        "/system/app/Superuser.apk",
        "/system/bin/su",
        "/system/xbin/su",
        "/sbin/su"
    ).any { File(it).exists() }
}
