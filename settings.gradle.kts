pluginManagement {
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

rootProject.name = "Komorebi"
include(":app")

val ffmpegDecoderDir = File(rootDir, "/Users/hayato/Documents/decoder_ffmpeg")
if (ffmpegDecoderDir.exists()) {
    include(":media-decoder-ffmpeg")
    project(":media-decoder-ffmpeg").projectDir = ffmpegDecoderDir
}
