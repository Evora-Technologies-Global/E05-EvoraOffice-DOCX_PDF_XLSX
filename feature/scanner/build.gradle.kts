plugins {
    alias(libs.plugins.evora.android.feature)
}

android {
    namespace = "com.evora.technologies.office.feature.scanner"
}

dependencies {
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.document.scanner)
    implementation(libs.mlkit.barcode.scanning)
}
