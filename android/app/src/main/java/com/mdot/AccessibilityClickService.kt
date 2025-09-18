package com.mdot

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.os.Build
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class AccessibilityClickService : AccessibilityService() {

    companion object {
        private const val TAG = "AccessibilityClickService"
        private var instance: AccessibilityClickService? = null

        fun getInstance(): AccessibilityClickService? = instance

        /**
         * 提供外部调用的点击方法
         */
        fun performClick(x: Float, y: Float): Boolean {
            val service = getInstance()
            return service?.performClickInternal(x, y) ?: false
        }
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 不需要处理辅助功能事件
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
        instance = null
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        Log.d(TAG, "Accessibility service destroyed")
    }

    private fun performClickInternal(x: Float, y: Float): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Log.d(TAG, "Attempting to perform click at ($x, $y)")

                val path = Path().apply { moveTo(x, y) }

                val gestureBuilder = GestureDescription.Builder()
                gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 200)) // 200ms 点击

                val gesture = gestureBuilder.build()

                val success = dispatchGesture(gesture, object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        super.onCompleted(gestureDescription)
                        Log.d(TAG, "Click gesture completed successfully at ($x, $y)")
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        super.onCancelled(gestureDescription)
                        Log.e(TAG, "Click gesture was cancelled at ($x, $y)")
                    }
                }, null)

                if (success) {
                    Log.d(TAG, "Gesture dispatch initiated successfully at ($x, $y)")
                } else {
                    Log.e(TAG, "Failed to dispatch gesture at ($x, $y)")
                }

                success
            } else {
                Log.e(TAG, "Gesture dispatch not supported on Android API ${Build.VERSION.SDK_INT}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing click at ($x, $y)", e)
            false
        }
    }
}
