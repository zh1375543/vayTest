package com.vaycore.finance.util.deivce

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import androidx.core.app.ActivityCompat
import com.vaycore.finance.app.App
import java.io.File
import java.util.UUID

/** Reads stable device identifiers and telephony or SIM information. */
object DeviceIdentityReader {

    private var cachedId: String? = null

    private val telephonyManager: TelephonyManager?
        get() = App.appContext.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager

    private fun getImei(slotId: Int): String? = try {
        val manager = App.appContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        val method = manager.javaClass.getMethod("getImei", Int::class.javaPrimitiveType)
        method.invoke(manager, slotId) as String?
    } catch (_: Exception) {
        ""
    }

    /** Gets a persisted unique device ID with an Android ID fallback. */
    fun getDeviceId(): String {
        cachedId?.let { return it }

        val androidId = getImei(0) ?: getImei(1) ?: getAndroidID()
        if (androidId.isNotBlank()) {
            cachedId = androidId
            return androidId
        }

        return readOrCreateUuid().also { cachedId = it }
    }

    @SuppressLint("HardwareIds")
    fun getAndroidID(): String {
        val id = Settings.Secure.getString(
            App.appContext.contentResolver,
            Settings.Secure.ANDROID_ID
        ) ?: return ""
        return if (id == "9774d56d682e549c") "" else id
    }

    fun getMcc(): String {
        val operator = (App.appContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager)
            .networkOperator
        return if (!operator.isNullOrEmpty()) operator.substring(0, 3) else ""
    }

    /** Gets the baseband version reported by the device. */
    fun getBasebandVer(): String = runCatching {
        val clazz = Class.forName("android.os.SystemProperties")
        val method = clazz.getMethod("get", String::class.java, String::class.java)
        method.invoke(null, "gsm.version.baseband", "") as String
    }.getOrDefault("")

    /** Gets the hardware serial number when the platform allows access. */
    fun getSerialNumbers(): String = runCatching {
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

    fun getPhoneSimCount(): Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        telephonyManager?.phoneCount ?: 0
    } else {
        0
    }

    fun getSimSerialNumbers(): String {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) return ""
        return runCatching { telephonyManager?.simSerialNumber ?: "" }.getOrDefault("")
    }

    fun getSimCountryIso(): String = telephonyManager?.simCountryIso ?: ""

    fun getPhoneNumber(): String? = try {
        val manager = App.appContext.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
        manager.line1Number
    } catch (e: Exception) {
        e.printStackTrace()
        null
    }

    private fun readOrCreateUuid(): String {
        val mediaDir = File(App.appContext.filesDir, ".device_id.txt")
        if (!mediaDir.exists()) mediaDir.mkdirs()

        val file = File(mediaDir, ".device_id.txt")
        if (file.exists()) return file.readText()

        return UUID.randomUUID().toString().also(file::writeText)
    }
}
