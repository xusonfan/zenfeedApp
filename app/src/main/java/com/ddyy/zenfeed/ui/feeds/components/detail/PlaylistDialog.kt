package com.ddyy.zenfeed.ui.feeds.components.detail

import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.ddyy.zenfeed.extension.getCardContainerColor
import com.ddyy.zenfeed.extension.getCurrentPlayingIcon
import com.ddyy.zenfeed.extension.getFontWeight
import com.ddyy.zenfeed.extension.getRepeatModeIcon
import com.ddyy.zenfeed.extension.getShuffleModeIcon
import com.ddyy.zenfeed.extension.getTextColor
import com.ddyy.zenfeed.extension.getThemeColorByStatus
import com.ddyy.zenfeed.extension.orDefaultSource
import com.ddyy.zenfeed.extension.orDefaultTitle
import com.ddyy.zenfeed.ui.player.PlayerViewModel

@Composable
fun PlaylistDialog(
    playerViewModel: PlayerViewModel,
    onDismiss: () -> Unit
) {
    val playlist = remember { playerViewModel.getCurrentPlaylist() }
    val playlistInfo by playerViewModel.playlistInfo.observeAsState()
    val playbackSpeedText by playerViewModel.playbackSpeedText.observeAsState("1.0x")
    val sleepTimerText by playerViewModel.sleepTimerText.observeAsState("关闭")
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
                            horizontalArrangement = Arrangement.SpaceAround,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // 循环播放按钮和文字组合
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { playerViewModel.toggleRepeatMode() }
                            ) {
                                Icon(
                                    imageVector = getRepeatModeIcon(info.isRepeat),
                                    contentDescription = "循环播放",
                                    tint = getThemeColorByStatus(info.isRepeat),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (info.isRepeat) "循环" else "顺序",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = getThemeColorByStatus(info.isRepeat)
                                )
                            }


                            // 乱序播放按钮和文字组合
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { playerViewModel.toggleShuffleMode() }
                            ) {
                                Icon(
                                    imageVector = getShuffleModeIcon(info.isShuffle),
                                    contentDescription = "乱序播放",
                                    tint = getThemeColorByStatus(info.isShuffle),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = if (info.isShuffle) "乱序" else "顺序",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = getThemeColorByStatus(info.isShuffle)
                                )
                            }


                            // 倍速播放按钮和文字组合
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { playerViewModel.togglePlaybackSpeed() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Speed,
                                    contentDescription = "倍速播放",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = playbackSpeedText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
    
    
                            // 定时停止按钮和文字组合
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { playerViewModel.toggleSleepTimer() }
                            ) {
                                val isSleepTimerOn = sleepTimerText != "关闭"
                                Icon(
                                    imageVector = Icons.Default.Timer,
                                    contentDescription = "定时停止",
                                    tint = getThemeColorByStatus(isSleepTimerOn),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = sleepTimerText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = getThemeColorByStatus(isSleepTimerOn)
                                )
                            }
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