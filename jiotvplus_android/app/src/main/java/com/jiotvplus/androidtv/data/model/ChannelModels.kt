package com.jiotvplus.androidtv.data.model

import com.google.gson.annotations.SerializedName

data class ChannelResponse(
    @SerializedName("data") val data: List<Channel>? = null
)

data class Channel(
    @SerializedName("channelId") val channelId: String?,
    @SerializedName("contentId") val contentId: String?,
    @SerializedName("id") val id: String?,
    @SerializedName("name") val name: String?,
    @SerializedName("title") val title: String?,
    @SerializedName("channelLogo") val channelLogo: String?,
    @SerializedName("logoUrl") val logoUrl: String?,
    @SerializedName("image") val image: String?,
    @SerializedName("thumbnail") val thumbnail: String?,
    @SerializedName("still") val still: String?,
    @SerializedName("channelNumber") val channelNumber: String?,
    @SerializedName("genres") val genres: List<String>?,
    @SerializedName("language") val language: String?
) {
    fun getResolvedId(): String = channelId ?: contentId ?: id ?: ""
    fun getResolvedName(): String = name ?: title ?: "Unknown Channel"
    fun getResolvedLogo(): String {
        val logo = channelLogo ?: logoUrl ?: image ?: thumbnail ?: still ?: ""
        return if (logo.isNotEmpty() && !logo.startsWith("http")) {
            "https://jiotv.catchup.cdn.jio.com/dare_images/images/$logo"
        } else {
            logo
        }
    }
    fun getResolvedCategory(): String {
        return if (!genres.isNullOrEmpty()) {
            genres[0]
        } else {
            language ?: "JioTV+"
        }
    }
}
