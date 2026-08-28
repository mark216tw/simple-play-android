package com.simpleplay.app.ui

import android.content.ComponentName
import android.content.Intent
import android.graphics.Color as AndroidColor
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.simpleplay.app.MainViewModel
import com.simpleplay.app.R
import com.simpleplay.app.data.AccentTheme
import com.simpleplay.app.data.ThemeMode
import com.simpleplay.app.data.WidgetBackgroundStyle
import com.simpleplay.app.data.WidgetButtonSize
import com.simpleplay.app.media.MediaNotificationListenerService
import com.simpleplay.app.media.MediaSessionInfo
import com.simpleplay.app.media.PlaybackAction
import com.simpleplay.app.ui.theme.accentPalettes
import kotlinx.coroutines.delay

@Composable
fun SimplePlayApp(viewModel: MainViewModel) {
    val playerState by viewModel.playerState.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    var showSettings by rememberSaveable { mutableStateOf(false) }

    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) {
        viewModel.start()
    }
    BackHandler(enabled = showSettings) { showSettings = false }

    if (showSettings) {
        SettingsScreen(
            themeMode = preferences.themeMode,
            accentTheme = preferences.accentTheme,
            customAccentArgb = preferences.customAccentArgb,
            widgetButtonSize = preferences.widgetButtonSize,
            widgetThemeMode = preferences.widgetThemeMode,
            widgetAccentTheme = preferences.widgetAccentTheme,
            widgetCustomAccentArgb = preferences.widgetCustomAccentArgb,
            widgetBackgroundStyle = preferences.widgetBackgroundStyle,
            onThemeModeSelected = viewModel::setThemeMode,
            onAccentSelected = viewModel::setAccentTheme,
            onCustomAccentChanged = viewModel::setCustomAccent,
            onWidgetButtonSizeSelected = viewModel::setWidgetButtonSize,
            onWidgetThemeModeSelected = viewModel::setWidgetThemeMode,
            onWidgetAccentSelected = viewModel::setWidgetAccentTheme,
            onWidgetCustomAccentChanged = viewModel::setWidgetCustomAccent,
            onWidgetBackgroundStyleSelected = viewModel::setWidgetBackgroundStyle,
            onBack = { showSettings = false },
        )
    } else {
        PlayerScreen(
            hasNotificationAccess = playerState.hasNotificationAccess,
            sessions = playerState.sessions,
            selectedSession = playerState.selectedSession,
            onOpenSettings = { showSettings = true },
            onSelectSession = { session ->
                viewModel.selectSession(session.sessionId, session.packageName)
            },
            onPlaybackAction = viewModel::perform,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerScreen(
    hasNotificationAccess: Boolean,
    sessions: List<MediaSessionInfo>,
    selectedSession: MediaSessionInfo?,
    onOpenSettings: () -> Unit,
    onSelectSession: (MediaSessionInfo) -> Unit,
    onPlaybackAction: (PlaybackAction) -> Unit,
) {
    val context = LocalContext.current
    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "簡單播放",
                        fontWeight = FontWeight.ExtraBold,
                    )
                },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            painter = painterResource(R.drawable.ic_settings),
                            contentDescription = "設定",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        when {
            !hasNotificationAccess -> PermissionContent(
                modifier = Modifier.padding(padding),
                onGrantAccess = {
                    val component = ComponentName(context, MediaNotificationListenerService::class.java)
                    val detailIntent = Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                        .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, component.flattenToString())
                    runCatching { context.startActivity(detailIntent) }
                        .onFailure {
                            context.startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                        }
                },
            )
            selectedSession == null -> EmptyPlayerContent(modifier = Modifier.padding(padding))
            else -> LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .navigationBarsPadding(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(20.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp),
            ) {
                item { NowPlayingCard(selectedSession) }
                item {
                    PlaybackControls(
                        session = selectedSession,
                        onPlaybackAction = onPlaybackAction,
                    )
                }
                if (sessions.size > 1) {
                    item {
                        Text(
                            text = "選擇播放器",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    item {
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                            items(sessions, key = { it.sessionId }) { session ->
                                PlayerSourceChip(
                                    session = session,
                                    selected = session.sessionId == selectedSession.sessionId,
                                    onClick = { onSelectSession(session) },
                                )
                            }
                        }
                    }
                }
                item {
                    Text(
                        text = "可用按鈕由目前的播放 App 決定。若按鈕呈灰色，代表該播放器未提供這項操作。",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                    )
                }
            }
        }
    }
}

@Composable
private fun PermissionContent(
    modifier: Modifier,
    onGrantAccess: () -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(24.dp),
        contentAlignment = Alignment.Center,
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(28.dp),
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(76.dp)
                        .background(MaterialTheme.colorScheme.primary, CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        painter = painterResource(R.drawable.ic_music),
                        contentDescription = null,
                        modifier = Modifier.size(38.dp),
                        tint = MaterialTheme.colorScheme.onPrimary,
                    )
                }
                Text(
                    text = "連結目前的播放器",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    textAlign = TextAlign.Center,
                )
                Text(
                    text = "簡單播放需要通知存取權，才能讀取媒體標題並操作其他播放 App。資料只留在裝置上。",
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onGrantAccess) {
                    Text("開啟通知存取設定", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun EmptyPlayerContent(modifier: Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .navigationBarsPadding()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_music),
            contentDescription = null,
            modifier = Modifier.size(72.dp),
            tint = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(20.dp))
        Text(
            text = "尚未播放",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.ExtraBold,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = "請先開啟音樂或 Podcast App，開始播放後就能在這裡控制。",
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun NowPlayingCard(session: MediaSessionInfo) {
    var displayedArtwork by remember(session.packageName) { mutableStateOf(session.artwork) }
    LaunchedEffect(session.packageName, session.title, session.artwork) {
        if (session.artwork != null) {
            displayedArtwork = session.artwork
        } else {
            // Media apps commonly clear artwork briefly before publishing the next bitmap.
            delay(500)
            displayedArtwork = null
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(30.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(22.dp))
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                val artwork = displayedArtwork
                if (artwork != null) {
                    Image(
                        bitmap = artwork.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                    )
                } else {
                    if (session.appIcon != null) {
                        Image(
                            bitmap = session.appIcon.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(92.dp),
                        )
                    } else {
                        Icon(
                            painter = painterResource(R.drawable.ic_music),
                            contentDescription = null,
                            modifier = Modifier.size(72.dp),
                            tint = MaterialTheme.colorScheme.primary,
                        )
                    }
                }
            }
            Spacer(Modifier.height(18.dp))
            Text(
                text = session.title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = session.artist,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(12.dp))
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                shape = CircleShape,
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (session.appIcon != null) {
                        Image(
                            bitmap = session.appIcon.asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                        )
                        Spacer(Modifier.width(7.dp))
                    }
                    Text(session.appName, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

@Composable
private fun PlaybackControls(
    session: MediaSessionInfo,
    onPlaybackAction: (PlaybackAction) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        ControlButton(
            icon = R.drawable.ic_previous,
            description = "上一首",
            enabled = session.supports(PlaybackAction.PREVIOUS),
            onClick = { onPlaybackAction(PlaybackAction.PREVIOUS) },
        )
        FilledIconButton(
            onClick = { onPlaybackAction(PlaybackAction.PLAY_PAUSE) },
            enabled = session.supports(PlaybackAction.PLAY_PAUSE),
            modifier = Modifier.size(70.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(
                painter = painterResource(if (session.isPlaying) R.drawable.ic_pause else R.drawable.ic_play),
                contentDescription = if (session.isPlaying) "暫停" else "播放",
                modifier = Modifier.size(34.dp),
            )
        }
        ControlButton(
            icon = R.drawable.ic_stop,
            description = "停止",
            enabled = session.supports(PlaybackAction.STOP),
            onClick = { onPlaybackAction(PlaybackAction.STOP) },
        )
        ControlButton(
            icon = R.drawable.ic_next,
            description = "下一首",
            enabled = session.supports(PlaybackAction.NEXT),
            onClick = { onPlaybackAction(PlaybackAction.NEXT) },
        )
    }
}

@Composable
private fun ControlButton(
    icon: Int,
    description: String,
    enabled: Boolean,
    onClick: () -> Unit,
) {
    FilledTonalButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.size(52.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(13.dp),
        shape = CircleShape,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = description,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun PlayerSourceChip(
    session: MediaSessionInfo,
    selected: Boolean,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier.clickable(onClick = onClick),
        shape = CircleShape,
        color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (session.appIcon != null) {
                Image(
                    bitmap = session.appIcon.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                text = session.appName,
                fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
            )
            if (selected) {
                Spacer(Modifier.width(7.dp))
                Icon(
                    painter = painterResource(R.drawable.ic_check),
                    contentDescription = "已選擇",
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsScreen(
    themeMode: ThemeMode,
    accentTheme: AccentTheme,
    customAccentArgb: Int,
    widgetButtonSize: WidgetButtonSize,
    widgetThemeMode: ThemeMode,
    widgetAccentTheme: AccentTheme,
    widgetCustomAccentArgb: Int,
    widgetBackgroundStyle: WidgetBackgroundStyle,
    onThemeModeSelected: (ThemeMode) -> Unit,
    onAccentSelected: (AccentTheme) -> Unit,
    onCustomAccentChanged: (Int) -> Unit,
    onWidgetButtonSizeSelected: (WidgetButtonSize) -> Unit,
    onWidgetThemeModeSelected: (ThemeMode) -> Unit,
    onWidgetAccentSelected: (AccentTheme) -> Unit,
    onWidgetCustomAccentChanged: (Int) -> Unit,
    onWidgetBackgroundStyleSelected: (WidgetBackgroundStyle) -> Unit,
    onBack: () -> Unit,
) {
    Scaffold(
        contentWindowInsets = WindowInsets.statusBars,
        topBar = {
            TopAppBar(
                title = { Text("外觀設定", fontWeight = FontWeight.ExtraBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            painter = painterResource(R.drawable.ic_arrow_back),
                            contentDescription = "返回",
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                ),
            )
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = "顯示模式",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            ThemeMode.entries.forEach { mode ->
                SelectionRow(
                    title = when (mode) {
                        ThemeMode.SYSTEM -> "跟隨系統"
                        ThemeMode.LIGHT -> "淺色模式"
                        ThemeMode.DARK -> "深色模式"
                    },
                    selected = mode == themeMode,
                    onClick = { onThemeModeSelected(mode) },
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            Text(
                text = "主題色彩",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "點擊後會立即套用",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            accentPalettes.forEach { palette ->
                SelectionRow(
                    title = palette.displayName,
                    selected = palette.theme == accentTheme,
                    swatchColor = palette.lightPrimary,
                    onClick = { onAccentSelected(palette.theme) },
                )
            }
            SelectionRow(
                title = "自訂顏色",
                selected = accentTheme == AccentTheme.CUSTOM,
                swatchColor = Color(customAccentArgb),
                onClick = { onAccentSelected(AccentTheme.CUSTOM) },
            )
            if (accentTheme == AccentTheme.CUSTOM) {
                CustomColorEditor(
                    color = customAccentArgb,
                    onColorChange = onCustomAccentChanged,
                )
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp))
            Text(
                text = "小工具外觀",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = "按鈕大小",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            WidgetButtonSize.entries.forEach { size ->
                SelectionRow(
                    title = when (size) {
                        WidgetButtonSize.SMALL -> "小"
                        WidgetButtonSize.MEDIUM -> "中"
                        WidgetButtonSize.LARGE -> "大"
                    },
                    selected = widgetButtonSize == size,
                    onClick = { onWidgetButtonSizeSelected(size) },
                )
            }

            Text(
                text = "顯示模式",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            ThemeMode.entries.forEach { mode ->
                SelectionRow(
                    title = when (mode) {
                        ThemeMode.SYSTEM -> "跟隨系統"
                        ThemeMode.LIGHT -> "淺色"
                        ThemeMode.DARK -> "深色"
                    },
                    selected = widgetThemeMode == mode,
                    onClick = { onWidgetThemeModeSelected(mode) },
                )
            }

            Text(
                text = "背景樣式",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            WidgetBackgroundStyle.entries.forEach { style ->
                SelectionRow(
                    title = when (style) {
                        WidgetBackgroundStyle.THEME -> "主題色背景"
                        WidgetBackgroundStyle.TRANSPARENT -> "透明背景"
                    },
                    selected = widgetBackgroundStyle == style,
                    onClick = { onWidgetBackgroundStyleSelected(style) },
                )
            }

            Text(
                text = "主題色彩",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 8.dp),
            )
            accentPalettes.forEach { palette ->
                SelectionRow(
                    title = palette.displayName,
                    selected = palette.theme == widgetAccentTheme,
                    swatchColor = palette.lightPrimary,
                    onClick = { onWidgetAccentSelected(palette.theme) },
                )
            }
            SelectionRow(
                title = "自訂顏色",
                selected = widgetAccentTheme == AccentTheme.CUSTOM,
                swatchColor = Color(widgetCustomAccentArgb),
                onClick = { onWidgetAccentSelected(AccentTheme.CUSTOM) },
            )
            if (widgetAccentTheme == AccentTheme.CUSTOM) {
                CustomColorEditor(
                    color = widgetCustomAccentArgb,
                    onColorChange = onWidgetCustomAccentChanged,
                )
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun CustomColorEditor(
    color: Int,
    onColorChange: (Int) -> Unit,
) {
    val red = AndroidColor.red(color)
    val green = AndroidColor.green(color)
    val blue = AndroidColor.blue(color)
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        ),
        shape = RoundedCornerShape(22.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .background(Color(color), CircleShape)
                        .border(2.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape),
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("自訂主題色", fontWeight = FontWeight.Bold)
                    Text(
                        text = "#%06X".format(color and 0xFFFFFF),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            ColorChannelSlider("紅", red) { value ->
                onColorChange(AndroidColor.rgb(value, green, blue))
            }
            ColorChannelSlider("綠", green) { value ->
                onColorChange(AndroidColor.rgb(red, value, blue))
            }
            ColorChannelSlider("藍", blue) { value ->
                onColorChange(AndroidColor.rgb(red, green, value))
            }
        }
    }
}

@Composable
private fun ColorChannelSlider(
    label: String,
    value: Int,
    onValueChange: (Int) -> Unit,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = label,
            modifier = Modifier.width(28.dp),
            fontWeight = FontWeight.Bold,
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt().coerceIn(0, 255)) },
            modifier = Modifier.weight(1f),
            valueRange = 0f..255f,
        )
        Text(
            text = value.toString(),
            modifier = Modifier.width(36.dp),
            textAlign = TextAlign.End,
            style = MaterialTheme.typography.labelMedium,
        )
    }
}

@Composable
private fun SelectionRow(
    title: String,
    selected: Boolean,
    swatchColor: Color? = null,
    onClick: () -> Unit,
) {
    val shape = RoundedCornerShape(18.dp)
    val background = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceContainer
    val content = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .then(
                if (selected) Modifier else Modifier.border(
                    1.dp,
                    MaterialTheme.colorScheme.outlineVariant,
                    shape,
                ),
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (swatchColor != null) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .background(swatchColor, CircleShape)
                    .border(1.dp, content.copy(alpha = 0.25f), CircleShape),
            )
            Spacer(Modifier.width(12.dp))
        }
        Text(
            text = title,
            modifier = Modifier.weight(1f),
            color = content,
            fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Medium,
        )
        if (selected) {
            Icon(
                painter = painterResource(R.drawable.ic_check),
                contentDescription = "已選擇",
                modifier = Modifier.size(22.dp),
                tint = content,
            )
        }
    }
}
