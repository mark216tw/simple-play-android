# v0.1.0-debug.2 Prerelease 版本說明

## 版本狀態

這是 **R8 Prerelease 測試版本**，不是正式發布版本。

此 APK：

- Build Type 為 `prerelease`。
- APK 不開放除錯。
- 啟用 R8 程式碼壓縮、最佳化與混淆。
- 啟用未使用資源縮減。
- 使用 Android Debug 金鑰簽署。
- 僅供功能測試與介面預覽。
- 可能包含尚未發現的錯誤或裝置相容性問題。
- 不保證可直接升級到未來正式版本。

## APK 資訊

- 檔案：`app-prerelease.apk`
- Application ID：`com.simpleplay.app`
- Version name：`0.1.0-prerelease`
- Version code：`2`
- 最低版本：Android 12 / API 31
- Build Type：`prerelease`
- R8：已啟用
- 資源縮減：已啟用
- 簽章：Android Debug 金鑰
- APK 大小：2,069,554 bytes（約 1.97 MiB）
- SHA-256：`62BC177FAA77C6E691A48BE857C4B7FFED203647B2851B8426A7385390DB727D`

## 本次更新

- 新增 `prerelease` Build Type。
- 啟用 R8 程式碼壓縮、最佳化與混淆。
- 啟用資源縮減。
- Prerelease APK 使用 Android Debug 金鑰簽署。
- 控制列小工具支援依 `4 x 1` 至 `1 x 1` 寬度調整按鈕與歌曲資訊。
- 修正小工具播放／暫停圖示未即時同步的問題。
- 移除獨立的 `1 x 1` 播放／暫停小工具。
- 修正暫停或停止時切歌可能恢復播放及圖示閃動的問題。

## 目前功能

- 控制其他 App 的 MediaSession。
- 播放、暫停、停止、上一首與下一首。
- 多播放器選擇。
- 封面、歌曲、演唱者與來源 App 顯示。
- 六種預設主題與 RGB 自訂色。
- 淺色、深色及跟隨系統模式。
- 單列媒體控制小工具。
- 小工具按鈕大小、色彩、顯示模式與背景設定。

## 測試重點

- 通知存取權授權及撤銷流程。
- 不同播放器的可用控制按鈕。
- 播放、暫停與停止狀態下切換歌曲。
- 切歌時封面更新是否穩定。
- 不同 Launcher 的小工具尺寸與透明背景。
- App 與小工具的自訂色彩、深色及淺色模式。

## 建置驗證

- `testPrereleaseUnitTest`：9 項測試通過。
- `lintPrerelease`：通過，0 errors。
- `assemblePrerelease`：通過。
- R8 mapping：已產生 `app/build/outputs/mapping/prerelease/mapping.txt`。
- 資源縮減報告：已產生 `app/build/outputs/mapping/prerelease/resources.txt`。
- APK 簽章：Android Debug，APK Signature Scheme v2 驗證通過。
