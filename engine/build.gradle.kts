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

val localeCompatibilityTest = tasks.register<Test>("localeCompatibilityTest") {
    group = "verification"
    description = "Runs engine tests in a fresh JVM using the Arabic (Egypt) locale"
    dependsOn(tasks.testClasses)
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnit()
    systemProperty("user.language", "ar")
    systemProperty("user.country", "EG")
    shouldRunAfter(tasks.test)
}

tasks.check {
    dependsOn(localeCompatibilityTest)
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
