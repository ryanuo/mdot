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
import androidx.core.app.NotificationCompat

class FloatingWindowService : Service() {

    companion object {
        private const val TAG = "FloatingWindowService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "floating_window_channel"
    }

    private var windowManager: WindowManager? = null
    private var floatingView: FrameLayout? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")
        // 必须立即启动前台服务
        startForeground(NOTIFICATION_ID, createNotification())

        showFloatingWindow()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        hideFloatingWindow()
        Log.d(TAG, "Service onDestroy")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Floating Window Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Service for managing floating buttons"
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        Log.d(TAG, "Notification created")
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Floating Clicker Active")
            .setContentText("悬浮窗服务正在运行")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .build()
    }

    private fun showFloatingWindow() {
        Log.d(TAG, "Showing floating window")
        if (floatingView != null) hideFloatingWindow()

        floatingView = FrameLayout(this)

        val layoutParams = WindowManager.LayoutParams().apply {
            type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            else
                @Suppress("DEPRECATION")
                WindowManager.LayoutParams.TYPE_PHONE

            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

            format = PixelFormat.TRANSLUCENT
            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.MATCH_PARENT
            gravity = Gravity.TOP or Gravity.START
        }

        windowManager?.addView(floatingView, layoutParams)
        Log.d(TAG, "Floating window added to WindowManager")

        setupButtons(floatingView!!)
    }

    private fun setupButtons(container: FrameLayout) {
        val buttonNames = listOf("触发", "按钮2", "按钮3", "按钮4")
        buttonNames.forEachIndexed { index, name ->
            val btn = Button(this).apply {
                text = name
                textSize = 12f
                setBackgroundColor(android.graphics.Color.parseColor("#007AFF"))
                layoutParams = FrameLayout.LayoutParams(200, 120).apply {
                    leftMargin = 50
                    topMargin = 50 + index * 150
                }

                setOnClickListener {
                    Log.d(TAG, "Button clicked: $name")
                    // 这里可以执行对应操作
                }

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
                                return true
                            }
                        }
                        return false
                    }
                })
            }
            container.addView(btn)
        }
    }

    private fun hideFloatingWindow() {
        floatingView?.let {
            windowManager?.removeView(it)
            floatingView = null
            Log.d(TAG, "Floating window removed")
        }
    }
}
