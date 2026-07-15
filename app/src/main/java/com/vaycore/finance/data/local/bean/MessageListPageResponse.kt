package com.vaycore.finance.data.local.bean

import android.os.Parcelable
import com.vaycore.finance.data.local.language
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@Parcelize
data class MessageListPageResponse(
    val total: Int = 0,
    val list: MutableList<MessageRecord>? = null,
) : Parcelable

@Parcelize
data class MessageRecord(
    val id: Long? = null,
    var readStatus: Boolean = false,
    val content: String? = null,
    val createTime: String? = null,
    val respTime: String? = null,
    val theme: String? = null,
    val unreadMark: Boolean = false,
    val unreadCount: Int = 0,
) : Parcelable {
    fun getTime(): String {
        val timeStr: String = createTime ?: ""
        val isEnglish: Boolean = language == "en"
        val nowMillis: Long = System.currentTimeMillis()
        val inputFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())
        val outputFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        val time: Date = try {
            inputFormat.parse(timeStr) ?: return ""
        } catch (e: Exception) {
            return timeStr
        }

        val diffMillis = abs(nowMillis - time.time)
        val diffHours = diffMillis / (1000 * 60 * 60)

        return when {
            diffHours < 1 -> {
                if (isEnglish) "Now" else "เพิ่งเสร็จ"
            }

            diffHours < 24 -> {
                val hour = diffHours.toInt()
                if (isEnglish) {
                    if (hour == 1) "1 hour ago" else "$hour hours ago"
                } else {
                    "$hour ชั่วโมงที่แล้ว"
                }
            }

            else -> {
                outputFormat.format(time)
            }
        }
    }
}
