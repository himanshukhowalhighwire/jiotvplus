package com.jiotvplus.apk.data.repository

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.jiotvplus.apk.data.local.SettingsDataStore
import com.jiotvplus.apk.data.model.Channel
import com.jiotvplus.apk.data.remote.MetadataApi
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepository @Inject constructor(
    private val metadataApi: MetadataApi,
    private val dataStore: SettingsDataStore
) {
    private val gson = Gson()
    private var cachedChannels: List<Channel>? = null

    suspend fun getChannels(): List<Channel> {
        if (cachedChannels != null) return cachedChannels!!

        val uniqueId = dataStore.uniqueId.firstOrNull() ?: return emptyList()
        val subId = dataStore.subscriberId.firstOrNull() ?: return emptyList()
        val accessToken = dataStore.accessToken.firstOrNull() ?: dataStore.ssoToken.firstOrNull() ?: return emptyList()

        return try {
            val response = metadataApi.getChannels(uniqueId = uniqueId, subId = subId, accessToken = accessToken)
            if (response.isSuccessful && response.body() != null) {
                val jsonBody = response.body()!!.asJsonObject
                if (jsonBody.has("data")) {
                    val dataElement = jsonBody.get("data")
                    if (dataElement.isJsonArray) {
                        val type = object : TypeToken<List<Channel>>() {}.type
                        val channels: List<Channel> = gson.fromJson(dataElement, type)
                        cachedChannels = channels.filter { it.getResolvedId().isNotEmpty() }
                        return cachedChannels!!
                    } else if (dataElement.isJsonObject) {
                        val type = object : TypeToken<Map<String, Channel>>() {}.type
                        val channelMap: Map<String, Channel> = gson.fromJson(dataElement, type)
                        cachedChannels = channelMap.values.filter { it.getResolvedId().isNotEmpty() }
                        return cachedChannels!!
                    }
                }
            }
            emptyList()
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
