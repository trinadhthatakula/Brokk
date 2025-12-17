package com.valhalla.brokk

import android.app.Application
import com.valhalla.brokk.di.dbModule
import com.valhalla.brokk.di.installerModule
import com.valhalla.brokk.di.presentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androix.startup.KoinStartup
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.core.logger.Level
import org.koin.dsl.KoinConfiguration

@OptIn(KoinExperimentalAPI::class)
class Brokk : Application(), KoinStartup {
    override fun onKoinStartup() = KoinConfiguration {
        androidLogger(level = if (BuildConfig.DEBUG) Level.DEBUG else Level.NONE)
        androidContext(this@Brokk)
        modules(
            installerModule,
            dbModule,
            presentationModule
        )
    }
}