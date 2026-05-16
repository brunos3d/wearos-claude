plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.kotlinAndroid)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
}

android {
    namespace = "com.brunos3d.wearosclaude"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.brunos3d.wearosclaude"
        minSdk = 30           // Wear OS 3 / Galaxy Watch 4+
        targetSdk = 34
        versionCode = 1
        versionName = "0.1.0"

        // Override at build time:
        //   -Pwearosclaude.backendUrl="http://192.168.1.7:47823"
        //   -Pwearosclaude.authToken="..."
        // The Settings screen on the watch lets the user override at runtime.
        val defaultBackend = (project.findProperty("wearosclaude.backendUrl") as String?)
            ?: "http://10.0.2.2:47823"
        val defaultAuthToken = (project.findProperty("wearosclaude.authToken") as String?) ?: ""
        buildConfigField("String", "DEFAULT_BACKEND_URL", "\"$defaultBackend\"")
        buildConfigField("String", "DEFAULT_AUTH_TOKEN", "\"$defaultAuthToken\"")
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            applicationIdSuffix = ".debug"
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            signingConfig = signingConfigs.getByName("debug") // adjust for production
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf("-opt-in=kotlin.RequiresOptIn")
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources.excludes += setOf(
            "/META-INF/{AL2.0,LGPL2.1}",
            "META-INF/DEPENDENCIES",
        )
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)

    val composeBom = platform(libs.androidx.compose.bom)
    implementation(composeBom)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui.tooling.preview)
    debugImplementation(libs.androidx.compose.ui.tooling)

    implementation(libs.wear.compose.foundation)
    implementation(libs.wear.compose.material)
    implementation(libs.wear.compose.navigation)
    implementation(libs.wear.input)

    implementation(libs.wear.tiles)
    implementation(libs.wear.tiles.renderer)
    implementation(libs.wear.protolayout)
    implementation(libs.wear.protolayout.material)
    implementation(libs.wear.protolayout.expression)
    debugImplementation(libs.wear.tiles.tooling)
    implementation(libs.wear.tiles.tooling.preview)

    implementation(libs.watchface.complications.datasource)
    implementation(libs.watchface.complications.datasource.ktx)
    implementation(libs.horologist.tiles)

    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.serialization.json)

    implementation(libs.ktor.client.core)
    implementation(libs.ktor.client.okhttp)
    implementation(libs.ktor.client.content.negotiation)
    implementation(libs.ktor.serialization.kotlinx.json)

    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.work.runtime.ktx)
}
