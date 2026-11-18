package com.app.videodownloader.presentation.status

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.provider.DocumentsContract
import android.util.LruCache
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.videodownloader.domain.model.StatusItem
import com.app.videodownloader.domain.model.StatusSource
import com.app.videodownloader.presentation.common.StorageHeader
import com.app.videodownloader.presentation.common.formatSize
import com.app.videodownloader.presentation.share.VideoActions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Composable
fun StatusScreen(
    modifier: Modifier = Modifier,
    viewModel: StatusViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHost = remember { SnackbarHostState() }
    var pendingDelete by remember { mutableStateOf<StatusItem?>(null) }

    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) {
        viewModel.onFolderPicked(it?.toString())
    }

    LaunchedEffect(Unit) { viewModel.refresh() }
    LaunchedEffect(state.message) {
        state.message?.let {
            snackbarHost.showSnackbar(it)
            viewModel.messageShown()
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            TabRow(selectedTabIndex = state.tab.ordinal) {
                StatusTab.entries.forEach { tab ->
                    Tab(
                        selected = state.tab == tab,
                        onClick = { viewModel.selectTab(tab) },
                        text = { Text(tab.name) },
                    )
                }
            }
            if (state.tab == StatusTab.Live) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    StatusSource.entries.forEach { source ->
                        FilterChip(
                            selected = state.source == source,
                            onClick = { viewModel.selectSource(source) },
                            label = { Text(source.label) },
                        )
                    }
                }
            }
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                when {
                    state.loading -> Unit
                    !state.hasAccess -> GrantAccess(
                        source = state.source,
                        onGrant = { folderPicker.launch(initialPickerUri(state.source)) },
                    )
                    state.items.isEmpty() -> Text(
                        if (state.tab == StatusTab.Live) {
                            "No statuses found. View a status in ${state.source.label} first."
                        } else {
                            "No saved statuses yet."
                        },
                        modifier = Modifier.align(Alignment.Center).padding(24.dp),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    else -> StatusGrid(
                        items = state.items,
                        saved = state.tab == StatusTab.Saved,
                        showStorage = state.tab == StatusTab.Saved,
                        onOpen = { VideoActions.play(context, it.uri, it.mimeType) },
                        onSave = viewModel::save,
                        onShare = { VideoActions.share(context, it.uri, it.mimeType) },
                        onDelete = { pendingDelete = it },
                    )
                }
            }
        }
        SnackbarHost(snackbarHost, modifier = Modifier.align(Alignment.BottomCenter))
    }

    pendingDelete?.let { item ->
        AlertDialog(
            onDismissRequest = { pendingDelete = null },
            title = { Text("Delete status?") },
            text = { Text(item.name) },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.delete(item)
                    pendingDelete = null
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { pendingDelete = null }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun GrantAccess(source: StatusSource, onGrant: () -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            "To list ${source.label} statuses, allow access to its .Statuses folder.",
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodyLarge,
        )
        Text(
            "The folder picker opens in the right place. Tap \"Use this folder\", then \"Allow\".",
            modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.bodySmall,
        )
        Button(onClick = onGrant) { Text("Grant access") }
    }
}

@Composable
private fun StatusGrid(
    items: List<StatusItem>,
    saved: Boolean,
    showStorage: Boolean,
    onOpen: (StatusItem) -> Unit,
    onSave: (StatusItem) -> Unit,
    onShare: (StatusItem) -> Unit,
    onDelete: (StatusItem) -> Unit,
) {
    var largestFirst by rememberSaveable { mutableStateOf(false) }
    val sorted = remember(items, largestFirst) {
        if (largestFirst) items.sortedByDescending { it.sizeBytes } else items
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(2),
        contentPadding = PaddingValues(16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        if (showStorage) {
            item(key = "header", span = { GridItemSpan(maxLineSpan) }) {
                val photos = items.count { !it.isVideo }
                val videos = items.size - photos
                StorageHeader(
                    summary = "${items.size} ${if (items.size == 1) "file" else "files"}" +
                        " · ${formatSize(items.sumOf { it.sizeBytes })} used" +
                        "\n$photos ${if (photos == 1) "photo" else "photos"} · " +
                        "$videos ${if (videos == 1) "video" else "videos"}",
                    largestFirst = largestFirst,
                    onSortChange = { largestFirst = it },
                )
            }
        }
        items(sorted, key = { it.uri }) { item ->
            Box(
                modifier = Modifier
                    .aspectRatio(0.75f)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable { onOpen(item) },
            ) {
                Thumbnail(item, Modifier.fillMaxSize())
                Text(
                    formatSize(item.sizeBytes),
                    color = Color.White,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .background(Color.Black.copy(alpha = 0.6f), MaterialTheme.shapes.small)
                        .padding(horizontal = 6.dp, vertical = 2.dp),
                )
                if (item.isVideo) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Video",
                        tint = Color.White,
                        modifier = Modifier.align(Alignment.Center),
                    )
                }
                Row(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(4.dp),
                ) {
                    if (saved) {
                        IconButton(onClick = { onShare(item) }) {
                            Icon(Icons.Default.Share, contentDescription = "Share", tint = Color.White)
                        }
                        IconButton(onClick = { onDelete(item) }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.White)
                        }
                    } else {
                        FilledTonalButton(onClick = { onSave(item) }) { Text("Save") }
                    }
                }
            }
        }
    }
}

@Composable
private fun Thumbnail(item: StatusItem, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val bitmap by produceState<Bitmap?>(thumbnails[item.uri], item.uri) {
        value = thumbnails[item.uri] ?: withContext(Dispatchers.IO) {
            runCatching { decodeThumbnail(context, item) }.getOrNull()
        }?.also { thumbnails.put(item.uri, it) }
    }
    val image = bitmap
    if (image != null) {
        Image(image.asImageBitmap(), contentDescription = null, modifier = modifier, contentScale = ContentScale.Crop)
    } else {
        Box(modifier.background(MaterialTheme.colorScheme.surfaceVariant))
    }
}

private val thumbnails = LruCache<String, Bitmap>(64)

private fun decodeThumbnail(context: android.content.Context, item: StatusItem): Bitmap? {
    val uri = item.uri.toUri()
    if (item.isVideo) {
        val retriever = MediaMetadataRetriever()
        return try {
            retriever.setDataSource(context, uri)
            retriever.getFrameAtTime(0)
        } finally {
            retriever.release()
        }
    }
    val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
    context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
    var sample = 1
    while (bounds.outWidth / (sample * 2) >= THUMB_SIZE) sample *= 2
    val options = BitmapFactory.Options().apply { inSampleSize = sample }
    return context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
}

/** Points the folder picker at the `.Statuses` folder so the user only has to confirm. */
private fun initialPickerUri(source: StatusSource): Uri =
    DocumentsContract.buildDocumentUri("com.android.externalstorage.documents", "primary:${source.statusesPath}")

private const val THUMB_SIZE = 400
