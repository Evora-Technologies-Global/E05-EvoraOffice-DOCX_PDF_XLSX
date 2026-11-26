plugins {
    alias(libs.plugins.evora.android.feature)
}

android {
    namespace = "com.evora.technologies.office.feature.cloud"
}

dependencies {
    implementation(libs.play.services.auth)
    implementation(libs.microsoft.msal)
}
