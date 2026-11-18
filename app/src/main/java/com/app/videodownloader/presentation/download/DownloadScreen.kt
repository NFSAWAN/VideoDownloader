package com.app.videodownloader.presentation.download

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.edit
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.app.videodownloader.domain.model.DownloadError
import com.app.videodownloader.domain.model.VideoQuality
import com.app.videodownloader.presentation.share.VideoActions

@Composable
fun DownloadScreen(
    viewModel: DownloadViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences("tutorial", Context.MODE_PRIVATE) }
    var showTutorial by remember { mutableStateOf(!prefs.getBoolean("share_seen", false)) }
    if (showTutorial && state == DownloadUiState.Idle) {
        ShareTutorialDialog(onDismiss = {
            prefs.edit { putBoolean("share_seen", true) }
            showTutorial = false
        })
    }
    DownloadContent(
        state = state,
        onFetch = viewModel::onSharedText,
        onQualitySelected = viewModel::onQualitySelected,
        onDownload = viewModel::onDownload,
        onCancel = viewModel::onCancel,
        onRetry = viewModel::onRetry,
        onReset = viewModel::onReset,
        onShare = { VideoActions.share(context, it) },
        onPlay = { VideoActions.play(context, it) },
        modifier = modifier,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun DownloadContent(
    state: DownloadUiState,
    onFetch: (String) -> Unit,
    onQualitySelected: (VideoQuality) -> Unit,
    onDownload: () -> Unit,
    onCancel: () -> Unit,
    onRetry: () -> Unit,
    onReset: () -> Unit,
    onShare: (String) -> Unit,
    onPlay: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        when (state) {
            DownloadUiState.Idle -> PasteLink(error = null, onFetch = onFetch)
            DownloadUiState.InvalidLink ->
                PasteLink(error = "That doesn't look like a valid video link.", onFetch = onFetch)
            is DownloadUiState.LoadingInfo -> {
                CircularProgressIndicator()
                Text("Fetching video info…")
            }
            is DownloadUiState.Ready -> {
                ExpandableText(
                    state.info.title.ifBlank { "Video" },
                    style = MaterialTheme.typography.titleMedium,
                )
                if (state.info.durationSeconds > 0) Text(formatDuration(state.info.durationSeconds.toLong() * 1000))
                Text("Quality", style = MaterialTheme.typography.labelLarge)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    state.info.qualities.forEach { quality ->
                        FilterChip(
                            selected = quality == state.selected,
                            onClick = { onQualitySelected(quality) },
                            label = { Text(quality.height?.let { "${it}p" } ?: "Best") },
                        )
                    }
                }
                Button(onClick = onDownload, modifier = Modifier.fillMaxWidth()) { Text("Download") }
            }
            is DownloadUiState.Downloading -> {
                ExpandableText(
                    state.title.ifBlank { "Downloading…" },
                    style = MaterialTheme.typography.titleMedium,
                )
                if (state.percent > 0f) {
                    LinearProgressIndicator(
                        progress = { state.percent / 100f },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    Text(
                        buildString {
                            append("${state.percent.toInt()}%")
                            state.etaSeconds?.let { append(" · ${it}s left") }
                        },
                    )
                } else {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                OutlinedButton(onClick = onCancel) { Text("Cancel") }
            }
            is DownloadUiState.Completed -> {
                Text("Download complete", style = MaterialTheme.typography.titleMedium)
                ExpandableText(state.video.title)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { onShare(state.video.uri) }) { Text("Share") }
                    OutlinedButton(onClick = { onPlay(state.video.uri) }) { Text("Play") }
                }
                OutlinedButton(onClick = onReset) { Text("Download another") }
            }
            is DownloadUiState.Error -> {
                Text(errorMessage(state.error), color = MaterialTheme.colorScheme.error)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = onRetry) { Text("Try again") }
                    OutlinedButton(onClick = onReset) { Text("Start over") }
                }
            }
        }
    }
}

/** Shows [text] clamped to [collapsedLines] with a More/Less toggle when it doesn't fit. */
@Composable
private fun ExpandableText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyLarge,
    collapsedLines: Int = 3,
) {
    var expanded by rememberSaveable(text) { mutableStateOf(false) }
    var overflows by remember(text) { mutableStateOf(false) }
    Column(modifier) {
        Text(
            text,
            style = style,
            maxLines = if (expanded) Int.MAX_VALUE else collapsedLines,
            overflow = TextOverflow.Ellipsis,
            onTextLayout = { if (!expanded) overflows = it.hasVisualOverflow },
        )
        if (overflows || expanded) {
            TextButton(onClick = { expanded = !expanded }) { Text(if (expanded) "Less" else "More") }
        }
    }
}

@Composable
private fun PasteLink(error: String?, onFetch: (String) -> Unit) {
    var text by rememberSaveable { mutableStateOf("") }
    Text(
        "Share a video link to this app, or paste a link below.",
        style = MaterialTheme.typography.bodyLarge,
    )
    OutlinedTextField(
        value = text,
        onValueChange = { text = it },
        label = { Text("Video link") },
        isError = error != null,
        supportingText = error?.let { { Text(it) } },
        modifier = Modifier.fillMaxWidth(),
    )
    Button(
        onClick = { onFetch(text) },
        enabled = text.isNotBlank(),
        modifier = Modifier.fillMaxWidth(),
    ) { Text("Fetch video") }
    ShareTutorialCard()
}

private fun errorMessage(error: DownloadError): String = when (error) {
    DownloadError.Network -> "Network problem. Check your connection and try again."
    DownloadError.PrivateOrUnavailable -> "This video is private or no longer available. Only public videos can be downloaded."
    DownloadError.Storage -> "Couldn't save the video to your device."
    DownloadError.Unknown -> "Something went wrong while processing this video."
}

internal fun formatDuration(millis: Long): String {
    val totalSeconds = millis / 1000
    val hours = totalSeconds / 3600
    val minutes = totalSeconds % 3600 / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "%d:%02d:%02d".format(hours, minutes, seconds) else "%d:%02d".format(minutes, seconds)
}
