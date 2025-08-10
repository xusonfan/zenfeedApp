package com.ddyy.zenfeed.ui.feeds

import android.webkit.WebView
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlaylistPlay
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.toColorInt
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
                .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = feed.labels.title,
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "发布于: ${feed.formattedTime}",
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
    val listState = rememberLazyListState()
    
    // 当弹窗打开时，自动滚动到当前播放的项目
    LaunchedEffect(playlistInfo?.currentIndex) {
        playlistInfo?.let { info ->
            if (info.currentIndex >= 0 && info.currentIndex < playlist.size) {
                // 滚动到当前播放项，居中显示
                listState.animateScrollToItem(
                    index = info.currentIndex,
                    scrollOffset = -200 // 负偏移让当前项更靠近顶部
                )
            }
        }
    }
    
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
                    state = listState,
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
                                        text = "${feedItem.labels.source} • ${feedItem.formattedTime}",
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
    val isDarkTheme = isSystemInDarkTheme()
    
    AndroidView(
        modifier = modifier,
        factory = { context ->
            WebView(context).apply {
                // 启用JavaScript支持，以便可以动态设置主题
                settings.javaScriptEnabled = true
                
                // 根据系统主题设置WebView背景色
                setBackgroundColor(if (isDarkTheme)
                    "#1E1E1E".toColorInt()
                else
                    "#FFFFFF".toColorInt()
                )
                
                // 根据主题调整HTML内容
                val themedHtml = if (isDarkTheme) {
                    """
                    <html>
                    <head>
                        <style>
                            body {
                                background-color: #1E1E1E;
                                color: #E0E0E0;
                                font-family: sans-serif;
                                line-height: 1.6;
                            }
                            a {
                                color: #BB86FC;
                            }
                            img {
                                max-width: 100%;
                                height: auto;
                                filter: brightness(0.7) contrast(0.8);
                                opacity: 0.85;
                                border-radius: 4px;
                            }
                            /* 强制覆盖所有元素的背景色和文字颜色 */
                            *, *::before, *::after {
                                background-color: transparent !important;
                                background-image: none !important;
                                color: #E0E0E0 !important;
                                border-color: #444444 !important;
                                box-shadow: none !important;
                                text-shadow: none !important;
                            }
                            /* 特殊处理链接颜色 */
                            a, a * {
                                color: #BB86FC !important;
                            }
                            /* 特殊处理body背景 */
                            body, body * {
                                background-color: #1E1E1E !important;
                            }
                            /* 表格样式 */
                            table {
                                background-color: #2D2D2D !important;
                                color: #E0E0E0 !important;
                                border-collapse: collapse;
                                width: 100%;
                                margin: 16px 0;
                                filter: brightness(0.8) contrast(0.9);
                                border-radius: 4px;
                                overflow: hidden;
                                border: 1px solid #444444 !important;
                            }
                            /* 表格单元格样式 */
                            th, td {
                                background-color: #2D2D2D !important;
                                color: #E0E0E0 !important;
                                border: 1px solid #444444 !important;
                                padding: 8px 12px;
                                text-align: left;
                            }
                            /* 表头样式 */
                            th {
                                background-color: #3A3A3A !important;
                                font-weight: bold;
                            }
                            /* 斑马纹样式 */
                            tr:nth-child(even) {
                                background-color: #252525 !important;
                            }
                            tr:hover {
                                background-color: #333333 !important;
                            }
                            /* 确保所有div和p元素使用深色背景 */
                            div, p, span, section, article {
                                background-color: #2D2D2D !important;
                                color: #E0E0E0 !important;
                                border: 1px solid #444444 !important;
                                border-radius: 4px;
                                padding: 8px;
                                margin: 8px 0;
                            }
                        </style>
                    </head>
                    <body>
                        $html
                    </body>
                    </html>
                    """
                } else {
                    """
                    <html>
                    <head>
                        <style>
                            body {
                                background-color: #FFFFFF;
                                color: #000000;
                                font-family: sans-serif;
                                line-height: 1.6;
                            }
                            a {
                                color: #6650a4;
                            }
                            img {
                                max-width: 100%;
                                height: auto;
                            }
                            table {
                                border-collapse: collapse;
                                width: 100%;
                                margin: 16px 0;
                            }
                            th, td {
                                border: 1px solid #dddddd;
                                padding: 8px 12px;
                                text-align: left;
                            }
                            th {
                                background-color: #f2f2f2;
                                font-weight: bold;
                            }
                            tr:nth-child(even) {
                                background-color: #f9f9f9;
                            }
                            tr:hover {
                                background-color: #f5f5f5;
                            }
                        </style>
                    </head>
                    <body>
                        $html
                    </body>
                    </html>
                    """
                }
                
                loadDataWithBaseURL(null, themedHtml, "text/html", "UTF-8", null)
            }
        },
        update = { webView ->
            // 当主题变化时更新WebView
            webView.setBackgroundColor(if (isDarkTheme)
                "#1E1E1E".toColorInt()
            else
                "#FFFFFF".toColorInt()
            )
        }
    )
}