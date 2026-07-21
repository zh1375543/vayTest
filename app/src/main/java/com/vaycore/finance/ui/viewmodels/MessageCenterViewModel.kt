package com.vaycore.finance.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.vaycore.finance.base.BaseViewModel
import com.vaycore.finance.data.local.bean.MessageRecord
import com.vaycore.finance.data.repository.MessageRepository

class MessageCenterViewModel(
    private val messageRepository: MessageRepository = MessageRepository(BaseViewModel.api),
) : BaseViewModel() {

    private val _msgResult = MutableLiveData<List<MessageRecord>>()
    val msgResult: LiveData<List<MessageRecord>> = _msgResult

    fun getMessageList(errorAction: () -> Unit) {
        launchData {
            messageRepository.fetchMessages()
        }.onSuccess {
            _msgResult.value = it ?: emptyList()
        }.onFailed {
            errorAction.invoke()
            false
        }
    }

    fun markAsRead(message: MessageRecord) {
        if (message.readStatus) return
        updateReadStatus(listOf(message.id ?: 0L))
    }

    fun markAllAsRead() {
        val unreadIds = _msgResult.value.orEmpty()
            .filterNot(MessageRecord::readStatus)
            .map { it.id ?: 0L }
        if (unreadIds.isEmpty()) return
        updateReadStatus(unreadIds)
    }

    private fun updateReadStatus(idList: List<Long>) {
        launchData {
            messageRepository.markAsRead(idList)
        }.showLoading().onSuccess {
            val readIds = idList.toSet()
            _msgResult.value = _msgResult.value.orEmpty().map { message ->
                if ((message.id ?: 0L) in readIds) {
                    message.copy(readStatus = true)
                } else {
                    message
                }
            }
        }.execute()
    }
}
