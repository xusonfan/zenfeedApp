package com.ddyy.zenfeed.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ddyy.zenfeed.data.SettingsDataStore
import com.ddyy.zenfeed.data.network.ApiClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * 设置页面的UI状态
 */
data class SettingsUiState(
    val apiUrl: String = "",
    val backendUrl: String = "",
    val proxyEnabled: Boolean = false,
    val proxyType: String = "HTTP",
    val proxyHost: String = "",
    val proxyPort: Int = 8080,
    val proxyUsername: String = "",
    val proxyPassword: String = "",
    val themeMode: String = "system",
    val checkUpdateOnStart: Boolean = true,
    val isLoading: Boolean = false,
    val message: String = ""
)

/**
 * 设置页面的ViewModel
 * 管理设置页面的状态和业务逻辑
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    
    private val settingsDataStore = SettingsDataStore(application.applicationContext)
    
    private val _uiState = MutableStateFlow(SettingsUiState())
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()
    
    private var currentInputApiUrl = ""
    private var currentInputBackendUrl = ""
    private var currentProxyEnabled = false
    private var currentInputProxyType = "HTTP"
    private var currentInputProxyHost = ""
    private var currentInputProxyPort = 8080
    private var currentInputProxyUsername = ""
    private var currentInputProxyPassword = ""
    private var currentThemeMode = "system"
    private var currentCheckUpdateOnStart = true

    init {
        // 初始化时加载当前设置
        loadCurrentSettings()
    }
    
    /**
     * 加载当前设置
     */
    private fun loadCurrentSettings() {
        viewModelScope.launch {
            // 加载所有设置
            settingsDataStore.apiBaseUrl.collect { apiUrl ->
                val backendUrl = settingsDataStore.backendUrl.first()
                val proxyEnabled = settingsDataStore.proxyEnabled.first()
                val proxyType = settingsDataStore.proxyType.first()
                val proxyHost = settingsDataStore.proxyHost.first()
                val proxyPort = settingsDataStore.proxyPort.first()
                val proxyUsername = settingsDataStore.proxyUsername.first()
                val proxyPassword = settingsDataStore.proxyPassword.first()
                
                _uiState.value = _uiState.value.copy(
                    apiUrl = apiUrl,
                    backendUrl = backendUrl,
                    proxyEnabled = proxyEnabled,
                    proxyType = proxyType,
                    proxyHost = proxyHost,
                    proxyPort = proxyPort,
                    proxyUsername = proxyUsername,
                    proxyPassword = proxyPassword,
                    themeMode = settingsDataStore.themeMode.first(),
                    checkUpdateOnStart = settingsDataStore.checkUpdateOnStart.first()
                )
                
                if (currentInputApiUrl.isEmpty()) {
                    currentInputApiUrl = apiUrl
                }
                if (currentInputBackendUrl.isEmpty()) {
                    currentInputBackendUrl = backendUrl
                }
                currentProxyEnabled = proxyEnabled
                if (currentInputProxyType.isEmpty()) {
                    currentInputProxyType = proxyType
                }
                if (currentInputProxyHost.isEmpty()) {
                    currentInputProxyHost = proxyHost
                }
                currentInputProxyPort = proxyPort
                if (currentInputProxyUsername.isEmpty()) {
                    currentInputProxyUsername = proxyUsername
                }
                if (currentInputProxyPassword.isEmpty()) {
                    currentInputProxyPassword = proxyPassword
                }
                currentThemeMode = settingsDataStore.themeMode.first()
                currentCheckUpdateOnStart = settingsDataStore.checkUpdateOnStart.first()
            }
        }
    }
    
    /**
     * 更新输入的API地址
     * @param url 用户输入的API地址
     */
    fun updateApiUrl(url: String) {
        currentInputApiUrl = url
    }
    
    /**
     * 更新输入的后端URL
     * @param url 用户输入的后端URL
     */
    fun updateBackendUrl(url: String) {
        currentInputBackendUrl = url
    }
    
    /**
     * 更新代理启用状态
     */
    fun updateProxyEnabled(enabled: Boolean) {
        currentProxyEnabled = enabled
    }
    
    /**
     * 更新代理类型
     */
    fun updateProxyType(type: String) {
        currentInputProxyType = type
    }
    
    /**
     * 更新代理主机地址
     */
    fun updateProxyHost(host: String) {
        currentInputProxyHost = host
    }
    
    /**
     * 更新代理端口
     */
    fun updateProxyPort(port: Int) {
        currentInputProxyPort = port
    }
    
    /**
     * 更新代理用户名
     */
    fun updateProxyUsername(username: String) {
        currentInputProxyUsername = username
    }
    
    /**
     * 更新代理密码
     */
    fun updateProxyPassword(password: String) {
        currentInputProxyPassword = password
    }
    
    /**
     * 更新主题模式
     * @param mode 主题模式，可以是 "light", "dark", "system"
     */
    fun updateThemeMode(mode: String) {
        currentThemeMode = mode
    }
    
    /**
     * 更新启动时检查更新的设置
     * @param enabled 是否启用
     */
    fun updateCheckUpdateOnStart(enabled: Boolean) {
        currentCheckUpdateOnStart = enabled
    }

    /**
     * 保存API地址设置
     */
    fun saveApiUrl() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val trimmedUrl = currentInputApiUrl.trim()
                
                // 验证URL格式
                if (trimmedUrl.isEmpty()) {
                    showMessage("请输入API服务器地址")
                    return@launch
                }
                
                if (!settingsDataStore.isValidUrl(trimmedUrl)) {
                    showMessage("请输入有效的API服务器地址（需以 http:// 或 https:// 开头）")
                    return@launch
                }
                
                // 格式化URL并保存
                val formattedUrl = settingsDataStore.formatUrl(trimmedUrl)
                settingsDataStore.saveApiBaseUrl(formattedUrl)
                
                showMessage("API地址设置已保存")
                
            } catch (e: Exception) {
                showMessage("保存失败：${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    /**
     * 保存后端URL设置
     */
    fun saveBackendUrl() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val trimmedUrl = currentInputBackendUrl.trim()
                
                // 验证URL格式
                if (trimmedUrl.isEmpty()) {
                    showMessage("请输入后端URL")
                    return@launch
                }
                
                // 格式化URL并保存
                settingsDataStore.saveBackendUrl(trimmedUrl)
                
                showMessage("后端URL设置已保存")
                
            } catch (e: Exception) {
                showMessage("保存失败：${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    /**
     * 保存所有设置
     */
    fun saveAllSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                val trimmedApiUrl = currentInputApiUrl.trim()
                val trimmedBackendUrl = currentInputBackendUrl.trim()
                
                // 验证API URL格式
                if (trimmedApiUrl.isEmpty()) {
                    showMessage("请输入API服务器地址")
                    return@launch
                }
                
                if (!settingsDataStore.isValidUrl(trimmedApiUrl)) {
                    showMessage("请输入有效的API服务器地址（需以 http:// 或 https:// 开头）")
                    return@launch
                }
                
                // 验证后端URL
                if (trimmedBackendUrl.isEmpty()) {
                    showMessage("请输入后端URL")
                    return@launch
                }
                
                // 验证代理设置
                if (currentProxyEnabled) {
                    val trimmedProxyHost = currentInputProxyHost.trim()
                    if (trimmedProxyHost.isEmpty()) {
                        showMessage("启用代理时，请输入代理主机地址")
                        return@launch
                    }
                    if (currentInputProxyPort <= 0 || currentInputProxyPort > 65535) {
                        showMessage("请输入有效的代理端口（1-65535）")
                        return@launch
                    }
                }
                
                // 保存所有设置
                val formattedApiUrl = settingsDataStore.formatUrl(trimmedApiUrl)
                settingsDataStore.saveApiBaseUrl(formattedApiUrl)
                settingsDataStore.saveBackendUrl(trimmedBackendUrl)
                settingsDataStore.saveProxySettings(
                    enabled = currentProxyEnabled,
                    type = currentInputProxyType,
                    host = currentInputProxyHost.trim(),
                    port = currentInputProxyPort,
                    username = currentInputProxyUsername.trim(),
                    password = currentInputProxyPassword
                )
                
                // 刷新API客户端以应用新的设置
                ApiClient.refreshApiService(getApplication())
                
                showMessage("所有设置已保存")
                
            } catch (e: Exception) {
                showMessage("保存失败：${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    /**
     * 保存代理设置
     */
    fun saveProxySettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // 验证代理设置
                if (currentProxyEnabled) {
                    val trimmedProxyHost = currentInputProxyHost.trim()
                    if (trimmedProxyHost.isEmpty()) {
                        showMessage("启用代理时，请输入代理主机地址")
                        return@launch
                    }
                    if (currentInputProxyPort <= 0 || currentInputProxyPort > 65535) {
                        showMessage("请输入有效的代理端口（1-65535）")
                        return@launch
                    }
                }
                
                // 保存代理设置
                settingsDataStore.saveProxySettings(
                    enabled = currentProxyEnabled,
                    type = currentInputProxyType,
                    host = currentInputProxyHost.trim(),
                    port = currentInputProxyPort,
                    username = currentInputProxyUsername.trim(),
                    password = currentInputProxyPassword
                )
                
                // 刷新API客户端以应用新的代理设置
                ApiClient.refreshApiService(getApplication())
                
                if (currentProxyEnabled) {
                    showMessage("代理设置已保存并启用")
                } else {
                    showMessage("代理已禁用")
                }
                
            } catch (e: Exception) {
                showMessage("保存代理设置失败：${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    /**
     * 保存主题设置
     */
    fun saveThemeSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                // 保存主题设置
                settingsDataStore.saveThemeMode(currentThemeMode)
                showMessage("主题设置已保存")
                
            } catch (e: Exception) {
                showMessage("保存主题设置失败：${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    /**
     * 保存启动时检查更新的设置
     */
    fun saveCheckUpdateOnStart() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                settingsDataStore.saveCheckUpdateOnStart(currentCheckUpdateOnStart)
                showMessage("设置已保存")
                
            } catch (e: Exception) {
                showMessage("保存失败：${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    /**
     * 重置所有设置到默认值
     */
    fun resetAllSettings() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            
            try {
                settingsDataStore.resetAllSettings()
                currentInputApiUrl = SettingsDataStore.DEFAULT_API_BASE_URL
                currentInputBackendUrl = SettingsDataStore.DEFAULT_BACKEND_URL
                currentProxyEnabled = SettingsDataStore.DEFAULT_PROXY_ENABLED
                currentInputProxyType = SettingsDataStore.DEFAULT_PROXY_TYPE
                currentInputProxyHost = SettingsDataStore.DEFAULT_PROXY_HOST
                currentInputProxyPort = SettingsDataStore.DEFAULT_PROXY_PORT
                currentInputProxyUsername = SettingsDataStore.DEFAULT_PROXY_USERNAME
                currentInputProxyPassword = SettingsDataStore.DEFAULT_PROXY_PASSWORD
                currentThemeMode = SettingsDataStore.DEFAULT_THEME_MODE
                currentCheckUpdateOnStart = SettingsDataStore.DEFAULT_CHECK_UPDATE_ON_START

                // 刷新API客户端以应用重置的设置
                ApiClient.refreshApiService(getApplication())
                
                showMessage("已重置为默认设置")
                
            } catch (e: Exception) {
                showMessage("重置失败：${e.message}")
            } finally {
                _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }
    
    /**
     * 显示消息
     * @param message 要显示的消息
     */
    private fun showMessage(message: String) {
        _uiState.value = _uiState.value.copy(message = message)
    }
    
    /**
     * 清除消息
     */
    fun clearMessage() {
        _uiState.value = _uiState.value.copy(message = "")
    }
    
    /**
     * 获取当前输入的API URL（用于UI显示）
     */
    fun getCurrentInputApiUrl(): String = currentInputApiUrl
    
    /**
     * 获取当前输入的后端URL（用于UI显示）
     */
    fun getCurrentInputBackendUrl(): String = currentInputBackendUrl
}