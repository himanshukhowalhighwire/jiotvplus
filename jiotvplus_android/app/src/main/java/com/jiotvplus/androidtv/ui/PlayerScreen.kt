package com.jiotvplus.androidtv.ui

import android.net.Uri
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.annotation.OptIn
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
import androidx.media3.exoplayer.dash.DashMediaSource
import androidx.media3.exoplayer.drm.DefaultDrmSessionManagerProvider
import androidx.media3.ui.PlayerView
import com.jiotvplus.androidtv.data.AppConfig
import com.jiotvplus.androidtv.data.local.SettingsDataStore
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
    private val dataStore: SettingsDataStore,
    private val okHttpClient: OkHttpClient
) : ViewModel() {
    
    var keyUrl by mutableStateOf<String?>(null)
    var error by mutableStateOf<String?>(null)

    fun fetchRights(contentId: String) {
        viewModelScope.launch {
            dataStore.saveLastChannelId(contentId)
            val url = playbackRepository.getPlaybackRights(contentId)
            if (url != null) {
                keyUrl = url
            } else {
                error = "Failed to fetch playback rights"
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
            "x-accesstoken" to accessToken,
            "uniqueid" to uniqueId,
            "subId" to subId
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

@OptIn(UnstableApi::class)
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

    LaunchedEffect(viewModel.keyUrl) {
        if (viewModel.keyUrl != null) {
            val drmCallback = JioMediaDrmCallback(
                keyUrl = viewModel.keyUrl!!,
                dataStore = viewModel.getSettingsDataStore(),
                okHttpClient = viewModel.getOkHttpClient()
            )

            val drmSessionManagerProvider = DefaultDrmSessionManagerProvider()
            drmSessionManagerProvider.setDrmHttpDataSourceFactory(DefaultHttpDataSource.Factory())

            val dataSourceFactory = DefaultHttpDataSource.Factory()
                .setUserAgent(AppConfig.USER_AGENT)
                .setDefaultRequestProperties(headers)

            val dashMediaSource = DashMediaSource.Factory(dataSourceFactory)
                .createMediaSource(
                    MediaItem.Builder()
                        .setUri(Uri.parse("https://jiotv.catchup.cdn.jio.com/bpk-tv/${channelId}_Fallback/${channelId}_Fallback.mpd"))
                        .setDrmConfiguration(
                            MediaItem.DrmConfiguration.Builder(C.WIDEVINE_UUID)
                                .setLicenseUri(viewModel.keyUrl)
                                .build()
                        )
                        .build()
                )

            // Wait, we need to inject our custom MediaDrmCallback. ExoPlayer's DefaultDrmSessionManager doesn't let us inject a callback directly through MediaItem if we use DefaultDrmSessionManagerProvider.
            // Actually, we can use ExoPlayer.Builder and set a custom DrmSessionManager, but it's easier to create DefaultDrmSessionManager.
            
            val drmSessionManager = androidx.media3.exoplayer.drm.DefaultDrmSessionManager.Builder()
                .setUuidAndExoMediaDrmProvider(C.WIDEVINE_UUID, androidx.media3.exoplayer.drm.FrameworkMediaDrm.DEFAULT_PROVIDER)
                .build(drmCallback)

            val player = ExoPlayer.Builder(context).build().apply {
                val mediaSource = DashMediaSource.Factory(dataSourceFactory)
                    .setDrmSessionManagerProvider { drmSessionManager }
                    .createMediaSource(
                        MediaItem.Builder()
                            .setUri(Uri.parse("https://jiotv.catchup.cdn.jio.com/bpk-tv/${channelId}_Fallback/${channelId}_Fallback.mpd")) // Standard JioTV+ MPD URL format, we should probably pass the URL or resolve it. 
                            // Wait, the PHP proxy renders MPD by replacing BaseURL. If we use the raw MPD without proxy, we just need the correct URL.
                            // In PHP's channels.php, the MPD is fetched from live.php.
                            // Let's use the standard live URL for JioTV+ channels.
                            .build()
                    )
                setMediaSource(mediaSource)
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
        androidx.tv.material3.Text(viewModel.error!!, color = Color.Red, modifier = Modifier.background(Color.Black).fillMaxSize())
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
