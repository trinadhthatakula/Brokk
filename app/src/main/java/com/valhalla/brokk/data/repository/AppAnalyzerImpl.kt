package com.valhalla.brokk.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.util.Log
import com.valhalla.brokk.domain.model.AppMetadata
import com.valhalla.brokk.domain.repository.AppAnalyzer
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import androidx.core.graphics.createBitmap

class AppAnalyzerImpl(private val context: Context) : AppAnalyzer {

    override suspend fun analyze(uri: Uri): Result<AppMetadata> = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "analysis_${System.currentTimeMillis()}.apk")
        val contentResolver = context.contentResolver

        val obbList = mutableListOf<String>()
        val abis = mutableSetOf<String>()
        var isZip = false

        try {
            // Step 1: Scan ZIP content for OBB files, native ABIs, and locate the base APK
            var hasBaseApk = false
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    ZipInputStream(inputStream).use { zipStream ->
                        var entry = zipStream.nextEntry
                        while (entry != null) {
                            isZip = true
                            val name = entry.name
                            if (name.endsWith(".obb", ignoreCase = true)) {
                                obbList.add(name)
                            } else if (name.startsWith("lib/")) {
                                val parts = name.split('/')
                                if (parts.size > 2) {
                                    abis.add(parts[1])
                                }
                            } else if (name.endsWith(".apk", ignoreCase = true)) {
                                // Extract the first APK (or prioritizing base.apk) for metadata extraction
                                if (!hasBaseApk || name.contains("base.apk", ignoreCase = true)) {
                                    FileOutputStream(tempFile).use { fos ->
                                        zipStream.copyTo(fos)
                                    }
                                    hasBaseApk = true
                                }
                            }
                            zipStream.closeEntry()
                            entry = zipStream.nextEntry
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d("BrokkAnalyzer", "Error scanning zip entries: ${e.message}")
            }

            // Step 2: Fallback (Standard standalone APK)
            // If it's not a ZIP, or we didn't find any APK inside, treat the source file itself as the APK.
            if (!isZip || !hasBaseApk) {
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }

                // If it is a standalone APK, it is also a ZIP archive. We can scan it for native ABIs.
                try {
                    tempFile.inputStream().use { fis ->
                        ZipInputStream(fis).use { zipStream ->
                            var entry = zipStream.nextEntry
                            while (entry != null) {
                                val name = entry.name
                                if (name.startsWith("lib/")) {
                                    val parts = name.split('/')
                                    if (parts.size > 2) {
                                        abis.add(parts[1])
                                    }
                                }
                                zipStream.closeEntry()
                                entry = zipStream.nextEntry
                            }
                        }
                    }
                } catch (_: Exception) {}
            }

            // Step 3: Run package analysis on the extracted APK
            val pm = context.packageManager
            val flags = PackageManager.GET_META_DATA or
                    PackageManager.GET_PERMISSIONS or
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        PackageManager.GET_SIGNING_CERTIFICATES
                    } else {
                        @Suppress("DEPRECATION")
                        PackageManager.GET_SIGNATURES
                    }

            val archiveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(tempFile.absolutePath, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(tempFile.absolutePath, flags)
            }

            if (archiveInfo == null) {
                return@withContext Result.failure(Exception("Failed to parse APK manifest. The file might be corrupted or encrypted."))
            }

            archiveInfo.applicationInfo?.sourceDir = tempFile.absolutePath
            archiveInfo.applicationInfo?.publicSourceDir = tempFile.absolutePath

            val label = archiveInfo.applicationInfo?.loadLabel(pm).toString()
            val drawable = archiveInfo.applicationInfo?.loadIcon(pm)
            val version = archiveInfo.versionName ?: "Unknown"
            val pkgName = archiveInfo.packageName

            // Extract SDK versions
            val targetSdk = archiveInfo.applicationInfo?.targetSdkVersion ?: 0
            val minSdk = if (Build.VERSION.SDK_INT >= 24) {
                archiveInfo.applicationInfo?.minSdkVersion ?: 0
            } else {
                0
            }

            // Extract permissions
            val permissionsList = archiveInfo.requestedPermissions?.toList() ?: emptyList()

            // Extract signatures
            val signaturesList = mutableListOf<String>()
            try {
                val signatures = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                    archiveInfo.signingInfo?.apkContentsSigners
                } else {
                    @Suppress("DEPRECATION")
                    archiveInfo.signatures
                }

                signatures?.forEach { signature ->
                    val certBytes = signature.toByteArray()
                    val md = java.security.MessageDigest.getInstance("SHA-256")
                    val digest = md.digest(certBytes)
                    val hexString = digest.joinToString(":") { "%02X".format(it) }
                    signaturesList.add(hexString)
                }
            } catch (e: Exception) {
                Log.e("BrokkAnalyzer", "Error reading signatures", e)
            }

            Result.success(
                AppMetadata(
                    label = label,
                    packageName = pkgName,
                    version = version,
                    icon = drawable?.toBitmap(),
                    hasObb = obbList.isNotEmpty(),
                    obbFiles = obbList,
                    targetSdkVersion = targetSdk,
                    minSdkVersion = minSdk,
                    permissions = permissionsList,
                    signatures = signaturesList,
                    nativeAbis = abis.toList().sorted()
                )
            )

        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            if (tempFile.exists()) {
                tempFile.delete()
            }
        }
    }

    private fun Drawable.toBitmap(): Bitmap {
        if (this is BitmapDrawable) return this.bitmap

        val bitmap = createBitmap(intrinsicWidth.coerceAtLeast(1), intrinsicHeight.coerceAtLeast(1))
        val canvas = Canvas(bitmap)
        setBounds(0, 0, canvas.width, canvas.height)
        draw(canvas)
        return bitmap
    }
}