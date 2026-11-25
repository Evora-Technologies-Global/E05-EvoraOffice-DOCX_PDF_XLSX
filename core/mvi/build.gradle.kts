plugins {
    alias(libs.plugins.evora.android.library)
    alias(libs.plugins.evora.android.compose)
}

android {
    namespace = "com.evora.technologies.office.core.mvi"
}

dependencies {
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.kotlinx.coroutines.android)
    testImplementation(libs.turbine)
}
