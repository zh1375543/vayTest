package com.vaycore.finance.data.repository

import com.vaycore.finance.data.local.bean.MessageRecord
import com.vaycore.finance.data.local.bean.ApiRequest
import com.vaycore.finance.data.network.Api

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
