// Top-level build file. Plugins are declared with `apply false` here and
// then applied per-module so each module can pick which ones it needs.
plugins {
    alias(libs.plugins.androidApplication) apply false
    alias(libs.plugins.kotlinAndroid) apply false
    alias(libs.plugins.composeCompiler) apply false
}
