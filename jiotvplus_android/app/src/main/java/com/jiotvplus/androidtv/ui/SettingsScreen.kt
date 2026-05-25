package com.jiotvplus.androidtv.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Switch
import androidx.tv.material3.Text
import com.jiotvplus.androidtv.data.local.SettingsDataStore
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.Alignment
import androidx.tv.material3.Button

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val dataStore: SettingsDataStore
) : ViewModel() {
    val autoPlay = dataStore.autoPlay
    val autoStart = dataStore.autoStart

    fun setAutoPlay(enabled: Boolean) {
        viewModelScope.launch { dataStore.saveAutoPlay(enabled) }
    }

    fun setAutoStart(enabled: Boolean) {
        viewModelScope.launch { dataStore.saveAutoStart(enabled) }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = viewModel(),
    onBack: () -> Unit
) {
    val autoPlay by viewModel.autoPlay.collectAsState(initial = false)
    val autoStart by viewModel.autoStart.collectAsState(initial = false)

    Column(modifier = Modifier.fillMaxSize().background(Color.Black).padding(32.dp)) {
        Button(onClick = onBack, modifier = Modifier.padding(bottom = 24.dp)) {
            Text("Back")
        }

        Text("Settings", fontSize = 28.sp, color = Color.White, modifier = Modifier.padding(bottom = 24.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(bottom = 16.dp)) {
            Text("Play last played channel on startup", color = Color.White, modifier = Modifier.weight(1f))
            Switch(checked = autoPlay, onCheckedChange = { viewModel.setAutoPlay(it) })
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Auto start app on TV start", color = Color.White, modifier = Modifier.weight(1f))
            Switch(checked = autoStart, onCheckedChange = { viewModel.setAutoStart(it) })
        }
    }
}
