package com.valhalla.brokk.di

import android.content.pm.PackageManager
import com.valhalla.brokk.data.repository.AppAnalyzerImpl
import com.valhalla.brokk.data.repository.InstallerRepositoryImpl
import com.valhalla.brokk.domain.BrokkEventBus
import com.valhalla.brokk.domain.repository.AppAnalyzer
import com.valhalla.brokk.domain.repository.InstallerRepository
import com.valhalla.brokk.presentation.installer.BrokkViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val appModule = module {
    // 1. The Singleton Event Bus (Critical for Receiver <-> VM comms)
    single { BrokkEventBus() }

    // 2. Repository
    single<InstallerRepository> {
        InstallerRepositoryImpl(
            context = androidContext(),
            eventBus = get()
        )
    }

    single<PackageManager>{
        androidContext().packageManager
    }

    single<AppAnalyzer> { AppAnalyzerImpl(androidContext()) }

    viewModelOf(::BrokkViewModel)
}