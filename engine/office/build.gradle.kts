plugins {
    alias(libs.plugins.evora.android.library)
    alias(libs.plugins.evora.android.hilt)
}

android {
    namespace = "com.evora.technologies.office.engine.office"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
}
