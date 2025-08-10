package com.ddyy.zenfeed.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
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
import androidx.media.session.MediaButtonReceiver
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

class PlayerService : Service() {

    var mediaSession: MediaSessionCompat? = null
    private var mediaPlayer: MediaPlayer? = null
    private val binder = LocalBinder()
    private var playlist: List<Feed> = emptyList()
    private var currentTrackIndex = -1
    private var isPrepared = false
    var currentTrackUrl: String? = null
        private set
    
    // 播放模式：false为顺序播放，true为循环播放
    private var isRepeatMode = false

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
        return START_STICKY
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        mediaSession = MediaSessionCompat(this, "PlayerService")
        mediaSession?.setCallback(object : MediaSessionCompat.Callback() {
            override fun onPlay() {
                try {
                    resume()
                } catch (e: Exception) {
                    Log.e("PlayerService", "播放时出错", e)
                }
            }

            override fun onPause() {
                try {
                    pause()
                } catch (e: Exception) {
                    Log.e("PlayerService", "暂停时出错", e)
                }
            }

            override fun onSkipToNext() {
                try {
                    Log.d("PlayerService", "收到下一首命令")
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
                    Log.d("PlayerService", "收到上一首命令")
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
        mediaSession?.isActive = true
    }

    fun setPlaylist(feeds: List<Feed>, startIndex: Int) {
        playlist = feeds
        currentTrackIndex = startIndex
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
            if (currentFeed.labels.podcastUrl.isBlank()) {
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
        playInternal(feed.labels.podcastUrl)
    }

    private fun playInternal(url: String) {
        currentTrackUrl = url
        isPrepared = false
        stopProgressUpdate()
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
                                    .putString(MediaMetadataCompat.METADATA_KEY_TITLE, track?.labels?.title)
                                    .putString(MediaMetadataCompat.METADATA_KEY_ARTIST, track?.labels?.source)
                                    .putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration)
                                    .build()
                                mediaSession?.setMetadata(metadata)

                                start()
                                Log.d("PlayerService", "开始播放: $url")
                                updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
                                startProgressUpdate()
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
            val fileName = "${url.hashCode().toString()}.tmp"
            val localFile = File(cacheDir, fileName)
            
            // 如果文件已存在且大小合理，直接使用
            if (localFile.exists() && localFile.length() > 0) {
                Log.d("PlayerService", "使用缓存的媒体文件: ${localFile.absolutePath}")
                return@withContext localFile
            }
            
            // 下载文件
            response.body?.let { responseBody ->
                FileOutputStream(localFile).use { output ->
                    responseBody.byteStream().use { input ->
                        input.copyTo(output)
                    }
                }
                Log.d("PlayerService", "媒体文件下载完成: ${localFile.absolutePath}, 大小: ${localFile.length()} 字节")
            } ?: throw Exception("响应体为空")
            
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
            
            if (isRepeatMode) {
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
            
            if (isRepeatMode) {
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
            playlist.isNotEmpty() && currentTrackIndex >= 0 && currentTrackIndex < playlist.size - 1
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
            playlist.isNotEmpty() && currentTrackIndex > 0 && currentTrackIndex < playlist.size
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
            isRepeat = isRepeatMode
        )
    }

    fun pause() {
        mediaPlayer?.pause()
        stopProgressUpdate()
        Log.d("PlayerService", "暂停播放")
        updatePlaybackState(PlaybackStateCompat.STATE_PAUSED)
    }

    fun resume() {
        if (isPrepared && mediaPlayer?.isPlaying == false) {
            mediaPlayer?.start()
            startProgressUpdate()
            Log.d("PlayerService", "继续播放")
            updatePlaybackState(PlaybackStateCompat.STATE_PLAYING)
        }
    }

    fun isSameTrack(url: String): Boolean {
        return currentTrackUrl == url
    }

    override fun onDestroy() {
        stopProgressUpdate()
        mediaPlayer?.release()
        mediaPlayer = null
        mediaSession?.release()
        cleanupOldCacheFiles()
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
        updateNotification()
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
        val track = playlist.getOrNull(currentTrackIndex) ?: return
        
        // 创建点击通知跳转到文章详情的Intent
        val contentIntent = Intent(this, com.ddyy.zenfeed.MainActivity::class.java).apply {
            action = "ACTION_OPEN_FEED_DETAIL"
            putExtra("FEED_TITLE", track.labels.title)
            putExtra("FEED_SOURCE", track.labels.source)
            putExtra("FEED_CONTENT", track.labels.content)
            putExtra("FEED_LINK", track.labels.link)
            putExtra("FEED_SUMMARY", track.labels.summary)
            putExtra("FEED_SUMMARY_HTML_SNIPPET", track.labels.summaryHtmlSnippet)
            putExtra("FEED_PUB_TIME", track.labels.pubTime)
            putExtra("FEED_CATEGORY", track.labels.category)
            putExtra("FEED_TAGS", track.labels.tags)
            putExtra("FEED_TYPE", track.labels.type)
            putExtra("FEED_PODCAST_URL", track.labels.podcastUrl)
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
        
        val notification = NotificationCompat.Builder(this, "zenfeed_player")
            .setContentTitle(track.labels.title)
            .setContentText(track.labels.source)
            .setSmallIcon(android.R.drawable.ic_media_play) // Replace with your app icon
            .setContentIntent(contentPendingIntent) // 设置点击通知的跳转Intent
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_previous,
                    "Previous",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                )
            )
            .addAction(
                NotificationCompat.Action(
                    if (isPrepared && mediaPlayer?.isPlaying == true) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                    if (isPrepared && mediaPlayer?.isPlaying == true) "Pause" else "Play",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_PLAY_PAUSE)
                )
            )
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_next,
                    "Next",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_NEXT)
                )
            )
            .setStyle(
                androidx.media.app.NotificationCompat.MediaStyle()
                    .setMediaSession(mediaSession?.sessionToken)
                    .setShowActionsInCompactView(0, 1, 2)
            )
            .build()
        startForeground(1, notification)
    }
}