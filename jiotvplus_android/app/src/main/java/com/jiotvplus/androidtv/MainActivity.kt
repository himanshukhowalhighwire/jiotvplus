package com.jiotvplus.androidtv

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.jiotvplus.androidtv.data.local.SettingsDataStore
import com.jiotvplus.androidtv.ui.HomeScreen
import com.jiotvplus.androidtv.ui.LoginScreen
import com.jiotvplus.androidtv.ui.PlayerScreen
import com.jiotvplus.androidtv.ui.SettingsScreen
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var dataStore: SettingsDataStore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val navController = rememberNavController()
            val accessToken by dataStore.accessToken.collectAsState(initial = null)
            val ssoToken by dataStore.ssoToken.collectAsState(initial = null)
            val autoPlay by dataStore.autoPlay.collectAsState(initial = false)
            val lastChannelId by dataStore.lastChannelId.collectAsState(initial = null)

            val startDestination = if (accessToken != null || ssoToken != null) {
                val fromBoot = intent.getBooleanExtra("from_boot", false)
                if (fromBoot && autoPlay && lastChannelId != null) "player/$lastChannelId"
                else "home"
            } else "login"

            NavHost(navController = navController, startDestination = startDestination) {
                composable("login") {
                    LoginScreen(
                        viewModel = hiltViewModel(),
                        onLoginSuccess = {
                            navController.navigate("home") {
                                popUpTo("login") { inclusive = true }
                            }
                        }
                    )
                }
                composable("home") {
                    HomeScreen(
                        viewModel = hiltViewModel(),
                        onChannelClick = { channelId ->
                            navController.navigate("player/$channelId")
                        },
                        onSettingsClick = {
                            navController.navigate("settings")
                        }
                    )
                }
                composable("player/{channelId}") { backStackEntry ->
                    val channelId = backStackEntry.arguments?.getString("channelId") ?: return@composable
                    PlayerScreen(
                        channelId = channelId,
                        viewModel = hiltViewModel()
                    )
                }
                composable("settings") {
                    SettingsScreen(
                        viewModel = hiltViewModel(),
                        onBack = { navController.popBackStack() }
                    )
                }
            }
        }
    }
}
