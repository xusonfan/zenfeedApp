package com.ddyy.zenfeed.ui.feeds

import android.media.MediaPlayer
import android.util.Log
import android.webkit.WebView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ddyy.zenfeed.data.Feed
import com.ddyy.zenfeed.ui.player.PlayerViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedDetailScreen(
    feed: Feed,
    onBack: () -> Unit,
    onOpenWebView: (String, String) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val playerViewModel: PlayerViewModel = viewModel()
    val context = LocalContext.current
    val isPlaying by playerViewModel.isPlaying.observeAsState(false)
    val playlistInfo by playerViewModel.playlistInfo.observeAsState()
    var showPlaylistDialog by remember { mutableStateOf(false) }

    DisposableEffect(Unit) {
        playerViewModel.bindService(context)
        onDispose {
            playerViewModel.unbindService(context)
        }
    }

    Scaffold(
        floatingActionButton = {
            // 播放控制按钮组
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {

                // 主播放按钮
                FloatingActionButton(onClick = {
                    playerViewModel.playerService?.let {
                        if (isPlaying) {
                            it.pause()
                        } else {
                            if (it.isSameTrack(feed.labels.podcastUrl)) {
                                it.resume()
                            } else {
                                it.play(feed)
                            }
                        }
                    }
                }) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "暂停" else "播放"
                    )
                }

            }
        },
        topBar = {
            TopAppBar(
                title = { Text(text = feed.labels.source) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            onOpenWebView(feed.labels.link, feed.labels.title)
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.OpenInBrowser,
                            contentDescription = "打开原网页"
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = modifier
                .padding(innerPadding)
                .padding(16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = feed.labels.title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "发布于: ${feed.labels.pubTime}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
            
            // 播放列表信息
            playlistInfo?.let { info ->
                if (info.totalCount >= 1) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showPlaylistDialog = true },
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlaylistPlay,
                                    contentDescription = "播放列表",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (info.totalCount > 1) "播放列表: ${info.currentIndex + 1}/${info.totalCount}" else "正在播放",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = if (info.totalCount > 1) {
                                    if (info.isRepeat) "循环播放" else "顺序播放"
                                } else {
                                    "单曲播放"
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HtmlText(html = feed.labels.summaryHtmlSnippet)
        }
        
        // 播放列表弹窗
        if (showPlaylistDialog) {
            PlaylistDialog(
                playerViewModel = playerViewModel,
                onDismiss = { showPlaylistDialog = false }
            )
        }
    }
}

@Composable
fun PlaylistDialog(
    playerViewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    val playlist = remember { playerViewModel.getCurrentPlaylist() }
    val playlistInfo by playerViewModel.playlistInfo.observeAsState()
    
    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                // 弹窗标题栏
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "播放列表 (${playlist.size}首)",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "关闭"
                        )
                    }
                }
                
                Divider()
                
                // 播放列表内容
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    itemsIndexed(playlist) { index, feedItem ->
                        val isCurrentPlaying = playlistInfo?.currentIndex == index
                        
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .clickable {
                                    playerViewModel.playTrackAtIndex(index)
                                    onDismiss()
                                },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrentPlaying) {
                                    MaterialTheme.colorScheme.primaryContainer
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                // 播放状态图标
                                Icon(
                                    imageVector = if (isCurrentPlaying) {
                                        Icons.Default.PlayArrow
                                    } else {
                                        Icons.Default.PlaylistPlay
                                    },
                                    contentDescription = if (isCurrentPlaying) "正在播放" else "播放",
                                    tint = if (isCurrentPlaying) {
                                        MaterialTheme.colorScheme.primary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                                
                                // 曲目信息
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = feedItem.labels.title,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = if (isCurrentPlaying) FontWeight.Bold else FontWeight.Normal,
                                        color = if (isCurrentPlaying) {
                                            MaterialTheme.colorScheme.primary
                                        } else {
                                            MaterialTheme.colorScheme.onSurface
                                        },
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${feedItem.labels.source} • ${feedItem.labels.pubTime}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                                
                                // 序号
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun HtmlText(html: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                loadDataWithBaseURL(null, html, "text/html", "UTF-8", null)
            }
        }
    )
}