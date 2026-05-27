package com.jiotvplus.apk.data.model

import com.google.gson.annotations.SerializedName

data class MetadataResponse(
    @SerializedName("data") val data: List<Channel>?
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
    @SerializedName("genres") val genres: List<String>?,
    @SerializedName("language") val language: String?
) {
    fun getResolvedId(): String {
        return channelId ?: contentId ?: id ?: ""
    }

    fun getResolvedName(): String {
        return name ?: title ?: "Unknown Channel"
    }

    fun getResolvedLogo(): String {
        val candidates = listOf(channelLogo, logoUrl, image, thumbnail, still)
        for (candidate in candidates) {
            if (!candidate.isNullOrEmpty()) {
                if (candidate.startsWith("http")) {
                    return candidate
                }
                return "https://jiotv.catchup.cdn.jio.com/dare_images/images/$candidate"
            }
        }
        return ""
    }

    fun getResolvedCategory(): String {
        if (!genres.isNullOrEmpty()) return genres[0]
        if (language != null) return language
        return "JioTV+"
    }
}
