package com.ddyy.zenfeed.ui.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.ddyy.zenfeed.R
import com.ddyy.zenfeed.data.SettingsDataStore
import com.ddyy.zenfeed.ui.theme.ZenfeedTheme

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
                        text = stringResource(R.string.settings),
                        style = MaterialTheme.typography.headlineMedium.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
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

            // AI模型配置卡片
            AiSettingsCard(
                aiApiUrl = uiState.aiApiUrl,
                aiApiKey = uiState.aiApiKey,
                aiModelName = uiState.aiModelName,
                aiPrompt = uiState.aiPrompt,
                isLoading = uiState.isLoading,
                onAiApiUrlChange = settingsViewModel::updateAiApiUrl,
                onAiApiKeyChange = settingsViewModel::updateAiApiKey,
                onAiModelNameChange = settingsViewModel::updateAiModelName,
                onAiPromptChange = settingsViewModel::updateAiPrompt,
                onSaveAiSettings = settingsViewModel::saveAiSettings
            )

            // 更新设置卡片
            UpdateSettingsCard(
                checkUpdateOnStart = uiState.checkUpdateOnStart,
                onCheckUpdateOnStartChange = {
                    settingsViewModel.updateCheckUpdateOnStart(it)
                    settingsViewModel.saveCheckUpdateOnStart()
                },
                isLoading = uiState.isLoading
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
                    Text(stringResource(R.string.reset))
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
                        Text(stringResource(R.string.save))
                    }
                }
            }
            
            // 当前生效地址显示
            if (currentApiUrl.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                
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
    
    // 检查是否有变更
    val hasChanges = tempProxyEnabled != proxyEnabled ||
                    tempProxyHost != proxyHost ||
                    tempProxyPort != proxyPort.toString() ||
                    tempProxyUsername != proxyUsername ||
                    tempProxyPassword != proxyPassword
    
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
                    visualTransformation = PasswordVisualTransformation()
                )
            }
            
            // 代理设置的保存按钮（移到条件外，确保关闭代理时也能保存）
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
                    enabled = !isLoading && hasChanges && (!tempProxyEnabled ||
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
            
            // 当前代理状态显示
            if (proxyEnabled) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                
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

@Composable
private fun UpdateSettingsCard(
    checkUpdateOnStart: Boolean,
    onCheckUpdateOnStartChange: (Boolean) -> Unit,
    isLoading: Boolean
) {
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
            Text(
                text = "更新设置",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "启动时检查更新",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Switch(
                    checked = checkUpdateOnStart,
                    onCheckedChange = onCheckUpdateOnStartChange,
                    enabled = !isLoading
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AiSettingsCard(
    aiApiUrl: String,
    aiApiKey: String,
    aiModelName: String,
    aiPrompt: String,
    isLoading: Boolean,
    onAiApiUrlChange: (String) -> Unit,
    onAiApiKeyChange: (String) -> Unit,
    onAiModelNameChange: (String) -> Unit,
    onAiPromptChange: (String) -> Unit,
    onSaveAiSettings: () -> Unit
) {
    var tempAiApiUrl by remember(aiApiUrl) { mutableStateOf(aiApiUrl) }
    var tempAiApiKey by remember(aiApiKey) { mutableStateOf(aiApiKey) }
    var tempAiModelName by remember(aiModelName) { mutableStateOf(aiModelName) }
    var tempAiPrompt by remember(aiPrompt) { mutableStateOf(aiPrompt) }
    
    // 检查是否有变更
    val hasChanges = tempAiApiUrl != aiApiUrl ||
                    tempAiApiKey != aiApiKey ||
                    tempAiModelName != aiModelName ||
                    tempAiPrompt != aiPrompt
    
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
                text = "AI模型配置",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
            
            Text(
                text = "配置AI模型的接入参数，包括API地址、密钥和模型名称",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            // AI API URL输入框
            OutlinedTextField(
                value = tempAiApiUrl,
                onValueChange = { tempAiApiUrl = it },
                label = { Text("AI API地址") },
                placeholder = { Text("https://api.openai.com/v1") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                ),
            )
            
            // AI API密钥输入框
            OutlinedTextField(
                value = tempAiApiKey,
                onValueChange = { tempAiApiKey = it },
                label = { Text("AI API密钥") },
                placeholder = { Text("sk-...") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                ),
                visualTransformation = PasswordVisualTransformation()
            )
            
            // AI模型名称输入框
            OutlinedTextField(
                value = tempAiModelName,
                onValueChange = { tempAiModelName = it },
                label = { Text("AI模型名称") },
                placeholder = { Text("gpt-4.1-mini") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !isLoading,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    focusedLabelColor = MaterialTheme.colorScheme.primary
                ),
            )
            
            // AI提示词输入框
            OutlinedTextField(
                value = tempAiPrompt,
                onValueChange = {
                    tempAiPrompt = it
                    onAiPromptChange(it)
                },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("AI提示词") },
                placeholder = { Text("请输入AI提示词，用于指导AI处理内容...") },
                supportingText = { Text("提示词将用于指导AI如何处理和分析内容") },
                maxLines = 8,
                minLines = 3
            )
            
            // 保存按钮
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 重置按钮
                OutlinedButton(
                    onClick = {
                        tempAiApiUrl = SettingsDataStore.DEFAULT_AI_API_URL
                        tempAiApiKey = SettingsDataStore.DEFAULT_AI_API_KEY
                        tempAiModelName = SettingsDataStore.DEFAULT_AI_MODEL_NAME
                        tempAiPrompt = SettingsDataStore.DEFAULT_AI_PROMPT
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
                        onAiApiUrlChange(tempAiApiUrl)
                        onAiApiKeyChange(tempAiApiKey)
                        onAiModelNameChange(tempAiModelName)
                        onAiPromptChange(tempAiPrompt)
                        onSaveAiSettings()
                    },
                    modifier = Modifier.weight(1f),
                    enabled = !isLoading && hasChanges &&
                        tempAiApiUrl.trim().isNotEmpty() &&
                        tempAiApiKey.trim().isNotEmpty() &&
                        tempAiModelName.trim().isNotEmpty(),
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
                        Text("保存配置")
                    }
                }
            }
            
            // 当前配置显示
            if (aiApiUrl.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    // API地址显示
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "当前AI API地址：",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = aiApiUrl,
                            style = MaterialTheme.typography.bodyMedium.copy(
                                fontWeight = FontWeight.Medium
                            ),
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // 模型名称显示
                    if (aiModelName.isNotEmpty()) {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "当前AI模型：",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = aiModelName,
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

@Preview(showBackground = true)
@Composable
fun UpdateSettingsCardPreview() {
    ZenfeedTheme {
        UpdateSettingsCard(
            checkUpdateOnStart = true,
            onCheckUpdateOnStartChange = {},
            isLoading = false
        )
    }
    
}
