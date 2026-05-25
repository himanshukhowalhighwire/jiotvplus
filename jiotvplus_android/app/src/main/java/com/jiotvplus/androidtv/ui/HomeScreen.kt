package com.jiotvplus.androidtv.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.foundation.lazy.list.TvLazyColumn
import androidx.tv.foundation.lazy.list.TvLazyRow
import androidx.tv.foundation.lazy.list.items
import androidx.tv.material3.Card
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Text
import coil.compose.AsyncImage
import com.jiotvplus.androidtv.data.model.Channel
import com.jiotvplus.androidtv.data.repository.ChannelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val channelRepository: ChannelRepository
) : ViewModel() {
    var channels by mutableStateOf<List<Channel>>(emptyList())
    var groupedChannels by mutableStateOf<Map<String, List<Channel>>>(emptyMap())

    init {
        viewModelScope.launch {
            val list = channelRepository.getChannels()
            channels = list
            groupedChannels = list.groupBy { it.getResolvedCategory() }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onChannelClick: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E))) {
        Button(
            onClick = onSettingsClick,
            modifier = Modifier.padding(16.dp)
        ) {
            Text("Settings")
        }

        TvLazyColumn(modifier = Modifier.fillMaxSize()) {
            viewModel.groupedChannels.forEach { (category, channels) ->
                item {
                    Text(
                        text = category,
                        fontSize = 20.sp,
                        color = Color.White,
                        modifier = Modifier.padding(start = 16.dp, top = 16.dp, bottom = 8.dp)
                    )
                }
                item {
                    TvLazyRow {
                        items(channels) { channel ->
                            Card(
                                onClick = { onChannelClick(channel.getResolvedId()) },
                                modifier = Modifier.padding(horizontal = 8.dp).size(width = 160.dp, height = 120.dp)
                            ) {
                                Column {
                                    AsyncImage(
                                        model = channel.getResolvedLogo(),
                                        contentDescription = channel.getResolvedName(),
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.size(width = 160.dp, height = 90.dp).background(Color.Black)
                                    )
                                    Text(
                                        text = channel.getResolvedName(),
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
