package com.lanfund.app

import android.app.Application

/**
 * Application类
 */
class LanFundApp : Application() {

    override fun onCreate() {
        super.onCreate()
        instance = this
    }

    companion object {
        lateinit var instance: LanFundApp
            private set
    }
}
