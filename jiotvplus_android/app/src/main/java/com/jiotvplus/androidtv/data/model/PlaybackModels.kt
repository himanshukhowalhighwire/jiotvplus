package com.jiotvplus.androidtv.data.model

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
    @SerializedName("playbackToken") val playbackToken: String?
)
