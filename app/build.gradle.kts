import com.android.build.gradle.internal.cxx.configure.gradleLocalProperties

val localProperties = gradleLocalProperties(rootDir)
val gitHash = providers.exec {
    commandLine("git", "rev-parse", "--short", "HEAD")
}.standardOutput.asText.getOrElse("(error)").trim()

val isDirty = providers.exec {
    commandLine("git", "status", "--porcelain")
}.standardOutput.asText.getOrElse("").isNotEmpty()

plugins {
    id("com.android.application")
    id("kotlin-android")
    id("dev.rikka.tools.refine") version "4.3.0"
}

android {
    val buildTime = System.currentTimeMillis()
    val baseVersionName = "0.7"

    compileSdk = 34

    defaultConfig {
        applicationId = "io.github.sterlingshell.yamff"
        minSdk = 31
        targetSdk = 34
        versionCode = 7
        versionName = "$baseVersionName-git.$gitHash${if (isDirty) "-dirty" else ""}"

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
            sourceSets.getByName("main").java.srcDir(File("build/generated/ksp/release/kotlin"))
        }
        getByName("debug") {
            val minifyEnabled = localProperties.getProperty("minify.enabled", "false")
            isMinifyEnabled = minifyEnabled.toBoolean()
            isShrinkResources = minifyEnabled.toBoolean()
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = if (System.getenv("REL_KEY") != null) {
                signingConfigs.getByName("release")
            } else {
                signingConfigs.getByName("debug")
            }
//            sourceSets.getByName("main").java.srcDir(File("build/generated/ksp/debug/kotlin"))
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
        languageVersion = "2.0"
    }
    buildFeatures {
        viewBinding = true
        aidl = true
        compose = true
    }
    composeOptions {
        kotlinCompilerExtensionVersion = "1.5.1"
    }
    androidResources {
        additionalParameters += listOf("--allow-reserved-package-id", "--package-id", "0x64")
    }
    lint {
        abortOnError = false
    }
    namespace = "io.github.sterlingshell.yamff"
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    implementation("androidx.fragment:fragment-ktx:1.6.2")
    implementation("androidx.appcompat:appcompat:1.6.1")
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.preference:preference-ktx:1.2.1")

    //kotlinx-coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-jdk8:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    compileOnly(project(":android-stub"))
    compileOnly("dev.rikka.hidden:stub:4.2.0")
    implementation("dev.rikka.hidden:compat:4.2.0")

    //never upgrade until new extension function
    //noinspection GradleDependency
    implementation("com.github.kyuubiran:EzXHelper:1.0.3")
    compileOnly("de.robv.android.xposed:api:82")

    //lifecycle
    val lifecycleVersion = "2.6.2"
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:$lifecycleVersion")
    implementation("androidx.lifecycle:lifecycle-common-java8:$lifecycleVersion")

    //ViewBindingUtil
    implementation("com.github.matsudamper:ViewBindingUtil:0.1")

    //FlexboxLayout
    implementation("com.google.android.flexbox:flexbox:3.0.0")

    // Compose
    val composeBom = platform("androidx.compose:compose-bom:2024.06.00")
    implementation(composeBom)
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material3:material3-adaptive-navigation-suite:1.3.0-beta04")
    implementation("androidx.compose.material:material-icons-core")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.6.2")
    implementation("androidx.navigation:navigation-compose:2.7.7")

    //dynamicanimation
    implementation("androidx.dynamicanimation:dynamicanimation-ktx:1.0.0-alpha03")

    //gson
    implementation("com.google.code.gson:gson:2.10.1")
}