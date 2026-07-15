package com.vaycore.finance.util.runtime

import android.Manifest
import android.annotation.SuppressLint
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
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
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.text.TextUtils
import android.view.WindowManager
import androidx.core.app.ActivityCompat
import com.vaycore.finance.App
import java.io.BufferedReader
import java.io.File
import java.io.FileReader
import java.util.UUID
import kotlin.math.pow
import kotlin.math.sqrt

object DeviceHelper {

    private var cachedId: String? = null

    private fun getIMEI(slotId: Int): String? {
        try {
            val manager = App.appContext
                .getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            val method = manager.javaClass.getMethod("getImei", Int::class.javaPrimitiveType)
            val imei = method.invoke(manager, slotId) as String?
            return imei
        } catch (e: Exception) {
            return ""
        }
    }

    /**
     * Get unique device ID (persists across reinstall, with fallback)
     */
    fun getDeviceId(): String {
        cachedId?.let { return it }

        // 1. try ANDROID_ID (usually stable on Android 8+)
        val androidId = getIMEI(0) ?: getIMEI(1) ?: getAndroidID()

        if (androidId.isNotBlank()) {
            cachedId = androidId
            return androidId
        }

        // 2. use UUID stored in external shared private directory (survives uninstall)
        val uuid = readOrCreateUUID()
        cachedId = uuid
        return uuid
    }

    @SuppressLint("HardwareIds")
    fun getAndroidID(): String {
        val id = Settings.Secure.getString(
            App.appContext.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: return ""

        return if (id == "9774d56d682e549c") "" else id
    }

    /**
     * Try to read UUID from external file, generate and save if not found
     */
    private fun readOrCreateUUID(): String {
        // no permission needed, but lost on uninstall
        val mediaDir = File(App.appContext.filesDir, ".device_id.txt")
        if (!mediaDir.exists()) mediaDir.mkdirs()

        val file = File(mediaDir, ".device_id.txt")
        if (file.exists()) return file.readText()

        val uuid = UUID.randomUUID().toString()
        file.writeText(uuid)
        return uuid
    }

    fun getMcc(): String {
        val operator =
            (App.appContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager).networkOperator
        return if (!operator.isNullOrEmpty()) operator.substring(0, 3) else ""
    }

    private val telephonyManager: TelephonyManager?
        get() = App.appContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    /** Get baseband version */
    fun getBasebandVer(): String {
        return runCatching {
            val clazz = Class.forName("android.os.SystemProperties")
            val method = clazz.getMethod("get", String::class.java, String::class.java)
            method.invoke(null, "gsm.version.baseband", "") as String
        }.getOrDefault("")
    }

    /** Get screen resolution */
    fun getResolutions(): String {
        val wm = App.appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val point = Point()
        wm.defaultDisplay.getRealSize(point)
        return "${point.x}*${point.y}"
    }

    /** Get serial number */
    fun getSerialNumbers(): String {
        return runCatching {
            when {
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.P -> Build.getSerial()
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> Build.SERIAL
                else -> {
                    val clazz = Class.forName("android.os.SystemProperties")
                    val method = clazz.getMethod("get", String::class.java)
                    method.invoke(null, "ro.serialno") as String
                }
            }
        }.getOrDefault("")
    }

    /** Get screen size in inches */
    fun getScreenSizeInches(): String {
        val wm = App.appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val point = Point()
        wm.defaultDisplay.getRealSize(point)

        val dm = App.appContext.resources.displayMetrics

        val x = (point.x / dm.xdpi).toDouble().pow(2.0)
        val y = (point.y / dm.ydpi).toDouble().pow(2.0)

        val inches = sqrt(x + y)

        return inches.toString()
    }

    /** Get SIM card count */
    fun getSimCount(): Int {
        if (ActivityCompat.checkSelfPermission(
                App.appContext,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) return 0

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            SubscriptionManager.from(App.appContext).activeSubscriptionInfoCount
        } else {
            0
        }
    }

    /** Get SIM slot count */
    fun getPhoneSimCount(): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            telephonyManager?.phoneCount ?: 0
        } else {
            0
        }
    }

    /** Get SIM serial number */
    fun getSimSerialNumbers(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return ""

        return runCatching {
            telephonyManager?.simSerialNumber ?: ""
        }.getOrDefault("")
    }

    /** Get SIM country ISO */
    fun getSimCountryIso(): String {
        return telephonyManager?.simCountryIso ?: ""
    }

    /** Whether device is rooted */
    fun isRoot(): Int {
        return if (isDeviceRooted()) 1 else 0
    }

    fun isDeviceRooted(): Boolean {
        return checkRootMethod1() || checkRootMethod2()
    }

    private fun checkRootMethod1(): Boolean {
        return Build.TAGS?.contains("test-keys") == true
    }

    private fun checkRootMethod2(): Boolean {
        val paths = arrayOf(
            "/system/app/Superuser.apk",
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su"
        )
        return paths.any { File(it).exists() }
    }

    /** Whether running on emulator */
    fun isEmulator(): Int {

        val result =
            Build.FINGERPRINT.startsWith("generic") ||
                    Build.MODEL.contains("Emulator") ||
                    Build.MODEL.contains("Android SDK built for x86") ||
                    Build.MANUFACTURER.contains("Genymotion") ||
                    Build.BRAND.startsWith("generic")

        if (result) return 1

        val intent = Intent(Intent.ACTION_DIAL)
        intent.data = Uri.parse("tel:123456")

        val canDial = intent.resolveActivity(App.appContext.packageManager)

        return if (canDial == null) 1 else 0
    }

    /** Whether developer mode is enabled */
    fun isAppDebug(): Int {
        val enabled = Settings.Secure.getInt(
            App.appContext.contentResolver,
            Settings.Secure.ADB_ENABLED,
            0
        ) > 0

        return if (enabled) 1 else 0
    }

    /** Whether airplane mode is on */
    fun isAirplaneModeOn(): Int {
        val enabled = Settings.Global.getInt(
            App.appContext.contentResolver,
            Settings.Global.AIRPLANE_MODE_ON,
            0
        ) != 0

        return if (enabled) 1 else 0
    }

    /** Get phone ringer mode */
    fun getPhoneMode(): Int {
        val audio = App.appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        return audio.ringerMode
    }

    /** Get boot time */
    fun getBootTime(): Long {
        return System.currentTimeMillis() - SystemClock.elapsedRealtime()
    }

    /** Get screen brightness */
    fun getBrightness(): Int {
        return Settings.System.getInt(
            App.appContext.contentResolver,
            Settings.System.SCREEN_BRIGHTNESS,
            255
        )
    }

    /** Get screen density */
    fun getScreenDensity(): Float {
        return Resources.getSystem().displayMetrics.density
    }

    /**
     * DPI
     */
    fun getScreenDensityDpi(): Int {
        return Resources.getSystem().displayMetrics.densityDpi
    }

    /** Whether device is a tablet */
    fun isTabletDevice(): Int {

        val isTablet =
            App.appContext.resources.configuration.screenLayout and
                    Configuration.SCREENLAYOUT_SIZE_MASK >=
                    Configuration.SCREENLAYOUT_SIZE_LARGE

        return if (isTablet) 1 else 0
    }

    /** Get total memory */
    fun getTotalMemory(): Long {

        return runCatching {

            BufferedReader(FileReader("/proc/meminfo")).use { reader ->

                val line = reader.readLine()
                val array = line.split("\\s+".toRegex())

                array[1].toLong() * 1024
            }

        }.getOrDefault(0)
    }

    /** Get available memory */
    fun getAvailMemory(): Long {

        val am = App.appContext.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager

        val mi = ActivityManager.MemoryInfo()

        am.getMemoryInfo(mi)

        return mi.availMem
    }

    fun getScreenSizeOfDevice2(): Double {
        try {
            val dm = Resources.getSystem().displayMetrics

            val width = dm.widthPixels / dm.xdpi
            val height = dm.heightPixels / dm.ydpi

            return sqrt((width * width + height * height).toDouble())
        } catch (e: Exception) {
            return 0.0
        }
    }

    fun getFsTotalSize(anyPathInFs: String?): Long {
        if (TextUtils.isEmpty(anyPathInFs)) return 0
        val statFs = StatFs(anyPathInFs)
        val blockSize: Long = statFs.blockSizeLong
        val totalSize: Long = statFs.blockCountLong
        return blockSize * totalSize
    }
    fun getInternalTotalSize(): Long {
        return getFsTotalSize(Environment.getDataDirectory().absolutePath)
    }

    fun getInternalAvailableSize(): Long {
        return getFsAvailableSize(Environment.getDataDirectory().absolutePath)
    }

    fun getFsAvailableSize(anyPathInFs: String?): Long {
        if (TextUtils.isEmpty(anyPathInFs)) return 0
        val statFs = StatFs(anyPathInFs)
        val blockSize: Long = statFs.blockSizeLong
        val availableSize: Long = statFs.availableBlocksLong
        return blockSize * availableSize
    }

    fun getPhoneNumber(): String? {
        return try {
            val tm = App.appContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
            // requires READ_PHONE_STATE permission in AndroidManifest
            // may return phone number directly on Android 10 and below
            tm.line1Number
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}