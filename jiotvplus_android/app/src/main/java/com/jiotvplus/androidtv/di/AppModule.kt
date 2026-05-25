package com.jiotvplus.androidtv.di

import android.content.Context
import com.jiotvplus.androidtv.data.AppConfig
import com.jiotvplus.androidtv.data.local.SettingsDataStore
import com.jiotvplus.androidtv.data.remote.AuthApi
import com.jiotvplus.androidtv.data.remote.ExchangeTokenApi
import com.jiotvplus.androidtv.data.remote.MetadataApi
import com.jiotvplus.androidtv.data.remote.PlaybackApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSettingsDataStore(@ApplicationContext context: Context): SettingsDataStore {
        return SettingsDataStore(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        val headerInterceptor = Interceptor { chain ->
            val request = chain.request().newBuilder()
                .addHeader("x-apisignatures", AppConfig.X_APISIGNATURE)
                .addHeader("x-feature-code", AppConfig.X_FEATURE_CODE)
                .addHeader("x-api-key", AppConfig.X_API_KEY)
                .addHeader("x-platform", AppConfig.X_PLATFORM)
                .addHeader("x-appname", "JioTVPlus")
                .addHeader("app-name", "RJIL_JioTVPlus")
                .addHeader("User-Agent", AppConfig.USER_AGENT)
                .addHeader("devicetype", "tv")
                .addHeader("os", "android")
                .addHeader("lbcookie", "1")
                .build()
            chain.proceed(request)
        }
        return OkHttpClient.Builder()
            .addInterceptor(headerInterceptor)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    @Named("AuthRetrofit")
    fun provideAuthRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(AppConfig.AUTH_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideAuthApi(@Named("AuthRetrofit") retrofit: Retrofit): AuthApi {
        return retrofit.create(AuthApi::class.java)
    }

    @Provides
    @Singleton
    fun provideExchangeTokenApi(okHttpClient: OkHttpClient): ExchangeTokenApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(AppConfig.EXCHANGE_TOKEN_URL.substringBeforeLast("exchangetoken"))
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(ExchangeTokenApi::class.java)
    }

    @Provides
    @Singleton
    fun provideMetadataApi(okHttpClient: OkHttpClient): MetadataApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(AppConfig.METADATA_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(MetadataApi::class.java)
    }

    @Provides
    @Singleton
    fun providePlaybackApi(okHttpClient: OkHttpClient): PlaybackApi {
        val retrofit = Retrofit.Builder()
            .baseUrl(AppConfig.PLAYBACK_BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        return retrofit.create(PlaybackApi::class.java)
    }
}
