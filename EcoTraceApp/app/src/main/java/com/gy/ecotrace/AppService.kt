package com.gy.ecotrace

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log

class AppService: Service() {
    private val filesSuffixes = mutableListOf("info", "events", "friends", "groups", "EVENT", "GROUP")
    override fun onDestroy() {
        super.onDestroy()
        deleteFiles()
    }

    private fun deleteFiles() {
        val filesDir = filesDir
        for (file in filesDir.listFiles()!!) {
            if (file.isFile && matchesSuffixes(file.name)) {
                file.delete()
            }
        }
    }

    private fun matchesSuffixes(fileName: String): Boolean {
        for (suffix in filesSuffixes) {
            if (fileName.matches(".+_$suffix\\.json$".toRegex())) {
                return true
            }
        }
        return false
    }

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }
}