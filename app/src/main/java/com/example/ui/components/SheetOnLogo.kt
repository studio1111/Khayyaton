package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/**
 * SheetOn Dark Yellow Brand Name Component.
 * Displays only the app title "SheetOn" with a rich dark yellow typography.
 */
val DarkYellowBrandColor = Color(0xFFD97706) // زرد تیره / کهربایی تیره جذاب و خوانا

@Composable
fun SheetOnLogo(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 21.sp,
    size: Dp? = null
) {
    Text(
        text = "SheetOn",
        color = DarkYellowBrandColor,
        fontSize = fontSize,
        fontWeight = FontWeight.Black,
        letterSpacing = 0.5.sp,
        modifier = modifier
    )
}


