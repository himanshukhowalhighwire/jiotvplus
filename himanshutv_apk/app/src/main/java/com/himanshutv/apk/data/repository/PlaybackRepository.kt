package com.himanshutv.apk.data.repository

import android.util.Base64
import android.util.Log
import com.himanshutv.apk.data.local.SettingsDataStore
import com.himanshutv.apk.data.model.PlaybackInfo
import com.himanshutv.apk.data.model.PlaybackRightsRequest
import com.himanshutv.apk.data.remote.PlaybackApi
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class PlaybackRepository @Inject constructor(
    private val playbackApi: PlaybackApi,
    private val dataStore: SettingsDataStore
) {
    suspend fun getPlaybackRights(contentId: String): PlaybackInfo? {
        return try {
            val uniqueId = dataStore.uniqueId.firstOrNull() ?: return null
            val subId = dataStore.subscriberId.firstOrNull() ?: return null
            val accessToken = dataStore.accessToken.firstOrNull() ?: dataStore.ssoToken.firstOrNull() ?: return null
            val mobileNumber = dataStore.mobileNumber.firstOrNull() ?: "9999999999"
            
            val jToken = dataStore.jToken.firstOrNull()
            val lbCookie = dataStore.lbCookie.firstOrNull()
            
            val cleaned = mobileNumber.replace(Regex("[^0-9]"), "")
            val rmn = Base64.encodeToString(cleaned.toByteArray(), Base64.NO_WRAP)
            
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
                deviceId = uniqueId,
                restriction = "0",
                xPage = "Player",
                xAnalyticRestriction = "0",
                request = request
            )
            
            if (response.isSuccessful) {
                val data = response.body()?.data
                if (data != null) {
                    val mpdAuto = data.mpd?.auto
                    val m3u8Auto = data.m3u8?.auto
                    
                    val streamUrl = if (!mpdAuto.isNullOrEmpty()) mpdAuto
                                   else if (!m3u8Auto.isNullOrEmpty()) m3u8Auto
                                   else null
                    
                    val isM3u8 = streamUrl?.contains(".m3u8") == true
                    val keyUrl = data.keyURL
                    val playbackToken = data.playbackToken
                    
                    if (streamUrl != null) {
                        PlaybackInfo(
                            streamUrl = streamUrl, 
                            keyUrl = keyUrl, 
                            isM3u8 = isM3u8, 
                            playbackToken = playbackToken
                        )
                    } else {
                        Log.e("PlaybackRepo", "No stream URL found in playback rights response")
                        null
                    }
                } else {
                    Log.e("PlaybackRepo", "data field is null in playback rights response")
                    null
                }
            } else {
                Log.e("PlaybackRepo", "Playback API returned code: ${response.code()} body: ${response.errorBody()?.string()}")
                null
            }
        } catch (e: Exception) {
            Log.e("PlaybackRepo", "Exception fetching playback rights", e)
            null
        }
    }
}
