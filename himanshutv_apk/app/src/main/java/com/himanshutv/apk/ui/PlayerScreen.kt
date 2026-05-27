package com.himanshutv.apk.ui

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
import com.himanshutv.apk.data.local.SettingsDataStore
import com.himanshutv.apk.data.model.PlaybackInfo
import com.himanshutv.apk.data.repository.PlaybackRepository
import com.himanshutv.apk.player.HimanshuMediaDrmCallback
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.firstOrNull
import okhttp3.OkHttpClient
import java.util.UUID
import java.util.Calendar
import java.util.Locale
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

    private fun buildCookieFromUrl(url: String): String {
        if (!url.contains("?")) return ""
        val query = url.substringAfter("?")
        val sb = java.lang.StringBuilder()
        for (param in query.split("&")) {
            val eqIdx = param.indexOf('=')
            if (eqIdx > 0) {
                val k = param.substring(0, eqIdx)
                val v = param.substring(eqIdx + 1)
                if (sb.isNotEmpty()) sb.append("; ")
                sb.append("$k=$v")
                if (k == "hdnea" || k == "__hdnea__") {
                    sb.append("; __hdnea__=$v; hdnea=$v")
                }
            }
        }
        return sb.toString()
    }

    fun getExoPlayer(context: Context, contentId: String): ExoPlayer {
        val player = ExoPlayer.Builder(context).build()
        val info = playbackInfo ?: return player
        val streamUrl = info.streamUrl ?: return player

        var accessToken = ""
        var ssoToken = ""
        var uniqueId = ""
        var subscriberId = ""
        var encodedRmn = ""
        var jToken: String? = null
        var lbCookie: String? = null

        runBlocking {
            accessToken = dataStore.accessToken.firstOrNull() ?: dataStore.ssoToken.firstOrNull() ?: ""
            ssoToken = dataStore.ssoToken.firstOrNull() ?: ""
            uniqueId = dataStore.uniqueId.firstOrNull() ?: ""
            subscriberId = dataStore.subscriberId.firstOrNull() ?: ""
            val mobileNumber = dataStore.mobileNumber.firstOrNull() ?: ""
            val cleaned = mobileNumber.replace(Regex("[^0-9]"), "")
            encodedRmn = android.util.Base64.encodeToString(cleaned.toByteArray(), android.util.Base64.NO_WRAP)
            jToken = dataStore.jToken.firstOrNull()
            lbCookie = dataStore.lbCookie.firstOrNull()
        }

        val cal = Calendar.getInstance()
        val srno = String.format(
            Locale.US,
            "%02d%02d%02d%02d%02d%03d",
            cal.get(Calendar.YEAR) % 100,
            cal.get(Calendar.MONTH) + 1,
            cal.get(Calendar.DAY_OF_MONTH),
            cal.get(Calendar.HOUR_OF_DAY),
            cal.get(Calendar.MINUTE),
            cal.get(Calendar.MILLISECOND)
        )

        val headers = mutableMapOf(
            "User-Agent" to "okhttp/4.2.2",
            "appkey" to "NzNiMDhlYzQyNjJm",
            "appName" to "RJIL_JioTV",
            "devicetype" to "phone",
            "os" to "android",
            "osVersion" to "13",
            "isott" to "false",
            "lbcookie" to "1",
            "usergroup" to "tvYR7NSNn7rymo3F",
            "versionCode" to "389",
            "ssotoken" to ssoToken,
            "accesstoken" to accessToken,
            "uniqueId" to uniqueId,
            "deviceId" to uniqueId,
            "crmid" to subscriberId,
            "userId" to subscriberId,
            "subscriberId" to subscriberId,
            "channel_id" to contentId,
            "channelid" to contentId,
            "srno" to srno,
            "x-platform" to "android",
            "x-jio-app" to "RJIL_JioTV",
            "x-jio-os" to "android"
        )

        val cookieHeader = buildCookieFromUrl(streamUrl)
        if (cookieHeader.isNotEmpty()) {
            headers["Cookie"] = cookieHeader
        }
        val jTokenVal = jToken
        if (jTokenVal != null) {
            headers["jToken"] = jTokenVal
            val currentCookie = headers["Cookie"] ?: ""
            headers["Cookie"] = if (currentCookie.isNotEmpty()) "$currentCookie; jToken=$jTokenVal" else "jToken=$jTokenVal"
        }
        val lbCookieVal = lbCookie
        if (lbCookieVal != null) {
            headers["lbCookie"] = lbCookieVal
            val currentCookie = headers["Cookie"] ?: ""
            headers["Cookie"] = if (currentCookie.isNotEmpty()) "$currentCookie; lbCookie=$lbCookieVal" else "lbCookie=$lbCookieVal"
        }

        val dataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(headers)

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
            val hlsFactory = HlsMediaSource.Factory(dataSourceFactory)
            if (info.keyUrl != null) {
                val drmCallback = HimanshuMediaDrmCallback(
                    keyUrl = info.keyUrl,
                    channelId = contentId,
                    playbackToken = info.playbackToken,
                    dataStore = dataStore,
                    okHttpClient = okHttpClient
                )
                val drmSessionManager = DefaultDrmSessionManager.Builder()
                    .setUuidAndExoMediaDrmProvider(androidx.media3.common.C.WIDEVINE_UUID, androidx.media3.exoplayer.drm.FrameworkMediaDrm.DEFAULT_PROVIDER)
                    .build(drmCallback)
                hlsFactory.setDrmSessionManagerProvider { drmSessionManager }
            }
            hlsFactory.createMediaSource(mediaItem)
        } else {
            val dashFactory = DashMediaSource.Factory(dataSourceFactory)
            if (info.keyUrl != null) {
                val drmCallback = HimanshuMediaDrmCallback(
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
