# Presentation Layer — Design System Theme (`com.example.treasure.ui.theme`)

## 1. Architectural Overview

The `ui.theme` package establishes the **Material Design 3 (M3)** design system for the **Treasure** application. It provides semantic color schemes, dynamic theming support (Android 12+ wallpaper extraction), typography hierarchies, and composition locals for runtime theme querying.

```mermaid
flowchart TD
    subgraph Config["Theme Configuration"]
        UserPref["AppTheme DataStore Setting\n(LIGHT / DARK / SYSTEM)"]
        SystemDetect["isSystemInDarkTheme()"]
    end

    subgraph ThemeEngine["TreasureTheme Composable"]
        ThemeEngineNode["TreasureTheme(darkTheme, dynamicColor)"]
        LocalThemeIsDark["CompositionLocalProvider(LocalThemeIsDark)"]
        MaterialThemeHolder["MaterialTheme(colorScheme, typography)"]
    end

    subgraph UIComponents["Presentation Hierarchy"]
        Screens["Screens & Composables"]
    end

    UserPref --> ThemeEngineNode
    SystemDetect --> ThemeEngineNode
    ThemeEngineNode --> LocalThemeIsDark
    LocalThemeIsDark --> MaterialThemeHolder
    MaterialThemeHolder --> Screens
```

---

## 2. File Taxonomy & Capabilities

| File | Primary Symbols | Architectural Functionality |
| :--- | :--- | :--- |
| [`Color.kt`](file:///c:/Users/Arnab%20Kumar%20Jana/Documents/Android%20Projects/Copies/Treasure/app/src/main/java/com/example/treasure/ui/theme/Color.kt) | `DarkColorScheme`, `LightColorScheme` | Defines complete light and dark color schemes conforming to Material 3 tokens. Implements a high-contrast dark palette tailored for media-heavy game deals. |
| [`Theme.kt`](file:///c:/Users/Arnab%20Kumar%20Jana/Documents/Android%20Projects/Copies/Treasure/app/src/main/java/com/example/treasure/ui/theme/Theme.kt) | `TreasureTheme`, `LocalThemeIsDark` | Core theme wrapper composable. Manages dynamic color detection, configures window status and navigation bars via `LocalView`, and provides dark-mode state via `LocalThemeIsDark`. |
| [`Type.kt`](file:///c:/Users/Arnab%20Kumar%20Jana/Documents/Android%20Projects/Copies/Treasure/app/src/main/java/com/example/treasure/ui/theme/Type.kt) | `Typography`, `RobotoFontFamily` | Type scale definitions providing standard typographic hierarchy across headlines, titles, body, and labels with the Roboto font family. |

---

## 3. Dynamic Color & Theme Determination

The application dynamically decides the active palette based on platform version and user preferences:

```mermaid
flowchart TD
    Start["Launch Composable with darkTheme & dynamicColor"] --> CheckDynamic{"dynamicColor && Android 12+ (SDK 31+)?"}
    CheckDynamic -->|Yes| CheckDarkDynamic{"darkTheme?"}
    CheckDarkDynamic -->|Yes| DynamicDark["dynamicDarkColorScheme(context)"]
    CheckDarkDynamic -->|No| DynamicLight["dynamicLightColorScheme(context)"]
    CheckDynamic -->|No| CheckStaticDark{"darkTheme?"}
    CheckStaticDark -->|Yes| StaticDark["DarkColorScheme (Color.kt)"]
    CheckStaticDark -->|No| StaticLight["LightColorScheme (Color.kt)"]
    
    DynamicDark --> Apply["Apply to MaterialTheme & LocalThemeIsDark"]
    DynamicLight --> Apply
    StaticDark --> Apply
    StaticLight --> Apply
```

---

## 4. Edge-to-Edge System Bars & Composition Locals

1. **System Bar Styling:** When the decor view is attached to a window, `TreasureTheme` sets the system status bar and navigation bar colors to transparent, and configures the `WindowInsetsControllerCompat` to toggle light or dark status bar icons to match the theme.
2. **`LocalThemeIsDark`:** Exposed as a `staticCompositionLocalOf<Boolean>`. Any child composable in the subtree can query `LocalThemeIsDark.current` without needing to pass flags through deep composable signatures.
