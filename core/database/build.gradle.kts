plugins {
    alias(libs.plugins.evora.android.library)
    alias(libs.plugins.evora.android.hilt)
    alias(libs.plugins.room)
}

android {
    namespace = "com.evora.technologies.office.core.database"
    testOptions {
        unitTests.isIncludeAndroidResources = true
    }
}

room {
    schemaDirectory("$projectDir/schemas")
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    testImplementation(libs.robolectric)
    testImplementation(libs.androidx.test.core)
}
