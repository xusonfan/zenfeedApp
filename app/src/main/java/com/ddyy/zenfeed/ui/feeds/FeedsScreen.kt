package com.ddyy.zenfeed.ui.feeds

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Article
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ddyy.zenfeed.data.Feed
import com.ddyy.zenfeed.data.Labels

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FeedItem(feed: Feed, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // 简化卡片设计，减少重绘开销
    Card(
        onClick = onClick,
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
        Column(
            modifier = Modifier.padding(16.dp) // 统一内边距
        ) {
            // 来源信息区域 - 简化设计
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
                    text = feed.labels.source,
                    style = MaterialTheme.typography.labelMedium.copy(
                        fontWeight = FontWeight.Medium
                    ),
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // 标题
            Text(
                text = feed.labels.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold
                ),
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            // 摘要内容处理
            val displayContent = remember(feed.labels.summaryHtmlSnippet, feed.labels.summary) {
                if (feed.labels.summaryHtmlSnippet.isNotBlank()) {
                    feed.labels.summaryHtmlSnippet
                        .replace(Regex("<[^>]*>"), "")
                        .replace("&nbsp;", " ")
                        .replace("&amp;", "&")
                        .replace("&lt;", "<")
                        .replace("&gt;", ">")
                        .replace("&quot;", "\"")
                        .replace("&#39;", "'")
                        .trim()
                } else {
                    feed.labels.summary.trim()
                }
            }
            
            // 显示摘要
            if (displayContent.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = displayContent,
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 3, // 减少行数
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun FeedsList(feeds: List<Feed>, onFeedClick: (Feed) -> Unit, modifier: Modifier = Modifier) {
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Adaptive(minSize = 200.dp), // 调整最小尺寸以减少重排
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(12.dp), // 减少边距
        verticalItemSpacing = 12.dp, // 减少间距
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = feeds,
            key = { feed -> "${feed.labels.title}-${feed.time}" }, // 添加唯一key避免重组
            contentType = { "FeedItem" } // 添加contentType优化
        ) { feed ->
            // 移除动画以减少抖动
            FeedItem(
                feed = feed,
                onClick = { onFeedClick(feed) }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
@Stable
fun FeedsScreen(
    feedsUiState: FeedsUiState,
    onFeedClick: (Feed) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior()

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            // 现代化的顶部应用栏
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = "Zenfeed",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 1.sp
                        ),
                        color = MaterialTheme.colorScheme.primary
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { innerPadding ->
        when (feedsUiState) {
            is FeedsUiState.Loading -> ModernLoadingScreen(Modifier.padding(innerPadding))
            is FeedsUiState.Success -> FeedsList(
                feeds = feedsUiState.feeds,
                onFeedClick = onFeedClick,
                modifier = Modifier.padding(innerPadding)
            )
            is FeedsUiState.Error -> ModernErrorScreen(Modifier.padding(innerPadding))
        }
    }
}

@Composable
fun ModernLoadingScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 现代化加载动画
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(40.dp),
                strokeWidth = 4.dp,
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "精彩内容加载中...",
            style = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "请稍候片刻",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun ModernErrorScreen(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // 现代化错误图标
        Box(
            modifier = Modifier
                .size(80.dp)
                .background(
                    color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.1f),
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ErrorOutline,
                contentDescription = "错误图标",
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.error
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Text(
            text = "内容加载失败",
            style = MaterialTheme.typography.headlineSmall.copy(
                fontWeight = FontWeight.Bold
            ),
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "请检查网络连接后重试",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // 现代化重试按钮
        FilledTonalButton(
            onClick = { /* TODO: 实现重试逻辑 */ },
            modifier = Modifier
                .height(48.dp)
                .widthIn(min = 120.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Refresh,
                contentDescription = null,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "重新加载",
                style = MaterialTheme.typography.labelLarge.copy(
                    fontWeight = FontWeight.SemiBold
                )
            )
        }
    }
}

// 保持原有的预览组件，但使用新的设计
@Preview(showBackground = true)
@Composable
fun FeedItemPreview() {
    MaterialTheme {
        FeedItem(
            feed = Feed(
                labels = Labels(
                    title = "这是一个现代化的示例标题，它展示了全新的设计风格和视觉效果",
                    summary = "这是一个更加精美的摘要展示，采用了现代化的排版和间距设计，提供更好的阅读体验。新的设计包含了渐变背景、圆角卡片和优雅的动画效果，让整个界面看起来更加专业和吸引人。",
                    source = "现代化来源",
                    category = "",
                    content = "",
                    link = "",
                    podcastUrl = "",
                    pubTime = "",
                    summaryHtmlSnippet = "",
                    tags = "",
                    type = ""
                ),
                time = "2023-10-27T12:00:00Z"
            ),
            onClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun FeedsScreenSuccessPreview() {
    MaterialTheme {
        FeedsScreen(
            feedsUiState = FeedsUiState.Success(
                List(8) {
                    Feed(
                        labels = Labels(
                            title = "现代化标题 $it - 展示新的设计风格",
                            summary = "这是第 $it 条内容的现代化摘要信息，采用了全新的视觉设计和排版风格，提供更好的用户体验。",
                            source = "精选来源 $it",
                            category = "",
                            content = "",
                            link = "",
                            podcastUrl = "",
                            pubTime = "",
                            summaryHtmlSnippet = "",
                            tags = "",
                            type = ""
                        ),
                        time = "2023-10-27T12:00:00Z"
                    )
                }
            ),
            onFeedClick = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
fun ModernLoadingScreenPreview() {
    MaterialTheme {
        ModernLoadingScreen()
    }
}

@Preview(showBackground = true)
@Composable
fun ModernErrorScreenPreview() {
    MaterialTheme {
        ModernErrorScreen()
    }
}