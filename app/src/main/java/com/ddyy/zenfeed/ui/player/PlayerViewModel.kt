package com.ddyy.zenfeed.ui.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.IBinder
import android.support.v4.media.session.MediaControllerCompat
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
    
    private val _playbackSpeed = MutableLiveData<Float>()
    val playbackSpeed: LiveData<Float> = _playbackSpeed
    
    private val _playbackSpeedText = MutableLiveData<String>()
    val playbackSpeedText: LiveData<String> = _playbackSpeedText
    
    private val _sleepTimerText = MutableLiveData<String>()
    val sleepTimerText: LiveData<String> = _sleepTimerText
    
    // 添加服务绑定状态跟踪
    private var isServiceBound = false
    private var mediaControllerCallback: MediaControllerCompat.Callback? = null

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
            mediaControllerCallback = object : MediaControllerCompat.Callback() {
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
            updatePlaybackSpeed()
            updateSleepTimer()
            
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
                // 【后台播放修复】先启动前台服务，再绑定服务
                // 问题：原来只是bindService，服务生命周期依赖于绑定，应用退出时服务会被销毁
                // 解决：通过startForegroundService启动独立的前台服务，即使应用退出服务也继续运行
                try {
                    // API level 26+ 使用 startForegroundService，低版本使用 startService
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(intent)
                    } else {
                        context.startService(intent)
                    }
                } catch (e: Exception) {
                    Log.e("PlayerViewModel", "启动前台服务失败", e)
                }
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
     * 【后台播放修复】添加context参数，确保播放时启动前台服务
     */
    fun playPodcastPlaylist(feeds: List<Feed>, startIndex: Int = 0, context: Context) {
        try {
            // 【后台播放修复】确保服务已启动为前台服务
            // 原因：每次播放时都确保服务处于前台模式，防止播放中途被系统杀死
            startPlayerService(context)
            
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
     * 启动播放器服务
     * 【后台播放修复】新增方法，确保播放时服务作为前台服务启动
     * 原因：前台服务具有更高优先级，不会因为内存不足而被系统杀死
     */
    private fun startPlayerService(context: Context) {
        try {
            Log.d("PlayerViewModel", "启动播放器服务")
            val intent = Intent(context, PlayerService::class.java)
            // API level 26+ 使用 startForegroundService，低版本使用 startService
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "启动播放器服务失败", e)
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
     * 切换倍速播放
     */
    fun togglePlaybackSpeed() {
        try {
            playerService?.let { service ->
                service.togglePlaybackSpeed()
                updatePlaybackSpeed()
            }
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "切换倍速播放时出错", e)
        }
    }
    
    /**
     * 切换定时停止
     */
    fun toggleSleepTimer() {
        try {
            playerService?.let { service ->
                service.toggleSleepTimer()
                updateSleepTimer()
            }
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "切换定时停止时出错", e)
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
     * 更新倍速播放信息
     */
    private fun updatePlaybackSpeed() {
        try {
            playerService?.let { service ->
                _playbackSpeed.value = service.getCurrentPlaybackSpeed()
                _playbackSpeedText.value = service.getCurrentPlaybackSpeedText()
            }
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "更新倍速播放信息时出错", e)
        }
    }
    
    /**
     * 更新定时停止信息
     */
    private fun updateSleepTimer() {
        try {
            playerService?.let { service ->
                _sleepTimerText.value = service.getCurrentSleepTimerText()
            }
        } catch (e: Exception) {
            Log.e("PlayerViewModel", "更新定时停止信息时出错", e)
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