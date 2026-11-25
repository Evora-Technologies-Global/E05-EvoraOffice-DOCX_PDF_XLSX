plugins {
    alias(libs.plugins.evora.android.library)
    alias(libs.plugins.evora.android.hilt)
}

android {
    namespace = "com.evora.technologies.office.engine.scanner"
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:data"))
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)
    implementation(libs.mlkit.document.scanner)
    implementation(libs.mlkit.text.recognition)
    implementation(libs.mlkit.text.recognition.chinese)
    implementation(libs.mlkit.text.recognition.devanagari)
    implementation(libs.mlkit.text.recognition.japanese)
    implementation(libs.mlkit.text.recognition.korean)
    implementation(libs.mlkit.barcode.scanning)
}
