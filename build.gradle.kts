plugins {
    base
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.kover) apply false
    alias(libs.plugins.detekt) apply false
}

buildscript {
    configurations.classpath {
        resolutionStrategy.activateDependencyLocking()
    }
    dependencies {
        constraints {
            // Kover's reporting dependency: retain Flow's reviewed advisory floor.
            classpath("org.freemarker:freemarker:2.3.35")
        }
    }
}

allprojects {
    group = "app.astrid"
    version = "0.1.0"
    buildscript {
        configurations.classpath {
            resolutionStrategy.activateDependencyLocking()
        }
    }
    dependencyLocking {
        lockAllConfigurations()
        lockMode = org.gradle.api.artifacts.dsl.LockMode.STRICT
    }
}

tasks.named("check") {
    dependsOn(":engine:check", ":server:check")
}

tasks.named("build") {
    dependsOn(":engine:build", ":server:build")
}
