import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidMultiplatformLibrary)
    alias(libs.plugins.kotlinSerialization)
}

kotlin {
    listOf(
        iosArm64(),
        iosSimulatorArm64()
    ).forEach { iosTarget ->
        iosTarget.binaries.framework {
            baseName = "SharedLogic"
            isStatic = true
        }
    }
    
    android {
       namespace = "com.pawan.kmpnativeuitrading.sharedLogic"
       compileSdk = libs.versions.android.compileSdk.get().toInt()
       minSdk = libs.versions.android.minSdk.get().toInt()
    
       compilerOptions {
           jvmTarget = JvmTarget.JVM_11
       }
       androidResources {
           enable = true
       }
       withHostTest {
           isIncludeAndroidResources = true
       }
    }
    
    sourceSets {
        commonMain.dependencies {
            implementation(libs.kotlinx.serialization.json)
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.10.2")
            implementation("io.ktor:ktor-client-core:3.3.1")
            implementation("io.ktor:ktor-client-content-negotiation:3.3.1")
            implementation("io.ktor:ktor-serialization-kotlinx-json:3.3.1")
        }
        androidMain.dependencies {
            implementation("io.ktor:ktor-client-okhttp:3.3.1")
        }
        iosMain.dependencies {
            implementation("io.ktor:ktor-client-darwin:3.3.1")
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation("io.ktor:ktor-client-mock:3.3.1")
            implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.10.2")
        }
    }
}
// A cached unit-only run must not hide a newly enabled real HTTP contract check.
tasks.withType<org.gradle.api.tasks.testing.Test>().configureEach {
    val origin = providers.environmentVariable("NORTHSTAR_TEST_API").getOrElse("")
    inputs.property("contractApiOrigin", origin)
    environment("NORTHSTAR_TEST_API", origin)
}
