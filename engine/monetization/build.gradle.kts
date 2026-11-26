plugins {
    alias(libs.plugins.evora.android.library)
    alias(libs.plugins.evora.android.hilt)
}

android {
    namespace = "com.evora.technologies.office.engine.monetization"
    buildFeatures {
        buildConfig = true
    }
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:datastore"))
    implementation(project(":core:data"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.billing.ktx)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.datastore.preferences)
    testImplementation(libs.turbine)
}
