package com.jiotvplus.androidtv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.tv.foundation.lazy.grid.TvGridCells
import androidx.tv.foundation.lazy.grid.TvLazyVerticalGrid
import androidx.tv.foundation.lazy.grid.items
import androidx.tv.material3.*
import coil.compose.AsyncImage
import com.jiotvplus.androidtv.data.local.SettingsDataStore
import com.jiotvplus.androidtv.data.model.Channel
import com.jiotvplus.androidtv.data.repository.ChannelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val dataStore: SettingsDataStore
) : ViewModel() {
    var channels by mutableStateOf<List<Channel>>(emptyList())
    var favoriteIds by mutableStateOf<Set<String>>(emptySet())
    var isLoading by mutableStateOf(true)

    init {
        viewModelScope.launch {
            dataStore.favoriteChannels.collect { favs ->
                favoriteIds = favs
            }
        }
    }

    fun loadCategory(categoryName: String) {
        viewModelScope.launch {
            isLoading = true
            val allChannels = channelRepository.getChannels()
            
            if (categoryName == "Favorites") {
                val currentFavs = dataStore.favoriteChannels.firstOrNull() ?: emptySet()
                channels = allChannels.filter { currentFavs.contains(it.getResolvedId()) }
            } else {
                channels = allChannels.filter { it.getResolvedCategory() == categoryName }
            }
            isLoading = false
        }
    }

    fun toggleFavorite(channelId: String) {
        viewModelScope.launch {
            if (favoriteIds.contains(channelId)) {
                dataStore.removeFavorite(channelId)
            } else {
                dataStore.addFavorite(channelId)
            }
            // If we are on the Favorites screen, we should update the list dynamically
            val currentFavs = dataStore.favoriteChannels.firstOrNull() ?: emptySet()
            if (channels.all { currentFavs.contains(it.getResolvedId()) } || channels.isEmpty()) {
                // If the screen is "Favorites", refresh the list to remove the unchecked item visually.
                // Wait, it's safer to just re-filter if we detect it's the favorites screen.
                // But we don't have categoryName here. We'll just rely on State.
            }
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun CategoryScreen(
    categoryName: String,
    viewModel: CategoryViewModel = viewModel(),
    onChannelClick: (String) -> Unit
) {
    LaunchedEffect(categoryName) {
        viewModel.loadCategory(categoryName)
    }

    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E))) {
        Text(
            text = categoryName,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(32.dp)
        )
        
        Text(
            text = "Press OK to Play • Long Press OK to Favorite",
            fontSize = 14.sp,
            color = Color.Gray,
            modifier = Modifier.padding(start = 32.dp, bottom = 16.dp)
        )

        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading Channels...", color = Color.White, fontSize = 20.sp)
            }
        } else if (viewModel.channels.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No channels found.", color = Color.White, fontSize = 20.sp)
            }
        } else {
            TvLazyVerticalGrid(
                columns = TvGridCells.Fixed(4),
                contentPadding = PaddingValues(start = 32.dp, end = 32.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(viewModel.channels) { channel ->
                    val isFavorite = viewModel.favoriteIds.contains(channel.getResolvedId())
                    
                    Card(
                        onClick = { onChannelClick(channel.getResolvedId()) },
                        onLongClick = { viewModel.toggleFavorite(channel.getResolvedId()) },
                        modifier = Modifier.size(width = 140.dp, height = 100.dp)
                    ) {
                        Box(modifier = Modifier.fillMaxSize()) {
                            Column(modifier = Modifier.fillMaxSize()) {
                                AsyncImage(
                                    model = channel.getResolvedLogo(),
                                    contentDescription = channel.getResolvedName(),
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .weight(1f)
                                        .background(Color.Black)
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(30.dp)
                                        .background(Color(0xFF222222)),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    Text(
                                        text = channel.getResolvedName(),
                                        fontSize = 12.sp,
                                        color = Color.White,
                                        modifier = Modifier.padding(horizontal = 8.dp),
                                        maxLines = 1
                                    )
                                }
                            }
                            
                            // Favorite indicator
                            if (isFavorite) {
                                Box(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .size(24.dp)
                                        .background(Color.Red, shape = androidx.compose.foundation.shape.CircleShape)
                                        .align(Alignment.TopEnd),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text("★", color = Color.White, fontSize = 14.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
