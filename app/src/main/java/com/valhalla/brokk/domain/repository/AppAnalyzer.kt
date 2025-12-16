package com.valhalla.brokk.domain.repository

import com.valhalla.brokk.domain.model.AppMetadata

interface AppAnalyzer {
    /**
     * Extracts metadata from a URI (APK, XAPK, APKS) without installing it.
     */
    suspend fun analyze(uri: android.net.Uri): Result<AppMetadata>
}