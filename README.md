<p align="center">
  <img src="assets/banner.png" alt="Treasure Banner" width="100%">
</p>

<h1 align="center">Treasure</h1>

<p align="center">
  <strong>Enterprise-Grade Video Game Deal Tracker & Cloud-Connected Media Companion for Android</strong>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Kotlin-2.2.10+-7F52FF?logo=kotlin&logoColor=white" alt="Kotlin">
  <img src="https://img.shields.io/badge/Android%20SDK-35%20(VanillaIceCream)-3DDC84?logo=android&logoColor=white" alt="Android SDK 35">
  <img src="https://img.shields.io/badge/Jetpack_Compose-Material_3-4285F4?logo=jetpackcompose&logoColor=white" alt="Compose">
  <img src="https://img.shields.io/badge/Hilt-2.51+-0052CC?logo=dagger&logoColor=white" alt="Dagger Hilt">
  <img src="https://img.shields.io/badge/Room%20DB-2.6+-4285F4?logo=sqlite&logoColor=white" alt="Room">
  <img src="https://img.shields.io/badge/Paging_3-RemoteMediator-34A853?logo=google&logoColor=white" alt="Paging 3">
  <img src="https://img.shields.io/badge/Coil-3.0+-FF4081?logo=coil&logoColor=white" alt="Coil 3">
  <img src="https://img.shields.io/badge/Media3-ExoPlayer_1.4+-E91E63?logo=youtube&logoColor=white" alt="Media3">
  <img src="https://img.shields.io/badge/WorkManager-Periodic_Sync-FF6F00?logo=android&logoColor=white" alt="WorkManager">
  <img src="https://img.shields.io/badge/Backend-Spring_Boot_BFF-6DB33F?logo=springboot&logoColor=white" alt="Spring Boot">
  <img src="https://img.shields.io/badge/License-MIT-yellow.svg" alt="License">
</p>

---

## 1. Overview & Technical Value Proposition

**Treasure** is a modern, reactive, offline-first Android application designed to track and discover digital video game discounts, historic pricing lows, and free game promotions across major digital storefronts, including **Steam**, **GOG**, **Epic Games Store**, and more.

Built according to modern Android development standards, Treasure pairs an unbundled **Jetpack Compose / Material 3** user experience with an enterprise **Spring Boot Backend-for-Frontend (BFF)**. The client utilizes an offline-first caching layer with **Room Database** and **Paging 3 RemoteMediator**, background price drop verification via **AndroidX WorkManager**, zero-friction Google Sign-In via **Credential Manager**, and an asynchronous cloud wallpaper sync pipeline targeting the **Google Drive API**.

Companion Backend: [Treasure Backend Repository](https://github.com/ArnabKrJana/Treasure-Backend.git)

---

## 2. System Architecture & Workflows

### 2.1 End-to-End System Blueprint

```mermaid
flowchart LR
    subgraph Client["Android Application (:app)"]
        UI["Jetpack Compose UI"]
        VM["Hilt ViewModels"]
        Repo["Repository Layer"]
        Room["Room Database (Offline Cache)"]
        WM["WorkManager (PriceSyncWorker)"]
    end

    subgraph BFF["Spring Boot BFF Backend"]
        Gateway["REST Gateway & Auth Controller"]
        SyncService["Deal Aggregation Service"]
        Queue["RabbitMQ Message Broker"]
        RedisCache["Redis Cache"]
        PostgresDB[(PostgreSQL Database)]
        CloudWorker["Cloud Wallpaper Worker"]
    end

    subgraph External["External APIs & Services"]
        ITAD["IsThereAnyDeal API"]
        SteamAPI["Steam Store API"]
        IGDB["IGDB / Twitch API"]
        GoogleDrive["Google Drive API"]
    end

    UI <--> VM
    VM <--> Repo
    Repo <--> Room
    Repo <--> Gateway
    WM <--> Gateway

    Gateway <--> RedisCache
    Gateway <--> PostgresDB
    SyncService <--> ITAD
    SyncService <--> SteamAPI
    SyncService <--> IGDB
    SyncService --> PostgresDB

    Gateway --> Queue
    Queue --> CloudWorker
    CloudWorker --> GoogleDrive
```

---

### 2.2 Offline-First Paging 3 Architecture (`RemoteMediator`)

Categorized deals (Hot Deals, Lowest Price Ever, Mac Deals, Linux Deals) are loaded seamlessly with bidirectional network and disk coordination:

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant UI as HomeScreen (LazyRow / LazyColumn)
    participant Paging as Paging 3 Flow
    participant Mediator as DealRemoteMediator
    participant DB as Room Database (DealDao)
    participant API as TreasureBackendApi

    UI->>Paging: Request page stream for category
    Paging->>DB: Query cached deals for category
    DB-->>UI: Instantly emit cached deals to screen
    
    alt Remote Boundary Reached (REFRESH / APPEND)
        Paging->>Mediator: load(LoadType, PagingState)
        Mediator->>DB: Query RemoteKeys for category
        Mediator->>API: getHotDeals(page, size)
        API-->>Mediator: SpringPageResponseDto<DealDto>
        
        rect rgb(240, 248, 255)
            note over Mediator, DB: Single Room Transaction
            Mediator->>DB: Insert DealEntities (composite key: id + category)
            Mediator->>DB: Upsert RemoteKeysEntity (nextPage, prevPage)
        end
        
        DB-->>Paging: Auto-invalidate & emit updated PagingData
        Paging-->>UI: Render updated deal stream smoothly
    end
```

---

### 2.3 Asynchronous Google Drive Wallpaper Pipeline

Treasure allows users to backup 4K screenshots and desktop wallpapers directly to their private Google Drive storage without consuming local device storage or mobile bandwidth:

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant App as DetailScreen / DetailViewModel
    participant Backend as Spring Boot BFF
    participant Rabbit as RabbitMQ (drive.sync.queue)
    participant Worker as Backend Cloud Worker
    participant Drive as Google Drive Cloud

    User->>App: Clicks "Sync to Drive" on Wallpaper
    App->>App: Check isDriveLinked (SettingsRepository)
    
    opt Not Linked
        App->>User: Launch Google Sign-In with drive.file scope
        User-->>App: Grants authorization code
        App->>Backend: linkGoogleDrive(serverAuthCode)
        Backend-->>App: { driveLinked: true }
    end

    App->>Backend: queueDriveSync(gameId, imageUrl)
    Backend->>Rabbit: Publish DriveSyncItemDto
    Backend-->>App: HTTP 202 Accepted
    
    par Asynchronous Processing
        Rabbit->>Worker: Consume DriveSyncItemDto
        Worker->>Worker: Download raw asset stream
        Worker->>Drive: Upload to "Treasure Wallpapers" folder
        Worker->>Backend: Mark job status = SUCCESS
    and Polling from Android Client
        loop Every 3 Seconds
            App->>Backend: checkDriveSyncStatus([gameId])
            Backend-->>App: { status: "PENDING" | "SUCCESS" }
        end
        App->>App: Set uploadStatus = SUCCESS
    end
```

---

## 3. Core Feature Highlights

| Feature | Technical Implementation | Highlights |
| :--- | :--- | :--- |
| **Deal Discovery & Filtering** | `DealRemoteMediator`, `Paging 3`, `Room` | Multi-category scrolling feeds (`HOT_DEALS`, `LOWEST_PRICE`, `MAC_DEALS`, `LINUX_DEALS`) with composite primary keys (`["id", "category"]`) preventing cross-category key collisions. |
| **Immersive Media & Overlays** | `AndroidX Media3 ExoPlayer`, `Coil 3` | In-app 1080p trailer streaming with lifecycle observer management (`ON_PAUSE` / `ON_STOP`), pinch-to-zoom Coil image viewer, and dynamic blur backgrounds (`Modifier.blur()`). |
| **Smart Price-Drop Alerts** | `WorkManager`, `PriceSyncWorker` | Battery-conscious, network-constrained periodic background synchronization checking wishlist deals against remote thresholds, dispatching deep-linked Android notifications. |
| **Cloud Wallpaper Backups** | `Google Credential Manager`, `Google Drive API` | Server-assisted cloud uploads using RabbitMQ asynchronous workers with non-blocking status polling and re-authentication challenge handling. |
| **Material You Design System** | `Jetpack Compose Material 3` | Dynamic color extraction from device wallpaper on Android 12+, dark/light high-contrast palettes, and scroll-linked app bar alpha animations. |
| **Security & Credential Storage** | `Security-Crypto EncryptedSharedPreferences` | Hardware-backed keystore token persistence (`MasterKey`), atomic token refresh via OkHttp `TokenAuthenticator`, and circular DI resolution via Dagger `Provider<T>`. |

---

## 4. Repository Structure & Package Documentation Index

Every individual package within the `:app` module features an exhaustive, standalone technical guide detailing its architecture, file taxonomy, sequence flows, and threading models.

| Package | Path | Architectural Layer | Package Documentation Guide |
| :--- | :--- | :--- | :--- |
| **Core Utilities** | `com.example.treasure.utils` | Foundation | [📘 Utilities Architecture Guide](app/src/main/java/com/example/treasure/utils/README.md) |
| **Domain Layer** | `com.example.treasure.domain` | Clean Architecture Domain | [📘 Domain Layer Architecture Guide](app/src/main/java/com/example/treasure/domain/README.md) |
| **Data — Local Persistence** | `com.example.treasure.data.local` | Data Persistence | [📘 Room Local Persistence Guide](app/src/main/java/com/example/treasure/data/local/README.md) |
| **Data — Remote Networking** | `com.example.treasure.data.remote` | Network & API | [📘 Remote Networking Guide](app/src/main/java/com/example/treasure/data/remote/README.md) |
| **Data — Repositories & Mappers** | `com.example.treasure.data.repositoryImpl` | Repository Implementation | [📘 Repositories & Mappers Guide](app/src/main/java/com/example/treasure/data/repositoryImpl/README.md) |
| **Dependency Injection** | `com.example.treasure.di` | Dagger / Hilt DI | [📘 Dependency Injection Guide](app/src/main/java/com/example/treasure/di/README.md) |
| **Background Workers** | `com.example.treasure.workers` | WorkManager Background Tasks | [📘 WorkManager Workers Guide](app/src/main/java/com/example/treasure/workers/README.md) |
| **UI — Design System Theme** | `com.example.treasure.ui.theme` | Presentation (Theme) | [📘 Material 3 Design System Guide](app/src/main/java/com/example/treasure/ui/theme/README.md) |
| **UI — Reusable Components** | `com.example.treasure.ui.uiComponents` | Presentation (Components) | [📘 UI Components & Media Guide](app/src/main/java/com/example/treasure/ui/uiComponents/README.md) |
| **UI — ViewModels** | `com.example.treasure.ui.viewModels` | Presentation (State Holders) | [📘 ViewModels & UDF State Guide](app/src/main/java/com/example/treasure/ui/viewModels/README.md) |
| **UI — Screens** | `com.example.treasure.ui.screens` | Presentation (Screen Views) | [📘 Screens & Composables Guide](app/src/main/java/com/example/treasure/ui/screens/README.md) |
| **UI — Type-Safe Navigation** | `com.example.treasure.ui.navigationGraphs` | Presentation (Navigation) | [📘 Navigation Graphs Guide](app/src/main/java/com/example/treasure/ui/navigationGraphs/README.md) |

---

## 5. Getting Started & Build Instructions

### Prerequisites
- **Android Studio Ladybug (2024.2.1)** or newer
- **JDK 21** configured as your Gradle JDK
- **Android SDK Platform 35 (Android 15)** installed
- Active Google Cloud Console project with:
  - OAuth 2.0 Client IDs (Android Client & Web Server Client)
  - Google Drive API enabled

### Building the Project

1. **Clone the Repository:**
   ```bash
   git clone https://github.com/ArnabKrJana/Treasure-gamedeals.git
   cd Treasure-gamedeals
   ```

2. **Configure Constants:**
   Update your Google Web Client ID and backend server base URLs in:
   - [`Constants.kt`](app/src/main/java/com/example/treasure/utils/Constants.kt)

3. **Compile Kotlin & Verify Build:**
   ```bash
   ./gradlew compileDebugKotlin
   ```

4. **Assemble Debug APK:**
   ```bash
   ./gradlew assembleDebug
   ```

---

## 6. Screenshots & Visual Showcase

<p align="center">
  <img src="assets/screenshots/screenshot_1.png" width="31%" alt="Home Discovery Screen">
  &nbsp;
  <img src="assets/screenshots/screenshot_2.png" width="31%" alt="Game Details & Overlays">
  &nbsp;
  <img src="assets/screenshots/screenshot_3.png" width="31%" alt="Settings & Cloud Backups">
</p>

---

## 7. Credits

Developed by **Arnab Kumar Jana**
- Companion Backend: [Treasure-Backend on GitHub](https://github.com/ArnabKrJana/Treasure-Backend.git)
