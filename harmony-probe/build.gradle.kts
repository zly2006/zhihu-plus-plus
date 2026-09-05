plugins {
    alias(libs.plugins.kotlinMultiplatform) apply false
    alias(libs.plugins.composeMultiplatform) apply false
    alias(libs.plugins.composeCompiler) apply false
    alias(libs.plugins.kotlinSerialization) apply false
}

// CPF runtime has no desktop JVM variant. Host-only parser tests use the matching
// upstream runtime; OHOS configurations continue to resolve the CPF artifacts.
subprojects {
    configurations.matching { it.name.startsWith("jvm") }.configureEach {
        resolutionStrategy.dependencySubstitution {
            substitute(module("org.jetbrains.compose.runtime:runtime:1.9.2-1.0.0"))
                .using(module("org.jetbrains.compose.runtime:runtime:1.9.2"))
        }
    }
}
