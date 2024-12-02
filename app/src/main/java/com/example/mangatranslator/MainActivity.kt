package com.example.mangatranslator
import android.content.Intent
import com.example.mangatranslator.ui.components.SettingsPage


import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.mangatranslator.ui.components.FloatingWidgetService



class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // enableEdgeToEdge()
        setContent {
            SettingsPage(context = this)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, FloatingWidgetService::class.java))
    }
}



