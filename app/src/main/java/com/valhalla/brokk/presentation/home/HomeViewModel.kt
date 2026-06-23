package com.valhalla.brokk.presentation.home

import android.graphics.drawable.Icon
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Share
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

    fun getIcon(selected: Boolean  = false) = when(this){
        INSTALLER -> if(selected) Icons.Default.Home else Icons.Outlined.Home
        BIFROST -> if(selected) Icons.Default.Share else Icons.Outlined.Share
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