pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        maven("https://maven.aliyun.com/repository/google")
        mavenCentral()
        gradlePluginPortal()
    }

    // plugin marker の解決に失敗する環境向けに、AGP の実体モジュールへ明示マッピング
    resolutionStrategy {
        eachPlugin {
            if (requested.id.id == "com.android.application" || requested.id.id == "com.android.library") {
                useModule("com.android.tools.build:gradle:${requested.version}")
            }
        }
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        maven("https://maven.aliyun.com/repository/google")
        mavenCentral()
    }
}

rootProject.name = "Komorebi"
include(":app")

val ffmpegDecoderDir = File(rootDir, "media/libraries/decoder_ffmpeg")
if (ffmpegDecoderDir.exists()) {
    include(":media-decoder-ffmpeg")
    project(":media-decoder-ffmpeg").projectDir = ffmpegDecoderDir
}
