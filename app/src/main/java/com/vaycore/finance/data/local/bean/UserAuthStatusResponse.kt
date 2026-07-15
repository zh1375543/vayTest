package com.vaycore.finance.data.local.bean

data class UserAuthStatusResponse(
    val id: Long = 0,
    val userId: Long = 0,
    var idState: String? = null,
    val idTime: String? = null,
    var bankCardState: String? = null,
    val bankCardTime: String? = null,
    var workInfoState: String? = null,
    val workInfoTime: String? = null,
    var kycState: String? = null,
    val kycTime: String? = null,
    val userAuthState: String? = null,
    val createTime: String? = null,
    val updateTime: String? = null,
    val telecomPermissionState: String? = null,
) {
    fun isPass(configList: List<String>): Boolean {
        val configList = configList.filterNot { it.isBlank() }
        return areRequiredAuthStepsPassed(
            mapOf(
                "BANK" to (configList.contains("BANK") to bankCardState),
                "KYC" to (configList.contains("KYC") to kycState),
                "ID" to (configList.contains("ID") to idState),
                "TELECOM" to (configList.contains("TELECOM") to telecomPermissionState)
            )
        )
    }

    fun isFillBank(): Boolean {
        return bankCardState == "30"
    }
}

fun areRequiredAuthStepsPassed(configs: Map<String, Pair<Boolean, String?>>): Boolean {
    configs.forEach { (_, pair) ->
        val (isConfig, state) = pair
        if (isConfig && state != "30") return false
    }
    return true
}
