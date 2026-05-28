package com.himanshutv.apk.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.material3.Text
import coil.compose.AsyncImage
import com.himanshutv.apk.data.local.SettingsDataStore
import com.himanshutv.apk.data.model.Channel
import com.himanshutv.apk.data.repository.ChannelRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val dataStore: SettingsDataStore
) : ViewModel() {
    var channelsByCategory by mutableStateOf<Map<String, List<Channel>>>(emptyMap())
    var isLoading by mutableStateOf(true)
    var lastSelectedChannelId by mutableStateOf<String?>(null)
    var replayLastChannelEnabled by mutableStateOf(false)
    var favoriteChannelIds by mutableStateOf<Set<String>>(emptySet())
    private var allChannelsList = emptyList<Channel>()

    init {
        loadChannels()
        loadSettings()
    }

    private fun loadChannels() {
        viewModelScope.launch {
            isLoading = true
            allChannelsList = channelRepository.getChannels()
            updateGroupedChannels()
            isLoading = false
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            dataStore.replayLastChannel.collect { enabled ->
                replayLastChannelEnabled = enabled
            }
        }
        viewModelScope.launch {
            dataStore.lastPlayedChannelId.collect { lastId ->
                if (lastSelectedChannelId == null && lastId != null) {
                    lastSelectedChannelId = lastId
                }
            }
        }
        viewModelScope.launch {
            dataStore.favoriteChannels.collect { favs ->
                favoriteChannelIds = favs
                updateGroupedChannels()
            }
        }
    }

    private fun updateGroupedChannels() {
        val grouped = mutableMapOf<String, List<Channel>>()
        
        // Add Favorites at the top if there are any
        if (favoriteChannelIds.isNotEmpty() && allChannelsList.isNotEmpty()) {
            val favChannels = allChannelsList.filter { it.getResolvedId() in favoriteChannelIds }
            if (favChannels.isNotEmpty()) {
                grouped["Favorites"] = favChannels
            }
        }
        
        // Group remaining channels by category
        val restGrouped = allChannelsList.groupBy { it.getResolvedCategory() }
        grouped.putAll(restGrouped)
        
        channelsByCategory = grouped
    }

    fun setReplayLastChannel(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setReplayLastChannel(enabled)
        }
    }

    fun toggleFavorite(channelId: String) {
        viewModelScope.launch {
            if (channelId in favoriteChannelIds) {
                dataStore.removeFavorite(channelId)
            } else {
                dataStore.addFavorite(channelId)
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CategoryScreen(
    viewModel: CategoryViewModel = viewModel(),
    onChannelSelected: (Channel) -> Unit
) {
    val context = LocalContext.current
    val activity = remember(context) {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is Activity) {
                break
            }
            ctx = ctx.baseContext
        }
        ctx as? Activity
    }

    var showSettingsDialog by remember { mutableStateOf(false) }
    var showExitDialog by remember { mutableStateOf(false) }
    var activeChannelForMenu by remember { mutableStateOf<Channel?>(null) }

    // Intercept back button to show Exit Confirm dialog
    BackHandler(enabled = true) {
        showExitDialog = true
    }

    if (viewModel.isLoading) {
        Box(modifier = Modifier.fillMaxSize().background(Color(0xFF1E1E1E)), contentAlignment = Alignment.Center) {
            Text("Loading Channels...", color = Color.White)
        }
        return
    }

    val focusRequesters = remember { mutableMapOf<String, FocusRequester>() }

    // Top level focus restoration LaunchedEffect to solve race conditions during layout
    LaunchedEffect(viewModel.isLoading) {
        if (!viewModel.isLoading && viewModel.lastSelectedChannelId != null) {
            delay(150)
            focusRequesters[viewModel.lastSelectedChannelId]?.requestFocus()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().background(Color(0xFF141414)),
            contentPadding = PaddingValues(24.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "HimanshuTV",
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )

                    var isSettingsFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .onFocusChanged { isSettingsFocused = it.isFocused }
                            .clickable { showSettingsDialog = true }
                            .background(
                                color = if (isSettingsFocused) Color.White else Color(0xFF2C2C2C),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 2.dp,
                                color = if (isSettingsFocused) Color.Yellow else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Settings",
                            color = if (isSettingsFocused) Color.Black else Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
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
                            val focusRequester = focusRequesters.getOrPut(channel.getResolvedId()) { FocusRequester() }
                            
                            ChannelCard(
                                channel = channel,
                                focusRequester = focusRequester,
                                onClick = {
                                    viewModel.lastSelectedChannelId = channel.getResolvedId()
                                    onChannelSelected(channel)
                                },
                                onLongClick = {
                                    activeChannelForMenu = channel
                                }
                            )

                            LaunchedEffect(viewModel.lastSelectedChannelId) {
                                if (viewModel.lastSelectedChannelId == channel.getResolvedId()) {
                                    focusRequester.requestFocus()
                                }
                            }
                        }
                    }
                }
            }
        }

        if (showSettingsDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable { showSettingsDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(400.dp)
                        .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(12.dp))
                        .border(2.dp, Color.Yellow, shape = RoundedCornerShape(12.dp))
                        .padding(24.dp)
                        .clickable(enabled = false) {}
                ) {
                    Text(
                        text = "App Settings",
                        color = Color.White,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    var isToggleFocused by remember { mutableStateOf(false) }
                    val toggleFocusRequester = remember { FocusRequester() }
                    LaunchedEffect(showSettingsDialog) {
                        toggleFocusRequester.requestFocus()
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isToggleFocused = it.isFocused }
                            .focusRequester(toggleFocusRequester)
                            .clickable {
                                viewModel.setReplayLastChannel(!viewModel.replayLastChannelEnabled)
                            }
                            .background(
                                color = if (isToggleFocused) Color.Yellow.copy(alpha = 0.2f) else Color.Transparent,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Auto-Replay Last Channel",
                                color = if (isToggleFocused) Color.Yellow else Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Automatically plays the last watched channel on app startup",
                                color = Color.Gray,
                                fontSize = 14.sp,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                        
                        Text(
                            text = if (viewModel.replayLastChannelEnabled) "ENABLED" else "DISABLED",
                            color = if (viewModel.replayLastChannelEnabled) Color.Green else Color.Red,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    var isCloseFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .align(Alignment.End)
                            .onFocusChanged { isCloseFocused = it.isFocused }
                            .clickable { showSettingsDialog = false }
                            .background(
                                color = if (isCloseFocused) Color.Yellow else Color.Gray.copy(alpha = 0.3f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Text(
                            text = "Close",
                            color = if (isCloseFocused) Color.Black else Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        if (showExitDialog) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable { showExitDialog = false },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(12.dp))
                        .border(2.dp, Color.Yellow, shape = RoundedCornerShape(12.dp))
                        .padding(24.dp)
                        .clickable(enabled = false) {}
                ) {
                    Text(
                        text = "Exit App",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Text(
                        text = "Are you sure you want to close HimanshuTV?",
                        color = Color.Gray,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        var isCancelFocused by remember { mutableStateOf(false) }
                        val cancelFocusRequester = remember { FocusRequester() }
                        
                        LaunchedEffect(showExitDialog) {
                            cancelFocusRequester.requestFocus()
                        }
                        
                        Box(
                            modifier = Modifier
                                .onFocusChanged { isCancelFocused = it.isFocused }
                                .focusRequester(cancelFocusRequester)
                                .clickable { showExitDialog = false }
                                .background(
                                    color = if (isCancelFocused) Color.Yellow else Color.Gray.copy(alpha = 0.3f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Cancel",
                                color = if (isCancelFocused) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        Spacer(modifier = Modifier.width(16.dp))
                        
                        var isExitFocused by remember { mutableStateOf(false) }
                        Box(
                            modifier = Modifier
                                .onFocusChanged { isExitFocused = it.isFocused }
                                .clickable { activity?.finish() }
                                .background(
                                    color = if (isExitFocused) Color.Yellow else Color.Red.copy(alpha = 0.8f),
                                    shape = RoundedCornerShape(8.dp)
                                )
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Text(
                                text = "Exit",
                                color = if (isExitFocused) Color.Black else Color.White,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }

        if (activeChannelForMenu != null) {
            val channel = activeChannelForMenu!!
            val isFav = channel.getResolvedId() in viewModel.favoriteChannelIds
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.8f))
                    .clickable { activeChannelForMenu = null },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    modifier = Modifier
                        .width(320.dp)
                        .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(12.dp))
                        .border(2.dp, Color.Yellow, shape = RoundedCornerShape(12.dp))
                        .padding(24.dp)
                        .clickable(enabled = false) {}
                ) {
                    Text(
                        text = channel.getResolvedName(),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    var isClickable by remember { mutableStateOf(false) }
                    var isFavOptionFocused by remember { mutableStateOf(false) }
                    val favOptionFocusRequester = remember { FocusRequester() }
                    LaunchedEffect(activeChannelForMenu) {
                        favOptionFocusRequester.requestFocus()
                        delay(300)
                        isClickable = true
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isFavOptionFocused = it.isFocused }
                            .focusRequester(favOptionFocusRequester)
                            .clickable(enabled = isClickable) {
                                viewModel.toggleFavorite(channel.getResolvedId())
                                activeChannelForMenu = null
                            }
                            .background(
                                color = if (isFavOptionFocused) Color.Yellow else Color.Gray.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = if (isFav) "Remove from Favorites" else "Add to Favorites",
                            color = if (isFavOptionFocused) Color.Black else Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    var isCancelOptionFocused by remember { mutableStateOf(false) }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .onFocusChanged { isCancelOptionFocused = it.isFocused }
                            .clickable(enabled = isClickable) { activeChannelForMenu = null }
                            .background(
                                color = if (isCancelOptionFocused) Color.Yellow else Color.Gray.copy(alpha = 0.1f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(16.dp)
                    ) {
                        Text(
                            text = "Cancel",
                            color = if (isCancelOptionFocused) Color.Black else Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelCard(
    channel: Channel,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    var downTime by remember { mutableStateOf(0L) }
    var isLongClickTriggered by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .width(140.dp)
            .height(100.dp)
            .onFocusChanged { isFocused = it.isFocused }
            .focusRequester(focusRequester)
            .onKeyEvent { keyEvent ->
                val nativeEvent = keyEvent.nativeKeyEvent
                val keyCode = nativeEvent.keyCode
                val action = nativeEvent.action
                if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER || keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                    if (action == android.view.KeyEvent.ACTION_DOWN) {
                        if (nativeEvent.repeatCount == 0) {
                            downTime = System.currentTimeMillis()
                            isLongClickTriggered = false
                            scope.launch {
                                delay(800)
                                if (downTime > 0 && !isLongClickTriggered) {
                                    isLongClickTriggered = true
                                    onLongClick()
                                }
                            }
                        }
                    } else if (action == android.view.KeyEvent.ACTION_UP) {
                        val elapsed = System.currentTimeMillis() - downTime
                        downTime = 0
                        if (!isLongClickTriggered) {
                            isLongClickTriggered = true
                            onClick()
                        }
                    }
                    true
                } else {
                    false
                }
            }
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .background(Color(0xFF2C2C2C), shape = RoundedCornerShape(8.dp))
            .border(
                width = 2.dp,
                color = if (isFocused) Color.Yellow else Color.Transparent,
                shape = RoundedCornerShape(8.dp)
            )
            .clip(RoundedCornerShape(8.dp)),
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
