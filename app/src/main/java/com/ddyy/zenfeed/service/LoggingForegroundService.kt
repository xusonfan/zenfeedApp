package com.ddyy.zenfeed.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.ddyy.zenfeed.MainActivity
import com.ddyy.zenfeed.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.InterruptedIOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 日志记录前台服务
 * 在后台持续记录应用日志，直到用户手动停止
 */
class LoggingForegroundService : Service() {
    
    companion object {
        private const val TAG = "LoggingForegroundService"
        private const val NOTIFICATION_ID = 2001
        private const val CHANNEL_ID = "logging_service_channel"
        private const val LOG_FILE_NAME = "zenfeed_debug.log"
        private const val MAX_LOG_SIZE = 5 * 1024 * 1024 // 5MB
        
        const val ACTION_START_LOGGING = "start_logging"
        const val ACTION_STOP_LOGGING = "stop_logging"
        
        fun startService(context: Context) {
            val intent = Intent(context, LoggingForegroundService::class.java).apply {
                action = ACTION_START_LOGGING
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
        
        fun stopService(context: Context) {
            val intent = Intent(context, LoggingForegroundService::class.java).apply {
                action = ACTION_STOP_LOGGING
            }
            context.startService(intent)
        }
    }
    
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val binder = LoggingBinder()
    private var logcatProcess: Process? = null
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    
    private val _isLogging = MutableStateFlow(false)
    val isLogging: StateFlow<Boolean> = _isLogging.asStateFlow()
    
    private val _logContent = MutableStateFlow("")
    val logContent: StateFlow<String> = _logContent.asStateFlow()
    
    inner class LoggingBinder : Binder() {
        fun getService(): LoggingForegroundService = this@LoggingForegroundService
    }
    
    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        Log.d(TAG, "LoggingForegroundService 创建")
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_START_LOGGING -> {
                startLogging()
            }
            ACTION_STOP_LOGGING -> {
                stopLogging()
                stopSelf()
            }
        }
        return START_STICKY // 服务被杀死后会重启
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "日志记录服务",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "持续记录应用日志用于问题排查"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val stopIntent = Intent(this, LoggingForegroundService::class.java).apply {
            action = ACTION_STOP_LOGGING
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        val mainIntent = Intent(this, MainActivity::class.java)
        val mainPendingIntent = PendingIntent.getActivity(
            this, 0, mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("正在记录日志")
            .setContentText("点击打开应用，或点击停止按钮结束记录")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(mainPendingIntent)
            .addAction(
                android.R.drawable.ic_media_pause,
                "停止记录",
                stopPendingIntent
            )
            .setOngoing(true)
            .setShowWhen(true)
            .build()
    }
    
    private fun startLogging() {
        if (_isLogging.value) {
            Log.w(TAG, "日志记录已在进行中")
            return
        }
        
        startForeground(NOTIFICATION_ID, createNotification())
        
        serviceScope.launch {
            try {
                _isLogging.value = true
                val logFile = getLogFile()
                
                // 清空之前的日志内容显示
                _logContent.value = "开始记录日志...\n"
                
                // 添加开始记录的时间戳
                val startTime = dateFormat.format(Date())
                val startMessage = "=== 日志记录开始: $startTime ===\n"
                logFile.appendText(startMessage)
                _logContent.value += startMessage
                
                // 启动logcat进程，尝试多种方式记录本应用的日志
                val packageName = applicationContext.packageName
                val command = arrayOf(
                    "logcat",
                    "-v", "threadtime",  // 显示详细时间戳和线程信息
                    "-T", "20",  // 先输出最近20行历史日志
                    "--pid=${android.os.Process.myPid()}"  // 使用进程ID过滤，这样更准确
                )
                
                logcatProcess = Runtime.getRuntime().exec(command)
                val reader = BufferedReader(InputStreamReader(logcatProcess!!.inputStream))
                
                Log.i(TAG, "开始记录日志")
                
                // 在后台线程中持续读取日志
                var line: String?
                while (_isLogging.value) {
                    try {
                        line = reader.readLine()
                        if (line == null) break
                        
                        line.let { logLine ->
                            // 过滤掉不相关的日志行
                            if (shouldFilterLogLine(logLine)) {
                                return@let // 跳过这行日志
                            }
                            
                            // 写入文件
                            logFile.appendText("$logLine\n")
                            
                            // 更新显示内容（保持最近的1000行）
                            withContext(Dispatchers.Main) {
                                val currentContent = _logContent.value
                                val lines = (currentContent + "$logLine\n").split("\n")
                                if (lines.size > 1000) {
                                    _logContent.value = lines.takeLast(1000).joinToString("\n")
                                } else {
                                    _logContent.value = currentContent + "$logLine\n"
                                }
                            }
                            
                            // 检查文件大小，如果太大则清理
                            if (logFile.length() > MAX_LOG_SIZE) {
                                cleanupLogFile(logFile)
                            }
                        }
                    } catch (e: InterruptedIOException) {
                        // 这是正常的中断，通常在停止日志记录时发生
                        Log.d(TAG, "日志读取被中断，正在停止记录")
                        break
                    } catch (e: Exception) {
                        Log.w(TAG, "读取日志行时出错: ${e.message}")
                        // 继续读取，不要因为单行错误而停止整个服务
                    }
                }
                
            } catch (e: Exception) {
                Log.e(TAG, "启动日志记录失败", e)
                withContext(Dispatchers.Main) {
                    _logContent.value += "错误: 启动日志记录失败 - ${e.message}\n"
                }
            }
        }
    }
    
    private fun stopLogging() {
        if (!_isLogging.value) {
            Log.w(TAG, "日志记录未在进行中")
            return
        }
        
        serviceScope.launch {
            try {
                _isLogging.value = false
                
                // 终止logcat进程
                logcatProcess?.destroy()
                logcatProcess = null
                
                // 添加结束记录的时间戳
                val endTime = dateFormat.format(Date())
                val endMessage = "=== 日志记录结束: $endTime ===\n\n"
                
                val logFile = getLogFile()
                logFile.appendText(endMessage)
                
                withContext(Dispatchers.Main) {
                    _logContent.value += endMessage
                }
                
                Log.i(TAG, "日志记录已停止，日志文件: ${logFile.absolutePath}")
                
            } catch (e: Exception) {
                Log.e(TAG, "停止日志记录失败", e)
                withContext(Dispatchers.Main) {
                    _logContent.value += "错误: 停止日志记录失败 - ${e.message}\n"
                }
            }
        }
    }
    
    /**
     * 获取日志文件路径
     */
    private fun getLogFile(): File {
        val logDir = File(getExternalFilesDir(null), "logs")
        if (!logDir.exists()) {
            logDir.mkdirs()
        }
        return File(logDir, LOG_FILE_NAME)
    }
    
    /**
     * 清理日志文件（保留最新的一半内容）
     */
    private fun cleanupLogFile(logFile: File) {
        try {
            val lines = logFile.readLines()
            val keepLines = lines.takeLast(lines.size / 2)
            logFile.writeText(keepLines.joinToString("\n") + "\n")
            Log.i(TAG, "日志文件已清理，保留 ${keepLines.size} 行")
        } catch (e: Exception) {
            Log.e(TAG, "清理日志文件失败", e)
        }
    }
    
    /**
     * 获取日志文件路径
     */
    fun getLogFilePath(): String {
        return getLogFile().absolutePath
    }
    
    /**
     * 清空日志文件
     */
    fun clearLogFile() {
        serviceScope.launch {
            try {
                val logFile = getLogFile()
                logFile.writeText("")
                withContext(Dispatchers.Main) {
                    _logContent.value = "日志文件已清空\n"
                }
                Log.i(TAG, "日志文件已清空")
            } catch (e: Exception) {
                Log.e(TAG, "清空日志文件失败", e)
                withContext(Dispatchers.Main) {
                    _logContent.value += "错误: 清空日志文件失败 - ${e.message}\n"
                }
            }
        }
    }
    
    /**
     * 加载现有日志内容
     */
    fun loadExistingLogs() {
        serviceScope.launch {
            try {
                val logFile = getLogFile()
                if (logFile.exists()) {
                    val content = logFile.readText()
                    val lines = content.split("\n")
                    // 只显示最后1000行
                    withContext(Dispatchers.Main) {
                        if (lines.size > 1000) {
                            _logContent.value = lines.takeLast(1000).joinToString("\n")
                        } else {
                            _logContent.value = content
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        _logContent.value = "暂无日志记录\n"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "加载现有日志失败", e)
                withContext(Dispatchers.Main) {
                    _logContent.value = "错误: 加载日志失败 - ${e.message}\n"
                }
            }
        }
    }
    
    /**
     * 判断是否应该过滤掉某行日志
     */
    private fun shouldFilterLogLine(logLine: String): Boolean {
        val lowerLine = logLine.lowercase()
        
        // 过滤掉包含这些关键词的日志行
        val filterKeywords = listOf(
            "miui",
            "webview",
            "chromium",
            "adreno",
            "vulkan",
            "camera",
            "nativeloader",
            "ziparchive",
            "trichrome",
            "libmigl",
            "audiocapabilities",
            "videocapabilities",
            "ahardwarebuffer",
            "choreographer",
            "windowonbackdispatcher",
            "contentcatcher",
            "cameramanagerglobal",
            "camerainjector",
            "cameraextimplxiaomi",
            "applicationloaders",
            "cr_",
            "libc",
            "finalizerdaemon",
            "apkassets",
            "profileinstaller",
            "frameinsert",
            "this is non sticky gc",
            "this is sticky gc",
            "compiler allocated",
            "entry not found",
            "avc: denied"
        )
        
        // 如果包含任何过滤关键词，则过滤掉
        for (keyword in filterKeywords) {
            if (lowerLine.contains(keyword)) {
                return true
            }
        }
        
        // 保留包含应用包名或应用相关标签的日志
        val appKeywords = listOf(
            "feedsscreen",
            "appnavigation",
            "loggingforegroundservice",
            "loggingviewmodel",
            "feedrepository",
            "apiclient",
            "playerservice"
        )
        
        for (keyword in appKeywords) {
            if (lowerLine.contains(keyword)) {
                return false // 不过滤，保留这行日志
            }
        }
        
        // 如果不包含应用相关关键词，也过滤掉
        return true
    }
    
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
        logcatProcess?.destroy()
        Log.d(TAG, "LoggingForegroundService 销毁")
    }
}