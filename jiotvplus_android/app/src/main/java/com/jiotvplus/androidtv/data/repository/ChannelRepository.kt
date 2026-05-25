package com.jiotvplus.androidtv.data.repository

import com.jiotvplus.androidtv.data.local.SettingsDataStore
import com.jiotvplus.androidtv.data.model.Channel
import com.jiotvplus.androidtv.data.remote.MetadataApi
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepository @Inject constructor(
    private val metadataApi: MetadataApi,
    private val dataStore: SettingsDataStore
) {
    private var cachedChannels: List<Channel>? = null

    suspend fun getChannels(forceRefresh: Boolean = false): List<Channel> {
        if (!forceRefresh && cachedChannels != null) return cachedChannels!!
        return try {
            val uniqueId = dataStore.uniqueId.firstOrNull() ?: return emptyList()
            val subId = dataStore.subscriberId.firstOrNull() ?: return emptyList()
            val accessToken = dataStore.accessToken.firstOrNull() ?: dataStore.ssoToken.firstOrNull() ?: return emptyList()

            val response = metadataApi.getLiveChannels(uniqueId, subId, accessToken)
            if (response.isSuccessful) {
                val dataElement = response.body()?.data
                if (dataElement == null || dataElement.isJsonNull) {
                    return emptyList()
                }
                val gson = com.google.gson.Gson()
                val listType = object : com.google.gson.reflect.TypeToken<List<Channel>>() {}.type
                
                val list: List<Channel> = if (dataElement.isJsonArray) {
                    gson.fromJson(dataElement, listType) ?: emptyList()
                } else if (dataElement.isJsonObject) {
                    val mapType = object : com.google.gson.reflect.TypeToken<Map<String, Channel>>() {}.type
                    val map: Map<String, Channel>? = gson.fromJson(dataElement, mapType)
                    map?.values?.toList() ?: emptyList()
                } else {
                    emptyList()
                }
                
                val finalChannels = list.toMutableList()
                finalChannels.addAll(fetchSonyLivChannels())
                
                cachedChannels = finalChannels
                finalChannels
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private suspend fun fetchSonyLivChannels(): List<Channel> = kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
        val list = mutableListOf<Channel>()
        try {
            val m3u = java.net.URL("https://raw.githubusercontent.com/tg-aadi/content/refs/heads/main/sonyliv.m3u").readText()
            val lines = m3u.lines()
            var currentId = ""
            var currentLogo = ""
            var currentName = ""
            val currentGroup = "SonyLIV"
            
            for (line in lines) {
                val trimmed = line.trim()
                if (trimmed.isEmpty() || trimmed == "#EXTM3U") continue
                if (trimmed.startsWith("#EXTINF")) {
                    val idMatch = Regex("""tvg-id="([^"]*)"""").find(trimmed)
                    val logoMatch = Regex("""tvg-logo="([^"]*)"""").find(trimmed)
                    
                    currentId = idMatch?.groupValues?.get(1) ?: java.util.UUID.randomUUID().toString()
                    currentLogo = logoMatch?.groupValues?.get(1) ?: ""
                    
                    val parts = trimmed.split(",")
                    if (parts.size > 1) {
                        currentName = parts.last().trim()
                    }
                } else if (!trimmed.startsWith("#")) {
                    if (currentName.isNotEmpty()) {
                        list.add(Channel(
                            channelId = currentId,
                            contentId = null,
                            id = currentId,
                            name = currentName,
                            title = currentName,
                            channelLogo = currentLogo,
                            logoUrl = null,
                            image = null,
                            thumbnail = null,
                            still = null,
                            channelNumber = null,
                            genres = listOf(currentGroup),
                            language = null,
                            streamUrl = trimmed
                        ))
                    }
                    currentId = ""
                    currentLogo = ""
                    currentName = ""
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        list
    }

    fun getChannelById(id: String): Channel? {
        return cachedChannels?.find { it.getResolvedId() == id }
    }
}
