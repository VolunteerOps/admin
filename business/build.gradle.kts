plugins { alias(libs.plugins.kotlinMultiplatform) }
kotlin {
    jvm()
    js { browser(); binaries.library(); useEsModules(); generateTypeScriptDefinitions() }
    sourceSets { commonTest.dependencies { implementation(kotlin("test")) } }
}
