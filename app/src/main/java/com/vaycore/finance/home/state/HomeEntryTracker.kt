package com.vaycore.finance.home.state

/** Tracks the first eligible home entry for the lifetime of the app process. */
object HomeEntryTracker {

    private var firstEntry = true

    fun consumeFirstEntry(): Boolean {
        if (!firstEntry) return false

        firstEntry = false
        return true
    }

    fun reset() {
        firstEntry = true
    }
}
