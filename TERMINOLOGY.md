# 📚 YAMFF (Yet Another Mi-FreeForm) Code & Architecture Conventions
> **Last synced:** 2026-07-27

To maintain consistency, readability, and a domain-specific vocabulary across the project, all contributors must strictly adhere to the following naming conventions and architectural guidelines. YAMFF is the project abbreviation and must always be written in all-caps.

---

## 1. Core Design Principles

1. **Package as Context:**
   The base package for this project is `io.github.sterlingshell.yamff`. Inside dedicated sub-packages (e.g., `window`), avoid prefixing every class with redundant words like `Freeform` or `YAMFF`. Let the package hierarchy supply the contextual safety.
2. **Entity vs. Capability Distinction:**
* **`Window`**: Represents **concrete UI entities and control logic** within the core module (`Window`, `State`, `Controller`).
* **`Freeform`**: Represents **system-level modes, behaviors, IPC protocols, and global services** (`FreeformManager`, `LaunchRequest`).


3. **Avoid Name Collision:**
   Utility classes and extensions must be qualified with explicit context or module names. Generic, catch-all names like `Utils.kt` are strictly prohibited.

---

## 2. Terminology & Class Mapping

### 🔹 Window Core Module (`...yamff.xposed.window`)

| Legacy / Verbose Name | **Standard Refactored Name** | **Full Class Path** | **Responsibility & Domain Scope** |
| --- | --- | --- | --- |
| `AppWindow` | **`Window`** | `...window.Window` | Core freeform window container (Frame, Titlebar, Content Area). |
| `AppWindowState` | **`State`** | `...window.model.State` | Immutable window state model (Rect, Scale, Alpha, Minimized, etc.). |
| `AppWindowAction` | **`Action`** | `...window.model.Action` | Window interaction intents (`Resize`, `Minimize`, `Close`, etc.). |
| `AppWindowLogic` | **`Controller`** | `...window.logic.Controller` | Lifecycle and state controller for an individual window. |
| `AppListWindow` | **`AppPicker`** | `...window.AppPicker` | Application picker panel for launching apps in freeform mode. |
| `BubbleView` | **`Bubble`** | `...window.Bubble` | Floating bubble representation when a window is minimized. |
| `AppWindowRenderer` | **`Renderer`** | `...window.render.Renderer` | Drawing interface for borders, shadows, and rounded corners. |
| `WindowGestureHandler` | **`GestureHandler`** | `...window.logic.GestureHandler` | Touch event resolver for dragging and resizing gestures. |

### 🔹 Core Services & Inter-Process Communication (`...yamff.xposed.core` & `common`)

| Legacy Name | **Standard Name** | **Responsibility & Domain Scope** |
| --- | --- | --- |
| `StartCommand` | **`LaunchRequest`** | Data parcel requesting an app launch in freeform mode (`common.model`). |
| `YAMFFServer` | **`IpcService`** | Core IPC server handling Xposed-side communications. |
| `YAMFFWindowManager` | **`FreeformManager`** | Global window manager handling Z-Order, focus state, and window layering. |
| `HookSystem` | **`SystemUiHook`** | Aspect entry point for hooks injected specifically into SystemUI. |
| `YAMFFManagerProxy` | **`IpcProxy`** | Host App-side proxy stub for communicating with the Xposed service. |

---

## 3. Standardized Package Tree

```text
io.github.sterlingshell.yamff
├── common
│   ├── ext
│   └── model
│       ├── Config.kt
│       ├── LaunchRequest.kt
│       ├── SurfaceType.kt
│       └── WindowStyle.kt
│
├── manager
│   ├── ui
│   ├── service
│   └── util
│
└── xposed
    ├── compat                       // System OS compatibility layer
    │   └── SystemCompat.kt          // Compatibility entry
    ├── core                         // System services & IPC
    │   ├── FreeformManager.kt
    │   ├── IpcEntry.kt              // IPC entry point
    │   ├── IpcService.kt
    │   └── ConfigManager.kt
    ├── hooks
    │   ├── HookLauncher.kt
    │   ├── SystemUiHook.kt
    │   └── launcher/                // Hook details targeting launcher process
    └── sys
        ├── AppInfoCache.kt
        ├── SystemServices.kt
        └── graphics/
```

---

## 4. Method Naming & Verb Conventions

### 1) Verb Standards

* **`mount()` / `unmount()**`: Specifically used for mounting and unmounting hooks (do NOT use `init` or `destroy`).
* **`launch()`**: Specifically used for launching an app in Freeform window mode (e.g., `launchApp(pkgName: String)`).
* **`minimize()` / `restore()` / `maximize()**`: Standard terms for window state transitions (avoid vague terms like `hide` or `show`).
* **`dismiss()`**: Permanently close and destroy a window or pop-up.

### 2) Omit Redundant Subjects

Since the calling context is already defined by the class, **do NOT repeat class names or parameter types in method names**:

```kotlin
// ❌ BAD: Redundant subject repetition
class Controller {
    fun updateWindowState(state: State) { }
    fun minimizeWindowToBubble() { }
}

// ✅ GOOD: Concise, expressive, and idiomatic
class Controller {
    fun updateState(state: State) { }
    fun minimize() { }
}

```

---

## 5. Future-Proof Naming Rules

To maintain code hygiene when adding new features or components, follow these generative rules:

### 🔹 Component Naming Rules
When creating new components inside a specific sub-package, follow these patterns:

*   **New Window UI Component:** Name it directly after its primary function, omitting `Freeform` or `Window` if inside `...window` package.
    *   *Examples:* `Titlebar.kt`, `ResizeHandle.kt`, `ControlPanel.kt`
*   **New Manager / Service:** Use `...Manager` ONLY for long-lived, stateful singletons managing lifecycle.
    *   *Examples:* `FocusManager.kt`, `LayoutManager.kt`
    *   *Avoid:* Avoid naming stateless helper functions as `Manager` (e.g., use `LayoutCalculator.kt` or `MathUtils.kt`).
    *   *Existing:* `FreeformManager.kt` (global window lifecycle & Z-Order), `ConfigManager.kt` (persistent config state) — all conform to this rule.
*   **New Aspect/Hook Entry:** Always end with `Hook` and prefix with the target process/feature.
    *   *Examples:* `StatusBarHook.kt`, `GestureBarHook.kt`

---

### 🔹 Method Naming Patterns
When writing new functions, adhere to these standard prefixes:

*   **`can...()` / `is...()` / `has...()`**: For boolean properties and checks.
    *   *Good:* `isMinimized()`, `canResize()`, `hasActiveWindow()`
*   **`on...()`**: For event callbacks or passive lifecycle handling.
    *   *Good:* `onWindowFocused()`, `onGestureStarted()`
*   **`handle...()`**: For internal event processing logic.
    *   *Good:* `handleTouch()`, `handleIpcMessage()`
*   **`obtain...()` / `create...()`**: For factories or constructors generating new instances.
    *   *Good:* `createWindow()`, `obtainState()`

---

### 🔹 IPC / AIDL Interface Naming
- All AIDL interfaces **must** use the `I` prefix (Android convention).
- The interface name should describe the **capability**, not the implementation.
    - *Good:* `IFreeform.aidl`, `IOpenCountListener.aidl`
    - *Bad:* `IFreeformService.aidl` (redundant — AIDL is inherently a service)
- Corresponding proxy classes on the host-app side should end with `IpcProxy`.

---

### 🔹 Abbreviation Restrictions
To prevent ambiguities, only the following abbreviations are permitted across the codebase:

| Allowed Abbreviation | Full Term | Forbidden Variations |
| :--- | :--- | :--- |
| `pkg` / `packageName` | Package Name | `pack`, `pName` |
| `ctx` | Context | `cntx` |
| `ext` | Extension | `extension` (in package names) |
| `ipc` | Inter-Process Communication | `remote` (when referring to IPC) |
| `config` | Configuration | `cfg`, `conf` |

*Rule:* Never invent new abbreviations for class names or public APIs beyond those listed above.

---

## 6. Abstraction Guidelines

### When to Create Interfaces
- ✅ Multiple implementations exist or are reasonably expected
- ✅ Need for dependency injection / mocking in tests  
- ✅ Clear contract boundary between modules

### When NOT to Create Interfaces
- ❌ Only one implementation exists
- ❌ "Just in case" future extensibility
- ❌ Wrapping a single utility class

**Rule of Thumb:** Start with concrete `object` or class. Extract interface only when a second implementation is actually needed.

---

## 7. Design Restraint Principles

### YAGNI (You Aren't Gonna Need It)
- Do not add `validateAndFix()` methods that return `this` without validation
- Do not add null checks for non-nullable Kotlin types
- Do not create `copy()` wrappers when data class `copy()` suffices

### KISS (Keep It Simple, Stupid)
- Prefer inline logic to premature abstraction
- One function in a file? Consider merging with related extensions
- Three-line wrapper class? Consider direct usage

---

## 8. Code Duplication Prevention

Before creating a new utility class:
1. Search for existing similar implementations
2. Prefer unifying `xposed/util/` and `manager/util/` when functionality overlaps
3. If both modules need the same utility, place it in `common/`

---

## 9. Architecture Pattern Selection

### Use MVI (Action/State) When:
- Complex UI with multiple interacting states
- Need for unidirectional data flow
- State changes from multiple sources

### Use Simple Callbacks When:
- Single event handling
- Direct method calls suffice
- State is trivially managed

**Current Usage:** `window.model.Action/State` for window UI complexity.
Do NOT apply MVI to simple services or utilities.

