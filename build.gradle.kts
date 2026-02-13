// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
// ここに同じIDを2回書かないようにします
    id("com.android.application") version "8.5.0" apply false
    id("com.android.library") version "8.5.0" apply false
    id("org.jetbrains.kotlin.android") version "2.1.0" apply false
    id("com.google.devtools.ksp") version "2.1.0-1.0.29" apply false
    id("com.google.dagger.hilt.android") version "2.54" apply false
    alias(libs.plugins.kotlin.compose) apply false
}
