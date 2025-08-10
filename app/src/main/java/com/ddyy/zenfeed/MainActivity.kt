package com.ddyy.zenfeed

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.ddyy.zenfeed.ui.feeds.FeedsScreen
import com.ddyy.zenfeed.ui.feeds.FeedsViewModel
import com.ddyy.zenfeed.ui.theme.ZenfeedTheme

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3Api::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZenfeedTheme {
                Scaffold(
                    topBar = {
                        TopAppBar(title = { Text("Zenfeed") })
                    }
                ) { innerPadding ->
                    val feedsViewModel: FeedsViewModel = viewModel()
                    FeedsScreen(
                        feedsUiState = feedsViewModel.feedsUiState,
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}