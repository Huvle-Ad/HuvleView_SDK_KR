# Huvle SDK — Flutter 연동 가이드

> 이 문서는 Flutter 앱에 Huvle SDK(HuvleView)를 연동하는 방법을 설명합니다.
> 네이티브 Android 가이드는 프로젝트 루트의 [README.md](../README.md)를 참고하세요.

---

## 목차

1. [Gradle 설정 (SDK 추가)](#1-gradle-설정-sdk-추가)
2. [권한 및 매니페스트 설정](#2-권한-및-매니페스트-설정)
3. [권한 요청 및 SDK 초기화](#3-권한-요청-및-sdk-초기화-mainactivityjava)
4. [Flutter ↔ Android 통신 (MethodChannel)](#4-flutter--android-통신-methodchannel)
5. [Flutter UI 구현](#5-flutter-ui-구현-maindart)
6. [노티바 커스텀 (선택사항)](#6-노티바-커스텀-선택사항)
7. [빌드 및 실행](#7-빌드-및-실행)

---

## 1. Gradle 설정 (SDK 추가)

### 1) `android/settings.gradle`

Flutter 플러그인 및 AGP 버전을 설정합니다.

```groovy
pluginManagement {
    def flutterSdkPath = {
        def properties = new Properties()
        file("local.properties").withInputStream { properties.load(it) }
        def flutterSdkPath = properties.getProperty("flutter.sdk")
        assert flutterSdkPath != null, "flutter.sdk not set in local.properties"
        return flutterSdkPath
    }()

    includeBuild("$flutterSdkPath/packages/flutter_tools/gradle")

    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

plugins {
    id "dev.flutter.flutter-plugin-loader" version "1.0.0"
    id "com.android.application" version "8.7.1" apply false
}

include ':app'
```

### 2) `android/build.gradle`

Huvle 전용 메이븐 저장소를 추가합니다.

```groovy
allprojects {
    repositories {
        google()
        // Huvle SDK Repository
        maven {
            name "Huvle"
            url "https://sdk.huvle.com/repository/internal"
        }
    }
}
```

### 3) `android/app/build.gradle`

라이브러리 의존성을 추가합니다.

```groovy
plugins {
    id 'com.android.application'
    id 'dev.flutter.flutter-gradle-plugin'
}

android {
    namespace "com.huvle.huvlesdk.huvleflutter"
    compileSdk 36

    defaultConfig {
        applicationId "com.huvle.huvlesdk.huvleflutter"
        minSdk 23
        targetSdk 35
        multiDexEnabled true
    }
}

dependencies {
    // Huvle SDK & Dependencies
    implementation 'com.byappsoft.sap:HuvleSDK:6.3.0.2'
    implementation 'com.google.android.gms:play-services-ads-identifier:18.0.1'
    implementation 'androidx.activity:activity:1.8.0'
    implementation 'androidx.multidex:multidex:2.0.1'
}
```

### 4) `android/gradle/wrapper/gradle-wrapper.properties`

```properties
distributionUrl=https\://services.gradle.org/distributions/gradle-8.9-bin.zip
```

---

## 2. 권한 및 매니페스트 설정

### `AndroidManifest.xml`

```xml
<!-- android/app/src/main/AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools"
    package="com.huvle.huvlesdk.huvleflutter">

    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
    <uses-permission android:name="com.google.android.gms.permission.AD_ID" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />

    <application
        android:networkSecurityConfig="@xml/network_security_config"
        tools:replace="android:networkSecurityConfig"
        tools:targetApi="n">

        <activity android:name=".MainActivity" ... />

        <!-- Huvle NotiBar Service (SDK 선언과 병합) -->
        <service
            android:name="com.byappsoft.sap.service.HuvleNotiBarService"
            android:exported="true"
            android:foregroundServiceType="specialUse"
            tools:node="merge">
            <property
                android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
                android:value="notification_bar"
                tools:replace="android:value" />
        </service>

    </application>
</manifest>
```

> v6.3.0.1부터 `HuvleNotiBarService`가 SDK manifest에 내장되어 있습니다.
> 신규 연동 시에는 service 블록 없이도 동작하며, 기존 연동 시에는 `tools:node="merge"` + `tools:replace="android:value"`를 사용하세요.

### 네트워크 보안 설정 (`network_security_config.xml`)

```xml
<!-- android/app/src/main/res/xml/network_security_config.xml -->
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="true" />
</network-security-config>
```

---

## 3. 권한 요청 및 SDK 초기화 (`MainActivity.java`)

앱 실행 시 **알림 → 다른 앱 위에 그리기 → 배터리 최적화 제외** 순서대로 권한을 요청하고 SDK를 초기화합니다.
Flutter와의 통신을 위해 **MethodChannel**도 함께 설정합니다.

> ⚠️ **SDK 초기화 위치 — 필수 적용**
> **SDK는 반드시 앱 실행 후 사용자에게 가장 먼저 표시되는 화면(Main Activity)의 `onResume()`에서 초기화해야 합니다.**
> 스플래시 등 별도 런처 화면이 있더라도 **실질적인 첫 메인 화면**의 `onResume()`에 적용해야 올바르게 동작합니다.

```java
// android/app/src/main/java/.../MainActivity.java
public class MainActivity extends FlutterActivity {

    private static final String CHANNEL = "com.huvle.sdk/huvle";
    private static final int REQ_PERMISSION_POST_NOTIFICATION = 1001;
    private boolean isPermissionFlowDone = false;

    @Override
    public void configureFlutterEngine(@NonNull FlutterEngine flutterEngine) {
        GeneratedPluginRegistrant.registerWith(flutterEngine);

        // Flutter ↔ Android MethodChannel 설정
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
                            intent.putExtra(Sap_BrowserActivity.PARAM_OPEN_URL,
                                "https://www.huvle.com/global_set.php");
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

        // [Step 1] 알림 권한 (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!checkPostNotificationPermission()) {
                requestPostNotificationPermission();
            }
        }

        // [Step 2] 오버레이 → 배터리 최적화 권한 플로우
        startPermissionFlow();
    }

    // 앱 실행 후 첫 번째로 표시되는 화면(Main Activity)의 onResume에서 초기화
    @Override
    public void onResume() {
        super.onResume();
        // Android 14+ 서비스 상태 갱신 *필수
        if (Build.VERSION.SDK_INT >= 34) {
            Sap_Func.setServiceState(this, true);
        }
        huvleView();
    }

    public void huvleView() {
        Sap_Func.setNotiBarLockScreen(this, false);
        Sap_act_main_launcher.initsapStart(this, "bynetwork", true, true,
            new Sap_act_main_launcher.OnLauncher() {
                @Override public void onDialogOkClicked() { checkDrawOverlayPermission(); }
                @Override public void onDialogCancelClicked() { }
                @Override public void onInitSapStartapp() { }
                @Override public void onUnknown() { }
            });
    }

    // ... 권한 플로우 (checkDrawOverlayPermission, checkBatteryOptimizationPermission 등)
}
```

> 전체 소스 코드는 샘플 프로젝트의
> `android/app/src/main/java/com/huvle/huvlesdk/huvleflutter/MainActivity.java`를 참고하세요.

---

## 4. Flutter ↔ Android 통신 (MethodChannel)

Flutter에서 Android 네이티브 기능을 호출하기 위해 `MethodChannel`을 사용합니다.

| 메서드명 | 동작 | Android API |
|---------|------|-------------|
| `notiUpdate` | 노티바 ON / 업데이트 | `Sap_Func.notiUpdate()` |
| `notiCancel` | 노티바 OFF | `Sap_Func.notiCancel()` |
| `openBrowser` | Huvle 브라우저 열기 | `Sap_MainActivity` 실행 |

**채널명**: `com.huvle.sdk/huvle`

---

## 5. Flutter UI 구현 (`main.dart`)

```dart
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';

void main() {
  runApp(const MyApp());
}

class MyApp extends StatelessWidget {
  const MyApp({Key? key}) : super(key: key);

  @override
  Widget build(BuildContext context) {
    return MaterialApp(
      title: 'HuvleSDK Flutter',
      theme: ThemeData(primarySwatch: Colors.blue),
      home: const HuvleSamplePage(),
    );
  }
}

class HuvleSamplePage extends StatelessWidget {
  const HuvleSamplePage({Key? key}) : super(key: key);

  static const _channel = MethodChannel('com.huvle.sdk/huvle');

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      appBar: AppBar(title: const Text('Huvle SDK Sample'), centerTitle: true),
      body: Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            // 노티바 ON
            ElevatedButton(
              onPressed: () => _channel.invokeMethod('notiUpdate'),
              style: ElevatedButton.styleFrom(backgroundColor: Colors.green),
              child: const Text('Huvle ON / Update'),
            ),
            const SizedBox(height: 16),
            // 노티바 OFF
            ElevatedButton(
              onPressed: () => _channel.invokeMethod('notiCancel'),
              style: ElevatedButton.styleFrom(backgroundColor: Colors.red),
              child: const Text('Huvle OFF'),
            ),
            const SizedBox(height: 16),
            // 브라우저 열기
            ElevatedButton(
              onPressed: () => _channel.invokeMethod('openBrowser'),
              style: ElevatedButton.styleFrom(backgroundColor: Colors.blue),
              child: const Text('Open Huvle Browser'),
            ),
          ],
        ),
      ),
    );
  }
}
```

---

## 6. 노티바 커스텀 (선택사항)

### `CustomNotibarConfig.java`

`android/app/src/main/java/com/byappsoft/sap/CustomNotibarConfig.java` 파일을 생성합니다.
SDK가 리플렉션으로 `com.byappsoft.sap.CustomNotibarConfig`를 탐색하므로 **패키지 경로가 정확히 일치**해야 합니다.

```java
package com.byappsoft.sap;

import android.app.Activity;
import com.byappsoft.sap.launcher.NotibarConfig;

public class CustomNotibarConfig extends NotibarConfig {

    // 노티바 아이콘 (1~5, drawable 리소스 ID)
    @Override
    public int getNotibarIcon1() {
        return R.drawable.your_noti_icon;
    }

    // 노티바 텍스트 (1~5, string 리소스 ID)
    @Override
    public int getNotibarString1() {
        return R.string.your_custom_string;
    }

    // 노티바 버튼 클릭 동작 (1~5)
    @Override
    public void callNotibar1(Activity activity, String nt) {
        // 노티바 클릭 시 실행할 동작
    }
}
```

---

## 7. 빌드 및 실행

```bash
# 프로젝트 디렉토리로 이동
cd Huvleflutter

# Flutter 환경 확인
flutter doctor

# 의존성 설치
flutter pub get

# 연결된 기기에 빌드 & 실행
flutter run

# Release APK 빌드
flutter build apk --release

# AAB 빌드 (Google Play 업로드용)
flutter build appbundle --release

# 빌드 에러 시 캐시 정리
flutter clean
flutter pub get
cd android && ./gradlew clean && cd ..
flutter run
```

---

## License

Huvle SDK의 저작권은 (주)허블에 있습니다.

```
Huvle SDK Android
Copyright 2021-present Huvle Corp.

Unauthorized use, modification and redistribution of this software are strongly prohibited.
```
