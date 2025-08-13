package com.ddyy.zenfeed

import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.ddyy.zenfeed.data.SettingsDataStore
import com.ddyy.zenfeed.ui.SharedViewModel
import com.ddyy.zenfeed.ui.navigation.AppNavigation
import com.ddyy.zenfeed.ui.theme.ZenfeedTheme
import com.ddyy.zenfeed.ui.theme.rememberThemeController

class MainActivity : ComponentActivity() {
    private val sharedViewModel: SharedViewModel by viewModels()
    private lateinit var settingsDataStore: SettingsDataStore
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 初始化设置数据存储
        settingsDataStore = SettingsDataStore(applicationContext)
        
        // 处理启动时的Intent
        handleIntent(intent)
        
        // 检查应用更新
        sharedViewModel.checkForUpdate()
        
        setContent {
            // 获取主题控制器
            val themeController = rememberThemeController(settingsDataStore)
            
            ZenfeedTheme(darkTheme = themeController.useDarkTheme) {
                AppNavigation(sharedViewModel = sharedViewModel)
            }
        }
    }
    
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // 处理新的Intent
        handleIntent(intent)
    }
    
    private fun handleIntent(intent: Intent?) {
        intent?.let {
            if (it.action == "ACTION_OPEN_FEED_DETAIL") {
                // 从Intent中创建Feed对象并设置到SharedViewModel
                sharedViewModel.selectFeedFromIntent(it)
                // 设置一个标志表示需要导航到详情页
                sharedViewModel.setNavigateToDetail(true)
            }
        }
    }
    
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.d("MainActivity", "配置变更: uiMode=${newConfig.uiMode}, orientation=${newConfig.orientation}")
        
        // 当配置变更时（如夜间模式切换），不需要重建Activity
        // Compose会自动响应主题变化并重新组合UI
        // 这样可以保持播放服务的连接状态
    }
}