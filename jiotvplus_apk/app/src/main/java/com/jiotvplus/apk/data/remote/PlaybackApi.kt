package com.jiotvplus.apk.data.remote

import com.jiotvplus.apk.data.model.PlaybackRightsRequest
import com.jiotvplus.apk.data.model.PlaybackRightsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.http.Path

interface PlaybackApi {
    @POST("{contentId}")
    suspend fun getPlaybackRights(
        @Path("contentId") contentId: String,
        @Header("x-accesstoken") accessToken: String,
        @Header("uniqueid") uniqueId: String,
        @Header("subId") subId: String,
        @Header("rmn") rmn: String,
        @Header("jToken") jToken: String?,
        @Header("lbCookie") lbCookie: String?,
        @Header("Cookie") cookie: String?,
        @Header("deviceId") deviceId: String,
        @Header("restriction") restriction: String,
        @Header("x-page") xPage: String,
        @Header("x-analytic-restriction") xAnalyticRestriction: String,
        @Body request: PlaybackRightsRequest
    ): Response<PlaybackRightsResponse>
}
