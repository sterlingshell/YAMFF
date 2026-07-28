# YAMFF

[![GitHub license](https://img.shields.io/github/license/sterlingshell/YAMFF?style=flat-square)](https://github.com/sterlingshell/YAMFF/blob/main/LICENSE)
![Android SDK min 31](https://img.shields.io/badge/Android%20SDK-%3E%3D%2031-brightgreen?style=flat-square&logo=android)
![Xposed Module](https://img.shields.io/badge/Xposed-Module-blue?style=flat-square)
![Architecture MVI](https://img.shields.io/badge/Architecture-MVI-orange?style=flat-square)

Yet Another Mi-FreeForm — **Refactored & Evolved**

**YAMFF** is a modernized, heavily refactored fork of [YAMF](https://github.com/duzhaokun123/YAMF).
The extra **"F"** stands for **Form** — emphasizing a cleaner MVI architecture form, better viewport scaling, and refined engineering design.

---

## 🌟 Key Features

| Feature / Aspect                          | YAMFF | YAMF |
|:------------------------------------------| :---: | :---: |
| **Android Compatibility**                 | **>= Android 12 (API 31)** | >= Android 12 (API 31) |
| **Architecture Pattern**                  | **Pure MVI Pattern** | Legacy State Driven |
| **Manager UI**                            | **Jetpack Compose + MD3** | Legacy Views / XML |
| **Proportional Scaling**                  | ✅ | ❌ |
| **Edge-Snapping Bubble**                  | ✅ | ❌ |
| **TextureView / SurfaceView Enhancement** | ✅  | ❌  |
| **Spring Animation Support**              | ✅ | ❌ |
| **Dual Window UI Styles**                 | ✅ (Classic & Gesture) | ❌ (Classic Only) |

---

## 🛠️ My Major Refactoring Changes

- **Architecture Overhaul**: Replaced outdated state handling with a clean **MVI (Model-View-Intent)** architecture, decoupling "god classes" and streamlining the window pipeline.
- **Extended Android 12L Support**: Kept compatibility with Android 12/12L (API 31+) while incorporating modern fixes and architectural overhauls.
- **Modern Manager Interface**: Rebuilt the management app using **Jetpack Compose** with Material Design 3 guidelines.
- **Viewport & Gesture Fixes**: Resolved `TextureView` resizing distortion, `SurfaceView` FitCenter scaling issues, and refined gesture bounds.
- **Dual UI Styles**:
    - **Classic**: Inherited and polished from `YAMF`.
    - **Gesture**: Adapted layout designs from `reYAMF`.

---

## 📋 Requirements

- Android 12+ (API level >= 31, tested only on Android 12L)
- LSPosed Framework

---

## 🙏 Credits & Special Thanks

Special thanks to the original creators and upstream projects:

- [duzhaokun123/YAMF](https://github.com/duzhaokun123/YAMF) - Original repository and base code.
- [JuanArton/reYAMF](https://github.com/JuanArton/reYAMF) - Layout design inspiration for Gesture UI.
- [sunshine0523/Mi-FreeForm](https://github.com/sunshine0523/Mi-FreeForm) - The original inspiration.
