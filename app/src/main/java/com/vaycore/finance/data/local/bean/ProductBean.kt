package com.vaycore.finance.data.local.bean

import android.os.Parcelable
import com.vaycore.finance.data.local.ORDER_STATUS_IN_RENEWAL
import com.vaycore.finance.data.local.ORDER_STATUS_IN_RENEWAL_PROCESS
import com.vaycore.finance.data.local.ORDER_STATUS_PAYMENT_PENDING
import com.vaycore.finance.data.local.ORDER_STATUS_PAYMENT_PROCESS
import kotlinx.parcelize.Parcelize
import java.math.BigDecimal

@Parcelize
data class ProductBean(
    val productId: Long,
    val productName: String? = null,
    val minLoanAmount: BigDecimal? = null,
    val maxLoanAmount: BigDecimal? = null,
    val timeLimit: Int? = null,
    val interestRate: String? = null,
    val jumpType: Int? = null,
    val downloadUrl: String? = null,
    val tagList: List<String>? = null,
    val productImageUrl: String? = null,
    var canApply: Boolean = true,
    val showConditionTypeSign: String? = null,
    val creditStatus: Int? = null,
    val enableLoanStr: String? = null,
    var currency: String? = null,
    var currencySymbol: String? = null,
    val loanTermRange: String? = null,
    val bankErrorFlag: Boolean = false,
    val newSign: Int = 0,
    val orderId: Long? = null,
    val orderNo: String? = null,
    val loanAmount: BigDecimal? = null,
    var pushStatus: Int,
    val pushMessage: String? = null,
    val repayTimeStr: String? = null,
    val orderStatus: Int? = null,
    var isTogether: Boolean = false,
    val loanAmountRange: String? = null,
    val applyDateStr: String? = null,
    val actualRepayAmount: BigDecimal? = null,
    var pDetail: ProductBean? = null,
    val actualAmount: BigDecimal? = null,
    val interestAmount: BigDecimal? = null,
    var isCheck: Boolean = true,
    val appProductHandleFeeConfigDtos: List<ProductFeeBean>? = null,
    val appRepaymentPlanDTOList: List<ProductPlanBean>? = null,
    val productInstallmentPlanDTOList: List<ProductBean>? = null,
    var isFillBank: Boolean = false,
    val loanTermConfigDTOList: List<ProductBean>? = null,
    var isExpand: Boolean = false,
    var selectedTermIndex: Int? = null,         // index of the selected plan
    var isPlanLayoutVisible: Boolean? = null,   // plan section collapse state
    var isRepaymentGroupVisible: Boolean = true,


    val firstRepayment: BigDecimal? = null,
    val planNums: Int? = null,
    val serviceFeeType: Int? = null,
    val dailyInterest: BigDecimal? = null,
    val isDefault: Int = 0,
    val defaultSign: Int = 0,
    val isDelete: Int = 0,
    val cardName: String? = null,
    val id: Long? = null,
    val appId: Long? = null,
    val productState: Int? = null,
    val dailyAmount: BigDecimal? = null,
    val interestRateType: Int? = null,
    val handleFeeState: Int? = null,
    val overdueType: Int? = null,
    val overdueValue: Double? = null,
    val maxPenaltyType: Int? = null,
    val maxPenaltyValue: Int? = null,
    val tags: String? = null,
    val canLoanAmount: BigDecimal? = null,
    val loanTermId: Long? = null,
    val installmentServiceFee: BigDecimal? = null,
    val nowTimeStr: String? = null,
    val bankNo: String? = null,
    val serviceAmount: BigDecimal? = null,
    val bankInfoId: Long? = null,
    val isSign: Int? = null,
    val isNew: Int? = null,
    val loanTermList: List<LoanTermBean>? = null,
    val bankInfoPayOutFailSign: Boolean = false,
    val repayInterestAmountRate: BigDecimal? = null,
    val repayActualAmount: BigDecimal? = null,
) : Parcelable {
    fun isNormalProduct(): Boolean {
        return showConditionTypeSign == null || showConditionTypeSign == "0"
    }

    fun isAddInfoProduct(): Boolean {
        return showConditionTypeSign == "1"
    }

    fun isPendingRepayment(): Boolean {
        return when (orderStatus) {
            ORDER_STATUS_PAYMENT_PENDING,
            ORDER_STATUS_IN_RENEWAL,
            ORDER_STATUS_IN_RENEWAL_PROCESS -> true

            else -> false
        }
    }

    fun isRepaymentProcessing(): Boolean {
        return when (orderStatus) {
            ORDER_STATUS_PAYMENT_PROCESS -> true
            else -> false
        }
    }
}
