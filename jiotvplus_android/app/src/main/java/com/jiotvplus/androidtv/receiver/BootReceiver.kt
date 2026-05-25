package com.jiotvplus.androidtv.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.jiotvplus.androidtv.MainActivity
import com.jiotvplus.androidtv.data.local.SettingsDataStore
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject
    lateinit val dataStore: SettingsDataStore

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            CoroutineScope(Dispatchers.IO).launch {
                val autoStart = dataStore.autoStart.firstOrNull() ?: false
                if (autoStart) {
                    val activityIntent = Intent(context, MainActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        putExtra("from_boot", true)
                    }
                    context.startActivity(activityIntent)
                }
            }
        }
    }
}
