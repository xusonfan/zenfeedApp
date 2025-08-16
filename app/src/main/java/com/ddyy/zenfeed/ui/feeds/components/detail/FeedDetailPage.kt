package com.ddyy.zenfeed.ui.feeds.components.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.PlaylistPlay
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ddyy.zenfeed.data.Feed
import com.ddyy.zenfeed.data.PlaylistInfo
import com.ddyy.zenfeed.extension.orDefaultSource
import com.ddyy.zenfeed.extension.orDefaultTitle
import com.ddyy.zenfeed.ui.feeds.components.common.FeedTags
import com.ddyy.zenfeed.ui.player.PlayerViewModel
import kotlin.math.min

@Composable
fun FeedDetailPage(
    feed: Feed,
    playerViewModel: PlayerViewModel,
    playlistInfo: PlaylistInfo?,
    showPlaylistDialog: Boolean,
    onShowPlaylistDialog: (Boolean) -> Unit,
    isPlaying: Boolean,
    onPlayClick: () -> Unit,
    onScrollProgressChanged: (Float) -> Unit = {},
    onTableClick: (String, String) -> Unit = { _, _ -> },
    scrollToTopTrigger: Int = 0,
) {
    // 滚动状态
    val listState = rememberLazyListState()

    // 响应滚动触发器
    LaunchedEffect(scrollToTopTrigger) {
        if (scrollToTopTrigger > 0) {
            listState.animateScrollToItem(0)
        }
    }

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

        // Tags 展示区域和播放按钮在同一行
        item {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // 添加标题和tags之间的间距
                Spacer(modifier = Modifier.height(12.dp))
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Tags 区域
                    FeedTags(
                        feed = feed,
                        maxTags = 4, // 减少一个标签数量为播放按钮留出空间
                        isDetail = true,
                        isRead = feed.isRead,
                        modifier = Modifier.weight(1f)
                    )
                    
                    // 播放按钮 - 作为标签样式
                    Box(
                        modifier = Modifier
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 0.5.dp,
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .clickable { onPlayClick() }
                            .padding(horizontal = 10.dp, vertical = 4.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                contentDescription = if (isPlaying) "暂停" else "播放",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = if (isPlaying) "暂停" else "播放",
                                style = MaterialTheme.typography.labelSmall.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.primary
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
                visible = playlistInfo != null && (playlistInfo.totalCount ?: 0) >= 1 && isPlaying,
                enter = fadeIn(animationSpec = tween(300)) + expandVertically(
                    animationSpec = tween(
                        300
                    )
                ),
                exit = fadeOut(animationSpec = tween(300)) + shrinkVertically(
                    animationSpec = tween(
                        300
                    )
                )
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
            HtmlText(
                html = feed.labels.summaryHtmlSnippet ?: "",
                onTableClick = { tableHtml ->
                    onTableClick(tableHtml, feed.labels.title.orDefaultTitle())
                }
            )
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