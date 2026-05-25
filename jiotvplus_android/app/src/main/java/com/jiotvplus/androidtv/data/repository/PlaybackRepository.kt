package com.jiotvplus.androidtv.data.repository

import com.jiotvplus.androidtv.data.local.SettingsDataStore
import com.jiotvplus.androidtv.data.model.PlaybackRightsRequest
import com.jiotvplus.androidtv.data.remote.PlaybackApi
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackRepository @Inject constructor(
    private val playbackApi: PlaybackApi,
    private val dataStore: SettingsDataStore
) {
    suspend fun getPlaybackRights(contentId: String): String? {
        return try {
            val uniqueId = dataStore.uniqueId.firstOrNull() ?: return null
            val subId = dataStore.subscriberId.firstOrNull() ?: return null
            val accessToken = dataStore.accessToken.firstOrNull() ?: dataStore.ssoToken.firstOrNull() ?: return null
            val mobileNumber = dataStore.mobileNumber.firstOrNull() ?: return null
            
            val jToken = dataStore.jToken.firstOrNull()
            val lbCookie = dataStore.lbCookie.firstOrNull()
            
            val cleaned = mobileNumber.replace(Regex("[^0-9]"), "")
            val rmn = android.util.Base64.encodeToString(cleaned.toByteArray(), android.util.Base64.NO_WRAP)
            
            var cookieHeader: String? = null
            if (jToken != null && lbCookie != null) {
                cookieHeader = "jToken=$jToken; lbCookie=$lbCookie"
            }

            val request = PlaybackRightsRequest(serialNo = uniqueId)
            
            val response = playbackApi.getPlaybackRights(
                contentId = contentId,
                accessToken = accessToken,
                uniqueId = uniqueId,
                subId = subId,
                rmn = rmn,
                jToken = jToken,
                lbCookie = lbCookie,
                cookie = cookieHeader,
                request = request
            )
            
            if (response.isSuccessful) {
                response.body()?.data?.keyURL
            } else {
                null
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
