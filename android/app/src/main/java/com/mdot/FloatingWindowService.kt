package com.mdot

import android.app.*
import android.content.Context
import android.content.Intent
import android.graphics.*
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import android.view.*
import android.widget.TextView
import android.widget.FrameLayout
import androidx.core.app.NotificationCompat
import java.util.concurrent.Executors

class FloatingWindowService : Service() {

    companion object {
        private const val TAG = "FloatingWindowService"
        private const val NOTIFICATION_ID = 1001
        private const val CHANNEL_ID = "floating_window_channel"
    }

    private var windowManager: WindowManager? = null
    private var floatingView: View? = null
    private val clickPoints = mutableListOf<ClickPoint>()
    private val executor = Executors.newCachedThreadPool()
    private val handler = Handler(Looper.getMainLooper())

    data class ClickPoint(
        val id: Int,
        var x: Int,
        var y: Int,
        val view: View,
        val layoutParams: WindowManager.LayoutParams
    )

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
            showFloatingPoints()
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
        executor.shutdown() // 关闭线程池
        hideFloatingPoints()
        Log.d(TAG, "Service onDestroy")
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Floating Window Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Service for managing floating click points"
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
            .setContentText("悬浮点击器正在运行")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun showFloatingPoints() {
        Log.d(TAG, "Showing floating points")

        try {
            // 清理现有的点位
            hideFloatingPoints()

            // 创建4个悬浮点位
            val initialPositions = listOf(
                Pair(100, 200),   // 触发按钮 - 移到左上角
                Pair(300, 400),   // 点位1 - 中间偏左
                Pair(500, 600),   // 点位2 - 中间偏右
                Pair(700, 800)    // 点位3 - 右下角
            )
            val pointLabels = listOf("触发", "点位1", "点位2", "点位3")

            initialPositions.forEachIndexed { index, (x, y) ->
                createFloatingPoint(index, x, y, pointLabels[index])
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error showing floating points", e)
            stopSelf()
        }
    }

    private fun createFloatingPoint(id: Int, x: Int, y: Int, label: String) {
        try {
            // 创建圆形点位视图
            val pointView = TextView(this).apply {
                text = label
                textSize = 10f
                setTextColor(Color.WHITE)
                gravity = Gravity.CENTER

                // 设置空心圆形背景
                background = createCircleDrawable(id == 0) // 触发按钮使用不同颜色

                // 设置固定大小
                width = 80
                height = 80
            }

            val layoutParams = WindowManager.LayoutParams().apply {
                type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
                } else {
                    @Suppress("DEPRECATION")
                    WindowManager.LayoutParams.TYPE_PHONE
                }

                flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                        WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL or
                        WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN

                format = PixelFormat.TRANSLUCENT
                width = 80
                height = 80
                gravity = Gravity.TOP or Gravity.START
                this.x = x
                this.y = y
            }

            // 添加触摸监听器
            pointView.setOnTouchListener(createPointTouchListener(id))

            windowManager?.addView(pointView, layoutParams)

            // 保存点位信息
            val clickPoint = ClickPoint(id, x, y, pointView, layoutParams)
            clickPoints.add(clickPoint)

            Log.d(TAG, "Created floating point $id at ($x, $y)")

        } catch (e: Exception) {
            Log.e(TAG, "Error creating floating point", e)
        }
    }

    private fun createCircleDrawable(isTrigger: Boolean): GradientDrawable {
        return GradientDrawable().apply {
            shape = GradientDrawable.OVAL

            if (isTrigger) {
                // 触发按钮：实心红色
                setColor(Color.parseColor("#FF4444"))
            } else {
                // 普通点位：空心蓝色
                setColor(Color.TRANSPARENT)
                setStroke(4, Color.parseColor("#0088FF"))
            }

            // 添加半透明效果
            alpha = 200
        }
    }

    private fun createPointTouchListener(pointId: Int): View.OnTouchListener {
        return object : View.OnTouchListener {
            private var initialX = 0
            private var initialY = 0
            private var initialTouchX = 0f
            private var initialTouchY = 0f
            private var isDragging = false
            private val dragThreshold = 15 // 增加拖拽阈值

            override fun onTouch(v: View, event: MotionEvent): Boolean {
                return try {
                    when (event.action) {
                        MotionEvent.ACTION_DOWN -> {
                            val clickPoint = clickPoints.find { it.id == pointId }
                            if (clickPoint != null) {
                                initialX = clickPoint.layoutParams.x
                                initialY = clickPoint.layoutParams.y
                                initialTouchX = event.rawX
                                initialTouchY = event.rawY
                                isDragging = false

                                // 视觉反馈：按下时变暗
                                v.alpha = 0.6f
                                Log.d(TAG, "Touch DOWN on point $pointId at (${initialX}, ${initialY})")
                            }
                            true
                        }
                        MotionEvent.ACTION_MOVE -> {
                            val deltaX = (event.rawX - initialTouchX).toInt()
                            val deltaY = (event.rawY - initialTouchY).toInt()

                            if (Math.abs(deltaX) > dragThreshold || Math.abs(deltaY) > dragThreshold) {
                                if (!isDragging) {
                                    Log.d(TAG, "Started dragging point $pointId")
                                    isDragging = true
                                }
                                updatePointPosition(pointId, initialX + deltaX, initialY + deltaY)
                            }
                            true
                        }
                        MotionEvent.ACTION_UP -> {
                            // 恢复透明度
                            v.alpha = 1f

                            if (!isDragging) {
                                Log.d(TAG, "Point $pointId was CLICKED (not dragged)")
                                // 延时执行点击，避免立即执行
                                handler.postDelayed({
                                    handlePointClick(pointId)
                                }, 50)
                            } else {
                                Log.d(TAG, "Point $pointId was DRAGGED to new position")
                            }
                            isDragging = false
                            true // 返回true表示事件已处理
                        }
                        else -> false
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error in point touch listener", e)
                    false
                }
            }
        }
    }

    private fun updatePointPosition(pointId: Int, x: Int, y: Int) {
        try {
            val clickPoint = clickPoints.find { it.id == pointId }
            if (clickPoint != null) {
                clickPoint.layoutParams.x = x
                clickPoint.layoutParams.y = y
                clickPoint.x = x
                clickPoint.y = y

                windowManager?.updateViewLayout(clickPoint.view, clickPoint.layoutParams)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error updating point position", e)
        }
    }

    private fun handlePointClick(pointId: Int) {
        try {
            when (pointId) {
                0 -> {
                    // 触发按钮：执行所有其他点位的屏幕点击
                    Log.d(TAG, "Trigger button clicked - executing screen clicks at all point positions")
                    triggerAllClickPoints()
                }
                else -> {
                    // 普通点位：在该点位的屏幕位置执行屏幕点击
                    val clickPoint = clickPoints.find { it.id == pointId }
                    if (clickPoint != null) {
                        // 获取圆点当前的屏幕坐标
                        val screenX = clickPoint.layoutParams.x
                        val screenY = clickPoint.layoutParams.y

                        Log.d(TAG, "Point $pointId clicked - executing screen click at position: ($screenX, $screenY)")

                        // 执行屏幕点击（不是圆点点击）
                        executeScreenClickAtPosition(screenX, screenY)

                        // 视觉反馈：点击动画
                        showClickAnimation(clickPoint.view)
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error handling point click", e)
        }
    }

    private fun triggerAllClickPoints() {
        try {
            // 获取除触发按钮外的所有点位
            val clickablePoints = clickPoints.filter { it.id != 0 }

            Log.d(TAG, "Triggering screen clicks at ${clickablePoints.size} point positions:")
            clickablePoints.forEach { point ->
                Log.d(TAG, "  Point ${point.id}: (${point.layoutParams.x}, ${point.layoutParams.y})")
            }

            // 使用线程池同时执行所有屏幕点击操作
            clickablePoints.forEach { clickPoint ->
                executor.execute {
                    // 获取最新的屏幕坐标
                    val screenX = clickPoint.layoutParams.x
                    val screenY = clickPoint.layoutParams.y

                    // 在后台线程执行屏幕点击操作
                    executeScreenClickAtPosition(screenX, screenY)

                    // 在主线程更新UI动画
                    handler.post {
                        showClickAnimation(clickPoint.view)
                    }
                }
            }

            Log.d(TAG, "Started simultaneous screen clicks for ${clickablePoints.size} positions")
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering all click points", e)
        }
    }

    private fun executeScreenClickAtPosition(x: Int, y: Int) {
        try {
            // 检查无障碍服务是否可用
            val accessibilityService = AccessibilityClickService.getInstance()
            if (accessibilityService == null) {
                Log.e(TAG, "❌ Accessibility service is not available! Please enable it in Settings.")
                return
            }

            // 计算圆点中心的屏幕坐标
            // x, y 是圆点左上角的坐标，需要加上圆点半径(40px)得到中心点
            val centerX = x + 40f
            val centerY = y + 40f

            Log.d(TAG, "🎯 Executing SCREEN CLICK (not point click):")
            Log.d(TAG, "  - Point top-left: ($x, $y)")
            Log.d(TAG, "  - Target screen center: ($centerX, $centerY)")
            Log.d(TAG, "  - This will click whatever is at that screen position")

            // 调用无障碍服务在屏幕位置执行实际点击
            val success = AccessibilityClickService.performClick(centerX, centerY)
            if (success) {
                Log.d(TAG, "✅ SCREEN CLICK executed successfully at ($centerX, $centerY)")
                Log.d(TAG, "   Any UI element at this position should respond")
            } else {
                Log.e(TAG, "❌ Failed to execute SCREEN CLICK at ($centerX, $centerY)")
                Log.e(TAG, "Troubleshooting:")
                Log.e(TAG, "  1. Ensure Accessibility Service is enabled")
                Log.e(TAG, "  2. Check if coordinates are within screen bounds")
                Log.e(TAG, "  3. Verify target app accepts touch events")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception during screen click execution", e)
        }
    }

    private fun showClickAnimation(view: View) {
        try {
            // 简单的缩放动画
            view.animate()
                .scaleX(1.3f)
                .scaleY(1.3f)
                .setDuration(150)
                .withEndAction {
                    view.animate()
                        .scaleX(1f)
                        .scaleY(1f)
                        .setDuration(150)
                        .start()
                }
                .start()
        } catch (e: Exception) {
            Log.e(TAG, "Error showing click animation", e)
        }
    }

    private fun hideFloatingPoints() {
        try {
            clickPoints.forEach { clickPoint ->
                windowManager?.removeView(clickPoint.view)
            }
            clickPoints.clear()
            Log.d(TAG, "All floating points removed")
        } catch (e: Exception) {
            Log.e(TAG, "Error hiding floating points", e)
        }
    }
}