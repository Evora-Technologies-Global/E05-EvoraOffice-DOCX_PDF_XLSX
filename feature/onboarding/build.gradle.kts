plugins {
    alias(libs.plugins.evora.android.feature)
}

android {
    namespace = "com.evora.technologies.office.feature.onboarding"
}

dependencies {
    implementation(libs.androidx.appcompat)
}
