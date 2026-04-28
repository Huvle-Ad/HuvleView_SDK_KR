# Huvle SDK — Android 연동 가이드

> **최신 버전** 적용을 권장합니다.
> 제휴 신청: https://www.huvleview.com/sub/location.php

---

## 목차

1. [Manifest 설정](#1-manifest-설정)
2. [SDK 추가](#2-sdk-추가) — Kotlin DSL / Groovy DSL
3. [앱에 적용하기](#3-앱에-적용하기)
4. [노티바 ON/OFF 제어](#4-노티바-onoff-제어)
5. [노티바/동의창 커스텀](#5-노티바동의창-커스텀-선택사항)
6. [연동 체크리스트](#6-연동-체크리스트)
7. [자주 발생하는 오류](#7-자주-발생하는-오류)

---

## 1. Manifest 설정

### 기본 권한

```xml
<manifest>
    <!-- 구글 광고 ID -->
    <uses-permission android:name="com.google.android.gms.permission.AD_ID" />
    <!-- 알림 권한 (Target SDK 33+) -->
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
    <!-- 배터리 최적화 제외 -->
    <uses-permission android:name="android.permission.REQUEST_IGNORE_BATTERY_OPTIMIZATIONS" />
</manifest>
```

> SDK 내부에서 `INTERNET`, `ACCESS_NETWORK_STATE`, `FOREGROUND_SERVICE`, `FOREGROUND_SERVICE_SPECIAL_USE` 등을 이미 선언하고 있어 manifest merger를 통해 자동으로 포함됩니다.
> 위 3개 권한은 SDK가 선언하지 않으므로 **앱에서 반드시 추가**해야 합니다.

> 📘 POST_NOTIFICATIONS 상세: [Android developer 문서](https://developer.android.com/develop/ui/views/notifications/notification-permission?hl=en)

---

### Target SDK 34 이상 — ForegroundService 설정

v6.3.0.1부터 `HuvleNotiBarService` 선언이 SDK manifest에 내장되어 자동으로 포함됩니다.
**신규 연동 앱은 manifest에 별도로 서비스를 선언할 필요가 없습니다.**

Google Play 심사 시 `PROPERTY_SPECIAL_USE_FGS_SUBTYPE`의 기능 안내가 필요하며, SDK 기본값은 `notification_bar`(노티바)입니다.

> ⚠️ **이전 버전에서 업그레이드하는 경우**
> 기존에 앱 manifest에 서비스를 직접 선언한 경우, 아래 두 가지 방법 중 하나를 선택하세요.
>
> **방법 1 — 기존 선언 유지** (변경 없이 그대로 사용)
> ```xml
> <uses-permission android:name="android.permission.FOREGROUND_SERVICE" />
> <uses-permission android:name="android.permission.FOREGROUND_SERVICE_SPECIAL_USE" />
>
> <service
>     android:name="com.byappsoft.sap.service.HuvleNotiBarService"
>     android:exported="true"
>     android:foregroundServiceType="specialUse">
>     <property
>         android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
>         android:value="notification_bar" />
> </service>
> ```
>
> **방법 2 — SDK 선언과 병합 방식으로 전환** (권장)
> ```xml
> <service
>     android:name="com.byappsoft.sap.service.HuvleNotiBarService"
>     android:exported="true"
>     android:foregroundServiceType="specialUse"
>     tools:node="merge">
>     <property
>         android:name="android.app.PROPERTY_SPECIAL_USE_FGS_SUBTYPE"
>         android:value="notification_bar"
>         tools:replace="android:value" />
> </service>
> ```

> 📘 ForegroundService 상세: [Android developer 문서](https://developer.android.com/about/versions/14/changes/fgs-types-required?hl=ko#use-cases)

---

### Activity 설정

앱이 항상 올바르게 실행될 수 있도록 `launchMode`와 `clearTaskOnLaunch`를 추가합니다.

```xml
<activity
    android:name=".MainActivity"
    android:launchMode="singleInstance"
    android:clearTaskOnLaunch="true">
```

---

### 네트워크 보안 설정 (HTTP 허용)

Android 9 (API 28)부터 강화된 보안 정책으로 HTTP 접속을 허용하려면 아래 설정이 필요합니다.
광고 이미지, 트래킹 요소, 리소스가 HTTP로 구성될 수 있습니다.

**`res/xml/network_security_config.xml` 생성:**

```xml
<?xml version="1.0" encoding="utf-8"?>
<network-security-config>
    <base-config cleartextTrafficPermitted="true" />
</network-security-config>
```

**`AndroidManifest.xml` application 속성에 추가:**

```xml
<application
    android:networkSecurityConfig="@xml/network_security_config"
    tools:replace="android:networkSecurityConfig">
```

---

## 2. SDK 추가

> **Kotlin DSL (`*.gradle.kts`) / Groovy DSL (`*.gradle`)** 두 방식 모두 지원합니다.
> 프로젝트 방식에 맞는 탭을 참고하세요.

---

### 지원 빌드 환경

| 항목 | 권장/요구 | 비고 |
|------|----------|------|
| **AGP (Android Gradle Plugin)** | **8.5 ~ 8.7.x** | |
| Gradle | 8.7 이상 | AGP 버전에 맞춰 사용 |
| compileSdk | 35 이상 | |
| minSdk | 23 이상 | |
| Kotlin | 1.9.x 이상 (Kotlin 앱) | |
| Material Components | 앱 테마 적용 필수 | `Theme.MaterialComponents.*` / `Theme.Material3.*` 중 하나를 앱 테마로 적용 |

---

### 저장소 추가

#### `settings.gradle.kts` (Kotlin DSL)

```kotlin
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            name = "Huvle"
            url = uri("https://sdk.huvle.com/repository/internal")
        }
    }
}
rootProject.name = "MyApp"
include(":app")
```

#### `settings.gradle` (Groovy DSL)

```groovy
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            name "Huvle"
            url "https://sdk.huvle.com/repository/internal"
        }
    }
}
rootProject.name = "MyApp"
include ':app'
```

---

### 앱 모듈 의존성 추가

#### `app/build.gradle.kts` (Kotlin DSL — Kotlin 앱)

> Kotlin 플러그인 버전은 프로젝트 루트 `build.gradle.kts` 또는 `libs.versions.toml`에서 선언되어 있어야 합니다.

```kotlin
plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
}

android {
    namespace = "com.example.myapp"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.example.myapp"
        minSdk = 23
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            ndk { debugSymbolLevel = "SYMBOL_TABLE" }
        }
        debug {
            ndk { debugSymbolLevel = "SYMBOL_TABLE" }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("com.google.android.gms:play-services-ads-identifier:18.0.1")
    implementation("com.byappsoft.sap:HuvleSDK:$last_version")
    implementation("androidx.activity:activity-ktx:1.8.0")

    // Material 1.12 이상 버전 — SDK UI(노티바 동의창 등)에서 사용하므로 모앱에도 필수
    implementation("com.google.android.material:material:1.12.0")
}
```

#### `app/build.gradle` (Groovy DSL — Java 앱)

```groovy
plugins {
    id 'com.android.application'
}

android {
    namespace 'com.example.myapp'
    compileSdk 35

    defaultConfig {
        applicationId 'com.example.myapp'
        minSdk 23
        targetSdk 35
        versionCode 1
        versionName '1.0'
        testInstrumentationRunner 'androidx.test.runner.AndroidJUnitRunner'
    }

    buildTypes {
        release {
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
            ndk { debugSymbolLevel 'SYMBOL_TABLE' }
        }
        debug {
            ndk { debugSymbolLevel 'SYMBOL_TABLE' }
        }
    }

    compileOptions {
        sourceCompatibility JavaVersion.VERSION_17
        targetCompatibility JavaVersion.VERSION_17
    }
}

dependencies {
    implementation 'com.google.android.gms:play-services-ads-identifier:18.0.1'
    implementation 'com.byappsoft.sap:HuvleSDK:$last_version'
    implementation 'androidx.activity:activity:1.8.0'

    // Material 1.12.0 이상 버전 — SDK UI(노티바 동의창 등)에서 사용하므로 모앱에도 필수
    implementation 'com.google.android.material:material:1.12.0'
}
```

> 버전에 대한 자세한 사항은 문의 주시길 바랍니다.

---

### 네이티브 디버그 심볼 설정 (Google Play 업로드용)

`buildTypes` 블록 안에 아래 설정을 추가합니다 (위 의존성 추가 예시에 이미 포함되어 있습니다).

**Kotlin DSL**
```kotlin
buildTypes {
    debug { ndk { debugSymbolLevel = "SYMBOL_TABLE" } }
    release { ndk { debugSymbolLevel = "SYMBOL_TABLE" } }
}
```

**Groovy DSL**
```groovy
buildTypes {
    debug { ndk { debugSymbolLevel 'SYMBOL_TABLE' } }
    release { ndk { debugSymbolLevel 'SYMBOL_TABLE' } }
}
```

빌드 후 생성 경로: `app/build/outputs/native-debug-symbols/native-debug-symbols.zip`
Google Play Console → 앱 번들 탐색기 → 저작물 → 네이티브 디버그 기호에 업로드하세요.
([참조](https://developer.android.com/studio/build/shrink-code?hl=ko#native-crash-support))

---

### ProGuard 설정

`proguard-rules.pro`에 추가 (Kotlin DSL / Groovy DSL 공통):

```proguard
-keep class com.byappsoft.sap.** { *; }
-dontwarn com.byappsoft.sap.**
```

---

## 3. 앱에 적용하기

> ⚠️ **앱 테마 설정 필수**
> SDK가 노티바 동의창을 직접 표시하며, 이 동의창이 Material 리소스를 사용합니다.
> 앱 테마가 `Theme.MaterialComponents.*` 또는 `Theme.Material3.*` 계열이 아닌 경우
> 동의창 버튼 텍스트가 비정상적으로 작게 표시될 수 있습니다.
> `res/values/themes.xml`을 아래와 같이 설정하세요.
>
> ```xml
> <style name="AppTheme" parent="Theme.Material3.Light.NoActionBar">
> ```

> ### ⚠️ SDK 초기화 위치 — 필수 적용
> **`HuvleSDK.initialize()`는 반드시 앱 실행 후 사용자에게 가장 먼저 표시되는 화면(Main Activity)의 `onResume()`에서 호출해야 합니다.**
>
> 아래 예시 코드의 `MainActivity`는 앱 실행 직후 첫 번째로 보이는 화면을 기준으로 작성되었습니다.
> 스플래시 등 별도 런처 화면이 있더라도 **실질적인 첫 메인 화면**의 `onResume()`에 적용해야 올바르게 동작합니다.

---

### A. Kotlin 신규 연동 (권장)

`HuvleSDK.initialize()`는 코루틴 기반 API로 호출합니다.

> ⚠️ **동의창 동작 변경 안내**
> 레거시 방식(`Sap_act_main_launcher`)은 SDK 내부에서 노티바 동의창을 자동으로 표시하지만,
> 신규 방식은 동의창 표시 여부와 디자인을 **앱에서 직접 제어**합니다.
> 노티바 동의 여부가 필요한 경우, 앱에서 자체 UI(다이얼로그 등)를 구현한 뒤
> 사용자가 동의하면 `Sap_Func.notiUpdate(context)`를, 거부하면 `Sap_Func.notiCancel(context)`를 호출하세요.

```kotlin
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
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.byappsoft.sap.api.HuvleConfig
import com.byappsoft.sap.api.HuvleSDK
import com.byappsoft.sap.utils.Sap_Func
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_POST_NOTIFICATIONS = 1001
    }

    // 권한 요청 플로우 중복 실행 방지 플래그
    // onResume()이 설정화면 복귀 시에도 호출되기 때문에 필요
    private var isPermissionFlowDone = false

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            checkBatteryOptimizationPermission()
        }

    private val batteryOptimizationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Android 13+ 알림 권한 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!checkPostNotificationPermission()) {
                requestPostNotificationPermission()
            }
        }

        // 앱 자체 권한 플로우 (SDK 초기화와 독립적)
        startPermissionFlow()
    }

    // 앱 실행 후 첫 번째로 표시되는 화면(Main Activity)의 onResume에서 초기화
    override fun onResume() {
        super.onResume()
        // Android 14+ 서비스 상태 갱신 *필수
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Sap_Func.setServiceState(this, true)
        }
        huvleView()
    }

    private fun huvleView() {
        lifecycleScope.launch {
            HuvleSDK.initialize(
                context = this@MainActivity,
                agencyKey = "발급받은_에이전트_키",  // agent.huvle.com 에서 등록한 에이전트 키
                config = HuvleConfig(
                    enableNotification = true,  // 노티바 사용 여부
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
```

**동의창을 직접 구현하려면 (선택사항)**

신규 방식에는 SDK 자동 동의창이 없습니다. 사용자에게 노티바 표시 동의를 받고 싶다면 아래 플로우로 앱에서 직접 구현하세요.

**권장 플로우**
```
① HuvleSDK.initialize(enableNotification = false)  → 노티바 OFF 상태로 초기화
② 앱에서 자체 동의 다이얼로그 표시
③ 사용자 선택 결과에 따라 노티바 상태 갱신
   ├─ 동의 → Sap_Func.notiUpdate(context)  (노티바 ON)
   └─ 거부 → Sap_Func.notiCancel(context)  (노티바 OFF 유지)
④ 재진입 시 중복 노출 방지를 위해 동의 여부를 저장(SharedPreferences 등)하여 재표시 제어
```

**Kotlin 예시**

```kotlin
private fun huvleView() {
    lifecycleScope.launch {
        // ① 노티바 OFF 상태로 SDK 초기화
        HuvleSDK.initialize(
            context = this@MainActivity,
            agencyKey = "발급받은_에이전트_키",
            config = HuvleConfig(
                enableNotification = false,  // 동의창 결과에 따라 이후에 제어
                enableUrlSearch = true
            )
        )

        // ② 이전에 동의한 적 없으면 커스텀 다이얼로그 표시
        if (!hasShownNotiConsent()) {
            showNotiConsentDialog()
        }
    }
}

private fun showNotiConsentDialog() {
    AlertDialog.Builder(this)
        .setTitle("알림 표시 동의")
        .setMessage("노티바를 통해 유용한 정보를 받아보시겠습니까?")
        .setPositiveButton("동의") { _, _ ->
            // ③-a 동의 → 노티바 ON
            Sap_Func.notiUpdate(applicationContext)
            markNotiConsentShown()
        }
        .setNegativeButton("거부") { _, _ ->
            // ③-b 거부 → 노티바 OFF 유지
            Sap_Func.notiCancel(applicationContext)
            markNotiConsentShown()
        }
        .setCancelable(false)
        .show()
}

// ④ 동의 여부 저장 (중복 노출 방지)
private fun hasShownNotiConsent(): Boolean =
    getSharedPreferences("huvle_prefs", Context.MODE_PRIVATE)
        .getBoolean("noti_consent_shown", false)

private fun markNotiConsentShown() {
    getSharedPreferences("huvle_prefs", Context.MODE_PRIVATE)
        .edit().putBoolean("noti_consent_shown", true).apply()
}
```

> 💡 동의창 **디자인/문구를 완전히 커스텀**할 수 있다는 것이 신규 방식의 장점입니다. 일반 `AlertDialog` 외에 `BottomSheetDialog`, 풀스크린 Fragment 등 앱 UX에 맞게 자유롭게 구성하세요.

---

**인앱 버튼으로 브라우저 열기 (노티바 미사용)**

```kotlin
// initialize 시 enableNotification = false 설정
HuvleSDK.initialize(
    context = this,
    agencyKey = "발급받은_에이전트_키",
    config = HuvleConfig(enableNotification = false, enableUrlSearch = true)
)

// 버튼 클릭 시 브라우저 직접 실행
binding.testBtn.setOnClickListener {
    startActivity(
        Intent(this, Sap_MainActivity::class.java).apply {
            putExtra(Sap_BrowserActivity.PARAM_OPEN_URL, "https://www.huvle.com/global_set.php")
        }
    )
}
```

---

### B. 레거시 연동 (Java 앱 또는 기존 코드 유지)

기존 Java SDK와 동일한 콜백 방식입니다.

> ⚠️ `Sap_act_main_launcher`는 하위 호환을 위해 유지되지만, 신규 연동에는 **A 방식을 권장**합니다.

**Kotlin**

```kotlin
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.core.content.ContextCompat
import com.byappsoft.sap.launcher.Sap_act_main_launcher
import com.byappsoft.sap.utils.Sap_Func

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_POST_NOTIFICATIONS = 1001
    }

    private var isPermissionFlowDone = false

    private val overlayPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            checkBatteryOptimizationPermission()
        }

    private val batteryOptimizationLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // Android 13+ 알림 권한 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!checkPostNotificationPermission()) {
                requestPostNotificationPermission()
            }
        }

        startPermissionFlow()
    }

    // 앱 실행 후 첫 번째로 표시되는 화면(Main Activity)의 onResume에서 초기화
    override fun onResume() {
        super.onResume()
        // Android 14+ 서비스 상태 갱신 *필수
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Sap_Func.setServiceState(this, true)
        }
        huvleView()
    }

    private fun huvleView() {
        Sap_act_main_launcher.initsapStart(
            this,
            "발급받은_에이전트_키",  // agent.huvle.com 에서 등록한 에이전트 키
            true,   // 노티바 사용 여부
            true,   // URL 검색 사용 여부
            object : Sap_act_main_launcher.OnLauncher {
                override fun onDialogOkClicked() { }
                override fun onDialogCancelClicked() { }
                override fun onInitSapStartapp() { }
                override fun onUnknown() { }
            }
        )
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
```

**Java**

```java
import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import com.byappsoft.sap.launcher.Sap_act_main_launcher;
import com.byappsoft.sap.utils.Sap_Func;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_POST_NOTIFICATIONS = 1001;

    // 권한 요청 플로우 중복 실행 방지 플래그
    // onResume()이 설정화면 복귀 시에도 호출되기 때문에 필요
    private boolean isPermissionFlowDone = false;

    private ActivityResultLauncher<Intent> overlayPermissionLauncher;
    private ActivityResultLauncher<Intent> batteryOptimizationLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        overlayPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> checkBatteryOptimizationPermission()
        );
        batteryOptimizationLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> { }
        );

        // Android 13+ 알림 권한 확인
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (!checkPostNotificationPermission()) {
                requestPostNotificationPermission();
            }
        }

        startPermissionFlow();
    }

    // 앱 실행 후 첫 번째로 표시되는 화면(Main Activity)의 onResume에서 초기화
    @Override
    public void onResume() {
        super.onResume();
        // Android 14+ 서비스 상태 갱신 *필수
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Sap_Func.setServiceState(this, true);
        }
        huvleView();
    }

    public void huvleView() {
        Sap_act_main_launcher.initsapStart(
            this,
            "발급받은_에이전트_키",  // agent.huvle.com 에서 등록한 에이전트 키
            true,   // 노티바 사용 여부
            true,   // URL 검색 사용 여부
            new Sap_act_main_launcher.OnLauncher() {
                @Override public void onDialogOkClicked() { }
                @Override public void onDialogCancelClicked() { }
                @Override public void onInitSapStartapp() { }
                @Override public void onUnknown() { }
            }
        );
    }

    // 권한 플로우 진입점 - 중복 실행 방지 후 오버레이 권한 확인
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
                    Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    overlayPermissionLauncher.launch(intent);
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

    private void checkBatteryOptimizationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (!pm.isIgnoringBatteryOptimizations(getPackageName())) {
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
                    batteryOptimizationLauncher.launch(
                        new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)
                    );
                }
            })
            .setNegativeButton("취소", (dialog, which) -> {
                Toast.makeText(this, "권한이 거부되어 일부 기능이 제한될 수 있습니다.", Toast.LENGTH_SHORT).show();
            })
            .setCancelable(false)
            .create()
            .show();
    }

    private boolean checkPostNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            return ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED;
        }
        return true;
    }

    private void requestPostNotificationPermission() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                requestPermissions(
                    new String[] { Manifest.permission.POST_NOTIFICATIONS },
                    REQUEST_CODE_POST_NOTIFICATIONS
                );
            }
        } catch (Exception ignored) { }
    }

    @Override
    public void onRequestPermissionsResult(
        int requestCode, String[] permissions, int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }
}
```

> **에이전트 키 안내**
> `initsapStart` 두 번째 파라미터는 `http://agent.huvle.com/` 에서 회원가입 시 등록하는 에이전트 키입니다.
> 문의 사항은 사이트 내 제휴 문의를 이용해 주세요.

---

### 연동 방식 비교

| 항목 | A. Kotlin 신규 (권장) | B. 레거시 |
|------|:---:|:---:|
| 진입 API | `HuvleSDK.initialize()` | `Sap_act_main_launcher.initsapStart()` |
| 결과 처리 | 코루틴 기반 (결과 확인 선택적) | 콜백 (`OnLauncher`) |
| 노티바 제어 | `Sap_Func.notiUpdate()` / `Sap_Func.notiCancel()` | 동일 |
| Java 호환 | ⚠️ (코루틴 필수) | ✅ |
| Kotlin 권장 | ✅ | ⚠️ |
| 향후 지원 | 신규 기능 추가 | 호환 유지만 |

---

## 4. 노티바 ON/OFF 제어

A/B 연동 방식과 무관하게 **공통 API**를 사용합니다.

**Kotlin / Java 공통**

```kotlin
Sap_Func.notiUpdate(applicationContext)  // 노티바 켜기
Sap_Func.notiCancel(applicationContext)  // 노티바 끄기
```

---

## 5. 노티바/동의창 커스텀 (선택사항)

기본 모드를 사용하는 경우 이 섹션은 건너뛰어도 됩니다.

앱의 `com.byappsoft.sap` 패키지 하위에 `CustomNotibarConfig` 클래스를 추가합니다.
SDK가 리플렉션으로 `com.byappsoft.sap.CustomNotibarConfig`를 탐색하므로 **패키지 경로가 정확히 일치**해야 합니다.

**Kotlin**

```kotlin
// 파일 위치: com/byappsoft/sap/CustomNotibarConfig.kt
package com.byappsoft.sap

import android.app.Activity
import com.byappsoft.sap.launcher.NotibarConfig

class CustomNotibarConfig : NotibarConfig() {

    // 동의창 배경 이미지 (drawable 리소스 ID)
    override fun getNotibarPopupBg(): Int = R.drawable.your_popup_bg

    // 노티바 아이콘 (1~5, drawable 리소스 ID)
    override fun getNotibarIcon1(): Int = R.drawable.your_noti_icon

    // 노티바 텍스트 (1~5, string 리소스 ID)
    override fun getNotibarString1(): Int = R.string.your_custom_string

    // 노티바 버튼 클릭 동작 (1~5)
    override fun callNotibar1(activity: Activity, nt: String) {
        // 노티바 클릭 시 실행할 동작
        // 예: activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://example.com")))
        //     activity.finish()
    }
}
```

**Java**

```java
// 파일 위치: com/byappsoft/sap/CustomNotibarConfig.java
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

### 오버라이드 가능 메서드 목록

| 메서드 | 반환 타입 | 설명 |
|--------|-----------|------|
| `getNotibarPopupBg()` | `Int` | 동의창 배경 drawable 리소스 ID |
| `getNotibarIcon1()` ~ `getNotibarIcon5()` | `Int` | 노티바 버튼 아이콘 drawable 리소스 ID |
| `getNotibarString1()` ~ `getNotibarString5()` | `Int` | 노티바 버튼 텍스트 string 리소스 ID |
| `callNotibar1(activity, nt)` ~ `callNotibar5(activity, nt)` | `void` | 노티바 버튼 클릭 시 실행할 동작 |

### 다크모드 대응

Android 10 이상에서 기기 다크모드 활성화 시 노티바 배경색이 자동으로 변경됩니다.

**`res/values/themes.xml`**

```xml
<style name="HuvleStatusbar">
    <item name="android:textColor">#000000</item>
</style>
```

**`res/values-night/themes.xml`**

```xml
<style name="HuvleStatusbar">
    <item name="android:textColor">#FFFFFF</item>
</style>
```

---

## 6. 연동 체크리스트

### Manifest

| 항목 | 확인 |
|------|:----:|
| `AD_ID` 권한 추가 | ☐ |
| `POST_NOTIFICATIONS` 권한 추가 (Target SDK 33+) | ☐ |
| `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` 권한 추가 | ☐ |
| `HuvleNotiBarService` 선언 오버라이드 (Target SDK 34+, 필요 시) | ☐ |
| `network_security_config.xml` HTTP 허용 설정 | ☐ |
| MainActivity `launchMode`, `clearTaskOnLaunch` 설정 | ☐ |

### SDK 설정

| 항목 | 확인 |
|------|:----:|
| AGP 8.5 ~ 8.7.x 사용 | ☐ |
| Huvle Maven 저장소 추가 | ☐ |
| `HuvleSDK` 의존성 추가 | ☐ |
| Material 1.12.0 이상 의존성 추가 | ☐ |
| 앱 테마를 `Theme.Material3.*` 계열로 설정 | ☐ |
| ProGuard `-keep` 규칙 추가 | ☐ |

### 코드 적용

| 항목 | 확인 |
|------|:----:|
| `onCreate`: 알림 권한 요청 + `startPermissionFlow()` 호출 | ☐ |
| `onResume`: `setServiceState` + `huvleView()` 호출 | ☐ |
| 에이전트 키 입력 | ☐ |

---

## 7. 자주 발생하는 오류

| 오류 메시지 | 원인 | 해결 방법 |
|------------|------|----------|
| 노티바가 나타나지 않음 | 알림 권한 미허용 | 권한 요청 플로우 확인 |
| 서비스가 강제적으로 중단됨 | 다른 앱 그리기 권한 및 배터리 최적화 미제외 | `SYSTEM_ALERT_WINDOW` 권한 및 `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` 적용 |
| `NoSuchFieldError: No field colorError … in com.google.android.material.R$attr` | Material 라이브러리 미적용 상태에서 AGP 8.0 이상의 `nonTransitiveRClass=true` 환경으로 빌드한 경우 | `dependencies`에 `implementation 'com.google.android.material:material:1.12.0'` 추가 |

---

## License

Huvle SDK의 저작권은 (주)허블에 있습니다.

```
Huvle SDK Android
Copyright 2021-present Huvle Corp.

Unauthorized use, modification and redistribution of this software are strongly prohibited.
```
