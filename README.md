# Blind Glasses App（導盲眼鏡 Android）

本倉庫根目錄即 **Android Gradle 專案根**（含 `app/`、`gradle/`、`settings.gradle.kts` 等），請以 Android Studio **Open** 本目錄，勿再多包一層 `App1` 資料夾。

## 環境建議

- Android Studio 最新穩定版
- JDK 17（與 AGP 預設一致即可）

## 建置

於專案根目錄：

```bash
./gradlew :app:assembleDebug
```

Windows：

```bat
gradlew.bat :app:assembleDebug
```
