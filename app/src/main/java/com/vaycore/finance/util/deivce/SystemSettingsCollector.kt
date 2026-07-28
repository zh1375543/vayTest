package com.vaycore.finance.util.deivce

import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import com.vaycore.finance.app.App
import org.json.JSONArray
import org.json.JSONObject

/** Collects system, display, and storage settings for the system information payload. */
object SystemSettingsCollector {

    fun collect(context: Context): JSONObject = JSONObject().apply {
        val configuration = Resources.getSystem().configuration
        val language = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            configuration.locales[0]
        } else {
            @Suppress("DEPRECATION")
            configuration.locale
        }
        val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
        val metrics = DisplayMetrics().apply {
            @Suppress("DEPRECATION")
            windowManager.defaultDisplay.getMetrics(this)
        }

        put("apiLevel", Build.VERSION.SDK_INT.toString())
        put("bootloader", Build.BOOTLOADER)
        put("codeName", Build.VERSION.CODENAME)
        put("currentSystemTime", System.currentTimeMillis().toString())
        put("deviceId", DeviceIdentityReader.getDeviceId())
        put("imei", DeviceIdentityReader.getDeviceId())
        put("isEmulator", DeviceEnvironmentReader.isEmulator() == 1)
        put("isRooted", DeviceEnvironmentReader.isDeviceRooted())
        put("isTabletDevice", DeviceEnvironmentReader.isTabletDevice() == 1)
        put("language", language.language)
        put("languageTag", language.displayLanguage)
        put("isO3language", language.isO3Language)
        put("kernelVersion", getLinuxKernel())
        put("manufacturer", Build.MANUFACTURER)
        put("model", Build.MODEL)
        put("packageName", context.packageName)
        put("product", Build.PRODUCT)
        put("releasedWith", Build.VERSION.RELEASE ?: "unknown")
        put("securityPatchLevel", Build.VERSION.SECURITY_PATCH)
        put("serial", DeviceIdentityReader.getSerialNumbers())
        put("manufacturingDay", Build.TIME.toString())
        put("disk", collectDisk())
        put("display", collectDisplay(windowManager, metrics))
        put("jvm", JSONObject())
        put("ram", JSONObject())
        put("sensors", "unknown")
    }

    private fun collectDisplay(windowManager: WindowManager, metrics: DisplayMetrics): JSONObject =
        JSONObject().apply {
            @Suppress("DEPRECATION")
            val display = windowManager.defaultDisplay
            put("brightness", DeviceEnvironmentReader.getBrightness().toDouble())
            put("density", DeviceEnvironmentReader.getScreenDensity())
            put("densityDpi", DeviceEnvironmentReader.getScreenDensityDpi())
            put("displayId", "unknown")
            put("heightPixels", metrics.heightPixels)
            put("widthPixels", metrics.widthPixels)
            put("offTimeout", getScreenOffTimeout())
            put("refreshRate", display.refreshRate.toInt())
            put("scaledDensity", metrics.scaledDensity)
            put("xdpi", metrics.xdpi.toString())
            put("ydpi", metrics.ydpi.toString())
            put("hdrsupportedTyps", JSONArray())
        }

    private fun collectDisk(): JSONObject = JSONObject().apply {
        val isSdPresent = try {
            android.os.Environment.getExternalStorageState() == android.os.Environment.MEDIA_MOUNTED
        } catch (_: Exception) {
            false
        }
        put("isSdPresent", isSdPresent)
        put("blockCount", JSONObject.NULL)
        put("blockSize", JSONObject.NULL)
        put("availCount", JSONObject.NULL)
    }

    private fun getScreenOffTimeout(): Int = try {
        Settings.System.getInt(App.appContext.contentResolver, Settings.System.SCREEN_OFF_TIMEOUT)
    } catch (_: Exception) {
        0
    }

    private fun getLinuxKernel(): String = try {
        Runtime.getRuntime()
            .exec("cat /proc/version")
            .inputStream
            .bufferedReader()
            .readLine()
            ?.substringAfter("version ")
            ?.substringBefore(" ")
            ?: ""
    } catch (_: Exception) {
        ""
    }
}
