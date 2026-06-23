# Project Context: Brokk (The Assembler)

Brokk is a lightweight, efficient, and modern Android app installer designed to handle all Android package formats (standard `.apk`, split `.apks`, bundled `.xapk`, and zip archives of APKs). It provides rootless installation by streaming installation data directly to the Android OS `PackageInstaller` sessions, eliminating the need to extract large split APKs to disk first.

---

## Technical Stack

Brokk is built using modern Android development practices and libraries:

* **Language**: Kotlin (Targeting JVM 21, compiler version 2.4.0)
* **Build Configuration**: Gradle Kotlin DSL (`.gradle.kts`) with Android Gradle Plugin (AGP) version `9.4.0-alpha01`
* **SDK Compatibility**: `minSdk = 26` (Android 8.0), `targetSdk = 37` (Android 15), `compileSdk = 37`
* **UI & Theming**: Jetpack Compose with Material 3 (Material You styled theme, dynamic color adaptation)
* **Navigation**: AndroidX Navigation 3 (`androidx-navigation3-runtime`, `androidx-navigation3-ui`, and `androidx-material3-adaptive-navigation3` for adaptive layouts)
* **Dependency Injection**: Koin for Android & Compose (version `4.2.2`)
* **Local Database**: Room DB (version `2.8.4` with KSP compiler) for storing installation history
* **Concurrency & Flow**: Kotlin Coroutines and reactive Kotlin Flows (`SharedFlow` is used to implement a lightweight, application-wide `InstallerEventBus` bridging Broadcast Receivers with ViewModels)
* **Serialization & DateTime**: Kotlinx Serialization (JSON) and Kotlinx Datetime

---

## Core Architecture

Brokk follows **Clean Architecture** guidelines, divided into three layers:

```
app/src/main/java/com/valhalla/brokk/
├── app/               # Application-level entry points (App.kt, AppRoute.kt)
├── data/              # Database entities, Room DB DAOs, repositories, and receivers
├── di/                # Koin Dependency Injection Modules configuration
├── domain/            # Core business models, interfaces, and the state-machine logic
└── presentation/      # Main UI screens, view models, custom composables, and themes
```

### Architectural Flow

1. **Domain Layer**:
   * Defines repository interfaces (`InstallerRepository`, `AppAnalyzer`, `HistoryRepository`).
   * Models the installation lifecycle via the `InstallState` sealed interface (`Idle`, `Parsing`, `ReadyToInstall`, `Installing`, `UserConfirmationRequired`, `Success`, `Error`).
   * Implements `InstallerEventBus` as an event-driven flow synapse for asynchronous updates from system receivers.
2. **Data Layer**:
   * `InstallerRepositoryImpl` manages `PackageInstaller` sessions. It reads incoming package streams (from file/content URIs), decompresses Zip streams on-the-fly to locate split APKs, and pipes data directly into active system installation sessions.
   * `AppAnalyzerImpl` copies the package URI temporarily to extract target metadata (package name, version, app label, app icon) prior to user confirmation.
   * `InstallReceiver` intercepts installation callback broadcasts (`com.valhalla.brokk.INSTALL_STATUS`), processes OS outcomes (success, pending user action, failures), and communicates status back to the application bus.
3. **Presentation Layer**:
   * `MainActivity` serves as the primary dashboard displaying the user interface.
   * `PortableInstallerActivity` is registered as a translucent overlay activity. It intercepts incoming `Intent.ACTION_VIEW` intents from other apps (e.g. file managers, browsers) for all supported package files, rendering a floating bottom sheet (`PortableInstaller`) over the current screen context.

---

## Project Structure & Current Status

* **Home Screen (`HomeScreen.kt`)**: Contains a bottom navigation bar with three sections:
  1. **Installer**: Fully implemented. Allows the user to select local package files (via Android's Storage Access Framework) and proceed with installation.
  2. **History**: A partial placeholder showing "Work in progress". Although `HistorySheet.kt` and `HistoryViewModel.kt` exist to display and clear the Room-based installation log, they are not yet fully integrated into the home navigation layout as a dedicated view.
  3. **Share Apps (Bifrost)**: A total placeholder showing "Work in progress" intended for a future peer-to-peer app sharing protocol.
* **Database Logs**: History records (`HistoryRecord`) are successfully saved into the local Room database (`brokk_db`) whenever a package installation reaches `Success`.

---

## Future Pathways & Development Roadmap

Brokk is currently in its early phases. Below are key pathways and features that would make excellent expansion options:

### 1. Integrate the Bifrost App Sharing Protocol
* **Package Export**: Implement functionality to package currently installed user/system apps into shareable `.apks` or `.xapk` bundles. This involves reading split APK paths from `ApplicationInfo.splitSourceDirs` and archiving them with a manifest.
* **P2P Transfer Engine**: Build a local network transfer service using Wi-Fi Direct (P2P), Local Hotspot, or Network Service Discovery (NSD) to share packaged apps directly between devices running Brokk without internet connectivity.

### 2. Complete the History Screen
* **Tab Integration**: Replace the home-screen placeholder for the History tab with a full-page scrollable log of past installations.
* **Interactive Logs**: Allow users to tap a log entry to view application status, launch the installed app directly, reinstall/update it if a source path is cached, or uninstall the app from the device.

### 3. OBB and Data Directory Expansion Support
* **Game Bundles**: Zipped `.xapk` installers often bundle large expansion files (`.obb` files) or configuration directories under `Android/obb/` or `Android/data/`.
* **Auto-Extraction**: Update `InstallerRepositoryImpl` to parse and extract these folders during assembly, transferring them to the correct storage locations (requires requesting appropriate storage access permissions on newer Android versions).

### 4. Batch Installation Support
* **Queue Management**: Allow users to select multiple `.apk`/`.xapk` files at once.
* **Sequential Session Processing**: Create a background queue that processes each installation session sequentially, providing a consolidated progress bar in the UI.

### 5. Detailed Package Inspector
* **Security & Permission Audits**: Prior to installation, display additional metadata in the analyzer sheet: target SDK level, list of requested permissions (flagging sensitive permissions like location or background services), signature certificates, and supported CPU architectures (`arm64-v8a`, `armeabi-v7a`, `x86_64`).
