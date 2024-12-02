package com.example.mangatranslator.ui.components

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext

@Composable
fun SettingsPage(context: Context) {
    var enableWidget by remember { mutableStateOf(false) }
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
            // Start Overlay Widget Switch Button
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
                            if (!canDrawOverlays(context)) {
                                requestOverlayPermission(context)
                            } else {
                                context.startService(Intent(context, FloatingWidgetService::class.java))
                            }
                        } else {
                            context.stopService(Intent(context, FloatingWidgetService::class.java))
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

@Composable
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

fun canDrawOverlays(context: Context): Boolean {
    return Settings.canDrawOverlays(context)
}

fun requestOverlayPermission(context: Context) {
    val intent = Intent(
        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
        android.net.Uri.parse("package:${context.packageName}")
    )
    context.startActivity(intent)
}
