package com.simpleplay.app

import android.app.Application
import android.content.res.Configuration
import com.simpleplay.app.widget.PlaybackWidgetProvider

class SimplePlayApplication : Application() {
    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        PlaybackWidgetProvider.updateAll(this)
    }
}
