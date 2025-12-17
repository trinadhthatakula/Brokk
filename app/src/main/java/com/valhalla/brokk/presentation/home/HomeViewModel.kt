package com.valhalla.brokk.presentation.home

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

enum class HomeNavItem{
    INSTALLER,//App installer
    BIFROST; //App share protocol

    fun getTitle(): String {
        return when(this){
            INSTALLER -> "Installer"
            BIFROST -> "Share Apps"
        }
    }
}

data class HomeState(
    val selectedNavItem: HomeNavItem = HomeNavItem.INSTALLER
)

class HomeViewModel: ViewModel() {

    private val _state = MutableStateFlow<HomeState>(HomeState())
    val state = _state.asStateFlow()

    fun selectNavItem(item: HomeNavItem){
        _state.update {
            it.copy(selectedNavItem = item)
        }
    }

}