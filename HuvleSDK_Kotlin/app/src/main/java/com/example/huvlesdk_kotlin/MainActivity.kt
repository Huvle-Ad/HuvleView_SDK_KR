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
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.byappsoft.sap.api.HuvleConfig
import com.byappsoft.sap.api.HuvleSDK
import com.byappsoft.sap.browser.Sap_BrowserActivity
import com.byappsoft.sap.browser.Sap_MainActivity
import com.byappsoft.sap.launcher.Sap_act_main_launcher
import com.byappsoft.sap.utils.Sap_Func
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_POST_NOTIFICATIONS = 1001
    }

    // 권한 요청 플로우 중복 실행 방지 플래그
    // onResume()이 설정화면 복귀 시에도 호출되기 때문에 필요
    private var isPermissionFlowDone = false

    private val overlayPermissionLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            // 오버레이 설정 복귀 후 배터리 최적화만 확인
            checkBatteryOptimizationPermission()
        }

    private val batteryOptimizationLauncher: ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 안드로이드 13 (TIRAMISU) 이상 알림 권한 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!checkPostNotificationPermission()) {
                requestPostNotificationPermission()
            }
        }

        startPermissionFlow()

        findViewById<Button>(R.id.noti_on_btn).setOnClickListener {
            Sap_Func.notiUpdate(applicationContext)
        }
        findViewById<Button>(R.id.noti_off_btn).setOnClickListener {
            Sap_Func.notiCancel(applicationContext)
        }
        findViewById<Button>(R.id.inapp_browser_btn).setOnClickListener {
            val intent = Intent(this, Sap_MainActivity::class.java).apply {
                putExtra(Sap_BrowserActivity.PARAM_OPEN_URL, "https://www.huvle.com/global_set.php")
            }
            startActivity(intent)
        }

        val txtPackageName = findViewById<TextView>(R.id.txt)
        txtPackageName.text = "Package : ${baseContext.packageName}"
    }

    override fun onResume() {
        super.onResume()
        // Android 14+ 서비스 상태 갱신
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Sap_Func.setServiceState(this, true)
        }
        huvleView()
    }

    private fun huvleView() {
        lifecycleScope.launch {
            HuvleSDK.initialize(
                context = this@MainActivity,
                agencyKey = "bynetwork",  // agent.huvle.com 에서 등록한 에이전트 키
                config = HuvleConfig(
                    enableNotification = false,  // 노티바 사용 여부
                    enableUrlSearch = true      // URL 검색 사용 여부
                )
            )
        }
    }

    // 권한 플로우 진입점 - 중복 실행 방지 후 오버레이 권한 확인
    private fun startPermissionFlow() {
        if (!isPermissionFlowDone) {
            isPermissionFlowDone = true
            checkDrawOverlayPermission()
        }
    }

    private fun checkDrawOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            AlertDialog.Builder(this).apply {
                setTitle("다른 앱 위에 그리기")
                setMessage("원활한 서비스 제공을 위해 '다른 앱 위에 그리기' 권한이 필요합니다.")
                setPositiveButton("설정으로 이동") { _, _ ->
                    overlayPermissionLauncher.launch(
                        Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION).apply {
                            data = Uri.parse("package:$packageName")
                        }
                    )
                }
                setNegativeButton("취소") { _, _ ->
                    Toast.makeText(this@MainActivity, "권한이 거부되어 일부 기능이 제한될 수 있습니다.", Toast.LENGTH_SHORT).show()
                    checkBatteryOptimizationPermission()
                }
                setCancelable(false)
            }.create().show()
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
                    batteryOptimizationLauncher.launch(
                        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    )
                }
            }
            setNegativeButton("취소") { _, _ ->
                Toast.makeText(this@MainActivity, "권한이 거부되어 일부 기능이 제한될 수 있습니다.", Toast.LENGTH_SHORT).show()
            }
            setCancelable(false)
        }.create().show()
    }

    private fun checkPostNotificationPermission(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else true

    private fun requestPostNotificationPermission() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQUEST_CODE_POST_NOTIFICATIONS
                )
            }
        } catch (e: Exception) { }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }
}
