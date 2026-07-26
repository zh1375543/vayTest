package com.vaycore.finance.ui.binding

import android.widget.ImageView
import androidx.databinding.BindingAdapter
import com.vaycore.finance.model.ui.UiImageSource
import com.vaycore.finance.ui.extension.loadImage

/** Renders either a remote URL or a local Uri from one consistent UI state. */
@BindingAdapter("imageUrl")
fun ImageView.bindImageUrl(source: UiImageSource?) {
    when (source) {
        is UiImageSource.RemoteUrl -> loadImage(source.value)
        is UiImageSource.LocalUri -> loadImage(source.value)
        null -> setImageDrawable(null)
    }
}
