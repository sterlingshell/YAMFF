# YAMFF 扩展开发指南 (Extension Development Guide)

YAMFF (Yet Another Mi-FreeForm) 提供了一套系统级的小窗引擎。通过开发 **扩展 (Extension)**，您可以为 YAMFF 增加侧边栏、手势唤起、助手代显等增强功能，而无需修改 YAMFF 本体代码。

## 1. 安全与准入要求

为了确保系统安全，只有满足以下条件的扩展才能获得小窗控制权：

- **权限声明**：必须在 `AndroidManifest.xml` 中声明私有权限：
  `io.github.sterlingshell.yamff.permission.MANAGE_FREEFORM`
- **授权机制**：所有扩展应用必须在 YAMFF 管理器的“扩展中心”由用户手动开启授权开关。YAMFF 采用零信任安全模型，未授权应用调用任何接口（包括获取版本号）都将触发 `SecurityException`。

---

## 2. 快速开始 (使用 YAMFF-SDK)

强烈建议使用官方提供的 `YAMFF-SDK` 模块进行开发，它封装了复杂的 IPC 握手逻辑。

### 步骤 1：集成 SDK

在您的项目中引入 `YAMFF-SDK`。如果您是在 YAMFF 项目内开发，可以直接依赖：

```kotlin
dependencies {
    implementation(project(":YAMFF-SDK"))
}
```

### 步骤 2：添加权限与声明 Provider

在扩展应用的 `AndroidManifest.xml` 中添加权限声明，并继承 `YAMFFExtensionProvider`。

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.yamff.extension">

    <!-- 必须声明此权限 -->
    <uses-permission android:name="io.github.sterlingshell.yamff.permission.MANAGE_FREEFORM" />

    <application>
        <!-- 核心：用于自动接收 YAMFF 控制权 -->
        <!-- 注意：authorities 必须严格遵循 [packageName].yamff.provider 格式 -->
        <provider
            android:name=".MyYAMFFReceiver"
            android:authorities="com.example.yamff.extension.yamff.provider"
            android:exported="true" />
            
        <!-- 可选：扩展设置页面，管理器会自动发现 -->
        <activity android:name=".SettingsActivity" android:exported="true">
            <intent-filter>
                <action android:name="io.github.sterlingshell.yamff.action.FREEFORM_EXTENSION" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

### 步骤 3：实现 Receiver

```kotlin
class MyYAMFFReceiver : YAMFFExtensionProvider() {
    override fun onYAMFFConnected(service: IFreeform) {
        // 当与 YAMFF 建立连接时触发
        // 您可以在这里做一些初始化逻辑
    }
}
```

### 步骤 4：通过 YAMFF 门面调用 API

SDK 提供了 `YAMFF` 单例，您可以像调用普通方法一样使用小窗功能：

```kotlin
// 检查是否已获得授权并激活
if (YAMFF.isActivated) {
    YAMFF.createWindow()
} else {
    // 提示用户前往管理器开启授权
}
```

---

## 3. 核心 API 接口

通过 `YAMFF` 对象可访问以下功能：

| 方法 | 描述 |
| :--- | :--- |
| `createWindow()` | 创建一个新的空白窗口。 |
| `currentToWindow()` | 将当前应用移动到自由窗。 |
| `openAppList()` | 弹出应用选择器。 |
| `resetAllWindow()` | 重置窗口布局。 |
| `getVersionName()` | 获取 YAMFF 版本（需授权）。 |
| `registerConfigChangeListener(...)` | 监听 YAMFF 全局配置变更。 |

---

## 4. 最佳实践

1.  **无图标化**：扩展应用可以不声明 `android.intent.category.LAUNCHER`，从而避免在桌面创建图标。用户可以通过 YAMFF 管理器直接管理和配置您的扩展。
2.  **死亡处理**：建议为 YAMFF-SDK 注册 `linkToDeath` (SDK 已内建处理)，以便在 YAMFF 核心重启（如 Xposed 环境重载）时重新建立连接。
3.  **多客户端共存**：YAMFF 支持同时向多个授权扩展推送 Binder，请确保您的 Provider 实现能处理重复推送（通常只需覆盖旧实例）。
