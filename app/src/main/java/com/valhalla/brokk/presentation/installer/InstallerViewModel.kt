package com.valhalla.brokk.presentation.installer

import android.content.pm.PackageManager
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valhalla.brokk.domain.InstallerEventBus
import com.valhalla.brokk.domain.InstallState
import com.valhalla.brokk.domain.model.AppMetadata
import com.valhalla.brokk.domain.model.HistoryRecord
import com.valhalla.brokk.domain.model.OperationType
import com.valhalla.brokk.domain.repository.AppAnalyzer
import com.valhalla.brokk.domain.repository.HistoryRepository
import com.valhalla.brokk.domain.repository.InstallerRepository
import kotlinx.coroutines.launch

class InstallerViewModel(
    private val repository: InstallerRepository,
    private val analyzer: AppAnalyzer,
    private val eventBus: InstallerEventBus,
    private val packageManager: PackageManager,
    private val historyRepository: HistoryRepository
) : ViewModel() {

    val installState = eventBus.events

    // History Read Logic Moved to HistoryViewModel

    var currentPackageName: String? = null
        private set

    var isQueueActive by mutableStateOf(false)
        private set
    var queueCurrentIndex by mutableStateOf(0)
        private set
    var queueTotalCount by mutableStateOf(0)
        private set

    private val uriQueue = mutableListOf<Uri>()

    private var pendingUri: Uri? = null
    private var pendingMetadata: AppMetadata? = null
    private var isUpdateOperation: Boolean = false

    init {
        // Clear sticky state logic
        val lastState = eventBus.events.replayCache.firstOrNull()
        if (lastState is InstallState.Success || lastState is InstallState.ObbExported || lastState is InstallState.Error) {
            viewModelScope.launch { eventBus.emit(InstallState.Idle) }
        }

        // Listen for SUCCESS to trigger OBB copying if present, and save history
        viewModelScope.launch {
            eventBus.events.collect { state ->
                if (state is InstallState.Success) {
                    val meta = pendingMetadata
                    val uri = pendingUri
                    if (meta != null && meta.hasObb && uri != null) {
                        val packageName = meta.packageName
                        val obbFiles = meta.obbFiles

                        // Clear OBB flags so subsequent Success events don't trigger OBB copy again
                        pendingMetadata = meta.copy(hasObb = false, obbFiles = emptyList())

                        viewModelScope.launch {
                            eventBus.emit(InstallState.CopyingObb(0.0f))
                            val result = repository.copyObb(
                                uri = uri,
                                packageName = packageName,
                                obbEntries = obbFiles,
                                onProgress = { progress ->
                                    viewModelScope.launch {
                                        eventBus.emit(InstallState.CopyingObb(progress))
                                    }
                                }
                            )

                            result.onSuccess { directCopied ->
                                if (directCopied) {
                                    eventBus.emit(InstallState.Success)
                                } else {
                                    val fallbackPath = "Download/Brokk/$packageName"
                                    eventBus.emit(InstallState.ObbExported(packageName, fallbackPath))
                                }
                            }.onFailure { error ->
                                eventBus.emit(InstallState.Error("APK installed but OBB extraction failed: ${error.message}"))
                            }
                        }
                    } else {
                        saveHistoryRecord()
                    }
                } else if (state is InstallState.ObbExported) {
                    saveHistoryRecord()
                }
            }
        }
    }

    fun installFile(uri: Uri) {
        viewModelScope.launch {
            currentPackageName = null
            pendingMetadata = null
            eventBus.emit(InstallState.Parsing)

            val analysis = analyzer.analyze(uri)

            analysis.onSuccess { meta ->
                pendingUri = uri
                pendingMetadata = meta
                currentPackageName = meta.packageName

                isUpdateOperation = try {
                    packageManager.getPackageInfo(meta.packageName, 0)
                    true
                } catch (_: PackageManager.NameNotFoundException) {
                    false
                }

                eventBus.emit(InstallState.ReadyToInstall(meta, isUpdateOperation))
            }.onFailure {
                eventBus.emit(InstallState.Error("Failed to parse package."))
            }
        }
    }

    fun confirmInstall() {
        val uri = pendingUri ?: return
        viewModelScope.launch {
            repository.installPackage(uri)
        }
    }

    private fun saveHistoryRecord() {
        val meta = pendingMetadata ?: return
        val uri = pendingUri

        viewModelScope.launch {
            historyRepository.addRecord(
                HistoryRecord(
                    packageName = meta.packageName,
                    label = meta.label,
                    version = meta.version,
                    timestamp = System.currentTimeMillis(),
                    type = if (isUpdateOperation) OperationType.UPDATE else OperationType.INSTALL,
                    path = uri?.toString() ?: "Unknown"
                )
            )
            // Cleanup
            pendingUri = null
            pendingMetadata = null
        }
    }

    fun resetState() {
        viewModelScope.launch {
            eventBus.emit(InstallState.Idle)
            pendingUri = null
            currentPackageName = null
        }
    }

    fun installMultipleFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            uriQueue.clear()
            uriQueue.addAll(uris)
            queueTotalCount = uris.size
            queueCurrentIndex = 1
            isQueueActive = true
            installFile(uriQueue.first())
        }
    }

    fun loadNextInQueue() {
        viewModelScope.launch {
            if (queueCurrentIndex < queueTotalCount) {
                val nextUri = uriQueue[queueCurrentIndex]
                queueCurrentIndex++
                installFile(nextUri)
            } else {
                resetQueue()
            }
        }
    }

    fun cancelQueue() {
        uriQueue.clear()
        queueCurrentIndex = 0
        queueTotalCount = 0
        isQueueActive = false
        resetState()
    }

    private fun resetQueue() {
        uriQueue.clear()
        queueCurrentIndex = 0
        queueTotalCount = 0
        isQueueActive = false
        resetState()
    }
}