package com.ddyy.zenfeed.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// 扩展属性，创建DataStore实例
val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

/**
 * 设置数据存储管理类
 * 用于保存和读取用户的应用设置，包括API请求地址等配置
 */
class SettingsDataStore(private val context: Context) {
    
    companion object {
        // 定义存储的键
        private val API_BASE_URL_KEY = stringPreferencesKey("api_base_url")
        private val BACKEND_URL_KEY = stringPreferencesKey("backend_url")
        private val PROXY_ENABLED_KEY = booleanPreferencesKey("proxy_enabled")
        private val PROXY_TYPE_KEY = stringPreferencesKey("proxy_type")
        private val PROXY_HOST_KEY = stringPreferencesKey("proxy_host")
        private val PROXY_PORT_KEY = intPreferencesKey("proxy_port")
        private val PROXY_USERNAME_KEY = stringPreferencesKey("proxy_username")
        private val PROXY_PASSWORD_KEY = stringPreferencesKey("proxy_password")
        private val THEME_MODE_KEY = stringPreferencesKey("theme_mode")
        private val CHECK_UPDATE_ON_START_KEY = booleanPreferencesKey("check_update_on_start")
        
        // AI模型配置相关的键
        private val AI_API_URL_KEY = stringPreferencesKey("ai_api_url")
        private val AI_API_KEY_KEY = stringPreferencesKey("ai_api_key")
        private val AI_MODEL_NAME_KEY = stringPreferencesKey("ai_model_name")
        private val AI_PROMPT_KEY = stringPreferencesKey("ai_prompt")

        // 默认的API地址
        const val DEFAULT_API_BASE_URL = "https://zenfeed.xyz/"
        const val DEFAULT_BACKEND_URL = "http://zenfeed:1300"

        // 默认的代理设置
        const val DEFAULT_PROXY_ENABLED = false
        const val DEFAULT_PROXY_TYPE = "HTTP"
        const val DEFAULT_PROXY_HOST = ""
        const val DEFAULT_PROXY_PORT = 8080
        const val DEFAULT_PROXY_USERNAME = ""
        const val DEFAULT_PROXY_PASSWORD = ""

        // 默认的主题设置
        const val DEFAULT_THEME_MODE = "system" // 可以是 "light", "dark", "system"
        const val DEFAULT_CHECK_UPDATE_ON_START = true
        
        // 默认的AI模型配置
        const val DEFAULT_AI_API_URL = "https://api.openai.com/v1"
        const val DEFAULT_AI_API_KEY = ""
        const val DEFAULT_AI_MODEL_NAME = "gpt-4.1-mini"
        const val DEFAULT_AI_PROMPT = """请总结以下网页内容的主要观点和关键信息。要求：
1. 提取文章的核心观点和主要论点
2. 总结重要的数据和事实
3. 保持客观中立的立场
4. 用简洁清晰的语言表达
5. 字数控制在300字以内

请开始总结："""
    }
    
    /**
     * 获取API基础地址的Flow
     * @return Flow<String> API基础地址的数据流，默认为DEFAULT_API_BASE_URL
     */
    val apiBaseUrl: Flow<String> = context.settingsDataStore.data
        .map { preferences ->
            preferences[API_BASE_URL_KEY] ?: DEFAULT_API_BASE_URL
        }
    
    /**
     * 获取后端URL的Flow
     * @return Flow<String> 后端URL的数据流，默认为DEFAULT_BACKEND_URL
     */
    val backendUrl: Flow<String> = context.settingsDataStore.data
        .map { preferences ->
            preferences[BACKEND_URL_KEY] ?: DEFAULT_BACKEND_URL
        }
    
    /**
     * 获取代理是否启用的Flow
     */
    val proxyEnabled: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences ->
            preferences[PROXY_ENABLED_KEY] ?: DEFAULT_PROXY_ENABLED
        }
    
    /**
     * 获取代理类型的Flow
     */
    val proxyType: Flow<String> = context.settingsDataStore.data
        .map { preferences ->
            preferences[PROXY_TYPE_KEY] ?: DEFAULT_PROXY_TYPE
        }
    
    /**
     * 获取代理主机地址的Flow
     */
    val proxyHost: Flow<String> = context.settingsDataStore.data
        .map { preferences ->
            preferences[PROXY_HOST_KEY] ?: DEFAULT_PROXY_HOST
        }
    
    /**
     * 获取代理端口的Flow
     */
    val proxyPort: Flow<Int> = context.settingsDataStore.data
        .map { preferences ->
            preferences[PROXY_PORT_KEY] ?: DEFAULT_PROXY_PORT
        }
    
    /**
     * 获取代理用户名的Flow
     */
    val proxyUsername: Flow<String> = context.settingsDataStore.data
        .map { preferences ->
            preferences[PROXY_USERNAME_KEY] ?: DEFAULT_PROXY_USERNAME
        }
    
    /**
     * 获取代理密码的Flow
     */
    val proxyPassword: Flow<String> = context.settingsDataStore.data
        .map { preferences ->
            preferences[PROXY_PASSWORD_KEY] ?: DEFAULT_PROXY_PASSWORD
        }
        
        /**
         * 获取主题模式的Flow
         */
        val themeMode: Flow<String> = context.settingsDataStore.data
            .map { preferences ->
                preferences[THEME_MODE_KEY] ?: DEFAULT_THEME_MODE
            }
        
    /**
     * 获取启动时检查更新设置的Flow
     */
    val checkUpdateOnStart: Flow<Boolean> = context.settingsDataStore.data
        .map { preferences ->
            preferences[CHECK_UPDATE_ON_START_KEY] ?: DEFAULT_CHECK_UPDATE_ON_START
        }
    
    /**
     * 获取AI API地址的Flow
     */
    val aiApiUrl: Flow<String> = context.settingsDataStore.data
        .map { preferences ->
            preferences[AI_API_URL_KEY] ?: DEFAULT_AI_API_URL
        }
    
    /**
     * 获取AI API密钥的Flow
     */
    val aiApiKey: Flow<String> = context.settingsDataStore.data
        .map { preferences ->
            preferences[AI_API_KEY_KEY] ?: DEFAULT_AI_API_KEY
        }
    
    /**
     * 获取AI模型名称的Flow
     */
    val aiModelName: Flow<String> = context.settingsDataStore.data
        .map { preferences ->
            preferences[AI_MODEL_NAME_KEY] ?: DEFAULT_AI_MODEL_NAME
        }
    
    /**
     * 获取AI提示词的Flow
     */
    val aiPrompt: Flow<String> = context.settingsDataStore.data
        .map { preferences ->
            preferences[AI_PROMPT_KEY] ?: DEFAULT_AI_PROMPT
        }

    /**
     * 保存API基础地址
     * @param url 要保存的API基础地址
     */
    suspend fun saveApiBaseUrl(url: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[API_BASE_URL_KEY] = url
        }
    }
    
    /**
     * 保存后端URL
     * @param url 要保存的后端URL
     */
    suspend fun saveBackendUrl(url: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[BACKEND_URL_KEY] = url
        }
    }
    
    /**
     * 保存代理设置
     */
    suspend fun saveProxySettings(
        enabled: Boolean,
        type: String = DEFAULT_PROXY_TYPE,
        host: String,
        port: Int,
        username: String = "",
        password: String = ""
    ) {
        context.settingsDataStore.edit { preferences ->
            preferences[PROXY_ENABLED_KEY] = enabled
            preferences[PROXY_TYPE_KEY] = type
            preferences[PROXY_HOST_KEY] = host
            preferences[PROXY_PORT_KEY] = port
            preferences[PROXY_USERNAME_KEY] = username
            preferences[PROXY_PASSWORD_KEY] = password
        }
    }
    
    /**
     * 保存主题设置
     * @param mode 主题模式，可以是 "light", "dark", "system"
     */
    suspend fun saveThemeMode(mode: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[THEME_MODE_KEY] = mode
        }
    }
    
    /**
     * 保存启动时检查更新的设置
     * @param enabled 是否启用
     */
    suspend fun saveCheckUpdateOnStart(enabled: Boolean) {
        context.settingsDataStore.edit { preferences ->
            preferences[CHECK_UPDATE_ON_START_KEY] = enabled
        }
    }
    
    /**
     * 保存AI API地址
     * @param url AI API地址
     */
    suspend fun saveAiApiUrl(url: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[AI_API_URL_KEY] = url
        }
    }
    
    /**
     * 保存AI API密钥
     * @param apiKey AI API密钥
     */
    suspend fun saveAiApiKey(apiKey: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[AI_API_KEY_KEY] = apiKey
        }
    }
    
    /**
     * 保存AI模型名称
     * @param modelName AI模型名称
     */
    suspend fun saveAiModelName(modelName: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[AI_MODEL_NAME_KEY] = modelName
        }
    }
    
    /**
     * 保存所有AI模型配置
     * @param apiUrl AI API地址
     * @param apiKey AI API密钥
     * @param modelName AI模型名称
     */
    suspend fun saveAiSettings(apiUrl: String, apiKey: String, modelName: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[AI_API_URL_KEY] = apiUrl
            preferences[AI_API_KEY_KEY] = apiKey
            preferences[AI_MODEL_NAME_KEY] = modelName
        }
    }
    
    /**
     * 保存AI提示词
     * @param prompt AI提示词
     */
    suspend fun saveAiPrompt(prompt: String) {
        context.settingsDataStore.edit { preferences ->
            preferences[AI_PROMPT_KEY] = prompt
        }
    }
    
    /**
     * 获取当前保存的API基础地址（同步方式，用于初始化）
     * @return 当前保存的API基础地址，如果没有则返回默认地址
     */
    fun getCurrentApiBaseUrl(): String {
        val preferences = context.settingsDataStore.data
        return preferences.map { prefs ->
            prefs[API_BASE_URL_KEY] ?: DEFAULT_API_BASE_URL
        }.toString() // 这里需要改进，应该使用first()
    }
    
    /**
     * 重置API基础地址到默认值
     */
    suspend fun resetApiBaseUrl() {
        saveApiBaseUrl(DEFAULT_API_BASE_URL)
    }
    
    /**
     * 重置后端URL到默认值
     */
    suspend fun resetBackendUrl() {
        saveBackendUrl(DEFAULT_BACKEND_URL)
    }
    
    /**
     * 重置所有设置到默认值
     */
    suspend fun resetAllSettings() {
        context.settingsDataStore.edit { preferences ->
            preferences[API_BASE_URL_KEY] = DEFAULT_API_BASE_URL
            preferences[BACKEND_URL_KEY] = DEFAULT_BACKEND_URL
            preferences[PROXY_ENABLED_KEY] = DEFAULT_PROXY_ENABLED
            preferences[PROXY_TYPE_KEY] = DEFAULT_PROXY_TYPE
            preferences[PROXY_HOST_KEY] = DEFAULT_PROXY_HOST
            preferences[PROXY_PORT_KEY] = DEFAULT_PROXY_PORT
            preferences[PROXY_USERNAME_KEY] = DEFAULT_PROXY_USERNAME
            preferences[PROXY_PASSWORD_KEY] = DEFAULT_PROXY_PASSWORD
            preferences[THEME_MODE_KEY] = DEFAULT_THEME_MODE
            preferences[CHECK_UPDATE_ON_START_KEY] = DEFAULT_CHECK_UPDATE_ON_START
            preferences[AI_API_URL_KEY] = DEFAULT_AI_API_URL
            preferences[AI_API_KEY_KEY] = DEFAULT_AI_API_KEY
            preferences[AI_MODEL_NAME_KEY] = DEFAULT_AI_MODEL_NAME
            preferences[AI_PROMPT_KEY] = DEFAULT_AI_PROMPT
        }
    }
    
    /**
     * 重置代理设置到默认值
     */
    suspend fun resetProxySettings() {
        context.settingsDataStore.edit { preferences ->
            preferences[PROXY_ENABLED_KEY] = DEFAULT_PROXY_ENABLED
            preferences[PROXY_TYPE_KEY] = DEFAULT_PROXY_TYPE
            preferences[PROXY_HOST_KEY] = DEFAULT_PROXY_HOST
            preferences[PROXY_PORT_KEY] = DEFAULT_PROXY_PORT
            preferences[PROXY_USERNAME_KEY] = DEFAULT_PROXY_USERNAME
            preferences[PROXY_PASSWORD_KEY] = DEFAULT_PROXY_PASSWORD
        }
    }
    
    /**
     * 验证URL格式是否有效
     * @param url 要验证的URL
     * @return 是否为有效的URL格式
     */
    fun isValidUrl(url: String): Boolean {
        return try {
            val cleanUrl = url.trim()
            if (cleanUrl.isEmpty()) return false
            
            // 简单的URL格式验证，支持端口号
            val urlPattern = Regex("^https?://[a-zA-Z0-9.-]+(:[0-9]+)?(/.*)?$")
            urlPattern.matches(cleanUrl) && !cleanUrl.endsWith("//")
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * 格式化URL，确保以/结尾
     * @param url 原始URL
     * @return 格式化后的URL
     */
    fun formatUrl(url: String): String {
        val cleanUrl = url.trim()
        return if (cleanUrl.endsWith("/")) {
            cleanUrl
        } else {
            "$cleanUrl/"
        }
    }
}