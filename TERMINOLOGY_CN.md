# 📚 YAMFF 代码与架构规范

> 最后同步：2026-07-27

本项目所有代码须遵守以下命名规范和架构约定。目的就三个：**风格统一、一眼能读懂、术语不歧义**。

---

## 1. 核心设计原则

### 包名即上下文

基础包：`io.github.sterlingshell.yamff`

进了子包（比如 `window`），类名就别再带 `Freeform`、`Yam` 这种前缀了——包路径已经告诉你它是什么了。

### `Window` vs `Freeform` 的分工

| 前缀 | 指什么 | 举例 |
|---|---|---|
| `Window` | 核心模块里的具体 UI 实体和控制逻辑 | `Window`、`State`、`Controller` |
| `Freeform` | 系统级模式、行为、IPC 协议、全局服务 | `FreeformManager`、`LaunchRequest` |

### 禁止泛化命名

工具类、扩展函数必须带上明确的模块/功能限定词。`Utils.kt` 这种名字**不允许出现**。

---

## 2. 术语与类映射

### 🔹 窗口核心模块（`...yamff.xposed.window`）

| 旧名（废弃） | 现用名 | 路径 | 干什么的 |
|---|---|---|---|
| AppWindow | `Window` | `...window.Window` | 自由窗口容器（框架、标题栏、内容区） |
| AppWindowState | `State` | `...window.model.State` | 不可变窗口状态（Rect、Scale、Alpha、是否最小化等） |
| AppWindowAction | `Action` | `...window.model.Action` | 窗口交互意图（Resize / Minimize / Close 等） |
| AppWindowLogic | `Controller` | `...window.logic.Controller` | 单个窗口的生命周期 & 状态控制 |
| AppListWindow | `AppPicker` | `...window.AppPicker` | 应用选择面板（选哪个 app 开自由窗口） |
| BubbleView | `Bubble` | `...window.Bubble` | 窗口最小化后的悬浮气泡 |
| AppWindowRenderer | `Renderer` | `...window.render.Renderer` | 边框、阴影、圆角绘制 |
| WindowGestureHandler | `GestureHandler` | `...window.logic.GestureHandler` | 拖拽 / 缩放手势的触摸事件分发 |

### 🔹 核心服务 & IPC（`...yamff.xposed.core` / `common`）

| 旧名（废弃） | 现用名 | 干什么的 |
|---|---|---|
| StartCommand | `LaunchRequest` | 启动自由窗口的请求数据包（`common.model`） |
| YAMFFServer | `IpcService` | Xposed 侧 IPC 通信服务 |
| YAMFFWindowManager | `FreeformManager` | 全局窗口管理：Z-Order、焦点、层级 |
| HookSystem | `SystemUiHook` | SystemUI 进程的 Hook 入口 |
| YAMFFManagerProxy | `IpcProxy` | 宿主 App 侧的 IPC 代理，用来跟 Xposed 服务通信 |

---

## 3. 标准包结构

```
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
     ├── compat                       // 系统 API 兼容层
     │   └── SystemCompat.kt          // 兼容入口
     ├── core                         // 系统服务 & IPC
     │   ├── FreeformManager.kt
     │   ├── IpcEntry.kt              // IPC 入口
     │   ├── IpcService.kt
     │   └── ConfigManager.kt
     ├── hooks
     │   ├── HookLauncher.kt
     │   ├── SystemUiHook.kt
     │   └── launcher/                // Launcher 进程的 Hook 细节
     └── sys
         ├── AppInfoCache.kt
         ├── SystemServices.kt
         └── graphics/
```

---

## 4. 方法命名 & 动词约定

### 动词表

| 动词 | 用在哪 | 别用 |
|---|---|---|
| `mount()` / `unmount()` | Hook 的挂载 / 卸载 | ~~init~~ / ~~destroy~~ |
| `launch()` | 以自由窗口模式启动 App | — |
| `minimize()` / `restore()` / `maximize()` | 窗口状态切换 | ~~hide~~ / ~~show~~ |
| `dismiss()` | 彻底关闭并销毁窗口 / 弹窗 | — |

### 方法名里别重复类名

类本身已经提供了上下文，方法名不用再写一遍：

```kotlin
// ❌ 啰嗦
class Controller {
    fun updateWindowState(state: State) { }
    fun minimizeWindowToBubble() { }
}

// ✅ 干净
class Controller {
    fun updateState(state: State) { }
    fun minimize() { }
}
```

---

## 5. 新增组件的命名规则

加新功能、新组件时，按下面的套路来。

### 组件命名

| 场景 | 规则 | 示例 |
|---|---|---|
| `window` 包内的 UI 组件 | 直接按功能命名，**不加** `Freeform` / `Window` 前缀 | `Titlebar.kt`、`ResizeHandle.kt`、`ControlPanel.kt` |
| Manager / Service | 只有**长生命周期、有状态、管生命周期**的单例才叫 `...Manager` | `FocusManager.kt`、`LayoutManager.kt` |
| 无状态工具函数 | 别叫 Manager，叫 `Calculator` / `Helper` / `Utils` | `LayoutCalculator.kt` |
| Hook 入口 | 以 `Hook` 结尾，前面加目标进程/功能名 | `StatusBarHook.kt`、`GestureBarHook.kt` |

> 现有的 `FreeformManager`（全局窗口生命周期 & Z-Order）、`ConfigManager`（持久化配置）都符合这条规则。

### 方法前缀

| 前缀 | 用途 | 示例 |
|---|---|---|
| `is...()` / `can...()` / `has...()` | 布尔判断 | `isMinimized()`、`canResize()`、`hasActiveWindow()` |
| `on...()` | 事件回调 / 生命周期通知 | `onWindowFocused()`、`onGestureStarted()` |
| `handle...()` | 内部事件处理 | `handleTouch()`、`handleIpcMessage()` |
| `create...()` / `obtain...()` | 工厂方法，返回新实例 | `createWindow()`、`obtainState()` |

### IPC / AIDL 接口

- AIDL 接口一律加 `I` 前缀（Android 惯例）。
- 接口名描述**能力**，不描述实现。
    - ✅ `IFreeform.aidl`、`IOpenCountListener.aidl`
    - ❌ `IFreeformService.aidl`（AIDL 本身就是服务，`Service` 多余）
- 宿主 App 侧的代理类以 `IpcProxy` 结尾。

### 缩写白名单

**只允许**以下缩写，其余一律用全称：

| 允许 | 全称 | 禁止的写法 |
|---|---|---|
| `pkg` / `packageName` | Package Name | ~~pack~~、~~pName~~ |
| `ctx` | Context | ~~cntx~~ |
| `ext` | Extension（包名中） | ~~extension~~（包名中） |
| `ipc` | Inter-Process Communication | ~~remote~~（指 IPC 时） |
| `config` | Configuration | ~~cfg~~、~~conf~~ |

类名和公共 API 里不许自创缩写。

---

## 6. 什么时候该抽接口

**该抽：**
- 确实有（或大概率会有）多个实现
- 需要 DI / 测试时 mock
- 模块之间有清晰的契约边界

**别抽：**
- 就一个实现
- "万一以后要扩展呢"——不要
- 只是包一层工具类

> 一般原则：先写具体的 `object` 或 class。等真的出现第二个实现了，再提接口。

---

## 7. 别过度设计

### YAGNI

- 别写一个 `validateAndFix()` 然后里面直接 `return this`，什么也没验证
- Kotlin 非空类型不需要 null check
- data class 自带 `copy()`，别再包一层

### KISS

- 能内联就内联，别急着抽象
- 一个文件就一个函数？合并到相关的扩展文件里去
- 三行的包装类？直接用原来的就行

---

## 8. 别重复造轮子

写新工具类之前，先做三件事：

1. **搜一下**——是不是已经有人写过类似的了
2. `xposed/util/` 和 `manager/util/` 功能重叠的，**合并**
3. 两个模块都要用的工具，放 `common/`

---

## 9. 架构模式怎么选

| 用 MVI（Action / State） | 用简单回调 |
|---|---|
| UI 复杂，多个状态互相影响 | 单一事件处理 |
| 需要单向数据流 | 直接调方法就够了 |
| 状态来源多 | 状态管理很简单 |

当前项目里，`window.model.Action / State` 走 MVI——因为窗口 UI 确实复杂。

简单的 Service、工具类**不要**套 MVI。