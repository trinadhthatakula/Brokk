package com.valhalla.brokk.presentation.installer

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.valhalla.brokk.domain.InstallState
import kotlinx.coroutines.delay
import org.koin.androidx.compose.koinViewModel

@Composable
fun BrokkInstallerScreen(
    viewModel: InstallerViewModel = koinViewModel()
) {
    val state by viewModel.installState.collectAsState(initial = InstallState.Idle)
    val context = LocalContext.current

    var launchIntent by remember { mutableStateOf<Intent?>(null) }
    val currentPackageName = viewModel.currentPackageName

    LaunchedEffect(Unit) {
        val activity = context as? Activity
        val intent = activity?.intent

        if (state is InstallState.Idle && intent?.action == Intent.ACTION_VIEW) {
            intent.data?.let { uri ->
                viewModel.installFile(uri)
            }
        }
    }

    fun refreshLaunchState() {
        if (currentPackageName != null) {
            launchIntent = context.packageManager.getLaunchIntentForPackage(currentPackageName)
        }
    }

    DisposableEffect(currentPackageName) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context, intent: Intent) {
                if (intent.action == Intent.ACTION_PACKAGE_ADDED ||
                    intent.action == Intent.ACTION_PACKAGE_REPLACED) {

                    val installedPkg = intent.data?.schemeSpecificPart

                    if (installedPkg == currentPackageName) {
                        refreshLaunchState()
                    }
                }
            }
        }

        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }

        context.registerReceiver(receiver, filter)

        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    LaunchedEffect(state) {
        if (state is InstallState.UserConfirmationRequired) {
            val intent = (state as InstallState.UserConfirmationRequired).intent
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        }

        if (state is InstallState.Success) {
            refreshLaunchState()
            if (launchIntent == null) {
                delay(500)
                refreshLaunchState()
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "BROKK",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.Black,
                color = MaterialTheme.colorScheme.onBackground,
                letterSpacing = 4.sp
            )

            Text(
                text = "THE ASSEMBLER",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 48.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                when (val s = state) {

                    is InstallState.ReadyToInstall -> {
                        val meta = s.meta
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            if (meta.icon != null) {
                                Image(
                                    bitmap = meta.icon.asImageBitmap(),
                                    contentDescription = "App Icon",
                                    modifier = Modifier.size(64.dp)
                                )
                            }

                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = meta.label,
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = meta.version,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                                )
                                Text(
                                    text = meta.packageName,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }

                            Button(
                                onClick = { viewModel.confirmInstall() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                )
                            ) {
                                // Dynamic Text based on Install/Update status
                                Text(if (s.isUpdate) "UPDATE" else "INSTALL")
                            }
                        }
                    }

                    is InstallState.Idle -> {
                        IdleView(viewModel)
                    }

                    is InstallState.Parsing -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.height(16.dp))
                            Text(
                                "Analyzing Package...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    is InstallState.Installing -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val percentage = (s.progress * 100).toInt()

                            LinearProgressIndicator(
                                progress = { s.progress },
                                modifier = Modifier.width(200.dp),
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                            Spacer(Modifier.height(16.dp))
                            Text("Assembling: $percentage%", color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }

                    is InstallState.UserConfirmationRequired -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            LinearProgressIndicator(
                                modifier = Modifier.width(150.dp),
                                color = MaterialTheme.colorScheme.tertiary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Text(
                                "Waiting for System Confirmation...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.tertiary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }

                    is InstallState.Success -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                "Installation Complete",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Green,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(16.dp))

                            if (launchIntent != null) {
                                Button(
                                    onClick = {
                                        launchIntent?.let {
                                            it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            context.startActivity(it)
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary
                                    )
                                ) {
                                    Text("Open App")
                                }
                                Spacer(Modifier.height(8.dp))
                            }

                            Button(onClick = {
                                val activity = context as? Activity
                                if (activity?.intent?.action == Intent.ACTION_VIEW) {
                                    activity.finish()
                                } else {
                                    viewModel.resetState()
                                }
                            }) {
                                Text("Done")
                            }
                        }
                    }

                    is InstallState.Error -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                "Failed",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                s.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                            Spacer(Modifier.height(16.dp))
                            Button(
                                onClick = { viewModel.resetState() },
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun IdleView(viewModel: InstallerViewModel) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let { viewModel.installFile(it) }
    }

    Button(
        onClick = {
            launcher.launch(
                arrayOf(
                    "application/vnd.android.package-archive",
                    "application/octet-stream",
                    "application/zip"
                )
            )
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        )
    ) {
        Text("Select File to Install")
    }
}