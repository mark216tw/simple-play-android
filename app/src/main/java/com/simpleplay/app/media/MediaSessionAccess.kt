package com.simpleplay.app.media

import android.content.ComponentName
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import androidx.core.app.NotificationManagerCompat
import androidx.core.graphics.createBitmap
import java.util.concurrent.ConcurrentHashMap

enum class PlaybackAction {
    PLAY_PAUSE,
    STOP,
    PREVIOUS,
    NEXT,
}

internal enum class RestoreTransport {
    PLAY,
    PAUSE,
    STOP,
}

internal fun restoreTransportFor(playbackState: Int): RestoreTransport = when (playbackState) {
    PlaybackState.STATE_PLAYING,
    PlaybackState.STATE_BUFFERING,
    PlaybackState.STATE_CONNECTING,
    PlaybackState.STATE_FAST_FORWARDING,
    PlaybackState.STATE_REWINDING,
    PlaybackState.STATE_SKIPPING_TO_NEXT,
    PlaybackState.STATE_SKIPPING_TO_PREVIOUS,
    PlaybackState.STATE_SKIPPING_TO_QUEUE_ITEM -> RestoreTransport.PLAY
    PlaybackState.STATE_STOPPED -> RestoreTransport.STOP
    else -> RestoreTransport.PAUSE
}

data class MediaSessionInfo(
    val controller: MediaController,
    val sessionId: String,
    val packageName: String,
    val appName: String,
    val appIcon: Bitmap?,
    val artwork: Bitmap?,
    val title: String,
    val artist: String,
    val hasMetadata: Boolean,
    val isPlaying: Boolean,
    val playbackState: Int,
    val actions: Long,
) {
    fun supports(action: PlaybackAction): Boolean = when (action) {
        PlaybackAction.PLAY_PAUSE -> {
            val expected = if (isPlaying) PlaybackState.ACTION_PAUSE else PlaybackState.ACTION_PLAY
            actions.hasAny(expected or PlaybackState.ACTION_PLAY_PAUSE)
        }
        PlaybackAction.STOP -> actions.hasAny(PlaybackState.ACTION_STOP)
        PlaybackAction.PREVIOUS -> actions.hasAny(PlaybackState.ACTION_SKIP_TO_PREVIOUS)
        PlaybackAction.NEXT -> actions.hasAny(PlaybackState.ACTION_SKIP_TO_NEXT)
    }
}

object MediaSessionAccess {
    fun hasNotificationAccess(context: Context): Boolean =
        NotificationManagerCompat.getEnabledListenerPackages(context).contains(context.packageName)

    fun activeControllers(context: Context): List<MediaController> {
        if (!hasNotificationAccess(context)) return emptyList()
        val manager = context.getSystemService(MediaSessionManager::class.java)
        val listener = ComponentName(context, MediaNotificationListenerService::class.java)
        return try {
            manager.getActiveSessions(listener)
        } catch (_: SecurityException) {
            emptyList()
        }
    }

    fun sessions(context: Context): List<MediaSessionInfo> =
        activeControllers(context).map { it.toInfo(context) }

    fun chooseSession(
        sessions: List<MediaSessionInfo>,
        preferredPackage: String?,
        preferredSessionId: String? = null,
    ): MediaSessionInfo? {
        val index = chooseSessionIndex(
            candidates = sessions.map { SessionCandidate(it.sessionId, it.packageName, it.isPlaying) },
            preferredPackage = preferredPackage,
            preferredSessionId = preferredSessionId,
        )
        return sessions.getOrNull(index)
    }

    fun perform(
        context: Context,
        action: PlaybackAction,
        preferredPackage: String?,
        preferredSessionId: String? = null,
    ): Boolean {
        val session = chooseSession(sessions(context), preferredPackage, preferredSessionId) ?: return false
        if (!session.supports(action)) return false
        perform(session, action)
        return true
    }

    fun perform(session: MediaSessionInfo, action: PlaybackAction) {
        if (!session.supports(action)) return
        val controls = session.controller.transportControls
        when (action) {
            PlaybackAction.PLAY_PAUSE -> if (session.isPlaying) controls.pause() else controls.play()
            PlaybackAction.STOP -> controls.stop()
            PlaybackAction.PREVIOUS,
            PlaybackAction.NEXT -> {
                val originalState = session.playbackState
                if (action == PlaybackAction.PREVIOUS) controls.skipToPrevious()
                else controls.skipToNext()
                restorePlaybackState(session.controller, originalState)
            }
        }
    }

    private fun restorePlaybackState(
        controller: MediaController,
        originalState: Int,
    ) {
        val controls = controller.transportControls
        when (restoreTransportFor(originalState)) {
            RestoreTransport.PLAY -> controls.play()
            RestoreTransport.PAUSE -> controls.pause()
            RestoreTransport.STOP -> controls.stop()
        }
    }
}

internal data class SessionCandidate(
    val sessionId: String,
    val packageName: String,
    val isPlaying: Boolean,
)

internal fun chooseSessionIndex(
    candidates: List<SessionCandidate>,
    preferredPackage: String?,
    preferredSessionId: String? = null,
): Int {
    val sessionIndex = candidates.indexOfFirst { it.sessionId == preferredSessionId }
    if (sessionIndex >= 0) return sessionIndex
    val preferredIndex = candidates.indexOfFirst { it.packageName == preferredPackage }
    if (preferredIndex >= 0) return preferredIndex
    val playingIndex = candidates.indexOfFirst { it.isPlaying }
    return if (playingIndex >= 0) playingIndex else candidates.indices.firstOrNull() ?: -1
}

private fun MediaController.toInfo(context: Context): MediaSessionInfo {
    val metadata = metadata
    val state = playbackState
    val identity = appIdentityCache.getOrPut(packageName) { loadAppIdentity(context, packageName) }
    val appName = identity.name
    val appIcon = identity.icon
    val artwork = metadata?.getBitmap(MediaMetadata.METADATA_KEY_ALBUM_ART)
        ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_ART)
        ?: metadata?.getBitmap(MediaMetadata.METADATA_KEY_DISPLAY_ICON)
    val title = metadata?.getString(MediaMetadata.METADATA_KEY_TITLE)
        ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE)
        ?: appName
    val artist = metadata?.getString(MediaMetadata.METADATA_KEY_ARTIST)
        ?: metadata?.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
        ?: metadata?.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE)
        ?: appName

    return MediaSessionInfo(
        controller = this,
        sessionId = "$packageName:${sessionToken.hashCode()}",
        packageName = packageName,
        appName = appName,
        appIcon = appIcon,
        artwork = artwork,
        title = title,
        artist = artist,
        hasMetadata = metadata != null,
        isPlaying = state?.state == PlaybackState.STATE_PLAYING,
        playbackState = state?.state ?: PlaybackState.STATE_NONE,
        actions = state?.actions ?: 0L,
    )
}

private data class AppIdentity(val name: String, val icon: Bitmap?)

private val appIdentityCache = ConcurrentHashMap<String, AppIdentity>()

private fun loadAppIdentity(context: Context, packageName: String): AppIdentity {
    val packageManager = context.packageManager
    val name = runCatching {
        packageManager.getApplicationLabel(packageManager.getApplicationInfo(packageName, 0)).toString()
    }.getOrDefault(packageName)
    val icon = runCatching { packageManager.getApplicationIcon(packageName).toBitmap() }.getOrNull()
    return AppIdentity(name, icon)
}

private fun Long.hasAny(flags: Long): Boolean = this and flags != 0L

private fun android.graphics.drawable.Drawable.toBitmap(): Bitmap {
    if (this is BitmapDrawable && bitmap != null) return bitmap
    val bitmap = createBitmap(
        intrinsicWidth.coerceAtLeast(1),
        intrinsicHeight.coerceAtLeast(1),
    )
    val canvas = Canvas(bitmap)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bitmap
}
