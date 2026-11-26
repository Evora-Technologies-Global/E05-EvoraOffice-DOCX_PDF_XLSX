plugins {
    alias(libs.plugins.evora.android.feature)
}

android {
    namespace = "com.evora.technologies.office.feature.settings"
}

dependencies {
    implementation(libs.androidx.appcompat)
}
