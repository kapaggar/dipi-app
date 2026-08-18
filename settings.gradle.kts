pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://maven.pkg.jetbrains.space/public/p/compose/dev")
    }
}

rootProject.name = "DipiStaff"

val desktopOnly = providers.gradleProperty("dipi.desktopOnly").orNull.equals("true", ignoreCase = true) ||
    System.getenv("DIPI_DESKTOP_ONLY").equals("true", ignoreCase = true)

if (desktopOnly) {
    include(
        ":core:model",
        ":core:protocol",
        ":core:audit",
        ":desktop",
    )
} else {
    include(
        ":app",
        ":core:model",
        ":core:protocol",
        ":core:network",
        ":core:database",
        ":core:datastore",
        ":core:ui",
        ":core:audit",
        ":feature:auth",
        ":feature:course",
        ":feature:desk",
        ":feature:applicants",
        ":feature:photos",
        ":feature:summary",
        ":feature:settings",
        ":desktop",
    )
}
