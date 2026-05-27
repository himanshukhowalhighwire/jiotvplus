package com.himanshutv.apk

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
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
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
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

    private fun requestStoragePermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                } catch (e: Exception) {
                    val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                    startActivity(intent)
                }
            }
        } else {
            val permissions = arrayOf(
                Manifest.permission.READ_EXTERNAL_STORAGE,
                Manifest.permission.WRITE_EXTERNAL_STORAGE
            )
            val needed = permissions.filter {
                ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }
            if (needed.isNotEmpty()) {
                ActivityCompat.requestPermissions(this, needed.toTypedArray(), 100)
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        requestStoragePermissions()
        
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize().background(Color.Black),
                    color = Color.Black
                ) {
                    val navController = rememberNavController()
                    var startDestination by remember { mutableStateOf<String?>(null) }
                    var autoReplayId by remember { mutableStateOf<String?>(null) }

                    LaunchedEffect(Unit) {
                        if (authRepository.isLoggedIn()) {
                            val shouldReplay = authRepository.shouldReplayLastChannel()
                            val lastChannelId = authRepository.getLastPlayedChannelId()
                            if (shouldReplay && !lastChannelId.isNullOrEmpty()) {
                                autoReplayId = lastChannelId
                            }
                            startDestination = "home"
                        } else {
                            startDestination = "login"
                        }
                    }

                    LaunchedEffect(startDestination, autoReplayId) {
                        if (startDestination == "home" && autoReplayId != null) {
                            val id = autoReplayId
                            autoReplayId = null
                            navController.navigate("player/$id")
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
