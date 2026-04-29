# Huvle SDK — React Native 연동 가이드

> 이 문서는 React Native 앱에 Huvle SDK(HuvleView)를 연동하는 방법을 설명합니다.
> 네이티브 Android 가이드는 프로젝트 루트의 [README.md](../README.md)를 참고하세요.

---

## 목차

1. [Gradle 설정 (SDK 추가)](#1-gradle-설정-sdk-추가)
2. [권한 및 매니페스트 설정](#2-권한-및-매니페스트-설정)
3. [권한 요청 및 SDK 초기화](#3-권한-요청-및-sdk-초기화-mainactivitykt)
4. [네이티브 모듈 구현 (Huvle ON/OFF & 브라우저)](#4-네이티브-모듈-구현-huvle-onoff--브라우저)
5. [React Native에서 호출](#5-react-native에서-호출-apptsx)
6. [노티바 커스텀 (선택사항)](#6-노티바-커스텀-선택사항)
7. [빌드 및 실행](#7-빌드-및-실행)

---

## 1. Gradle 설정 (SDK 추가)

### 1) 프로젝트 수준 `android/build.gradle`

Huvle 전용 메이븐 저장소와 SDK 빌드 설정을 추가합니다.

```groovy
// android/build.gradle
buildscript {
    ext {
        buildToolsVersion = "35.0.0"
        minSdkVersion = 24
        compileSdkVersion = 36
        targetSdkVersion = 35
        ndkVersion = "26.1.10909125"
        kotlinVersion = "1.9.24"
    }
    repositories {
        google()
        mavenCentral()
    }
    dependencies {
        classpath("com.android.tools.build:gradle")
        classpath("com.facebook.react:react-native-gradle-plugin")
        classpath("org.jetbrains.kotlin:kotlin-gradle-plugin")
    }
}

allprojects {
    repositories {
        google()
        maven {
            name "Huvle"
            url "https://sdk.huvle.com/repository/internal"
        }
        mavenCentral()
    }
}
```

### 2) 앱 수준 `android/app/build.gradle`

라이브러리 의존성을 추가합니다.

```groovy
// android/app/build.gradle
dependencies {
    implementation("com.facebook.react:react-android")

    // Huvle SDK & Dependencies
    implementation 'com.byappsoft.sap:HuvleSDK:6.3.0.2'
    implementation 'com.google.android.gms:play-services-ads-identifier:18.0.1'
    implementation 'androidx.activity:activity:1.8.0'
}
```

---

## 2. 권한 및 매니페스트 설정

### 1) `AndroidManifest.xml` 권한 추가

```xml
<!-- android/app/src/main/AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:tools="http://schemas.android.com/tools">

    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.RECEIVE_BOOT_COMPLETED" />
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <uses-permission android:name="android.permission.SYSTEM_ALERT_WINDOW" />
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
    <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
    <uses-permission android:name="com.google.android.gms.permission.AD_ID" />

    <application
      ...
      android:networkSecurityConfig="@xml/network"
      tools:replace="android:networkSecurityConfig">

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

### 2) 네트워크 보안 설정 (`network.xml`)

개발 시 Metro 서버(localhost) 통신을 허용하기 위한 설정입니다.

```xml
<!-- android/app/src/main/res/xml/network.xml -->
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <domain-config cleartextTrafficPermitted="true">
        <domain includeSubdomains="true">localhost</domain>
        <domain includeSubdomains="true">10.0.2.2</domain>
    </domain-config>
</network-security-config>
```

---

## 3. 권한 요청 및 SDK 초기화 (`MainActivity.kt`)

앱 실행 시 **알림 → 다른 앱 위에 그리기 → 배터리 최적화 제외** 순서대로 권한을 요청하고 SDK를 초기화합니다.

> ⚠️ **SDK 초기화 위치 — 필수 적용**
> **SDK는 반드시 앱 실행 후 사용자에게 가장 먼저 표시되는 화면(Main Activity)의 `onResume()`에서 초기화해야 합니다.**
> 스플래시 등 별도 런처 화면이 있더라도 **실질적인 첫 메인 화면**의 `onResume()`에 적용해야 올바르게 동작합니다.

```kotlin
// android/app/src/main/java/.../MainActivity.kt
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
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                    this, arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    REQ_PERMISSION_POST_NOTIFICATION
                )
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

    // [Step 2] 다른 앱 위에 그리기 권한
    private fun checkDrawOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, REQ_PERMISSION_OVERLAY)
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

    // [Step 3] 배터리 최적화 제외
    private fun requestIgnoreBatteryOptimizations() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val pm = getSystemService(POWER_SERVICE) as android.os.PowerManager
            if (!pm.isIgnoringBatteryOptimizations(packageName)) {
                try {
                    val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS,
                        Uri.parse("package:$packageName"))
                    startActivity(intent)
                } catch (e: Exception) { e.printStackTrace() }
            }
        }
        initHuvleSDK()
    }

    // [Step 4] Huvle SDK 초기화
    private fun initHuvleSDK() {
        try {
            Sap_act_main_launcher.initsapStart(this, "bynetwork", false, true)
        } catch (e: Exception) { e.printStackTrace() }
    }

    // 앱 실행 후 첫 번째로 표시되는 화면(Main Activity)의 onResume에서 초기화
    override fun onResume() {
        super.onResume()
        // Android 14+ 서비스 상태 갱신 *필수
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Sap_Func.setServiceState(this, true)
        }
        initHuvleSDK()
    }

    override fun getMainComponentName(): String = "huvleSDKreactSample"

    override fun createReactActivityDelegate(): ReactActivityDelegate =
        DefaultReactActivityDelegate(this, mainComponentName, fabricEnabled)
}
```

---

## 4. 네이티브 모듈 구현 (Huvle ON/OFF & 브라우저)

React Native에서 호출 가능한 네이티브 브릿지 모듈을 구현합니다.

### 1) `HuvleViewModule.kt` (네이티브 모듈)

```kotlin
@ReactModule(name = "BrowserModule")
class BrowserModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    override fun getName(): String = "BrowserModule"

    @ReactMethod  // 노티바 ON / Update
    fun openNotificationSettings() {
        if (checkPermission()) {
            val context = reactApplicationContext
            if (!NotificationManagerCompat.from(context).areNotificationsEnabled()) {
                // 알림이 꺼져있으면 시스템 설정으로 이동
                val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                    }
                } else {
                    Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                        data = Uri.parse("package:${context.packageName}")
                    }
                }
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            } else {
                context.runOnUiQueueThread { Sap_Func.notiUpdate(context) }
            }
        }
    }

    @ReactMethod  // 노티바 OFF
    fun turnOffNotification() {
        reactApplicationContext.runOnUiQueueThread {
            Sap_Func.notiCancel(reactApplicationContext)
        }
    }

    @ReactMethod  // Huvle 브라우저 열기
    fun openSapMainActivity() {
        val context = reactApplicationContext
        val intent = Intent(context, Sap_MainActivity::class.java).apply {
            putExtra(Sap_BrowserActivity.PARAM_OPEN_URL, "https://www.huvle.com/global_set.php")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }

    private fun checkPermission(): Boolean {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                reactApplicationContext, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        }
        return true
    }
}
```

### 2) `HuvleViewPackage.kt` (패키지 등록)

```kotlin
class HuvleViewPackage : ReactPackage {
    override fun createNativeModules(reactContext: ReactApplicationContext): List<NativeModule> {
        return listOf(BrowserModule(reactContext))
    }
    override fun createViewManagers(reactContext: ReactApplicationContext): List<ViewManager<*, *>> {
        return emptyList()
    }
}
```

### 3) `MainApplication.kt`에 패키지 등록

```kotlin
override fun getPackages(): List<ReactPackage> =
    PackageList(this).packages.apply {
        add(HuvleViewPackage())
    }
```

---

## 5. React Native에서 호출 (`App.tsx`)

```tsx
import React from 'react';
import {
  SafeAreaView, StatusBar, StyleSheet, Text,
  TouchableOpacity, View, NativeModules,
} from 'react-native';

const { BrowserModule } = NativeModules;

function App(): React.JSX.Element {
  return (
    <SafeAreaView style={styles.container}>
      <StatusBar barStyle="dark-content" backgroundColor="#FFFFFF" />
      <View style={styles.header}>
        <Text style={styles.title}>Huvle SDK Sample</Text>
      </View>
      <View style={styles.content}>
        <TouchableOpacity style={[styles.button, { backgroundColor: '#388E3C' }]}
          onPress={() => BrowserModule.openNotificationSettings()}>
          <Text style={styles.buttonText}>Huvle ON / Update</Text>
        </TouchableOpacity>

        <TouchableOpacity style={[styles.button, { backgroundColor: '#D32F2F' }]}
          onPress={() => BrowserModule.turnOffNotification()}>
          <Text style={styles.buttonText}>Huvle OFF</Text>
        </TouchableOpacity>

        <TouchableOpacity style={[styles.button, { backgroundColor: '#1976D2' }]}
          onPress={() => BrowserModule.openSapMainActivity()}>
          <Text style={styles.buttonText}>Open Huvle Browser</Text>
        </TouchableOpacity>
      </View>
    </SafeAreaView>
  );
}
```

---

## 6. 노티바 커스텀 (선택사항)

### 1) `CustomNotibarConfig` 클래스

`android/app/src/main/java/com/byappsoft/sap/CustomNotibarConfig.kt` 파일을 생성합니다.
SDK가 리플렉션으로 `com.byappsoft.sap.CustomNotibarConfig`를 탐색하므로 **패키지 경로가 정확히 일치**해야 합니다.

### 2) 아이콘/텍스트 변경

`res/drawable` 폴더에 아이콘 이미지를, `res/values/strings.xml`에 텍스트를 추가합니다.
시스템 언어를 고려하여 `values-ko` 폴더를 사용하는 것을 권장합니다.

### 3) 다크모드 대응

**`res/values/styles.xml`**
```xml
<style name="HuvleStatusbar" parent="@android:style/TextAppearance">
    <item name="android:textColor">@color/black</item>
</style>
```

**`res/values-night/styles.xml`**
```xml
<style name="HuvleStatusbar" parent="@android:style/TextAppearance">
    <item name="android:textColor">@color/white</item>
</style>
```

---

## 7. 빌드 및 실행

```bash
# 프로젝트 디렉토리로 이동
cd HuvleSDK_ReactNative/huvleSDKreactSample

# 의존성 설치
npm install

# Metro 번들러 시작 (터미널 1)
npx react-native start

# Android 빌드 & 실행 (터미널 2)
npx react-native run-android

# 특정 기기 지정 실행
npx react-native run-android --deviceId=DEVICE_ID

# 빌드 에러 시 캐시 정리
cd android && ./gradlew clean && cd ..
npx react-native start --reset-cache
```

---

## License

Huvle SDK의 저작권은 (주)허블에 있습니다.

```
Huvle SDK Android
Copyright 2021-present Huvle Corp.

Unauthorized use, modification and redistribution of this software are strongly prohibited.
```
