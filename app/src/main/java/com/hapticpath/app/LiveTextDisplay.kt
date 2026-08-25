package com.hapticpath.app

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LiveTextDisplay(
    words: List<AnnotatedWord>,
    isRecording: Boolean,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Box(
        modifier = modifier
            .padding(12.dp)
            .verticalScroll(scrollState)
    ) {
        if (words.isEmpty()) {
            Text(
                text = if (isRecording) "Tala nu..." else "Tryck på Starta Session för att påbörja...",
                color = Color.Gray,
                fontSize = 14.sp
            )
        } else {
            val annotatedText = buildAnnotatedString {
                words.forEachIndexed { index, annotatedWord ->
                    if (annotatedWord.isError) {
                        withStyle(
                            style = SpanStyle(
                                color = Color.White,
                                background = Color(0xFFFF3333),
                                fontWeight = FontWeight.Bold
                            )
                        ) {
                            append(annotatedWord.text)
                        }
                    } else {
                        withStyle(style = SpanStyle(color = Color.White)) {
                            append(annotatedWord.text)
                        }
                    }

                    if (index < words.size - 1) {
                        append(" ")
                    }
                }
            }

            Text(
                text = annotatedText,
                fontSize = 16.sp,
                lineHeight = 22.sp
            )
        }
    }
}