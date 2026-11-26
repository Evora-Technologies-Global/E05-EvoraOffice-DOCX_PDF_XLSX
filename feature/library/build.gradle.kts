plugins {
    alias(libs.plugins.evora.android.feature)
}

android {
    namespace = "com.evora.technologies.office.feature.library"
}

dependencies {
    implementation(libs.androidx.glance.appwidget)
    implementation(libs.androidx.glance.material3)
}
