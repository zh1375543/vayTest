package com.vaycore.finance.model.ui

import android.net.Uri

/** A single image source for UI rendering, independent of where the image originated. */
sealed interface UiImageSource {
    data class RemoteUrl(val value: String) : UiImageSource
    data class LocalUri(val value: Uri) : UiImageSource
}
