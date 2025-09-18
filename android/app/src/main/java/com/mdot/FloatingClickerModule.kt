package com.mdot

import android.content.Context
import android.content.Intent
import android.provider.Settings
import android.util.Log
import android.os.Build  // 添加这个导入
import com.facebook.react.bridge.*
import com.facebook.react.modules.core.DeviceEventManagerModule

class FloatingClickerModule(private val reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    companion object {
        private const val TAG = "FloatingClickerModule"
    }

    override fun getName(): String {
        return "FloatingClicker"
    }

    @ReactMethod
    fun startFloatingWindow(promise: Promise) {
        try {
            // 使用 applicationContext 而不是 currentActivity
            val context = reactContext.applicationContext

            // 检查悬浮窗权限
            if (!Settings.canDrawOverlays(context)) {
                promise.reject("NO_OVERLAY_PERMISSION", "Overlay permission not granted")
                return
            }

            // 检查无障碍服务是否启用
            val hasAccessibilityPermission = checkAccessibilityServiceEnabled()
            if (!hasAccessibilityPermission) {
                Log.w(TAG, "Accessibility service not enabled, but continuing...")
            }

            // 创建 Intent 启动服务
            val intent = Intent(context, FloatingWindowService::class.java)

            // 使用 applicationContext 启动前台服务
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }

                Log.d(TAG, "FloatingWindowService started successfully")
                promise.resolve(true)

            } catch (e: SecurityException) {
                Log.e(TAG, "SecurityException starting service", e)
                promise.reject("SECURITY_ERROR", "无法启动前台服务: ${e.message}")
            } catch (e: Exception) {
                Log.e(TAG, "Exception starting service", e)
                promise.reject("START_ERROR", "启动服务失败: ${e.message}")
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error in startFloatingWindowTest", e)
            promise.reject("ERROR", e.message)
        }
    }

    private fun checkAccessibilityServiceEnabled(): Boolean {
        return try {
            val context = reactContext.applicationContext
            val enabledServices = Settings.Secure.getString(
                context.contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            )
            val packageName = context.packageName
            val serviceName = "$packageName/.AccessibilityClickService"

            enabledServices?.contains(serviceName) == true
        } catch (e: Exception) {
            Log.e(TAG, "Error checking accessibility service", e)
            false
        }
    }

    @ReactMethod
    fun stopFloatingWindow(promise: Promise) {
        try {
            val context = reactContext.applicationContext
            val intent = Intent(context, FloatingWindowService::class.java)
            context.stopService(intent)

            Log.d(TAG, "Floating window service stopped")
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error stopping floating window", e)
            promise.reject("STOP_ERROR", e.message)
        }
    }

    @ReactMethod
    fun checkOverlayPermission(promise: Promise) {
        try {
            val context = reactContext.currentActivity
            if (context == null) {
                promise.reject("NO_ACTIVITY", "No current activity")
                return
            }

            val hasPermission = Settings.canDrawOverlays(context)
            promise.resolve(hasPermission)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking overlay permission", e)
            promise.reject("CHECK_ERROR", e.message)
        }
    }

    @ReactMethod
    fun requestOverlayPermission(promise: Promise) {
        try {
            val context = reactContext.currentActivity
            if (context == null) {
                promise.reject("NO_ACTIVITY", "No current activity")
                return
            }

            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            context.startActivity(intent)
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting overlay permission", e)
            promise.reject("REQUEST_ERROR", e.message)
        }
    }

    @ReactMethod
    fun checkAccessibilityPermission(promise: Promise) {
        try {
            val context = reactContext.applicationContext
            val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            val packageName = context.packageName

            // 尝试多种可能的服务名称格式
            val possibleServiceNames = listOf(
                "$packageName/.AccessibilityClickService",
                "$packageName/com.mdot.AccessibilityClickService",
                "com.mdot/.AccessibilityClickService",
                "com.mdot/com.mdot.AccessibilityClickService"
            )

            Log.d(TAG, "Package name: $packageName")
            Log.d(TAG, "Enabled services: $enabledServices")

            var hasPermission = false
            var matchedServiceName = ""

            for (serviceName in possibleServiceNames) {
                Log.d(TAG, "Checking service name: $serviceName")
                if (enabledServices?.contains(serviceName) == true) {
                    hasPermission = true
                    matchedServiceName = serviceName
                    Log.d(TAG, "Found matching service: $serviceName")
                    break
                }
            }

            val isServiceActive = AccessibilityClickService.getInstance() != null

            Log.d(TAG, "Accessibility permission check: hasPermission=$hasPermission, matchedService=$matchedServiceName, isServiceActive=$isServiceActive")

            // 如果权限已授予，即使服务实例暂时不可用也返回 true
            val result = hasPermission
            Log.d(TAG, "Final result: $result")
            promise.resolve(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error checking accessibility permission", e)
            promise.reject("CHECK_ERROR", e.message)
        }
    }

    @ReactMethod
    fun requestAccessibilityPermission(promise: Promise) {
        try {
            val context = reactContext.currentActivity
            if (context == null) {
                promise.reject("NO_ACTIVITY", "No current activity")
                return
            }

            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            context.startActivity(intent)
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting accessibility permission", e)
            promise.reject("REQUEST_ERROR", e.message)
        }
    }

    @ReactMethod
    fun disableAccessibilityService(promise: Promise) {
        try {
            val context = reactContext.applicationContext
            val packageName = context.packageName
            val serviceName = "$packageName/.AccessibilityClickService"

            // 尝试通过设置禁用服务
            val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)

            Log.d(TAG, "Redirected to accessibility settings to disable service: $serviceName")
            promise.resolve(true)
        } catch (e: Exception) {
            Log.e(TAG, "Error disabling accessibility service", e)
            promise.reject("DISABLE_ERROR", e.message)
        }
    }
    @ReactMethod
    fun disableOverlayPermission(promise: Promise){
        try {
            val context = reactContext.currentActivity
            if (context == null) {
                promise.reject("NO_ACTIVITY", "No current activity")
                return
            }

            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
            promise.resolve(true)
        }
        catch (e: Exception) {
            Log.e(TAG, "Error disabling overlay permission", e)
            promise.reject("DISABLE_ERROR", e.message)
        }
    }

    @ReactMethod
    fun triggerClick(x: Int, y: Int, promise: Promise) {
        try {
            val serviceInstance = AccessibilityClickService.getInstance()
            if (serviceInstance == null) {
                Log.e(TAG, "Accessibility service instance is null")
                promise.reject("CLICK_ERROR", "无障碍服务实例不可用，请确保服务已启用并重新启动应用")
                return
            }

            val success = AccessibilityClickService.performClick(x.toFloat(), y.toFloat())
            if (success) {
                Log.d(TAG, "Click triggered successfully at ($x, $y)")
                promise.resolve(true)
            } else {
                Log.e(TAG, "Failed to trigger click at ($x, $y)")
                promise.reject("CLICK_ERROR", "点击手势执行失败，请检查无障碍服务设置")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering click", e)
            promise.reject("CLICK_ERROR", e.message)
        }
    }

    @ReactMethod
    fun triggerMultipleClicks(points: ReadableArray, promise: Promise) {
        try {
            var successCount = 0
            var totalCount = 0

            for (i in 0 until points.size()) {
                val point = points.getMap(i)
                if (point != null) {
                    val x = point.getInt("x")
                    val y = point.getInt("y")
                    totalCount++

                    val success = AccessibilityClickService.performClick(x.toFloat(), y.toFloat())
                    if (success) {
                        successCount++
                        Log.d(TAG, "Click $i triggered successfully at ($x, $y)")
                    } else {
                        Log.e(TAG, "Failed to trigger click $i at ($x, $y)")
                    }

                    // Small delay between clicks
                    Thread.sleep(100)
                }
            }

            if (successCount == totalCount && totalCount > 0) {
                promise.resolve(true)
            } else {
                promise.reject("CLICKS_ERROR", "Only $successCount out of $totalCount clicks succeeded")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error triggering multiple clicks", e)
            promise.reject("CLICKS_ERROR", e.message)
        }
    }

    private fun convertReadableArrayToJson(array: ReadableArray): String {
        val jsonArray = org.json.JSONArray()
        for (i in 0 until array.size()) {
            val map = array.getMap(i)
            if (map != null) {
                val jsonObject = org.json.JSONObject()
                jsonObject.put("id", map.getString("id"))
                jsonObject.put("x", map.getInt("x"))
                jsonObject.put("y", map.getInt("y"))
                jsonArray.put(jsonObject)
            }
        }
        return jsonArray.toString()
    }

    @ReactMethod
    fun forceCheckAccessibilityService(promise: Promise) {
        try {
            val context = reactContext.applicationContext
            val enabledServices = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            val packageName = context.packageName
            val serviceName = "$packageName/.AccessibilityClickService"

            Log.d(TAG, "Force check - Package: $packageName")
            Log.d(TAG, "Force check - Service: $serviceName")
            Log.d(TAG, "Force check - Enabled services: $enabledServices")

            val hasPermission = enabledServices?.contains(serviceName) == true
            val isServiceActive = AccessibilityClickService.getInstance() != null

            Log.d(TAG, "Force check - hasPermission: $hasPermission")
            Log.d(TAG, "Force check - isServiceActive: $isServiceActive")

            val result = mapOf(
                "hasPermission" to hasPermission,
                "isServiceActive" to isServiceActive,
                "serviceName" to serviceName,
                "enabledServices" to (enabledServices ?: "null")
            )

            promise.resolve(result)
        } catch (e: Exception) {
            Log.e(TAG, "Error in force check", e)
            promise.reject("FORCE_CHECK_ERROR", e.message)
        }
    }

    fun sendEvent(eventName: String, params: WritableMap?) {
        reactContext
            .getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter::class.java)
            .emit(eventName, params)
    }
}
