import org.jetbrains.kotlin.gradle.ExperimentalWasmDsl

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    js { browser(); nodejs() }
    @OptIn(ExperimentalWasmDsl::class)
    wasmJs { browser() }
    sourceSets {
        commonMain { kotlin.srcDir("../business/src/commonMain/kotlin") }
        commonMain.dependencies {
            implementation("io.ktor:ktor-client-core:3.6.0")
            implementation("io.ktor:ktor-client-engine-defaults:3.6.0")
            implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.11.0")
            implementation(libs.compose.runtime)
            implementation(libs.compose.foundation)
            implementation(libs.compose.material3)
            implementation(libs.compose.ui)
            implementation(libs.compose.components.resources)
            implementation(libs.compose.uiToolingPreview)
            implementation(libs.androidx.lifecycle.viewmodelCompose)
            implementation(libs.androidx.lifecycle.runtimeCompose)
        }
        commonTest.dependencies { implementation(libs.kotlin.test); implementation("io.ktor:ktor-client-mock:3.6.0"); implementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.11.0") }
        jsMain.dependencies {
            implementation(npm("firebase", "12.19.0"))
            implementation(libs.wrappers.browser) }
    }
}
