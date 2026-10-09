plugins {
    id("com.android.application")
}

android {
    lint { baseline = file("lint-baseline.xml") }
    buildFeatures { buildConfig = true }
    namespace = "com.linkcore.emulator"
    compileSdk = 36
    ndkVersion = "28.2.13676358"

    defaultConfig {
        applicationId = "com.brumastudio.bruma"
        minSdk = 26
        targetSdk = 36
        versionCode = 41
        versionName = "1.0.1"
        testInstrumentationRunner = "com.linkcore.emulator.CoreSmokeTest"

        ndk {
            abiFilters += listOf("arm64-v8a")
        }

        externalNativeBuild {
            cmake {
                cppFlags += "-std=c++20"
            }
        }
    }

    externalNativeBuild {
        cmake {
            path = file("src/main/cpp/CMakeLists.txt")
            version = "4.1.2"
        }
    }

    buildTypes { getByName("debug") { applicationIdSuffix = ".rpgfix" } }

    packaging {
        jniLibs { useLegacyPackaging = true }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    implementation("com.intuit.sdp:sdp-android:1.1.0")
}








