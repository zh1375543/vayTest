package com.vaycore.finance.util

import android.app.Activity
import com.hjq.permissions.XXPermissions
import com.hjq.permissions.permission.PermissionLists
import com.hjq.permissions.permission.base.IPermission
import com.vaycore.finance.R
import com.vaycore.finance.ui.showConfirmDialog

fun Activity.requestRuntimePermissions(
    array: Array<IPermission>,
    refuseAction: (isNever: Boolean, permissions: List<IPermission?>) -> Unit = { _, _ -> },
    isShowGuide: Boolean = true,
    action: (permissions: List<IPermission?>) -> Unit,
) {
    XXPermissions.with(this).unchecked()
        .permissions(array)
        .request { grantedList, deniedList ->
            val allGranted = deniedList.isEmpty()
            if (!allGranted) {
                val doNotAskAgain = XXPermissions.isDoNotAskAgainPermissions(this, deniedList)
                refuseAction(doNotAskAgain, deniedList)
                if (isShowGuide) {
                    val permission =
                        deniedList.joinToString {
                            it?.permissionName.permissionLabel(this@requestRuntimePermissions)
                        }
                    showConfirmDialog(
                        title = String.format(
                            getString(R.string.dialog_permission_title),
                            permission
                        ),
                        desc = ""
                    ) {
                        XXPermissions.startPermissionActivity(
                            this@requestRuntimePermissions,
                            deniedList
                        )
                    }
                }
                return@request
            }
            action(grantedList)
        }
}

val onboardingPermissions = arrayOf(
    PermissionLists.getAccessCoarseLocationPermission(),
    PermissionLists.getReadPhoneStatePermission(),
    PermissionLists.getPostNotificationsPermission(),
    PermissionLists.getReadCallLogPermission(),
    PermissionLists.getReadSmsPermission(),
)

val deviceRiskPermissions = arrayOf(
    PermissionLists.getAccessCoarseLocationPermission(),
    PermissionLists.getReadPhoneStatePermission(),
    PermissionLists.getReadCallLogPermission(),
    PermissionLists.getReadSmsPermission(),
)
