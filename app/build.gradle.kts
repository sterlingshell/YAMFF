import java.util.Properties

val localProperties = Properties().apply {
    val file = rootProject.file("local.properties")
    if (file.exists()) {
        file.inputStream().use { load(it) }
    }
}

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("org.jetbrains.kotlin.plugin.compose")
    id("dev.rikka.tools.refine") version "4.4.0"
}

android {
    val buildTime = System.currentTimeMillis()
    val baseVersionName = project.properties["yamff_version"]?.toString() ?: "0.1.0"

    compileSdk = 36

    defaultConfig {
        applicationId = "io.github.sterlingshell.yamff"
        minSdk = 31
        targetSdk = 36
        versionCode = 7
        versionName = baseVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("long", "BUILD_TIME", buildTime.toString())
    }

    packaging {
        resources.excludes.addAll(
            arrayOf(
                "META-INF/**",
                "kotlin/**"
            )
        )
    }

    signingConfigs {
        create("release") {
            storeFile = file("../releaseKey.jks")
            storePassword = System.getenv("REL_KEY")
            keyAlias = "key0"
            keyPassword = System.getenv("REL_KEY")
            enableV1Signing = false
            enableV2Signing = false
            enableV3Signing = true
            enableV4Signing = true
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (System.getenv("REL_KEY") != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }

        getByName("debug") {
            val gitHash = providers.exec {
                commandLine("git", "rev-parse", "--short", "HEAD")
            }.standardOutput.asText.getOrElse("(error)").trim()

            val isDirty = providers.exec {
                commandLine("git", "status", "--porcelain")
            }.standardOutput.asText.getOrElse("").isNotEmpty()

            versionNameSuffix = ".git.$gitHash${if (isDirty) "-dirty" else ""}"

            val minifyEnabled = localProperties.getProperty("minify.enabled", "false").toBoolean()
            isMinifyEnabled = minifyEnabled
            isShrinkResources = minifyEnabled

            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (System.getenv("REL_KEY") != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
        aidl = true
        compose = true
        buildConfig = true
    }

    lint {
        abortOnError = false
    }

    namespace = "io.github.sterlingshell.yamff"
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_11)
    }
}

dependencies {
    // AndroidX & UI Essentials
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("androidx.recyclerview:recyclerview:1.4.0")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.preference:preference-ktx:1.2.1")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // YAMFF Ecosystem & Refine System
    implementation(project(":yamff-sdk"))
    compileOnly(project(":android-stub"))
    compileOnly("dev.rikka.hidden:stub:4.4.0")
    implementation("dev.rikka.hidden:compat:4.4.0")
    implementation("dev.rikka.tools.refine:runtime:4.4.0")

    // Xposed Development
    // noinspection GradleDependency
    implementation("com.github.kyuubiran:EzXHelper:1.0.3")
    compileOnly("de.robv.android.xposed:api:82")

    // Lifecycle components
    val lifecycleVersion = "2.6.2"
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-common-java8:$lifecycleVersion")

    // Utilities
    implementation("com.github.matsudamper:ViewBindingUtil:0.1")
    implementation("com.google.android.flexbox:flexbox:3.0.0")
    implementation("com.google.code.gson:gson:2.10.1")

    // Jetpack Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-window-size-class")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.3.0-beta04")
    implementation("androidx.compose.material3.adaptive:adaptive:1.0.0-beta04")
    implementation("androidx.compose.material3.adaptive:adaptive-layout:1.0.0-beta04")
    implementation("androidx.compose.material3.adaptive:adaptive-navigation:1.0.0-beta04")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // Dynamic Animation
    implementation("androidx.dynamicanimation:dynamicanimation-ktx:1.0.0-alpha03")

    // Koin Dependency Injection
    implementation("io.insert-koin:koin-core:4.2.2")
    implementation("io.insert-koin:koin-android:4.2.2")
    implementation("io.insert-koin:koin-compose:4.2.2")
    implementation("io.insert-koin:koin-compose-viewmodel:4.2.2")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.2.0")
}