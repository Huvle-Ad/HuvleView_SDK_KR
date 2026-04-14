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
import com.byappsoft.sap.domain.model.Result
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
            // 오버레이 설정 복귀 후 배터리 최적화만 확인 (initHuvleSDK 재호출 없음)
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
        // Android 14+ 서비스 상태 갱신
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Sap_Func.setServiceState(this, true)
        }
        initHuvleSDK()
    }

    private fun initHuvleSDK() {

        lifecycleScope.launch {
            val result = HuvleSDK.initialize(
                context = this@MainActivity,
                agencyKey = "bynetwork",
                config = HuvleConfig(
                    enableNotification = false,
                    enableUrlSearch = true
                )
            )

            when (result) {
                is Result.Success -> {
                    // 권한 플로우는 최초 1회만 실행
                    // (onResume이 설정화면 복귀 시마다 호출되므로 중복 방지)
                    if (!isPermissionFlowDone) {
                        isPermissionFlowDone = true
                        // [선택] 노티바 동의창을 앱에서 직접 운영하려면 아래 주석을 해제하세요.
                        // 동의 여부는 SharedPreferences 등에 저장해 재기동 시 중복 노출을 막아야 합니다.
                        // showNotificationConsentDialog()
                        checkDrawOverlayPermission()
                    }
                }
                is Result.Error -> { /* 초기화 실패 처리 */ }
                is Result.Loading -> { /* 무시 */ }
            }
        }
    }

    // [선택] 신규 연동 전용 노티바 동의창 샘플
    // SDK가 동의창을 자동으로 띄우지 않으므로 앱이 직접 구현합니다.
    // 사용하려면 위 initHuvleSDK()의 showNotificationConsentDialog() 주석을 해제하세요.
    // 동의 여부는 SharedPreferences 등에 저장해 재기동 시 중복 노출을 막아야 합니다.
    private fun showNotificationConsentDialog() {
        if (isFinishing || isDestroyed) return
        AlertDialog.Builder(this).apply {
            setTitle("알림 서비스")
            setMessage("알림바 서비스를 사용하시겠습니까?")
            setPositiveButton("동의") { _, _ ->
                Sap_Func.notiUpdate(applicationContext)
                checkDrawOverlayPermission()
            }
            setNegativeButton("거부") { _, _ ->
                Sap_Func.notiCancel(applicationContext)
                checkDrawOverlayPermission()
            }
            setCancelable(false)
        }.create().show()
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
