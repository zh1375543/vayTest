package com.vaycore.finance.util.deivce

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/** Collects battery and CPU data for the system information payload. */
object BatteryCpuCollector {

    fun collectBattery(context: Context): JSONObject {
        val batteryManager = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
        return getBatteryStatusJson(context, batteryManager).apply {
            put("propertyCapacity", batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY))
            put("propertyCurrentAverage", batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_AVERAGE))
            put("propertyChargeCounter", batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER))
            put("propertyCurrentNow", batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW))
            put(
                "propertyEnergyCounter",
                batteryManager.getLongProperty(BatteryManager.BATTERY_PROPERTY_ENERGY_COUNTER).toString()
            )
        }
    }

    fun collectCpu(): JSONObject {
        val cpuCore = JSONObject().apply {
            put("name", getCpuName().orEmpty())
            put("cpuCurrentFreq", getCurrentCpuFreq().toIntOrNull() ?: 0)
            put("maximumFreq", getMaxCpuFreq().toIntOrNull() ?: 0)
            put("minimumFreq", getMinCpuFreq().toIntOrNull() ?: 0)
        }
        return JSONObject().apply {
            put("supportedAbis", JSONArray(Build.SUPPORTED_ABIS.toList()))
            put("coreCount", getCoreCount())
            put("cores", JSONArray().put(cpuCore))
        }
    }

    private fun getBatteryStatusJson(
        context: Context,
        batteryManager: BatteryManager
    ): JSONObject = JSONObject().apply {
        put(
            "battery_pct",
            batteryManager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).toDouble()
        )

        context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))?.let { intent ->
            val status = intent.getIntExtra(
                BatteryManager.EXTRA_STATUS,
                BatteryManager.BATTERY_STATUS_UNKNOWN
            )
            put("is_charging", status)

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

        put("screen_brightness", DeviceEnvironmentReader.getBrightness().toDouble())
    }

    private fun getCpuName(): String? = try {
        File("/proc/cpuinfo").useLines { lines ->
            val line = lines.firstOrNull() ?: return null
            line.split(":\\s+".toRegex(), 2).getOrNull(1)
        }
    } catch (_: Exception) {
        null
    }

    private fun getMaxCpuFreq(): String =
        readFrequency("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_max_freq")

    private fun getMinCpuFreq(): String =
        readFrequency("/sys/devices/system/cpu/cpu0/cpufreq/cpuinfo_min_freq")

    private fun getCurrentCpuFreq(): String =
        readFrequency("/sys/devices/system/cpu/cpu0/cpufreq/scaling_cur_freq")

    private fun getCoreCount(): Int = try {
        val tokens = File("/sys/devices/system/cpu/present").readText().trim().split("-")
        if (tokens.size >= 2) {
            tokens[1].toInt() - tokens[0].toInt() + 1
        } else {
            1
        }
    } catch (_: Exception) {
        1
    }

    private fun readFrequency(path: String): String = try {
        File(path).readText().trim()
    } catch (_: Exception) {
        "N/A"
    }
}
