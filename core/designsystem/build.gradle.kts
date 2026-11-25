plugins {
    alias(libs.plugins.evora.android.library)
    alias(libs.plugins.evora.android.compose)
}

android {
    namespace = "com.evora.technologies.office.core.designsystem"
}

dependencies {
    implementation(libs.androidx.compose.material3)
}
