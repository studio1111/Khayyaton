package com.example.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * خیاطان Brand Typography Component.
 * Displays the artistic brand title "خیاطان" with dark yellow-orange color.
 */
val DarkYellowBrandColor = Color(0xFFD97706)

@Composable
fun KhayyatonLogo(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 24.sp
) {
    Text(
        text = "خیاطان",
        color = DarkYellowBrandColor,
        fontSize = fontSize,
        fontWeight = FontWeight.Black,
        fontFamily = FontFamily.Serif,
        style = TextStyle(
            shadow = Shadow(
                color = DarkYellowBrandColor.copy(alpha = 0.28f),
                offset = Offset(0f, 2f),
                blurRadius = 6f
            )
        ),
        modifier = modifier
    )
}
