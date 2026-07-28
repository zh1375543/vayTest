package com.vaycore.finance.util.deivce

import com.vaycore.finance.app.App
import com.vaycore.finance.data.gaId
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

/** Builds and caches the system information payload from focused collectors. */
object SystemInfoHelper {

    @Volatile
    private var cachedSystemInfoJson: JSONObject? = null

    suspend fun getSystemInfoJson(): JSONObject =
        cachedSystemInfoJson ?: withContext(Dispatchers.IO) {
            synchronized(this@SystemInfoHelper) {
                cachedSystemInfoJson ?: buildSystemInfo().also { cachedSystemInfoJson = it }
            }
        }

    private fun buildSystemInfo(): JSONObject {
        val context = App.appContext
        return SystemSettingsCollector.collect(context).apply {
            put("adid", gaId)
            put("battery", BatteryCpuCollector.collectBattery(context))
            put("cpu", BatteryCpuCollector.collectCpu())
        }
    }
}
