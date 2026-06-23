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
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
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
import android.net.Uri
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString

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
                text = if (viewModel.isQueueActive) "QUEUE: ${viewModel.queueCurrentIndex} OF ${viewModel.queueTotalCount}" else "THE ASSEMBLER",
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

                            SecurityInspector(meta = meta)

                            Button(
                                onClick = { viewModel.confirmInstall() },
                                modifier = Modifier.fillMaxWidth(),
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

                    is InstallState.CopyingObb -> {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            val percentage = (s.progress * 100).toInt()

                            LinearProgressIndicator(
                                progress = { s.progress },
                                modifier = Modifier.width(200.dp),
                                color = MaterialTheme.colorScheme.secondary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                            Spacer(Modifier.height(16.dp))
                            Text("Extracting expansion (OBB) files: $percentage%", color = MaterialTheme.colorScheme.onSurfaceVariant)
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
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Installation Complete",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Green,
                                fontWeight = FontWeight.Bold
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val hasNext = viewModel.isQueueActive && viewModel.queueCurrentIndex < viewModel.queueTotalCount
                                if (hasNext) {
                                    Button(
                                        onClick = { viewModel.loadNextInQueue() },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Next App")
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = {
                                            if (viewModel.isQueueActive) viewModel.cancelQueue()
                                            val activity = context as? Activity
                                            if (activity?.intent?.action == Intent.ACTION_VIEW) {
                                                activity.finish()
                                            } else {
                                                viewModel.resetState()
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Done")
                                    }
                                }

                                if (launchIntent != null) {
                                    Button(
                                        onClick = {
                                            launchIntent?.let {
                                                it.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                context.startActivity(it)
                                                val activity = context as? Activity
                                                if (activity?.intent?.action == Intent.ACTION_VIEW) {
                                                    activity.finish()
                                                } else {
                                                    viewModel.resetState()
                                                }
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Open")
                                    }
                                }
                            }
                        }
                    }

                    is InstallState.ObbExported -> {
                        val clipboardManager = LocalClipboardManager.current
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "Installed (OBB Exported)",
                                style = MaterialTheme.typography.titleMedium,
                                color = Color.Green,
                                fontWeight = FontWeight.Bold
                            )

                            Text(
                                text = "Android restrictions prevent copying expansion files directly. They have been exported to your Downloads folder.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(
                                        color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.2f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(12.dp)
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = "Action Required:",
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.error,
                                        style = MaterialTheme.typography.labelMedium
                                    )
                                    Text(
                                        text = "Copy folder:\n${s.exportPath}\n\nTo target:\nAndroid/obb/${s.packageName}/",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(s.exportPath))
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Copy Src", fontSize = 11.sp)
                                }

                                OutlinedButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString("Android/obb/${s.packageName}"))
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Copy Dst", fontSize = 11.sp)
                                }
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                OutlinedButton(
                                    onClick = {
                                        try {
                                            val intent = Intent(Intent.ACTION_VIEW).apply {
                                                setDataAndType(Uri.parse("content://com.android.externalstorage.documents/document/primary%3ADownload"), "*/*")
                                                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                            }
                                            context.startActivity(intent)
                                        } catch (e: Exception) {
                                            try {
                                                val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
                                                    type = "*/*"
                                                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                                }
                                                context.startActivity(intent)
                                            } catch (_: Exception) {}
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text("Open Files", fontSize = 11.sp)
                                }

                                val hasNext = viewModel.isQueueActive && viewModel.queueCurrentIndex < viewModel.queueTotalCount
                                Button(
                                    onClick = {
                                        if (hasNext) {
                                            viewModel.loadNextInQueue()
                                        } else {
                                            if (viewModel.isQueueActive) viewModel.cancelQueue()
                                            val activity = context as? Activity
                                            if (activity?.intent?.action == Intent.ACTION_VIEW) {
                                                activity.finish()
                                            } else {
                                                viewModel.resetState()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(if (hasNext) "Next App" else "Done")
                                }
                            }
                        }
                    }

                    is InstallState.Error -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text(
                                "Failed",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                s.message,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(16.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                val hasNext = viewModel.isQueueActive && viewModel.queueCurrentIndex < viewModel.queueTotalCount
                                if (viewModel.isQueueActive) {
                                    OutlinedButton(
                                        onClick = { viewModel.cancelQueue() },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text("Cancel Queue")
                                    }
                                    if (hasNext) {
                                        Button(
                                            onClick = { viewModel.loadNextInQueue() },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Skip & Next")
                                        }
                                    }
                                } else {
                                    Button(
                                        onClick = { viewModel.resetState() },
                                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                                        modifier = Modifier.fillMaxWidth()
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
    }
}

@Composable
fun IdleView(viewModel: InstallerViewModel) {
    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenMultipleDocuments()
    ) { uris ->
        if (uris.isNotEmpty()) {
            if (uris.size == 1) {
                viewModel.installFile(uris.first())
            } else {
                viewModel.installMultipleFiles(uris)
            }
        }
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
        Text("Select Files to Install")
    }
}