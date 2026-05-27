package com.himanshutv.apk

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.himanshutv.apk.data.repository.AuthRepository
import com.himanshutv.apk.ui.CategoryScreen
import com.himanshutv.apk.ui.CategoryViewModel
import com.himanshutv.apk.ui.LoginScreen
import com.himanshutv.apk.ui.LoginViewModel
import com.himanshutv.apk.ui.PlayerScreen
import com.himanshutv.apk.ui.PlayerViewModel
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var authRepository: AuthRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().background(Color.Black),
                    color = Color.Black
                ) {
                    val navController = rememberNavController()
                    var startDestination by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(Unit) {
                        startDestination = if (authRepository.isLoggedIn()) {
                            "home"
                        } else {
                            "login"
                        }
                    }

                    if (startDestination == null) {
                        Box(modifier = Modifier.fillMaxSize().background(Color.Black))
                        return@Surface
                    }

                    NavHost(navController = navController, startDestination = startDestination!!) {
                        composable("login") {
                            val viewModel: LoginViewModel = hiltViewModel()
                            LoginScreen(
                                viewModel = viewModel,
                                onLoginSuccess = {
                                    navController.navigate("home") {
                                        popUpTo("login") { inclusive = true }
                                    }
                                }
                            )
                        }
                        composable("home") {
                            val viewModel: CategoryViewModel = hiltViewModel()
                            CategoryScreen(
                                viewModel = viewModel,
                                onChannelSelected = { channel ->
                                    val id = channel.getResolvedId()
                                    if (id.isNotEmpty()) {
                                        navController.navigate("player/$id")
                                    }
                                }
                            )
                        }
                        composable(
                            route = "player/{contentId}",
                            arguments = listOf(navArgument("contentId") { type = NavType.StringType })
                        ) { backStackEntry ->
                            val contentId = backStackEntry.arguments?.getString("contentId") ?: return@composable
                            val viewModel: PlayerViewModel = hiltViewModel()
                            PlayerScreen(
                                contentId = contentId,
                                viewModel = viewModel
                            )
                        }
                    }
                }
            }
        }
    }
}
