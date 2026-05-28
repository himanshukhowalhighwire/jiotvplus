package com.himanshutv.apk.ui

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
import com.himanshutv.apk.data.AppConfig
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
@UnstableApi
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
            dataStore.saveLastPlayedChannel(contentId)
            val info = playbackRepository.getPlaybackRights(contentId)
            if (info != null && info.streamUrl != null) {
                playbackInfo = info
            } else {
                error = "Failed to fetch playback rights for this channel."
            }
            isLoading = false
        }
    }

    fun savePreferredLanguage(contentId: String, langCode: String) {
        viewModelScope.launch {
            dataStore.saveChannelLanguage(contentId, langCode)
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
        var preferredLanguage: String? = null

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
            preferredLanguage = dataStore.getChannelLanguage(contentId)
        }

        if (!preferredLanguage.isNullOrEmpty()) {
            player.trackSelectionParameters = player.trackSelectionParameters
                .buildUpon()
                .setPreferredAudioLanguage(preferredLanguage)
                .build()
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
            "User-Agent" to AppConfig.USER_AGENT,
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

        android.util.Log.d("PLAYER_SCREEN", "Stream URL: $streamUrl")
        android.util.Log.d("PLAYER_SCREEN", "Stream Headers: $headers")

        val baseDataSourceFactory = DefaultHttpDataSource.Factory()
            .setAllowCrossProtocolRedirects(true)
            .setDefaultRequestProperties(headers)

        val resolver = com.himanshutv.apk.player.HimanshuStreamResolver(
            contentId = contentId,
            playbackRepository = playbackRepository,
            dataStore = dataStore,
            initialPlaybackInfo = info
        )
        val dataSourceFactory = androidx.media3.datasource.ResolvingDataSource.Factory(baseDataSourceFactory, resolver)

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
        player.addListener(object : androidx.media3.common.Player.Listener {
            override fun onPlayerError(playbackError: androidx.media3.common.PlaybackException) {
                var rootCause: Throwable? = playbackError
                while (rootCause?.cause != null) {
                    rootCause = rootCause.cause
                }
                val causeMessage = rootCause?.message ?: playbackError.message
                error = "Playback Error (${playbackError.errorCodeName}): $causeMessage"
            }
        })
        player.playWhenReady = true
        return player
    }
}

data class AudioTrackInfo(
    val language: String?,
    val displayName: String,
    val group: androidx.media3.common.Tracks.Group,
    val trackIndex: Int
)

@androidx.annotation.OptIn(UnstableApi::class)
private fun getAvailableAudioTracks(player: ExoPlayer): List<AudioTrackInfo> {
    val tracks = player.currentTracks
    val audioTracks = mutableListOf<AudioTrackInfo>()
    val seenLanguages = mutableSetOf<String>()
    
    for (group in tracks.groups) {
        if (group.type == androidx.media3.common.C.TRACK_TYPE_AUDIO) {
            for (i in 0 until group.length) {
                val format = group.getTrackFormat(i)
                val language = format.language ?: "und"
                if (seenLanguages.add(language)) {
                    val locale = if (language == "und") java.util.Locale.getDefault() else java.util.Locale.forLanguageTag(language)
                    val displayName = if (language == "und") "Default" else locale.getDisplayName(java.util.Locale.US).replaceFirstChar { it.uppercase() }
                    audioTracks.add(AudioTrackInfo(language, displayName, group, i))
                }
            }
        }
    }
    return audioTracks
}

@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun PlayerScreen(
    contentId: String,
    viewModel: PlayerViewModel = viewModel()
) {
    val context = LocalContext.current
    var player by remember { mutableStateOf<ExoPlayer?>(null) }
    var showAudioDialog by remember { mutableStateOf(false) }
    var isControllerVisible by remember { mutableStateOf(false) }

    LaunchedEffect(contentId) {
        viewModel.loadStream(contentId)
    }

    LaunchedEffect(viewModel.playbackInfo) {
        if (viewModel.playbackInfo != null) {
            player?.release()
            player = viewModel.getExoPlayer(context, contentId)
        }
    }

    val latestPlayer = rememberUpdatedState(player)
    DisposableEffect(Unit) {
        onDispose {
            val playerToRelease = latestPlayer.value
            if (playerToRelease != null) {
                android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                    playerToRelease.release()
                }, 1000)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize().background(Color.Black), contentAlignment = Alignment.Center) {
        if (viewModel.isLoading) {
            Text("Loading Stream...", color = Color.White, fontSize = 24.sp)
        } else if (viewModel.error != null) {
            Text(viewModel.error!!, color = Color.Red, fontSize = 24.sp)
        } else {
            Box(modifier = Modifier.fillMaxSize()) {
                AndroidView(
                    factory = { ctx ->
                        PlayerView(ctx).apply {
                            this.player = player
                            useController = true
                            keepScreenOn = true
                            isFocusable = true
                            isFocusableInTouchMode = true
                            setControllerVisibilityListener(PlayerView.ControllerVisibilityListener { visibility ->
                                isControllerVisible = (visibility == android.view.View.VISIBLE)
                            })
                            requestFocus()
                        }
                    },
                    update = { view ->
                        if (view.player != player) {
                            view.player = player
                        }
                        view.requestFocus()
                    },
                    modifier = Modifier.fillMaxSize()
                )

                // Floating Audio button (Top Right) - Only visible when player controls are visible
                if (isControllerVisible) {
                    var isFocused by remember { mutableStateOf(false) }
                    val focusRequester = remember { FocusRequester() }
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(24.dp)
                            .onFocusChanged { isFocused = it.isFocused }
                            .focusRequester(focusRequester)
                            .clickable { showAudioDialog = true }
                            .background(
                                color = if (isFocused) Color.White else Color.Black.copy(alpha = 0.6f),
                                shape = RoundedCornerShape(8.dp)
                            )
                            .border(
                                width = 2.dp,
                                color = if (isFocused) Color.Yellow else Color.Gray,
                                shape = RoundedCornerShape(8.dp)
                            )
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "Audio",
                            color = if (isFocused) Color.Black else Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (showAudioDialog) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.8f))
                            .clickable { showAudioDialog = false },
                        contentAlignment = Alignment.Center
                    ) {
                        val availableTracks = player?.let { getAvailableAudioTracks(it) } ?: emptyList()
                        Column(
                            modifier = Modifier
                                .width(320.dp)
                                .background(Color(0xFF1E1E1E), shape = RoundedCornerShape(12.dp))
                                .border(2.dp, Color.Yellow, shape = RoundedCornerShape(12.dp))
                                .padding(16.dp)
                                .clickable(enabled = false) {}
                        ) {
                            Text(
                                text = "Select Audio Language",
                                color = Color.White,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(bottom = 16.dp)
                            )
                            
                            if (availableTracks.isEmpty()) {
                                Text(
                                    text = "No alternative audio tracks available",
                                    color = Color.Gray,
                                    fontSize = 16.sp,
                                    modifier = Modifier.padding(bottom = 16.dp)
                                )
                            } else {
                                val firstItemFocusRequester = remember { FocusRequester() }
                                LaunchedEffect(showAudioDialog) {
                                    firstItemFocusRequester.requestFocus()
                                }
                                
                                availableTracks.forEachIndexed { index, track ->
                                    var isItemFocused by remember { mutableStateOf(false) }
                                    val itemModifier = if (index == 0) {
                                        Modifier.focusRequester(firstItemFocusRequester)
                                    } else {
                                        Modifier
                                    }
                                    Box(
                                        modifier = itemModifier
                                            .fillMaxWidth()
                                            .onFocusChanged { isItemFocused = it.isFocused }
                                            .clickable {
                                                player?.let { p ->
                                                    val override = androidx.media3.common.TrackSelectionOverride(
                                                        track.group.mediaTrackGroup,
                                                        track.trackIndex
                                                    )
                                                    p.trackSelectionParameters = p.trackSelectionParameters
                                                        .buildUpon()
                                                        .setOverrideForType(override)
                                                        .setPreferredAudioLanguage(track.language)
                                                        .build()
                                                    viewModel.savePreferredLanguage(contentId, track.language ?: "und")
                                                }
                                                showAudioDialog = false
                                            }
                                            .background(
                                                color = if (isItemFocused) Color.Yellow.copy(alpha = 0.2f) else Color.Transparent,
                                                shape = RoundedCornerShape(6.dp)
                                            )
                                            .padding(vertical = 12.dp, horizontal = 12.dp)
                                    ) {
                                        Text(
                                            text = track.displayName,
                                            color = if (isItemFocused) Color.Yellow else Color.White,
                                            fontSize = 18.sp
                                        )
                                    }
                                }
                            }
                            
                            var isCloseFocused by remember { mutableStateOf(false) }
                            Box(
                                modifier = Modifier
                                    .align(Alignment.End)
                                    .padding(top = 16.dp)
                                    .onFocusChanged { isCloseFocused = it.isFocused }
                                    .clickable { showAudioDialog = false }
                                    .background(
                                        color = if (isCloseFocused) Color.Yellow else Color.Gray.copy(alpha = 0.3f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                            ) {
                                Text(
                                    text = "Cancel",
                                    color = if (isCloseFocused) Color.Black else Color.White,
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
