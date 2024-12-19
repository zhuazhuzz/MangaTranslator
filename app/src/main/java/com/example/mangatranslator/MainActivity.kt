package com.example.mangatranslator

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.projection.MediaProjectionManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import com.example.mangatranslator.ui.components.FloatingWidgetService

class MainActivity : ComponentActivity() {
    private lateinit var screenCaptureLauncher: ActivityResultLauncher<Intent>
    private var enableWidget by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Manual Manage of window fitting
        //WindowCompat.setDecorFitsSystemWindows(window, false)

        // Ask for Notification Permissions
        val permissions = mutableListOf<String>()
        permissions.add(Manifest.permission.POST_NOTIFICATIONS) // API 33 (Android 13)
        permissions.add(Manifest.permission.SYSTEM_ALERT_WINDOW)
        permissions.add(Manifest.permission.FOREGROUND_SERVICE)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_MEDIA_PROJECTION) // API 34 (Android 14 required)
        }
        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), 0)
        }

        val mediaProjectionManager = getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
        screenCaptureLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK && result.data != null) {
                val intent = Intent(this, FloatingWidgetService::class.java).apply {
                    putExtra("RESULT_CODE", result.resultCode)
                    putExtra("DATA", result.data)
                    action = FloatingWidgetService.Actions.START.toString()
                }
                startService(intent) // Start MediaProjection & Widget service
            } else { // If Denied
                enableWidget = false
                Toast.makeText(this, "Permission denied!", Toast.LENGTH_SHORT).show()
            }
        }

        // Main Page Settings
        setContent {
            var showInputDropdown by remember { mutableStateOf(false) }
            var showOutputDropdown by remember { mutableStateOf(false) }
            var selectedInputLanguage by remember { mutableStateOf("Japanese") }
            var selectedOutputLanguage by remember { mutableStateOf("English") }

            val inputLanguages = listOf("Chinese", "Korean", "Japanese", "Vietnamese")
            val outputLanguages = listOf("English")

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.background
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "Start Overlay Widget",
                            fontSize = 24.sp
                        )
                        Spacer(modifier = Modifier.padding(start = 8.dp))
                        Switch(
                            checked = enableWidget,
                            onCheckedChange = { isChecked ->
                                enableWidget = isChecked
                                if (isChecked) {
                                    // Check if the overlay permission is granted
                                    if (!canDrawOverlays(applicationContext)) {
                                        requestOverlayPermission(applicationContext)
                                    } else { // Start Services
                                        val projectionIntent = mediaProjectionManager.createScreenCaptureIntent()
                                        screenCaptureLauncher.launch(projectionIntent)
                                    }
                                } else { // Stop services
                                    Intent(applicationContext, FloatingWidgetService::class.java).also {
                                        it.action = FloatingWidgetService.Actions.STOP.toString()
                                        startService(it)
                                    }
                                }
                            }
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    // Input Language Dropdown
                    DropdownSelector(
                        label = "Input Language",
                        options = inputLanguages,
                        selectedOption = selectedInputLanguage,
                        onOptionSelected = { selectedInputLanguage = it },
                        isDropdownVisible = showInputDropdown,
                        onDropdownVisibilityChange = { showInputDropdown = it }
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Output Language Dropdown
                    DropdownSelector(
                        label = "Output Language",
                        options = outputLanguages,
                        selectedOption = selectedOutputLanguage,
                        onOptionSelected = { selectedOutputLanguage = it },
                        isDropdownVisible = showOutputDropdown,
                        onDropdownVisibilityChange = { showOutputDropdown = it }
                    )
                }
            }

        }
    }


    @Composable // Dropdown component ========
    fun DropdownSelector(
        label: String,
        options: List<String>,
        selectedOption: String,
        onOptionSelected: (String) -> Unit,
        isDropdownVisible: Boolean,
        onDropdownVisibilityChange: (Boolean) -> Unit
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        )
        {
            Text(text = label, fontSize = 20.sp)
            Box {
                Button(onClick = { onDropdownVisibilityChange(true) }) {
                    Text(text = selectedOption)
                }
                DropdownMenu(
                    expanded = isDropdownVisible,
                    onDismissRequest = { onDropdownVisibilityChange(false) }
                ) {
                    options.forEach { option ->
                        DropdownMenuItem(
                            text = { Text(option) },
                            onClick = {
                                onOptionSelected(option)
                                onDropdownVisibilityChange(false)
                            }
                        )
                    }
                }
            }
        }
    }

    // Widget Permissions ===========
    private fun canDrawOverlays(context: Context): Boolean {
        return Settings.canDrawOverlays(context)
    }
    private fun requestOverlayPermission(context: Context) {
        val intent = Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            android.net.Uri.parse("package:${context.packageName}")
        )
        context.startActivity(intent)
    }


    override fun onDestroy() {
        super.onDestroy()
        Intent(applicationContext, FloatingWidgetService::class.java).also {
            it.action = FloatingWidgetService.Actions.STOP.toString()
            startService(it)
        }
    }
}


