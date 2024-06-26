package com.example.huvlesdk_kotlin

import android.Manifest
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.byappsoft.sap.launcher.Sap_act_main_launcher
import com.byappsoft.sap.utils.Sap_Func

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 안드로이드 13 이상 알림권한 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!checkPermission()) {
                requestSapPermissions()
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

        val txt = findViewById<TextView>(R.id.txt)
        txt.text = "Package : " + baseContext.packageName

    }

    override fun onResume() {
        super.onResume()
        // TODO-- huvleView apply
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkPermission()) {
                huvleView()
            }
        } else {
            huvleView()
        }

    }

    private fun huvleView() {
        Sap_Func.setNotiBarLockScreen(this,false)
        Sap_act_main_launcher.initsapStart(this,"bynetwork",true,true,
            object : Sap_act_main_launcher.OnLauncher {
                override fun onDialogOkClicked() {
                    checkDrawOverlayPermission()
                }

                override fun onDialogCancelClicked() {
                }

                override fun onInitSapStartapp() {
                }

                override fun onUnknown() {
                }

            })
    }

    private fun checkDrawOverlayPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true
        }
        return if (!Settings.canDrawOverlays(this)) {
            AlertDialog.Builder(this)
                .setTitle("다른앱 위에 그리기")
                .setMessage("다른 앱 위에 그리기 권한을 허용해주세요.")
                .setPositiveButton("확인") { dialog, which ->
                    val intent = Intent()
                    intent.action = Settings.ACTION_MANAGE_OVERLAY_PERMISSION
                    val uri = Uri.parse("package:$packageName")
                    intent.data = uri
                    startActivity(intent)
                }
                .setNegativeButton(
                    "취소"
                ) { dialog, which -> dialog.cancel() }
                .create()
                .show()
            false
        } else {
            true
        }
    }

    private fun checkPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else true
    }

    private fun requestSapPermissions() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) requestPermissions(
                arrayOf(
                    Manifest.permission.POST_NOTIFICATIONS
                ), 0
            )
        } catch (ignored: Exception) {
        }
    }

}