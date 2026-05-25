package com.jiotvplus.androidtv.data.remote

import com.jiotvplus.androidtv.data.model.PlaybackRightsRequest
import com.jiotvplus.androidtv.data.model.PlaybackRightsResponse
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
        @Header("x-page") xPage: String = "Player",
        @Header("x-analytic-restriction") xAnalyticRestriction: String = "0",
        @Body request: PlaybackRightsRequest
    ): Response<PlaybackRightsResponse>
}
