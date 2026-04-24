package com.huvle.huvlesdk.huvleflutter;

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
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.byappsoft.sap.browser.Sap_BrowserActivity;
import com.byappsoft.sap.browser.Sap_MainActivity;
import com.byappsoft.sap.launcher.Sap_act_main_launcher;
import com.byappsoft.sap.utils.Sap_Func;

import io.flutter.embedding.android.FlutterActivity;
import io.flutter.embedding.engine.FlutterEngine;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugins.GeneratedPluginRegistrant;

public class MainActivity extends FlutterActivity {

    private static final String CHANNEL = "com.huvle.sdk/huvle";
    private static final int REQ_PERMISSION_POST_NOTIFICATION = 1001;

    // 권한 요청 플로우 중복 실행 방지
    private boolean isPermissionFlowDone = false;

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        GeneratedPluginRegistrant.registerWith(flutterEngine);

        new MethodChannel(flutterEngine.getDartExecutor().getBinaryMessenger(), CHANNEL)
                .setMethodCallHandler((call, result) -> {
                    switch (call.method) {
                        case "notiUpdate":
                            Sap_Func.notiUpdate(getApplicationContext());
                            result.success(null);
                            break;
                        case "notiCancel":
                            Sap_Func.notiCancel(getApplicationContext());
                            result.success(null);
                            break;
                        case "openBrowser":
                            Intent intent = new Intent(this, Sap_MainActivity.class);
                            intent.putExtra(Sap_BrowserActivity.PARAM_OPEN_URL, "https://www.huvle.com/global_set.php");
                            startActivity(intent);
                            result.success(null);
                            break;
                        default:
                            result.notImplemented();
                            break;
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // [Step 1] 알림 권한 확인 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!checkPostNotificationPermission()) {
                requestPostNotificationPermission();
            }
        }

        // [Step 2] 오버레이 → 배터리 최적화 권한 플로우
        startPermissionFlow();
    }

    @Override
    public void onResume() {
        super.onResume();
        // Android 14+ 서비스 상태 갱신
        if (Build.VERSION.SDK_INT >= 34) {
            Sap_Func.setServiceState(this, true);
        }
        huvleView();
    }

    public void huvleView() {
        Sap_Func.setNotiBarLockScreen(this, false);
        Sap_act_main_launcher.initsapStart(this, "bynetwork", true, true, new Sap_act_main_launcher.OnLauncher() {
            @Override
            public void onDialogOkClicked() {
                checkDrawOverlayPermission();
            }

            @Override
            public void onDialogCancelClicked() {
            }

            @Override
            public void onInitSapStartapp() {
            }

            @Override
            public void onUnknown() {
            }
        });
    }

    // ── 권한 플로우 ──

    private void startPermissionFlow() {
        if (!isPermissionFlowDone) {
            isPermissionFlowDone = true;
            checkDrawOverlayPermission();
        }
    }

    private void checkDrawOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("다른 앱 위에 그리기")
                    .setMessage("원활한 서비스 제공을 위해 '다른 앱 위에 그리기' 권한이 필요합니다.")
                    .setPositiveButton("설정으로 이동", (dialog, which) -> {
                        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                                Uri.parse("package:" + getPackageName()));
                        startActivityForResult(intent, 100);
                    })
                    .setNegativeButton("취소", (dialog, which) -> {
                        Toast.makeText(this, "권한이 거부되어 일부 기능이 제한될 수 있습니다.", Toast.LENGTH_SHORT).show();
                        checkBatteryOptimizationPermission();
                    })
                    .setCancelable(false)
                    .create()
                    .show();
        } else {
            checkBatteryOptimizationPermission();
        }
    }

    @SuppressLint("BatteryLife")
    private void checkBatteryOptimizationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                new AlertDialog.Builder(this)
                        .setTitle("배터리 사용량 최적화 제외")
                        .setMessage("앱의 안정적인 백그라운드 동작을 위해 배터리 최적화를 해제해 주세요.")
                        .setPositiveButton("설정으로 이동", (dialog, which) -> {
                            try {
                                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                                        Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                            } catch (Exception e) {
                                Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                                startActivity(intent);
                            }
                        })
                        .setNegativeButton("취소", (dialog, which) ->
                                Toast.makeText(this, "권한이 거부되어 일부 기능이 제한될 수 있습니다.", Toast.LENGTH_SHORT).show())
                        .setCancelable(false)
                        .create()
                        .show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100) {
            // 오버레이 설정 복귀 후 배터리 최적화 확인
            checkBatteryOptimizationPermission();
        }
    }

    // ── 알림 권한 ──

    private boolean checkPostNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestPostNotificationPermission() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQ_PERMISSION_POST_NOTIFICATION);
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
