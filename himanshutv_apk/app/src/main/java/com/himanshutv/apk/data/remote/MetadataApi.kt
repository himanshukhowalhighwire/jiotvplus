package com.himanshutv.apk.data.remote

import com.google.gson.JsonElement
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Header

interface MetadataApi {
    @GET("livechannels")
    suspend fun getChannels(
        @Header("uniqueid") uniqueId: String,
        @Header("subId") subId: String,
        @Header("x-accesstoken") accessToken: String,
        @Header("x-page") xPage: String = "Metadata",
        @Header("x-analytic-restriction") xAnalyticRestriction: String = "0"
    ): Response<JsonElement>
}
