package com.huvle.huvlesdk;

import android.Manifest;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;


import androidx.appcompat.app.AppCompatActivity;

import com.byappsoft.sap.launcher.Sap_act_main_launcher;
import com.byappsoft.sap.utils.Sap_Func;


public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate( savedInstanceState );

        setContentView( R.layout.activity_main );

        // 안드로이드 13 이상 알림권한 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if(!checkPermission()){
                requestSapPermissions();
            }
        }

        //-- Notification On Event.
        findViewById(R.id.noti_on_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sap_Func.notiUpdate(getApplicationContext());
            }
        });
        //-- Notification Off Event.
        findViewById(R.id.noti_off_btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Sap_Func.notiCancel(getApplicationContext());
            }
        });


        TextView txt = findViewById(R.id.txt);
        txt.setText("Package : "+getBaseContext().getPackageName());
    }

    @Override
    public void onResume() {
        super.onResume();
        // TODO -- HuvleView apply
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkPermission()) {
                huvleView();
            }
        } else {
            huvleView();
        }
    }

    public void huvleView() {
        Sap_Func.setNotiBarLockScreen(this, false);
        Sap_act_main_launcher.initsapStart(this, "bynetwork", true, true, new Sap_act_main_launcher.OnLauncher() {

            @Override
            public void onDialogOkClicked() { //허블뷰 동의창 확인 후 작업
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

    public boolean checkDrawOverlayPermission() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return true;
        }
        if (!Settings.canDrawOverlays(this)) {
            new AlertDialog.Builder(this)
                    .setTitle("다른앱 위에 그리기")
                    .setMessage("다른 앱 위에 그리기 권한을 허용해주세요.")
                    .setPositiveButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent();
                            intent.setAction(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                            Uri uri = Uri.parse("package:" + getPackageName());
                            intent.setData(uri);
                            startActivity(intent);

                        }
                    })
                    .setNegativeButton("취소", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    })
                    .create()
                    .show();
            return false;
        } else {
            return true;
        }
    }


    private boolean checkPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestSapPermissions() {
        try{
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU)
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 0);
        }catch (Exception ignored){
        }
    }



}
