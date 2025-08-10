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
        mediaPlayer = MediaPlayer().apply {
            try {
                setDataSource(url)
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
        super.onDestroy()
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