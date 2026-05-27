package com.himanshutv.apk.data.model

import com.google.gson.annotations.SerializedName

data class OtpResponse(
    @SerializedName("identifier") val identifier: String?
)

data class VerifyOtpRequest(
    @SerializedName("identifier") val identifier: String,
    @SerializedName("otp") val otp: String,
    @SerializedName("upgradeAuth") val upgradeAuth: String = "Y",
    @SerializedName("rememberUser") val rememberUser: String = "T",
    @SerializedName("deviceInfo") val deviceInfo: DeviceInfo = DeviceInfo()
)

data class DeviceInfo(
    @SerializedName("consumptionDeviceName") val consumptionDeviceName: String = "HimanshuTVAndroidTV",
    @SerializedName("info") val info: Info = Info()
)

data class Info(
    @SerializedName("type") val type: String = "android",
    @SerializedName("androidId") val androidId: String = "30303030303030303030303030303030",
    @SerializedName("platform") val platform: Platform = Platform()
)

data class Platform(
    @SerializedName("name") val name: String = "HimanshuTVAndroidTV"
)

data class VerifyOtpResponse(
    @SerializedName("ssoToken", alternate = ["ssotoken"]) val ssoToken: String?,
    @SerializedName("jToken", alternate = ["jtoken"]) val jToken: String?,
    @SerializedName("lbCookie", alternate = ["lbcookie"]) val lbCookie: String?,
    @SerializedName("subscriberId", alternate = ["subscriberid"]) val subscriberId: String?,
    @SerializedName("sessionAttributes") val sessionAttributes: SessionAttributes?
)

data class SessionAttributes(
    @SerializedName("lbCookie", alternate = ["lbcookie"]) val lbCookie: String?,
    @SerializedName("user") val user: User?
)

data class User(
    @SerializedName("ssoToken", alternate = ["ssotoken"]) val ssoToken: String?,
    @SerializedName("jToken", alternate = ["jtoken"]) val jToken: String?,
    @SerializedName("lbCookie", alternate = ["lbcookie"]) val lbCookie: String?,
    @SerializedName("subscriberId", alternate = ["subscriberid"]) val subscriberId: String?,
    @SerializedName("unique", alternate = ["uniqueId", "uniqueid"]) val unique: String?
)

data class ExchangeTokenRequest(
    @SerializedName("number") val numberBase64: String
)

data class ExchangeTokenResponse(
    @SerializedName("authToken") val authToken: String?,
    @SerializedName("refreshToken") val refreshToken: String?,
    @SerializedName("data") val data: ExchangeTokenData?
)

data class ExchangeTokenData(
    @SerializedName("authToken") val authToken: String?
)
