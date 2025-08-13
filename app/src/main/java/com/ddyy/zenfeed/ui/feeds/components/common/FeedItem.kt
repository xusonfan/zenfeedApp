package com.ddyy.zenfeed.ui.feeds.components.common

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ddyy.zenfeed.data.Feed
import com.ddyy.zenfeed.extension.getDisplayContent
import com.ddyy.zenfeed.extension.getPodcastButtonContainerColor
import com.ddyy.zenfeed.extension.getPodcastButtonContentColor
import com.ddyy.zenfeed.extension.orDefaultSource
import com.ddyy.zenfeed.extension.orDefaultTitle
import com.ddyy.zenfeed.extension.withReadAlpha
import com.ddyy.zenfeed.extension.withReadSummaryAlpha


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedItem(
    feed: Feed,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    onPlayPodcastList: (() -> Unit)? = null,
    onTogglePlayPause: (() -> Unit)? = null,
    isCurrentlyPlaying: Boolean = false,
    isPlaying: Boolean = false
) {
    // 使用 remember 缓存不变的属性，减少重组开销
    val hasValidPodcast = remember(feed.labels.podcastUrl) {
        !feed.labels.podcastUrl.isNullOrBlank()
    }
    val feedTitle = remember(feed.labels.title) {
        feed.labels.title.orDefaultTitle()
    }
    val feedSource = remember(feed.labels.source) {
        feed.labels.source.orDefaultSource()
    }
    // 简化卡片设计，减少重绘开销
    Card(
        modifier = modifier
            .fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 4.dp, // 减少阴影计算
            pressedElevation = 6.dp,
            hoveredElevation = 5.dp
        ),
        shape = RoundedCornerShape(12.dp), // 简化圆角
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Box {
            Column(
                modifier = Modifier
                    .clickable { onClick() }
                    .padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 12.dp) // 减少底部内边距
            ) {
                // 来源信息区域 - 简化设计，恢复原始布局
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // 简化图标
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.Article,
                        contentDescription = "来源图标",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = feedSource,
                        style = MaterialTheme.typography.labelMedium.copy(
                            fontWeight = FontWeight.Medium
                        ),
                        color = MaterialTheme.colorScheme.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = "•",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    )

                    Spacer(modifier = Modifier.width(8.dp))

                    Text(
                        text = feed.formattedTimeShort,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                        maxLines = 1
                    )

                    Spacer(modifier = Modifier.width(8.dp))
                }

                Spacer(modifier = Modifier.height(8.dp))

                // 标题 - 根据阅读状态调整透明度
                Text(
                    text = feedTitle,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold
                    ),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface.withReadAlpha(feed.isRead)
                )

                // 摘要内容处理 - 优化HTML处理性能
                val displayContent = remember(feed.labels.summaryHtmlSnippet, feed.labels.summary) {
                    feed.labels.summaryHtmlSnippet.getDisplayContent(feed.labels.summary)
                }

                // 显示摘要
                if (displayContent.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = displayContent,
                        style = MaterialTheme.typography.bodyMedium,
                        maxLines = 3, // 减少行数
                        overflow = TextOverflow.Ellipsis,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.withReadSummaryAlpha(feed.isRead)
                    )
                }

                // Tags 展示区域 - 使用抽取的组件
                FeedTags(
                    feed = feed,
                    maxTags = 3,
                    isDetail = false,
                    isRead = feed.isRead,
                    modifier = Modifier.fillMaxWidth()
                )

            }

            // 播客播放按钮 - 绝对定位在右上角
            if (hasValidPodcast && (onPlayPodcastList != null || onTogglePlayPause != null)) {
                FilledTonalButton(
                    onClick = {
                        if (isCurrentlyPlaying) {
                            onTogglePlayPause?.invoke()
                        } else {
                            onPlayPodcastList?.invoke()
                        }
                    },
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(top = 8.dp, end = 8.dp)
                        .height(28.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = getPodcastButtonContainerColor(isCurrentlyPlaying),
                        contentColor = getPodcastButtonContentColor(isCurrentlyPlaying)
                    ),
                    contentPadding = PaddingValues(horizontal = 8.dp, vertical = 0.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = if (isCurrentlyPlaying && isPlaying) {
                                Icons.Default.Pause
                            } else {
                                Icons.Default.PlayArrow
                            },
                            contentDescription = if (isCurrentlyPlaying && isPlaying) "暂停播客" else "播放播客",
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = if (isCurrentlyPlaying && isPlaying) "暂停" else "播放",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                }
            }
        }
    }
}

