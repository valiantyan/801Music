plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.spotless)
}

android {
    namespace = "com.valiantyan.music801.player.impl"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 24
    }

    testOptions {
        unitTests.isIncludeAndroidResources = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }
}

spotless {
    kotlin {
        target("**/*.kt")
        ktlint("0.50.0")
            .editorConfigOverride(
                mapOf(
                    "ktlint_standard_no-unit-return" to "disabled",
                ),
            )
    }
    kotlinGradle {
        target("**/*.kts")
        ktlint("0.50.0")
    }
}

dependencies {
    implementation(project(":player-api"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.media3.exoplayer)
    implementation(libs.androidx.media3.session)
    implementation(libs.androidx.media3.ui)
    implementation(libs.androidx.media3.common)
    implementation(libs.androidx.startup.runtime)
    testImplementation(libs.junit)
    testImplementation(libs.robolectric)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.kotlin)
}
