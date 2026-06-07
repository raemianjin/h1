# myHub

설정한 여러 URL을 **좌우 스와이프로 전환**하며 보는 개인 웹 허브 앱.
첫 번째로 등록한 URL이 앱의 첫 화면이 된다.

- 작성자: 영찬영하 Daddy
- 버전: 1.0.0
- 패키지: `com.myhub.app`

## 기능

- 설정 화면에서 URL을 추가/편집/삭제/순서변경 (이름·색상 지정)
- 등록한 페이지마다 **독립 WebView** → 좌우 스와이프 + 상단 탭으로 전환
- 당겨서 새로고침, 상단 진행률 표시, 뒤로가기 = 페이지 내 뒤로
- **로그인 지원**: 쿠키/세션 영속(다음 실행에도 로그인 유지), 로그인 팝업(window.open) 및 파일 첨부 처리

## 아키텍처

- 설정 화면(`assets/index.html`): 수려한 다크 UI + `Native` JS 브리지로 설정 저장/로드
- 표시 화면(네이티브 `ViewPager2` + `WebPageFragment`): 외부 사이트는 iframe 차단(X-Frame-Options)
  때문에 페이지마다 네이티브 WebView를 사용한다.
- 설정은 `SharedPreferences`에 JSON으로 로컬 저장 (외부 전송 없음)

## 빌드

GitHub Actions(`.github/workflows/android-build.yml`)가 push 시 자동으로 debug APK를 만든다.
- Gradle 8.7 / AGP 8.5.0 / Kotlin 1.9.24 / compileSdk·targetSdk 34 / minSdk 26 / JDK 17
- 산출물: `app/build/outputs/apk/debug/app-debug.apk` (아티팩트 `APK-<run_number>`, 30일 보관)

## 한계 (정직 고지)

- Claude 환경에 Android SDK가 없어 **문법 검증만** 했고 실기기 빌드/실행은 미검증.
- 구글 계정 등 일부 OAuth 제공자는 보안 정책상 인앱 WebView 로그인을 차단할 수 있다.
- 일부 사이트는 자체적으로 WebView 임베딩을 제한할 수 있다.
