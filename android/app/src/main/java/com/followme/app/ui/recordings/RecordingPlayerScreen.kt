package com.followme.app.ui.recordings

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.datasource.okhttp.OkHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.followme.app.data.repository.RecordingRepository
import com.followme.app.ui.GenericViewModelFactory
import com.followme.app.ui.common.DownloadHelper
import okhttp3.OkHttpClient

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordingPlayerScreen(
    recordingRepository: RecordingRepository,
    okHttpClient: OkHttpClient,
    recordingId: String,
    recordingType: String,
    onNavigateBack: () -> Unit,
) {
    val viewModel: RecordingPlayerViewModel = viewModel(
        factory = GenericViewModelFactory { RecordingPlayerViewModel(recordingRepository, recordingId) }
    )
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current

    val exoPlayer = remember {
        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        ExoPlayer.Builder(context)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory))
            .build()
            .apply {
                setMediaItem(MediaItem.fromUri(viewModel.streamingUrl))
                prepare()
            }
    }
    DisposableEffect(Unit) {
        onDispose { exoPlayer.release() }
    }

    fun performDownload() {
        val extension = if (recordingType == "audio") "aac" else "mp4"
        val mimeType = if (recordingType == "audio") "audio/aac" else "video/mp4"
        val fileName = "FollowMe_$recordingId.$extension"
        val sink = DownloadHelper.openDownloadsOutputStream(context, fileName, mimeType)
        if (sink != null) viewModel.downloadTo(sink)
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted -> if (granted) performDownload() }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Riproduzione") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Indietro")
                    }
                },
            )
        }
    ) { padding ->
        Column(modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp)) {
            AndroidView(
                factory = { PlayerView(it).apply { player = exoPlayer } },
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
            )

            Button(
                onClick = {
                    val needsPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.Q &&
                        ContextCompat.checkSelfPermission(context, Manifest.permission.WRITE_EXTERNAL_STORAGE) !=
                        PackageManager.PERMISSION_GRANTED
                    if (needsPermission) {
                        permissionLauncher.launch(Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    } else {
                        performDownload()
                    }
                },
                enabled = !uiState.isDownloading,
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            ) {
                if (uiState.isDownloading) {
                    CircularProgressIndicator(modifier = Modifier.padding(2.dp))
                } else {
                    Text("Salva sul dispositivo")
                }
            }

            uiState.downloadMessage?.let { message ->
                Text(
                    text = message,
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(top = 12.dp),
                )
            }
        }
    }
}
