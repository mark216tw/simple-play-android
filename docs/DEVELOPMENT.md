# 開發文件

## 專案資訊

- Application ID：`com.simpleplay.app`
- 最低版本：Android 12 / API 31
- Compile SDK：36
- Target SDK：36
- Java：17
- UI：Jetpack Compose Material 3
- 小工具：RemoteViews

## 開發環境

建議使用支援目前 Android Gradle Plugin 的 Android Studio，並安裝：

- JDK 17
- Android SDK Platform 36
- Android SDK Build Tools

專案已包含 Gradle Wrapper，不需要另外安裝 Gradle。

## 建置

Windows：

```powershell
.\gradlew.bat assembleDebug
```

macOS 或 Linux：

```shell
./gradlew assembleDebug
```

輸出位置：

```text
app/build/outputs/apk/debug/app-debug.apk
```

這個 APK 是使用開發簽章產生的 Debug 版本，不適合正式發布。

## 驗證

執行單元測試：

```powershell
.\gradlew.bat testDebugUnitTest
```

執行 Android Lint：

```powershell
.\gradlew.bat lintDebug
```

執行完整 Debug 驗證：

```powershell
.\gradlew.bat testDebugUnitTest lintDebug assembleDebug
```

## 架構

### 媒體控制

`MediaSessionAccess` 透過 `MediaSessionManager` 取得作用中的 `MediaController`，並根據來源播放器公開的 `PlaybackState.actions` 決定可用操作。

`MediaNotificationListenerService` 負責監聽 MediaSession、metadata 與播放狀態變化，並通知桌面小工具更新。

### UI 狀態

`MainViewModel` 管理可用播放器、選取的 MediaSession 與 MediaController callback。歌曲 metadata 更新不會重新註冊所有 callback，以減少切歌時的畫面重組。

封面區保留目前 Bitmap，直到新封面已取得才一次替換。來源播放器短暫清空封面時，不會立即切換成預設圖片。

### 設定

`AppPreferencesRepository` 使用 Preferences DataStore 保存：

- App 顯示模式與主題色。
- App RGB 自訂色。
- 選取的播放器及 MediaSession。
- 小工具按鈕大小。
- 小工具顯示模式、主題色與自訂色。
- 小工具背景樣式。

### 小工具

`PlaybackWidgetProvider` 提供可調整寬度的單列控制器，並依 `4 x 1` 至 `1 x 1` 寬度逐步隱藏歌曲資訊、上一首與下一首。

Widget 廣播透過 `goAsync()` 與序列化 coroutine 執行，避免在 BroadcastReceiver 主執行緒同步讀取 DataStore 或查詢 MediaSession。

## Release 注意事項

正式版本發布前至少需要：

- 建立正式簽章與安全的簽章管理方式。
- 更新 `versionCode` 與 `versionName`。
- 在多個 Android 版本與 Launcher 上驗證小工具。
- 驗證 Spotify、YouTube Music、Podcast 等常見來源播放器。
- 建立正式隱私權政策與 Google Play 上架素材。
- 執行 release build、R8 與實機回歸測試。
