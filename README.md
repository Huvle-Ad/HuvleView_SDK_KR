# Huvle SDK — Android 연동 가이드

> **최신 버전** 적용을 권장합니다.
> 제휴 신청: https://www.huvleview.com/sub/location.php

---

## 목차

1. [Manifest 설정](#1-manifest-설정)
2. [SDK 추가](#2-sdk-추가)
3. [Hilt 설정 (필수)](#3-hilt-설정-필수)
4. [앱에 적용하기](#4-앱에-적용하기)
5. [노티바 ON/OFF 제어](#5-노티바-onoff-제어)
6. [노티바/동의창 커스텀](#6-노티바동의창-커스텀-선택사항)
7. [연동 방식 비교](#7-연동-방식-비교)
8. [연동 체크리스트](#8-연동-체크리스트)
9. [자주 발생하는 오류](#9-자주-발생하는-오류)

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

v6.3.0.0부터 `HuvleNotiBarService` 선언이 SDK manifest에 내장되어 자동으로 포함됩니다.
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

### 저장소 추가

**`settings.gradle.kts`**

```kotlin
dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
        maven {
            name = "Huvle"
            url = uri("https://sdk.huvle.com/repository/internal")
        }
    }
}
```

**`settings.gradle` (Groovy)**

```groovy
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
```

---

### 의존성 추가

**Kotlin 앱 (`build.gradle.kts`)**

```kotlin
plugins {
    id("com.android.application")
    id("kotlin-android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android") // Hilt 필수
}

dependencies {
    implementation("com.google.android.gms:play-services-ads-identifier:18.0.1")
    implementation("com.byappsoft.sap:HuvleSDK:$last_version")
    implementation("androidx.activity:activity-ktx:1.8.0")

    // Hilt — SDK 내부에서 사용하므로 모앱에도 필수
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-compiler:2.50")

    // Material — SDK UI(노티바 동의창 등)에서 사용하므로 모앱에도 필수
    implementation("com.google.android.material:material:1.12.0")
}
```

**Java 앱 (`build.gradle`)**

```groovy
plugins {
    id 'com.android.application'
    id 'com.google.dagger.hilt.android' // Hilt 필수
}

dependencies {
    implementation 'com.google.android.gms:play-services-ads-identifier:18.0.1'
    implementation 'com.byappsoft.sap:HuvleSDK:$last_version'
    implementation 'androidx.activity:activity:1.8.0'

    // Hilt — SDK 내부에서 사용하므로 모앱에도 필수
    implementation 'com.google.dagger:hilt-android:2.50'
    annotationProcessor 'com.google.dagger:hilt-compiler:2.50'

    // Material — SDK UI(노티바 동의창 등)에서 사용하므로 모앱에도 필수
    implementation 'com.google.android.material:material:1.12.0'
}
```

> 버전에 대한 자세한 사항은 문의 주시길 바랍니다.

---

### 네이티브 디버그 심볼 설정 (Google Play 업로드용)

```kotlin
buildTypes {
    debug { ndk { debugSymbolLevel = "SYMBOL_TABLE" } }
    release { ndk { debugSymbolLevel = "SYMBOL_TABLE" } }
}
```

빌드 후 생성 경로: `app/build/outputs/native-debug-symbols/native-debug-symbols.zip`
Google Play Console → 앱 번들 탐색기 → 저작물 → 네이티브 디버그 기호에 업로드하세요.
([참조](https://developer.android.com/studio/build/shrink-code?hl=ko#native-crash-support))

---

### ProGuard 설정

`proguard-rules.pro`에 추가:

```proguard
-keep class com.byappsoft.sap.** { *; }
-dontwarn com.byappsoft.sap.**
```

---

## 3. Hilt 설정 (필수)

SDK 내부에서 Hilt를 사용하므로 **연동 앱에도 Hilt 초기화가 반드시 필요합니다.**

### Application 클래스 설정

**Application 클래스가 없는 경우 — 신규 생성**

```kotlin
// Kotlin
@HiltAndroidApp
class MyApp : Application()
```

```java
// Java
@HiltAndroidApp
public class MyApp extends Application {}
```

**Application 클래스가 이미 있는 경우 — 어노테이션만 추가**

```kotlin
@HiltAndroidApp  // 추가
class MyApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // 기존 초기화 코드 유지
    }
}
```

### AndroidManifest.xml에 Application 등록

```xml
<application
    android:name=".MyApp"
    ...>
```

> 이미 `android:name`이 지정되어 있다면 해당 클래스에 `@HiltAndroidApp`만 추가하면 됩니다.

---

## 4. 앱에 적용하기

### A. Kotlin 신규 연동 (권장)

`HuvleSDK.initialize()`는 코루틴 기반 API로, 콜백 없이 결과를 `Result` 타입으로 직접 받습니다.


```kotlin
import com.byappsoft.sap.api.HuvleConfig
import com.byappsoft.sap.api.HuvleSDK
import com.byappsoft.sap.domain.model.Result  // kotlin.Result 와 혼동 주의
import com.byappsoft.sap.utils.Sap_Func

class MainActivity : AppCompatActivity() {

    companion object {
        private const val REQUEST_CODE_POST_NOTIFICATIONS = 1001
    }

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
                agencyKey = "발급받은_에이전트_키",  // agent.huvle.com 에서 등록한 에이전트 키
                config = HuvleConfig(
                    enableNotification = true,  // 노티바 사용 여부
                    enableUrlSearch = true       // URL 검색 사용 여부
                )
            )

            when (result) {
                is Result.Success -> checkDrawOverlayPermission()  // 초기화 성공
                is Result.Error -> { /* 초기화 실패 처리 */ }
                else -> { }
            }
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

> **`@AndroidEntryPoint` 참고**
> SDK는 `EntryPointAccessors.fromApplication()`을 사용하므로 Activity에 `@AndroidEntryPoint`가 없어도 동작합니다.
> 앱에서 별도로 Hilt 의존성 주입이 필요한 경우에만 Activity에 `@AndroidEntryPoint`를 추가하세요.

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

> ⚠️ **앱 테마 설정 필수**
> 레거시 방식은 SDK가 노티바 동의창을 직접 표시하며, 이 동의창이 Material3 리소스를 사용합니다.
> 앱 테마가 Material3 계열이 아닌 경우 동의창이 표시되지 않으므로 `res/values/themes.xml`을 아래와 같이 설정하세요.
>
> ```xml
> <style name="AppTheme" parent="Theme.Material3.Light.NoActionBar">
> ```

**Kotlin**

```kotlin
override fun onResume() {
    super.onResume()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        Sap_Func.setServiceState(this, true)
    }
    huvleView()
}

private fun huvleView() {
    Sap_act_main_launcher.initsapStart(
        this,
        "발급받은_에이전트_키",
        true,   // 노티바 사용 여부
        true,   // URL 검색 사용 여부
        object : Sap_act_main_launcher.OnLauncher {
            override fun onDialogOkClicked() { checkDrawOverlayPermission() }
            override fun onDialogCancelClicked() { }
            override fun onInitSapStartapp() { }
            override fun onUnknown() { }
        }
    )
}
```

**Java**

```java
@Override
public void onResume() {
    super.onResume();
    if (Build.VERSION.SDK_INT >= 34) {
        Sap_Func.setServiceState(this, true);
    }
    huvleView();
}

public void huvleView() {
    Sap_act_main_launcher.initsapStart(this, "발급받은_에이전트_키", true, true,
        new Sap_act_main_launcher.OnLauncher() {
            @Override public void onDialogOkClicked() { checkDrawOverlayPermission(); }
            @Override public void onDialogCancelClicked() { }
            @Override public void onInitSapStartapp() { }
            @Override public void onUnknown() { }
        });
}
```

> **에이전트 키 안내**
> `agencyKey` / `initsapStart` 두 번째 파라미터는 `http://agent.huvle.com/` 에서 회원가입 시 등록하는 에이전트 키입니다.
> 문의 사항은 사이트 내 제휴 문의를 이용해 주세요.

---

## 5. 노티바 ON/OFF 제어

```kotlin
Sap_Func.notiUpdate(applicationContext)  // 노티바 켜기
Sap_Func.notiCancel(applicationContext)  // 노티바 끄기
```

---

## 6. 노티바/동의창 커스텀 (선택사항)

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

## 7. 연동 방식 비교

| 항목 | A. Kotlin 신규 (권장) | B. 레거시 |
|------|:---:|:---:|
| 진입 API | `HuvleSDK.initialize()` | `Sap_act_main_launcher.initsapStart()` |
| 결과 처리 | `Result<Unit>` (코루틴) | 콜백 (`OnLauncher`) |
| 노티바 제어 | `Sap_Func.notiUpdate()` / `notiCancel()` | `Sap_Func.notiUpdate()` / `notiCancel()` |
| Java 호환 | △ (코루틴 필요) | ✅ |
| Kotlin 권장 | ✅ | △ |
| 향후 지원 | 신규 기능 추가 | 호환 유지만 |

---

## 8. 연동 체크리스트

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
| Huvle Maven 저장소 추가 | ☐ |
| `HuvleSDK` 의존성 추가 | ☐ |
| Hilt 플러그인 추가 (`com.google.dagger.hilt.android`) | ☐ |
| Hilt 의존성 추가 (`hilt-android`, `hilt-compiler`) | ☐ |
| ProGuard `-keep` 규칙 추가 | ☐ |

### 코드 적용

| 항목 | 확인 |
|------|:----:|
| `@HiltAndroidApp` Application 클래스 생성/수정 | ☐ |
| `android:name` Application Manifest 등록 | ☐ |
| `onCreate`: 알림 권한 요청 (Android 13+) | ☐ |
| `onResume`: `setServiceState` + SDK 초기화 호출 | ☐ |
| 에이전트 키 입력 | ☐ |

---

## 9. 자주 발생하는 오류

| 오류 메시지 | 원인 | 해결 방법 |
|------------|------|----------|
| `@HiltAndroidApp must be set` | Application 어노테이션 누락 | `@HiltAndroidApp` 추가 |
| `Hilt components are not initialized` | Application이 Manifest에 미등록 | `android:name` 설정 확인 |
| `kapt` 빌드 오류 (Java 앱) | Java 앱에서 `kapt` 사용 | `annotationProcessor`로 교체 |
| `Duplicate class` 오류 | Hilt 버전 충돌 | SDK와 동일 버전(`2.50`) 사용 |
| `HuvleSDK is not initialized` | `initialize()` 전에 다른 API 호출 | `Result.Success` 확인 후 호출 |
| `Result` import 오류 | `kotlin.Result`와 이름 충돌 | `import com.byappsoft.sap.domain.model.Result` 명시 |
| 노티바가 나타나지 않음 | 배터리 최적화 미제외 또는 오버레이 권한 미허용 | 권한 요청 플로우 확인 |
| 서비스가 간헐적으로 중단됨 | 배터리 최적화 미제외 | `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` 적용 |

---

## License

Huvle SDK의 저작권은 (주)허블에 있습니다.

```
Huvle SDK Android
Copyright 2021-present Huvle Corp.

Unauthorized use, modification and redistribution of this software are strongly prohibited.
```
