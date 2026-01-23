# Huvle SDK React Native Integration Guide

이 문서는 React Native 애플리케이션에 Huvle SDK(HuvleView)를 연동하는 방법을 설명합니다.

---

## 1. Gradle 설정 (SDK 추가)

### 1) 프로젝트 수준의 `build.gradle`
Huvle 전용 메이븐 저장소를 추가합니다.

```gradle
// android/build.gradle
allprojects {
    repositories {
        google()
        mavenCentral()
        // Huvle SDK Repository 추가
        maven {
            name "Huvle"
            url "https://sdk.huvle.com/repository/internal"
        }
    }
}
```

### 2) 앱 수준의 `build.gradle`
라이브러리 의존성을 추가합니다.

```gradle
// android/app/build.gradle
dependencies {
    implementation 'com.facebook.react:react-android'
    
    // Huvle SDK & Dependencies
    implementation 'com.byappsoft.sap:HuvleSDK:lastVersion'
    implementation 'com.google.android.gms:play-services-ads-identifier:18.0.1'
    implementation 'androidx.activity:activity:1.8.0'
}
```

---

## 2. 권한 및 매니페스트 설정

### 1) `AndroidManifest.xml` 권한 추가
필수 권한과 네트워크 보안 설정을 추가합니다.

```xml
<!-- android/app/src/main/AndroidManifest.xml -->
<manifest xmlns:android="http://schemas.android.com/apk/res/android">

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
      android:usesCleartextTraffic="true"
      android:networkSecurityConfig="@xml/network">

      <!-- Huvle NotiBar Service -->
      <service
          android:name="com.byappsoft.sap.service.HuvleNotiBarService"
          android:exported="true"
          android:foregroundServiceType="specialUse">
          <property android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
              android:value="explanation_for_special_use"/>
      </service>

    </application>
</manifest>
```

### 2) 네트워크 보안 설정 (`network.xml`)
개발 시 Metro 서버(localhost) 및 API 통신을 허용하기 위한 설정입니다. 

> [!TIP]
> **개발 vs 배포**: 이 설정은 앱사의 서버 환경에 따라 도메인을 추가/변경할 수 있습니다. 보안상 배포(Production) 시점에는 필요한 도메인만 남기거나, 공문서 가이드에 따라 파일 자체를 제외해도 무방합니다. (단, 개발 중에는 Metro 연결을 위해 `localhost` 허용이 필수입니다.)

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

앱 실행 시 알림 → 다른 앱 위에 그리기 → 배터리 최적화 제외 순서대로 권한을 요청하고 SDK를 초기화합니다.

```kotlin
// android/app/src/main/java/.../MainActivity.kt
class MainActivity : ReactActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        checkNotificationPermission() // 1. 알림 권한부터 시작
    }

    private fun checkNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1001)
                return
            }
        }
        checkDrawOverlayPermission()
    }

    private fun checkDrawOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !Settings.canDrawOverlays(this)) {
            val intent = Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:$packageName"))
            startActivityForResult(intent, 1002)
        } else {
            requestIgnoreBatteryOptimizations()
        }
    }

    private fun requestIgnoreBatteryOptimizations() {
        val pm = getSystemService(POWER_SERVICE) as android.os.PowerManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !pm.isIgnoringBatteryOptimizations(packageName)) {
            val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, Uri.parse("package:$packageName"))
            startActivity(intent)
        }
        initHuvleSDK()
    }

    private fun initHuvleSDK() {
        try {
            // Huvle SDK 실행 초기화
            Sap_act_main_launcher.initsapStart(this, "bynetwork", false, true)
        } catch (e: Exception) { e.printStackTrace() }
    }
}
```

---

## 4. 로직 구현 (Huvle ON/OFF & 브라우저 열기)

리액트 네이티브에서 호출 가능한 네이티브 모듈을 구현합니다.

### 1) 네이티브 모듈 (`BrowserModule.kt`)

```kotlin
@ReactModule(name = "BrowserModule")
class BrowserModule(reactContext: ReactApplicationContext) : ReactContextBaseJavaModule(reactContext) {
    override fun getName(): String = "BrowserModule"

    @ReactMethod // Huvle ON / Update
    fun openNotificationSettings() {
        val context = reactApplicationContext
        context.runOnUiQueueThread { Sap_Func.notiUpdate(context) }
    }

    @ReactMethod // Huvle OFF
    fun turnOffNotification() {
        val context = reactApplicationContext
        context.runOnUiQueueThread { Sap_Func.notiCancel(context) }
    }

    @ReactMethod // Huvle Browser Open
    fun openSapMainActivity() {
        val context = reactApplicationContext
        val url = "https://www.huvle.com/global_set.php"
        val intent = Intent(context, Sap_MainActivity::class.java).apply {
            putExtra(Sap_BrowserActivity.PARAM_OPEN_URL, url)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
```

### 2) React Native 호출 (`App.tsx`)

```tsx
import { NativeModules, TouchableOpacity, Text } from 'react-native';
const { BrowserModule } = NativeModules;

const App = () => {
  return (
    <>
      <TouchableOpacity onPress={() => BrowserModule.openNotificationSettings()}>
        <Text>Huvle ON</Text>
      </TouchableOpacity>
      <TouchableOpacity onPress={() => BrowserModule.turnOffNotification()}>
        <Text>Huvle OFF</Text>
      </TouchableOpacity>
      <TouchableOpacity onPress={() => BrowserModule.openSapMainActivity()}>
        <Text>Open Huvle Browser</Text>
      </TouchableOpacity>
    </>
  );
};
```

---

## 5. 노티바 커스텀 (아이콘 및 문구 변경)

### 1) 레이아웃 오버라이딩
`android/app/src/main/res/layout/lay_sap_act_noti.xml` 파일을 생성하여 SDK 내부의 레이아웃을 덮어씁니다. (파일명 일치 필수)

### 2) 아이콘 변경 (`notibar_icon_6` 등)
`res/drawable` 폴더 혹은 `drawable-xhdpi` 등 해상도별 폴더에 동일한 이름(`notibar_icon_6.png` 등)의 이미지 파일을 넣으면 자동으로 교체됩니다.

### 3) 텍스트 변경 (`strings.xml`)
시스템 언어 설정을 고려하여 `values-ko` 폴더를 사용하는 것이 권장됩니다.

```xml
<!-- res/values-ko/strings.xml (한국어 기기 우선 적용) -->
<resources>
    <string name="noti_icon_web_search">빠른 뉴스 (커스텀)</string>
    <string name="noti_icon_1">전화</string>
    <!-- 나머지 아이콘 텍스트들... -->
</resources>
```

### 4) 적용이 안 될 때 (Clean Build)
리소스 변경 후에는 반드시 클린 빌드가 필요합니다.
```batch
cd android
gradlew clean
cd ..
npx react-native run-android
```
