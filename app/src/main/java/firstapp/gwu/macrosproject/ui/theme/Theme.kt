package firstapp.gwu.macrosproject.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.material3.*
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import firstapp.gwu.macrosproject.R

private val BytesFontFamily = FontFamily(
    Font(R.font.poppins_regular, weight = FontWeight.Normal),
    Font(R.font.poppins_semibold, weight = FontWeight.SemiBold),
)

private val LightColors = lightColorScheme(
    primary = BytesPurple,
    secondary = BytesMint,
    tertiary = BytesPink,
    background = BytesBackground,
    surface = BytesSurface,
    onBackground = BytesNavy,
    onSurface = BytesNavy,
)

private val BytesTypography = Typography(
    titleLarge = Typography().titleLarge.copy(
        fontFamily = BytesFontFamily,
        fontWeight = FontWeight.SemiBold
    ),
    bodyMedium = Typography().bodyMedium.copy(
        fontFamily = BytesFontFamily
    ),
    bodySmall = Typography().bodySmall.copy(
        fontFamily = BytesFontFamily
    ),
    labelSmall = Typography().labelSmall.copy(
        fontFamily = BytesFontFamily
    )
)

@Composable
fun MacrosProjectTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = BytesTypography,
        shapes = MaterialTheme.shapes,
        content = content
    )
}