package com.simpleplay.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.core.graphics.ColorUtils
import com.simpleplay.app.data.AccentTheme

data class AccentPalette(
    val theme: AccentTheme,
    val displayName: String,
    val lightPrimary: Color,
    val lightOnPrimary: Color,
    val lightContainer: Color,
    val darkPrimary: Color,
    val darkOnPrimary: Color,
    val darkContainer: Color,
)

val accentPalettes = listOf(
    AccentPalette(AccentTheme.CORAL, "珊瑚橘", Color(0xFFE45248), Color(0xFF350603), Color(0xFFFFDAD5), Color(0xFFFFB4AB), Color(0xFF5F120D), Color(0xFF8D2721)),
    AccentPalette(AccentTheme.MANGO, "向日黃", Color(0xFFA86F00), Color.White, Color(0xFFFFDDA2), Color(0xFFFFBA3F), Color(0xFF442B00), Color(0xFF624000)),
    AccentPalette(AccentTheme.MINT, "湖水綠", Color(0xFF007D68), Color.White, Color(0xFF8FF8DC), Color(0xFF70DBC1), Color(0xFF00382E), Color(0xFF005143)),
    AccentPalette(AccentTheme.SKY, "天空藍", Color(0xFF2367C8), Color.White, Color(0xFFD7E3FF), Color(0xFFA9C7FF), Color(0xFF003062), Color(0xFF174A91)),
    AccentPalette(AccentTheme.PURPLE, "葡萄紫", Color(0xFF7146C7), Color.White, Color(0xFFEBDCFF), Color(0xFFD2B7FF), Color(0xFF3E147A), Color(0xFF57309A)),
    AccentPalette(AccentTheme.BERRY, "莓果粉", Color(0xFFC52B69), Color.White, Color(0xFFFFD9E3), Color(0xFFFFB0C8), Color(0xFF63002F), Color(0xFF8D1247)),
)

fun paletteFor(theme: AccentTheme, customAccentArgb: Int): AccentPalette {
    if (theme != AccentTheme.CUSTOM) return accentPalettes.first { it.theme == theme }

    val lightPrimary = customAccentArgb or 0xFF000000.toInt()
    val darkPrimary = ColorUtils.blendARGB(lightPrimary, android.graphics.Color.WHITE, 0.38f)
    return AccentPalette(
        theme = AccentTheme.CUSTOM,
        displayName = "自訂顏色",
        lightPrimary = Color(lightPrimary),
        lightOnPrimary = Color(contrastColor(lightPrimary)),
        lightContainer = Color(ColorUtils.blendARGB(lightPrimary, android.graphics.Color.WHITE, 0.78f)),
        darkPrimary = Color(darkPrimary),
        darkOnPrimary = Color(contrastColor(darkPrimary)),
        darkContainer = Color(ColorUtils.blendARGB(lightPrimary, android.graphics.Color.BLACK, 0.42f)),
    )
}

private fun contrastColor(background: Int): Int =
    if (ColorUtils.calculateLuminance(background) > 0.42) 0xFF211A1E.toInt()
    else android.graphics.Color.WHITE

@Composable
fun SimplePlayTheme(
    accentTheme: AccentTheme,
    customAccentArgb: Int,
    darkTheme: Boolean,
    content: @Composable () -> Unit,
) {
    val accent = paletteFor(accentTheme, customAccentArgb)
    val colors = if (darkTheme) {
        darkColorScheme(
            primary = accent.darkPrimary,
            onPrimary = accent.darkOnPrimary,
            primaryContainer = accent.darkContainer,
            onPrimaryContainer = Color(0xFFFFDAD5),
            background = Color(0xFF17151A),
            onBackground = Color(0xFFEAE0E7),
            surface = Color(0xFF17151A),
            onSurface = Color(0xFFEAE0E7),
            surfaceContainer = Color(0xFF242127),
            surfaceContainerHigh = Color(0xFF302D33),
        )
    } else {
        lightColorScheme(
            primary = accent.lightPrimary,
            onPrimary = accent.lightOnPrimary,
            primaryContainer = accent.lightContainer,
            onPrimaryContainer = Color(0xFF31111A),
            background = Color(0xFFFFF8FA),
            onBackground = Color(0xFF211A1E),
            surface = Color(0xFFFFF8FA),
            onSurface = Color(0xFF211A1E),
            surfaceContainer = Color(0xFFF5EDF1),
            surfaceContainerHigh = Color(0xFFEFE7EB),
        )
    }

    MaterialTheme(
        colorScheme = colors,
        typography = androidx.compose.material3.Typography(),
        content = content,
    )
}
