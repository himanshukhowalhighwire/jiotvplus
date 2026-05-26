package com.jiotvplus.apk

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class JioTVApplication : Application() {
    override fun onCreate() {
        super.onCreate()
    }
}
