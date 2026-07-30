# 📚 YAMFF 代码与架构规范

> 最后同步：2026-07-30

本项目所有代码须遵守以下命名规范和架构约定。目的就三个：**风格统一、一眼能读懂、术语不歧义**。YAMFF 为项目缩写，须保持大写。

---

## 1. 核心设计原则

1. **现代优先架构：**
   YAMFF 优先针对 **Android 12 (API 31) 及以上版本** 进行特性优化和性能开发。
2. **解耦兼容层：**
   所有版本相关的反射、隐藏 API 绕过以及回退逻辑必须封装在 `compat` 包中。核心业务逻辑（如 `Controller`、`Renderer`）应保持纯净，默认使用现代 API。
3. **包名即上下文：**
   基础包：`io.github.sterlingshell.yamff`。在子包（如 `window`）内，类名禁止带冗余的 `Freeform` 或 `YAMFF` 前缀。
4. **实体与能力的区分：**
    - **`Window`**: 指代具体的 UI 实体和控制逻辑（`Window`、`State`、`Controller`）。
    - **`Freeform`**: 指代系统级模式、IPC 协议和全局服务（`FreeformManager`）。
5. **禁止泛化命名：**
   不允许出现 `Utils.kt` 等通用名称。Manager UI 层使用明确的架构后缀：
    - **`Screen`**: 顶级 Composable 页面（如 `HomeScreen`）。
    - **`ViewModel`**: 标准 AAC ViewModel。
    - **`Bridge`**: 负责 App 与 Service 之间响应式数据同步的中转类。

---

## 2. 术语与类映射

### 🔹 窗口核心模块（`...yamff.xposed.window`）

| 标准名 | **职责与定义域** |
| --- | --- |
| **`Window`** | 自由窗口容器（框架、标题栏、内容区）。 |
| **`State`** | 不可变窗口状态模型（Rect、缩放、透明度等）。 |
| **`Action`** | 窗口交互意图（调整大小、最小化、关闭等）。 |
| **`Controller`** | 单个窗口的生命周期与状态控制器。 |
| **`AppPicker`** | 自由窗口应用启动选择面板。 |
| **`Bubble`** | 窗口最小化后的悬浮气泡表现。 |
| **`Renderer`** | 边框、阴影、圆角等视觉元素的渲染接口。 |
| **`GestureHandler`** | 处理拖拽、缩放等手势的触摸事件解析器。 |

### 🔹 数据模型 (`...common.model`)

| 标准名 | **职责与定义域** |
| --- | --- |
| **`LaunchRequest`** | 启动自由窗口的请求数据模型。 |
| **`Config`** | 全局持久化配置模型。 |
| **`RecentTaskMode`** | 最近任务显示模式枚举（`Native`, `Hidden`, `Decorated`）。 |

### 🔹 核心服务 & IPC (`...yamff.xposed.core` / `manager.service`)

| 标准名 | **职责与定义域** |
| --- | --- |
| **`IpcService`** | Xposed 侧核心 IPC 通信服务。 |
| **`FreeformManager`** | 全局窗口管理：处理 Z 轴顺序、焦点和层级。 |
| **`IpcProxy`** | 宿主 App 侧的 IPC 代理桩，用于跨进程通信。 |
| **`ModuleContextBridge`** | `[规划中]` 将模块资源注入系统进程的逻辑。目前暂由 `io.github.qauxv` 承载。 |

---

## 3. 专项体系规范

### 🔹 扩展系统 (Extension System)
- **`Extension`**: YAMFF 的功能插件。
- **`ExtensionRegistry`**: 系统进程内管理已授权扩展及其 UID 映射的中心。
- **`ExtensionDiscovery`**: 扫描并识别系统中兼容扩展的逻辑。

### 🔹 渲染层级 (Renderer Hierarchy)
- **`Renderer`** (接口): 定义窗口绘制的契约。
- **`BaseRenderer`** (抽象类): 提供动画、布局更新等通用逻辑。
- **`impl/`** (包): 存放具体渲染实现（如 `FramedRenderer`, `MinimalRenderer`）。

### 🔹 最近任务 (Recents)
- **`Task Mode`**: 全局最近任务处理策略。
    - `Native`: 原生。不作干预。
    - `Hidden`: 隐藏。从小窗任务列表中彻底移除。
    - `Decorated`: 修饰。显示带窗口修饰的自定义快照。
- **`Snapshot Background`**: 快照背景填充策略（如“模糊”、“透明”）。

---

## 4. 标准包结构

```text
io.github.sterlingshell.yamff
├── common
│   ├── Constants.kt                 // 全局静态常量
│   ├── ext                          // 通用功能扩展 (如 ViewExt.kt)
│   └── model                        // 所有数据模型 (Config, LaunchRequest 等)
│
├── data
│   └── bridge                       // 响应式数据桥接层 (ConfigBridge 等)
│
├── di
│   └── KoinModules.kt               // 工业标准 DI 中心
│
├── manager
│   ├── App.kt                       // Application 入口
│   ├── provider                     // 外部进程发现与 Binder 注入入口
│   ├── service                      // IpcProxy, 磁贴服务等
│   └── ui
│       ├── about
│       ├── common                   // LocalProviders.kt
│       ├── components               // 可复用的卡片、列表项、对话框
│       ├── extensions
│       ├── features.settings        // screens/ (高级、外观、行为设置等)
│       ├── home
│       └── theme
│
└── xposed
    ├── compat                       // 【唯一】存放适配逻辑的地方
    ├── core                         // ConfigManager, ExtensionRegistry, FreeformManager, IpcEntry, IpcService
    ├── hooks
    │   ├── HookLauncher.kt          // Hook 生命周期协调器
    │   ├── SystemUiHook.kt          // SystemUI 进程 Hook 入口
    │   └── launcher                 // 针对桌面进程的各类 Hook 子项
    ├── sys                          // 应用缓存、扩展发现、系统服务访问
    │   └── graphics                 // 快照处理、图形工具类
    ├── util
    │   ├── MainThreadQueue.kt
    │   ├── Toast.kt
    │   └── ext                      // Xposed 特有的扩展 (Context, Log, Hooks 等)
    └── window                       // 窗口核心模块
        ├── logic                    // 控制器、手势处理、自动隐藏逻辑
        ├── model                    // 动作 (Action)、状态 (State)
        └── render                   // 渲染接口、基类及具体实现 (impl/)

io.github.qauxv                      // [遗留] ModuleContextBridge 逻辑的过渡托管区
```

---

## 5. 第三方与遗留代码处理 (如 QAuxiliary/qauxv)

1. **吸收融合原则：** 
   禁止简单的代码“搬运”。须提取核心逻辑（如 AssetPath 注入），并将其重构为地道的 Kotlin 扩展函数，放置于 YAMFF 命名空间下。
2. **合规与致谢：**
   在吸收如 **QAuxiliary (AGPL-3.0)** 等项目逻辑时：
    - **必须** 在代码头部保留原始版权声明。
    - **必须** 包含致谢说明，并明确标注逻辑来源，符合开源协议要求。

---

## 6. 命名约定

- **动词**: `mount`/`unmount` (挂载钩子), `launch` (启动应用), `minimize`/`restore` (状态切换), `dismiss` (永久关闭)。
- **精简性**: 省略冗余主语。使用 `updateState(state)` 而非 `updateWindowState(state)`。
- **扩展模式**: 推荐使用 `*Ext.kt` 命名扩展函数文件（如 `JsonExt.kt`），用于替代被禁止的 `Utils.kt`。

---

## 7. IPC / AIDL 接口规范

- 所有 AIDL 接口 **必须** 加 `I` 前缀（如 `IFreeform.aidl`）。
- 接口名描述 **能力** 而非实现。
- 宿主 App 侧的代理桩类（若存在）应以 **`IpcProxy`** 结尾。
