package com.vaycore.finance.ui.extension

import android.net.Uri
import android.widget.ImageView
import coil3.load
import coil3.request.error
import coil3.request.placeholder

fun ImageView.loadImage(url: String?, errorImage: Int? = null) {
//    Glide.with(context).load(url).error(errorImage).into(this)
    load(url) {
        errorImage?.let {
            placeholder(errorImage)
            error(errorImage)
        }
    }
}

fun ImageView.loadImage(uri: Uri?, errorImage: Int? = null) {
//    Glide.with(context).load(uri).error(errorImage).into(this)
    load(uri) {
        errorImage?.let {
            placeholder(errorImage)
            error(errorImage)
        }
    }
}
