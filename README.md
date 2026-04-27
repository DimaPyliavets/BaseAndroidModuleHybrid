# BaseAndroidModuleHybrid

Модульний Android-додаток з гібридною архітектурою (Kotlin + Dynamic WebView).

---

## 📁 Структура модулів

```
app/             → Головний модуль (Activity, DI, Application)
core/            → Спільні моделі, утиліти, Room БД, константи
updater/         → Завантаження та перевірка веб-бандлів з GitHub
webview/         → WebView контейнер + assets (index.html, error.html)
bridge/          → JavascriptInterface (window.Android.*)
notifications/   → Локальні сповіщення
widget/          → Glance AppWidget
```

---

## 🚀 Швидкий старт: що ОБОВ'ЯЗКОВО змінити

### 1. Package name
У всіх файлах замінити `com.example.baseandroidmodulehybrid` на свій package.
- `app/build.gradle.kts` → `applicationId`
- `app/src/main/AndroidManifest.xml` → `package`
- Всі `package com.example...` рядки у Kotlin файлах

### 2. GitHub URLs (core/src/.../AppConfig.kt)
```kotlin
const val GITHUB_VERSION_URL = "https://raw.githubusercontent.com/YOUR_USER/YOUR_REPO/main/dist/version.json"
const val GITHUB_BUNDLE_BASE_URL = "https://github.com/YOUR_USER/YOUR_REPO/releases/download/"
```

### 3. Підготовка веб-бандлу
1. Зібери свій SPA/PWA у zip-архів (наприклад: `bundle-1.0.0.zip`)
2. Обчисли SHA-256: `sha256sum bundle-1.0.0.zip`
3. Опублікуй як GitHub Release
4. Оновити `dist/version.json` у репозиторії:
```json
{
  "version": "1.0.0",
  "bundleFileName": "bundle-1.0.0.zip",
  "sha256": "ВАШ_ХЕШ_ТУТ"
}
```

### 4. Іконки та ресурси
- `app/src/main/res/mipmap-*/ic_launcher*` — замінити на свої іконки
- `app/src/main/res/drawable/ic_notification` — іконка для сповіщень (24dp, білий)
- `app/src/main/res/values/strings.xml` → `app_name`
- `app/src/main/res/values/colors.xml` → кольорова палітра

### 5. Notification channels (HybridApp.kt)
Змінити назви каналів та їх важливість.

---

## 🔧 Налаштування середовища

**Вимоги:**
- Android Studio Hedgehog або новіший
- JDK 17
- Android SDK 34
- Gradle 8.x

**Перший запуск:**
```bash
./gradlew assembleDebug
```

---

## 🌐 JavaScript Bridge API

Після підключення бандлу виклики доступні як:

```javascript
// Показати нативне сповіщення
window.Android.showNotification("Заголовок", "Текст")

// Оновити дані віджета
window.Android.syncData('{"title":"Привіт","subtitle":"Оновлено"}')

// Отримати локаль пристрою (BCP 47)
const locale = window.Android.getDeviceLocale() // "uk-UA"

// Отримати версію нативного додатка
const version = window.Android.getAppVersion() // "1.0.0"
```

⚠️ Ім'я об'єкта `Android` задається у `AppConfig.JS_BRIDGE_NAME`.

---

## 🔄 Логіка оновлень

```
App Start
    ↓
fetchVersionInfo() ← GitHub version.json
    ↓
isUpdateRequired()?
    ├── Ні → UpToDate → завантажити локальний бандл
    └── Так → downloadBundle() → verifyAndExtract() → saveVersion()
                                     ↓ SHA-256 fail → Error
```

---

## 📋 Checklist перед публікацією

- [ ] Змінено `applicationId` на реальний package
- [ ] Замінено GitHub URLs в `AppConfig.kt`
- [ ] Додано власні іконки (ic_launcher, ic_notification)
- [ ] Налаштовано Notification Channel назви
- [ ] `WebView.setWebContentsDebuggingEnabled(false)` у release
- [ ] Перевірено `fallbackToDestructiveMigration` → замінено на Migration
- [ ] Підписано APK release keystore
- [ ] Перевірено SHA-256 першого бандлу
