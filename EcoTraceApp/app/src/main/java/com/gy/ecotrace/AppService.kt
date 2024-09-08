package com.gy.ecotrace

import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import java.io.File
import java.security.Timestamp
import java.sql.Time
import java.util.Date

class AppService: Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val cleanupIntervalMillis: Long = 10 * 60 * 1000 // 10 минут

    override fun onCreate() {
        super.onCreate()
        startCleanupTask()
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    private fun startCleanupTask() {
        handler.postDelayed(object : Runnable {
            override fun run() {
                cleanUpCache()
                handler.postDelayed(this, cleanupIntervalMillis)
            }
        }, cleanupIntervalMillis)
    }

    private fun cleanUpCache() {
        val now = Date().time
        filesDir.listFiles()?.forEach { file ->
            if (now - file.lastModified() > 15 * 60 * 1000 ) {
                file.delete()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        getSharedPreferences("getData", Context.MODE_PRIVATE).edit().putLong("exit", System.currentTimeMillis()).apply()
        Log.d("Destroy", "bye-bye")
    }
}