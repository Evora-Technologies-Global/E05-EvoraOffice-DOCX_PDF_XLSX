plugins {
    alias(libs.plugins.evora.android.application)
    alias(libs.plugins.evora.android.compose)
    alias(libs.plugins.evora.android.hilt)
}

android {
    namespace = "com.evora.technologies.office"

    defaultConfig {
        applicationId = "com.evora.technologies.office"
        versionCode = 100
        versionName = "1.0.0"
    }

    buildTypes {
        debug {
            ndk {
                abiFilters += listOf("arm64-v8a", "x86_64")
            }
        }
        release {
            ndk {
                abiFilters += "arm64-v8a"
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    buildFeatures {
        buildConfig = true
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:mvi"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:data"))
    implementation(project(":engine:pdf"))
    implementation(project(":engine:office"))
    implementation(project(":engine:hwp"))
    implementation(project(":engine:archive"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:monetization"))
    implementation(project(":feature:library"))
    implementation(project(":feature:reader"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.windowsize)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}
