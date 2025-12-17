package com.valhalla.brokk.app

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import com.valhalla.brokk.presentation.components.ExitDialog
import com.valhalla.brokk.presentation.home.HomeScreen
import com.valhalla.brokk.presentation.theme.BrokkTheme

@Composable
fun App(modifier: Modifier = Modifier, onExit: () -> Unit) {

    val backStack = rememberNavBackStack(AppRoute.Home)
    var showExitDialog by remember { mutableStateOf(false) }

    BrokkTheme{
        NavDisplay(backStack, onBack = {
            if (backStack.size > 1) backStack.removeAt(backStack.lastIndex)
            else showExitDialog = true
        }, entryProvider = { key ->
            when (key) {
                is AppRoute.Home -> NavEntry(key){
                    HomeScreen()
                }
                else -> NavEntry(key){
                    Text("Not Implemented Yet")
                }
            }
        }, modifier = modifier)
    }

    if (showExitDialog) {
        ExitDialog(onDismiss = { confirmed ->
            showExitDialog = false
            if (confirmed) {
                onExit()
            }
        })
    }

}