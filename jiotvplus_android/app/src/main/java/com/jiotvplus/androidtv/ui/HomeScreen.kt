package com.jiotvplus.androidtv.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.jiotvplus.androidtv.data.repository.ChannelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    private val channelRepository: ChannelRepository
) : ViewModel() {
    var categories by mutableStateOf<List<String>>(emptyList())
    var isLoading by mutableStateOf(true)

    init {
        viewModelScope.launch {
            val list = channelRepository.getChannels()
            val grouped = list.groupBy { it.getResolvedCategory() }
            
            // Extract unique categories and ensure "Favorites" is implicitly handled in UI
            // We just provide the actual remote categories here.
            val uniqueCategories = grouped.keys.sorted().toMutableList()
            // Add Favorites manually at the top
            uniqueCategories.add(0, "Favorites")
            
            categories = uniqueCategories
            isLoading = false
        }
    }
}

@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun HomeScreen(
    viewModel: HomeViewModel = viewModel(),
    onCategoryClick: (String) -> Unit,
    onSettingsClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E))) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(32.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "JioTV+",
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Button(onClick = onSettingsClick) {
                Text("Settings")
            }
        }

        if (viewModel.isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("Loading Categories...", color = Color.White, fontSize = 20.sp)
            }
        } else {
            TvLazyVerticalGrid(
                columns = TvGridCells.Fixed(3),
                contentPadding = PaddingValues(start = 32.dp, end = 32.dp, bottom = 32.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(viewModel.categories) { category ->
                    Card(
                        onClick = { onCategoryClick(category) },
                        modifier = Modifier.fillMaxWidth().height(100.dp)
                    ) {
                        Box(
                            modifier = Modifier.fillMaxSize().background(if(category == "Favorites") Color(0xFFE50914) else Color(0xFF333333)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = category,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
