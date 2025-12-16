package com.valhalla.brokk

import android.app.Application
import com.valhalla.brokk.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.androix.startup.KoinStartup
import org.koin.core.annotation.KoinExperimentalAPI
import org.koin.dsl.KoinConfiguration

@OptIn(KoinExperimentalAPI::class)
class Brokk : Application(), KoinStartup {
    override fun onKoinStartup() = KoinConfiguration {
        androidLogger()
        androidContext(this@Brokk)
        modules(appModule)
    }
}