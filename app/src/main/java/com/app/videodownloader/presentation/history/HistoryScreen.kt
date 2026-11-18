package com.app.videodownloader.presentation.history

import android.graphics.Bitmap
import android.util.Size
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.videodownloader.domain.model.DownloadedVideo
import com.app.videodownloader.presentation.common.StorageHeader
import com.app.videodownloader.presentation.common.formatSize
import com.app.videodownloader.presentation.download.formatDuration
import com.app.videodownloader.presentation.share.VideoActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun HistoryScreen(
    modifier: Modifier = Modifier,
    viewModel: HistoryViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    var pendingDelete by remember { mutableStateOf<DownloadedVideo?>(null) }

    var largestFirst by rememberSaveable { mutableStateOf(false) }
    val videos = remember(state.videos, largestFirst) {
        if (largestFirst) state.videos.sortedByDescending { it.sizeBytes } else state.videos
    }

    LaunchedEffect(Unit) { viewModel.refresh() }

    Box(modifier = modifier.fillMaxSize()) {
        if (!state.loading && state.videos.isEmpty()) {
            Text(
                "No downloads yet.",
                modifier = Modifier.align(Alignment.Center),
                style = MaterialTheme.typography.bodyLarge,
            )
        } else {
            LazyColumn(
                contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item(key = "header") {
                    StorageHeader(
                        summary = "${state.videos.size} ${if (state.videos.size == 1) "video" else "videos"}" +
                            " · ${formatSize(state.videos.sumOf { it.sizeBytes })} used",
                        largestFirst = largestFirst,
                        onSortChange = { largestFirst = it },
                    )
                }
                items(videos, key = { it.id }) { video ->
                    VideoRow(
                        video = video,
                        onPlay = { VideoActions.play(context, video.uri) },
                        onShare = { VideoActions.share(context, video.uri) },
                        onDelete = { pendingDelete = video },
                    )
                }
            }
        }
    }

    pendingDelete?.let { video ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete video?") },
            text = { Text(video.title) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(video)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun VideoRow(
    video: DownloadedVideo,
    onPlay: () -> Unit,
    onShare: () -> Unit,
    onDelete: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onPlay),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Thumbnail(video.uri, Modifier.size(width = 96.dp, height = 64.dp).clip(MaterialTheme.shapes.small))
        Column(modifier = Modifier.weight(1f)) {
            Text(video.title, maxLines = 2, style = MaterialTheme.typography.bodyLarge)
            Text(
                "${formatDuration(video.durationMs)} · ${formatSize(video.sizeBytes)}",
                style = MaterialTheme.typography.bodyMedium,
            )
        }
        IconButton(onClick = onShare) { Icon(Icons.Default.Share, contentDescription = "Share") }
        IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete") }
    }
}

@Composable
private fun Thumbnail(uri: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(null, uri) {
        value = withContext(Dispatchers.IO) {
            runCatching { context.contentResolver.loadThumbnail(uri.toUri(), Size(320, 200), null) }.getOrNull()
        }
    }
    val image = bitmap
    if (image != null) {
        Image(image.asImageBitmap(), contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}
