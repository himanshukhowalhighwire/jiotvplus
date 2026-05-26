package com.jiotvplus.androidtv.ui

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.drm.DefaultDrmSessionManager
import androidx.media3.exoplayer.drm.FrameworkMediaDrm
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.ui.PlayerView
import com.jiotvplus.androidtv.data.AppConfig
import com.jiotvplus.androidtv.data.local.SettingsDataStore
import com.jiotvplus.androidtv.data.repository.ChannelRepository
import com.jiotvplus.androidtv.data.repository.PlaybackRepository
import com.jiotvplus.androidtv.player.JioMediaDrmCallback
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import javax.inject.Inject

@HiltViewModel
class PlayerViewModel @Inject constructor(
    private val playbackRepository: PlaybackRepository,
    private val channelRepository: ChannelRepository,
    private val dataStore: SettingsDataStore,
    private val okHttpClient: OkHttpClient
) : ViewModel() {
    
    var keyUrl by mutableStateOf<String?>(null)
    var streamUrl by mutableStateOf<String?>(null)
    var error by mutableStateOf<String?>(null)

    fun fetchRights(contentId: String) {
        viewModelScope.launch {
            dataStore.saveLastChannelId(contentId)
            
            val channel = channelRepository.getChannelById(contentId)
            if (channel?.streamUrl != null) {
                // Direct stream URL available (e.g. Sony Liv channels)
                streamUrl = channel.streamUrl
                keyUrl = null // No DRM for direct HLS
            } else {
                // Fetch Jio Playback Rights
                val info = playbackRepository.getPlaybackRights(contentId)
                if (info != null && info.streamUrl != null) {
                    streamUrl = info.streamUrl
                    keyUrl = info.keyUrl
                } else {
                    error = "Failed to fetch playback rights"
                }
            }
        }
    }

    suspend fun getHeaders(): Map<String, String> {
        val uniqueId = dataStore.uniqueId.firstOrNull() ?: ""
        val subId = dataStore.subscriberId.firstOrNull() ?: ""
        val accessToken = dataStore.accessToken.firstOrNull() ?: dataStore.ssoToken.firstOrNull() ?: ""
        val jToken = dataStore.jToken.firstOrNull() ?: ""
        val lbCookie = dataStore.lbCookie.firstOrNull() ?: ""

        val headers = mutableMapOf(
            "x-apisignatures" to AppConfig.X_APISIGNATURE,
            "x-feature-code" to AppConfig.X_FEATURE_CODE,
            "x-appname" to "JioTVPlus",
            "x-page" to "Player",
            "x-analytic-restriction" to "0",
            "x-platform" to AppConfig.X_PLATFORM,
            "x-accesstoken" to accessToken,
            "uniqueid" to uniqueId,
            "subId" to subId,
            "User-Agent" to AppConfig.USER_AGENT
        )
        if (jToken.isNotEmpty()) headers["jToken"] = jToken
        if (lbCookie.isNotEmpty()) headers["lbCookie"] = lbCookie
        if (jToken.isNotEmpty() && lbCookie.isNotEmpty()) {
            headers["Cookie"] = "jToken=$jToken; lbCookie=$lbCookie"
        }
        return headers
    }
    
    fun getSettingsDataStore() = dataStore
    fun getOkHttpClient() = okHttpClient
}

@OptIn(UnstableApi::class, androidx.tv.material3.ExperimentalTvMaterial3Api::class)
@Composable
fun PlayerScreen(
    channelId: String,
    viewModel: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    var exoPlayer by remember { mutableStateOf<ExoPlayer?>(null) }
    var headers by remember { mutableStateOf<Map<String, String>>(emptyMap()) }

    LaunchedEffect(channelId) {
        headers = viewModel.getHeaders()
        viewModel.fetchRights(channelId)
    }

    LaunchedEffect(viewModel.streamUrl) {
        if (viewModel.streamUrl != null) {
            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(AppConfig.USER_AGENT)
                .setDefaultRequestProperties(headers)
                
            val mediaItemBuilder = MediaItem.Builder()
                .setUri(Uri.parse(viewModel.streamUrl))
                
            var drmSessionManager: DefaultDrmSessionManager? = null

            if (viewModel.keyUrl != null) {
                val drmCallback = JioMediaDrmCallback(
                    keyUrl = viewModel.keyUrl!!,
                    channelId = channelId,
                    dataStore = viewModel.getSettingsDataStore(),
                    okHttpClient = viewModel.getOkHttpClient()
                )

                drmSessionManager = DefaultDrmSessionManager.Builder()
                    .setUuidAndExoMediaDrmProvider(C.WIDEVINE_UUID, FrameworkMediaDrm.DEFAULT_PROVIDER)
                    .build(drmCallback)
                    
                mediaItemBuilder.setDrmConfiguration(
                    MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                        .setLicenseUri(viewModel.keyUrl)
                        .build()
                )
            }

            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            if (drmSessionManager != null) {
                mediaSourceFactory.setDrmSessionManagerProvider { drmSessionManager }
            }

            val player = ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .build()
                .apply {
                    setMediaItem(mediaItemBuilder.build())
                    prepare()
                    playWhenReady = true
                }
                
            exoPlayer = player
        }
    }

    DisposableEffect(Unit) {
        onDispose {
            exoPlayer?.release()
        }
    }

    if (viewModel.error != null) {
        androidx.tv.material3.Text(
            text = viewModel.error!!, 
            color = Color.Red, 
            modifier = Modifier.background(Color.Black).fillMaxSize()
        )
    } else {
        AndroidView(
            factory = { ctx ->
                PlayerView(ctx).apply {
                    player = exoPlayer
                    layoutParams = FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
                    useController = true
                }
            },
            modifier = Modifier.fillMaxSize().background(Color.Black),
            update = { view ->
                view.player = exoPlayer
            }
        )
    }
}
