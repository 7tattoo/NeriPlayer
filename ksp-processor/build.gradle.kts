plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    implementation(project(":ksp-annotations"))
    implementation(libs.ksp.symbol.processing.api)
}
