package com.ddyy.zenfeed.ui.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.support.v4.media.session.PlaybackStateCompat
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.ddyy.zenfeed.data.Feed
import com.ddyy.zenfeed.data.PlaylistInfo
import com.ddyy.zenfeed.service.PlayerService

class PlayerViewModel : ViewModel() {

    var playerService: PlayerService? by mutableStateOf(null)
        private set

    private val _isPlaying = MutableLiveData<Boolean>()
    val isPlaying: LiveData<Boolean> = _isPlaying
    
    private val _playlistInfo = MutableLiveData<PlaylistInfo>()
    val playlistInfo: LiveData<PlaylistInfo> = _playlistInfo

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as PlayerService.LocalBinder
            playerService = binder.getService()
            val controller = playerService?.mediaSession?.controller
            controller?.registerCallback(object : android.support.v4.media.session.MediaControllerCompat.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                    _isPlaying.value = state?.state == PlaybackStateCompat.STATE_PLAYING
                    // 播放状态变化时更新播放列表信息
                    updatePlaylistInfo()
                }
            })
            // 服务连接后，立即获取并更新当前播放状态
            _isPlaying.value = controller?.playbackState?.state == PlaybackStateCompat.STATE_PLAYING
            updatePlaylistInfo()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playerService = null
            _playlistInfo.value = null
        }
    }

    fun bindService(context: Context) {
        Intent(context, PlayerService::class.java).also { intent ->
            context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }
    }

    fun unbindService(context: Context) {
        context.unbindService(connection)
    }
    
    /**
     * 切换播放/暂停状态
     */
    fun togglePlayPause() {
        try {
            val controller = playerService?.mediaSession?.controller
            val currentState = controller?.playbackState?.state
            
            when (currentState) {
                PlaybackStateCompat.STATE_PLAYING -> {
                    controller.transportControls?.pause()
                }
                PlaybackStateCompat.STATE_PAUSED -> {
                    controller.transportControls?.play()
                }
                else -> {
                    // 如果没有在播放，什么也不做
                    android.util.Log.d("PlayerViewModel", "当前没有可播放的内容")
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("PlayerViewModel", "切换播放暂停时出错", e)
        }
    }
    
    /**
     * 播放播客列表
     */
    fun playPodcastPlaylist(feeds: List<Feed>, startIndex: Int = 0) {
        try {
            // 过滤出有播客URL的Feed
            val podcastFeeds = feeds.filter { it.labels.podcastUrl.isNotBlank() }
            if (podcastFeeds.isNotEmpty()) {
                val validStartIndex = startIndex.coerceIn(0, podcastFeeds.size - 1)
                playerService?.setPlaylist(podcastFeeds, validStartIndex)
                updatePlaylistInfo()
            } else {
                android.util.Log.w("PlayerViewModel", "没有找到有效的播客Feed")
            }
        } catch (e: Exception) {
            android.util.Log.e("PlayerViewModel", "播放播客列表时出错", e)
        }
    }
    
    /**
     * 切换播放模式（顺序/循环）
     */
    fun toggleRepeatMode() {
        try {
            playerService?.let { service ->
                service.setRepeatMode(!service.isRepeatMode())
                updatePlaylistInfo()
            }
        } catch (e: Exception) {
            android.util.Log.e("PlayerViewModel", "切换播放模式时出错", e)
        }
    }
    
    /**
     * 切换乱序播放模式（顺序/乱序）
     */
    fun toggleShuffleMode() {
        try {
            playerService?.let { service ->
                service.setShuffleMode(!service.isShuffleMode())
                updatePlaylistInfo()
            }
        } catch (e: Exception) {
            android.util.Log.e("PlayerViewModel", "切换乱序播放模式时出错", e)
        }
    }
    
    /**
     * 播放下一首
     */
    fun playNext() {
        try {
            playerService?.playNextTrack()
            updatePlaylistInfo()
        } catch (e: Exception) {
            android.util.Log.e("PlayerViewModel", "播放下一首时出错", e)
        }
    }
    
    /**
     * 播放上一首
     */
    fun playPrevious() {
        try {
            playerService?.playPreviousTrack()
            updatePlaylistInfo()
        } catch (e: Exception) {
            android.util.Log.e("PlayerViewModel", "播放上一首时出错", e)
        }
    }
    
    /**
     * 获取当前播放列表
     */
    fun getCurrentPlaylist(): List<Feed> {
        return try {
            playerService?.getCurrentPlaylist() ?: emptyList()
        } catch (e: Exception) {
            android.util.Log.e("PlayerViewModel", "获取播放列表时出错", e)
            emptyList()
        }
    }
    
    /**
     * 播放指定索引的曲目
     */
    fun playTrackAtIndex(index: Int) {
        try {
            playerService?.playTrackAtIndex(index)
            updatePlaylistInfo()
        } catch (e: Exception) {
            android.util.Log.e("PlayerViewModel", "播放指定曲目时出错", e)
        }
    }
    
    /**
     * 更新播放列表信息
     */
    private fun updatePlaylistInfo() {
        try {
            playerService?.let { service ->
                _playlistInfo.value = service.getCurrentPlaylistInfo()
            }
        } catch (e: Exception) {
            android.util.Log.e("PlayerViewModel", "更新播放列表信息时出错", e)
        }
    }
}