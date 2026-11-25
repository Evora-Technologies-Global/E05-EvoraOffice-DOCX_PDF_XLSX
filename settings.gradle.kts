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
include(":feature:onboarding")
include(":feature:monetization")
include(":feature:library")
include(":feature:reader")
