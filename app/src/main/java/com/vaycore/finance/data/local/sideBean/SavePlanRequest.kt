package com.vaycore.finance.data.local.sideBean

import com.vaycore.finance.data.local.APPCODE

data class SavePlanRequest(
    val appCode: String = APPCODE,
    val id: Long? = null,
    val amount: Double? = null,
    val remark: String? = null,
    val imageUrls: String? = null,
    val locationText: String? = null,
    val provinceCode: String? = null,
    val provinceName: String? = null,
    val cityCode: String? = null,
    val cityName: String? = null,
    val districtCode: String? = null,
    val districtName: String? = null,
    val detailAddress: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val locationSource: Int? = null,
    val timezone: String? = null,
    val extField1: String? = null,
    val extField2: String? = null,
    val extField3: String? = null,
    val extField4: String? = null,
    val extField5: String? = null,
)