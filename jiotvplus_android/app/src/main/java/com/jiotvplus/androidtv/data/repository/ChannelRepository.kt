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
    suspend fun getChannels(): List<Channel> {
        return try {
            val uniqueId = dataStore.uniqueId.firstOrNull() ?: return emptyList()
            val subId = dataStore.subscriberId.firstOrNull() ?: return emptyList()
            val accessToken = dataStore.accessToken.firstOrNull() ?: dataStore.ssoToken.firstOrNull() ?: return emptyList()

            val response = metadataApi.getLiveChannels(uniqueId, subId, accessToken)
            if (response.isSuccessful) {
                response.body()?.data ?: emptyList()
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }
}
