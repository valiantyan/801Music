plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.parcelize)
    alias(libs.plugins.spotless)
}

android {
    namespace = "com.valiantyan.music801.player.api"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 24
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
    implementation(libs.kotlinx.coroutines.core)
}
