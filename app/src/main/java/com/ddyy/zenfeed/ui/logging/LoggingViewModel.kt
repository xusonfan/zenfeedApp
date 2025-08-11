package com.ddyy.zenfeed.ui.logging

import android.app.Application
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ddyy.zenfeed.service.LoggingForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.lang.ref.WeakReference

/**
 * 日志记录页面的视图模型
 */
class LoggingViewModel(application: Application) : AndroidViewModel(application) {
    
    companion object {
        private const val TAG = "LoggingViewModel"
    }
    
    private var loggingServiceRef: WeakReference<LoggingForegroundService>? = null
    private var isBound = false
    
    private val _isLogging = MutableStateFlow(false)
    val isLogging: StateFlow<Boolean> = _isLogging.asStateFlow()
    
    private val _logContent = MutableStateFlow("")
    val logContent: StateFlow<String> = _logContent.asStateFlow()
    
    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as LoggingForegroundService.LoggingBinder
            val loggingService = binder.getService()
            loggingServiceRef = WeakReference(loggingService)
            isBound = true
            
            // 连接到服务后，监听状态变化
            viewModelScope.launch {
                loggingService.isLogging.collect { isLogging ->
                    _isLogging.value = isLogging
                }
            }
            
            viewModelScope.launch {
                loggingService.logContent.collect { content ->
                    _logContent.value = content
                }
            }
            
            // 加载现有日志
            loggingService.loadExistingLogs()
            
            Log.d(TAG, "服务已连接")
        }
        
        override fun onServiceDisconnected(name: ComponentName?) {
            loggingServiceRef = null
            isBound = false
            Log.d(TAG, "服务已断开")
        }
    }
    
    init {
        // 绑定到前台服务
        bindToService()
    }
    
    private fun bindToService() {
        val intent = Intent(getApplication(), LoggingForegroundService::class.java)
        getApplication<Application>().bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }
    
    /**
     * 开始记录日志
     */
    fun startLogging() {
        LoggingForegroundService.startService(getApplication())
    }
    
    /**
     * 停止记录日志
     */
    fun stopLogging() {
        LoggingForegroundService.stopService(getApplication())
    }
    
    /**
     * 切换日志记录状态
     */
    fun toggleLogging() {
        if (isLogging.value) {
            stopLogging()
        } else {
            startLogging()
        }
    }
    
    /**
     * 清空日志
     */
    fun clearLogs() {
        loggingServiceRef?.get()?.clearLogFile()
    }
    
    /**
     * 获取日志文件路径
     */
    fun getLogFilePath(): String {
        return loggingServiceRef?.get()?.getLogFilePath() ?: ""
    }
    
    /**
     * 刷新日志内容
     */
    fun refreshLogs() {
        loggingServiceRef?.get()?.loadExistingLogs()
    }
    
    override fun onCleared() {
        super.onCleared()
        // 解绑服务
        if (isBound) {
            getApplication<Application>().unbindService(serviceConnection)
            isBound = false
        }
    }
}