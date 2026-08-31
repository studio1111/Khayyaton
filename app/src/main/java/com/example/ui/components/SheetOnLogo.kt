package com.example.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/**
 * SheetOn Brand Typography Component.
 * Displays only the stylish brand title "SheetOn" with warm dark yellow color.
 */
val DarkYellowBrandColor = Color(0xFFD97706)

@Composable
fun SheetOnLogo(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 21.sp
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
