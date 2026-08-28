package com.simpleplay.app.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.ColorStateList
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.View
import android.widget.RemoteViews
import com.simpleplay.app.MainActivity
import com.simpleplay.app.R
import com.simpleplay.app.data.AccentTheme
import com.simpleplay.app.data.AppPreferences
import com.simpleplay.app.data.AppPreferencesRepository
import com.simpleplay.app.data.ThemeMode
import com.simpleplay.app.data.WidgetBackgroundStyle
import com.simpleplay.app.data.WidgetButtonSize
import com.simpleplay.app.media.MediaSessionAccess
import com.simpleplay.app.media.MediaSessionInfo
import com.simpleplay.app.media.PlaybackAction
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import androidx.core.graphics.ColorUtils

private enum class WidgetKind { CONTROL_BAR, PLAY_PAUSE }

open class PlaybackWidgetProvider : AppWidgetProvider() {
    private val widgetKind: WidgetKind
        get() = if (this is PlayPauseWidgetProvider) WidgetKind.PLAY_PAUSE else WidgetKind.CONTROL_BAR

    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray,
    ) {
        launchUpdate {
            updateWidgetsNow(context, appWidgetManager, appWidgetIds, widgetKind)
        }
    }

    override fun onAppWidgetOptionsChanged(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetId: Int,
        newOptions: Bundle,
    ) {
        launchUpdate {
            updateWidgetNow(context, appWidgetManager, appWidgetId, widgetKind)
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val playbackAction = intent.action.toPlaybackAction()
        when {
            playbackAction != null -> runPendingUpdate {
                val preferences = readPreferences(context)
                MediaSessionAccess.perform(
                    context,
                    playbackAction,
                    preferences.selectedMediaPackage,
                    preferences.selectedMediaSessionId,
                )
                updateAllWidgetsNow(context)
            }
            intent.action == AppWidgetManager.ACTION_APPWIDGET_UPDATE -> {
                val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS) ?: intArrayOf()
                runPendingUpdate {
                    updateWidgetsNow(context, AppWidgetManager.getInstance(context), ids, widgetKind)
                }
            }
            intent.action == AppWidgetManager.ACTION_APPWIDGET_OPTIONS_CHANGED -> {
                val id = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID)
                runPendingUpdate {
                    if (id != AppWidgetManager.INVALID_APPWIDGET_ID) {
                        updateWidgetNow(context, AppWidgetManager.getInstance(context), id, widgetKind)
                    }
                }
            }
            intent.action == AppWidgetManager.ACTION_APPWIDGET_RESTORED -> {
                val ids = intent.getIntArrayExtra(AppWidgetManager.EXTRA_APPWIDGET_IDS) ?: intArrayOf()
                runPendingUpdate {
                    updateWidgetsNow(context, AppWidgetManager.getInstance(context), ids, widgetKind)
                }
            }
            intent.action == Intent.ACTION_CONFIGURATION_CHANGED -> runPendingUpdate {
                updateAllWidgetsNow(context)
            }
            else -> super.onReceive(context, intent)
        }
    }

    private fun runPendingUpdate(block: suspend () -> Unit) {
        val result = goAsync()
        updateScope.launch {
            try {
                updateMutex.withLock { block() }
            } catch (error: Exception) {
                Log.e(TAG, "Unable to update playback widget", error)
            } finally {
                result.finish()
            }
        }
    }

    companion object {
        private const val ACTION_PLAY_PAUSE = "com.simpleplay.app.widget.PLAY_PAUSE"
        private const val ACTION_PREVIOUS = "com.simpleplay.app.widget.PREVIOUS"
        private const val ACTION_NEXT = "com.simpleplay.app.widget.NEXT"
        private const val COMPACT_WIDTH_DP = 300
        private const val TAG = "PlaybackWidget"
        private val updateScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        private val updateMutex = Mutex()

        fun updateAll(context: Context) {
            launchUpdate { updateAllWidgetsNow(context.applicationContext) }
        }

        private fun launchUpdate(block: suspend () -> Unit) {
            updateScope.launch {
                try {
                    updateMutex.withLock { block() }
                } catch (error: Exception) {
                    Log.e(TAG, "Unable to update playback widget", error)
                }
            }
        }

        private suspend fun updateAllWidgetsNow(context: Context) {
            val manager = AppWidgetManager.getInstance(context)
            val controlBar = ComponentName(context, PlaybackWidgetProvider::class.java)
            val playPause = ComponentName(context, PlayPauseWidgetProvider::class.java)
            updateWidgetsNow(
                context,
                manager,
                manager.getAppWidgetIds(controlBar),
                WidgetKind.CONTROL_BAR,
            )
            updateWidgetsNow(
                context,
                manager,
                manager.getAppWidgetIds(playPause),
                WidgetKind.PLAY_PAUSE,
            )
        }

        private suspend fun updateWidgetsNow(
            context: Context,
            manager: AppWidgetManager,
            widgetIds: IntArray,
            kind: WidgetKind,
        ) {
            val preferences = readPreferences(context)
            val session = MediaSessionAccess.chooseSession(
                MediaSessionAccess.sessions(context),
                preferences.selectedMediaPackage,
                preferences.selectedMediaSessionId,
            )
            widgetIds.forEach { renderWidget(context, manager, it, preferences, session, kind) }
        }

        private suspend fun updateWidgetNow(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            kind: WidgetKind,
        ) {
            val preferences = readPreferences(context)
            val session = MediaSessionAccess.chooseSession(
                MediaSessionAccess.sessions(context),
                preferences.selectedMediaPackage,
                preferences.selectedMediaSessionId,
            )
            renderWidget(context, manager, widgetId, preferences, session, kind)
        }

        private fun renderWidget(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            preferences: AppPreferences,
            session: MediaSessionInfo?,
            kind: WidgetKind,
        ) {
            val dark = preferences.widgetIsDark(context)
            if (kind == WidgetKind.PLAY_PAUSE) {
                renderPlayPauseWidget(context, manager, widgetId, preferences, session, dark)
                return
            }
            val remoteViews = RemoteViews(context.packageName, R.layout.widget_playback)
            val options = manager.getAppWidgetOptions(widgetId)
            val compact = options.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH) < COMPACT_WIDTH_DP

            applyBackground(remoteViews, R.id.widget_root, preferences, dark)
            remoteViews.setViewVisibility(R.id.widget_source_icon, if (compact) View.GONE else View.VISIBLE)
            remoteViews.setViewVisibility(R.id.widget_metadata, if (compact) View.GONE else View.VISIBLE)

            val accent = preferences.widgetAccentColor(dark)
            val themedBackground = preferences.widgetBackgroundStyle == WidgetBackgroundStyle.THEME
            val primary = if (themedBackground) contrastColor(accent) else accent
            val secondary = if (themedBackground) {
                ColorUtils.setAlphaComponent(primary, 190)
            } else {
                ColorUtils.setAlphaComponent(accent, 210)
            }
            bindMetadata(context, remoteViews, session, primary, secondary, primary)
            bindControls(
                context = context,
                views = remoteViews,
                session = session,
                standardTint = if (themedBackground) primary else accent,
                accentTint = if (themedBackground) primary else accent,
                buttonSize = preferences.widgetButtonSize,
            )
            remoteViews.setOnClickPendingIntent(R.id.widget_root, openAppIntent(context))
            manager.updateAppWidget(widgetId, remoteViews)
        }

        private fun renderPlayPauseWidget(
            context: Context,
            manager: AppWidgetManager,
            widgetId: Int,
            preferences: AppPreferences,
            session: MediaSessionInfo?,
            dark: Boolean,
        ) {
            val views = RemoteViews(context.packageName, R.layout.widget_play_pause)
            applyBackground(views, R.id.widget_mini_root, preferences, dark)
            val accent = preferences.widgetAccentColor(dark)
            val iconTint = if (preferences.widgetBackgroundStyle == WidgetBackgroundStyle.THEME) {
                contrastColor(accent)
            } else {
                accent
            }
            views.setImageViewResource(
                R.id.widget_mini_play_pause,
                if (session?.isPlaying == true) {
                    R.drawable.ic_widget_pause
                } else {
                    R.drawable.ic_widget_play
                },
            )
            views.setInt(
                R.id.widget_mini_play_pause,
                "setColorFilter",
                iconTint,
            )
            applyButtonSize(
                views,
                intArrayOf(R.id.widget_mini_play_pause),
                preferences.widgetButtonSize,
            )
            bindAction(
                context,
                views,
                R.id.widget_mini_play_pause,
                ACTION_PLAY_PAUSE,
                session?.supports(PlaybackAction.PLAY_PAUSE) == true,
            )
            manager.updateAppWidget(widgetId, views)
        }

        private fun bindMetadata(
            context: Context,
            views: RemoteViews,
            session: MediaSessionInfo?,
            primaryText: Int,
            secondaryText: Int,
            iconTint: Int,
        ) {
            val hasAccess = MediaSessionAccess.hasNotificationAccess(context)
            views.setTextViewText(
                R.id.widget_title,
                when {
                    !hasAccess -> context.getString(R.string.permission_required)
                    session == null -> context.getString(R.string.not_playing)
                    else -> session.title
                },
            )
            views.setTextViewText(R.id.widget_subtitle, session?.artist.orEmpty())
            views.setTextColor(R.id.widget_title, primaryText)
            views.setTextColor(R.id.widget_subtitle, secondaryText)
            if (session?.appIcon != null) {
                views.setImageViewBitmap(R.id.widget_source_icon, session.appIcon)
            } else {
                views.setImageViewResource(R.id.widget_source_icon, R.drawable.ic_music)
            }
            views.setInt(R.id.widget_source_icon, "setColorFilter", iconTint)
        }

        private fun bindControls(
            context: Context,
            views: RemoteViews,
            session: MediaSessionInfo?,
            standardTint: Int,
            accentTint: Int,
            buttonSize: WidgetButtonSize,
        ) {
            views.setImageViewResource(
                R.id.widget_play_pause,
                if (session?.isPlaying == true) {
                    R.drawable.ic_widget_pause
                } else {
                    R.drawable.ic_widget_play
                },
            )
            views.setInt(R.id.widget_previous, "setColorFilter", standardTint)
            views.setInt(R.id.widget_play_pause, "setColorFilter", accentTint)
            views.setInt(R.id.widget_next, "setColorFilter", standardTint)
            applyButtonSize(
                views,
                intArrayOf(R.id.widget_previous, R.id.widget_play_pause, R.id.widget_next),
                buttonSize,
            )

            bindAction(context, views, R.id.widget_previous, ACTION_PREVIOUS, session?.supports(PlaybackAction.PREVIOUS) == true)
            bindAction(context, views, R.id.widget_play_pause, ACTION_PLAY_PAUSE, session?.supports(PlaybackAction.PLAY_PAUSE) == true)
            bindAction(context, views, R.id.widget_next, ACTION_NEXT, session?.supports(PlaybackAction.NEXT) == true)
        }

        private fun bindAction(
            context: Context,
            views: RemoteViews,
            viewId: Int,
            action: String,
            enabled: Boolean,
        ) {
            views.setBoolean(viewId, "setEnabled", enabled)
            views.setInt(viewId, "setImageAlpha", if (enabled) 255 else 70)
            if (enabled) {
                val intent = Intent(context, PlaybackWidgetProvider::class.java).setAction(action)
                val pendingIntent = PendingIntent.getBroadcast(
                    context,
                    action.hashCode(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
                )
                views.setOnClickPendingIntent(viewId, pendingIntent)
            } else {
                views.setOnClickPendingIntent(viewId, null)
            }
        }

        private fun openAppIntent(context: Context): PendingIntent {
            val intent = Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            return PendingIntent.getActivity(
                context,
                1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
            )
        }

        private suspend fun readPreferences(context: Context): AppPreferences =
            AppPreferencesRepository(context.applicationContext).preferences.first()

        private fun String?.toPlaybackAction(): PlaybackAction? = when (this) {
            ACTION_PLAY_PAUSE -> PlaybackAction.PLAY_PAUSE
            ACTION_PREVIOUS -> PlaybackAction.PREVIOUS
            ACTION_NEXT -> PlaybackAction.NEXT
            else -> null
        }

        private fun AppPreferences.widgetIsDark(context: Context): Boolean = when (widgetThemeMode) {
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
            ThemeMode.SYSTEM -> context.resources.configuration.uiMode and
                Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES
        }

        private fun AppPreferences.widgetAccentColor(dark: Boolean): Int = when (widgetAccentTheme) {
            AccentTheme.CORAL -> if (dark) 0xFFFFB4AB.toInt() else 0xFFE45248.toInt()
            AccentTheme.MANGO -> if (dark) 0xFFFFBA3F.toInt() else 0xFFA86F00.toInt()
            AccentTheme.MINT -> if (dark) 0xFF70DBC1.toInt() else 0xFF007D68.toInt()
            AccentTheme.SKY -> if (dark) 0xFFA9C7FF.toInt() else 0xFF2367C8.toInt()
            AccentTheme.PURPLE -> if (dark) 0xFFD2B7FF.toInt() else 0xFF7146C7.toInt()
            AccentTheme.BERRY -> if (dark) 0xFFFFB0C8.toInt() else 0xFFC52B69.toInt()
            AccentTheme.CUSTOM -> if (dark) {
                ColorUtils.blendARGB(widgetCustomAccentArgb, Color.WHITE, 0.38f)
            } else {
                widgetCustomAccentArgb
            }
        }

        private fun applyBackground(
            views: RemoteViews,
            rootId: Int,
            preferences: AppPreferences,
            dark: Boolean,
        ) {
            if (preferences.widgetBackgroundStyle == WidgetBackgroundStyle.TRANSPARENT) {
                views.setInt(rootId, "setBackgroundResource", android.R.color.transparent)
                return
            }
            views.setInt(
                rootId,
                "setBackgroundResource",
                if (dark) R.drawable.widget_background_dark else R.drawable.widget_background_light,
            )
            views.setColorStateList(
                rootId,
                "setBackgroundTintList",
                ColorStateList.valueOf(preferences.widgetAccentColor(dark)),
            )
        }

        private fun applyButtonSize(
            views: RemoteViews,
            viewIds: IntArray,
            size: WidgetButtonSize,
        ) {
            val dimensionDp = when (size) {
                WidgetButtonSize.SMALL -> 40f
                WidgetButtonSize.MEDIUM -> 52f
                WidgetButtonSize.LARGE -> 64f
            }
            viewIds.forEach { viewId ->
                views.setViewLayoutWidth(viewId, dimensionDp, TypedValue.COMPLEX_UNIT_DIP)
                views.setViewLayoutHeight(viewId, dimensionDp, TypedValue.COMPLEX_UNIT_DIP)
                views.setViewPadding(viewId, 0, 0, 0, 0)
            }
        }

        private fun contrastColor(background: Int): Int =
            if (ColorUtils.calculateLuminance(background) > 0.42) {
                Color.rgb(33, 26, 30)
            } else {
                Color.WHITE
            }
    }
}

class PlayPauseWidgetProvider : PlaybackWidgetProvider()
