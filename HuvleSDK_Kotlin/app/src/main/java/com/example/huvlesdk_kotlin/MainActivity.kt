package com.example.huvlesdk_kotlin

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.byappsoft.sap.launcher.Sap_act_main_launcher
import com.byappsoft.sap.utils.Sap_Func

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val PERMISSION_REQUEST_CODE_POST_NOTIFICATIONS = 0
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 안드로이드 13 (TIRAMISU) 이상 알림 권한 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!checkPostNotificationPermission()) {
                requestPostNotificationPermission()
            }
        }

        //-- Notification On Event.
        findViewById<Button>(R.id.noti_on_btn).setOnClickListener {
            Sap_Func.notiUpdate(applicationContext)
        }
        //-- Notification Off Event.
        findViewById<Button>(R.id.noti_off_btn).setOnClickListener {
            Sap_Func.notiCancel(applicationContext)
        }

        val txtPackageName = findViewById<TextView>(R.id.txt)
        txtPackageName.text = "Package : ${baseContext.packageName}"
    }

    override fun onResume() {
        super.onResume()
        // TODO-- huvleView apply
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Sap_Func.setServiceState(this, true)
        }
        huvleView()
    }

    private fun huvleView() {
        Sap_Func.setNotiBarLockScreen(this, false)
        Sap_act_main_launcher.initsapStart(this, "bynetwork", true, true,
            object : Sap_act_main_launcher.OnLauncher {
                override fun onDialogOkClicked() {
                    checkDrawOverlayPermission()
                }

                override fun onDialogCancelClicked() {
                    Log.d(TAG, "HuvleView dialog cancelled.")
                }

                override fun onInitSapStartapp() {
                    Log.d(TAG, "HuvleView onInitSapStartapp.")
                }

                override fun onUnknown() {
                    Log.w(TAG, "HuvleView onUnknown.")
                }
            })
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE_POST_NOTIFICATIONS) {
            if (checkPostNotificationPermission()) {
                // Post notification 권한이 허용된 경우.
                Log.i(TAG, "POST_NOTIFICATIONS permission granted via dialog.")
            } else {
                Log.w(TAG, "POST_NOTIFICATIONS permission denied via dialog.")
            }
        }
    }


    private fun checkDrawOverlayPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true // M 미만에서는 런타임 권한 필요 없음
        }

        return if (Settings.canDrawOverlays(this)) {
            true
        } else {
            AlertDialog.Builder(this).apply {
                setTitle("다른앱 위에 그리기")
                setMessage("다른 앱 위에 그리기 권한을 허용해주세요.")
                setPositiveButton("확인") { _, _ ->
                    val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                }
                setNegativeButton("취소") { dialog, _ ->
                    dialog.cancel()
                }
            }.create().show()
            false
        }
    }

    private fun checkPostNotificationPermission(): Boolean {
        return when {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU ->
                checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
            else -> true
        }
    }

    private fun requestPostNotificationPermission() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    PERMISSION_REQUEST_CODE_POST_NOTIFICATIONS
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting POST_NOTIFICATIONS permission", e)
        }
    }
}