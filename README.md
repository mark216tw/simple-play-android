# 簡單播放

「簡單播放」是一款 Android 媒體控制器，專注於簡單、直覺的播放操作。它會連接目前作用中的 Android `MediaSession`，讓使用者控制其他音樂、Podcast 或影音 App。

> [!WARNING]
> 目前提供的是 **Debug 測試版本**，不是正式發布版本。Debug APK 僅供功能測試，可能包含未完成的功能或相容性問題，請勿用於正式環境。

## 功能

- 顯示目前歌曲、演唱者、來源 App 與專輯封面。
- 支援播放、暫停、停止、上一首與下一首。
- 切換歌曲時盡量維持原本的播放、暫停或停止狀態。
- 支援同時存在的多個播放器與 MediaSession。
- 提供六種預設主題色與 RGB 自訂顏色。
- 支援跟隨系統、淺色與深色模式。
- 主題變更後立即更新 App 與系統導覽列。
- 提供單列媒體控制小工具。
- 提供只有播放／暫停按鈕的 `1 x 1` 小工具。
- 小工具可設定按鈕大小、主題色、顯示模式與背景樣式。

## 系統需求

- Android 12（API 31）以上。
- 必須授予「通知存取權」，App 才能取得其他播放器公開的 MediaSession。
- 實際可用的控制功能由來源播放器決定；來源 App 未提供的操作會停用。

## 安裝 Debug APK

1. 前往 [GitHub Releases](https://github.com/mark216tw/simple-play-android/releases)。
2. 開啟標示為 **Pre-release** 的 Debug 版本。
3. 下載 `app-debug.apk`。
4. 在 Android 裝置允許瀏覽器或檔案管理員安裝未知來源應用程式。
5. 安裝後開啟「簡單播放」，依畫面指示授予通知存取權。

Debug APK 使用開發用簽章，未來正式版不保證能直接覆蓋安裝。

## 使用方式

1. 開啟支援 Android 媒體控制的音樂、Podcast 或影音 App。
2. 開始播放內容。
3. 回到「簡單播放」查看歌曲資訊並進行控制。
4. 若有多個播放器，可在主畫面下方切換控制來源。
5. 點擊右上角齒輪可調整 App 與小工具外觀。

詳細操作請參閱[使用指南](docs/USER_GUIDE.md)。

## 小工具

專案提供兩種主畫面小工具：

- **簡單播放控制列**：上一首、播放／暫停、下一首，可在較寬尺寸顯示歌曲資訊。
- **播放／暫停按鈕**：`1 x 1`，只提供單一大型播放／暫停按鈕。

小工具支援以下設定：

- 按鈕大小：小、中、大。
- 六種預設色彩與 RGB 自訂顏色。
- 跟隨系統、淺色、深色。
- 主題色背景或透明背景。

## 隱私

- App 不需要讀取裝置內的音樂檔案。
- App 不提供音樂下載或網路串流服務。
- 媒體資訊只在裝置上用於畫面與小工具顯示。
- App 不會將歌曲資訊傳送到外部伺服器。

## 開發

主要技術：

- Kotlin
- Jetpack Compose Material 3
- Android MediaSession API
- NotificationListenerService
- RemoteViews App Widget
- Preferences DataStore

建置 Debug APK：

```shell
./gradlew assembleDebug
```

Windows：

```powershell
.\gradlew.bat assembleDebug
```

完整環境與驗證方式請參閱[開發文件](docs/DEVELOPMENT.md)。

## 已知限制

- 不同播放器公開的控制能力不完全相同。
- 部分播放器可能自行決定切歌後是否播放，App 會在標準 MediaSession 能力範圍內維持原狀態。
- Launcher 對小工具尺寸、透明背景及更新時機的處理可能不同。
- 跟隨系統的小工具會在 App 存活、媒體更新或系統週期更新時同步顯示模式。

## 授權

本專案使用 [MIT License](LICENSE)。
