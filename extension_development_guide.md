# YAMFF Extension Development Guide

YAMFF (Yet Another Mi-FreeForm) provides a robust, system-level freeform window engine. By developing **Extensions**, you can add enhanced features such as sidebars, gesture triggers, or custom launchers without modifying the YAMFF core code.

## 1. Security & Zero-Trust Model

To ensure system integrity, YAMFF employs a **Zero-Trust** security model:

- **Permission Declaration**: Extensions must declare the following private permission in their `AndroidManifest.xml`:
  `io.github.sterlingshell.yamff.permission.MANAGE_FREEFORM`
- **Explicit Authorization**: All extensions (including debug builds with matching signatures) must be manually enabled by the user in the "Extension Center" of the YAMFF Manager app.
- **Access Control**: Any IPC call (including metadata queries like `getVersionName`) made by an unauthorized UID will immediately trigger a `SecurityException`.

---

## 2. Quick Start (Using YAMFF-SDK)

We strongly recommend using the official `YAMFF-SDK` to simplify the IPC handshake and lifecycle management.

### Step 1: Integrate the SDK

Include the `YAMFF-SDK` in your project's dependencies:

```kotlin
dependencies {
    // If developing within the YAMFF multi-module project
    implementation(project(":yamff-sdk"))
}
```

### Step 2: Configure AndroidManifest.xml

Declare the required permission and inherit from `YAMFFExtensionProvider` to receive the control Binder.

```xml
<manifest xmlns:android="http://schemas.android.com/apk/res/android"
    package="com.example.yamff.extension">

    <!-- Required permission -->
    <uses-permission android:name="io.github.sterlingshell.yamff.permission.MANAGE_FREEFORM" />

    <application>
        <!-- The core receiver: automatically handles the YAMFF Binder handshake -->
        <!-- The authority MUST follow the "[packageName].yamff.provider" format -->
        <provider
            android:name=".MyYAMFFReceiver"
            android:authorities="com.example.yamff.extension.yamff.provider"
            android:exported="true" />
            
        <!-- Optional: Extension settings activity discovered by YAMFF Manager -->
        <activity android:name=".SettingsActivity" android:exported="true">
            <intent-filter>
                <action android:name="io.github.sterlingshell.yamff.action.FREEFORM_EXTENSION" />
            </intent-filter>
        </activity>
    </application>
</manifest>
```

### Step 3: Implement the Receiver

```kotlin
class MyYAMFFReceiver : YAMFFExtensionProvider() {
    override fun onYAMFFConnected(service: IFreeform) {
        // Triggered when a connection to YAMFF is established.
        // Perform your initialization logic here.
    }
}
```

### Step 4: Call APIs via the YAMFF Facade

The SDK provides a `YAMFF` singleton object for easy interaction:

```kotlin
// Check authorization and activation status
if (YAMFF.isActivated) {
    YAMFF.createWindow()
} else {
    // Prompt the user to enable the extension in YAMFF Manager
}
```

---

## 3. Core API Reference

The `YAMFF` object exposes the following capabilities:

| Method | Description |
| :--- | :--- |
| `createWindow()` | Creates a new empty freeform window. |
| `currentToWindow()` | Moves the current foreground task into a freeform window. |
| `openAppList()` | Opens the YAMFF default app picker. |
| `resetAllWindow()` | Resets the layout of all active windows. |
| `getVersionName()` | Returns the YAMFF core version (requires authorization). |
| `registerConfigChangeListener(...)` | Subscribes to global YAMFF configuration changes. |

---

## 4. Best Practices

1.  **Headless Mode**: Extensions can skip the `android.intent.category.LAUNCHER` category to avoid cluttering the app drawer. Users can manage your extension directly via the YAMFF Manager.
2.  **Death Handling**: The SDK handles `linkToDeath` internally. When the YAMFF core restarts (e.g., during an Xposed reload), `YAMFF.isActivated` will automatically reflect the state.
3.  **Background Dispatching**: IPC calls are synchronous. Avoid calling heavy YAMFF methods on the Main Thread.
