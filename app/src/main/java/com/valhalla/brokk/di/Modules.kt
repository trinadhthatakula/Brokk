package com.valhalla.brokk.di

import android.content.pm.PackageManager
import androidx.room.Room
import com.valhalla.brokk.data.db.BrokkDatabase
import com.valhalla.brokk.data.repository.AppAnalyzerImpl
import com.valhalla.brokk.data.repository.HistoryRepositoryImpl
import com.valhalla.brokk.data.repository.InstallerRepositoryImpl
import com.valhalla.brokk.domain.InstallerEventBus
import com.valhalla.brokk.domain.repository.AppAnalyzer
import com.valhalla.brokk.domain.repository.HistoryRepository
import com.valhalla.brokk.domain.repository.InstallerRepository
import com.valhalla.brokk.presentation.history.HistoryViewModel
import com.valhalla.brokk.presentation.home.HomeViewModel
import com.valhalla.brokk.presentation.installer.InstallerViewModel
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.bind
import org.koin.dsl.module

val installerModule = module {
    // 1. The Singleton Event Bus (Critical for Receiver <-> VM comms)
    single { InstallerEventBus() }

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
}

val dbModule = module {
    single<BrokkDatabase> { Room.databaseBuilder(
        androidContext(),
        BrokkDatabase::class.java,
        "brokk_db"
    ).build() }
    singleOf(::HistoryRepositoryImpl).bind(HistoryRepository::class)
}

val presentationModule = module {
    viewModelOf(::InstallerViewModel)
    viewModelOf(::HistoryViewModel)
    viewModelOf(::HomeViewModel)
}