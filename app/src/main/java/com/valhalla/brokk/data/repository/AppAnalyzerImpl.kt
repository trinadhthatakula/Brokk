package com.valhalla.brokk.data.repository

import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.SerialName
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipInputStream
import androidx.core.graphics.createBitmap

private val json = Json { ignoreUnknownKeys = true }

@Serializable
private data class XapkManifest(
    @SerialName("package_name") val packageName: String,
    val name: String,
    @SerialName("version_name") val versionName: String,
    @SerialName("version_code") val versionCode: String,
    val expansions: List<XapkExpansion>? = null
)

@Serializable
private data class XapkExpansion(
    val file: String,
    @SerialName("install_path") val installPath: String
)

class AppAnalyzerImpl(private val context: Context) : AppAnalyzer {

    override suspend fun analyze(uri: Uri): Result<AppMetadata> = withContext(Dispatchers.IO) {
        val tempFile = File(context.cacheDir, "analysis_${System.currentTimeMillis()}.apk")
        val contentResolver = context.contentResolver

        var manifestString: String? = null
        var iconBytes: ByteArray? = null
        val obbList = mutableListOf<String>()

        try {
            // Step 1: Scan ZIP content for manifest.json, icon.png, and OBB files
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    ZipInputStream(inputStream).use { zipStream ->
                        var entry = zipStream.nextEntry
                        while (entry != null) {
                            val name = entry.name
                            if (name == "manifest.json") {
                                manifestString = zipStream.bufferedReader().readText()
                            } else if (name.equals("icon.png", ignoreCase = true) || name.equals("logo.png", ignoreCase = true)) {
                                iconBytes = zipStream.readBytes()
                            } else if (name.endsWith(".obb", ignoreCase = true)) {
                                obbList.add(name)
                            }
                            zipStream.closeEntry()
                            entry = zipStream.nextEntry
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d("BrokkAnalyzer", "Error scanning zip entries or file is not a zip: ${e.message}")
            }

            // Step 2: If we found manifest.json, parse it and build AppMetadata
            if (!manifestString.isNullOrEmpty()) {
                try {
                    val manifest = json.decodeFromString<XapkManifest>(manifestString!!)
                    val iconBitmap = iconBytes?.let { BitmapFactory.decodeByteArray(it, 0, it.size) }
                    
                    // Check if expansions are explicitly listed in manifest, otherwise use scanned OBBs
                    val expansionsList = manifest.expansions?.map { it.file } ?: obbList
                    
                    return@withContext Result.success(
                        AppMetadata(
                            label = manifest.name,
                            packageName = manifest.packageName,
                            version = manifest.versionName,
                            icon = iconBitmap,
                            hasObb = expansionsList.isNotEmpty(),
                            obbFiles = expansionsList
                        )
                    )
                } catch (e: Exception) {
                    Log.e("BrokkAnalyzer", "Failed to parse XAPK manifest.json, falling back to APK analysis", e)
                }
            }

            // Step 3: Fallback logic - Extract the base/first APK inside ZIP or treat stream as APK
            var isNestedBundle = false
            try {
                contentResolver.openInputStream(uri)?.use { inputStream ->
                    ZipInputStream(inputStream).use { zipStream ->
                        var entry = zipStream.nextEntry
                        while (entry != null) {
                            val name = entry.name
                            if (name.endsWith(".apk", ignoreCase = true)) {
                                FileOutputStream(tempFile).use { fos ->
                                    zipStream.copyTo(fos)
                                }
                                isNestedBundle = true
                                break
                            }
                            zipStream.closeEntry()
                            entry = zipStream.nextEntry
                        }
                    }
                }
            } catch (e: Exception) {
                isNestedBundle = false
            }

            if (!isNestedBundle) {
                contentResolver.openInputStream(uri)?.use { input ->
                    FileOutputStream(tempFile).use { output ->
                        input.copyTo(output)
                    }
                }
            }

            // Step 4: Parse the APK file
            val pm = context.packageManager
            val flags = PackageManager.GET_META_DATA

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

            Result.success(
                AppMetadata(
                    label = label,
                    packageName = pkgName,
                    version = version,
                    icon = drawable?.toBitmap(),
                    hasObb = obbList.isNotEmpty(),
                    obbFiles = obbList
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