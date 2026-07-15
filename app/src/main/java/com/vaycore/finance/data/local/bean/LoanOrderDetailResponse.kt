package com.vaycore.finance.data.local.bean

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
data class LoanOrderDetailResponse(
    val appOrderInfoDto: OrderBean? = null,
    val appOrderRepayDto: OrderBean? = null,
    val bankNo: String? = null,
    val interestAmount: BigDecimal? = null,
    val dailyAmount: BigDecimal? = null,
    val interestRateTypeStr: String? = null,
    val actualAmount: BigDecimal? = null,
    val actualRepayAmount: BigDecimal? = null,
    val actualNeedRepayAmount: BigDecimal? = null,
    val totalInstallmentServiceFee: BigDecimal? = null,
    val afterDeductionActualNeedRepayAmount: BigDecimal? = null,
    val penaltyAmount: BigDecimal? = null,
    val repayCode: String? = null,
    val applyDateStr: String? = null,
    val loanDateStr: String? = null,
    val shouldRepayDateStr: String? = null,
    val dayRateStr: String? = null,
    val reliefAmount: BigDecimal? = null,
    val deductionFee: BigDecimal? = null,
    val userCouponName: String? = null,
    val installmentRepaymentPlanDTOList: List<ProductPlanBean>? = null,
) : Parcelable
