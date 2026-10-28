import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
    }
}
dependencies {
    implementation(project(":sharedLogic"))
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.androidx.compose.ui)
    implementation(libs.compose.uiToolingPreview)
    debugImplementation(libs.compose.uiTooling)
}

android {
    namespace = "com.pawan.kmpnativeuitrading"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.pawan.kmpnativeuitrading"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        versionCode = providers.gradleProperty("releaseVersionCode").getOrElse("1").toInt().also { require(it > 0) }
        versionName = providers.gradleProperty("releaseVersionName").getOrElse("1.0.0")
        val origin = providers.gradleProperty("apiOrigin").getOrElse("")
        require(!origin.contains('"') && !origin.contains('\\') && !origin.contains('\n'))
        buildConfigField("String", "API_ORIGIN", "\"$origin\"")
    }
    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
    val signingPath = providers.environmentVariable("NORTHSTAR_KEYSTORE_PATH").orNull
    if (signingPath != null) {
        signingConfigs.create("upload") {
            storeFile = file(signingPath)
            storePassword = providers.environmentVariable("NORTHSTAR_KEYSTORE_PASSWORD").get()
            keyAlias = providers.environmentVariable("NORTHSTAR_KEY_ALIAS").get()
            keyPassword = providers.environmentVariable("NORTHSTAR_KEY_PASSWORD").get()
        }
    }
    buildTypes {
        release {
            if (signingPath != null) signingConfig = signingConfigs.getByName("upload")
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}
// An unsigned artifact is useful for CI, but a release API may never use HTTP.
tasks.matching { it.name == "preReleaseBuild" }.configureEach {
    val origin = providers.gradleProperty("apiOrigin").getOrElse("")
    doFirst {
        require(origin.isBlank() || origin.startsWith("https://")) { "Release API origin must use HTTPS." }
    }
}
