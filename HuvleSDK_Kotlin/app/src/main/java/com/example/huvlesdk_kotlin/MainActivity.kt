package com.example.huvlesdk_kotlin

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.byappsoft.sap.launcher.Sap_act_main_launcher
import com.byappsoft.sap.utils.Sap_Func

class MainActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "MainActivity"
        private const val REQUEST_CODE_POST_NOTIFICATIONS = 1001
    }

    // 설정 화면 이동 및 결과 처리를 위한 ActivityResultLauncher
    private lateinit var overlayPermissionLauncher: ActivityResultLauncher<Intent>
    private lateinit var batteryOptimizationLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        initializeLaunchers()

        // 안드로이드 13 (TIRAMISU) 이상 알림 권한 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!hasPostNotificationPermission()) {
                requestPostNotificationPermission()
            }
        }

        findViewById<Button>(R.id.noti_on_btn).setOnClickListener {
            Sap_Func.notiUpdate(applicationContext)
        }
        findViewById<Button>(R.id.noti_off_btn).setOnClickListener {
            Sap_Func.notiCancel(applicationContext)
        }

        val txtPackageName = findViewById<TextView>(R.id.txt)
        txtPackageName.text = "Package : ${baseContext.packageName}"
    }

    override fun onResume() {
        super.onResume()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Sap_Func.setServiceState(this, true)
        }
        huvleView()
    }

    private fun initializeLaunchers() {
        overlayPermissionLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                checkBatteryOptimizationPermission()
            }, 500)
        }

        batteryOptimizationLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) {
            if (isIgnoringBatteryOptimizations()) {
                Toast.makeText(this, "배터리 최적화 제외 설정이 완료되었습니다.", Toast.LENGTH_SHORT).show()
            }
        }
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


    private fun checkDrawOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            requestOverlayPermissionDialog()
        } else {
            checkBatteryOptimizationPermission()
        }
    }

    private fun checkBatteryOptimizationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                requestIgnoreBatteryOptimizationsDialog()
            }
        }
    }

    private fun requestOverlayPermissionDialog() {
        if (isFinishing || isDestroyed) return
        AlertDialog.Builder(this).apply {
            setTitle("다른 앱 위에 그리기")
            setMessage("원활한 서비스 제공을 위해 '다른 앱 위에 그리기' 권한이 필요합니다.")
            setPositiveButton("설정으로 이동") { _, _ ->
                val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                    data = Uri.parse("package:$packageName")
                }
                overlayPermissionLauncher.launch(intent)
            }
            setNegativeButton("취소") { _, _ ->
                Toast.makeText(this@MainActivity, "권한이 거부되어 일부 기능이 제한될 수 있습니다.", Toast.LENGTH_SHORT).show()
                checkBatteryOptimizationPermission()
            }
            setCancelable(false)
        }.create().show()
    }

    @SuppressLint("BatteryLife")
    private fun requestIgnoreBatteryOptimizationsDialog() {
        if (isFinishing || isDestroyed) return
        AlertDialog.Builder(this).apply {
            setTitle("배터리 사용량 최적화 제외")
            setMessage("앱의 안정적인 백그라운드 동작을 위해 '배터리 사용량 최적화' 목록에서 이 앱을 '최적화 안 함'으로 설정해야 합니다.")
            setPositiveButton("설정으로 이동") { _, _ ->
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
                    data = Uri.parse("package:$packageName")
                }
                try {
                    batteryOptimizationLauncher.launch(intent)
                } catch (e: Exception) {
                    val generalIntent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    batteryOptimizationLauncher.launch(generalIntent)
                }
            }
            setNegativeButton("취소") { _, _ ->
                Toast.makeText(this@MainActivity, "권한이 거부되어 일부 기능이 제한될 수 있습니다.", Toast.LENGTH_SHORT).show()
            }
            setCancelable(false)
        }.create().show()
    }


    private fun hasPostNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }

    private fun isIgnoringBatteryOptimizations(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
            return pm.isIgnoringBatteryOptimizations(packageName)
        }
        return true
    }

    private fun requestPostNotificationPermission() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_POST_NOTIFICATIONS
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error requesting POST_NOTIFICATIONS permission", e)
        }
    }


    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.i(TAG, "POST_NOTIFICATIONS permission granted.")
            } else {
                Log.w(TAG, "POST_NOTIFICATIONS permission denied.")
            }
        }
    }
}