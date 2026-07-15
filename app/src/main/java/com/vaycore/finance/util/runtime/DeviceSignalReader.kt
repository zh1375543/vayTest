package com.vaycore.finance.util.runtime

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.ScanResult
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import android.telephony.CellInfoGsm
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.text.format.Formatter
import androidx.core.app.ActivityCompat
import com.vaycore.finance.App
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

/** Collects device signals that do not require a long-lived Android callback. */
object DeviceSignalReader {

    private val packageManager: PackageManager = App.appContext.packageManager

    fun getCurrentAppInstalledTime(): Long = try {
        packageManager.getPackageInfo(App.appContext.packageName, 0).firstInstallTime
    } catch (_: PackageManager.NameNotFoundException) {
        -1L
    }

    @SuppressLint("QueryPermissionsNeeded")
    fun getAppListData(): JSONArray {
        val result = JSONArray()
        val launcherIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }
        val seenPackages = HashSet<String>()

        packageManager.queryIntentActivities(launcherIntent, 0).forEach { resolveInfo ->
            val packageName = resolveInfo.activityInfo.packageName
            if (!seenPackages.add(packageName)) return@forEach

            runCatching {
                val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    packageManager.getPackageInfo(
                        packageName,
                        PackageManager.PackageInfoFlags.of(0)
                    )
                } else {
                    packageManager.getPackageInfo(packageName, 0)
                }
                val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    packageInfo.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    packageInfo.versionCode.toLong()
                }
                val flags = resolveInfo.activityInfo.applicationInfo.flags
                val isSystem = if ((flags and ApplicationInfo.FLAG_SYSTEM) != 0) 1 else 0

                result.put(JSONObject().apply {
                    put("name", resolveInfo.loadLabel(packageManager).toString())
                    put("appId", packageName)
                    put("installTime", (packageInfo.firstInstallTime / 1000L).toString())
                    put("upgradeTime", (packageInfo.lastUpdateTime / 1000L).toString())
                    put("appVersion", packageInfo.versionName ?: "")
                    put("isSystem", isSystem)
                    put("versionCode", versionCode.toString())
                    put("flags", flags.toString())
                    put("appType", isSystem)
                })
            }
        }
        return result
    }

    @SuppressLint("MissingPermission")
    suspend fun getNetworkInfo(): JSONObject = withContext(Dispatchers.IO) {
        JSONObject().apply {
            runCatching {
                val connectivityManager = App.appContext.getSystemService(
                    Context.CONNECTIVITY_SERVICE
                ) as ConnectivityManager
                val wifiManager = App.appContext.applicationContext.getSystemService(
                    Context.WIFI_SERVICE
                ) as WifiManager
                val network = connectivityManager.activeNetwork
                val capabilities = connectivityManager.getNetworkCapabilities(network)

                put("isWifiConnected", capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true)
                put("isVpn", capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true)

                val wifiInfo = wifiManager.connectionInfo
                put("ssid", wifiInfo?.ssid?.replace("\"", ""))
                put("bssid", wifiInfo?.bssid)
                put("ipAddress", Formatter.formatIpAddress(wifiInfo?.ipAddress ?: 0))
                put("macAddress", wifiInfo?.macAddress)
                put("type", when {
                    capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true -> "WIFI"
                    capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true -> "MOBILE"
                    else -> "UNKNOWN"
                })

                val linkProperties = connectivityManager.getLinkProperties(network)
                val dnsServers = linkProperties?.dnsServers
                put("dns1", dnsServers?.getOrNull(0)?.hostAddress)
                put("dns2", dnsServers?.getOrNull(1)?.hostAddress)
                put("gateway", linkProperties?.routes?.firstOrNull { it.gateway != null }?.gateway?.hostAddress)
                put("subnetMask", linkProperties?.linkAddresses?.firstOrNull()?.prefixLength?.toString())
                put("proxy", linkProperties?.httpProxy?.host)

                put("wifis", JSONArray().apply {
                    if (wifiManager.isWifiEnabled) {
                        wifiManager.scanResults.forEach { wifi: ScanResult ->
                            put(JSONObject().apply {
                                put("ssid", wifi.SSID)
                                put("bssid", wifi.BSSID)
                                put("capabilities", wifi.capabilities)
                                put("level", wifi.level)
                            })
                        }
                    }
                })
            }
        }
    }

    fun getSimCardInfo(): JSONObject = JSONObject().apply {
        runCatching {
            val gsm = getFirstGsmCell()
            val simCards = getSimCardsJson()
            put("phoneNumber", getPhoneNumber())
            put("isExists", getSimCardCount() > 0)
            put("gsmLac", gsm?.first?.toString())
            put("gsmCid", gsm?.second?.toString())
            put("simOperator", telephonyManager().simOperator)
            put("simOperatorName", telephonyManager().simOperatorName)
            put("cardSlotCount", getSimCardCount())
            put("simCards", simCards)
        }
    }

    fun getBluetoothInfo(): JSONObject = JSONObject().apply {
        put("mode", getBluetoothMode().toString())
        put("isEnabled", bluetoothAdapter()?.isEnabled ?: false)
        put("name", getBluetoothName())
        put("address", getBluetoothMac())
        put("state", getBluetoothStateDescription())
    }

    private fun hasPermission(permission: String): Boolean = ActivityCompat.checkSelfPermission(
        App.appContext,
        permission
    ) == PackageManager.PERMISSION_GRANTED

    @SuppressLint("MissingPermission")
    private fun getSimCardCount(): Int {
        if (!hasPermission(Manifest.permission.READ_PHONE_STATE)) return 0
        return runCatching {
            SubscriptionManager.from(App.appContext).activeSubscriptionInfoCount
        }.getOrDefault(0)
    }

    @SuppressLint("MissingPermission")
    private fun getSimCardsJson(): JSONArray {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1 || !hasPermission(Manifest.permission.READ_PHONE_STATE)) {
            return JSONArray()
        }
        val subscriptions = SubscriptionManager.from(App.appContext).activeSubscriptionInfoList
            ?: return JSONArray()
        val telephonyManager = telephonyManager()
        return JSONArray().apply {
            subscriptions.forEach { subscription ->
                put(JSONObject().apply {
                    put("cardSlotNum", subscription.simSlotIndex)
                    put("number", subscription.number)
                    put("operator", subscription.carrierName?.toString())
                    put("imsi", subscription.subscriptionId.toString())
                    put("imei", JSONObject.NULL)
                    put("isReady", telephonyManager.simState == TelephonyManager.SIM_STATE_READY)
                })
            }
        }
    }

    @SuppressLint("HardwareIds", "MissingPermission")
    private fun getPhoneNumber(): String? {
        if (!hasPermission(Manifest.permission.READ_PHONE_STATE)) return null
        return telephonyManager().line1Number
    }

    @SuppressLint("MissingPermission")
    private fun getFirstGsmCell(): Pair<Int, Int>? {
        if (!hasPermission(Manifest.permission.ACCESS_FINE_LOCATION)) return null
        return telephonyManager().allCellInfo
            ?.filterIsInstance<CellInfoGsm>()
            ?.firstOrNull()
            ?.cellIdentity
            ?.let { Pair(it.lac, it.cid) }
    }

    private fun telephonyManager(): TelephonyManager = App.appContext.getSystemService(
        Context.TELEPHONY_SERVICE
    ) as TelephonyManager

    private fun bluetoothAdapter(): BluetoothAdapter? = (App.appContext.getSystemService(
        Context.BLUETOOTH_SERVICE
    ) as? BluetoothManager)?.adapter

    private fun getBluetoothName(): String {
        val adapter = bluetoothAdapter() ?: return ""
        return if (!hasPermission(Manifest.permission.BLUETOOTH_CONNECT)) {
            ""
        } else {
            @SuppressLint("MissingPermission")
            adapter.name ?: ""
        }
    }

    private fun getBluetoothMode(): Int {
        val adapter = bluetoothAdapter() ?: return -1
        return if (!hasPermission(Manifest.permission.BLUETOOTH_SCAN)) {
            -1
        } else {
            @SuppressLint("MissingPermission")
            adapter.scanMode
        }
    }

    private fun getBluetoothStateDescription(): String = when (bluetoothAdapter()?.state ?: BluetoothAdapter.STATE_OFF) {
        BluetoothAdapter.STATE_OFF -> "off"
        BluetoothAdapter.STATE_ON -> "on"
        BluetoothAdapter.STATE_TURNING_OFF -> "turning_on"
        BluetoothAdapter.STATE_TURNING_ON -> "turning_off"
        else -> "unknow"
    }

    private fun getBluetoothMac(): String = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        Settings.Secure.getString(App.appContext.contentResolver, "bluetooth_address") ?: ""
    } else {
        ""
    }
}
