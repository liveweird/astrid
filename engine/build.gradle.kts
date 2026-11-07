plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kover)
    alias(libs.plugins.detekt)
}

kotlin {
    jvmToolchain(21)
}

dependencies {
    testImplementation(kotlin("test"))
}

val pilotReviewOutput = layout.buildDirectory.file("review/pilot-1.md")

tasks.register<JavaExec>("exportPilotReview") {
    group = "verification"
    description = "Writes the deterministic pilot content review to engine/build/review/pilot-1.md"
    dependsOn(tasks.testClasses)
    classpath = sourceSets.test.get().runtimeClasspath
    mainClass.set("app.astrid.engine.PilotReviewExportMain")
    argumentProviders.add(
        org.gradle.process.CommandLineArgumentProvider { listOf(pilotReviewOutput.get().asFile.absolutePath) },
    )
    outputs.file(pilotReviewOutput)
}

tasks.test {
    useJUnit()
}

detekt {
    config.setFrom(rootProject.files("config/detekt/detekt.yml"))
    buildUponDefaultConfig = true
}

kover {
    reports {
        verify {
            rule {
                minBound(85)
            }
        }
    }
}
