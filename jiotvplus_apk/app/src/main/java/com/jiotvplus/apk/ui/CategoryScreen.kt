package com.jiotvplus.apk.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.Text
import coil.compose.AsyncImage
import com.jiotvplus.apk.data.model.Channel
import com.jiotvplus.apk.data.repository.ChannelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val channelRepository: ChannelRepository
) : ViewModel() {
    var channelsByCategory by mutableStateOf<Map<String, List<Channel>>>(emptyMap())
    var isLoading by mutableStateOf(true)

    init {
        loadChannels()
    }

    private fun loadChannels() {
        viewModelScope.launch {
            isLoading = true
            val allChannels = channelRepository.getChannels()
            channelsByCategory = allChannels.groupBy { it.getResolvedCategory() }
            isLoading = false
        }
    }
}

@Composable
fun CategoryScreen(
    viewModel: CategoryViewModel = viewModel(),
    onChannelSelected: (Channel) -> Unit
) {
    if (viewModel.isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E)), contentAlignment = Alignment.Center) {
            Text("Loading Channels...", color = Color.White)
        }
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize().background(Color(0xFF141414)),
        contentPadding = PaddingValues(24.dp)
    ) {
        item {
            Text(
                "JioTV+",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 24.dp)
            )
        }
        viewModel.channelsByCategory.forEach { (category, channels) ->
            item {
                Text(
                    text = category,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White,
                    modifier = Modifier.padding(vertical = 12.dp)
                )
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(end = 24.dp)
                ) {
                    items(channels) { channel ->
                        ChannelCard(channel = channel, onClick = { onChannelSelected(channel) })
                    }
                }
            }
        }
    }
}

@Composable
fun ChannelCard(channel: Channel, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .width(140.dp)
            .height(100.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF2C2C2C))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = channel.getResolvedLogo(),
            contentDescription = channel.getResolvedName(),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(8.dp)
        )
    }
}
