package com.huvlesdkreactsample

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.byappsoft.sap.launcher.Sap_act_main_launcher
import com.facebook.react.ReactActivity
import com.facebook.react.ReactActivityDelegate
import com.facebook.react.defaults.DefaultNewArchitectureEntryPoint.fabricEnabled
import com.facebook.react.defaults.DefaultReactActivityDelegate

class MainActivity : ReactActivity() {

    private val REQ_PERMISSION_POST_NOTIFICATION = 1001
    private val REQ_PERMISSION_OVERLAY = 1002

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkNotificationPermission()
    }

    // [Step 1] 알림 권한 확인
    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), REQ_PERMISSION_POST_NOTIFICATION)
                return
            }
        }
        checkDrawOverlayPermission()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQ_PERMISSION_POST_NOTIFICATION) {
            checkDrawOverlayPermission()
        }
    }

    private fun checkDrawOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                // 권한 없으면 설정 화면으로 이동 -> onActivityResult로 이동
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
                startActivityForResult(intent, REQ_PERMISSION_OVERLAY)
            } else {
                requestIgnoreBatteryOptimizations()
            }
        } else {
            requestIgnoreBatteryOptimizations()
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQ_PERMISSION_OVERLAY) {
            Handler(Looper.getMainLooper()).postDelayed({
                requestIgnoreBatteryOptimizations()
            }, 500)
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val packageName = packageName
            val pm = getSystemService(POWER_SERVICE) as android.os.PowerManager

            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent()
                    intent.action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }

        initHuvleSDK()
    }

    private fun initHuvleSDK() {
        try {
            // Huvle SDK 실행
            Sap_act_main_launcher.initsapStart(this, "bynetwork", false, true)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getMainComponentName(): String = "huvleSDKreactSample"

    override fun createReactActivityDelegate(): ReactActivityDelegate =
        DefaultReactActivityDelegate(this, mainComponentName, fabricEnabled)
}
