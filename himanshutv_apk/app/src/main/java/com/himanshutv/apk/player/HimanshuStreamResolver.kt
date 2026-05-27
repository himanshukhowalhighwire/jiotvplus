package com.himanshutv.apk.player

import android.net.Uri
import android.util.Log
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.ResolvingDataSource
import com.himanshutv.apk.data.local.SettingsDataStore
import com.himanshutv.apk.data.model.PlaybackInfo
import com.himanshutv.apk.data.repository.PlaybackRepository
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import java.io.IOException

@UnstableApi
class HimanshuStreamResolver(
    private val contentId: String,
    private val playbackRepository: PlaybackRepository,
    private val dataStore: SettingsDataStore,
    initialPlaybackInfo: PlaybackInfo
) : ResolvingDataSource.Resolver {

    private var lastFetchTime = System.currentTimeMillis()
    private var currentPlaybackInfo = initialPlaybackInfo
    private var currentStreamUrl = initialPlaybackInfo.streamUrl ?: ""
    private var currentCookie = buildCookieFromUrl(currentStreamUrl)

    @Throws(IOException::class)
    override fun resolveDataSpec(dataSpec: DataSpec): DataSpec {
        val now = System.currentTimeMillis()
        if (now - lastFetchTime > 90_000) {
            synchronized(this) {
                if (now - lastFetchTime > 90_000) {
                    Log.d("StreamResolver", "Token expired (90s). Refreshing token for channel: $contentId")
                    val freshInfo = runBlocking {
                        playbackRepository.getPlaybackRights(contentId)
                    }
                    if (freshInfo != null && freshInfo.streamUrl != null) {
                        currentPlaybackInfo = freshInfo
                        currentStreamUrl = freshInfo.streamUrl
                        currentCookie = buildCookieFromUrl(currentStreamUrl)
                        lastFetchTime = System.currentTimeMillis()
                        Log.d("StreamResolver", "Token successfully refreshed. New Stream URL: $currentStreamUrl")
                    } else {
                        Log.e("StreamResolver", "Failed to refresh playback rights token.")
                    }
                }
            }
        }

        // Apply refreshed query params (specifically token) to the incoming segment/manifest URL
        val originalUri = dataSpec.uri
        val updatedUri = updateUriParams(originalUri, currentStreamUrl)

        // Rebuild headers to include the updated Cookie and required jToken/lbCookie headers
        val originalHeaders = dataSpec.httpRequestHeaders
        val updatedHeaders = originalHeaders.toMutableMap()
        if (currentCookie.isNotEmpty()) {
            updatedHeaders["Cookie"] = currentCookie
        }

        runBlocking {
            dataStore.jToken.firstOrNull()?.let { jt ->
                updatedHeaders["jToken"] = jt
            }
            dataStore.lbCookie.firstOrNull()?.let { lb ->
                updatedHeaders["lbCookie"] = lb
            }
        }

        return dataSpec.buildUpon()
            .setUri(updatedUri)
            .setHttpRequestHeaders(updatedHeaders)
            .build()
    }

    private fun updateUriParams(uri: Uri, newStreamUrl: String): Uri {
        val newUri = Uri.parse(newStreamUrl)
        val builder = uri.buildUpon()
        
        // Fetch all parameter names from the new stream URL (which contains the refreshed token)
        val queryNames = newUri.queryParameterNames
        if (queryNames.isNotEmpty()) {
            val newParams = mutableMapOf<String, String>()
            for (name in queryNames) {
                newUri.getQueryParameter(name)?.let { newParams[name] = it }
            }
            
            val originalParams = mutableMapOf<String, String>()
            for (name in uri.queryParameterNames) {
                uri.getQueryParameter(name)?.let { originalParams[name] = it }
            }
            
            // Overwrite or add the new query params
            originalParams.putAll(newParams)
            
            // Rebuild the query parameters
            builder.clearQuery()
            for ((k, v) in originalParams) {
                builder.appendQueryParameter(k, v)
            }
        }
        return builder.build()
    }

    private fun buildCookieFromUrl(url: String): String {
        if (!url.contains("?")) return ""
        val query = url.substringAfter("?")
        val sb = java.lang.StringBuilder()
        for (param in query.split("&")) {
            val eqIdx = param.indexOf('=')
            if (eqIdx > 0) {
                val k = param.substring(0, eqIdx)
                val v = param.substring(eqIdx + 1)
                if (sb.isNotEmpty()) sb.append("; ")
                sb.append("$k=$v")
                if (k == "hdnea" || k == "__hdnea__") {
                    sb.append("; __hdnea__=$v; hdnea=$v")
                }
            }
        }
        
        // Append jToken and lbCookie if they exist
        runBlocking {
            dataStore.jToken.firstOrNull()?.let { jt ->
                if (sb.isNotEmpty()) sb.append("; ")
                sb.append("jToken=$jt")
            }
            dataStore.lbCookie.firstOrNull()?.let { lb ->
                if (sb.isNotEmpty()) sb.append("; ")
                sb.append("lbCookie=$lb")
            }
        }
        return sb.toString()
    }
}
