plugins {
    alias(libs.plugins.evora.android.library)
    alias(libs.plugins.evora.android.hilt)
}

android {
    namespace = "com.evora.technologies.office.engine.pdf"
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.pdfium.android)
    implementation(libs.pdfbox.android) {
        exclude(group = "org.bouncycastle")
    }
    implementation(libs.bouncycastle.prov)
    implementation(libs.bouncycastle.pkix)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
