plugins {
    alias(libs.plugins.evora.android.library)
    alias(libs.plugins.evora.android.hilt)
}

android {
    namespace = "com.evora.technologies.office.engine.cloud"
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

configurations.configureEach {
    exclude(group = "org.apache.httpcomponents")
    exclude(group = "com.google.http-client", module = "google-http-client-apache-v2")
    exclude(group = "commons-logging", module = "commons-logging")
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:database"))
    implementation(project(":core:data"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.okhttp)
    implementation(libs.play.services.auth)
    implementation(libs.google.api.client.android)
    implementation(libs.google.api.services.drive)
    implementation(libs.microsoft.msal)
    implementation(libs.microsoft.graph)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.room.runtime)
    testImplementation(libs.androidx.room.ktx)
}
