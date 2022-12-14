# HuvleView_SDK

## 허블뷰 (Huvle) SDK Install Guide

Huvle SDK의 연동 방식은 Gradle을 이용한 방법으로 샘플 예제를 이용해 간단하게 연동이 가능합니다.
또한 Flutter와 Unity 3D에서도 연동이 가능합니다. 현재 Huvle SDK 최신버전은 **6.0.1** 입니다.
Huvle SDK 는 **TargetSDK 31** 이상 적용을 권장드립니다.
아래 가이드 문서 내용은 본 문서 적용가이드의 **"모든 허블뷰 샘플 프로젝트 다운로드"** 하시면 모든 내용을 보실 수 있습니다.



## 제휴 신청
허블뷰 (Huvle) SDK 제휴 방법은 https://www.huvleview.com/doc/contact.php 에 절차를 안내 드리고 있습니다.


### 적용가이드
- Usages 를 참고하시거나 아래 샘플 프로젝트를 참고해주세요.
- [모든 샘플 프로젝트 다운로드(android,flutter,unity)](https://github.com/Huvle-Ad/HuvleView_SDK_KR/archive/main.zip)


## Usages
### 1. Manifest

- 구글광고아이디 퍼미션 추가
```
<manifest>
...
    <uses-permission android:name="com.google.android.gms.permission.AD_ID" /> 
...
</manifest>
```

- APP Target SDK 33 이상일 경우 POST_Notification 권한 처리   
- [자세한 사항은 developer 문서 참고](https://developer.android.com/develop/ui/views/notifications/notification-permission?hl=en)

```java
<manifest>
...
    <uses-permission android:name="android.permission.POST_NOTIFICATIONS"/>
...
</manifest>

```

- 항상 귀사의 앱이 실행될 수 있도록 launchMode 및 clearTaskOnLaunch 추가
```
<activity
	android:name=".MainActivity"
	android:launchMode="singleInstance"
	android:clearTaskOnLaunch="true">
```

### 2. SDK 추가
HuvleView SDK 를 사용하기 위해서는 gradle에 SDK를 포함한 하위 라이브러리들을 추가해야합니다.
- build.gradle(Project)
```
allprojects {
    repositories {
        google()
        maven {
            name "Huvle"
            url "https://sdk.huvle.com/repository/internal"
        }
    }
}
```

- build.gradle(app)
```

dependencies {
	.
	.
	/**
	* huvle sdk , play-service-ads 
	*/
	implementation 'com.google.android.gms:play-services-ads:20.5.0'
	implementation 'com.byappsoft.sap:HuvleSDK:6.0.1' 
	.
	.
}
```

- Android Studio 4.1(com.android.tools.build:gradle:4.1.0)사용시 native-debug-symbols.zip자동생성 추가하고 이하버전은 아래 참조url을 참고해 주세요.
```
buildTypes {
	...
    debug {
        ndk {
            debugSymbolLevel 'SYMBOL_TABLE'
        }
    release {
        ndk {
            debugSymbolLevel 'SYMBOL_TABLE'
        }
	...
}
```
앱 업데이트시 네이티브 충돌 관련 워닝처리
- 위와같이 적용하시면 프로젝트\app\build\outputs\native-debug-symbols\debug\native-debug-symbols.zip 생성됩니다.
- 구글콘솔에서 앱 업데이트시 해당앱의 app bundle 탐색기 > 저작물 > 네이티브 디버그 기호 > native-debug-symbols.zip 파일 업로드
- 참조: https://developer.android.com/studio/build/shrink-code?hl=ko#native-crash-support

- proguard-rules.pro아래 코드 추가
```
-keep class com.byappsoft.sap.**{*;}
-dontwarn com.byappsoft.sap.**
```

### 3. 앱에 적용하기
- MainActivity(귀사의 MainActivity)

+ onResume 추가

+ Java code
```java
@Override
public void onResume() {
	super.onResume();
	// huvleView apply
	Sap_Func.setNotiBarLockScreen(this, false);
	Sap_act_main_launcher.initsapStart(this, "bynetwork", true, true);

	// APP target 33 이상일 경우 POST_NOTIFICATION 권한 처리 후 적용
	// if(Post_notification){
	// 	Sap_Func.setNotiBarLockScreen(this, false);
	// 	Sap_act_main_launcher.initsapStart(this, "bynetwork", true, true);
	// }
	
	
}
```
- Kotlin code
```java
override fun onResume() {
	super.onResume()
	// huvleView apply
	Sap_Func.setNotiBarLockScreen(this,false)
	Sap_act_main_launcher.initsapStart(this,"bynetwork",true,true)

}
```
   


- Sap_act_main_launcher.initsapStart(this, "bynetwork", true, true) 에서   
  **"bynetwork"** 값은 _http://agent.huvle.com/_ 에서 회원 가입시 등록하실 아이디와 동일하게 입력한 **에이전트** 키를 기입해주시면 됩니다.   
  그 외 문의 사항은 사이트 내 제휴 문의를 이용해 주시기 바랍니다.




### 4. 노티바/동의창내용 커스텀시(샘플앱에 적용되어 있음, 커스텀 하지 않을경우 아래 작업은 불필요.)
```
- 귀사의 앱 내에 com\byappsoft\sap\CustomNotibarConfig.java 추가 후 변경(기본모드 사용 시에는 모두 주석처리 또는 추가하지 않음.)
- 동의창 관련 매소드
	getNotibarPopupBg()
- 노티바 관련 매소드
	노티바 아이콘 : getNotibarIcon1() ~ getNotibarIcon5()
	노티바 텍스트 : getNotibarString1() ~ getNotibarString5()
	해당 액션 : callNotibar1() ~ callNotibar5()
- 기기 다크모드(야간모드) 활성화 시 노티바 배경색 자동 변경 (Adroid OS 10 이상 버전 자동 적용 가능)
	valuse 폴더 - themes 폴더 내 thems.xml / thems.xml(night) 에 textColor style ("HuvleStatusbar") 추가 
	안드로이드 스튜디오 4.1 이하 버전은 values - styles 폴더 내 styles.xml / styles.xml(night) 에 textColor style 추가
	layout 폴더 - lay_sap_act_noti.xml 추가 
	lay_sap_act_noti.xml 내의 모든 TextView 부분에 HuvleStatusbar Style 적용 
	 
```


## License
Huvle SDK 의 저작권은 (주)허블에 있습니다.
```
Huvle SDK Android
Copyright 2021-present Huvle Corp.

Unauthorized use, modification and redistribution of this software are strongly prohibited.
```

