package com.ddyy.zenfeed.ui.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
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
    
    // 添加服务绑定状态跟踪
    private var isServiceBound = false
    private var mediaControllerCallback: android.support.v4.media.session.MediaControllerCompat.Callback? = null

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            Log.d("PlayerViewModel", "服务已连接")
            val binder = service as PlayerService.LocalBinder
            playerService = binder.getService()
            isServiceBound = true
            
            val controller = playerService?.mediaSession?.controller
            
            // 移除之前的回调（如果存在）
            mediaControllerCallback?.let { callback ->
                controller?.unregisterCallback(callback)
            }
            
            // 创建新的回调并注册
            mediaControllerCallback = object : android.support.v4.media.session.MediaControllerCompat.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
//                    Log.d("PlayerViewModel", "播放状态变化: ${state?.state}")
                    _isPlaying.value = state?.state == PlaybackStateCompat.STATE_PLAYING
                    // 播放状态变化时更新播放列表信息
                    updatePlaylistInfo()
                }
            }
            
            controller?.registerCallback(mediaControllerCallback!!)
            
            // 服务连接后，立即获取并更新当前播放状态
            val currentState = controller?.playbackState?.state
            _isPlaying.value = currentState == PlaybackStateCompat.STATE_PLAYING
            updatePlaylistInfo()
            
            // 检查服务是否有活跃的播放会话，如果有则确保UI状态正确
            playerService?.let { service ->
                val (isPlaying, isPrepared, currentUrl) = service.getCurrentPlaybackInfo()
                Log.d("PlayerViewModel", "服务状态恢复 - 播放中: $isPlaying, 已准备: $isPrepared, URL: $currentUrl")
                
                if (service.hasActiveSession()) {
                    Log.d("PlayerViewModel", "检测到活跃的播放会话，恢复UI状态")
                    _isPlaying.value = isPlaying
                    updatePlaylistInfo()
                }
            }
            
            Log.d("PlayerViewModel", "当前播放状态: $currentState")
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            Log.d("PlayerViewModel", "服务连接断开")
            // 注意：不要在这里清空播放状态，因为服务可能只是暂时断开
            // 只有在真正停止播放时才清空状态
            playerService = null
            isServiceBound = false
            mediaControllerCallback = null
        }
    }

    fun bindService(context: Context) {
        if (!isServiceBound) {
            Log.d("PlayerViewModel", "绑定播放服务")
            Intent(context, PlayerService::class.java).also { intent ->
                context.bindService(intent, connection, Context.BIND_AUTO_CREATE)
            }
        } else {
            Log.d("PlayerViewModel", "服务已绑定，跳过重复绑定")
        }
    }

    fun unbindService(context: Context) {
        if (isServiceBound) {
            Log.d("PlayerViewModel", "解绑播放服务")
            try {
                // 移除媒体控制器回调
                mediaControllerCallback?.let { callback ->
                    playerService?.mediaSession?.controller?.unregisterCallback(callback)
                }
                context.unbindService(connection)
                isServiceBound = false
            } catch (e: Exception) {
                Log.e("PlayerViewModel", "解绑服务时出错", e)
            }
        }
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
                    Log.d("PlayerViewModel", "当前没有可播放的内容")
                }
            }
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "切换播放暂停时出错", e)
        }
    }
    
    /**
     * 播放播客列表
     */
    fun playPodcastPlaylist(feeds: List<Feed>, startIndex: Int = 0) {
        try {
            // 过滤出有播客URL的Feed
            val podcastFeeds = feeds.filter { !it.labels.podcastUrl.isNullOrBlank() }
            if (podcastFeeds.isNotEmpty()) {
                val validStartIndex = startIndex.coerceIn(0, podcastFeeds.size - 1)
                playerService?.setPlaylist(podcastFeeds, validStartIndex)
                updatePlaylistInfo()
            } else {
                Log.w("PlayerViewModel", "没有找到有效的播客Feed")
            }
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "播放播客列表时出错", e)
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
            Log.e("PlayerViewModel", "切换播放模式时出错", e)
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
            Log.e("PlayerViewModel", "切换乱序播放模式时出错", e)
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
            Log.e("PlayerViewModel", "播放下一首时出错", e)
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
            Log.e("PlayerViewModel", "播放上一首时出错", e)
        }
    }
    
    /**
     * 获取当前播放列表
     */
    fun getCurrentPlaylist(): List<Feed> {
        return try {
            playerService?.getCurrentPlaylist() ?: emptyList()
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "获取播放列表时出错", e)
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
            Log.e("PlayerViewModel", "播放指定曲目时出错", e)
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
            Log.e("PlayerViewModel", "更新播放列表信息时出错", e)
        }
    }
    
    /**
     * ViewModel被清理时的回调
     * 注意：这里不应该解绑服务，因为服务应该独立于ViewModel存在
     */
    override fun onCleared() {
        super.onCleared()
        Log.d("PlayerViewModel", "PlayerViewModel被清理")
        
        // 移除媒体控制器回调，但不解绑服务
        // 服务的生命周期应该由AppNavigation中的DisposableEffect管理
        mediaControllerCallback?.let { callback ->
            playerService?.mediaSession?.controller?.unregisterCallback(callback)
        }
        mediaControllerCallback = null
    }
}