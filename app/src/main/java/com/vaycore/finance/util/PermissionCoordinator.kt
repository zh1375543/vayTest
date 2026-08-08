package com.vaycore.finance.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.vaycore.finance.R
import com.vaycore.finance.base.BaseActivity
import com.vaycore.finance.ui.showConfirmDialog

enum class PermissionScenario {
    ONBOARDING,
    DEVICE_RISK,
}

/** Owns permission scenarios, requests, denial handling, and settings navigation. */
object PermissionCoordinator {

    private const val REQUEST_HISTORY = "runtime_permission_history"

    fun permissionsFor(scenario: PermissionScenario): Array<String> = when (scenario) {
        PermissionScenario.ONBOARDING -> arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.POST_NOTIFICATIONS,
        )

        PermissionScenario.DEVICE_RISK -> arrayOf(
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.READ_PHONE_STATE,
        )
    }

    fun hasAll(context: Context, scenario: PermissionScenario): Boolean =
        permissionsFor(scenario).all { hasPermission(context, it) }

    fun hasPermission(context: Context, permission: String): Boolean =
        !isRuntimePermissionSupported(permission) ||
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

    fun request(
        activity: BaseActivity<*>,
        scenario: PermissionScenario,
        onDenied: (isPermanentlyDenied: Boolean, permissions: List<String>) -> Unit = { _, _ -> },
        showSettingsGuide: Boolean = true,
        onGranted: (permissions: List<String>) -> Unit,
    ) = request(
        activity = activity,
        permissions = permissionsFor(scenario),
        onDenied = onDenied,
        showSettingsGuide = showSettingsGuide,
        onGranted = onGranted,
    )

    fun request(
        activity: BaseActivity<*>,
        permissions: Array<String>,
        onDenied: (isPermanentlyDenied: Boolean, permissions: List<String>) -> Unit = { _, _ -> },
        showSettingsGuide: Boolean = true,
        onGranted: (permissions: List<String>) -> Unit,
    ) {
        val requestedPermissions = permissions
            .filter(::isRuntimePermissionSupported)
            .distinct()
        val deniedBeforeRequest = requestedPermissions.filterNot { hasPermission(activity, it) }
        if (deniedBeforeRequest.isEmpty()) {
            onGranted(requestedPermissions)
            return
        }

        val history = activity.getSharedPreferences(REQUEST_HISTORY, Context.MODE_PRIVATE)
        val previouslyRequested = deniedBeforeRequest.associateWith { history.getBoolean(it, false) }
        history.edit().apply {
            deniedBeforeRequest.forEach { putBoolean(it, true) }
        }.apply()

        activity.launchRuntimePermissions(deniedBeforeRequest.toTypedArray()) { result ->
            val deniedPermissions = deniedBeforeRequest.filter { result[it] != true }
            if (deniedPermissions.isEmpty()) {
                onGranted(requestedPermissions)
                return@launchRuntimePermissions
            }

            val isPermanentlyDenied = deniedPermissions.any { permission ->
                previouslyRequested[permission] == true &&
                    !ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)
            }
            onDenied(isPermanentlyDenied, deniedPermissions)
            if (showSettingsGuide) showSettingsGuide(activity, deniedPermissions)
        }
    }

    fun permissionLabel(context: Context, permissionName: String?): String = when (permissionName) {
        Manifest.permission.READ_PHONE_STATE -> context.getString(R.string.dialog_permission_phone)
        Manifest.permission.READ_CALENDAR -> context.getString(R.string.dialog_permission_calendar)
        Manifest.permission.ACCESS_COARSE_LOCATION -> context.getString(R.string.dialog_permission_location)
        Manifest.permission.POST_NOTIFICATIONS -> context.getString(R.string.dialog_permission_notification)
        Manifest.permission.CAMERA -> context.getString(R.string.camera_str)
        else -> ""
    }

    fun openSystemSettings(activity: Activity, permissions: List<String>) {
        val intent = if (
            permissions.size == 1 &&
            permissions.first() == Manifest.permission.POST_NOTIFICATIONS &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
        ) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
            }
        } else {
            Intent(
                Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                Uri.fromParts("package", activity.packageName, null),
            )
        }
        activity.startActivity(intent)
    }

    private fun showSettingsGuide(activity: Activity, deniedPermissions: List<String>) {
        val labels = deniedPermissions.joinToString { permissionLabel(activity, it) }
        activity.showConfirmDialog(
            title = String.format(activity.getString(R.string.dialog_permission_title), labels),
            desc = "",
        ) {
            openSystemSettings(activity, deniedPermissions)
        }
    }

    private fun isRuntimePermissionSupported(permission: String): Boolean =
        permission != Manifest.permission.POST_NOTIFICATIONS ||
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU
}
