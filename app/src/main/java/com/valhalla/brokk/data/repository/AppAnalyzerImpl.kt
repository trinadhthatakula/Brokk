package com.valhalla.brokk.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
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

        try {
            val contentResolver = context.contentResolver
            val inputStream = contentResolver.openInputStream(uri)
                ?: return@withContext Result.failure(Exception("Cannot open file stream"))

            // 1. Extraction Phase
            // We need to find the "base.apk" or the main APK to parse manifest info.
            // If it's a single APK, we copy the whole thing.
            // If it's a ZIP/XAPK, we hunt for "base.apk".

            var foundApk = false

            // Check magic bytes or extension to see if it's a ZIP-based format vs raw APK
            // For simplicity, we try to open as Zip. If it fails, we treat as raw APK.
            try {
                ZipInputStream(inputStream).use { zipStream ->
                    var entry = zipStream.nextEntry
                    while (entry != null) {
                        val name = entry.name
                        // In App Bundles/XAPK, the main manifest is usually in 'base.apk'
                        // If not found, we take the first .apk we see as a fallback
                        if (name.endsWith("base.apk", ignoreCase = true) ||
                            (name.endsWith(".apk", ignoreCase = true))) {

                            FileOutputStream(tempFile).use { fos ->
                                zipStream.copyTo(fos)
                            }
                            foundApk = true
                            break // We only need one APK to get the icon/label
                        }
                        zipStream.closeEntry()
                        entry = zipStream.nextEntry
                    }
                }
            } catch (_: Exception) {
                // Not a zip? Maybe it's a raw APK file.
                // Reset stream
                contentResolver.openInputStream(uri)?.use { rawInput ->
                    FileOutputStream(tempFile).use { fos ->
                        rawInput.copyTo(fos)
                    }
                    foundApk = true
                }
            }

            if (!foundApk) {
                return@withContext Result.failure(Exception("No valid APK found for analysis"))
            }

            // 2. Parsing Phase
            val pm = context.packageManager
            val flags = PackageManager.GET_META_DATA

            val archiveInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                pm.getPackageArchiveInfo(tempFile.absolutePath, PackageManager.PackageInfoFlags.of(flags.toLong()))
            } else {
                @Suppress("DEPRECATION")
                pm.getPackageArchiveInfo(tempFile.absolutePath, flags)
            }

            if (archiveInfo == null) {
                return@withContext Result.failure(Exception("Failed to parse APK manifest"))
            }

            // Necessary to load resources properly from an external file
            archiveInfo.applicationInfo?.sourceDir = tempFile.absolutePath
            archiveInfo.applicationInfo?.publicSourceDir = tempFile.absolutePath

            val label = archiveInfo.applicationInfo?.loadLabel(pm)?.toString() ?: "Unknown"
            val drawable = archiveInfo.applicationInfo?.loadIcon(pm)
            val version = archiveInfo.versionName ?: "Unknown"
            val pkgName = archiveInfo.packageName

            Result.success(
                AppMetadata(
                    label = label,
                    packageName = pkgName,
                    version = version,
                    icon = drawable?.toBitmap()
                )
            )

        } catch (e: Exception) {
            Result.failure(e)
        } finally {
            // 3. Cleanup
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