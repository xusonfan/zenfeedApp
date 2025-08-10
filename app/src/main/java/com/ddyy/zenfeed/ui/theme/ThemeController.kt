package com.ddyy.zenfeed.ui.theme

import android.app.Activity
import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.ddyy.zenfeed.data.SettingsDataStore
import kotlinx.coroutines.flow.collectLatest

/**
 * 主题控制器，用于管理应用的主题状态
 */
class ThemeController(private val settingsDataStore: SettingsDataStore) {
    // 当前主题模式
    var currentThemeMode by mutableStateOf(SettingsDataStore.DEFAULT_THEME_MODE)
        private set
    
    // 是否使用暗色主题
    var useDarkTheme by mutableStateOf(false)
        private set
    
    /**
     * 初始化主题控制器
     */
    suspend fun initialize() {
        settingsDataStore.themeMode.collectLatest { mode ->
            currentThemeMode = mode
        }
    }
    
    /**
     * 更新主题模式
     * @param mode 新的主题模式
     * @param isSystemDark 系统是否处于暗色模式
     */
    fun updateThemeMode(mode: String, isSystemDark: Boolean) {
        currentThemeMode = mode
        useDarkTheme = shouldUseDarkTheme(mode, isSystemDark)
    }
    
    /**
     * 保存主题模式
     * @param mode 要保存的主题模式
     */
    suspend fun saveThemeMode(mode: String) {
        settingsDataStore.saveThemeMode(mode)
    }
}

/**
 * 创建并记住主题控制器
 */
@Composable
fun rememberThemeController(settingsDataStore: SettingsDataStore): ThemeController {
    val context = LocalContext.current
    val themeController = remember { ThemeController(settingsDataStore) }
    
    // 监听系统主题变化
    val isSystemDark = isSystemInDarkTheme()
    
    // 初始化主题控制器
    LaunchedEffect(Unit) {
        themeController.initialize()
    }
    
    // 当主题模式或系统主题变化时，更新useDarkTheme
    LaunchedEffect(themeController.currentThemeMode, isSystemDark) {
        themeController.updateThemeMode(themeController.currentThemeMode, isSystemDark)
    }
    
    return themeController
}

/**
 * 重启Activity以应用新的主题
 */
fun restartActivity(context: Context) {
    if (context is Activity) {
        context.recreate()
    }
}