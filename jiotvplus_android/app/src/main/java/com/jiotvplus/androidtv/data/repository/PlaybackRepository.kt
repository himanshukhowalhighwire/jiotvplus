package com.jiotvplus.androidtv.data.repository

import com.jiotvplus.androidtv.data.local.SettingsDataStore
import com.jiotvplus.androidtv.data.model.PlaybackInfo
import com.jiotvplus.androidtv.data.model.PlaybackRightsRequest
import com.jiotvplus.androidtv.data.remote.PlaybackApi
import com.google.gson.Gson
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackRepository @Inject constructor(
    private val playbackApi: PlaybackApi,
    private val dataStore: SettingsDataStore
) {
    private val gson = Gson()

    suspend fun getPlaybackRights(contentId: String): PlaybackInfo? {
        return try {
            val uniqueId = dataStore.uniqueId.firstOrNull() ?: return null
            val subId = dataStore.subscriberId.firstOrNull() ?: return null
            val accessToken = dataStore.accessToken.firstOrNull() ?: dataStore.ssoToken.firstOrNull() ?: return null
            val mobileNumber = dataStore.mobileNumber.firstOrNull() ?: "9999999999"
            
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
                val data = response.body()?.data
                if (data != null) {
                    // Extract stream URL: prefer mpd.auto, fallback to m3u8.auto
                    // Exactly as in PHP live.php lines 26-30
                    val mpdAuto = data.mpd?.auto
                    val m3u8Auto = data.m3u8?.auto
                    
                    val streamUrl = if (!mpdAuto.isNullOrEmpty()) mpdAuto
                                   else if (!m3u8Auto.isNullOrEmpty()) m3u8Auto
                                   else null
                    
                    val keyUrl = data.keyURL
                    
                    if (streamUrl != null) {
                        PlaybackInfo(streamUrl = streamUrl, keyUrl = keyUrl)
                    } else {
                        android.util.Log.e("PlaybackRepo", "No stream URL found in playback rights response")
                        null
                    }
                } else {
                    android.util.Log.e("PlaybackRepo", "data field is null in playback rights response")
                    null
                }
            } else {
                android.util.Log.e("PlaybackRepo", "Playback API returned code: ${response.code()} body: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("PlaybackRepo", "Exception fetching playback rights", e)
            null
        }
    }
}
