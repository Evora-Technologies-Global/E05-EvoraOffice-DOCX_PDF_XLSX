plugins {
    alias(libs.plugins.evora.android.library)
    alias(libs.plugins.evora.android.compose)
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

configurations.configureEach {
    // Strictly zero Pangle or lockscreen ad SDKs (06_SPRINT_BACKLOG.md T-024)
    exclude(group = "com.bytedance.sdk")
    exclude(group = "com.pangle.global")
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:datastore"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.billing.ktx)
    implementation(libs.play.services.ads)
    implementation(libs.google.ump)
    implementation(libs.mediation.applovin)
    implementation(libs.mediation.unity)
    implementation(libs.unity.ads)
    implementation(libs.mediation.mintegral)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.config)
    implementation(libs.adjust.android)
    implementation(libs.install.referrer)
    implementation(libs.play.services.ads.identifier)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.androidx.datastore.preferences)
    testImplementation(libs.turbine)
}
