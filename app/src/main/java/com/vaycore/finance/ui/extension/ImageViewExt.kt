package com.vaycore.finance.ui.extension

import android.net.Uri
import android.widget.ImageView
import com.bumptech.glide.Glide

fun ImageView.loadImage(url: String?, errorImage: Int? = null) {
    val request = Glide.with(this).load(url)
    errorImage?.let { request.placeholder(it).error(it) }
    request.into(this)
}

fun ImageView.loadImage(uri: Uri?, errorImage: Int? = null) {
    val request = Glide.with(this).load(uri)
    errorImage?.let { request.placeholder(it).error(it) }
    request.into(this)
}
