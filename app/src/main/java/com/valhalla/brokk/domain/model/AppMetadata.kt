package com.valhalla.brokk.domain.model

import android.graphics.Bitmap

data class AppMetadata(
    val label: String,
    val packageName: String,
    val version: String,
    val icon: Bitmap?
)
