package com.vaycore.finance.ui.viewmodels

import androidx.lifecycle.MutableLiveData
import com.vaycore.finance.base.BaseViewModel
import com.vaycore.finance.data.local.bean.MessageRecord
import com.vaycore.finance.data.repository.MessageRepository

class MessageCenterViewModel(
    private val messageRepository: MessageRepository = MessageRepository(BaseViewModel.api),
) : BaseViewModel() {

    val msgResult = MutableLiveData<List<MessageRecord>>()
    fun getMessageList(errorAction: () -> Unit) {
        launchData {
            messageRepository.fetchMessages()
        }.onSuccess {
            msgResult.value = it ?: emptyList()
        }.onFailed {
            errorAction.invoke()
            false
        }
    }

    fun updateReadStatus(idList: ArrayList<Long>, action: () -> Unit) {
        launchData {
            messageRepository.markAsRead(idList)
        }.showLoading().onSuccess {
            action.invoke()
        }.execute()
    }
}
