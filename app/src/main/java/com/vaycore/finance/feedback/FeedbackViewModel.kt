package com.vaycore.finance.feedback

import com.vaycore.finance.base.BaseViewModel
import com.vaycore.finance.mine.data.FeedbackRepository

/** Submits user feedback independently from the screen that presents the feedback prompt. */
class FeedbackViewModel(
    private val feedbackRepository: FeedbackRepository = FeedbackRepository(api),
) : BaseViewModel() {

    fun submitFeed(content: String, action: () -> Unit) {
        launchData {
            feedbackRepository.submitFeedback(content)
        }.showLoading().onSuccess {
            action()
        }.execute()
    }
}
