package com.himanshutv.apk.data.repository

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.himanshutv.apk.data.local.SettingsDataStore
import com.himanshutv.apk.data.model.Channel
import com.himanshutv.apk.data.remote.MetadataApi
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChannelRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val metadataApi: MetadataApi,
    private val dataStore: SettingsDataStore
) {
    private val gson = Gson()
    private val cacheFile = File(context.filesDir, "channels_cache.json")
    private var inMemoryCache: List<Channel>? = null

    fun getChannels(forceRefresh: Boolean = false): Flow<List<Channel>> = flow {
        // 1. Emit from memory or local file if not forcing a refresh
        if (!forceRefresh) {
            if (inMemoryCache != null) {
                emit(inMemoryCache!!)
            } else {
                val localChannels = loadFromLocalCache()
                if (localChannels.isNotEmpty()) {
                    inMemoryCache = localChannels
                    emit(localChannels)
                }
            }
        }

        // 2. Fetch from network in the background
        val networkChannels = fetchFromNetwork()
        if (networkChannels.isNotEmpty()) {
            inMemoryCache = networkChannels
            saveToLocalCache(networkChannels)
            emit(networkChannels)
        }
    }

    private fun loadFromLocalCache(): List<Channel> {
        return try {
            if (cacheFile.exists()) {
                val jsonStr = cacheFile.readText()
                val type = object : TypeToken<List<Channel>>() {}.type
                val channels: List<Channel> = gson.fromJson(jsonStr, type)
                channels
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    private fun saveToLocalCache(channels: List<Channel>) {
        try {
            val jsonStr = gson.toJson(channels)
            cacheFile.writeText(jsonStr)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private suspend fun fetchFromNetwork(): List<Channel> {
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
                        return channels.filter { it.getResolvedId().isNotEmpty() }
                    } else if (dataElement.isJsonObject) {
                        val type = object : TypeToken<Map<String, Channel>>() {}.type
                        val channelMap: Map<String, Channel> = gson.fromJson(dataElement, type)
                        return channelMap.values.filter { it.getResolvedId().isNotEmpty() }
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
