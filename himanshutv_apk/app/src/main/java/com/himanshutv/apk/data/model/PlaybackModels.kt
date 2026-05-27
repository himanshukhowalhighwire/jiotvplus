package com.himanshutv.apk.data.model

import com.google.gson.annotations.SerializedName

data class PlaybackRightsRequest(
    @SerializedName("bitrateProfile") val bitrateProfile: String = "xxhdpi",
    @SerializedName("model") val model: String = "JioSTB",
    @SerializedName("manufacturer") val manufacturer: String = "Jio",
    @SerializedName("osVersion") val osVersion: String = "9",
    @SerializedName("serialNo") val serialNo: String,
    @SerializedName("is4kSupport") val is4kSupport: Boolean = false,
    @SerializedName("hevcSupport") val hevcSupport: Boolean = false,
    @SerializedName("dolbySupport") val dolbySupport: Boolean = false,
    @SerializedName("appVersion") val appVersion: String = "2.6.1_2072"
)

data class PlaybackRightsResponse(
    @SerializedName("data") val data: PlaybackData?
)

data class PlaybackData(
    @SerializedName("keyURL") val keyURL: String?,
    @SerializedName("playbackToken") val playbackToken: String?,
    @SerializedName("mpd") val mpd: PlaybackStreamMap?,
    @SerializedName("m3u8") val m3u8: PlaybackStreamMap?
)

data class PlaybackStreamMap(
    @SerializedName("auto") val auto: String?
)

data class PlaybackInfo(
    val streamUrl: String?,
    val keyUrl: String?,
    val isM3u8: Boolean,
    val playbackToken: String?
)
