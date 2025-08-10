package com.ddyy.zenfeed.ui.player

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import android.support.v4.media.session.PlaybackStateCompat
import com.ddyy.zenfeed.service.PlayerService

class PlayerViewModel : ViewModel() {

    var playerService: PlayerService? by mutableStateOf(null)
        private set

    private val _isPlaying = MutableLiveData<Boolean>()
    val isPlaying: LiveData<Boolean> = _isPlaying

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as PlayerService.LocalBinder
            playerService = binder.getService()
            val controller = playerService?.mediaSession?.controller
            controller?.registerCallback(object : android.support.v4.media.session.MediaControllerCompat.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackStateCompat?) {
                    _isPlaying.value = state?.state == PlaybackStateCompat.STATE_PLAYING
                }
            })
            // 服务连接后，立即获取并更新当前播放状态
            _isPlaying.value = controller?.playbackState?.state == PlaybackStateCompat.STATE_PLAYING
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            playerService = null
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
}