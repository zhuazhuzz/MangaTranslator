package com.example.mangatranslator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.TranslatorOptions

@Composable
fun TranslatedPopUp(blocks: List<TextBlockData>) {

    Box(
        modifier = Modifier
            .background(Color.Black.copy(alpha = 0.13f))
            .fillMaxSize()
            .border(2.dp, Color.Magenta)
    ) {
        blocks.forEach { block ->
            block.boundingBox?.let { rect ->
                val offsetX = PixelsToDp(rect.left)
                val offsetY = PixelsToDp(rect.top)
                val sizeWidth = PixelsToDp(rect.right- rect.left)
                val sizeHeight = PixelsToDp((rect.bottom - rect.top))
                Box(
                    modifier = Modifier
                        .offset(x = offsetX, y = offsetY)
                        .size(width = sizeWidth + 22.dp, height = sizeHeight + 10.dp)
                        .background(Color.White)
                ) {
                    val options = TranslatorOptions.Builder()
                        .setSourceLanguage(TranslateLanguage.JAPANESE)
                        .setTargetLanguage(TranslateLanguage.ENGLISH)
                        .build()
                    val translator = Translation.getClient(options)

                    var translated by remember { mutableStateOf("") }

                    translator.downloadModelIfNeeded()
                        .addOnSuccessListener {
                            translator.translate(block.text)
                                .addOnSuccessListener { translatedText ->
                                    translated = translatedText
                                }
                        }

                    Text(
                        text = translated, // block.text
                        color = Color.Black,
                        modifier = Modifier  // add padding?
                    )
                }
            }
        }

    }

}


@Composable
fun Int.toDp(): Dp {
    val density = LocalDensity.current.density
    return (this / density).dp
}

@Composable
fun PixelsToDp(pixels: Int): Dp {
    val density = LocalDensity.current
    return with(density) { pixels.toDp() }
}
