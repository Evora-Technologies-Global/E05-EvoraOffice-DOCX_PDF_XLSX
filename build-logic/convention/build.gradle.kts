import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `kotlin-dsl`
}

group = "com.evora.technologies.office.buildlogic"

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_17
    }
}

dependencies {
    compileOnly(libs.android.gradlePlugin)
    compileOnly(libs.kotlin.gradlePlugin)
    compileOnly(libs.compose.gradlePlugin)
    compileOnly(libs.ksp.gradlePlugin)
    compileOnly(libs.hilt.gradlePlugin)
    compileOnly(libs.room.gradlePlugin)
}

gradlePlugin {
    plugins {
        register("kotlinJvm") {
            id = "evora.kotlin.jvm"
            implementationClass = "KotlinJvmConventionPlugin"
        }
        register("androidLibrary") {
            id = "evora.android.library"
            implementationClass = "AndroidLibraryConventionPlugin"
        }
        register("androidApplication") {
            id = "evora.android.application"
            implementationClass = "AndroidApplicationConventionPlugin"
        }
        register("androidCompose") {
            id = "evora.android.compose"
            implementationClass = "AndroidComposeConventionPlugin"
        }
        register("androidHilt") {
            id = "evora.android.hilt"
            implementationClass = "AndroidHiltConventionPlugin"
        }
        register("androidFeature") {
            id = "evora.android.feature"
            implementationClass = "AndroidFeatureConventionPlugin"
        }
    }
}
