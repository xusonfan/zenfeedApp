package com.ddyy.zenfeed

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ddyy.zenfeed.ui.SharedViewModel
import com.ddyy.zenfeed.ui.navigation.AppNavigation
import com.ddyy.zenfeed.ui.theme.ZenfeedTheme

class MainActivity : ComponentActivity() {
    private val sharedViewModel: SharedViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // 处理启动时的Intent
        handleIntent(intent)
        
        setContent {
            ZenfeedTheme {
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
}