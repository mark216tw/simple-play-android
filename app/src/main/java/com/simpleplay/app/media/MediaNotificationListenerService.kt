package com.simpleplay.app.media

import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.service.notification.NotificationListenerService
import com.simpleplay.app.widget.PlaybackWidgetProvider

class MediaNotificationListenerService : NotificationListenerService() {
    private val callbacks = mutableListOf<Pair<MediaController, MediaController.Callback>>()
    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener {
        observeSessions(it.orEmpty())
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        val manager = getSystemService(MediaSessionManager::class.java)
        val component = ComponentName(this, MediaNotificationListenerService::class.java)
        manager.removeOnActiveSessionsChangedListener(sessionsChangedListener)
        manager.addOnActiveSessionsChangedListener(sessionsChangedListener, component)
        observeSessions(MediaSessionAccess.activeControllers(this))
    }

    override fun onListenerDisconnected() {
        stopObserving()
        super.onListenerDisconnected()
    }

    override fun onDestroy() {
        stopObserving()
        super.onDestroy()
    }

    private fun observeSessions(controllers: List<MediaController>) {
        clearCallbacks()
        controllers.forEach { controller ->
            val callback = object : MediaController.Callback() {
                override fun onPlaybackStateChanged(state: PlaybackState?) = updateWidgets()
                override fun onMetadataChanged(metadata: MediaMetadata?) = updateWidgets()
                override fun onSessionDestroyed() = updateWidgets()
            }
            controller.registerCallback(callback)
            callbacks += controller to callback
        }
        updateWidgets()
    }

    private fun clearCallbacks() {
        callbacks.forEach { (controller, callback) -> controller.unregisterCallback(callback) }
        callbacks.clear()
    }

    private fun stopObserving() {
        clearCallbacks()
        getSystemService(MediaSessionManager::class.java)
            .removeOnActiveSessionsChangedListener(sessionsChangedListener)
    }

    private fun updateWidgets() {
        PlaybackWidgetProvider.updateAll(this)
    }
}
