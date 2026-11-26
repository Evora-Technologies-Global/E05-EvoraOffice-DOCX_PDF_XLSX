pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            url = uri("https://pkgs.dev.azure.com/MicrosoftDeviceSDK/DuoSDK-Public/_packaging/Duo-SDK-Feed/maven/v1")
            name = "Duo-SDK-Feed"
        }
        maven {
            url = uri("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea")
            name = "Mintegral-Maven"
        }
    }
}

rootProject.name = "EvoraOffice"

include(":app")
include(":core:model")
include(":core:domain")
include(":core:mvi")
include(":core:designsystem")
include(":core:database")
include(":core:datastore")
include(":core:data")
include(":engine:pdf")
include(":engine:office")
include(":engine:hwp")
include(":engine:archive")
include(":engine:scanner")
include(":engine:transfer")
include(":engine:cloud")
include(":engine:monetization")
include(":feature:onboarding")
include(":feature:monetization")
include(":feature:library")
include(":feature:reader")
include(":feature:editor")
include(":feature:tools")
include(":feature:scanner")
include(":feature:transfer")
include(":feature:cloud")
include(":feature:settings")
include(":baselineprofile")
