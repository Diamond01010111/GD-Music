package com.diamond.gdapplication.ui.components

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** Keeps short UI labels on one line and only reduces their size when space is tight. */
@Composable
fun AutoSizeSingleLineText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    color: Color = LocalContentColor.current,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign? = null,
    minFontSize: TextUnit = 11.sp,
    maxFontSize: TextUnit = 16.sp
) {
    val textMeasurer = rememberTextMeasurer()

    BoxWithConstraints(modifier = modifier) {
        val maximumStyle = style.copy(
            fontSize = maxFontSize,
            fontWeight = fontWeight ?: style.fontWeight
        )
        val measuredWidth = textMeasurer.measure(
            text = text,
            style = maximumStyle,
            maxLines = 1,
            softWrap = false
        ).size.width
        val availableWidth = constraints.maxWidth.coerceAtLeast(1)
        val scale = if (measuredWidth > availableWidth) {
            availableWidth.toFloat() / measuredWidth
        } else {
            1f
        }
        val fittedFontSize = (maxFontSize.value * scale)
            .coerceIn(minFontSize.value, maxFontSize.value)
            .sp

        Text(
            text = text,
            color = color,
            style = maximumStyle,
            fontSize = fittedFontSize,
            maxLines = 1,
            softWrap = false,
            textAlign = textAlign,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
