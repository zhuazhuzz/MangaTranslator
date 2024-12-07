package com.example.mangatranslator.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp


@Composable
fun TranslatedPopUp() {

    Surface(
        modifier = Modifier
        .wrapContentSize()
        .background(androidx.compose.ui.graphics.Color.LightGray)
        .padding(16.dp)
    ) {

        Text("hi you popped up this component")

    }


}