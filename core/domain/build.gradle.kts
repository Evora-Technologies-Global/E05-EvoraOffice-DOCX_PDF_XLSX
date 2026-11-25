plugins {
    alias(libs.plugins.evora.kotlin.jvm)
}

dependencies {
    implementation(project(":core:model"))
    implementation(libs.kotlinx.coroutines.core)
    implementation(libs.javax.inject)
}
