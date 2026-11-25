plugins {
    alias(libs.plugins.evora.android.library)
    alias(libs.plugins.evora.android.hilt)
}

android {
    namespace = "com.evora.technologies.office.engine.archive"
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.commons.compress)
    implementation(libs.tukaani.xz)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
