package com.kherio.nax

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext

val OnlineColor = Color(0xFF2E9E5B)
val OfflineColor = Color(0xFF8A939B)
val DownColor = Color(0xFFD9534F)
val NewColor = Color(0xFFF0A030)

@Composable
fun NaxTheme(content: @Composable () -> Unit) {
    val dark = isSystemInDarkTheme()
    val ctx = LocalContext.current
    val scheme = when {
        Build.VERSION.SDK_INT >= 31 -> if (dark) dynamicDarkColorScheme(ctx) else dynamicLightColorScheme(ctx)
        dark -> darkColorScheme(primary = Color(0xFF4DD0E1))
        else -> lightColorScheme(primary = Color(0xFF00838F))
    }
    MaterialTheme(colorScheme = scheme, content = content)
}
