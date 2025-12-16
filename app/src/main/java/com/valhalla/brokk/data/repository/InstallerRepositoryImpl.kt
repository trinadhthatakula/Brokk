package com.valhalla.brokk.data.repository

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.database.Cursor
import android.net.Uri
import android.provider.OpenableColumns
import android.util.Log
import com.valhalla.brokk.data.ACTION_INSTALL_STATUS
import com.valhalla.brokk.data.receivers.InstallReceiver
import com.valhalla.brokk.domain.BrokkEventBus
import com.valhalla.brokk.domain.InstallState
import com.valhalla.brokk.domain.repository.InstallerRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.util.zip.ZipInputStream

class InstallerRepositoryImpl(
    private val context: Context,
    private val eventBus: BrokkEventBus
) : InstallerRepository {

    private val packageInstaller = context.packageManager.packageInstaller

    override suspend fun installPackage(uri: Uri) = withContext(Dispatchers.IO) {
        // 1. Get File Size for Progress Calculation
        val totalBytes = getFileSize(uri)
        var bytesProcessed = 0L
        var lastProgressEmitted = 0

        // Notify UI we are starting
        eventBus.emit(InstallState.Parsing)

        val params = PackageInstaller.SessionParams(
            PackageInstaller.SessionParams.MODE_FULL_INSTALL
        )

        // Create session
        val sessionId = try {
            packageInstaller.createSession(params)
        } catch (e: Exception) {
            eventBus.emit(InstallState.Error("Failed to create session: ${e.message}"))
            return@withContext
        }

        val session = try {
            packageInstaller.openSession(sessionId)
        } catch (e: Exception) {
            eventBus.emit(InstallState.Error("Failed to open session: ${e.message}"))
            return@withContext
        }

        try {
            val originalStream: InputStream? = context.contentResolver.openInputStream(uri)
            if (originalStream == null) {
                session.abandon()
                eventBus.emit(InstallState.Error("Could not open file stream."))
                return@withContext
            }

            // 2. Wrap the stream to track progress
            val trackedStream = object : InputStream() {
                override fun read(): Int {
                    val b = originalStream.read()
                    if (b != -1) updateProgress(1)
                    return b
                }

                override fun read(b: ByteArray, off: Int, len: Int): Int {
                    val read = originalStream.read(b, off, len)
                    if (read != -1) updateProgress(read.toLong())
                    return read
                }

                override fun close() {
                    originalStream.close()
                }

                private fun updateProgress(readBytes: Long) {
                    bytesProcessed += readBytes
                    if (totalBytes > 0) {
                        val currentProgress = ((bytesProcessed.toDouble() / totalBytes) * 100).toInt()
                        // Only emit if progress changed by at least 1% to save UI thread
                        if (currentProgress > lastProgressEmitted) {
                            lastProgressEmitted = currentProgress
                            // Fire and forget (don't block the stream)
                            CoroutineScope(Dispatchers.IO).launch {
                                eventBus.emit(InstallState.Installing(bytesProcessed.toFloat() / totalBytes))
                            }
                        }
                    }
                }
            }

            eventBus.emit(InstallState.Installing(0.0f))

            var filesWritten = false

            // 3. Use the tracked stream
            ZipInputStream(trackedStream).use { zipStream ->
                var entry = zipStream.nextEntry
                while (entry != null) {
                    val name = entry.name

                    if (name.endsWith(".apk", ignoreCase = true)) {
                        filesWritten = true

                        val size = entry.size

                        if (size == -1L) {
                            // Buffer logic (Progress is tracked via trackedStream as we read from zipStream)
                            Log.w("Brokk", "Entry $name has unknown size. Buffering...")
                            val tempFile = File(context.cacheDir, "temp_${System.currentTimeMillis()}_$name")
                            FileOutputStream(tempFile).use { fos ->
                                zipStream.copyTo(fos)
                            }

                            val actualSize = tempFile.length()
                            val outStream = session.openWrite(name, 0, actualSize)
                            tempFile.inputStream().use { fis ->
                                fis.copyTo(outStream)
                            }
                            session.fsync(outStream)
                            outStream.close()
                            tempFile.delete()

                        } else {
                            // Direct stream logic
                            val outStream = session.openWrite(name, 0, size)
                            val buffer = ByteArray(65536)
                            var len: Int
                            // zipStream read calls trackedStream read, updating progress automatically
                            while (zipStream.read(buffer).also { len = it } > 0) {
                                outStream.write(buffer, 0, len)
                            }
                            session.fsync(outStream)
                            outStream.close()
                        }
                    }
                    zipStream.closeEntry()
                    entry = zipStream.nextEntry
                }
            }

            if (!filesWritten) {
                session.abandon()
                eventBus.emit(InstallState.Error("No valid APK files found in this archive."))
                return@withContext
            }

            // Finalizing
            eventBus.emit(InstallState.Installing(1.0f))

            val intent = Intent(context, InstallReceiver::class.java).apply {
                action = ACTION_INSTALL_STATUS
                setPackage(context.packageName)
            }

            val pendingIntent = PendingIntent.getBroadcast(
                context,
                sessionId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
            )

            session.commit(pendingIntent.intentSender)
            session.close()

        } catch (e: Exception) {
            session.abandon()
            Log.e("BrokkInstaller", "Install failed", e)
            eventBus.emit(InstallState.Error(e.message ?: "Unknown installation error"))
        }
    }

    private fun getFileSize(uri: Uri): Long {
        var size = -1L
        val cursor: Cursor? = context.contentResolver.query(uri, null, null, null, null)
        cursor?.use {
            if (it.moveToFirst()) {
                val sizeIndex = it.getColumnIndex(OpenableColumns.SIZE)
                if (sizeIndex != -1) {
                    size = it.getLong(sizeIndex)
                }
            }
        }
        return size
    }
}

