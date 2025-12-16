package com.valhalla.brokk.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.valhalla.brokk.presentation.installer.BrokkInstallerScreen
import com.valhalla.brokk.presentation.theme.BrokkTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BrokkTheme {
                BrokkInstallerScreen()
            }
        }
    }
}