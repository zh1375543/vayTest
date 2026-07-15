package com.vaycore.finance.data.local.bean

class Event<out T>(private val content: T) {
    private var hasBeenHandled = false
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) null else {
            hasBeenHandled = true
            content
        }
    }
    fun peekContent(): T = content
}

data class ClickablePart(
    val text: String,
    val color: Int,
    val sizeScale: Float = 1.1f,
    val onClick: () -> Unit,
)