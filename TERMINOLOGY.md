# 📚 YAMFF (Yet Another Mi-FreeForm) Code & Architecture Conventions
> **Last synced:** 2026-07-30

To maintain consistency, readability, and a domain-specific vocabulary across the project, all contributors must strictly adhere to the following naming conventions and architectural guidelines. YAMFF is the project abbreviation and must always be written in all-caps.

---

## 1. Core Design Principles

1. **Modern-First Architecture:**
   YAMFF prioritizes optimization and features for **Android 12 (API 31) and above**. 
2. **Decoupled Compatibility Layer:**
   All version-specific reflection, hidden API workarounds, and fallback logic must be encapsulated within the `compat` package. Core logic (e.g., `Controller`, `Renderer`) should remain clean and use modern APIs by default.
3. **Package as Context:**
   The base package is `io.github.sterlingshell.yamff`. Inside sub-packages (e.g., `window`), avoid prefixing classes with redundant words like `Freeform` or `YAMFF`.
4. **Entity vs. Capability Distinction:**
    - **`Window`**: Concrete UI entities and control logic (`Window`, `State`, `Controller`).
    - **`Freeform`**: System-level modes, IPC protocols, and global services (`FreeformManager`).
5. **Avoid Name Collision & Role Ambiguity:**
   Generic names like `Utils.kt` are prohibited. Use explicit suffixes in the Manager UI:
    - **`Screen`**: Top-level Composable screens (`HomeScreen`).
    - **`ViewModel`**: AAC ViewModels (`SettingsViewModel`).
    - **`Bridge`**: Reactive data mediators between App and Service (`ConfigBridge`).

---

## 2. Terminology & Class Mapping

### 🔹 Window Core Module (`...yamff.xposed.window`)

| Standard Name | **Responsibility & Domain Scope** |
| --- | --- |
| **`Window`** | Core freeform window container (Frame, Titlebar, Content Area). |
| **`State`** | Immutable window state model (Rect, Scale, Alpha, etc.). |
| **`Action`** | Window interaction intents (`Resize`, `Minimize`, `Close`, etc.). |
| **`Controller`** | Lifecycle and state controller for an individual window. |
| **`AppPicker`** | Application picker panel for launching apps in freeform mode. |
| **`Bubble`** | Floating bubble representation when a window is minimized. |
| **`Renderer`** | Drawing interface for borders, shadows, and rounded corners. |
| **`GestureHandler`** | Touch event resolver for dragging and resizing gestures. |

### 🔹 Models & Data Parcels (`...common.model`)

| Standard Name | **Responsibility & Domain Scope** |
| --- | --- |
| **`LaunchRequest`** | Data parcel requesting an app launch in freeform mode. |
| **`Config`** | Global persistent configuration model. |
| **`RecentTaskMode`** | Enum defining how windows appear in Recents (`Native`, `Hidden`, `Decorated`). |

### 🔹 Core Services & IPC (`...yamff.xposed.core` & `manager.service`)

| Standard Name | **Responsibility & Domain Scope** |
| --- | --- |
| **`IpcService`** | Core IPC server handling Xposed-side communications. |
| **`FreeformManager`** | Global window manager handling Z-Order, focus, and layering. |
| **`IpcProxy`** | Host App-side proxy stub for communicating with the Xposed service. |
| **`ModuleContextBridge`** | `[Planned]` Logic for injecting module resources into system processes. Currently handled by `io.github.qauxv`. |

---

## 3. Specialized Systems

### 🔹 Extension System
- **`Extension`**: A functional plugin for YAMFF.
- **`ExtensionRegistry`**: In-process center for managing authorized extensions and UID mappings.
- **`ExtensionDiscovery`**: Logic for scanning and identifying compatible extensions in the system.

### 🔹 Renderer Hierarchy
- **`Renderer`** (Interface): Defines the contract for window drawing.
- **`BaseRenderer`** (Abstract): Provides common logic like animations and layout updates.
- **`impl/`** (Package): Contains concrete implementations (e.g., `FramedRenderer`, `MinimalRenderer`).

### 🔹 Recent Tasks (Recents)
- **`Task Mode`**: Global policy for small windows in Recents.
    - `Native`: No intervention.
    - `Hidden`: Completely removed from the list.
    - `Decorated`: Customized snapshots with window frames.
- **`Snapshot Background`**: Filling strategy for empty space in snapshots (e.g., `Blur`, `Transparent`).

---

## 4. Standardized Package Tree

```text
io.github.sterlingshell.yamff
├── common
│   ├── Constants.kt                 // Global static constants
│   ├── ext                          // Functional extensions (e.g., ViewExt.kt)
│   └── model                        // All data models (Config, LaunchRequest, etc.)
│
├── data
│   └── bridge                       // ConfigBridge, ExtensionsBridge
│
├── di
│   └── KoinModules.kt               // Industry-standard DI center
│
├── manager
│   ├── App.kt                       // Application entry point
│   ├── provider                     // External process discovery & Binder injection
│   ├── service                      // IpcProxy, TileServices
│   └── ui
│       ├── about
│       ├── common                   // LocalProviders.kt
│       ├── components               // Reusable cards, items, dialogs
│       ├── extensions
│       ├── features.settings        // screens/ (Advanced, Appearance, Behavior, etc.)
│       ├── home
│       └── theme
│
└── xposed
    ├── compat                       // ONLY place for OS version-specific logic
    ├── core                         // ConfigManager, ExtensionRegistry, FreeformManager, IpcEntry, IpcService
    ├── hooks
    │   ├── HookLauncher.kt          // Hook lifecycle orchestrator
    │   ├── SystemUiHook.kt          // SystemUI process hook entry
    │   └── launcher                 // PopupHook, RecentsHook, TaskbarHook, etc.
    ├── sys                          // AppInfoCache, ExtensionDiscovery, SystemServices
    │   └── graphics                 // GraphicsExt, RoundedDrawable
    ├── util
    │   ├── MainThreadQueue.kt
    │   ├── Toast.kt
    │   └── ext                      // Xposed-specific extensions (Context, Log, Hooks, etc.)
    └── window                       // The Core Window Module
        ├── logic                    // Controller, GestureHandler, AutoHideHandler
        ├── model                    // Action, State
        └── render                   // Renderer, BaseRenderer, impl/ (Concrete renderers)

io.github.qauxv                      // [Legacy] Transitional area for ModuleContextBridge logic
```

---

## 5. Third-Party & Legacy Code (e.g., QAuxiliary/qauxv)

1. **Absorption Principle:** 
   Do not merely "move" legacy code. Extract the core logic and refactor into idiomatic Kotlin extensions (e.g., `Context.toModuleContext()`) within the YAMFF namespace.
2. **Compliance & Credits:**
   When absorbing logic from projects like **QAuxiliary (AGPL-3.0)**:
    - **MUST** retain original copyright notices.
    - **MUST** include a credit/thank-you note in the file header.
    - **MUST** explicitly state the source project to maintain license transparency.

---

## 6. Naming Conventions

- **Verbs**: `mount`/`unmount` (hooks), `launch` (apps), `minimize`/`restore` (state), `dismiss` (permanent close).
- **Conciseness**: Omit redundant subjects. Use `updateState(state)` instead of `updateWindowState(state)`.
- **Extensions**: `*Ext.kt` is the preferred naming pattern for utility extensions (e.g., `JsonExt.kt`), replacing the forbidden `Utils.kt`.

---

## 7. IPC / AIDL Interface Naming

- All AIDL interfaces **MUST** use the `I` prefix (e.g., `IFreeform.aidl`).
- The interface name should describe the **capability**, not the implementation.
- Host-app side proxy classes (if existing) should end with **`IpcProxy`**.
