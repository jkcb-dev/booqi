import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
}

kotlin {
    iosArm64()
    iosSimulatorArm64()

    android {
        namespace = "corp.khin.solutions.booqi.core.database"
        compileSdk = libs.versions.android.compileSdk.get().toInt()
        minSdk = libs.versions.android.minSdk.get().toInt()

        compilerOptions {
            jvmTarget = JvmTarget.JVM_11
        }
        withHostTest {
            isIncludeAndroidResources = false
        }
    }

    sourceSets {
        commonMain.dependencies {
            api(libs.sqldelight.runtime)
            implementation(libs.sqldelight.coroutines)
        }
        androidMain.dependencies {
            implementation(libs.sqldelight.android.driver)
        }
        // The default-hierarchy-template `iosMain` intermediate source set isn't registered
        // early enough in this plugin's evaluation order for eager `by getting`/`getByName`
        // lookups here — `matching {}.configureEach {}` applies the dependency as soon as it
        // actually exists, regardless of ordering.
        matching { it.name == "iosMain" }.configureEach {
            dependencies {
                implementation(libs.sqldelight.native.driver)
            }
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
