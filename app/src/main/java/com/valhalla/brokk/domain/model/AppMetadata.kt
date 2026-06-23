package com.valhalla.brokk.domain.model

import android.graphics.Bitmap

data class AppMetadata(
    val label: String,
    val packageName: String,
    val version: String,
    val icon: Bitmap?,
    val hasObb: Boolean = false,
    val obbFiles: List<String> = emptyList(),
    val targetSdkVersion: Int = 0,
    val minSdkVersion: Int = 0,
    val permissions: List<String> = emptyList(),
    val signatures: List<String> = emptyList(),
    val nativeAbis: List<String> = emptyList()
)
