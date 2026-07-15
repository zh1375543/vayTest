package com.vaycore.finance.ui.activities.auth

import android.content.Context
import com.vaycore.finance.data.local.authConfigList
import com.vaycore.finance.data.local.bean.UserAuthStatusResponse
import com.vaycore.finance.util.start

fun UserAuthStatusResponse.routeToNextAuthStep(
    context: Context,
    isFromAuthPage: Boolean = true,
) {
    val configList = authConfigList.filterNot { it.isBlank() }
    val hasPassedRequiredSteps = isPass(configList)

    if (userAuthState == "30" && hasPassedRequiredSteps) {
        context.start<AuthSuccessActivity>()
        return
    }
    if (isFromAuthPage
        && hasPassedRequiredSteps
    ) {
        context.start<AuthSuccessActivity>()
        return
    }
    configList.forEach {
        when {
            it.uppercase() == "KYC" && kycState != "30" -> {
                context.start<KycAuthActivity>()
                return
            }

            it.uppercase() == "ID" && idState != "30" -> {
                context.start<PersonalInfoAuthActivity>()
                return
            }

            it.uppercase() == "BANK" && bankCardState != "30" -> {
                context.start<BankAccountAuthActivity>()
                return
            }

            it.uppercase() == "TELECOM" && telecomPermissionState != "30" -> {
                context.start<CarrierAuthActivity>()
                return
            }
        }
    }
}
