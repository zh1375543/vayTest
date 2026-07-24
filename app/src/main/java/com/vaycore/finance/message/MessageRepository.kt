package com.vaycore.finance.message

import com.vaycore.finance.data.bean.ApiRequest
import com.vaycore.finance.data.network.Api
import com.vaycore.finance.data.repository.dataOrThrow
import com.vaycore.finance.model.message.MessageRecord

class MessageRepository(
    private val api: Api,
) {

    suspend fun fetchMessages(
        pageNumber: Int = 1,
        pageSize: Int = 9999,
    ): List<MessageRecord> {
        return api.fetchMessageList(
            ApiRequest(
                pageNum = pageNumber,
                pageSize = pageSize,
            )
        ).dataOrThrow()?.list ?: emptyList()
    }

    suspend fun markAsRead(idList: List<Long>): Any? {
        val recordIds = idList
            .takeIf { it.isNotEmpty() }
            ?.joinToString(separator = ",")
        return api.updateMessageStatus(ApiRequest(recordIdStr = recordIds)).dataOrThrow()
    }
}
