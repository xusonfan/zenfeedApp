package com.ddyy.zenfeed.data.network

import android.content.Context
import android.util.Log
import com.ddyy.zenfeed.data.SettingsDataStore
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.net.InetSocketAddress
import java.net.Proxy

/**
 * API客户端管理类
 * 支持动态配置请求地址，从用户设置中读取服务器地址
 */
object ApiClient {

    @Volatile
    private var _apiService: ApiService? = null
    
    @Volatile
    private var _currentBaseUrl: String = ""
    
    @Volatile
    private var _currentProxyConfig: String = ""
    
    private val loggingInterceptor = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BODY
    }

    /**
     * 获取API服务实例，支持动态更新请求地址和代理配置
     * @param context 上下文，用于读取设置
     * @return ApiService实例
     */
    fun getApiService(context: Context): ApiService {
        val settingsDataStore = SettingsDataStore(context)
        
        // 获取当前设置
        val currentUrl = runBlocking {
            settingsDataStore.apiBaseUrl.first()
        }
        
        val proxyConfig = runBlocking {
            val enabled = settingsDataStore.proxyEnabled.first()
            val host = settingsDataStore.proxyHost.first()
            val port = settingsDataStore.proxyPort.first()
            val username = settingsDataStore.proxyUsername.first()
            val password = settingsDataStore.proxyPassword.first()
            "$enabled:$host:$port:$username:$password"
        }
        
        // 如果URL或代理配置发生变化，重新创建服务
        if (_apiService == null || _currentBaseUrl != currentUrl || _currentProxyConfig != proxyConfig) {
            synchronized(this) {
                if (_apiService == null || _currentBaseUrl != currentUrl || _currentProxyConfig != proxyConfig) {
                    _currentBaseUrl = currentUrl
                    _currentProxyConfig = proxyConfig
                    _apiService = createApiService(currentUrl, context)
                }
            }
        }
        
        return _apiService!!
    }
    
    /**
     * 创建API服务实例
     * @param baseUrl 基础URL
     * @param context 上下文，用于读取代理设置
     * @return ApiService实例
     */
    private fun createApiService(baseUrl: String, context: Context): ApiService {
        val httpClient = createHttpClient(context)
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(httpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
    
    /**
     * 创建HTTP客户端，包含代理配置
     */
    private fun createHttpClient(context: Context): OkHttpClient {
        val settingsDataStore = SettingsDataStore(context)
        val builder = OkHttpClient.Builder()
            .addInterceptor(loggingInterceptor)
            // 添加连接超时和读取超时
            .connectTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .writeTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
        
        // 配置代理
        runBlocking {
            val proxyEnabled = settingsDataStore.proxyEnabled.first()
            
            if (proxyEnabled) {
                val proxyHost = settingsDataStore.proxyHost.first()
                val proxyPort = settingsDataStore.proxyPort.first()
                val proxyUsername = settingsDataStore.proxyUsername.first()
                val proxyPassword = settingsDataStore.proxyPassword.first()
                
                Log.d("ApiClient", "代理已启用，配置信息: host=$proxyHost, port=$proxyPort")
                
                if (proxyHost.isNotEmpty() && proxyPort > 0) {
                    try {
                        // 创建代理
                        val proxy = Proxy(Proxy.Type.HTTP, InetSocketAddress(proxyHost, proxyPort))
                        builder.proxy(proxy)
                        Log.d("ApiClient", "代理设置成功: $proxyHost:$proxyPort")
                        
                        // 如果有用户名和密码，添加认证
                        if (proxyUsername.isNotEmpty() && proxyPassword.isNotEmpty()) {
                            val authenticator = object : Authenticator {
                                override fun authenticate(route: Route?, response: Response): Request? {
                                    // 检查是否是代理认证失败 (407)
                                    if (response.code == 407) {
                                        Log.d("ApiClient", "代理需要认证，添加凭据")
                                        val credential = Credentials.basic(proxyUsername, proxyPassword)
                                        return response.request.newBuilder()
                                            .header("Proxy-Authorization", credential)
                                            .build()
                                    }
                                    Log.w("ApiClient", "代理认证失败，响应代码: ${response.code}")
                                    return null
                                }
                            }
                            builder.proxyAuthenticator(authenticator)
                            Log.d("ApiClient", "代理认证器已设置")
                        }
                    } catch (e: Exception) {
                        Log.e("ApiClient", "设置代理时发生错误", e)
                    }
                } else {
                    Log.w("ApiClient", "代理配置无效: host=$proxyHost, port=$proxyPort")
                }
            } else {
                Log.d("ApiClient", "代理未启用")
            }
        }
        
        return builder.build()
    }
    
    /**
     * 强制刷新API服务实例（当设置更改时调用）
     * @param context 上下文
     */
    fun refreshApiService(context: Context) {
        synchronized(this) {
            _apiService = null
            _currentBaseUrl = ""
            _currentProxyConfig = ""
        }
        // 重新获取服务实例
        getApiService(context)
    }
    
    /**
     * 获取当前使用的基础URL
     * @return 当前基础URL
     */
    fun getCurrentBaseUrl(): String = _currentBaseUrl
    
    /**
     * 获取配置了代理的OkHttp客户端
     * @param context 上下文，用于读取代理设置
     * @return 配置了代理的OkHttpClient实例
     */
    fun getHttpClient(context: Context): OkHttpClient {
        return createHttpClient(context)
    }
}