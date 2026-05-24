package me.lunarveil.ui.theme
import android.os.Build; import androidx.compose.foundation.isSystemInDarkTheme; import androidx.compose.material3.*; import androidx.compose.runtime.Composable; import androidx.compose.ui.graphics.Color; import androidx.compose.ui.platform.LocalContext
@Composable fun LunarVeilTheme(dark: Boolean = isSystemInDarkTheme(), dynamic: Boolean = true, content: @Composable () -> Unit) {
    val cs = when { dynamic && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> { val c = LocalContext.current; if (dark) dynamicDarkColorScheme(c) else dynamicLightColorScheme(c) } dark -> darkColorScheme(primary = Color(0xFFD0BCFF), secondary = Color(0xFFCCC2DC), tertiary = Color(0xFFEFB8C8), background = Color(0xFF1C1B1F), surface = Color(0xFF1C1B1F)) else -> lightColorScheme(primary = Color(0xFF6750A4), secondary = Color(0xFF625B71), tertiary = Color(0xFF7D5260), background = Color(0xFFFFFBFE), surface = Color(0xFFFFFBFE)) }
    MaterialTheme(colorScheme = cs, content = content)
}
