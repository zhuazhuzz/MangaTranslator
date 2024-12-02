package com.example.mangatranslator.ui.components

import android.app.Service
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.compose.ui.tooling.preview.Preview
import com.example.mangatranslator.R


class FloatingWidgetService : Service() {

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate() {
        super.onCreate()

        windowManager = getSystemService(WINDOW_SERVICE) as WindowManager

        // Inflate the floating widget layout (view)
        floatingView = LayoutInflater.from(this).inflate(R.layout.floating_widget, null)

        // View Layout parameters for the floating widget
        val layoutParams = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSLUCENT
        )

        // Widget always starts at top right corner (when switch turned on)
        layoutParams.gravity = Gravity.TOP or Gravity.END
        layoutParams.x = 0
        layoutParams.y = 200

        // Add floating view layout widget to WindowManager
        windowManager.addView(floatingView, layoutParams)

        // Touch gestures for moving widget
        floatingView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var touchX = 0f
            private var touchY = 0f

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        touchX = event.rawX
                        touchY = event.rawY
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        layoutParams.x = initialX - (event.rawX - touchX).toInt()
                        layoutParams.y = initialY + (event.rawY - touchY).toInt()
                        windowManager.updateViewLayout(floatingView, layoutParams)
                        return true
                    }
                }
                return false
            }
        })

    }

    override fun onDestroy() {
        super.onDestroy()
        windowManager.removeView(floatingView)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
