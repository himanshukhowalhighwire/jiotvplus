package com.jiotvplus.androidtv.player

import android.net.Uri
import com.jiotvplus.androidtv.data.AppConfig
import com.jiotvplus.androidtv.data.local.SettingsDataStore
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Call
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException

class JioMediaDrmCallback(
    private val keyUrl: String,
    private val dataStore: SettingsDataStore,
    private val okHttpClient: OkHttpClient
) : androidx.media3.exoplayer.drm.MediaDrmCallback {

    override fun executeProvisionRequest(
        uuid: java.util.UUID,
        request: androidx.media3.exoplayer.drm.ExoMediaDrm.ProvisionRequest
    ): ByteArray {
        val url = request.defaultUrl + "&signedRequest=" + String(request.data)
        val req = Request.Builder().url(url).build()
        okHttpClient.newCall(req).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            return response.body?.bytes() ?: ByteArray(0)
        }
    }

    override fun executeKeyRequest(
        uuid: java.util.UUID,
        request: androidx.media3.exoplayer.drm.ExoMediaDrm.KeyRequest
    ): ByteArray {
        val (uniqueId, subId, accessToken, mobileNumber, ssoToken, jToken, lbCookie) = runBlocking {
            val u = dataStore.uniqueId.firstOrNull() ?: ""
            val sub = dataStore.subscriberId.firstOrNull() ?: ""
            val access = dataStore.accessToken.firstOrNull() ?: dataStore.ssoToken.firstOrNull() ?: ""
            val mob = dataStore.mobileNumber.firstOrNull() ?: ""
            val sso = dataStore.ssoToken.firstOrNull() ?: ""
            val jt = dataStore.jToken.firstOrNull() ?: ""
            val lb = dataStore.lbCookie.firstOrNull() ?: ""
            listOf(u, sub, access, mob, sso, jt, lb)
        }

        val cleaned = mobileNumber.replace(Regex("[^0-9]"), "")
        val rmn = android.util.Base64.encodeToString(cleaned.toByteArray(), android.util.Base64.NO_WRAP)

        val isLegacy = keyUrl.contains("tv.media.jio.com/proxy")
        
        val builder = Request.Builder().url(keyUrl)
            .post(request.data.toRequestBody(null))
            .header("User-Agent", AppConfig.USER_AGENT)
            .header("Content-Type", "application/octet-stream")
            .header("Accept", "*/*")
            
        if (isLegacy) {
            builder.header("srno", "23061A22C051410")
            builder.header("versionCode", "2072")
            builder.header("os", "android")
            builder.header("devicetype", "tv")
            builder.header("deviceid", uniqueId)
            builder.header("usergroup", "474537347347373")
            builder.header("subscriberid", subId)
            builder.header("crmid", subId)
            builder.header("ssotoken", ssoToken)
            // PlaybackToken is skipped here as it requires parsing from rights response, usually works without it or we can pass it if strictly needed.
        } else {
            builder.header("x-accesstoken", accessToken)
            builder.header("uniqueid", uniqueId)
            builder.header("subId", subId)
            builder.header("x-api-key", AppConfig.X_API_KEY)
            builder.header("Authorization", "Bearer $accessToken")
            builder.header("rmn", rmn)
            if (jToken.isNotEmpty()) builder.header("jToken", jToken)
            if (lbCookie.isNotEmpty()) builder.header("lbCookie", lbCookie)
        }

        val req = builder.build()
        okHttpClient.newCall(req).execute().use { response ->
            if (!response.isSuccessful) throw IOException("Unexpected code $response")
            return response.body?.bytes() ?: ByteArray(0)
        }
    }
}
