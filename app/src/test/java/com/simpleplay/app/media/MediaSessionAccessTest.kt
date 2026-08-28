package com.simpleplay.app.media

import android.media.session.PlaybackState
import org.junit.Assert.assertEquals
import org.junit.Test

class MediaSessionAccessTest {
    private val candidates = listOf(
        SessionCandidate("session-1", "player.one", isPlaying = false),
        SessionCandidate("session-2", "player.two", isPlaying = true),
        SessionCandidate("session-3", "player.three", isPlaying = false),
    )

    @Test
    fun preferredPackageTakesPriorityOverPlayingSession() {
        assertEquals(2, chooseSessionIndex(candidates, "player.three"))
    }

    @Test
    fun preferredSessionTakesPriorityWithinSamePackage() {
        val samePackage = listOf(
            SessionCandidate("first", "player.one", isPlaying = true),
            SessionCandidate("second", "player.one", isPlaying = false),
        )
        assertEquals(1, chooseSessionIndex(samePackage, "player.one", "second"))
    }

    @Test
    fun playingSessionIsUsedWhenPreferenceIsMissing() {
        assertEquals(1, chooseSessionIndex(candidates, "missing.player"))
    }

    @Test
    fun emptySessionListHasNoSelection() {
        assertEquals(-1, chooseSessionIndex(emptyList(), null))
    }

    @Test
    fun skipRestoresOriginalPlaybackMode() {
        assertEquals(RestoreTransport.PLAY, restoreTransportFor(PlaybackState.STATE_PLAYING))
        assertEquals(RestoreTransport.PLAY, restoreTransportFor(PlaybackState.STATE_BUFFERING))
        assertEquals(RestoreTransport.PLAY, restoreTransportFor(PlaybackState.STATE_CONNECTING))
        assertEquals(RestoreTransport.PAUSE, restoreTransportFor(PlaybackState.STATE_PAUSED))
        assertEquals(RestoreTransport.STOP, restoreTransportFor(PlaybackState.STATE_STOPPED))
    }
}
