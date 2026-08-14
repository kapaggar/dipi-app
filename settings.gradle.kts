pluginManagement {
    repositories {
        google()
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

rootProject.name = "DipiStaff"
include(
    ":app",
    ":core:model",
    ":core:network",
    ":core:database",
    ":core:datastore",
    ":core:ui",
    ":core:audit",
    ":feature:auth",
    ":feature:course",
    ":feature:applicants",
    ":feature:photos",
    ":feature:summary",
    ":feature:settings",
)
