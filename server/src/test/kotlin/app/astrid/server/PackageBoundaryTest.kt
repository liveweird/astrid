package app.astrid.server

import java.io.File
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The one-way package layering of `server/` (README "Structure and conventions", ported from Flow's `PackageBoundaryTest`).
 *
 * The foundation (`infra`, `plugins`, `authz`) references no feature. The features layer `session` < `exercises` <
 * `practice`: `session` references neither of the others, and `exercises` never references `practice` (the attempt write
 * reaches practice state only through `exercises/AttemptPracticeHook`, implemented in `practice/`). The composition root
 * (`app.astrid.server` itself: `Application.kt`, `Main.kt`, `Stores.kt`, `PostgresStores.kt`) wires everything and is
 * referenced by nothing, whatever it declares (any qualified `app.astrid.server.<name>` that is not a known
 * sub-package). The engine never references the server.
 *
 * A plain source scan (no Docker, no classpath tricks): any non-comment line of a file declaring the lower package that names
 * a forbidden package by its qualified name (an import, a wildcard import or an inline reference) is a violation. Kotlin has
 * no implicit cross-package visibility, so every real dependency shows up as such a name.
 */
class PackageBoundaryTest {
    private val repoRoot = File(System.getProperty("repo.root") ?: "..")

    private val mainSources: List<File> by lazy { sources("server/src/main/kotlin") }

    private val featurePackages = listOf("session", "exercises", "practice")

    private fun sources(path: String): List<File> {
        val root = File(repoRoot, path)
        assertTrue(root.isDirectory, "${root.absolutePath} not found")
        return root.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    private fun packageOf(text: String): String? =
        text.lineSequence().firstOrNull { it.startsWith("package ") }?.removePrefix("package ")?.trim()

    private fun isComment(line: String): Boolean = line.trimStart().let { it.startsWith("//") || it.startsWith("*") || it.startsWith("/*") }

    /** `line number: line` of every code line in [text] that matches [pattern]. */
    private fun matchingLines(text: String, pattern: Regex): List<String> = text.lineSequence().withIndex()
        .filter { (_, line) -> !isComment(line) && pattern.containsMatchIn(line) }
        .map { (index, line) -> "${index + 1}: ${line.trim()}" }
        .toList()

    /** Every code line in [text] that names one of the [forbidden] `app.astrid.server` sub-packages by qualified name. */
    private fun forbiddenReferences(text: String, forbidden: List<String>): List<String> =
        matchingLines(text, Regex("""\bapp\.astrid\.server\.(${forbidden.joinToString("|")})\b"""))

    private fun violations(lowerPackages: Set<String>, forbidden: List<String>): List<String> = mainSources.flatMap { file ->
        val text = file.readText()
        val declared = packageOf(text)
        val lower = lowerPackages.any { declared == it || declared?.startsWith("$it.") == true }
        if (!lower) emptyList() else forbiddenReferences(text, forbidden).map { "${file.name} ($declared) $it" }
    }

    private fun assertDeclared(packagePrefix: String) {
        // The scan must actually see the package, or an empty result proves nothing (a moved directory, a renamed package).
        assertTrue(
            mainSources.any { packageOf(it.readText())?.let { p -> p == packagePrefix || p.startsWith("$packagePrefix.") } == true },
            "no source declares $packagePrefix",
        )
    }

    @Test
    fun `infra, plugins and authz reference no feature package`() {
        val lower = setOf("app.astrid.server.infra", "app.astrid.server.plugins", "app.astrid.server.authz")
        lower.forEach(::assertDeclared)
        assertEquals(emptyList(), violations(lower, featurePackages))
    }

    @Test
    fun `infra and authz reference no plugin`() {
        // `plugins/` installs Ktor features on top of `infra` and `authz` (it maps `authz` exceptions to problems), never
        // the other way round.
        val lower = setOf("app.astrid.server.infra", "app.astrid.server.authz")
        lower.forEach(::assertDeclared)
        assertEquals(emptyList(), violations(lower, listOf("plugins")))
    }

    @Test
    fun `session references neither exercises nor practice`() {
        assertDeclared("app.astrid.server.session")
        assertEquals(emptyList(), violations(setOf("app.astrid.server.session"), listOf("exercises", "practice")))
    }

    @Test
    fun `exercises never references practice`() {
        assertDeclared("app.astrid.server.exercises")
        assertEquals(emptyList(), violations(setOf("app.astrid.server.exercises"), listOf("practice")))
    }

    /**
     * A reference to anything directly under `app.astrid.server` that is not one of the known sub-packages, i.e. a
     * composition-root declaration of any kind (`class`, `data class`, `internal fun`, `const val`, `typealias`, an
     * extension...), found by the qualified name rather than by guessing the declaration keyword.
     */
    private val rootReference = Regex("""\bapp\.astrid\.server\.(?!(?:infra|plugins|authz|session|exercises|practice)\b)[A-Za-z]""")

    @Test
    fun `nothing outside the composition root references it`() {
        val rootFiles = mainSources.filter { packageOf(it.readText()) == "app.astrid.server" }
        assertTrue(rootFiles.size >= 4, "the scan saw only ${rootFiles.size} composition-root sources")
        val violations = mainSources.flatMap { file ->
            val text = file.readText()
            if (packageOf(text) == "app.astrid.server") emptyList() else matchingLines(text, rootReference).map { "${file.name} $it" }
        }
        assertEquals(emptyList(), violations)
    }

    @Test
    fun `the root scan flags any declaration kind but not sub-packages or comments`() {
        val source = """
            package app.astrid.server.session

            import app.astrid.server.Stores
            import app.astrid.server.RootDataClass
            import app.astrid.server.sealedThing
            import app.astrid.server.internalFun
            import app.astrid.server.practices.Lookalike
            val x = app.astrid.server.CONSTANT
            // import app.astrid.server.InComment
            import app.astrid.server.session.Fine
            import app.astrid.server.exercises.Fine
            import app.astrid.server.infra.config.Fine
        """.trimIndent()
        assertEquals(
            listOf(
                "3: import app.astrid.server.Stores",
                "4: import app.astrid.server.RootDataClass",
                "5: import app.astrid.server.sealedThing",
                "6: import app.astrid.server.internalFun",
                "7: import app.astrid.server.practices.Lookalike",
                "8: val x = app.astrid.server.CONSTANT",
            ),
            matchingLines(source, rootReference),
        )
    }

    @Test
    fun `the engine never references the server`() {
        val engine = sources("engine/src/main/kotlin")
        assertTrue(engine.size > 5, "the scan saw only ${engine.size} engine sources")
        val violations = engine.flatMap { file ->
            matchingLines(file.readText(), Regex("""\bapp\.astrid\.server\b""")).map { "${file.name} $it" }
        }
        assertEquals(emptyList(), violations)
    }

    @Test
    fun `the scan flags imports, wildcard imports and inline references but not comments`() {
        val source = """
            package app.astrid.server.exercises

            import app.astrid.server.practice.PracticeStore
            import app.astrid.server.practice.*
            // import app.astrid.server.practice.InComment
             * app.astrid.server.practice.InKdoc
            val x = app.astrid.server.practice.Other()
            import app.astrid.server.practices.NotThePackage
        """.trimIndent()
        assertEquals(
            listOf(
                "3: import app.astrid.server.practice.PracticeStore",
                "4: import app.astrid.server.practice.*",
                "7: val x = app.astrid.server.practice.Other()",
            ),
            forbiddenReferences(source, listOf("practice")),
        )
    }
}
