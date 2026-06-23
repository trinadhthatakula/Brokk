package com.valhalla.brokk.presentation.home

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.valhalla.brokk.presentation.history.HistoryScreen
import com.valhalla.brokk.presentation.installer.BrokkInstallerScreen
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    homeViewModel: HomeViewModel = koinViewModel()
) {
    val state by homeViewModel.state.collectAsStateWithLifecycle()
    Scaffold(
        bottomBar = {
            NavigationBar {
                HomeNavItem.entries.forEach { navItem ->
                    NavigationBarItem(
                        selected = state.selectedNavItem == navItem,
                        onClick = {
                            homeViewModel.selectNavItem(navItem)
                        },
                        icon = {
                            Icon(
                                navItem.getIcon(state.selectedNavItem == navItem),
                                navItem.getTitle()
                            )
                        },
                        label = {
                            Text(
                                navItem.getTitle()
                            )
                        }
                    )
                }

            }
        }
    ) { paddingValues ->
        Column(modifier
            .fillMaxSize()
            .padding(paddingValues)) {
            when (state.selectedNavItem) {
                HomeNavItem.INSTALLER -> BrokkInstallerScreen()
                HomeNavItem.HISTORY -> HistoryScreen()
                else -> Text("Work in progress")
            }
        }
    }
}