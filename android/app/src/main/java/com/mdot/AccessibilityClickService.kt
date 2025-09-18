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
        // We don't need to handle accessibility events for this use case
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

    fun performClickInternal(x: Float, y: Float): Boolean {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val path = Path()
                path.moveTo(x, y)
                
                val gestureBuilder = GestureDescription.Builder()
                gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 100))
                
                val gesture = gestureBuilder.build()
                
                val success = dispatchGesture(gesture, object : GestureResultCallback() {
                    override fun onCompleted(gestureDescription: GestureDescription?) {
                        super.onCompleted(gestureDescription)
                        Log.d(TAG, "Click gesture completed at ($x, $y)")
                    }

                    override fun onCancelled(gestureDescription: GestureDescription?) {
                        super.onCancelled(gestureDescription)
                        Log.e(TAG, "Click gesture cancelled at ($x, $y)")
                    }
                }, null)
                
                Log.d(TAG, "Gesture dispatch result: $success")
                success
            } else {
                Log.e(TAG, "Gesture dispatch not supported on this Android version")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error performing click", e)
            false
        }
    }
}
