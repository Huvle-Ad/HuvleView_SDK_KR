package com.huvlesdkreactsample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.byappsoft.sap.browser.Sap_BrowserActivity
import com.byappsoft.sap.browser.Sap_MainActivity
import com.byappsoft.sap.utils.Sap_Func
import com.facebook.react.bridge.ReactApplicationContext
import com.facebook.react.bridge.ReactContextBaseJavaModule
import com.facebook.react.bridge.ReactMethod
import com.facebook.react.module.annotations.ReactModule

@ReactModule(name = "BrowserModule")
class BrowserModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {

    override fun getName(): String {
        return "BrowserModule"
    }

    @ReactMethod
    fun openNotificationSettings() {
        if (checkPermission()) {
            val context = reactApplicationContext
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                } else {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        addCategory(Intent.CATEGORY_DEFAULT)
                        data = Uri.parse("package:${context.packageName}")
                    }
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                context.runOnUiQueueThread {
                    Sap_Func.notiUpdate(context)
                }
            }
        }
    }

    @ReactMethod
    fun turnOffNotification() {
        reactApplicationContext.runOnUiQueueThread {
            Sap_Func.notiCancel(reactApplicationContext)
        }
    }

    @ReactMethod
    fun openSapMainActivity() {
        val context = reactApplicationContext
        val url = "https://www.huvle.com/global_set.php"
        val intent = Intent(context, Sap_MainActivity::class.java).apply {
            putExtra(Sap_BrowserActivity.PARAM_OPEN_URL, url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(reactApplicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }
}