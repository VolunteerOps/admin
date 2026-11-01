rootProject.name = "admin"

pluginManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google {
            mavenContent {
                includeGroupAndSubgroups("androidx")
                includeGroupAndSubgroups("com.android")
                includeGroupAndSubgroups("com.google")
            }
        }
        mavenCentral()
    }
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

include(":shared")
include(":business")
// Browser policy builds and shared JS tests need neither the Android SDK nor native toolchains.
if (providers.gradleProperty("webOnly").orNull == "true") {
    project(":shared").buildFileName = "build.web.gradle.kts"
} else {
    include(":androidApp")
    include(":desktopApp")
}
