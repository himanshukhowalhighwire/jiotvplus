package com.himanshutv.apk.player

import android.util.Base64
import com.himanshutv.apk.data.AppConfig
import com.himanshutv.apk.data.local.SettingsDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

class HimanshuMediaDrmCallback(
    private val keyUrl: String,
    private val channelId: String,
    private val playbackToken: String?,
    private val dataStore: SettingsDataStore,
    private val okHttpClient: OkHttpClient
) : androidx.media3.exoplayer.drm.MediaDrmCallback {

    override fun executeProvisionRequest(
        uuid: UUID,
        request: androidx.media3.exoplayer.drm.ExoMediaDrm.ProvisionRequest
    ): ByteArray {
        val url = request.defaultUrl + "&signedRequest=" + String(request.data)
        val req = Request.Builder().url(url).build()
        okHttpClient.newCall(req).execute().use { response ->
            return response.body?.bytes() ?: ByteArray(0)
        }
    }

    override fun executeKeyRequest(
        uuid: UUID,
        request: androidx.media3.exoplayer.drm.ExoMediaDrm.KeyRequest
    ): ByteArray {
        var responseBytes = ByteArray(0)
        runBlocking {
            val accessToken = dataStore.accessToken.firstOrNull() ?: dataStore.ssoToken.firstOrNull() ?: ""
            val uniqueId = dataStore.uniqueId.firstOrNull() ?: ""
            val subscriberId = dataStore.subscriberId.firstOrNull() ?: ""
            val mobileNumber = dataStore.mobileNumber.firstOrNull() ?: ""
            val ssoToken = dataStore.ssoToken.firstOrNull() ?: ""
            val jToken = dataStore.jToken.firstOrNull()
            val lbCookie = dataStore.lbCookie.firstOrNull()

            val cleaned = mobileNumber.replace(Regex("[^0-9]"), "")
            val encodedRmn = Base64.encodeToString(cleaned.toByteArray(), Base64.NO_WRAP)

            val isLegacy = keyUrl.contains("tv.media.jio.com/proxy")
            
            val builder = Request.Builder()
                .url(keyUrl)
                .header("User-Agent", AppConfig.USER_AGENT)
                .header("Accept", "*/*")
            
            if (isLegacy) {
                builder.header("srno", "23061A22C051410")
                builder.header("channelid", channelId)
                builder.header("versionCode", "2072")
                builder.header("os", "android")
                builder.header("devicetype", "tv")
                builder.header("deviceid", uniqueId)
                builder.header("usergroup", "474537347347373")
                builder.header("subscriberid", subscriberId)
                builder.header("crmid", subscriberId)
                builder.header("playbackToken", playbackToken ?: "")
                builder.header("ssotoken", ssoToken)
            } else {
                val cal = java.util.Calendar.getInstance()
                val generatedSrno = String.format(
                    java.util.Locale.US,
                    "%02d%02d%02d%02d%02d%03d",
                    cal.get(java.util.Calendar.YEAR) % 100,
                    cal.get(java.util.Calendar.MONTH) + 1,
                    cal.get(java.util.Calendar.DAY_OF_MONTH),
                    cal.get(java.util.Calendar.HOUR_OF_DAY),
                    cal.get(java.util.Calendar.MINUTE),
                    cal.get(java.util.Calendar.MILLISECOND)
                )
                builder.header("x-accesstoken", accessToken)
                builder.header("accesstoken", accessToken)
                builder.header("ssotoken", ssoToken)
                builder.header("uniqueid", uniqueId)
                builder.header("uniqueId", uniqueId)
                builder.header("deviceId", uniqueId)
                builder.header("subId", subscriberId)
                builder.header("subscriberId", subscriberId)
                builder.header("crmid", subscriberId)
                builder.header("userId", subscriberId)
                builder.header("channelid", channelId)
                builder.header("channel_id", channelId)
                builder.header("srno", generatedSrno)
                builder.header("appName", "RJIL_JioTVPlus")
                builder.header("x-appname", "JioTVPlus")
                builder.header("appname", "RJIL_JioTVPlus")
                builder.header("app-name", "RJIL_JioTVPlus")
                builder.header("x-platform", "smartandroidtv")
                builder.header("x-api-key", AppConfig.X_API_KEY)
                builder.header("x-apisignatures", AppConfig.X_APISIGNATURE)
                builder.header("x-feature-code", AppConfig.X_FEATURE_CODE)
                builder.header("devicetype", "tv")
                builder.header("os", "android")
                builder.header("osVersion", "9")
                builder.header("usergroup", "474537347347373")
                builder.header("versionCode", "2072")
                builder.header("lbcookie", "1")
                builder.header("Authorization", "Bearer $accessToken")
                builder.header("rmn", encodedRmn)
                if (jToken != null) builder.header("jToken", jToken)
                if (lbCookie != null) builder.header("lbCookie", lbCookie)
            }

            val body = request.data.toRequestBody("application/octet-stream".toMediaType())
            builder.post(body)

            val okReq = builder.build()
            try {
                okHttpClient.newCall(okReq).execute().use { resp ->
                    responseBytes = resp.body?.bytes() ?: ByteArray(0)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        return responseBytes
    }
}
