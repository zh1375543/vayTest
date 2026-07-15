package com.vaycore.finance.util.runtime

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.res.Resources
import android.os.BatteryManager
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.WindowManager
import com.vaycore.finance.App
import com.vaycore.finance.data.local.gaId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

object SystemInfoHelper {

    private var cachedSystemInfoJson: JSONObject? = null

    suspend fun getSystemInfoJson(): JSONObject =
        cachedSystemInfoJson ?: withContext(Dispatchers.IO) {
            val appContext = App.appContext
            val configuration = Resources.getSystem().configuration
            val language = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                configuration.locales[0]
            } else {
                configuration.locale
            }
            val wm = appContext.getSystemService(Context.WINDOW_SERVICE) as WindowManager
            val metrics = DisplayMetrics().apply { wm.defaultDisplay.getMetrics(this) }

            val batteryManager = appContext.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            val batteryBase = getBatteryStatusJson()
            val battery = JSONObject(batteryBase.toString()).apply {
                put("propertyCapacity", batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY))
                put("propertyCurrentAverage", batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE))
                put("propertyChargeCounter", batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER))
                put("propertyCurrentNow", batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW))
                put("propertyEnergyCounter", batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER).toString())
            }

            val cpuCore = JSONObject().apply {
                put("name", getCpuName() ?: "")
                put("cpuCurrentFreq", getCurCpuFreq().toIntOrNull() ?: 0)
                put("maximumFreq", getMaxCpuFreq().toIntOrNull() ?: 0)
                put("minimumFreq", getMinCpuFreq().toIntOrNull() ?: 0)
            }
            val cpu = JSONObject().apply {
                put("supportedAbis", JSONArray(Build.SUPPORTED_ABIS.toList()))
                put("coreCount", getNumCores())
                put("cores", JSONArray().apply { put(cpuCore) })
            }

            val display = JSONObject().apply {
                put("brightness", DeviceHelper.getBrightness().toDouble())
                put("density", DeviceHelper.getScreenDensity())
                put("densityDpi", DeviceHelper.getScreenDensityDpi())
                put("displayId", "unknown")
                put("heightPixels", metrics.heightPixels)
                put("widthPixels", metrics.widthPixels)
                put("offTimeout", getScreenOffTimeout())
                put("refreshRate", wm.defaultDisplay.refreshRate.toInt())
                put("scaledDensity", metrics.scaledDensity)
                put("xdpi", metrics.xdpi.toString())
                put("ydpi", metrics.ydpi.toString())
                put("hdrsupportedTyps", JSONArray())
            }

            val disk = JSONObject().apply {
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

            val ram = JSONObject() // fill as needed
            val jvm = JSONObject() // fill as needed

            val root = JSONObject().apply {
                put("adid", gaId)
                put("apiLevel", Build.VERSION.SDK_INT.toString())
                put("bootloader", Build.BOOTLOADER)
                put("codeName", Build.VERSION.CODENAME)
                put("currentSystemTime", System.currentTimeMillis().toString())
                put("deviceId", DeviceHelper.getDeviceId())
                put("imei", DeviceHelper.getDeviceId())
                put("isEmulator", DeviceHelper.isEmulator() == 1)
                put("isRooted", DeviceHelper.isDeviceRooted())
                put("isTabletDevice", DeviceHelper.isTabletDevice() == 1)
                put("language", language.language)
                put("languageTag", language.displayLanguage)
                put("isO3language", language.isO3Language)
                put("kernelVersion", getLinuxKernel())
                put("manufacturer", Build.MANUFACTURER)
                put("model", Build.MODEL)
                put("packageName", appContext.packageName)
                put("product", Build.PRODUCT)
                put("releasedWith", Build.VERSION.RELEASE ?: "unknown")
                put("securityPatchLevel", Build.VERSION.SECURITY_PATCH)
                put("serial", DeviceHelper.getSerialNumbers())
                put("manufacturingDay", Build.TIME.toString())
                put("battery", battery)
                put("cpu", cpu)
                put("disk", disk)
                put("display", display)
                put("jvm", jvm)
                put("ram", ram)
                put("sensors", "unknown")
            }

            cachedSystemInfoJson = root
            root
        }

    private fun getScreenOffTimeout(): Int {
        return try {
            Settings.System.getInt(
                App.appContext.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT
            )
        } catch (_: Exception) {
            0
        }
    }

    private fun getBatteryStatusJson(): JSONObject {
        val batteryManager = App.appContext.getSystemService(
            Context.BATTERY_SERVICE
        ) as BatteryManager
        return JSONObject().apply {
            put(
                "battery_pct",
                batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).toDouble()
            )

            App.appContext.registerReceiver(
                null,
                IntentFilter(Intent.ACTION_BATTERY_CHANGED)
            )?.let { intent ->
                val status = intent.getIntExtra(
                    BatteryManager.EXTRA_STATUS,
                    BatteryManager.BATTERY_STATUS_UNKNOWN
                )
                put(
                    "is_charging",
                    when (status) {
                        BatteryManager.BATTERY_STATUS_CHARGING -> BatteryManager.BATTERY_STATUS_CHARGING
                        BatteryManager.BATTERY_STATUS_DISCHARGING -> BatteryManager.BATTERY_STATUS_DISCHARGING
                        BatteryManager.BATTERY_STATUS_NOT_CHARGING -> BatteryManager.BATTERY_STATUS_NOT_CHARGING
                        BatteryManager.BATTERY_STATUS_FULL -> BatteryManager.BATTERY_STATUS_FULL
                        else -> BatteryManager.BATTERY_STATUS_UNKNOWN
                    }
                )
                val health = intent.getIntExtra(
                    BatteryManager.EXTRA_HEALTH,
                    BatteryManager.BATTERY_HEALTH_UNKNOWN
                )
                put(
                    "battery_health",
                    when (health) {
                        BatteryManager.BATTERY_HEALTH_GOOD -> BatteryManager.BATTERY_HEALTH_GOOD
                        BatteryManager.BATTERY_HEALTH_DEAD -> BatteryManager.BATTERY_HEALTH_DEAD
                        BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE -> BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE
                        BatteryManager.BATTERY_HEALTH_OVERHEAT -> BatteryManager.BATTERY_HEALTH_OVERHEAT
                        else -> BatteryManager.BATTERY_HEALTH_UNKNOWN
                    }
                )
                val plugType = intent.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0)
                put(
                    "charge_type",
                    when (plugType) {
                        BatteryManager.BATTERY_PLUGGED_AC -> BatteryManager.BATTERY_PLUGGED_AC
                        BatteryManager.BATTERY_PLUGGED_USB -> BatteryManager.BATTERY_PLUGGED_USB
                        BatteryManager.BATTERY_PLUGGED_WIRELESS -> BatteryManager.BATTERY_PLUGGED_WIRELESS
                        else -> 0
                    }
                )
                put("battery_temperature", intent.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0))
            }

            put("screen_brightness", DeviceHelper.getBrightness().toDouble())
        }
    }

    private fun getLinuxKernel(): String {
        return try {
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

    private fun getCpuName(): String? {
        return try {
            File("/proc/cpuinfo")
                .useLines { lines ->
                    val line = lines.firstOrNull() ?: return null
                    line.split(":\\s+".toRegex(), 2).getOrNull(1)
                }
        } catch (_: Exception) {
            null
        }
    }

    /** Get CPU max frequency */
    private fun getMaxCpuFreq(): String {
        return readFreq("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")
    }

    /** Get CPU min frequency */
    private fun getMinCpuFreq(): String {
        return readFreq("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq")
    }

    /** Get current CPU frequency */
    private fun getCurCpuFreq(): String {
        return readFreq("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq")
    }

    /** Get CPU core count */
    private fun getNumCores(): Int {
        return try {
            val line = File("/sys/devices/system/cpu/present").readText().trim()
            val tokens = line.split("-")
            if (tokens.size >= 2) {
                val start = tokens[0].toInt()
                val end = tokens[1].toInt()
                end - start + 1
            } else {
                1
            }
        } catch (_: Exception) {
            1
        }
    }

    /** Read CPU frequency file */
    private fun readFreq(path: String): String {
        return try {
            File(path).readText().trim()
        } catch (_: Exception) {
            "N/A"
        }
    }
}
