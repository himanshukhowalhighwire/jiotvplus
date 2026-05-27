package com.jiotvplus.apk.player

import android.util.Base64
import com.jiotvplus.apk.data.AppConfig
import com.jiotvplus.apk.data.local.SettingsDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.UUID

class JioMediaDrmCallback(
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
                builder.header("x-accesstoken", accessToken)
                builder.header("uniqueid", uniqueId)
                builder.header("subId", subscriberId)
                builder.header("x-api-key", AppConfig.X_API_KEY)
                builder.header("devicetype", "tv")
                builder.header("os", "android")
                builder.header("lbcookie", "1")
                builder.header("deviceId", uniqueId)
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
