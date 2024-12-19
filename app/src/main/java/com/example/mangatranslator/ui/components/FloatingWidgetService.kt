package com.example.mangatranslator.ui.components

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.PixelFormat
import android.hardware.display.DisplayManager
import android.hardware.display.VirtualDisplay
import android.media.Image
import android.media.ImageReader
import android.media.projection.MediaProjection
import android.media.projection.MediaProjectionManager
import android.util.DisplayMetrics
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.core.app.NotificationCompat
import com.example.mangatranslator.R
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import androidx.lifecycle.LifecycleService
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.google.mlkit.vision.text.japanese.JapaneseTextRecognizerOptions
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs


class FloatingWidgetService : LifecycleService(), SavedStateRegistryOwner {
    private val savedStateRegistryController = SavedStateRegistryController.create(this)
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateRegistryController.savedStateRegistry

    private var textBlocksData by mutableStateOf<List<TextBlockData>>(emptyList())

    private lateinit var windowManager: WindowManager
    private lateinit var floatingView: View // Floating Widget View (static xml)
    private var floatingComposeView: ComposeView? = null // Floating composable Activity View

    private var mediaProjection: MediaProjection? = null
    private var virtualDisplay: VirtualDisplay? = null
    private lateinit var imageReader: ImageReader

    override fun onCreate() {
        savedStateRegistryController.performRestore(null)
        super.onCreate()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            Actions.START.toString() -> start(intent)
            Actions.STOP.toString() -> stopSelf()
        }
        return super.onStartCommand(intent, flags, startId)
    }


    @SuppressLint("ClickableViewAccessibility") // Fix later
    fun openTranslatePopUp() {
        var initialX = 0f
        var initialY = 0f
        val swipeThreshold = 100
        val composeView = ComposeView(this).apply {
            setContent {
                TranslatedPopUp(textBlocksData)
            }
        }
        composeView.setViewTreeSavedStateRegistryOwner(this)
        composeView.setViewTreeLifecycleOwner(this)

        composeView.setOnTouchListener { view, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    initialY = event.rawY
                    initialX = event.rawX
                    true
                }
                MotionEvent.ACTION_MOVE -> {
                    val deltaY = event.rawY - initialY
                    view.translationY = deltaY
                    true
                }
                MotionEvent.ACTION_UP -> {
                    val deltaY = event.rawY - initialY
                    val deltaX = event.rawX - initialX
                    if (abs(deltaY) > swipeThreshold && abs(deltaX) < swipeThreshold) {
                        closeTranslatePopUp()
                        true
                    } else {
                        view.animate()
                            .translationY(0f)
                            .setDuration(300)
                            .start()
                        false
                    }
                }
                else -> false
            }
        }

        val layoutParamsPopUp = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE,
            PixelFormat.TRANSPARENT
        )
        floatingComposeView = composeView
        windowManager.addView(floatingComposeView, layoutParamsPopUp)
    }

    private fun closeTranslatePopUp() {
        windowManager.removeView(floatingComposeView)
        floatingComposeView?.removeAllViews()
        floatingComposeView = null
    }

    private fun start(intent: Intent?) {
        startForegroundNotification() // Notify User in header dropdown

        val resultCode = intent?.getIntExtra("RESULT_CODE", Activity.RESULT_CANCELED) ?: Activity.RESULT_CANCELED
        val data: Intent? = intent?.getParcelableExtra("DATA", Intent::class.java)
        if (resultCode == Activity.RESULT_OK && data != null) {
            val projectionManager = getSystemService(MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
            mediaProjection = projectionManager.getMediaProjection(resultCode, data)
            startScreenCapture()
        } else {
            Log.e("MediaProjection", "Failed to get resultCode or data from Intent.")
        }

        // Start the floating widget
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
        // Widget always starts at top right corner
        layoutParams.gravity = Gravity.TOP or Gravity.END
        layoutParams.x = 0
        layoutParams.y = 300

        // Add floating view layout widget to WindowManager
        windowManager.addView(floatingView, layoutParams)
        floatingView.setOnTouchListener(object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var touchX = 0f
            private var touchY = 0f
            private var startTime = 0L
            private val tapThreshold = 200
            private val moveThreshold = 10

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        initialX = layoutParams.x
                        initialY = layoutParams.y
                        touchX = event.rawX
                        touchY = event.rawY
                        startTime = System.currentTimeMillis()
                        return true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        layoutParams.x = initialX - (event.rawX - touchX).toInt()
                        layoutParams.y = initialY + (event.rawY - touchY).toInt()
                        windowManager.updateViewLayout(floatingView, layoutParams)
                        return true
                    }
                    MotionEvent.ACTION_UP -> {
                        val endTime = System.currentTimeMillis()
                        val timeDiff = endTime - startTime
                        // Determine if tap vs drag
                        val deltaX = abs(event.rawX - touchX)
                        val deltaY = abs(event.rawY - touchY)
                        if (timeDiff < tapThreshold && deltaX < moveThreshold && deltaY < moveThreshold) {
                            onTap()
                        }
                        return true
                    }
                }
                return false
            }

            private fun onTap() {
                if(floatingComposeView == null) {
                    val bitmap = captureScreenshot()
                    Log.e("MyMangaApp", "on tap")
                    if (bitmap != null) {
                        Log.e("MyMangaApp", "bitmap exists")
                        recognizeTextFromBitmap(bitmap)

                        openTranslatePopUp()
                    } else {
                        Log.e("MyMangaApp", "there is no bitmap")
                    }
                } else {
                    Log.e("MyMangaApp", "floatingComposeView already exists")
                }

            }
        })
    }

    private fun recognizeTextFromBitmap(bitmap: Bitmap) {
        val croppedFile = File(this.cacheDir, "cropped_bitmap.png")
        val outputStream = FileOutputStream(croppedFile)
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, outputStream)
        outputStream.close()
//        val externalFile = File(this.getExternalFilesDir(Environment.DIRECTORY_PICTURES), "cropped_bitmap.png")
//        FileOutputStream(externalFile).use { fos ->
//            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
//        }
        //Log.d("File Path", "Saved to: ${externalFile.absolutePath}")

        Log.d("Crop Debug", "Cropped image saved at: ${croppedFile.absolutePath}")
        Log.d("ML Kit Debug", "Bitmap Dimensions Passed to ML Kit: ${bitmap.width}x${bitmap.height}")

        val inputImage = InputImage.fromBitmap(Bitmap.createBitmap(bitmap), 0)

        //val recognizer: TextRecognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        val recognizer = TextRecognition.getClient(JapaneseTextRecognizerOptions.Builder().build())

        recognizer.process(inputImage)
            .addOnSuccessListener { visionText ->
                Log.d("Width", "bitmap${bitmap.width}")
                Log.d("MyMangaApp", "Recognized Text: ${visionText.text}")
                textBlocksData = visionText.textBlocks.map { block ->
                    TextBlockData(
                        boundingBox = block.boundingBox,
                        text = block.text
                    )
                }
                Log.d("MyMangaApp", "TextBlockData: $textBlocksData")
                for (block in visionText.textBlocks) {
                    val blockText = block.text
                    val blockBoundingBox = block.boundingBox
                    val blockCornerPoints = block.cornerPoints
                    Log.d("MyMangaApp", "Block Text: $blockText")
                    Log.d("MyMangaApp", "Bounding Box: $blockBoundingBox")
                    Log.d("MyMangaApp", "Corner Points: ${blockCornerPoints?.joinToString()}")
                }

            }
            .addOnFailureListener { e ->
                Log.e("MyMangaApp", "Text recognition failed", e)
            }
    }

    private fun startScreenCapture() {
        val displayMetrics = DisplayMetrics()
        val windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        windowManager.defaultDisplay.getMetrics(displayMetrics)

        val displayWidth = displayMetrics.widthPixels
        val displayHeight = displayMetrics.heightPixels
        val dpi = displayMetrics.densityDpi

        imageReader = ImageReader.newInstance(displayWidth, displayHeight, PixelFormat.RGBA_8888, 5)
        virtualDisplay = mediaProjection?.createVirtualDisplay(
            "ScreenCapture",
            displayWidth, displayHeight, dpi,
            DisplayManager.VIRTUAL_DISPLAY_FLAG_AUTO_MIRROR,
            imageReader.surface, null, null
        )
    }

    fun captureScreenshot(): Bitmap? {
        var retries = 5
        var image: Image? = null
        while (retries > 0 && image == null) {
            image = imageReader.acquireLatestImage()
            if (image == null) {
                Thread.sleep(100)
                retries--
            } else {
                val buffer = image.planes[0].buffer
                val bytes = ByteArray(buffer.remaining())
                buffer.get(bytes)

                val statusBarHeightId = resources.getIdentifier("status_bar_height", "dimen", "android")
                val statusBarHeight = resources.getDimensionPixelSize(statusBarHeightId)

                val navBarHeightId = resources.getIdentifier("navigation_bar_height", "dimen", "android")
                val navBarHeight = resources.getDimensionPixelSize(navBarHeightId)

                Log.d("MyMangaApp", "statusBarHeight:$statusBarHeight")
                Log.d("MyMangaApp", "navBarHeight:$navBarHeight")

                val originalBitmap = Bitmap.createBitmap(imageReader.width, imageReader.height, Bitmap.Config.ARGB_8888)
                val mutableBitmap = originalBitmap.copy(Bitmap.Config.ARGB_8888, true)

                val bitmap = Bitmap.createBitmap(
                    mutableBitmap,
                    0,
                    statusBarHeight,
                    mutableBitmap.width,
                    mutableBitmap.height - navBarHeight
                )
                Log.d("BitMapDebug", "Original Bitmap: ${mutableBitmap.width}x${mutableBitmap.height}")
                Log.d("BitMapDebug", "Crop bitmap: ${bitmap.width}x${bitmap.height}")

                buffer.rewind()
                bitmap.copyPixelsFromBuffer(buffer)
                image.close()

                return bitmap
            }
        }
        Log.e("MyMangaApp", "Failed to acquire image after retries")
        Toast.makeText(this, "Snapshot failed, try again!", Toast.LENGTH_SHORT).show()
        return null
    }

    private fun startForegroundNotification() {
        val notification = NotificationCompat.Builder(this, "running_channel")
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("MangaTranslator")
            .setContentText("Tap widget to translate words on screen")
            .build()
        startForeground(1, notification)
    }

    override fun onDestroy() {
        stopForeground(STOP_FOREGROUND_REMOVE)
        imageReader.close()
        virtualDisplay?.release()
        mediaProjection?.stop()
        windowManager.removeView(floatingView)
        stopSelf()
        super.onDestroy()
    }

    enum class Actions {
        START, STOP
    }


}