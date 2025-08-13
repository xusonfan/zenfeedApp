package com.ddyy.zenfeed.ui.feeds

import android.webkit.WebView
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.OpenInBrowser
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
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
import com.ddyy.zenfeed.data.PlaylistInfo
import com.ddyy.zenfeed.extension.generateTagColors
import com.ddyy.zenfeed.extension.getCardContainerColor
import com.ddyy.zenfeed.extension.getCurrentPlayingIcon
import com.ddyy.zenfeed.extension.getFontWeight
import com.ddyy.zenfeed.extension.getRepeatModeIcon
import com.ddyy.zenfeed.extension.getShuffleModeIcon
import com.ddyy.zenfeed.extension.getTagFontSize
import com.ddyy.zenfeed.extension.getTagTextAlpha
import com.ddyy.zenfeed.extension.getTextColor
import com.ddyy.zenfeed.extension.getThemeBackgroundColor
import com.ddyy.zenfeed.extension.getThemeColorByStatus
import com.ddyy.zenfeed.extension.orDefaultSource
import com.ddyy.zenfeed.extension.orDefaultTitle
import com.ddyy.zenfeed.extension.splitTags
import com.ddyy.zenfeed.extension.toThemedHtml
import com.ddyy.zenfeed.ui.player.PlayerViewModel
import kotlin.math.min

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun FeedDetailScreen(
    allFeeds: List<Feed>,
    initialFeedIndex: Int = 0,
    onBack: () -> Unit,
    onOpenWebView: (String, String) -> Unit = { _, _ -> },
    onPlayPodcastList: ((List<Feed>, Int) -> Unit)? = null,
    onFeedChanged: (Feed) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // 如果没有feeds数据，显示空状态
    if (allFeeds.isEmpty()) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("文章详情") },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "返回"
                            )
                        }
                    }
                )
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text("没有可显示的文章")
            }
        }
        return
    }

    val playerViewModel: PlayerViewModel = viewModel()
    val context = LocalContext.current
    val isPlaying by playerViewModel.isPlaying.observeAsState(false)
    val playlistInfo by playerViewModel.playlistInfo.observeAsState()
    var showPlaylistDialog by remember { mutableStateOf(false) }
    
    // 用于接收滚动进度的状态
    var scrollProgress by remember { mutableStateOf(0f) }
    
    // HorizontalPager状态
    val pagerState = rememberPagerState(
        initialPage = initialFeedIndex.coerceIn(0, allFeeds.size - 1),
        pageCount = { allFeeds.size }
    )
    
    // 当前显示的Feed
    val currentFeed = allFeeds[pagerState.currentPage]
    
    // 监听页面变化，通知父组件当前Feed已改变
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage < allFeeds.size) {
            onFeedChanged(allFeeds[pagerState.currentPage])
        }
    }

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
                    playerViewModel.playerService?.let { it ->
                        if (isPlaying) {
                            it.pause()
                        } else {
                            if (it.isSameTrack(currentFeed.labels.podcastUrl ?: "")) {
                                it.resume()
                            } else {
                                // 优先使用播放列表功能，如果没有提供则使用单曲播放
                                if (onPlayPodcastList != null) {
                                    // 使用全部feeds作为播放列表，从当前feed开始播放
                                    val currentIndex = allFeeds.indexOfFirst {
                                        it.labels.podcastUrl == currentFeed.labels.podcastUrl && !currentFeed.labels.podcastUrl.isNullOrBlank()
                                    }.takeIf { it >= 0 } ?: pagerState.currentPage
                                    onPlayPodcastList(allFeeds, currentIndex)
                                } else {
                                    it.play(currentFeed)
                                }
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
                title = {
                    Column {
                        // 根据滚动进度在来源和标题之间过渡
                        Text(
                            text = if (scrollProgress < 0.5f) {
                                currentFeed.labels.source.orDefaultSource()
                            } else {
                                currentFeed.labels.title.orDefaultTitle()
                            },
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${pagerState.currentPage + 1} / ${allFeeds.size}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
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
                            onOpenWebView(currentFeed.labels.link ?: "", currentFeed.labels.title.orDefaultTitle())
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
        HorizontalPager(
            state = pagerState,
            modifier = modifier.padding(innerPadding)
        ) { page ->
            if (page < allFeeds.size) {
                FeedDetailPage(
                    feed = allFeeds[page],
                    playerViewModel = playerViewModel,
                    playlistInfo = playlistInfo,
                    showPlaylistDialog = showPlaylistDialog,
                    onShowPlaylistDialog = { showPlaylistDialog = it },
                    onScrollProgressChanged = { progress ->
                        scrollProgress = progress
                    }
                )
            }
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
                
                HorizontalDivider()
                
                // 播放控制按钮行
                playlistInfo?.let { info ->
                    if (info.totalCount > 1) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 循环播放按钮
                            IconButton(
                                onClick = { playerViewModel.toggleRepeatMode() },
                                modifier = Modifier
                                    .size(40.dp)
                            ) {
                                Icon(
                                    imageVector = getRepeatModeIcon(info.isRepeat),
                                    contentDescription = "循环播放",
                                    tint = getThemeColorByStatus(info.isRepeat),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            // 循环模式文字标签
                            Text(
                                text = if (info.isRepeat) "循环" else "顺序",
                                style = MaterialTheme.typography.bodySmall,
                                color = getThemeColorByStatus(info.isRepeat)
                            )
                            
                            Spacer(modifier = Modifier.width(24.dp))
                            
                            // 乱序播放按钮
                            IconButton(
                                onClick = { playerViewModel.toggleShuffleMode() },
                                modifier = Modifier
                                    .size(40.dp)
                            ) {
                                Icon(
                                    imageVector = getShuffleModeIcon(info.isShuffle),
                                    contentDescription = "乱序播放",
                                    tint = getThemeColorByStatus(info.isShuffle),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            
                            // 乱序模式文字标签
                            Text(
                                text = if (info.isShuffle) "乱序" else "顺序",
                                style = MaterialTheme.typography.bodySmall,
                                color = getThemeColorByStatus(info.isShuffle)
                            )
                        }
                        
                        HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
                
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
                                containerColor = getCardContainerColor(isCurrentPlaying)
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
                                    imageVector = getCurrentPlayingIcon(isCurrentPlaying),
                                    contentDescription = if (isCurrentPlaying) "正在播放" else "播放",
                                    tint = getThemeColorByStatus(isCurrentPlaying),
                                    modifier = Modifier.size(20.dp)
                                )
                                
                                // 曲目信息
                                Column(
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(
                                        text = feedItem.labels.title.orDefaultTitle(),
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = getFontWeight(isCurrentPlaying),
                                        color = getTextColor(isCurrentPlaying),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${feedItem.labels.source.orDefaultSource()} • ${feedItem.formattedTime}",
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
fun FeedDetailPage(
    feed: Feed,
    playerViewModel: PlayerViewModel,
    playlistInfo: PlaylistInfo?,
    showPlaylistDialog: Boolean,
    onShowPlaylistDialog: (Boolean) -> Unit,
    onScrollProgressChanged: (Float) -> Unit = {}
) {
    // 滚动状态
    val listState = rememberLazyListState()
    
    // 计算滚动进度（用于顶栏过渡效果）
    val scrollProgress by remember {
        derivedStateOf {
            val firstVisibleItemIndex = listState.firstVisibleItemIndex
            val firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset
            
            // 当滚动到第一个item（标题）之后开始过渡
            val threshold = 200f // 滚动200px后开始过渡
            val progress = if (firstVisibleItemIndex == 0) {
                min(firstVisibleItemScrollOffset / threshold, 1f)
            } else {
                1f
            }
            progress
        }
    }
    
    // 当滚动进度变化时通知父组件
    LaunchedEffect(scrollProgress) {
        onScrollProgressChanged(scrollProgress)
    }

    LazyColumn(
        state = listState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            bottom = 16.dp
        )
    ) {
        // 文章标题
        item {
            Text(
                text = feed.labels.title.orDefaultTitle(),
                style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                modifier = Modifier.padding(top = 16.dp)
            )
        }
        
        // Tags 展示区域
        item {
            val displayTags = remember(feed.labels.tags) {
                feed.labels.tags?.splitTags(5) ?: emptyList() // 详情页可以显示更多标签
            }
            
            if (displayTags.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    displayTags.forEachIndexed { index, tag ->
                        // 根据标签内容生成颜色
                        val (backgroundColor, borderColor, textColor) = tag.generateTagColors()
                        
                        Box(
                            modifier = Modifier
                                .background(
                                    color = backgroundColor,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .border(
                                    width = 0.5.dp,
                                    color = borderColor,
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tag,
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontSize = getTagFontSize(isDetail = true)
                                ),
                                color = textColor.copy(alpha = getTagTextAlpha(false, isDetail = true)),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
        
        // 来源和发布时间
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "${feed.labels.source.orDefaultSource()} • 发布于: ${feed.formattedTime}",
                style = MaterialTheme.typography.labelMedium,
                color = Color.Gray
            )
        }
        
        // 播放列表信息 - 使用动画避免闪烁
        item {
            AnimatedVisibility(
                visible = playlistInfo != null && (playlistInfo?.totalCount ?: 0) >= 1,
                enter = fadeIn(animationSpec = tween(300)) + expandVertically(animationSpec = tween(300)),
                exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(animationSpec = tween(300))
            ) {
                playlistInfo?.let { info ->
                    Column {
                        Spacer(modifier = Modifier.height(8.dp))
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onShowPlaylistDialog(true) },
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
                                        imageVector = Icons.AutoMirrored.Filled.PlaylistPlay,
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
                                        when {
                                            info.isShuffle && info.isRepeat -> "乱序循环"
                                            info.isShuffle -> "乱序播放"
                                            info.isRepeat -> "循环播放"
                                            else -> "顺序播放"
                                        }
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
            }
        }
        
        // 文章内容
        item {
            Spacer(modifier = Modifier.height(16.dp))
            HtmlText(html = feed.labels.summaryHtmlSnippet ?: "")
        }
    }
    
    // 播放列表弹窗
    if (showPlaylistDialog) {
        PlaylistDialog(
            playerViewModel = playerViewModel,
            onDismiss = { onShowPlaylistDialog(false) }
        )
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
                setBackgroundColor(getThemeBackgroundColor(isDarkTheme))
                
                // 根据主题调整HTML内容
                val themedHtml = html.toThemedHtml(isDarkTheme)
                
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