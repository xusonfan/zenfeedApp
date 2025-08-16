package com.ddyy.zenfeed.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Binder
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.support.v4.media.MediaMetadataCompat
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.graphics.drawable.IconCompat
import androidx.media.session.MediaButtonReceiver
import com.ddyy.zenfeed.data.FaviconManager
import com.ddyy.zenfeed.data.Feed
import com.ddyy.zenfeed.data.PlaylistInfo
import com.ddyy.zenfeed.data.network.ApiClient
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream

class PlayerService : Service(), AudioManager.OnAudioFocusChangeListener {

    var mediaSession: MediaSessionCompat? = null
    private var mediaPlayer: MediaPlayer? = null
    private val binder = LocalBinder()
    private lateinit var faviconManager: FaviconManager
    private var playlist: List<Feed> = emptyList()
    private var currentTrackIndex = -1
    private var isPrepared = false
    private var currentTrackFavicon: Bitmap? = null
    var currentTrackUrl: String? = null
        private set
    
    // 音频焦点管理
    private lateinit var audioManager: AudioManager
    private var audioFocusRequest: AudioFocusRequest? = null
    private var hasAudioFocus = false
    private var playbackNeedsToResume = false // 标记是否需要在获得音频焦点后恢复播放
    
    // 预加载相关变量
    private var preloadMediaPlayer: MediaPlayer? = null
    private var preloadedTrackIndex = -1
    private var isPreloadPrepared = false
    private var preloadTrackUrl: String? = null
    
    // 播放模式：false为顺序播放，true为循环播放
    private var isRepeatMode = true
    // 乱序播放模式：false为顺序播放，true为乱序播放
    private var isShuffleMode = false
    // 原始播放列表和乱序后的列表
    private var originalPlaylist: List<Feed> = emptyList()
    private var shuffledIndices: List<Int> = emptyList()
    
    // 倍速播放相关
    private val playbackSpeeds = listOf(0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    private var currentSpeedIndex = 1 // 默认1.0倍速
    
    // 定时停止相关
    private val sleepTimerOptions = listOf(0, 15, 30, 60) // 单位：分钟，0表示关闭
    private var currentSleepTimerIndex = 0 // 默认关闭
    private var sleepTimerRunnable: Runnable? = null
    private var sleepTimerEndTime: Long = 0 // 定时停止结束时间

    private val handler = Handler(Looper.getMainLooper())
    private val progressUpdateRunnable = object : Runnable {
        override fun run() {
            if (mediaPlayer?.isPlaying == true) {
                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                handler.postDelayed(this, 1000)
            }
        }
    }

    inner class LocalBinder : Binder() {
        fun getService(): PlayerService = this@PlayerService
    }

    override fun onBind(intent: Intent): IBinder {
        return binder
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.let {
            MediaButtonReceiver.handleIntent(mediaSession, it)
        }
        // 使用START_STICKY确保服务在系统资源紧张时被杀死后能自动重启
        // 使用START_NOT_STICKY可以避免在配置变更时不必要的重启
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        Log.d("PlayerService", "PlayerService onCreate")
        createNotificationChannel()
        faviconManager = FaviconManager(this)
        
        // 初始化AudioManager
        audioManager = getSystemService(Context.AUDIO_SERVICE) as AudioManager
        
        // 创建MediaSession，确保它在整个服务生命周期中保持活跃
        mediaSession = MediaSessionCompat(this, "PlayerService").apply {
            setCallback(object : MediaSessionCompat.Callback() {
                override fun onPlay() {
                    try {
                        Log.d("PlayerService", "MediaSession收到播放命令")
                        resume()
                    } catch (e: Exception) {
                        Log.e("PlayerService", "播放时出错", e)
                    }
                }

                override fun onPause() {
                    try {
                        Log.d("PlayerService", "MediaSession收到暂停命令")
                        pause()
                    } catch (e: Exception) {
                        Log.e("PlayerService", "暂停时出错", e)
                    }
                }

                override fun onSkipToNext() {
                    try {
                        Log.d("PlayerService", "MediaSession收到下一首命令")
                        if (playlist.isNotEmpty() && (isRepeatMode || hasNextTrack())) {
                            playNextTrack()
                        } else {
                            Log.d("PlayerService", "没有下一首可播放")
                        }
                    } catch (e: Exception) {
                        Log.e("PlayerService", "播放下一首时出错", e)
                    }
                }

                override fun onSkipToPrevious() {
                    try {
                        Log.d("PlayerService", "MediaSession收到上一首命令")
                        if (playlist.isNotEmpty() && (isRepeatMode || hasPreviousTrack())) {
                            playPreviousTrack()
                        } else {
                            Log.d("PlayerService", "没有上一首可播放")
                        }
                    } catch (e: Exception) {
                        Log.e("PlayerService", "播放上一首时出错", e)
                    }
                }
            })
            
            // 设置MediaSession为活跃状态，确保它能接收媒体按钮事件
            isActive = true
            
            // 设置初始播放状态
            setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setActions(
                        PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
                    )
                    .setState(PlaybackStateCompat.STATE_NONE, 0L, 0.0f)
                    .build()
            )
        }
        
        Log.d("PlayerService", "MediaSession创建完成，isActive: ${mediaSession?.isActive}")
        
        // 【ANR修复】立即启动前台服务，避免startForegroundService()超时
        // Android要求使用startForegroundService()启动的服务必须在5秒内调用startForeground()
        startForegroundWithDefaultNotification()
    }

    fun setPlaylist(feeds: List<Feed>, startIndex: Int) {
        originalPlaylist = feeds
        playlist = feeds
        currentTrackIndex = startIndex
        
        // 如果是乱序模式，重新生成乱序索引
        if (isShuffleMode) {
            generateShuffledIndices(startIndex)
        }
        
        playCurrentTrack()
    }

    private fun playCurrentTrack() {
        try {
            Log.d("PlayerService", "播放当前曲目，索引: $currentTrackIndex, 播放列表大小: ${playlist.size}")
            
            if (playlist.isEmpty()) {
                Log.w("PlayerService", "播放列表为空")
                return
            }
            
            if (currentTrackIndex < 0 || currentTrackIndex >= playlist.size) {
                Log.w("PlayerService", "无效的曲目索引: $currentTrackIndex")
                return
            }
            
            val currentFeed = playlist[currentTrackIndex]
            if (currentFeed.labels.podcastUrl.isNullOrBlank()) {
                Log.w("PlayerService", "当前曲目没有播客URL")
                return
            }
            
            playInternal(currentFeed.labels.podcastUrl)
        } catch (e: Exception) {
            Log.e("PlayerService", "播放当前曲目时发生异常", e)
        }
    }

    fun play(feed: Feed) {
        playlist = listOf(feed)
        currentTrackIndex = 0
        playInternal(feed.labels.podcastUrl ?: "")
    }

    private fun playInternal(url: String) {
        currentTrackUrl = url
        isPrepared = false
        currentTrackFavicon = null // 为新曲目重置图标
        stopProgressUpdate()

        // 在后台获取图标
        val track = playlist.getOrNull(currentTrackIndex)
        if (track != null) {
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val favicon = track.labels.link?.let { faviconManager.getFavicon(it) }
                    withContext(Dispatchers.Main) {
                        currentTrackFavicon = favicon
                        // 获取到图标后，更新一次通知
                        updateNotification()
                    }
                } catch (e: Exception) {
                    Log.e("PlayerService", "获取favicon失败", e)
                }
            }
        }
        
        /**
         * 后台播放修复：确保服务作为前台服务启动
         *
         * 问题：之前服务只是通过 bindService 绑定，当应用退出时容易被系统杀死导致播放停止
         * 解决：在开始播放时立即启动前台服务，确保即使应用退出也能继续播放
         */
        startAsForegroundService()
        
        // 请求音频焦点
        if (!requestAudioFocus()) {
            Log.w("PlayerService", "无法获取音频焦点，停止播放")
            return
        }
        
        // 检查是否已经预加载了这个文件
        if (preloadTrackUrl == url && isPreloadPrepared && preloadMediaPlayer != null) {
            Log.d("PlayerService", "使用预加载的媒体文件: $url")
            
            // 将预加载的播放器设为当前播放器
            mediaPlayer?.release()
            mediaPlayer = preloadMediaPlayer
            preloadMediaPlayer = null
            isPrepared = true
            isPreloadPrepared = false
            preloadTrackUrl = null
            preloadedTrackIndex = -1
            
            // 重要修复：为预加载的播放器设置onCompletionListener和onErrorListener
            mediaPlayer?.apply {
                setOnCompletionListener {
                    stopProgressUpdate()
                    updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
                    // 根据播放模式决定是否播放下一首
                    if (isRepeatMode || hasNextTrack()) {
                        playNextTrack()
                    } else {
                        // 播放列表结束，停止播放
                        Log.d("PlayerService", "播放列表播放完毕")
                    }
                }
                setOnErrorListener { _, what, extra ->
                    Log.e("PlayerService", "预加载播放器播放错误: what=$what, extra=$extra")
                    stopProgressUpdate()
                    updatePlaybackState(PlaybackStateCompat.STATE_ERROR)
                    isPrepared = false
                    true
                }
            }
            
            // 设置元数据
            val track = playlist.getOrNull(currentTrackIndex)
            val duration = mediaPlayer?.duration?.toLong() ?: 0L
            val metadata = MediaMetadataCompat.Builder()
                .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track?.labels?.title ?: "未知标题")
                .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track?.labels?.source ?: "未知来源")
                .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                .build()
            mediaSession?.setMetadata(metadata)
            
            // 开始播放
            mediaPlayer?.start()
            Log.d("PlayerService", "开始播放预加载的文件: $url")
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
            startProgressUpdate()
            
            // 应用当前的播放速度
            applyCurrentPlaybackSpeed()
            
            // 开始预加载下一个播客
            preloadNextTrack()
            return
        }
        
        mediaPlayer?.release()
        
        Log.d("PlayerService", "准备播放: $url")
        updatePlaybackState(PlaybackStateCompat.STATE_BUFFERING)
        
        // 使用协程在后台下载媒体文件（支持代理）
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val localFile = downloadMediaFile(url)
                
                // 在主线程设置MediaPlayer
                withContext(Dispatchers.Main) {
                    mediaPlayer = MediaPlayer().apply {
                        try {
                            setDataSource(localFile.absolutePath)
                            setOnPreparedListener { mediaPlayer ->
                                isPrepared = true
                                val track = playlist.getOrNull(currentTrackIndex)
                                val duration = mediaPlayer.duration.toLong()

                                val metadata = MediaMetadataCompat.Builder()
                                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track?.labels?.title ?: "未知标题")
                                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track?.labels?.source ?: "未知来源")
                                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                                    .build()
                                mediaSession?.setMetadata(metadata)

                                start()
                                Log.d("PlayerService", "开始播放: $url")
                                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                                startProgressUpdate()
                                
                                // 应用当前的播放速度
                                applyCurrentPlaybackSpeed()
                                
                                // 开始预加载下一个播客
                                preloadNextTrack()
                            }
                            setOnCompletionListener {
                                stopProgressUpdate()
                                updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
                                // 根据播放模式决定是否播放下一首
                                if (isRepeatMode || hasNextTrack()) {
                                    playNextTrack()
                                } else {
                                    // 播放列表结束，停止播放
                                    Log.d("PlayerService", "播放列表播放完毕")
                                }
                            }
                            setOnErrorListener { _, what, extra ->
                                Log.e("PlayerService", "播放错误: what=$what, extra=$extra")
                                stopProgressUpdate()
                                updatePlaybackState(PlaybackStateCompat.STATE_ERROR)
                                updateNotification()
                                isPrepared = false
                                true
                            }
                            prepareAsync()
                        } catch (e: Exception) {
                            Log.e("PlayerService", "设置数据源时出错", e)
                            updatePlaybackState(PlaybackStateCompat.STATE_ERROR)
                        }
                    }
                }
            } catch (e: Exception) {
                Log.e("PlayerService", "下载媒体文件失败", e)
                withContext(Dispatchers.Main) {
                    updatePlaybackState(PlaybackStateCompat.STATE_ERROR)
                }
            }
        }
    }
    
    /**
     * 通过代理下载媒体文件到本地缓存
     */
    private suspend fun downloadMediaFile(url: String): File {
        return withContext(Dispatchers.IO) {
            Log.d("PlayerService", "开始通过代理下载媒体文件: $url")
            
            // 使用配置了代理的 OkHttp 客户端
            val client = ApiClient.getHttpClient(this@PlayerService)
            
            val request = Request.Builder()
                .url(url)
                .build()
            
            val response = client.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw Exception("下载失败: ${response.code}")
            }
            
            // 创建缓存目录
            val cacheDir = File(cacheDir, "media_cache")
            if (!cacheDir.exists()) {
                cacheDir.mkdirs()
            }
            
            // 生成文件名（基于URL的hash值）
            val fileName = "${url.hashCode()}.tmp"
            val localFile = File(cacheDir, fileName)
            
            // 如果文件已存在且大小合理，直接使用
            if (localFile.exists() && localFile.length() > 0) {
                Log.d("PlayerService", "使用缓存的媒体文件: ${localFile.absolutePath}")
                return@withContext localFile
            }
            
            // 下载文件
            response.body.let { responseBody ->
                FileOutputStream(localFile).use { output ->
                    responseBody.byteStream().use { input ->
                        input.copyTo(output)
                    }
                }
                Log.d("PlayerService", "媒体文件下载完成: ${localFile.absolutePath}, 大小: ${localFile.length()} 字节")
            }
            
            localFile
        }
    }

    fun playNextTrack() {
        try {
            Log.d("PlayerService", "尝试播放下一首，当前索引: $currentTrackIndex, 播放列表大小: ${playlist.size}")
            
            if (playlist.isEmpty()) {
                Log.w("PlayerService", "播放列表为空，无法播放下一首")
                return
            }
            
            if (isShuffleMode) {
                // 乱序模式
                val currentShuffledIndex = shuffledIndices.indexOf(currentTrackIndex)
                if (currentShuffledIndex != -1) {
                    val nextShuffledIndex = if (isRepeatMode) {
                        (currentShuffledIndex + 1) % shuffledIndices.size
                    } else {
                        if (currentShuffledIndex < shuffledIndices.size - 1) {
                            currentShuffledIndex + 1
                        } else {
                            -1 // 已是最后一首
                        }
                    }
                    
                    if (nextShuffledIndex != -1) {
                        currentTrackIndex = shuffledIndices[nextShuffledIndex]
                        Log.d("PlayerService", "乱序模式，播放索引: $currentTrackIndex")
                        playCurrentTrack()
                    } else {
                        Log.d("PlayerService", "乱序播放已是最后一首，播放结束")
                        updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
                    }
                }
            } else if (isRepeatMode) {
                // 循环模式：播放到最后一首后回到第一首
                currentTrackIndex = (currentTrackIndex + 1) % playlist.size
                Log.d("PlayerService", "循环模式，播放索引: $currentTrackIndex")
                playCurrentTrack()
            } else {
                // 顺序模式：播放到最后一首后停止
                if (hasNextTrack()) {
                    currentTrackIndex++
                    Log.d("PlayerService", "顺序模式，播放下一首，索引: $currentTrackIndex")
                    playCurrentTrack()
                } else {
                    Log.d("PlayerService", "已是最后一首，播放结束")
                    updatePlaybackState(PlaybackStateCompat.STATE_STOPPED)
                }
            }
        } catch (e: Exception) {
            Log.e("PlayerService", "播放下一首时发生异常", e)
        }
    }

    fun playPreviousTrack() {
        try {
            Log.d("PlayerService", "尝试播放上一首，当前索引: $currentTrackIndex, 播放列表大小: ${playlist.size}")
            
            if (playlist.isEmpty()) {
                Log.w("PlayerService", "播放列表为空，无法播放上一首")
                return
            }
            
            if (isShuffleMode) {
                // 乱序模式
                val currentShuffledIndex = shuffledIndices.indexOf(currentTrackIndex)
                if (currentShuffledIndex != -1) {
                    val prevShuffledIndex = if (isRepeatMode) {
                        if (currentShuffledIndex > 0) {
                            currentShuffledIndex - 1
                        } else {
                            shuffledIndices.size - 1
                        }
                    } else {
                        if (currentShuffledIndex > 0) {
                            currentShuffledIndex - 1
                        } else {
                            -1 // 已是第一首
                        }
                    }
                    
                    if (prevShuffledIndex != -1) {
                        currentTrackIndex = shuffledIndices[prevShuffledIndex]
                        Log.d("PlayerService", "乱序模式，播放索引: $currentTrackIndex")
                        playCurrentTrack()
                    } else {
                        Log.d("PlayerService", "乱序播放已是第一首，无法播放上一首")
                    }
                }
            } else if (isRepeatMode) {
                // 循环模式：播放到第一首后回到最后一首
                currentTrackIndex = if (currentTrackIndex > 0) currentTrackIndex - 1 else playlist.size - 1
                Log.d("PlayerService", "循环模式，播放索引: $currentTrackIndex")
                playCurrentTrack()
            } else {
                // 顺序模式：播放到第一首后停止
                if (hasPreviousTrack()) {
                    currentTrackIndex--
                    Log.d("PlayerService", "顺序模式，播放上一首，索引: $currentTrackIndex")
                    playCurrentTrack()
                } else {
                    Log.d("PlayerService", "已是第一首，无法播放上一首")
                }
            }
        } catch (e: Exception) {
            Log.e("PlayerService", "播放上一首时发生异常", e)
        }
    }
    
    /**
     * 检查是否有下一首
     */
    fun hasNextTrack(): Boolean {
        return try {
            if (playlist.isEmpty()) return false
            
            if (isShuffleMode) {
                val currentShuffledIndex = shuffledIndices.indexOf(currentTrackIndex)
                return if (isRepeatMode) {
                    true // 循环模式总是有下一首
                } else {
                    currentShuffledIndex >= 0 && currentShuffledIndex < shuffledIndices.size - 1
                }
            } else {
                return if (isRepeatMode) {
                    true // 循环模式总是有下一首
                } else {
                    currentTrackIndex >= 0 && currentTrackIndex < playlist.size - 1
                }
            }
        } catch (e: Exception) {
            Log.e("PlayerService", "检查下一首时出错", e)
            false
        }
    }
    
    /**
     * 检查是否有上一首
     */
    fun hasPreviousTrack(): Boolean {
        return try {
            if (playlist.isEmpty()) return false
            
            if (isShuffleMode) {
                val currentShuffledIndex = shuffledIndices.indexOf(currentTrackIndex)
                return if (isRepeatMode) {
                    true // 循环模式总是有上一首
                } else {
                    currentShuffledIndex > 0
                }
            } else {
                return if (isRepeatMode) {
                    true // 循环模式总是有上一首
                } else {
                    currentTrackIndex > 0 && currentTrackIndex < playlist.size
                }
            }
        } catch (e: Exception) {
            Log.e("PlayerService", "检查上一首时出错", e)
            false
        }
    }
    
    /**
     * 设置播放模式
     * @param repeat true为循环播放，false为顺序播放
     */
    fun setRepeatMode(repeat: Boolean) {
        isRepeatMode = repeat
        Log.d("PlayerService", "播放模式设置为: ${if (repeat) "循环播放" else "顺序播放"}")
    }
    
    /**
     * 获取当前播放模式
     */
    fun isRepeatMode(): Boolean = isRepeatMode
    
    /**
     * 设置乱序播放模式
     * @param shuffle true为乱序播放，false为顺序播放
     */
    fun setShuffleMode(shuffle: Boolean) {
        if (isShuffleMode != shuffle) {
            isShuffleMode = shuffle
            if (shuffle && playlist.isNotEmpty()) {
                // 开启乱序模式，生成乱序索引
                generateShuffledIndices(currentTrackIndex)
            }
            Log.d("PlayerService", "乱序播放模式设置为: ${if (shuffle) "乱序播放" else "顺序播放"}")
        }
    }
    
    /**
     * 获取当前乱序播放模式
     */
    fun isShuffleMode(): Boolean = isShuffleMode
    
    /**
     * 切换倍速播放
     */
    fun togglePlaybackSpeed() {
        currentSpeedIndex = (currentSpeedIndex + 1) % playbackSpeeds.size
        val newSpeed = playbackSpeeds[currentSpeedIndex]
        
        mediaPlayer?.let { player ->
            try {
                player.playbackParams = player.playbackParams.setSpeed(newSpeed)
                Log.d("PlayerService", "设置播放速度: ${newSpeed}x")
            } catch (e: Exception) {
                Log.e("PlayerService", "设置播放速度失败", e)
            }
        }
    }
    
    /**
     * 获取当前播放速度
     */
    fun getCurrentPlaybackSpeed(): Float {
        return playbackSpeeds[currentSpeedIndex]
    }
    
    /**
     * 获取当前播放速度的显示文本
     */
    fun getCurrentPlaybackSpeedText(): String {
        val speed = playbackSpeeds[currentSpeedIndex]
        return when (speed) {
            0.75f -> "0.75x"
            1.0f -> "1.0x"
            1.25f -> "1.25x"
            1.5f -> "1.5x"
            1.75f -> "1.75x"
            2.0f -> "2.0x"
            else -> "${speed}x"
        }
    }
    
    /**
     * 应用当前的播放速度到MediaPlayer
     */
    private fun applyCurrentPlaybackSpeed() {
        val currentSpeed = playbackSpeeds[currentSpeedIndex]
        mediaPlayer?.let { player ->
            try {
                player.playbackParams = player.playbackParams.setSpeed(currentSpeed)
                Log.d("PlayerService", "应用播放速度: ${currentSpeed}x")
            } catch (e: Exception) {
                Log.e("PlayerService", "应用播放速度失败", e)
            }
        }
    }
    /**
     * 切换定时停止
     */
    fun toggleSleepTimer() {
        currentSleepTimerIndex = (currentSleepTimerIndex + 1) % sleepTimerOptions.size
        val minutes = sleepTimerOptions[currentSleepTimerIndex]
        
        // 清除之前的定时器
        cancelSleepTimer()
        
        if (minutes > 0) {
            // 设置新的定时器
            sleepTimerEndTime = System.currentTimeMillis() + minutes * 60 * 1000
            sleepTimerRunnable = object : Runnable {
                override fun run() {
                    if (System.currentTimeMillis() >= sleepTimerEndTime) {
                        // 时间到了，停止播放
                        pause()
                        cancelSleepTimer()
                        // 重置定时器索引到关闭状态
                        currentSleepTimerIndex = 0
                        Log.d("PlayerService", "定时停止：播放已暂停，定时器已重置")
                    } else {
                        // 继续检查，每秒检查一次
                        handler.postDelayed(this, 1000)
                    }
                }
            }
            handler.postDelayed(sleepTimerRunnable!!, 1000)
            Log.d("PlayerService", "定时停止已设置：$minutes 分钟后停止")
        } else {
            Log.d("PlayerService", "定时停止已关闭")
        }
    }
    
    /**
     * 取消定时停止
     */
    private fun cancelSleepTimer() {
        sleepTimerRunnable?.let {
            handler.removeCallbacks(it)
            sleepTimerRunnable = null
        }
        sleepTimerEndTime = 0
    }
    
    /**
     * 获取当前定时停止时间
     */
    fun getCurrentSleepTimerMinutes(): Int {
        return sleepTimerOptions[currentSleepTimerIndex]
    }
    
    /**
     * 获取当前定时停止的显示文本
     */
    fun getCurrentSleepTimerText(): String {
        val minutes = sleepTimerOptions[currentSleepTimerIndex]
        if (minutes == 0) {
            return "关闭"
        }
        
        // 如果定时器正在运行，计算剩余时间
        if (sleepTimerEndTime > 0) {
            val remainingMs = sleepTimerEndTime - System.currentTimeMillis()
            if (remainingMs <= 0) {
                return "关闭"
            }
            // 向上取值计算剩余分钟数
            val remainingMinutes = ((remainingMs + 60 * 1000 - 1) / (60 * 1000)).toInt()
            return "${remainingMinutes}分钟"
        }
        
        // 如果定时器设置了但未启动，显示设置的时间
        return when (minutes) {
            15 -> "15分钟"
            30 -> "30分钟"
            60 -> "60分钟"
            else -> "${minutes}分钟"
        }
    }
    
    /**
     * 获取定时停止剩余时间（秒）
     */
    fun getSleepTimerRemainingSeconds(): Int {
        if (sleepTimerEndTime <= 0) return 0
        val remaining = sleepTimerEndTime - System.currentTimeMillis()
        return maxOf(0, (remaining / 1000).toInt())
    }
    
    /**
     * 生成乱序播放索引，确保当前播放的歌曲位置保持不变
     */
    private fun generateShuffledIndices(currentIndex: Int) {
        try {
            if (playlist.isEmpty()) return
            
            val allIndices = (0 until playlist.size).toMutableList()
            
            // 如果当前有播放的歌曲，先移除当前索引
            if (currentIndex >= 0 && currentIndex < playlist.size) {
                allIndices.remove(currentIndex)
                allIndices.shuffle()
                // 将当前索引放在最前面
                shuffledIndices = listOf(currentIndex) + allIndices
            } else {
                allIndices.shuffle()
                shuffledIndices = allIndices
            }
            
            Log.d("PlayerService", "生成乱序索引: $shuffledIndices")
        } catch (e: Exception) {
            Log.e("PlayerService", "生成乱序索引时出错", e)
            shuffledIndices = (0 until playlist.size).toList()
        }
    }
    
    /**
     * 获取当前播放列表
     */
    fun getCurrentPlaylist(): List<Feed> = playlist.toList()
    
    /**
     * 播放指定索引的曲目
     */
    fun playTrackAtIndex(index: Int) {
        try {
            if (index >= 0 && index < playlist.size) {
                currentTrackIndex = index
                playCurrentTrack()
                Log.d("PlayerService", "播放指定索引曲目: $index")
            } else {
                Log.w("PlayerService", "无效的曲目索引: $index, 播放列表大小: ${playlist.size}")
            }
        } catch (e: Exception) {
            Log.e("PlayerService", "播放指定索引曲目时出错", e)
        }
    }
    
    /**
     * 获取当前播放列表信息
     */
    fun getCurrentPlaylistInfo(): PlaylistInfo {
        return PlaylistInfo(
            currentIndex = currentTrackIndex,
            totalCount = playlist.size,
            hasNext = hasNextTrack(),
            hasPrevious = hasPreviousTrack(),
            isRepeat = isRepeatMode,
            isShuffle = isShuffleMode
        )
    }

    fun pause() {
        try {
            mediaPlayer?.pause()
            stopProgressUpdate()
            Log.d("PlayerService", "暂停播放")
            updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
            updateNotification()
        } catch (e: Exception) {
            Log.e("PlayerService", "暂停播放时出错", e)
        }
    }

    fun resume() {
        try {
            Log.d("PlayerService", "尝试恢复播放，isPrepared=$isPrepared, mediaPlayer状态=${getMediaPlayerState()}")
            
            if (!isPrepared || mediaPlayer == null) {
                Log.w("PlayerService", "播放器未准备好，无法恢复播放")
                return
            }
            
            val player = mediaPlayer!!
            
            // 检查MediaPlayer实际状态
            if (player.isPlaying) {
                Log.d("PlayerService", "播放器已经在播放")
                return
            }
            
            // 请求音频焦点
            if (requestAudioFocus()) {
                try {
                    player.start()
                    startProgressUpdate()
                    Log.d("PlayerService", "继续播放")
                    updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                    updateNotification()
                    playbackNeedsToResume = false // 重置恢复标志
                } catch (e: IllegalStateException) {
                    Log.e("PlayerService", "MediaPlayer状态异常，尝试重新播放当前曲目", e)
                    isPrepared = false
                    playCurrentTrack() // 重新播放当前曲目
                } catch (e: Exception) {
                    Log.e("PlayerService", "恢复播放时出错", e)
                    updatePlaybackState(PlaybackStateCompat.STATE_ERROR)
                }
            } else {
                Log.w("PlayerService", "无法获取音频焦点，无法恢复播放")
                playbackNeedsToResume = true
            }
        } catch (e: Exception) {
            Log.e("PlayerService", "resume()方法执行异常", e)
        }
    }
    
    /**
     * 获取MediaPlayer状态的描述，用于调试
     */
    private fun getMediaPlayerState(): String {
        return when {
            mediaPlayer == null -> "null"
            !isPrepared -> "未准备"
            mediaPlayer?.isPlaying == true -> "播放中"
            else -> "已暂停"
        }
    }

    fun isSameTrack(url: String): Boolean {
        return currentTrackUrl == url
    }
    
    /**
     * 获取当前播放状态信息，用于UI恢复
     */
    fun getCurrentPlaybackInfo(): Triple<Boolean, Boolean, String?> {
        return Triple(
            isPrepared && mediaPlayer?.isPlaying == true, // 是否正在播放
            isPrepared, // 是否已准备好
            currentTrackUrl // 当前播放的URL
        )
    }
    
    /**
     * 检查服务是否有活跃的播放会话
     */
    fun hasActiveSession(): Boolean {
        return mediaSession?.isActive == true && (isPrepared || playlist.isNotEmpty())
    }
    
    /**
     * 预加载下一个播客
     */
    private fun preloadNextTrack() {
        try {
            Log.d("PlayerService", "开始预加载下一个播客")
            
            if (playlist.isEmpty()) {
                Log.d("PlayerService", "播放列表为空，跳过预加载")
                return
            }
            
            val nextIndex = getNextTrackIndex()
            if (nextIndex == -1) {
                Log.d("PlayerService", "没有下一个播客，跳过预加载")
                return
            }
            
            val nextFeed = playlist.getOrNull(nextIndex)
            val nextUrl = nextFeed?.labels?.podcastUrl
            
            if (nextUrl.isNullOrBlank()) {
                Log.d("PlayerService", "下一个播客没有URL，跳过预加载")
                return
            }
            
            // 如果已经预加载了同一个文件，跳过
            if (preloadTrackUrl == nextUrl && isPreloadPrepared) {
                Log.d("PlayerService", "下一个播客已经预加载，跳过")
                return
            }
            
            // 清理之前的预加载
            cleanupPreloadedTrack()
            
            preloadedTrackIndex = nextIndex
            preloadTrackUrl = nextUrl
            
            Log.d("PlayerService", "开始预加载播客: $nextUrl")
            
            // 使用协程在后台预加载
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val localFile = downloadMediaFile(nextUrl)
                    
                    // 在主线程设置预加载的MediaPlayer
                    withContext(Dispatchers.Main) {
                        preloadMediaPlayer = MediaPlayer().apply {
                            try {
                                setDataSource(localFile.absolutePath)
                                setOnPreparedListener { mediaPlayer ->
                                    isPreloadPrepared = true
                                    Log.d("PlayerService", "预加载完成: $nextUrl")
                                }
                                setOnErrorListener { _, what, extra ->
                                    Log.e("PlayerService", "预加载错误: what=$what, extra=$extra")
                                    cleanupPreloadedTrack()
                                    true
                                }
                                prepareAsync()
                            } catch (e: Exception) {
                                Log.e("PlayerService", "设置预加载数据源时出错", e)
                                cleanupPreloadedTrack()
                            }
                        }
                    }
                } catch (e: Exception) {
                    Log.e("PlayerService", "预加载媒体文件失败", e)
                    withContext(Dispatchers.Main) {
                        cleanupPreloadedTrack()
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("PlayerService", "预加载下一个播客时发生异常", e)
        }
    }
    
    /**
     * 获取下一个播客的索引
     */
    private fun getNextTrackIndex(): Int {
        try {
            if (playlist.isEmpty()) return -1
            
            if (isShuffleMode) {
                // 乱序模式
                val currentShuffledIndex = shuffledIndices.indexOf(currentTrackIndex)
                if (currentShuffledIndex != -1) {
                    val nextShuffledIndex = if (isRepeatMode) {
                        (currentShuffledIndex + 1) % shuffledIndices.size
                    } else {
                        if (currentShuffledIndex < shuffledIndices.size - 1) {
                            currentShuffledIndex + 1
                        } else {
                            -1 // 已是最后一首
                        }
                    }
                    
                    return if (nextShuffledIndex != -1) {
                        shuffledIndices[nextShuffledIndex]
                    } else {
                        -1
                    }
                }
            } else if (isRepeatMode) {
                // 循环模式：播放到最后一首后回到第一首
                return (currentTrackIndex + 1) % playlist.size
            } else {
                // 顺序模式：播放到最后一首后停止
                return if (currentTrackIndex < playlist.size - 1) {
                    currentTrackIndex + 1
                } else {
                    -1
                }
            }
        } catch (e: Exception) {
            Log.e("PlayerService", "获取下一个播客索引时出错", e)
        }
        return -1
    }
    
    /**
     * 清理预加载的播客资源
     */
    private fun cleanupPreloadedTrack() {
        try {
            preloadMediaPlayer?.release()
            preloadMediaPlayer = null
            isPreloadPrepared = false
            preloadTrackUrl = null
            preloadedTrackIndex = -1
            Log.d("PlayerService", "清理预加载资源")
        } catch (e: Exception) {
            Log.e("PlayerService", "清理预加载资源时出错", e)
        }
    }

    /**
     * 停止前台服务但不销毁服务
     */
    fun stopForegroundService() {
        try {
            Log.d("PlayerService", "停止前台服务")
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            Log.e("PlayerService", "停止前台服务失败", e)
        }
    }

    override fun onDestroy() {
        Log.d("PlayerService", "PlayerService onDestroy")
        stopProgressUpdate()
        
        /*
         * 后台播放修复：正确停止前台服务
         * 确保服务销毁时能正确清理前台服务状态，移除通知
         */
        try {
            stopForeground(STOP_FOREGROUND_REMOVE)
        } catch (e: Exception) {
            Log.w("PlayerService", "停止前台服务时出错", e)
        }
        
        // 释放音频焦点
        abandonAudioFocus()
        
        // 清理定时停止器
        cancelSleepTimer()
        
        // 释放媒体播放器资源
        mediaPlayer?.release()
        mediaPlayer = null
        
        // 清理预加载资源
        cleanupPreloadedTrack()
        
        // 释放MediaSession，但要确保在真正销毁时才释放
        mediaSession?.let { session ->
            session.isActive = false
            session.release()
        }
        mediaSession = null
        
        // 清理缓存文件
        cleanupOldCacheFiles()
        
        Log.d("PlayerService", "PlayerService销毁完成")
        super.onDestroy()
    }
    
    /**
     * 清理旧的缓存文件（保留最近的10个文件）
     */
    private fun cleanupOldCacheFiles() {
        try {
            val cacheDir = File(cacheDir, "media_cache")
            if (cacheDir.exists()) {
                val files = cacheDir.listFiles()?.toList() ?: return
                if (files.size > 10) {
                    // 按修改时间排序，删除最旧的文件
                    files.sortedBy { it.lastModified() }
                        .take(files.size - 10)
                        .forEach { file ->
                            if (file.delete()) {
                                Log.d("PlayerService", "删除旧缓存文件: ${file.name}")
                            }
                        }
                }
            }
        } catch (e: Exception) {
            Log.w("PlayerService", "清理缓存文件失败", e)
        }
    }

    private fun startProgressUpdate() {
        handler.post(progressUpdateRunnable)
    }

    private fun stopProgressUpdate() {
        handler.removeCallbacks(progressUpdateRunnable)
    }

    private fun updatePlaybackState(state: Int) {
        val position = if (isPrepared) mediaPlayer?.currentPosition?.toLong() ?: 0L else 0L
        val playbackSpeed = if (state == PlaybackStateCompat.STATE_PLAYING) 1.0f else 0.0f
        val playbackStateBuilder = PlaybackStateCompat.Builder()
            .setActions(
                PlaybackStateCompat.ACTION_PLAY or
                        PlaybackStateCompat.ACTION_PAUSE or
                        PlaybackStateCompat.ACTION_SKIP_TO_NEXT or
                        PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS
            )
            .setState(state, position, playbackSpeed)
        mediaSession?.setPlaybackState(playbackStateBuilder.build())
    }

    /**
     * 请求音频焦点
     */
    private fun requestAudioFocus(): Boolean {
        return try {
            val result = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                // Android 8.0及以上版本使用AudioFocusRequest
                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build()
                
                audioFocusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN)
                    .setAudioAttributes(audioAttributes)
                    .setAcceptsDelayedFocusGain(true)
                    .setOnAudioFocusChangeListener(this)
                    .build()
                
                audioManager.requestAudioFocus(audioFocusRequest!!)
            } else {
                // Android 8.0以下版本使用旧API
                @Suppress("DEPRECATION")
                audioManager.requestAudioFocus(
                    this,
                    AudioManager.STREAM_MUSIC,
                    AudioManager.AUDIOFOCUS_GAIN
                )
            }
            
            hasAudioFocus = result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED
            if (hasAudioFocus) {
                Log.d("PlayerService", "音频焦点获取成功")
            } else {
                Log.w("PlayerService", "音频焦点获取失败: $result")
            }
            hasAudioFocus
        } catch (e: Exception) {
            Log.e("PlayerService", "请求音频焦点时出错", e)
            false
        }
    }

    /**
     * 释放音频焦点
     */
    private fun abandonAudioFocus() {
        try {
            if (hasAudioFocus) {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && audioFocusRequest != null) {
                    audioManager.abandonAudioFocusRequest(audioFocusRequest!!)
                } else {
                    @Suppress("DEPRECATION")
                    audioManager.abandonAudioFocus(this)
                }
                hasAudioFocus = false
                playbackNeedsToResume = false
                Log.d("PlayerService", "音频焦点已释放")
            }
        } catch (e: Exception) {
            Log.e("PlayerService", "释放音频焦点时出错", e)
        }
    }

    /**
     * 音频焦点变化监听器
     */
    override fun onAudioFocusChange(focusChange: Int) {
        Log.d("PlayerService", "音频焦点变化: $focusChange")
        
        when (focusChange) {
            AudioManager.AUDIOFOCUS_GAIN -> {
                // 获得音频焦点
                hasAudioFocus = true
                Log.d("PlayerService", "获得音频焦点，playbackNeedsToResume=$playbackNeedsToResume, isPrepared=$isPrepared")
                
                if (playbackNeedsToResume && isPrepared && mediaPlayer != null) {
                    try {
                        // 检查MediaPlayer状态是否有效
                        if (!mediaPlayer!!.isPlaying) {
                            mediaPlayer!!.start()
                            startProgressUpdate()
                            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                            updateNotification()
                            playbackNeedsToResume = false
                            Log.d("PlayerService", "音频焦点恢复，继续播放")
                        } else {
                            Log.d("PlayerService", "播放器已经在播放，无需恢复")
                            playbackNeedsToResume = false
                        }
                    } catch (e: IllegalStateException) {
                        Log.e("PlayerService", "音频焦点恢复时MediaPlayer状态异常，尝试重新播放", e)
                        isPrepared = false
                        playbackNeedsToResume = false
                        playCurrentTrack() // 重新播放当前曲目
                    } catch (e: Exception) {
                        Log.e("PlayerService", "音频焦点恢复时出错", e)
                        playbackNeedsToResume = false
                        updatePlaybackState(PlaybackStateCompat.STATE_ERROR)
                    }
                }
                
                // 恢复正常音量
                try {
                    mediaPlayer?.setVolume(1.0f, 1.0f)
                } catch (e: Exception) {
                    Log.e("PlayerService", "恢复音量时出错", e)
                }
            }
            
            AudioManager.AUDIOFOCUS_LOSS -> {
                // 永久失去音频焦点，暂停播放但记住需要恢复
                hasAudioFocus = false
                try {
                    if (mediaPlayer?.isPlaying == true) {
                        playbackNeedsToResume = true // 即使永久失去焦点，也标记需要恢复，以便其他应用结束后自动恢复
                        mediaPlayer?.pause()
                        stopProgressUpdate()
                        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                        updateNotification()
                        Log.d("PlayerService", "永久失去音频焦点，暂停播放，等待恢复机会")
                    } else {
                        playbackNeedsToResume = false
                    }
                } catch (e: Exception) {
                    Log.e("PlayerService", "处理永久失去音频焦点时出错", e)
                }
            }
            
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT -> {
                // 临时失去音频焦点，暂停播放但记住需要恢复
                hasAudioFocus = false
                try {
                    if (mediaPlayer?.isPlaying == true) {
                        playbackNeedsToResume = true
                        mediaPlayer?.pause()
                        stopProgressUpdate()
                        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
                        updateNotification()
                        Log.d("PlayerService", "临时失去音频焦点，暂停播放")
                    }
                } catch (e: Exception) {
                    Log.e("PlayerService", "处理临时失去音频焦点时出错", e)
                }
            }
            
            AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK -> {
                // 临时失去音频焦点，但可以降低音量继续播放
                try {
                    if (mediaPlayer?.isPlaying == true) {
                        mediaPlayer?.setVolume(0.2f, 0.2f) // 降低音量到20%
                        Log.d("PlayerService", "临时失去音频焦点，降低音量")
                    }
                } catch (e: Exception) {
                    Log.e("PlayerService", "降低音量时出错", e)
                }
            }
        }
    }

    /**
     * 使用默认通知启动前台服务（ANR修复）
     *
     * 在服务创建时立即调用startForeground()，避免ANR
     */
    private fun startForegroundWithDefaultNotification() {
        try {
            Log.d("PlayerService", "使用默认通知启动前台服务")
            
            // 创建默认的播放器待机通知
            val defaultNotification = NotificationCompat.Builder(this, "zenfeed_player")
                .setContentTitle("ZenFeed 播放器")
                .setContentText("准备就绪")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .setAutoCancel(false)
                .build()
            
            startForeground(1, defaultNotification)
            Log.d("PlayerService", "前台服务启动成功")
        } catch (e: Exception) {
            Log.e("PlayerService", "启动默认前台服务失败", e)
        }
    }

    /**
     * 启动前台服务
     *
     * 后台播放修复：新增方法，确保播放时服务运行在前台模式
     *
     * @note 前台服务具有更高的优先级，不会因为应用退出而被立即杀死
     *       这是实现真正后台播放的关键
     */
    private fun startAsForegroundService() {
        try {
            if (playlist.isNotEmpty()) {
                updateNotification()
                Log.d("PlayerService", "服务已启动为前台服务")
            }
        } catch (e: Exception) {
            Log.e("PlayerService", "启动前台服务失败", e)
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "zenfeed_player",
                "Zenfeed Player",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun updateNotification() {
        val track = playlist.getOrNull(currentTrackIndex)

        // 如果没有播放列表，显示待机通知
        if (track == null) {
            val defaultNotification = NotificationCompat.Builder(this, "zenfeed_player")
                .setContentTitle("ZenFeed 播放器")
                .setContentText("准备就绪")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setOngoing(true)
                .setAutoCancel(false)
                .build()
            startForeground(1, defaultNotification)
            return
        }

        // 创建点击通知跳转到文章详情的Intent
        val contentIntent = Intent(this, com.ddyy.zenfeed.MainActivity::class.java).apply {
            action = "ACTION_OPEN_FEED_DETAIL"
            putExtra("FEED_TITLE", track.labels.title ?: "")
            putExtra("FEED_SOURCE", track.labels.source ?: "")
            putExtra("FEED_CONTENT", track.labels.content ?: "")
            putExtra("FEED_LINK", track.labels.link ?: "")
            putExtra("FEED_SUMMARY", track.labels.summary ?: "")
            putExtra("FEED_SUMMARY_HTML_SNIPPET", track.labels.summaryHtmlSnippet ?: "")
            putExtra("FEED_PUB_TIME", track.labels.pubTime ?: "")
            putExtra("FEED_CATEGORY", track.labels.category ?: "")
            putExtra("FEED_TAGS", track.labels.tags ?: "")
            putExtra("FEED_TYPE", track.labels.type ?: "")
            putExtra("FEED_PODCAST_URL", track.labels.podcastUrl ?: "")
            putExtra("FEED_TIME", track.time)
            putExtra("FEED_IS_READ", track.isRead)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }

        val contentPendingIntent = PendingIntent.getActivity(
            this,
            0,
            contentIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notificationBuilder = NotificationCompat.Builder(this, "zenfeed_player")
            .setContentTitle(track.labels.title ?: "未知标题")
            .setContentText(track.labels.source ?: "未知来源")
            .setContentIntent(contentPendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_previous,
                    "上一首",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                )
            )
            .addAction(
                NotificationCompat.Action(
                    if (isPrepared && mediaPlayer?.isPlaying == true) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                    if (isPrepared && mediaPlayer?.isPlaying == true) "暂停" else "播放",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)
                )
            )
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_next,
                    "下一首",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
                )
            )
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )

        // 使用缓存的图标（如果存在），否则使用默认图标
        if (currentTrackFavicon != null) {
            notificationBuilder.setSmallIcon(IconCompat.createWithBitmap(currentTrackFavicon!!))
        } else {
            notificationBuilder.setSmallIcon(android.R.drawable.ic_media_play)
        }

        startForeground(1, notificationBuilder.build())
    }
}