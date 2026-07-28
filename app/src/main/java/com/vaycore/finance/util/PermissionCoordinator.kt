package com.vaycore.finance.util

import android.Manifest
import android.app.Activity
import android.content.Context
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import com.hjq.permissions.permission.base.IPermission
import com.vaycore.finance.R
import com.vaycore.finance.ui.showConfirmDialog

enum class PermissionScenario {
    ONBOARDING,
    DEVICE_RISK,
}

/** Owns permission scenarios, requests, denial handling, and settings navigation. */
object PermissionCoordinator {

    fun permissionsFor(scenario: PermissionScenario): Array<IPermission> = when (scenario) {
        PermissionScenario.ONBOARDING -> arrayOf(
            PermissionLists.getAccessCoarseLocationPermission(),
            PermissionLists.getReadPhoneStatePermission(),
            PermissionLists.getPostNotificationsPermission(),
            PermissionLists.getReadSmsPermission(),
        )

        PermissionScenario.DEVICE_RISK -> arrayOf(
            PermissionLists.getAccessCoarseLocationPermission(),
            PermissionLists.getReadPhoneStatePermission(),
            PermissionLists.getReadSmsPermission(),
        )
    }

    fun hasAll(context: Context, scenario: PermissionScenario): Boolean =
        XXPermissions.isGrantedPermissions(context, permissionsFor(scenario))

    fun request(
        activity: Activity,
        scenario: PermissionScenario,
        onDenied: (isPermanentlyDenied: Boolean, permissions: List<IPermission?>) -> Unit = { _, _ -> },
        showSettingsGuide: Boolean = true,
        onGranted: (permissions: List<IPermission?>) -> Unit,
    ) = request(
        activity = activity,
        permissions = permissionsFor(scenario),
        onDenied = onDenied,
        showSettingsGuide = showSettingsGuide,
        onGranted = onGranted,
    )

    fun request(
        activity: Activity,
        permissions: Array<IPermission>,
        onDenied: (isPermanentlyDenied: Boolean, permissions: List<IPermission?>) -> Unit = { _, _ -> },
        showSettingsGuide: Boolean = true,
        onGranted: (permissions: List<IPermission?>) -> Unit,
    ) {
        XXPermissions.with(activity)
            .unchecked()
            .permissions(permissions)
            .request { grantedPermissions, deniedPermissions ->
                if (deniedPermissions.isEmpty()) {
                    onGranted(grantedPermissions)
                    return@request
                }

                val isPermanentlyDenied = XXPermissions.isDoNotAskAgainPermissions(activity, deniedPermissions)
                onDenied(isPermanentlyDenied, deniedPermissions)
                if (showSettingsGuide) showSettingsGuide(activity, deniedPermissions)
            }
    }

    fun permissionLabel(context: Context, permissionName: String?): String = when (permissionName) {
        Manifest.permission.READ_PHONE_STATE -> context.getString(R.string.dialog_permission_phone)
        Manifest.permission.READ_CALENDAR -> context.getString(R.string.dialog_permission_calendar)
        Manifest.permission.ACCESS_COARSE_LOCATION -> context.getString(R.string.dialog_permission_location)
        Manifest.permission.READ_SMS -> context.getString(R.string.dialog_permission_sms)
        Manifest.permission.POST_NOTIFICATIONS -> context.getString(R.string.dialog_permission_notification)
        Manifest.permission.CAMERA -> context.getString(R.string.camera_str)
        else -> ""
    }

    fun openSystemSettings(activity: Activity, permissions: List<IPermission?>) {
        XXPermissions.startPermissionActivity(activity, permissions)
    }

    private fun showSettingsGuide(activity: Activity, deniedPermissions: List<IPermission?>) {
        val labels = deniedPermissions.joinToString { permissionLabel(activity, it?.permissionName) }
        activity.showConfirmDialog(
            title = String.format(activity.getString(R.string.dialog_permission_title), labels),
            desc = "",
        ) {
            openSystemSettings(activity, deniedPermissions)
        }
    }
}
