# Presentation Layer — ViewModels (`com.example.treasure.ui.viewModels`)

## 1. Architectural Role & Design Philosophy

The `ui.viewModels` package houses the state holders and business logic coordinators for the **Treasure** Android application. 

Every ViewModel is managed by **Hilt** (`@HiltViewModel`) and bound to the Jetpack Compose presentation layer using **Unidirectional Data Flow (UDF)**:
- **State flows downward:** ViewModels expose immutable `StateFlow` and `SharedFlow` streams to Composable screens.
- **Events flow upward:** Screens emit user intents (clicks, text input, navigation triggers) as function calls into ViewModels.
- **State Invalidation & Configuration Resistance:** Heavy Room and repository queries are shared and cached using `.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ...)`, providing a 5-second buffer across activity recreations and configuration changes without thrashing database observers.

```mermaid
flowchart TD
    subgraph UI["Compose UI Layer"]
        Screen["Composable Screen"]
    end

    subgraph VM["Hilt ViewModel"]
        State["StateFlow / SharedFlow (Exposed State)"]
        Handler["Intent / Action Functions"]
        Scope["viewModelScope (SupervisorJob)"]
    end

    subgraph Data["Domain & Data Layer"]
        Repo["Repositories & Room DAOs"]
        API["TreasureBackendApi (Retrofit)"]
        Auth["GoogleAuthHelper & TokenManager"]
        WM["WorkManager"]
    end

    Screen -->|Calls Intent Functions| Handler
    Handler -->|Launches Coroutine| Scope
    Scope -->|Executes IO Operations| Repo
    Scope -->|Network Requests| API
    Scope -->|Credential & Token Tasks| Auth
    Scope -->|Reschedules Periodic Tasks| WM
    Repo -->|Emits Data Updates| State
    State -->|Collects as State in UI| Screen
```

---

## 2. ViewModel Taxonomy & Capabilities

| ViewModel | Key State & Flow Streams | Key Action Methods | Primary Responsibilities |
| :--- | :--- | :--- | :--- |
| [`AuthViewModel.kt`](file:///c:/Users/Arnab%20Kumar%20Jana/Documents/Android%20Projects/Copies/Treasure/app/src/main/java/com/example/treasure/ui/viewModels/AuthViewModel.kt) | `isSessionActive: StateFlow<Boolean>`<br>`isLoading: StateFlow<Boolean>`<br>`error: StateFlow<String?>` | `handleGoogleLogin()`<br>`deleteMyAccount()`<br>`logout()` | Coordinates Google Credential Manager authentication, server JWT verification, automatic post-login cloud wishlist synchronization, and account deletion. |
| [`DetailViewModel.kt`](file:///c:/Users/Arnab%20Kumar%20Jana/Documents/Android%20Projects/Copies/Treasure/app/src/main/java/com/example/treasure/ui/viewModels/DetailViewModel.kt) | `gameDetail: StateFlow<DealEntity?>`<br>`downloadStatuses: StateFlow<Map<String, ActionStatus>>`<br>`isDriveLinked: StateFlow<Boolean>`<br>`uploadStatuses: StateFlow<Map<String, ActionStatus>>`<br>`driveAuthEvent: SharedFlow<Unit>` | `fetchGameDetails()`<br>`toggleFavorite()`<br>`downloadWallpaperLocal()`<br>`initiateDriveSync()`<br>`linkDriveAccount()` | Manages deep game detail enrichment, Room caching, local wallpaper downloads via `DownloadManager`, and asynchronous Google Drive cloud backup with polling. |
| [`HomeScreenViewModel.kt`](file:///c:/Users/Arnab%20Kumar%20Jana/Documents/Android%20Projects/Copies/Treasure/app/src/main/java/com/example/treasure/ui/viewModels/HomeScreenViewModel.kt) | `hotDeals: Flow<PagingData<GameCardItem>>`<br>`lowestPriceDeals: Flow<PagingData<GameCardItem>>`<br>`macDeals: Flow<PagingData<GameCardItem>>`<br>`linuxDeals: Flow<PagingData<GameCardItem>>`<br>`anticipatedGames: StateFlow<List<GameCardItem>>`<br>`favoriteIds: StateFlow<Set<String>>`<br>`cartIds: StateFlow<Set<String>>` | `toggleFavorite()`<br>`toggleCart()` | Powers the home feed with categorized Paging 3 deal streams (`DealRemoteMediator`), hero carousel banners, and reactive sidecar sets for instant heart/cart icon states. |
| [`MainViewModel.kt`](file:///c:/Users/Arnab%20Kumar%20Jana/Documents/Android%20Projects/Copies/Treasure/app/src/main/java/com/example/treasure/ui/viewModels/MainViewModel.kt) | `currentUser: StateFlow<User?>` | *Synchronous state observation* | Exposes top-level authenticated user profile data retrieved from encrypted SharedPreferences to app bars and root navigation graphs. |
| [`NotificationViewModel.kt`](file:///c:/Users/Arnab%20Kumar%20Jana/Documents/Android%20Projects/Copies/Treasure/app/src/main/java/com/example/treasure/ui/viewModels/NotificationViewModel.kt) | `notifications: StateFlow<List<NotificationEntity>>`<br>`unreadCount: StateFlow<Int>` | `clearAll()`<br>`deleteNotification()`<br>`markAllAsRead()` | Observes Room database alerts, maintains unread badge counts, and executes dismissal or read-status mutations. |
| [`SearchViewModel.kt`](file:///c:/Users/Arnab%20Kumar%20Jana/Documents/Android%20Projects/Copies/Treasure/app/src/main/java/com/example/treasure/ui/viewModels/SearchViewModel.kt) | `searchQuery: StateFlow<String>`<br>`searchResults: StateFlow<List<GameCardItem>>`<br>`isSearching: StateFlow<Boolean>` | `onQueryChange()`<br>`performSearch()` | Isolates search input query changes from network execution to avoid API thrashing; queries repository only on explicit submission. |
| [`SettingsViewModel.kt`](file:///c:/Users/Arnab%20Kumar%20Jana/Documents/Android%20Projects/Copies/Treasure/app/src/main/java/com/example/treasure/ui/viewModels/SettingsViewModel.kt) | `uiState: StateFlow<SettingsUiState>` | `updateTheme()`<br>`toggleDynamicColors()`<br>`toggleNotifications()`<br>`updateSyncFrequency()`<br>`clearImageCache()`<br>`linkDriveAccount()`<br>`unlinkDrive()` | Consolidates DataStore user preferences into a single state stream, dynamically updates WorkManager periodic price sync requests, and evicts Coil image caches. |
| [`WishlistViewModel.kt`](file:///c:/Users/Arnab%20Kumar%20Jana/Documents/Android%20Projects/Copies/Treasure/app/src/main/java/com/example/treasure/ui/viewModels/WishlistViewModel.kt) | `wishlistItems: StateFlow<List<UserInteractionEntity>>` | `toggleFavorite()`<br>`toggleCart()` | Exposes a reactive Room stream of wishlist entries and executes favorite/cart state transitions. |

---

## 3. Asynchronous Google Drive Sync & Polling Flow

In [`DetailViewModel`](file:///c:/Users/Arnab%20Kumar%20Jana/Documents/Android%20Projects/Copies/Treasure/app/src/main/java/com/example/treasure/ui/viewModels/DetailViewModel.kt), wallpapers can be backed up to the user's private Google Drive through a server-side asynchronous task pipeline.

```mermaid
sequenceDiagram
    autonumber
    actor User
    participant Screen as DetailScreen
    participant VM as DetailViewModel
    participant Backend as TreasureBackendApi
    participant Drive as Google Drive Cloud

    User->>Screen: Clicks "Sync to Drive" on Wallpaper
    Screen->>VM: initiateDriveSync(imageUrl)
    
    alt Drive Not Linked
        VM-->>Screen: Emits driveAuthEvent (SharedFlow)
        Screen->>User: Prompts Google Sign-In with Drive Scopes
        User->>Screen: Grants OAuth Permission
        Screen->>VM: linkDriveAccount(authCode, pendingUrl)
        VM->>Backend: linkGoogleDrive(request)
        Backend-->>VM: { driveLinked: true }
    end

    VM->>VM: Update uploadStatus -> LOADING
    VM->>Backend: queueDriveSync(itemDto)
    Backend-->>VM: HTTP 200 Accepted

    loop Every 3 Seconds (Polling Job)
        VM->>Backend: checkDriveSyncStatus([gameId])
        Backend->>Drive: Query Asset Upload State
        Backend-->>VM: { status: "PENDING" | "SUCCESS" | "FAILED" }
        
        opt status == "SUCCESS"
            VM->>VM: Update uploadStatus -> SUCCESS
            Note over VM: Polling Job Terminates
        end
        opt status == "FAILED"
            VM->>VM: Reset uploadStatus
            Note over VM: Polling Job Terminates
        end
    end
```

---

## 4. Lifecycle Awareness & Cancellation

1. **`SharingStarted.WhileSubscribed(5000)`:** Ensures database observers halt when the app is placed in the background or during deep navigation transitions, while remaining active during brief configuration rotations (which typically take under 500ms).
2. **`onCleared()` Handling:** Active background polling jobs tracked in `DetailViewModel.pollingJobs` are explicitly cancelled to prevent thread leaks or outbound HTTP polls after the user exits the details screen.
3. **Dispatchers:** Network and disk-intensive tasks are dispatched on `Dispatchers.IO` while Compose-facing states are mutated on the Main thread within `viewModelScope`.
