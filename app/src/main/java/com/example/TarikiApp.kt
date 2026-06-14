package com.example

import android.app.Application
import com.example.core.config.SessionManager

class TarikiApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SessionManager.init(this)
    }
}
