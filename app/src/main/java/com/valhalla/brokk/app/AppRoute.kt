package com.valhalla.brokk.app

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
sealed interface AppRoute: NavKey{

    @Serializable
    data object Home: AppRoute

}