package com.himanshutv.apk.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.onPreviewKeyEvent
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
import androidx.hilt.navigation.compose.hiltViewModel

private fun FocusRequester.safeRequestFocus() {
    try {
        this.requestFocus()
    } catch (e: Exception) {
        // Ignore exception if the node is not yet attached
    }
}

@HiltViewModel
class CategoryViewModel @Inject constructor(
    private val channelRepository: ChannelRepository,
    private val dataStore: SettingsDataStore
) : ViewModel() {
    var channelsByCategory by mutableStateOf<Map<String, List<Channel>>>(emptyMap())
    var isLoading by mutableStateOf(true)
    var lastSelectedChannelId by mutableStateOf<String?>(null)
    var selectedCategory by mutableStateOf<String?>(null)
    var replayLastChannelEnabled by mutableStateOf(false)
    var autoStartOnBootEnabled by mutableStateOf(false)
    var favoriteChannelIds by mutableStateOf<Set<String>>(emptySet())
    private var allChannelsList = emptyList<Channel>()

    init {
        loadChannels()
        loadSettings()
    }

    private fun loadChannels() {
        viewModelScope.launch {
            isLoading = true
            channelRepository.getChannels(forceRefresh = false).collect { channels ->
                allChannelsList = channels
                updateGroupedChannels()
                isLoading = false
            }
        }
    }

    fun reloadChannels() {
        viewModelScope.launch {
            isLoading = true
            channelRepository.getChannels(forceRefresh = true).collect { channels ->
                allChannelsList = channels
                updateGroupedChannels()
                isLoading = false
            }
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            dataStore.replayLastChannel.collect { enabled ->
                replayLastChannelEnabled = enabled
            }
        }
        viewModelScope.launch {
            dataStore.autoStartOnBoot.collect { enabled ->
                autoStartOnBootEnabled = enabled
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
        
        if (favoriteChannelIds.isNotEmpty() && allChannelsList.isNotEmpty()) {
            val favChannels = allChannelsList.filter { it.getResolvedId() in favoriteChannelIds }
            if (favChannels.isNotEmpty()) {
                grouped["Favorites"] = favChannels
            }
        }
        
        val restGrouped = allChannelsList.groupBy { it.getResolvedCategory() }
        grouped.putAll(restGrouped)
        
        channelsByCategory = grouped
        
        if (selectedCategory == null && grouped.isNotEmpty()) {
            selectedCategory = grouped.keys.first()
        }
    }

    fun setReplayLastChannel(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setReplayLastChannel(enabled)
        }
    }

    fun setAutoStartOnBoot(enabled: Boolean) {
        viewModelScope.launch {
            dataStore.setAutoStartOnBoot(enabled)
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
    autoReplayId: String? = null,
    onReplayHandled: () -> Unit = {},
    onChannelSelected: (Channel) -> Unit,
    onNavigateToPlayer: (String) -> Unit = {},
    viewModel: CategoryViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val activity = remember(context) {
        var ctx = context
        while (ctx is android.content.ContextWrapper) {
            if (ctx is android.app.Activity) break
            ctx = ctx.baseContext
        }
        ctx as? android.app.Activity
    }
    var showExitDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var isCategoryListFocused by remember { mutableStateOf(true) }
    var activeChannelForMenu by remember { mutableStateOf<Channel?>(null) }
    var isCategorySelected by remember { mutableStateOf(false) }

    LaunchedEffect(autoReplayId, viewModel.isLoading) {
        if (autoReplayId != null && !viewModel.isLoading) {
            delay(150)
            onNavigateToPlayer(autoReplayId)
            onReplayHandled()
        }
    }

    BackHandler(enabled = showExitDialog) {
        showExitDialog = false
    }

    BackHandler(enabled = !showExitDialog && activeChannelForMenu != null) {
        activeChannelForMenu = null
    }

    BackHandler(enabled = !showExitDialog && activeChannelForMenu == null && showSettingsDialog) {
        showSettingsDialog = false
    }



    if (viewModel.isLoading) {
        Box(
            modifier = Modifier.fillMaxSize().background(Color(0xFF0F172A)), 
            contentAlignment = Alignment.Center
        ) {
            Text("Loading Channels...", color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Medium)
        }
        return
    }

    val categories = viewModel.channelsByCategory.keys.toList()
    val currentCategory = viewModel.selectedCategory ?: categories.firstOrNull() ?: ""
    val channelsInCurrentCategory = viewModel.channelsByCategory[currentCategory] ?: emptyList()
    var frozenChannels by remember { mutableStateOf(channelsInCurrentCategory) }
    
    LaunchedEffect(channelsInCurrentCategory, isCategorySelected) {
        if (isCategorySelected) {
            frozenChannels = channelsInCurrentCategory
        }
    }

    val categoryFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val channelFocusRequesters = remember { mutableMapOf<String, FocusRequester>() }
    val settingsFocusRequester = remember { FocusRequester() }

    LaunchedEffect(viewModel.isLoading) {
        if (!viewModel.isLoading && viewModel.lastSelectedChannelId != null) {
            val lastCategory = viewModel.channelsByCategory.entries.find { entry ->
                entry.value.any { it.getResolvedId() == viewModel.lastSelectedChannelId }
            }?.key
            if (lastCategory != null) {
                viewModel.selectedCategory = lastCategory
                isCategorySelected = true
                isCategoryListFocused = false
                delay(200)
                channelFocusRequesters[viewModel.lastSelectedChannelId]?.safeRequestFocus()
            }
        } else if (!viewModel.isLoading) {
            delay(150)
            categoryFocusRequesters[currentCategory]?.safeRequestFocus()
        }
    }

    LaunchedEffect(isCategoryListFocused) {
        if (isCategoryListFocused) {
            categoryFocusRequesters[currentCategory]?.safeRequestFocus()
        }
    }

    LaunchedEffect(activeChannelForMenu) {
        if (activeChannelForMenu == null && viewModel.lastSelectedChannelId != null) {
            delay(100)
            channelFocusRequesters[viewModel.lastSelectedChannelId]?.safeRequestFocus()
        }
    }


    val categoryWeight by animateFloatAsState(targetValue = if (isCategorySelected) 0.28f else 1f)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.radialGradient(
                colors = listOf(Color(0xFF0F172A), Color(0xFF020617)),
                radius = 1200f
            ))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            // LEFT PANE - Categories
            Column(
                modifier = Modifier
                    .weight(categoryWeight)
                    .fillMaxHeight()
                    .background(Brush.horizontalGradient(
                        colors = listOf(Color(0xFF000000).copy(alpha = 0.5f), Color.Transparent)
                    ))
                    .padding(vertical = 32.dp),
                horizontalAlignment = if (isCategorySelected) Alignment.Start else Alignment.CenterHorizontally
            ) {
                Text(
                    text = "HIMANSHU TV",
                    fontSize = if (isCategorySelected) 24.sp else 36.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(
                        start = if (isCategorySelected) 24.dp else 0.dp,
                        bottom = 32.dp
                    )
                )

                var isSettingsFocused by remember { mutableStateOf(false) }
                val settingsScale by animateFloatAsState(if (isSettingsFocused) 1.05f else 1f)
                
                Box(
                    modifier = Modifier
                        .fillMaxWidth(if (isCategorySelected) 1f else 0.4f)
                        .padding(
                            start = if (isCategorySelected) 16.dp else 0.dp, 
                            end = if (isCategorySelected) 16.dp else 0.dp, 
                            bottom = 16.dp
                        )
                        .scale(settingsScale)
                        .onFocusChanged { 
                            isSettingsFocused = it.isFocused
                            if (it.isFocused) {
                                isCategoryListFocused = true
                                isCategorySelected = false
                            }
                        }
                        .focusRequester(settingsFocusRequester)
                        .clickable { showSettingsDialog = true }
                        .background(
                            color = if (isSettingsFocused) Color(0xFF1E293B) else Color(0xFF0F172A).copy(alpha = 0.5f),
                            shape = RoundedCornerShape(12.dp)
                        )
                        .border(
                            width = 2.dp,
                            color = if (isSettingsFocused) Color(0xFF38BDF8) else Color.Transparent,
                            shape = RoundedCornerShape(12.dp)
                        )
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "⚙ Settings", 
                        color = if (isSettingsFocused) Color.White else Color(0xFF94A3B8), 
                        fontSize = 16.sp, 
                        fontWeight = FontWeight.Bold
                    )
                }

                LazyColumn(
                    modifier = Modifier.fillMaxWidth(if (isCategorySelected) 1f else 0.4f),
                    contentPadding = PaddingValues(
                        start = if (isCategorySelected) 16.dp else 0.dp, 
                        end = if (isCategorySelected) 16.dp else 0.dp, 
                        bottom = 24.dp
                    )
                ) {
                    items(categories) { category ->
                        val isSelected = category == currentCategory
                        var isFocused by remember { mutableStateOf(false) }
                        val scale by animateFloatAsState(if (isFocused) 1.08f else 1f)
                        
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp)
                                .scale(scale)
                                .onFocusChanged {
                                    isFocused = it.isFocused
                                    if (it.isFocused) {
                                        val wasCategorySelected = isCategorySelected
                                        isCategoryListFocused = true
                                        isCategorySelected = false
                                        if (!wasCategorySelected) {
                                            viewModel.selectedCategory = category
                                        }
                                    }
                                }
                                .focusRequester(categoryFocusRequesters.getOrPut(category) { FocusRequester() })
                                .clickable { 
                                    viewModel.selectedCategory = category
                                    isCategorySelected = true
                                }
                                .background(
                                    color = if (isFocused) Color(0xFF38BDF8).copy(alpha = 0.15f) else if (isSelected) Color(0xFFFFFFFF).copy(alpha = 0.05f) else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .border(
                                    width = if (isFocused) 2.dp else 1.dp,
                                    color = if (isFocused) Color(0xFF38BDF8) else if (isSelected) Color.White.copy(alpha = 0.1f) else Color.Transparent,
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            Text(
                                text = category.uppercase(),
                                fontSize = if (isFocused) 17.sp else 16.sp,
                                fontWeight = if (isFocused || isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isFocused) Color.White else if (isSelected) Color(0xFFE2E8F0) else Color(0xFF94A3B8),
                                letterSpacing = 1.sp
                            )
                        }
                    }
                }
            }

            // RIGHT PANE - Channels Grid
            val rightPaneWeight = 1f - categoryWeight
            
            if (rightPaneWeight > 0.05f) {
                Column(
                    modifier = Modifier
                        .weight(rightPaneWeight)
                        .fillMaxHeight()
                        .padding(start = 24.dp, top = 32.dp, end = 32.dp)
                ) {
                    androidx.compose.animation.AnimatedVisibility(
                        visible = isCategorySelected,
                        enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.slideInHorizontally(initialOffsetX = { 50 }),
                        exit = androidx.compose.animation.fadeOut()
                    ) {
                        Column {
                            Text(
                                text = currentCategory.uppercase(),
                                fontSize = 28.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                letterSpacing = 1.sp,
                                modifier = Modifier.padding(bottom = 24.dp)
                            )

                            LazyVerticalGrid(
                                columns = GridCells.Adaptive(minSize = 180.dp),
                                horizontalArrangement = Arrangement.spacedBy(20.dp),
                                verticalArrangement = Arrangement.spacedBy(20.dp),
                                contentPadding = PaddingValues(0.dp, 0.dp, 0.dp, 32.dp),
                                modifier = Modifier.fillMaxSize()
                            ) {
                                items(frozenChannels) { channel ->
                                    val focusRequester = channelFocusRequesters.getOrPut(channel.getResolvedId()) { FocusRequester() }
                                    
                                    ChannelCard(
                                        channel = channel,
                                        focusRequester = focusRequester,
                                        onClick = {
                                            viewModel.lastSelectedChannelId = channel.getResolvedId()
                                            onChannelSelected(channel)
                                        },
                                        onLongClick = {
                                            viewModel.lastSelectedChannelId = channel.getResolvedId()
                                            activeChannelForMenu = channel
                                        },
                                        onFocusGained = {
                                            isCategoryListFocused = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                Spacer(modifier = Modifier.weight(0.001f))
            }
        }

        // BackHandler for Category selection state
        BackHandler(enabled = isCategorySelected && activeChannelForMenu == null && !showSettingsDialog) {
            isCategorySelected = false
            categoryFocusRequesters[currentCategory]?.safeRequestFocus()
        }

        // BackHandler for App Exit
        BackHandler(enabled = !isCategorySelected && !showExitDialog && activeChannelForMenu == null && !showSettingsDialog && isCategoryListFocused) {
            showExitDialog = true
        }

        // Overlay Dialogs
        if (showSettingsDialog) {
            SettingsDialog(
                viewModel = viewModel,
                onDismiss = { showSettingsDialog = false }
            )
        }

        if (showExitDialog) {
            ExitDialog(
                onDismiss = { showExitDialog = false },
                onExit = { 
                    showExitDialog = false
                    activity?.finish() 
                }
            )
        }

        if (activeChannelForMenu != null) {
            val channel = activeChannelForMenu!!
            val isFav = channel.getResolvedId() in viewModel.favoriteChannelIds
            FavoriteDialog(
                channelName = channel.getResolvedName(),
                isFav = isFav,
                onToggleFavorite = {
                    viewModel.toggleFavorite(channel.getResolvedId())
                    activeChannelForMenu = null
                },
                onDismiss = { activeChannelForMenu = null }
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelCard(
    channel: Channel,
    focusRequester: FocusRequester,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onFocusGained: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    var downTime by remember { mutableStateOf(0L) }
    var isLongClickTriggered by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()

    val scale by animateFloatAsState(if (isFocused) 1.1f else 1f)
    val glowColor = if (isFocused) Color(0xFF3B82F6) else Color.Transparent

    Box(
        modifier = Modifier
            .aspectRatio(16f / 9f)
            .scale(scale)
            .onFocusChanged { 
                isFocused = it.isFocused
                if (it.isFocused) onFocusGained()
            }
            .focusRequester(focusRequester)
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
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
            .background(Color(0xFF1E293B), shape = RoundedCornerShape(12.dp))
            .border(
                width = if (isFocused) 3.dp else 1.dp,
                color = glowColor,
                shape = RoundedCornerShape(12.dp)
            )
            .clip(RoundedCornerShape(12.dp)),
        contentAlignment = Alignment.Center
    ) {
        AsyncImage(
            model = channel.getResolvedLogo(),
            contentDescription = channel.getResolvedName(),
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize().padding(12.dp)
        )
    }
}

// Dialog Composables extracted for cleaner code
@Composable
fun SettingsDialog(
    viewModel: CategoryViewModel,
    onDismiss: () -> Unit
) {
    var hasSettingsDownEvent by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(420.dp)
                .background(Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A))), shape = RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFF334155), shape = RoundedCornerShape(16.dp))
                .padding(32.dp)
                .clickable(enabled = false) {}
                .onPreviewKeyEvent { keyEvent ->
                    val nativeEvent = keyEvent.nativeKeyEvent
                    val action = nativeEvent.action
                    if (nativeEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER || nativeEvent.keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                        if (action == android.view.KeyEvent.ACTION_DOWN) {
                            hasSettingsDownEvent = true
                            false
                        } else if (action == android.view.KeyEvent.ACTION_UP) {
                            !hasSettingsDownEvent
                        } else false
                    } else false
                }
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
            LaunchedEffect(Unit) { toggleFocusRequester.safeRequestFocus() }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isToggleFocused = it.isFocused }
                    .focusRequester(toggleFocusRequester)
                    .clickable { viewModel.setReplayLastChannel(!viewModel.replayLastChannelEnabled) }
                    .background(
                        color = if (isToggleFocused) Color(0xFF3B82F6).copy(alpha = 0.3f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = if (isToggleFocused) Color(0xFF3B82F6) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Auto-Replay Last Channel",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Automatically plays the last watched channel on app startup",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Text(
                    text = if (viewModel.replayLastChannelEnabled) "ON" else "OFF",
                    color = if (viewModel.replayLastChannelEnabled) Color(0xFF34D399) else Color(0xFFF87171),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            var isAutoStartFocused by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isAutoStartFocused = it.isFocused }
                    .clickable { viewModel.setAutoStartOnBoot(!viewModel.autoStartOnBootEnabled) }
                    .background(
                        color = if (isAutoStartFocused) Color(0xFF3B82F6).copy(alpha = 0.3f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = if (isAutoStartFocused) Color(0xFF3B82F6) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Launch on TV Startup",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Automatically open HimanshuTV when your TV turns on",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Text(
                    text = if (viewModel.autoStartOnBootEnabled) "ON" else "OFF",
                    color = if (viewModel.autoStartOnBootEnabled) Color(0xFF34D399) else Color(0xFFF87171),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            var isReloadFocused by remember { mutableStateOf(false) }

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isReloadFocused = it.isFocused }
                    .clickable { 
                        viewModel.reloadChannels()
                        onDismiss()
                    }
                    .background(
                        color = if (isReloadFocused) Color(0xFF3B82F6).copy(alpha = 0.3f) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .border(
                        width = 2.dp,
                        color = if (isReloadFocused) Color(0xFF3B82F6) else Color.Transparent,
                        shape = RoundedCornerShape(12.dp)
                    )
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Reload Channels",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "Force refresh the channel list from the network",
                        color = Color(0xFF94A3B8),
                        fontSize = 14.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
                
                Text(
                    text = "↻",
                    color = Color(0xFF38BDF8),
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            var isCloseFocused by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .align(Alignment.End)
                    .onFocusChanged { isCloseFocused = it.isFocused }
                    .clickable { onDismiss() }
                    .background(
                        color = if (isCloseFocused) Color(0xFFE2E8F0) else Color(0xFF334155),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = "Close",
                    color = if (isCloseFocused) Color(0xFF0F172A) else Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
fun ExitDialog(
    onDismiss: () -> Unit,
    onExit: () -> Unit
) {
    var hasExitDownEvent by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(360.dp)
                .background(Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A))), shape = RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFF334155), shape = RoundedCornerShape(16.dp))
                .padding(32.dp)
                .clickable(enabled = false) {}
                .onPreviewKeyEvent { keyEvent ->
                    val nativeEvent = keyEvent.nativeKeyEvent
                    val action = nativeEvent.action
                    if (nativeEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER || nativeEvent.keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                        if (action == android.view.KeyEvent.ACTION_DOWN) {
                            hasExitDownEvent = true
                            false
                        } else if (action == android.view.KeyEvent.ACTION_UP) {
                            !hasExitDownEvent
                        } else false
                    } else false
                }
        ) {
            Text(
                text = "Exit HimanshuTV",
                color = Color.White,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Text(
                text = "Are you sure you want to close the app?",
                color = Color(0xFF94A3B8),
                fontSize = 16.sp,
                modifier = Modifier.padding(bottom = 32.dp)
            )
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                var isCancelFocused by remember { mutableStateOf(false) }
                val cancelFocusRequester = remember { FocusRequester() }
                
                LaunchedEffect(Unit) { cancelFocusRequester.safeRequestFocus() }
                
                Box(
                    modifier = Modifier
                        .onFocusChanged { isCancelFocused = it.isFocused }
                        .focusRequester(cancelFocusRequester)
                        .clickable { onDismiss() }
                        .background(
                            color = if (isCancelFocused) Color.White else Color(0xFF334155),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 10.dp)
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
                        .clickable { onExit() }
                        .background(
                            color = if (isExitFocused) Color(0xFFEF4444) else Color(0xFF7F1D1D),
                            shape = RoundedCornerShape(8.dp)
                        )
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = "Exit",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun FavoriteDialog(
    channelName: String,
    isFav: Boolean,
    onToggleFavorite: () -> Unit,
    onDismiss: () -> Unit
) {
    var hasFavDownEvent by remember { mutableStateOf(false) }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.85f))
            .clickable { onDismiss() },
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .width(360.dp)
                .background(Brush.linearGradient(listOf(Color(0xFF1E293B), Color(0xFF0F172A))), shape = RoundedCornerShape(16.dp))
                .border(1.dp, Color(0xFF334155), shape = RoundedCornerShape(16.dp))
                .padding(32.dp)
                .clickable(enabled = false) {}
                .onPreviewKeyEvent { keyEvent ->
                    val nativeEvent = keyEvent.nativeKeyEvent
                    val action = nativeEvent.action
                    if (nativeEvent.keyCode == android.view.KeyEvent.KEYCODE_DPAD_CENTER || nativeEvent.keyCode == android.view.KeyEvent.KEYCODE_ENTER) {
                        if (action == android.view.KeyEvent.ACTION_DOWN) {
                            hasFavDownEvent = true
                            false
                        } else if (action == android.view.KeyEvent.ACTION_UP) {
                            !hasFavDownEvent
                        } else false
                    } else false
                }
        ) {
            Text(
                text = channelName,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 24.dp)
            )

            var isClickable by remember { mutableStateOf(false) }
            var isFavOptionFocused by remember { mutableStateOf(false) }
            val favOptionFocusRequester = remember { FocusRequester() }
            
            LaunchedEffect(Unit) {
                favOptionFocusRequester.safeRequestFocus()
                delay(300)
                isClickable = true
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .onFocusChanged { isFavOptionFocused = it.isFocused }
                    .focusRequester(favOptionFocusRequester)
                    .clickable { if (isClickable) onToggleFavorite() }
                    .background(
                        color = if (isFavOptionFocused) Color(0xFF3B82F6) else Color(0xFF334155),
                        shape = RoundedCornerShape(8.dp)
                    )
                    .padding(16.dp)
            ) {
                Text(
                    text = if (isFav) "Remove from Favorites" else "Add to Favorites",
                    color = Color.White,
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
                    .clickable { if (isClickable) onDismiss() }
                    .background(
                        color = if (isCancelOptionFocused) Color.White else Color(0xFF1E293B),
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
