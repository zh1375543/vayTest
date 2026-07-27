package com.vaycore.finance.home.state

import com.vaycore.finance.model.loan.LoanDashboardResponse
import com.vaycore.finance.model.loan.ProductBean
import com.vaycore.finance.util.isPositive
import java.math.BigDecimal

/** The credit decision presented on the member home screen. */
enum class CreditStage {
    REVIEWING,
    APPROVED,
    REJECTED,
}

/** A product as displayed by the member home screen, without mutating the API model. */
data class HomeProductUi(
    val product: ProductBean,
    val canApply: Boolean,
)

/** Immutable snapshot of everything the authenticated home screen needs to render. */
data class MemberHomeUiState(
    val creditStage: CreditStage,
    val availableAmount: BigDecimal?,
    val totalAmount: BigDecimal?,
    val usedAmount: BigDecimal?,
    val creditCurrencySymbol: String?,
    val fallbackCurrencySymbol: String?,
    val recommendText: String?,
    val isApprovedCredit: Boolean,
    val loanEnabled: Boolean,
    val showAuthenticatedLayout: Boolean,
    val showCreditHeader: Boolean,
    val showProductList: Boolean,
    val showEmptyProducts: Boolean,
    val showBankError: Boolean,
    val showCalmPage: Boolean,
    val showReviewLayout: Boolean,
    val showRejectedLayout: Boolean,
    val products: List<HomeProductUi>,
    val newProducts: List<HomeProductUi>,
    val enableLoanDate: String?,
    val hasRepaymentProducts: Boolean,
    val hasPendingRepayment: Boolean,
) {
    val canShowAvailableCreditDialog: Boolean
        get() =
            isApprovedCredit &&
                availableAmount.isPositive() &&
                products.isNotEmpty()
}

/** One-off actions which must not be replayed when the screen observes state again. */
sealed interface HomeEffect {
    data object ShowAppRating : HomeEffect
    data object NavigateToOrders : HomeEffect
    data class ShowNewProducts(val products: List<HomeProductUi>) : HomeEffect
    data class ShowAvailableCredit(
        val amount: BigDecimal?,
        val currencySymbol: String?,
    ) : HomeEffect
}

/** Converts the broad dashboard response into the state owned by the member home UI. */
fun LoanDashboardResponse.toMemberHomeUiState(): MemberHomeUiState {
    val creditStage = when (userCreditStatus) {
        0 -> CreditStage.REVIEWING
        2 -> CreditStage.REJECTED
        else -> CreditStage.APPROVED
    }
    val isCertificationBlocked =
        creditStage == CreditStage.REVIEWING || creditStage == CreditStage.REJECTED
    val isApprovedCredit = userCreditStatus == 1

    val applicableProducts = showProducts.orEmpty().map { product ->
        HomeProductUi(
            product = product,
            canApply = !isCertificationBlocked &&
                (product.isNormalProduct() || product.isAddInfoProduct()),
        )
    }
    val unavailableProducts = canNotApplyProducts.orEmpty().map { product ->
        HomeProductUi(product = product, canApply = false)
    }
    val products = applicableProducts + unavailableProducts
    val hasProducts = products.isNotEmpty()

    return MemberHomeUiState(
        creditStage = creditStage,
        availableAmount = userCreditAmount,
        totalAmount = totalCreditAmount,
        usedAmount = usedAmount,
        creditCurrencySymbol = userCreditCurrencySymbol,
        fallbackCurrencySymbol = currencySymbol,
        recommendText = recommendText,
        isApprovedCredit = isApprovedCredit,
        loanEnabled = togetherLoanSign == 1 && userCreditAmount.isPositive(),
        showAuthenticatedLayout = creditStage == CreditStage.APPROVED,
        // The legacy screen hides this header when there are no products.
        showCreditHeader = isApprovedCredit && hasProducts,
        showProductList = hasProducts,
        showEmptyProducts = !hasProducts,
        showBankError = bankErrorFlag,
        // Certification states return before calm-page handling in the legacy renderer.
        showCalmPage = creditStage == CreditStage.APPROVED && calmFlag,
        showReviewLayout = creditStage == CreditStage.REVIEWING,
        showRejectedLayout = creditStage == CreditStage.REJECTED,
        products = products,
        newProducts = applicableProducts.filter { it.product.newSign == 1 },
        enableLoanDate = enableLoanStr,
        hasRepaymentProducts = repayProducts.isNullOrEmpty().not(),
        hasPendingRepayment =
            repayProducts?.any { it.isPendingRepayment() || it.isRepaymentProcessing() } == true,
    )
}
