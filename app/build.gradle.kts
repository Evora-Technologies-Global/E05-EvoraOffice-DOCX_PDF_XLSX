import java.util.Properties

plugins {
    alias(libs.plugins.evora.android.application)
    alias(libs.plugins.evora.android.compose)
    alias(libs.plugins.evora.android.hilt)
    alias(libs.plugins.androidx.baselineprofile)
}

if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}

fun Project.propOrDefault(name: String, default: String): String =
    (findProperty(name) as? String)?.takeIf { it.isNotBlank() } ?: default

val keystoreProps = Properties().apply {
    val propsFile = rootProject.file("keystore.properties")
    if (propsFile.exists()) {
        propsFile.inputStream().use(::load)
    }
}

fun Project.signingProp(propKey: String, envKey: String): String? =
    keystoreProps.getProperty(propKey)?.takeIf { it.isNotBlank() }
        ?: (findProperty(envKey) as? String)?.takeIf { it.isNotBlank() }
        ?: System.getenv(envKey)?.takeIf { it.isNotBlank() }

android {
    namespace = "com.evora.technologies.office"

    signingConfigs {
        val storePath = project.signingProp("storeFile", "EVORA_RELEASE_STORE_FILE")
        val storePass = project.signingProp("storePassword", "EVORA_RELEASE_STORE_PASSWORD")
        val alias = project.signingProp("keyAlias", "EVORA_RELEASE_KEY_ALIAS")
        val keyPass = project.signingProp("keyPassword", "EVORA_RELEASE_KEY_PASSWORD")
        if (storePath != null && storePass != null && alias != null && keyPass != null) {
            val resolvedFile = rootProject.file(storePath)
            if (resolvedFile.exists()) {
                create("release") {
                    storeFile = resolvedFile
                    storePassword = storePass
                    keyAlias = alias
                    keyPassword = keyPass
                    enableV1Signing = true
                    enableV2Signing = true
                    enableV3Signing = true
                    enableV4Signing = true
                }
            }
        }
    }

    defaultConfig {
        applicationId = "com.evora.technologies.office"
        versionCode = 100
        versionName = "1.0.0"

        val admobAppId = propOrDefault("EVORA_ADMOB_APP_ID", "ca-app-pub-3940256099942544~3347511713")
        val interstitialId = propOrDefault("EVORA_ADMOB_INTERSTITIAL_ID", "ca-app-pub-3940256099942544/1033173712")
        val appOpenId = propOrDefault("EVORA_ADMOB_APP_OPEN_ID", "ca-app-pub-3940256099942544/9257395921")
        val rewardedId = propOrDefault("EVORA_ADMOB_REWARDED_ID", "ca-app-pub-3940256099942544/5224354917")
        val nativeId = propOrDefault("EVORA_ADMOB_NATIVE_ID", "ca-app-pub-3940256099942544/2247696110")
        val adjustToken = propOrDefault("EVORA_ADJUST_APP_TOKEN", "")

        manifestPlaceholders["admobAppId"] = admobAppId
        buildConfigField("String", "ADMOB_APP_ID", "\"$admobAppId\"")
        buildConfigField("String", "ADMOB_INTERSTITIAL_ID", "\"$interstitialId\"")
        buildConfigField("String", "ADMOB_APP_OPEN_ID", "\"$appOpenId\"")
        buildConfigField("String", "ADMOB_REWARDED_ID", "\"$rewardedId\"")
        buildConfigField("String", "ADMOB_NATIVE_ID", "\"$nativeId\"")
        buildConfigField("String", "ADJUST_APP_TOKEN", "\"$adjustToken\"")
    }

    buildTypes {
        debug {
            ndk {
                abiFilters += listOf("arm64-v8a", "x86_64")
            }
        }
        release {
            signingConfigs.findByName("release")?.let { signingConfig = it }
            ndk {
                abiFilters += "arm64-v8a"
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
        }
    }

    bundle {
        abi {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        language {
            enableSplit = true
        }
    }

    buildFeatures {
        buildConfig = true
    }

    packaging {
        resources {
            excludes += listOf(
                "META-INF/INDEX.LIST",
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE.md",
                "META-INF/LICENSE-notice.md",
                "META-INF/NOTICE.md",
                "META-INF/io.netty.versions.properties",
            )
        }
    }

    androidResources {
        noCompress += listOf("rdb", "xcd", "res", "data", "ttf", "rc", "ini", "ui", "zip")
    }
}

configurations.configureEach {
    exclude(group = "org.bouncycastle", module = "bcprov-jdk15to18")
    exclude(group = "org.bouncycastle", module = "bcpkix-jdk15to18")
    exclude(group = "org.bouncycastle", module = "bcutil-jdk15to18")
    exclude(group = "org.apache.httpcomponents")
    exclude(group = "com.google.http-client", module = "google-http-client-apache-v2")
    // Strictly zero Pangle or lockscreen ad SDKs (06_SPRINT_BACKLOG.md T-024)
    exclude(group = "com.bytedance.sdk")
    exclude(group = "com.pangle.global")
}

dependencies {
    implementation(project(":core:model"))
    implementation(project(":core:domain"))
    implementation(project(":core:mvi"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:database"))
    implementation(project(":core:datastore"))
    implementation(project(":core:data"))
    implementation(project(":engine:pdf"))
    implementation(project(":engine:office"))
    implementation(project(":engine:hwp"))
    implementation(project(":engine:archive"))
    implementation(project(":engine:scanner"))
    implementation(project(":engine:transfer"))
    implementation(project(":engine:cloud"))
    implementation(project(":engine:monetization"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:monetization"))
    implementation(project(":feature:library"))
    implementation(project(":feature:reader"))
    implementation(project(":feature:editor"))
    implementation(project(":feature:tools"))
    implementation(project(":feature:scanner"))
    implementation(project(":feature:transfer"))
    implementation(project(":feature:cloud"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material3.windowsize)
    implementation(libs.androidx.hilt.navigation.compose)
    implementation(libs.androidx.work.runtime.ktx)
    implementation(libs.androidx.hilt.work)
    implementation(libs.androidx.profileinstaller)
    implementation(libs.microsoft.msal)
    baselineProfile(project(":baselineprofile"))
    ksp(libs.androidx.hilt.compiler)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
    testImplementation(libs.turbine)
}

baselineProfile {
    automaticGenerationDuringBuild = false
    saveInSrc = true
}
