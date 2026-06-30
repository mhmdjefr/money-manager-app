# 💰 Money Manager App

A clean, modern personal finance tracker built for Android. Track your income, expenses, and account balances with an intuitive Jetpack Compose interface.

<!-- 
  Add your screenshots here once available:
  ![Dashboard](docs/screenshots/dashboard.png)
  ![Statistics](docs/screenshots/statistics.png)
  ![Wallet](docs/screenshots/wallet.png)
-->

## ✨ Features

- **Dashboard Overview** — See your total balance, income, and expenses at a glance
- **Transaction Management** — Add, edit, and delete income/expense transactions
- **Multiple Accounts (Wallets)** — Organize your money across different accounts
- **Custom Categories** — Create and manage your own income/expense categories with custom icons
- **Statistics & Charts** — Visualize your spending patterns with dynamic, color-coded charts
- **CSV Export/Import** — Back up your data or migrate it between devices
- **Reset Data** — Start fresh anytime with a full data reset option
- **Splash Screen** — Polished app launch experience
- **Local-first Storage** — All data is stored locally using Room database; no internet connection required

## 🛠️ Tech Stack

- **Language:** [Kotlin](https://kotlinlang.org/)
- **UI Toolkit:** [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material 3
- **Architecture:** MVVM (Model-View-ViewModel)
- **Local Database:** [Room](https://developer.android.com/training/data-storage/room)
- **Navigation:** [Navigation Compose](https://developer.android.com/jetpack/compose/navigation)
- **Async:** Kotlin Coroutines & Flow
- **Build System:** Gradle (Kotlin DSL) with [KSP](https://kotlinlang.org/docs/ksp-overview.html) for annotation processing

## 🏗️ Architecture

The app follows a simple MVVM structure:
com.mhmdjefr.moneymanager

├── data

│   ├── local          # Room entities, DAO, and database setup

│   └── repository      # Single source of truth bridging data layer and UI

├── ui

│   ├── dashboard        # Home screen with balance overview

│   ├── transaction      # Add/edit transaction screen

│   ├── wallet            # Account/wallet management

│   ├── statistic         # Charts and spending insights

│   ├── settings           # App settings, category management, CSV export/import

│   ├── splash              # Launch screen

│   └── theme                # Colors, typography, and Material theme

├── MainActivity.kt    # Navigation host and bottom navigation

└── MoneyApplication.kt # App-level dependency setup (database & repository)

## 🚀 Getting Started

### Prerequisites

- [Android Studio](https://developer.android.com/studio) (latest stable version recommended)
- JDK 11 or higher
- An Android device or emulator running **API 26 (Android 8.0)** or above

### Build from Source

1. Clone the repository:
```bash
   git clone https://github.com/mhmdjefr/money-manager-app.git
   cd money-manager-app
```

2. Open the project in Android Studio, or build from the command line:
```bash
   ./gradlew assembleDebug
```

3. The generated APK will be located at:
app/build/outputs/apk/debug/app-debug.apk

4. Install it on a connected device/emulator:
```bash
   ./gradlew installDebug
```

### Building a Release APK

A release build requires a signing configuration. Create a `keystore.properties` file in the project root (this file is git-ignored and should never be committed):

```properties
storeFile=/path/to/your/keystore.jks
storePassword=your_store_password
keyAlias=your_key_alias
keyPassword=your_key_password
```

Then run:

```bash
./gradlew assembleRelease
```

## 📋 Requirements

| Item | Minimum |
|------|---------|
| Android SDK | API 26 (Android 8.0 Oreo) |
| Target SDK | API 36 |
| Kotlin | 2.2.10 |

## 🤝 Contributing

This is currently a personal project, but suggestions and feedback are welcome — feel free to open an issue.

## 📄 License

This project does not currently specify a license. All rights reserved by the author unless stated otherwise.

---

Built by [mhmdjefr](https://github.com/mhmdjefr)
