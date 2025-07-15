package com.huvle.huvlesdk;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.byappsoft.sap.launcher.Sap_act_main_launcher;
import com.byappsoft.sap.utils.Sap_Func;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 0;

    private ActivityResultLauncher<Intent> overlayPermissionLauncher;
    private ActivityResultLauncher<Intent> batteryOptimizationLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeLaunchers();

        // 안드로이드 13 이상 알림권한 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!checkPermission()) {
                requestSapPermissions();
            }
        }

        //-- Notification On/Off 버튼 리스너
        findViewById(R.id.noti_on_btn).setOnClickListener(v -> Sap_Func.notiUpdate(getApplicationContext()));
        findViewById(R.id.noti_off_btn).setOnClickListener(v -> Sap_Func.notiCancel(getApplicationContext()));

        TextView txt = findViewById(R.id.txt);
        txt.setText("Package : " + getPackageName());
    }

    @Override
    public void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= 34) {
            Sap_Func.setServiceState(this, true);
        }
        huvleView();
    }

    private void initializeLaunchers() {
        overlayPermissionLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(
                            this::checkBatteryOptimizationPermission, 500);
                }
        );

        batteryOptimizationLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if(isIgnoringBatteryOptimizations()) {
                        Toast.makeText(this, "배터리 최적화 제외 설정이 완료되었습니다.", Toast.LENGTH_SHORT).show();
                    }
                }
        );
    }

    public void huvleView() {
        Sap_Func.setNotiBarLockScreen(this, false);
        Sap_act_main_launcher.initsapStart(this, "bynetwork", true, true, new Sap_act_main_launcher.OnLauncher() {
            @Override
            public void onDialogOkClicked() {
                checkDrawOverlayPermission();
            }
            @Override
            public void onDialogCancelClicked() {}
            @Override
            public void onInitSapStartapp() {}
            @Override
            public void onUnknown() {}
        });
    }

    public void checkDrawOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("다른 앱 위에 그리기")
                    .setMessage("원활한 서비스 제공을 위해 '다른 앱 위에 그리기' 권한이 필요합니다.")
                    .setPositiveButton("설정으로 이동", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName()));
                        overlayPermissionLauncher.launch(intent); // Launcher로 설정 화면 이동
                    })
                    .setNegativeButton("취소", (dialog, which) -> {
                        Toast.makeText(this, "권한이 거부되어 일부 기능이 제한될 수 있습니다.", Toast.LENGTH_SHORT).show();
                        checkBatteryOptimizationPermission();
                    })
                    .setCancelable(false)
                    .show();
        } else {
            checkBatteryOptimizationPermission();
        }
    }

    private void checkBatteryOptimizationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                requestIgnoreBatteryOptimizationsDialog();
            }
        }
    }

    @SuppressLint("BatteryLife")
    private void requestIgnoreBatteryOptimizationsDialog() {
        if (isFinishing() || isDestroyed()) return;
        new AlertDialog.Builder(this)
                .setTitle("배터리 사용량 최적화 제외")
                .setMessage("앱의 안정적인 백그라운드 동작을 위해 '배터리 사용량 최적화' 목록에서 이 앱을 '최적화 안 함'으로 설정해야 합니다.")
                .setPositiveButton("설정으로 이동", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    try {
                        batteryOptimizationLauncher.launch(intent);
                    } catch (Exception e) {
                        // 일부 기기에서 위 인텐트가 없을 경우, 전체 목록 화면으로 안내
                        Intent generalIntent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                        batteryOptimizationLauncher.launch(generalIntent);
                    }
                })
                .setNegativeButton("취소", (dialog, which) -> {
                    Toast.makeText(this, "권한이 거부되어 일부 기능이 제한될 수 있습니다.", Toast.LENGTH_SHORT).show();
                })
                .setCancelable(false)
                .show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_CODE_POST_NOTIFICATIONS) {
        }
    }

    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestSapPermissions() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_CODE_POST_NOTIFICATIONS);
            }
        } catch (Exception ignored) {
        }
    }

    private boolean isIgnoringBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            return pm != null && pm.isIgnoringBatteryOptimizations(getPackageName());
        }
        return true;
    }
}