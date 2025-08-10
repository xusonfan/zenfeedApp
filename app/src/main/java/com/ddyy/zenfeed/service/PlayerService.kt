package com.ddyy.zenfeed.service

import android.app.NotificationChannel
import android.app.NotificationManager
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
                resume()
            }

            override fun onPause() {
                pause()
            }

            override fun onSkipToNext() {
                playNextTrack()
            }

            override fun onSkipToPrevious() {
                playPreviousTrack()
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
        if (playlist.isNotEmpty() && currentTrackIndex in playlist.indices) {
            playInternal(playlist[currentTrackIndex].labels.podcastUrl)
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
                                playNextTrack()
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
        if (playlist.isNotEmpty()) {
            currentTrackIndex = (currentTrackIndex + 1) % playlist.size
            playCurrentTrack()
        }
    }

    fun playPreviousTrack() {
        if (playlist.isNotEmpty()) {
            currentTrackIndex = if (currentTrackIndex > 0) currentTrackIndex - 1 else playlist.size - 1
            playCurrentTrack()
        }
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
        val position = mediaPlayer?.currentPosition?.toLong() ?: 0L
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
        val notification = NotificationCompat.Builder(this, "zenfeed_player")
            .setContentTitle(track.labels.title)
            .setContentText(track.labels.source)
            .setSmallIcon(android.R.drawable.ic_media_play) // Replace with your app icon
            .addAction(
                NotificationCompat.Action(
                    android.R.drawable.ic_media_previous,
                    "Previous",
                    MediaButtonReceiver.buildMediaButtonPendingIntent(this, PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS)
                )
            )
            .addAction(
                NotificationCompat.Action(
                    if (mediaPlayer?.isPlaying == true) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play,
                    if (mediaPlayer?.isPlaying == true) "Pause" else "Play",
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