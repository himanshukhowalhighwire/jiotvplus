package com.jiotvplus.androidtv.data.remote

import com.jiotvplus.androidtv.data.model.ChannelResponse
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface MetadataApi {
    @GET("livechannels")
    suspend fun getLiveChannels(
        @Header("uniqueid") uniqueId: String,
        @Header("subId") subId: String,
        @Header("x-accesstoken") accessToken: String,
        @Header("x-page") xPage: String = "Metadata",
        @Header("x-analytic-restriction") xAnalyticRestriction: String = "0"
    ): Response<ChannelResponse>
}
