package com.mdot

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.view.*
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import androidx.core.app.NotificationCompat
import org.json.JSONArray

class FloatingWindowService : Service() {

    companion object {
        private const val TAG = "FloatingWindowService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "floating_window_channel"
    }

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private var points: MutableList<FloatingPoint> = mutableListOf()

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        intent?.getStringExtra("points")?.let { pointsJson ->
            parsePoints(pointsJson)
            showFloatingWindow()
        }

        startForeground(NOTIFICATION_ID, createNotification())
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        hideFloatingWindow()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Floating Window Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Service for managing floating click points" }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("MultiPoint Floating Clicker")
            .setContentText("Floating points are active")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun parsePoints(pointsJson: String) {
        try {
            val jsonArray = JSONArray(pointsJson)
            points.clear()
            for (i in 0 until jsonArray.length()) {
                val pointJson = jsonArray.getJSONObject(i)
                points.add(
                    FloatingPoint(
                        id = pointJson.getString("id"),
                        x = pointJson.getInt("x"),
                        y = pointJson.getInt("y")
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parsing points", e)
        }
    }

    private fun showFloatingWindow() {
        if (floatingView != null) hideFloatingWindow()

        val layoutInflater = LayoutInflater.from(this)
        floatingView = layoutInflater.inflate(R.layout.floating_window_layout, null)

        val layoutParams = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE

            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED

            format = PixelFormat.TRANSLUCENT
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.TOP or Gravity.START
        }

        setupFloatingPoints()

        windowManager?.addView(floatingView, layoutParams)
        Log.d(TAG, "Floating window shown with ${points.size} points")
    }

    private fun setupFloatingPoints() {
        val container = floatingView?.findViewById<LinearLayout>(R.id.floating_container)
        container?.removeAllViews()

        // 添加点位按钮
        points.forEach { point ->
            val pointView = createFloatingPointView(point)
            container?.addView(pointView)
        }

        // 控制按钮容器
        val buttonContainer = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply { setMargins(50, 50, 0, 0) }
        }

        // 触发按钮
        val triggerButton = Button(this).apply {
            text = "触发所有"
            textSize = 14f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#34C759"))
            elevation = 10f
            layoutParams = FrameLayout.LayoutParams(200, 80).apply {
                leftMargin = 0
                topMargin = 0
            }
            setOnClickListener { triggerAllPoints() }
        }

        // 关闭按钮
        val closeButton = Button(this).apply {
            text = "关闭"
            textSize = 14f
            setTextColor(android.graphics.Color.WHITE)
            setBackgroundColor(android.graphics.Color.parseColor("#FF3B30"))
            elevation = 10f
            layoutParams = FrameLayout.LayoutParams(120, 80).apply {
                leftMargin = 20
                topMargin = 0
            }
            setOnClickListener {
                hideFloatingWindow()
                stopSelf()
            }
        }

        buttonContainer.addView(triggerButton)
        buttonContainer.addView(closeButton)
        container?.addView(buttonContainer)
    }

    private fun createFloatingPointView(point: FloatingPoint): View {
        return Button(this).apply {
            text = point.id
            textSize = 12f
            setBackgroundColor(android.graphics.Color.parseColor("#007AFF"))
            layoutParams = FrameLayout.LayoutParams(120, 120)
            x = point.x.toFloat()
            y = point.y.toFloat()
            elevation = 10f

            setOnClickListener { triggerSinglePoint(point) }

            setOnTouchListener(object : View.OnTouchListener {
                private var initialX = 0
                private var initialY = 0
                private var initialTouchX = 0f
                private var initialTouchY = 0f

                override fun onTouch(v: View, event: MotionEvent): Boolean {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = x.toInt()
                            initialY = y.toInt()
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            return true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val deltaX = (event.rawX - initialTouchX).toInt()
                            val deltaY = (event.rawY - initialTouchY).toInt()
                            x = (initialX + deltaX).toFloat()
                            y = (initialY + deltaY).toFloat()
                            point.x = (initialX + deltaX)
                            point.y = (initialY + deltaY)
                            return true
                        }
                    }
                    return false
                }
            })
        }
    }

    private fun triggerSinglePoint(point: FloatingPoint) {
        Log.d(TAG, "Triggering single point: ${point.id} at (${point.x}, ${point.y})")
        val intent = Intent(this, AccessibilityClickService::class.java).apply {
            putExtra("action", "click")
            putExtra("x", point.x.toFloat())
            putExtra("y", point.y.toFloat())
        }
        startService(intent)
    }

    private fun triggerAllPoints() {
        points.forEach {
            triggerSinglePoint(it)
            Thread.sleep(100)
        }
    }

    private fun hideFloatingWindow() {
        floatingView?.let { view ->
            windowManager?.removeView(view)
            floatingView = null
        }
        Log.d(TAG, "Floating window hidden")
    }

    data class FloatingPoint(
        val id: String,
        var x: Int,
        var y: Int
    )
}
