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
rootProject.name = "DownloadManager"
include(":app")
include(":common")
include(":domain")
include(":data")
include(":engine")
include(":service")
include(":ui:home")
include(":ui:category")
include(":ui:settings")