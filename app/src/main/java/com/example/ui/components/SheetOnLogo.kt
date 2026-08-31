package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R

/**
 * SheetOn Dark Yellow Brand Name & Icon Component.
 * Displays the app title "SheetOn" with the logo icon.
 */
val DarkYellowBrandColor = Color(0xFFD97706) // زرد تیره / کهربایی تیره جذاب و خوانا

@Composable
fun SheetOnLogo(
    modifier: Modifier = Modifier,
    fontSize: TextUnit = 21.sp,
    size: Dp? = null,
    showIcon: Boolean = true
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        if (showIcon) {
            val iconSize = size ?: if (fontSize >= 24.sp) 30.dp else 26.dp
            Image(
                painter = painterResource(id = R.drawable.sheeton_logo_icon),
                contentDescription = "SheetOn Logo",
                modifier = Modifier
                    .size(iconSize)
                    .clip(RoundedCornerShape(8.dp))
            )
        }
        Text(
            text = "SheetOn",
            color = DarkYellowBrandColor,
            fontSize = fontSize,
            fontWeight = FontWeight.Black,
            letterSpacing = 0.5.sp
        )
    }
}



