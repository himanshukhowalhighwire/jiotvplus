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
                
                val list = if (dataElement.isJsonArray) {
                    gson.fromJson(dataElement, listType) ?: emptyList()
                } else if (dataElement.isJsonObject) {
                    val mapType = object : com.google.gson.reflect.TypeToken<Map<String, Channel>>() {}.type
                    val map: Map<String, Channel>? = gson.fromJson(dataElement, mapType)
                    map?.values?.toList() ?: emptyList()
                } else {
                    emptyList()
                }
                cachedChannels = list
                list
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
