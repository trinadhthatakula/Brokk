package com.valhalla.brokk.presentation.installer

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.valhalla.brokk.presentation.theme.BrokkTheme

class PortableInstallerActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            // Apply your app theme here if you have one, e.g., BrokkTheme { ... }
            BrokkTheme {
                PortableInstaller(
                    onDismiss = { finishAffinity() }
                )
            }
        }
    }
}

