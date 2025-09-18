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
    private var floatingView: View? = null

    override fun onCreate() {
        super.onCreate()
        Log.d(TAG, "Service onCreate")
        windowManager = getSystemService(Context.WINDOW_SERVICE) as WindowManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d(TAG, "Service onStartCommand")

        try {
            // 必须立即启动前台服务
            startForeground(NOTIFICATION_ID, createNotification())
            showFloatingWindow()
        } catch (e: Exception) {
            Log.e(TAG, "Error in onStartCommand", e)
            stopSelf()
            return START_NOT_STICKY
        }

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
                setShowBadge(false)
            }
            val notificationManager = getSystemService(NotificationManager::class.java)
            notificationManager?.createNotificationChannel(channel)
            Log.d(TAG, "Notification channel created")
        }
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
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
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showFloatingWindow() {
        Log.d(TAG, "Showing floating window")

        try {
            if (floatingView != null) {
                hideFloatingWindow()
            }

            // 创建一个简单的容器视图
            floatingView = FrameLayout(this).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
            }

            val layoutParams = WindowManager.LayoutParams().apply {
                // 窗口类型
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                // 窗口标志 - 关键修复点
                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

                format = PixelFormat.TRANSLUCENT

                // 关键修复：使用具体尺寸而不是 MATCH_PARENT
                width = WindowManager.LayoutParams.WRAP_CONTENT
                height = WindowManager.LayoutParams.WRAP_CONTENT

                // 初始位置
                gravity = Gravity.TOP or Gravity.START
                x = 100
                y = 100
            }

            windowManager?.addView(floatingView, layoutParams)
            Log.d(TAG, "Floating window added to WindowManager")

            setupButtons(floatingView as FrameLayout)

        } catch (e: Exception) {
            Log.e(TAG, "Error showing floating window", e)
            stopSelf()
        }
    }

    private fun setupButtons(container: FrameLayout) {
        try {
            val buttonNames = listOf("触发", "按钮2", "按钮3", "按钮4")

            buttonNames.forEachIndexed { index, name ->
                val btn = Button(this).apply {
                    text = name
                    textSize = 12f
                    setBackgroundColor(android.graphics.Color.parseColor("#007AFF"))
                    setTextColor(android.graphics.Color.WHITE)

                    // 固定按钮大小
                    layoutParams = FrameLayout.LayoutParams(200, 120).apply {
                        leftMargin = 10
                        topMargin = 10 + index * 130
                    }

                    setOnClickListener {
                        Log.d(TAG, "Button clicked: $name")
                        handleButtonClick(name, index)
                    }

                    // 添加拖拽功能
                    setOnTouchListener(createDragTouchListener())
                }

                container.addView(btn)
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error setting up buttons", e)
        }
    }

    private fun handleButtonClick(buttonName: String, index: Int) {
        try {
            when (index) {
                0 -> {
                    // 触发按钮 - 可以执行点击操作
                    Log.d(TAG, "Trigger button clicked")
                }
                else -> {
                    Log.d(TAG, "Button $buttonName clicked")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling button click", e)
        }
    }

    private fun createDragTouchListener(): View.OnTouchListener {
        return object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                return try {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            initialX = (v.parent as View).x.toInt()
                            initialY = (v.parent as View).y.toInt()
                            initialTouchX = event.rawX
                            initialTouchY = event.rawY
                            isDragging = false
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val deltaX = (event.rawX - initialTouchX).toInt()
                            val deltaY = (event.rawY - initialTouchY).toInt()

                            if (Math.abs(deltaX) > 10 || Math.abs(deltaY) > 10) {
                                isDragging = true
                                updateWindowPosition(initialX + deltaX, initialY + deltaY)
                            }
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            // 如果是拖拽，则不触发点击
                            !isDragging
                        }
                        else -> false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in touch listener", e)
                    false
                }
            }
        }
    }

    private fun updateWindowPosition(x: Int, y: Int) {
        try {
            floatingView?.let { view ->
                val layoutParams = view.layoutParams as WindowManager.LayoutParams
                layoutParams.x = x
                layoutParams.y = y
                windowManager?.updateViewLayout(view, layoutParams)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating window position", e)
        }
    }

    private fun hideFloatingWindow() {
        try {
            floatingView?.let { view ->
                windowManager?.removeView(view)
                floatingView = null
                Log.d(TAG, "Floating window removed")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding floating window", e)
        }
    }
}