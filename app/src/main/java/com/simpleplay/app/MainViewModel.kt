package com.simpleplay.app

import android.app.Application
import android.content.ComponentName
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.simpleplay.app.data.AccentTheme
import com.simpleplay.app.data.AppPreferences
import com.simpleplay.app.data.AppPreferencesRepository
import com.simpleplay.app.data.ThemeMode
import com.simpleplay.app.data.WidgetBackgroundStyle
import com.simpleplay.app.data.WidgetButtonSize
import com.simpleplay.app.media.MediaNotificationListenerService
import com.simpleplay.app.media.MediaSessionAccess
import com.simpleplay.app.media.MediaSessionInfo
import com.simpleplay.app.media.PlaybackAction
import com.simpleplay.app.widget.PlaybackWidgetProvider
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class PlayerUiState(
    val hasNotificationAccess: Boolean = false,
    val sessions: List<MediaSessionInfo> = emptyList(),
    val selectedPackage: String? = null,
    val selectedSessionId: String? = null,
) {
    val selectedSession: MediaSessionInfo?
        get() = sessions.firstOrNull { it.sessionId == selectedSessionId }
            ?: MediaSessionAccess.chooseSession(sessions, selectedPackage, selectedSessionId)
}

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val app: Application
        get() = getApplication()
    private val preferencesRepository = AppPreferencesRepository(application)
    private val mediaSessionManager = application.getSystemService(MediaSessionManager::class.java)
    private val listenerComponent = ComponentName(application, MediaNotificationListenerService::class.java)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val controllerCallbacks = mutableMapOf<String, Pair<MediaController, MediaController.Callback>>()
    private var listening = false

    val preferences: StateFlow<AppPreferences> = preferencesRepository.preferences.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        AppPreferences(),
    )

    private val _playerState = MutableStateFlow(PlayerUiState())
    val playerState: StateFlow<PlayerUiState> = _playerState

    private val sessionsChangedListener = MediaSessionManager.OnActiveSessionsChangedListener {
        refreshSessions(rebindCallbacks = true)
    }

    init {
        viewModelScope.launch {
            preferences.collect { loadedPreferences ->
                val current = _playerState.value
                if (current.selectedSessionId == null &&
                    (current.selectedPackage != loadedPreferences.selectedMediaPackage ||
                        loadedPreferences.selectedMediaSessionId != null)
                ) {
                    _playerState.value = current.copy(
                        selectedPackage = loadedPreferences.selectedMediaPackage,
                        selectedSessionId = loadedPreferences.selectedMediaSessionId,
                    )
                }
                PlaybackWidgetProvider.updateAll(app)
            }
        }
    }

    fun start() {
        val hasAccess = MediaSessionAccess.hasNotificationAccess(app)
        if (!hasAccess) {
            stopListening()
            _playerState.value = PlayerUiState(hasNotificationAccess = false)
            return
        }
        if (!listening) {
            runCatching {
                mediaSessionManager.addOnActiveSessionsChangedListener(
                    sessionsChangedListener,
                    listenerComponent,
                    mainHandler,
                )
            }.onSuccess { listening = true }
        }
        refreshSessions(rebindCallbacks = true)
    }

    fun perform(action: PlaybackAction) {
        val session = _playerState.value.selectedSession ?: return
        if (!session.supports(action)) return
        MediaSessionAccess.perform(session, action)
    }

    fun selectSession(sessionId: String, packageName: String) {
        _playerState.value = _playerState.value.copy(
            selectedPackage = packageName,
            selectedSessionId = sessionId,
        )
        viewModelScope.launch {
            preferencesRepository.setSelectedMediaSession(packageName, sessionId)
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setThemeMode(mode) }
    }

    fun setAccentTheme(theme: AccentTheme) {
        viewModelScope.launch { preferencesRepository.setAccentTheme(theme) }
    }

    fun setCustomAccent(argb: Int) {
        viewModelScope.launch { preferencesRepository.setCustomAccent(argb) }
    }

    fun setWidgetButtonSize(size: WidgetButtonSize) {
        viewModelScope.launch { preferencesRepository.setWidgetButtonSize(size) }
    }

    fun setWidgetThemeMode(mode: ThemeMode) {
        viewModelScope.launch { preferencesRepository.setWidgetThemeMode(mode) }
    }

    fun setWidgetAccentTheme(theme: AccentTheme) {
        viewModelScope.launch { preferencesRepository.setWidgetAccentTheme(theme) }
    }

    fun setWidgetCustomAccent(argb: Int) {
        viewModelScope.launch { preferencesRepository.setWidgetCustomAccent(argb) }
    }

    fun setWidgetBackgroundStyle(style: WidgetBackgroundStyle) {
        viewModelScope.launch { preferencesRepository.setWidgetBackgroundStyle(style) }
    }

    private fun refreshSessions(rebindCallbacks: Boolean = false) {
        val previousSessions = _playerState.value.sessions.associateBy { it.sessionId }
        var sessions = MediaSessionAccess.sessions(app).map { current ->
            val previous = previousSessions[current.sessionId]
            if (!current.hasMetadata && previous != null) {
                current.copy(
                    artwork = previous.artwork,
                    title = previous.title,
                    artist = previous.artist,
                )
            } else {
                current
            }
        }
        if (rebindCallbacks) {
            val oldCallbacks = controllerCallbacks.values.toList()
            controllerCallbacks.clear()
            sessions.forEach { session ->
                val callback = object : MediaController.Callback() {
                    override fun onPlaybackStateChanged(state: PlaybackState?) = refreshSessions()
                    override fun onMetadataChanged(metadata: MediaMetadata?) = refreshSessions()
                    override fun onSessionDestroyed() = refreshSessions(rebindCallbacks = true)
                }
                session.controller.registerCallback(callback, mainHandler)
                controllerCallbacks[session.sessionId] = session.controller to callback
            }
            oldCallbacks.forEach { (controller, callback) ->
                controller.unregisterCallback(callback)
            }
            sessions = MediaSessionAccess.sessions(app)
        }
        val savedPreferences = preferences.value
        _playerState.value = PlayerUiState(
            hasNotificationAccess = true,
            sessions = sessions,
            selectedPackage = _playerState.value.selectedPackage ?: savedPreferences.selectedMediaPackage,
            selectedSessionId = (_playerState.value.selectedSessionId
                ?: savedPreferences.selectedMediaSessionId)
                ?.takeIf { selected -> sessions.any { it.sessionId == selected } },
        )
    }

    private fun clearControllerCallbacks() {
        controllerCallbacks.values.forEach { (controller, callback) ->
            controller.unregisterCallback(callback)
        }
        controllerCallbacks.clear()
    }

    private fun stopListening() {
        if (listening) {
            mediaSessionManager.removeOnActiveSessionsChangedListener(sessionsChangedListener)
            listening = false
        }
        clearControllerCallbacks()
    }

    override fun onCleared() {
        stopListening()
        super.onCleared()
    }
}
