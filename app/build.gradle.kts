import java.io.StringReader
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.room)
}

// Release signing credentials: keystore.properties at the repo root (git-ignored; keys
// storeFile, storePassword, keyAlias, keyPassword), else the TRIPCHECKER_STORE_FILE,
// TRIPCHECKER_STORE_PASSWORD, TRIPCHECKER_KEY_ALIAS and TRIPCHECKER_KEY_PASSWORD environment
// variables. If neither is complete, release stays unsigned. See docs/RELEASE_OPTIMIZATION.md.
val signingProps = Properties().apply {
    providers.fileContents(rootProject.layout.projectDirectory.file("keystore.properties"))
        .asText.orNull?.let { load(StringReader(it)) }
}

fun signingValue(propKey: String, envKey: String): String? =
    (signingProps.getProperty(propKey) ?: providers.environmentVariable(envKey).orNull)
        ?.trim()?.takeIf { it.isNotEmpty() }

val releaseStoreFile = signingValue("storeFile", "TRIPCHECKER_STORE_FILE")?.let { rootProject.file(it) }
val releaseStorePassword = signingValue("storePassword", "TRIPCHECKER_STORE_PASSWORD")
val releaseKeyAlias = signingValue("keyAlias", "TRIPCHECKER_KEY_ALIAS")
val releaseKeyPassword = signingValue("keyPassword", "TRIPCHECKER_KEY_PASSWORD")
val hasReleaseSigning = releaseStoreFile != null && releaseStoreFile.isFile &&
    releaseStorePassword != null && releaseKeyAlias != null && releaseKeyPassword != null
if (!hasReleaseSigning) {
    logger.warn("Release signing is not configured (keystore.properties or TRIPCHECKER_* env vars missing/incomplete); release builds will be unsigned.")
}

android {
    namespace = "com.example.tripchecker"
    compileSdk {
        version = release(37)
    }

    defaultConfig {
        applicationId = "com.example.tripchecker"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            if (hasReleaseSigning) signingConfig = signingConfigs.getByName("release")
            optimization {
                // R8 code + resource optimization (AGP 9.3+ DSL; includes the
                // proguard-android-optimize defaults). Keep rules: src/main/keepRules.
                enable = true
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
    }
    splits {
        abi {
            // ~90% of the native payload is ML Kit's bundled OCR pipeline, once per ABI.
            // Emit one APK per ABI (sideload the one matching the phone, usually arm64-v8a)
            // plus a universal APK that still runs everywhere.
            // App Bundles split by ABI on their own, and AGP can't build a bundle while APK
            // splits are on (issuetracker.google.com/402800800), so skip them for bundle tasks.
            isEnable = gradle.startParameter.taskNames.none { it.contains("bundle", ignoreCase = true) }
            reset()
            include("arm64-v8a", "armeabi-v7a", "x86_64", "x86")
            isUniversalApk = true
        }
    }
    packaging {
        resources {
            // BouncyCastle (via pdfbox-android) bundles ~4 MB of post-quantum parameter
            // tables (Picnic/SIKE). PdfBox never uses PQC; R8 strips the classes but not
            // these Java resources.
            excludes += "org/bouncycastle/pqc/**"
        }
    }
    lint {
        abortOnError = true
        warningsAsErrors = false
        lintConfig = file("lint.xml")
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.core)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.pdfbox.android)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.room.testing)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    debugImplementation(libs.androidx.compose.ui.tooling)
}
