import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

val localProps = Properties().apply {
    val f = rootProject.file("local.properties")
    if (f.exists()) f.inputStream().use { load(it) }
}
val overrideUrl = (findProperty("dipi.baseUrl") as String?)
    ?: localProps.getProperty("dipi.baseUrl")
    ?: "https://dipi.vridhamma.org"

android {
    namespace = "org.dhamma.dipi.staff"
    compileSdk = 35
    defaultConfig {
        applicationId = "org.dhamma.dipi.staff"
        minSdk = 26
        targetSdk = 35
        versionCode = 86
        versionName = "1.43.5"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BASE_URL", "\"${overrideUrl.trimEnd('/')}\"")
        val useMock = ((findProperty("dipi.useMock") as String?)
            ?: localProps.getProperty("dipi.useMock")
            ?: "false").equals("true", ignoreCase = true)
        buildConfigField("boolean", "USE_MOCK", if (useMock) "true" else "false")
    }
    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            // Desk installs go over Wi-Fi ADB; debug signing lets the release
            // replace a debug install in place. Swap in a real keystore before
            // any store distribution.
            signingConfig = signingConfigs.getByName("debug")
            // The desk tablet (Pixel C) and every device since 2015 is arm64.
            ndk { abiFilters += "arm64-v8a" }
            buildConfigField("boolean", "USE_MOCK", "false")
            buildConfigField("String", "BASE_URL", "\"https://dipi.vridhamma.org\"")
        }
        debug {
            // Live Drupal by default. Re-enable fixtures with -Pdipi.useMock=true
        }
    }
    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
            isReturnDefaultValues = true
        }
    }
}

dependencies {
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs_nio:2.1.5")
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:ui"))
    implementation(project(":core:audit"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:course"))
    implementation(project(":feature:desk"))
    implementation(project(":feature:teacher"))
    implementation(project(":feature:applicants"))
    implementation(project(":feature:photos"))
    implementation(project(":feature:summary"))
    implementation(project(":feature:settings"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.hilt.android)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.kotlinx.serialization.json)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.retrofit)
    implementation(libs.okhttp)
    ksp(libs.hilt.compiler)
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    // A real DeskViewModel drives the repo in mock mode over MockWebServer +
    // DipiMockDispatcher, so tests need the server type and the JSON converter
    // that the production NetworkModule uses.
    testImplementation(libs.okhttp.mockwebserver)
    testImplementation(libs.retrofit.kotlinx)
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}
