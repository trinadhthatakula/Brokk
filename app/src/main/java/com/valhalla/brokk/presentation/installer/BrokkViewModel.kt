package com.valhalla.brokk.presentation.installer

import android.content.pm.PackageManager
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.valhalla.brokk.domain.BrokkEventBus
import com.valhalla.brokk.domain.InstallState
import com.valhalla.brokk.domain.repository.AppAnalyzer
import com.valhalla.brokk.domain.repository.InstallerRepository
import kotlinx.coroutines.launch

class BrokkViewModel(
    private val repository: InstallerRepository,
    private val analyzer: AppAnalyzer,
    private val eventBus: BrokkEventBus,
    private val packageManager: PackageManager
) : ViewModel() {

    // Expose the bus events as our UI State
    val installState = eventBus.events

    // Track current package for "Open App" functionality
    var currentPackageName: String? = null
        private set

    private var pendingUri: Uri? = null

    init {
        val lastState = eventBus.events.replayCache.firstOrNull()
        if (lastState is InstallState.Success || lastState is InstallState.Error) {
            viewModelScope.launch {
                eventBus.emit(InstallState.Idle)
            }
        }
    }

    fun installFile(uri: Uri) {
        viewModelScope.launch {
            currentPackageName = null
            eventBus.emit(InstallState.Parsing)

            val analysis = analyzer.analyze(uri)

            analysis.onSuccess { meta ->
                pendingUri = uri
                currentPackageName = meta.packageName

                // CHECK: Does this package already exist?
                val isUpdate = try {
                    packageManager.getPackageInfo(meta.packageName, 0)
                    true // It exists
                } catch (_: PackageManager.NameNotFoundException) {
                    false // It does not
                }

                eventBus.emit(InstallState.ReadyToInstall(meta, isUpdate))
            }.onFailure {
                eventBus.emit(InstallState.Error("Failed to parse package. Is this a valid APK/XAPK?"))
            }
        }
    }

    fun confirmInstall() {
        val uri = pendingUri ?: return
        viewModelScope.launch {
            repository.installPackage(uri)
            pendingUri = null
        }
    }

    fun resetState() {
        viewModelScope.launch {
            eventBus.emit(InstallState.Idle)
            pendingUri = null
            currentPackageName = null
        }
    }
}