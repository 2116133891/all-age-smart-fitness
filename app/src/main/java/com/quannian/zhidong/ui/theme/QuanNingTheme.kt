package com.quannian.zhidong.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/**
 * Compose Material3 主题包装。浅色科技风为主视觉。
 */
@Composable
fun QuanNingTheme(content: @Composable () -> Unit) {
    val scheme = lightColorScheme(
        primary = Palette.accent,
        onPrimary = Color.White,
        background = Palette.bg,
        surface = Palette.card,
        onSurface = Palette.ink,
    )
    MaterialTheme(colorScheme = scheme, content = content)
}
