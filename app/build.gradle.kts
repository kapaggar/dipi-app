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
        versionCode = 5
        versionName = "1.2.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "BASE_URL", "\"${overrideUrl.trimEnd('/')}\"")
        val useMock = ((findProperty("dipi.useMock") as String?)
            ?: localProps.getProperty("dipi.useMock")
            ?: "false").equals("true", ignoreCase = true)
        buildConfigField("boolean", "USE_MOCK", if (useMock) "true" else "false")
    }
    buildTypes {
        release {
            isMinifyEnabled = false
            buildConfigField("boolean", "USE_MOCK", "false")
            buildConfigField("String", "BASE_URL", "\"https://dipi.vridhamma.org\"")
        }
        debug {
            // Live Drupal by default. Re-enable fixtures with -Pdipi.useMock=true
        }
    }
    compileOptions {
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
    implementation(project(":core:model"))
    implementation(project(":core:network"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:ui"))
    implementation(project(":core:audit"))
    implementation(project(":feature:auth"))
    implementation(project(":feature:course"))
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
    implementation(libs.androidx.material3.window)
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
    testImplementation(libs.robolectric)
    testImplementation(platform(libs.androidx.compose.bom))
    testImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.test.manifest)
}
