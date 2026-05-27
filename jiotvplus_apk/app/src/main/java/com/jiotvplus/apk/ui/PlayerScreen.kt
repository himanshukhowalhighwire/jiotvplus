package com.jiotvplus.apk.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.MimeTypes
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView
import androidx.compose.material3.Text
import com.jiotvplus.apk.data.local.SettingsDataStore
import com.jiotvplus.apk.data.model.PlaybackInfo
import com.jiotvplus.apk.data.repository.PlaybackRepository
import com.jiotvplus.apk.player.JioMediaDrmCallback
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackRepository: PlaybackRepository,
    private val dataStore: SettingsDataStore,
    private val okHttpClient: OkHttpClient
) : ViewModel() {
    var playbackInfo by mutableStateOf<PlaybackInfo?>(null)
    var error by mutableStateOf<String?>(null)
    var isLoading by mutableStateOf(true)

    fun loadStream(contentId: String) {
        viewModelScope.launch {
            isLoading = true
            error = null
            val info = playbackRepository.getPlaybackRights(contentId)
            if (info != null && info.streamUrl != null) {
                playbackInfo = info
            } else {
                error = "Failed to fetch playback rights for this channel."
            }
            isLoading = false
        }
    }

    fun getExoPlayer(context: Context, contentId: String): ExoPlayer {
        val player = ExoPlayer.Builder(context).build()
        val info = playbackInfo ?: return player
        val streamUrl = info.streamUrl ?: return player
        
        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setUserAgent("JioTV.Plus/2.6.1 (Linux; Android 9; JioSTB Build/PI; wv) ExoPlayerLib/2.19.1")
            .setDefaultRequestProperties(mapOf("x-platform" to "smartandroidtv"))

        val mediaItemBuilder = MediaItem.Builder()
            .setUri(streamUrl)
            .setMimeType(if (info.isM3u8) MimeTypes.APPLICATION_M3U8 else MimeTypes.APPLICATION_MPD)

        if (info.keyUrl != null) {
            val drmConfig = MediaItem.DrmConfiguration.Builder(androidx.media3.common.C.WIDEVINE_UUID)
                .setLicenseUri(info.keyUrl)
                .build()
            mediaItemBuilder.setDrmConfiguration(drmConfig)
        }

        val mediaItem = mediaItemBuilder.build()
        val mediaSource = if (info.isM3u8) {
            HlsMediaSource.Factory(dataSourceFactory).createMediaSource(mediaItem)
        } else {
            val dashFactory = DashMediaSource.Factory(dataSourceFactory)
            if (info.keyUrl != null) {
                val drmCallback = JioMediaDrmCallback(
                    keyUrl = info.keyUrl,
                    channelId = contentId,
                    playbackToken = info.playbackToken,
                    dataStore = dataStore,
                    okHttpClient = okHttpClient
                )
                val drmSessionManager = DefaultDrmSessionManager.Builder()
                    .setUuidAndExoMediaDrmProvider(androidx.media3.common.C.WIDEVINE_UUID, androidx.media3.exoplayer.drm.FrameworkMediaDrm.DEFAULT_PROVIDER)
                    .build(drmCallback)
                dashFactory.setDrmSessionManagerProvider { drmSessionManager }
            }
            dashFactory.createMediaSource(mediaItem)
        }

        player.setMediaSource(mediaSource)
        player.prepare()
        player.playWhenReady = true
        return player
    }
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    contentId: String,
    viewModel: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    var player by remember { mutableStateOf<ExoPlayer?>(null) }

    LaunchedEffect(contentId) {
        viewModel.loadStream(contentId)
    }

    LaunchedEffect(viewModel.playbackInfo) {
        if (viewModel.playbackInfo != null) {
            player?.release()
            player = viewModel.getExoPlayer(context, contentId)
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            player?.release()
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        if (viewModel.isLoading) {
            Text("Loading Stream...", color = Color.White, fontSize = 24.sp)
        } else if (viewModel.error != null) {
            Text(viewModel.error!!, color = Color.Red, fontSize = 24.sp)
        } else {
            AndroidView(
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        this.player = player
                        useController = true
                        keepScreenOn = true
                    }
                },
                update = { view ->
                    if (view.player != player) {
                        view.player = player
                    }
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}
