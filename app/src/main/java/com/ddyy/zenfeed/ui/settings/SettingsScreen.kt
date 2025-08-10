package com.ddyy.zenfeed.ui.settings

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ddyy.zenfeed.data.SettingsDataStore
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    modifier: Modifier = Modifier,
    settingsViewModel: SettingsViewModel = viewModel()
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val scrollState = rememberScrollState()
    
    // 订阅ViewModel中的状态
    val uiState by settingsViewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // 当有消息需要显示时，显示Snackbar
    LaunchedEffect(uiState.message) {
        if (uiState.message.isNotEmpty()) {
            snackbarHostState.showSnackbar(uiState.message)
            settingsViewModel.clearMessage()
        }
    }
    
    Scaffold(
        modifier = modifier,
        topBar = {
            CenterAlignedTopAppBar(
                title = { 
                    Text(
                        text = "设置",
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "返回"
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(scrollState)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // API地址设置卡片
            ApiUrlSettingCard(
                currentApiUrl = uiState.apiUrl,
                currentBackendUrl = uiState.backendUrl,
                isLoading = uiState.isLoading,
                onApiUrlChange = settingsViewModel::updateApiUrl,
                onBackendUrlChange = settingsViewModel::updateBackendUrl,
                onSaveAll = settingsViewModel::saveAllSettings,
                onReset = settingsViewModel::resetAllSettings
            )
            
            // 代理设置卡片
            ProxySettingCard(
                proxyEnabled = uiState.proxyEnabled,
                proxyHost = uiState.proxyHost,
                proxyPort = uiState.proxyPort,
                proxyUsername = uiState.proxyUsername,
                proxyPassword = uiState.proxyPassword,
                isLoading = uiState.isLoading,
                onProxyEnabledChange = settingsViewModel::updateProxyEnabled,
                onProxyHostChange = settingsViewModel::updateProxyHost,
                onProxyPortChange = settingsViewModel::updateProxyPort,
                onProxyUsernameChange = settingsViewModel::updateProxyUsername,
                onProxyPasswordChange = settingsViewModel::updateProxyPassword,
                onSaveProxy = settingsViewModel::saveProxySettings
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ApiUrlSettingCard(
    currentApiUrl: String,
    currentBackendUrl: String,
    isLoading: Boolean,
    onApiUrlChange: (String) -> Unit,
    onBackendUrlChange: (String) -> Unit,
    onSaveAll: () -> Unit,
    onReset: () -> Unit
) {
    var tempApiUrl by remember(currentApiUrl) { mutableStateOf(currentApiUrl) }
    var tempBackendUrl by remember(currentBackendUrl) { mutableStateOf(currentBackendUrl) }
    val hasChanges = tempApiUrl != currentApiUrl || tempBackendUrl != currentBackendUrl
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 卡片标题
            Text(
                text = "服务器配置",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = "配置应用连接的API服务器地址和后端URL",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // API URL输入框
            OutlinedTextField(
                value = tempApiUrl,
                onValueChange = { tempApiUrl = it },
                label = { Text("API服务器地址") },
                placeholder = { Text("https://zenfeed.xyz/") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                ),
                supportingText = { Text("API服务的基础地址，需以 http:// 或 https:// 开头") }
            )

            // 后端URL输入框
            OutlinedTextField(
                value = tempBackendUrl,
                onValueChange = { tempBackendUrl = it },
                label = { Text("后端URL") },
                placeholder = { Text("http://zenfeed:1300") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                ),
                supportingText = { Text("后端服务的URL地址") }
            )
            
            // 按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 重置按钮
                OutlinedButton(
                    onClick = {
                        tempApiUrl = SettingsDataStore.DEFAULT_API_BASE_URL
                        tempBackendUrl = SettingsDataStore.DEFAULT_BACKEND_URL
                        onReset()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Restore,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("重置")
                }
                
                // 保存按钮
                FilledTonalButton(
                    onClick = {
                        onApiUrlChange(tempApiUrl)
                        onBackendUrlChange(tempBackendUrl)
                        onSaveAll()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading && hasChanges && tempApiUrl.trim().isNotEmpty() && tempBackendUrl.trim().isNotEmpty(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    if (isLoading) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            strokeWidth = 2.dp,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Save,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("保存")
                    }
                }
            }
            
            // 当前生效地址显示
            if (currentApiUrl.isNotEmpty()) {
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // API地址显示
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "当前API地址：",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = currentApiUrl,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // 后端URL显示
                    if (currentBackendUrl.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "当前后端URL：",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = currentBackendUrl,
                                style = MaterialTheme.typography.bodyMedium.copy(
                                    fontWeight = FontWeight.Medium
                                ),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProxySettingCard(
    proxyEnabled: Boolean,
    proxyHost: String,
    proxyPort: Int,
    proxyUsername: String,
    proxyPassword: String,
    isLoading: Boolean,
    onProxyEnabledChange: (Boolean) -> Unit,
    onProxyHostChange: (String) -> Unit,
    onProxyPortChange: (Int) -> Unit,
    onProxyUsernameChange: (String) -> Unit,
    onProxyPasswordChange: (String) -> Unit,
    onSaveProxy: () -> Unit
) {
    var tempProxyEnabled by remember(proxyEnabled) { mutableStateOf(proxyEnabled) }
    var tempProxyHost by remember(proxyHost) { mutableStateOf(proxyHost) }
    var tempProxyPort by remember(proxyPort) { mutableStateOf(proxyPort.toString()) }
    var tempProxyUsername by remember(proxyUsername) { mutableStateOf(proxyUsername) }
    var tempProxyPassword by remember(proxyPassword) { mutableStateOf(proxyPassword) }
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 卡片标题
            Text(
                text = "代理设置",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = "配置HTTP代理来访问API服务，支持用户名密码认证",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // 代理启用开关
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "启用HTTP代理",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = tempProxyEnabled,
                    onCheckedChange = {
                        tempProxyEnabled = it
                        onProxyEnabledChange(it)
                    },
                    enabled = !isLoading
                )
            }
            
            // 当代理启用时显示配置选项
            if (tempProxyEnabled) {
                // 代理主机地址
                OutlinedTextField(
                    value = tempProxyHost,
                    onValueChange = {
                        tempProxyHost = it
                        onProxyHostChange(it)
                    },
                    label = { Text("代理主机地址") },
                    placeholder = { Text("127.0.0.1 或 proxy.example.com") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                
                // 代理端口
                OutlinedTextField(
                    value = tempProxyPort,
                    onValueChange = {
                        tempProxyPort = it
                        it.toIntOrNull()?.let { port ->
                            onProxyPortChange(port)
                        }
                    },
                    label = { Text("代理端口") },
                    placeholder = { Text("8080") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                
                // 用户名（可选）
                OutlinedTextField(
                    value = tempProxyUsername,
                    onValueChange = {
                        tempProxyUsername = it
                        onProxyUsernameChange(it)
                    },
                    label = { Text("用户名（可选）") },
                    placeholder = { Text("代理认证用户名") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    )
                )
                
                // 密码（可选）
                OutlinedTextField(
                    value = tempProxyPassword,
                    onValueChange = {
                        tempProxyPassword = it
                        onProxyPasswordChange(it)
                    },
                    label = { Text("密码（可选）") },
                    placeholder = { Text("代理认证密码") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isLoading,
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary,
                        focusedLabelColor = MaterialTheme.colorScheme.primary
                    ),
                    visualTransformation = androidx.compose.ui.text.input.PasswordVisualTransformation()
                )
                
                // 代理设置的保存按钮
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // 重置代理按钮
                    OutlinedButton(
                        onClick = {
                            tempProxyEnabled = SettingsDataStore.DEFAULT_PROXY_ENABLED
                            tempProxyHost = SettingsDataStore.DEFAULT_PROXY_HOST
                            tempProxyPort = SettingsDataStore.DEFAULT_PROXY_PORT.toString()
                            tempProxyUsername = SettingsDataStore.DEFAULT_PROXY_USERNAME
                            tempProxyPassword = SettingsDataStore.DEFAULT_PROXY_PASSWORD
                            onProxyEnabledChange(SettingsDataStore.DEFAULT_PROXY_ENABLED)
                            onProxyHostChange(SettingsDataStore.DEFAULT_PROXY_HOST)
                            onProxyPortChange(SettingsDataStore.DEFAULT_PROXY_PORT)
                            onProxyUsernameChange(SettingsDataStore.DEFAULT_PROXY_USERNAME)
                            onProxyPasswordChange(SettingsDataStore.DEFAULT_PROXY_PASSWORD)
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        ),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(
                            imageVector = Icons.Default.Restore,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("重置代理")
                    }
                    
                    // 应用代理设置按钮
                    FilledTonalButton(
                        onClick = {
                            // 先更新ViewModel中的值，然后触发保存
                            onProxyEnabledChange(tempProxyEnabled)
                            onProxyHostChange(tempProxyHost)
                            tempProxyPort.toIntOrNull()?.let { onProxyPortChange(it) }
                            onProxyUsernameChange(tempProxyUsername)
                            onProxyPasswordChange(tempProxyPassword)
                            // 触发保存代理设置
                            onSaveProxy()
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !isLoading && (!tempProxyEnabled ||
                            (tempProxyEnabled && tempProxyHost.trim().isNotEmpty() && tempProxyPort.toIntOrNull() != null)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Default.Save,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("应用代理")
                        }
                    }
                }
            }
            
            // 当前代理状态显示
            if (proxyEnabled) {
                Divider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        text = "当前代理状态：已启用",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (proxyHost.isNotEmpty()) {
                        Text(
                            text = "代理地址：$proxyHost:$proxyPort",
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
