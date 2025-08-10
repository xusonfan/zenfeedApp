package com.ddyy.zenfeed.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.ddyy.zenfeed.ui.SharedViewModel
import com.ddyy.zenfeed.ui.feeds.FeedDetailScreen
import com.ddyy.zenfeed.ui.feeds.FeedsScreen
import com.ddyy.zenfeed.ui.feeds.FeedsViewModel
import com.ddyy.zenfeed.ui.settings.SettingsScreen
import com.ddyy.zenfeed.ui.settings.SettingsViewModel

@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val sharedViewModel: SharedViewModel = androidx.lifecycle.viewmodel.compose.viewModel()

    NavHost(
        navController = navController,
        startDestination = "feeds"
    ) {
        composable("feeds") {
            val feedsViewModel = androidx.lifecycle.viewmodel.compose.viewModel<FeedsViewModel>()
            FeedsScreen(
                feedsUiState = feedsViewModel.feedsUiState,
                selectedCategory = feedsViewModel.selectedCategory,
                isRefreshing = feedsViewModel.isRefreshing,
                onFeedClick = { feed ->
                    sharedViewModel.selectFeed(feed)
                    navController.navigate("feedDetail")
                },
                onCategorySelected = { category ->
                    feedsViewModel.selectCategory(category)
                },
                onRefresh = {
                    feedsViewModel.refreshFeeds()
                },
                onSettingsClick = {
                    navController.navigate("settings")
                }
            )
        }
        composable("feedDetail") {
            val feed = sharedViewModel.selectedFeed
            if (feed != null) {
                FeedDetailScreen(feed = feed, onBack = { navController.popBackStack() })
            }
        }
        composable("settings") {
            val settingsViewModel = androidx.lifecycle.viewmodel.compose.viewModel<SettingsViewModel>()
            SettingsScreen(
                navController = navController,
                settingsViewModel = settingsViewModel
            )
        }
    }
}