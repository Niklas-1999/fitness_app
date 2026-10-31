pluginManagement {
    repositories { google(); mavenCentral(); gradlePluginPortal() }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google(); mavenCentral()
        // Tesseract4Android (offline OCR, Apache-2.0) is only published on JitPack
        maven { url = uri("https://jitpack.io"); content { includeGroup("cz.adaptech.tesseract4android") } }
    }
}
rootProject.name = "ironcore"
include(":app")
