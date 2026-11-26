plugins {
    alias(libs.plugins.evora.android.feature)
}

android {
    namespace = "com.evora.technologies.office.feature.transfer"
}

dependencies {
    implementation(libs.zxing.core)
}
