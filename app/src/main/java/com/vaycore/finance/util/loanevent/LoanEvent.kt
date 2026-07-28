package com.vaycore.finance.util.loanevent

/** Stable event markers used by the loan application event-log protocol. */
enum class LoanEvent(val marker: String) {
    VIEW_ENTER_LOAN("view_enter_loan"),
    CLICK_CHOOSE_WALLET("click_choose_wallet"),
    CLICK_CONFIRM_WALLET("click_confirm_wallet"),
    CLICK_OPEN_AGREEMENT("click_open_agreement"),
    CLICK_CHECK_AGREEMENT("click_check_agreement"),
    CLICK_APPLY_LOAN("click_apply_loan"),
    CLICK_SUBMIT_LOAN("click_submit_loan"),
    CLICK_CANCEL_LOAN("click_cancel_loan"),
    VIEW_QUIT_LOAN("view_quit_loan")
}
