package app.astrid.engine

import java.nio.file.Files
import java.security.MessageDigest
import java.util.Base64
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PilotFourteenCompatibilityGoldenTest {
    @Test
    fun `published pilot fourteen generation and evaluation remain frozen`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(FIXTURE_ROW_COUNT, expected.size)
        assertEquals(expected, PilotFourteenCompatibilityGolden.rows())
    }

    @Test
    fun `archive covers all formats module one tasks answer kinds and boundaries`() {
        val rows = resource(FIXTURE_PATH).bufferedReader().useLines { lines ->
            lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
        }
        assertEquals((1..28).map { "E${it.toString().padStart(3, '0')}" }.toSet(), rows.map { it[1] }.toSet())
        val authoredCounts = mapOf(
            "E016" to 7, "E017" to 7, "E018" to 8, "E019" to 8, "E020" to 2, "E021" to 2,
            "E022" to 8, "E023" to 8, "E024" to 3, "E025" to 6, "E026" to 2, "E027" to 2,
            "E028" to 3,
        )
        authoredCounts.forEach { (formatId, taskCount) ->
            val formatRows = rows.filter { it[1] == formatId }
            assertEquals(taskCount + BOUNDARY_ROW_COUNT, formatRows.count { it[0] == "G" })
            assertTrue(setOf("T", "O", "M", "P").all { kind -> formatRows.any { it[0] == kind } })
            BOUNDARY_SEEDS.forEach { seed ->
                assertTrue(formatRows.any { it[0] == "G" && it[3] == seed.toString() && it[4] == MAX_INDEX.toString() })
            }
        }
        val moduleOneTextAnswers = rows.filter { it[0] == "T" && it[1] in authoredCounts }
            .map { Base64.getDecoder().decode(it[5]).toString(Charsets.UTF_8) }
        assertTrue("i" in moduleOneTextAnswers)
        assertTrue("De" in moduleOneTextAnswers)
        assertTrue(setOf("jeg", "du", "han", "hun", "vi", "I", "de", "være").all { it in moduleOneTextAnswers })
        assertTrue(setOf("den", "det").all { it in moduleOneTextAnswers })
        assertTrue(setOf("arbejder", "læser", "sover", "arbejde", "læse", "sove").all { it in moduleOneTextAnswers })
        assertTrue(setOf("har", "have").all { it in moduleOneTextAnswers })
        assertTrue(setOf("læse", "at læse", "læser", "LÆSE", "ukendt").all { it in moduleOneTextAnswers })
    }

    @Test
    fun `provenance pins release source revisions review and bytes`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }
        assertTrue("releaseSourceCommit=9dd787c14b3a5d4a414da763b4ef28ce511e63be" in provenance)
        assertTrue("releaseVersion=0.16.0" in provenance)
        assertTrue("captureDate=2026-11-11" in provenance)
        assertTrue("contentRevision=pilot-14" in provenance)
        assertTrue("generatorRevision=generator-14" in provenance)
        assertTrue("evaluationRevision=evaluation-14" in provenance)
        assertTrue("reviewState=compatibility-only-not-linguistic-approval" in provenance)
        assertTrue("rowCount=$FIXTURE_ROW_COUNT" in provenance)
        assertTrue("fixtureSha256=$FIXTURE_SHA256" in provenance)
        assertTrue("reviewSha256=$REVIEW_SHA256" in provenance)
        assertEquals(REVIEW_SHA256, publishedPairFourteenReviewBytes().sha256())
    }

    @Test
    fun `teaching review contains all twelve released guided introductions`() {
        val review = publishedPairFourteenReviewBytes().toString(Charsets.UTF_8)
        val revisions = setOf(
            "module-one-singular-1", "module-one-groups-1", "module-one-er-1", "module-one-things-1",
            "module-one-present-1", "module-one-order-1", "module-one-possession-1",
            "module-one-personal-1",
            "module-one-ability-1",
            "module-one-volition-1", "module-one-obligation-1", "module-one-permission-1",
        )
        revisions.forEach { assertTrue(it in review, it) }
    }

    @Test
    fun `pair fourteen literal modal task and metadata membership are included`() {
        val interpretation = ModalRoundContent.interpretationTasks()
        val construction = ModalRoundContent.constructionTasks()
        assertEquals(3, interpretation.size)
        assertEquals(3, construction.size)
        assertEquals(
            setOf("modal-volition-interpret-letter", "modal-obligation-interpret-book", "modal-permission-interpret-letter"),
            interpretation.map { it.id }.toSet(),
        )
        assertTrue(construction.all { it.formatId == "E025" && it.targetScopeId == "supplied-modal-order" })
        assertEquals(ModalRoundContent.meaningChoices, interpretation.map { it.definition.selectedMeaning })
        assertEquals(
            PAIR_FOURTEEN_IDS,
            PilotContent.currentSnapshot.lexicalReview.map { it.id }.filter { it in PAIR_FOURTEEN_IDS }.toSet(),
        )
        assertEquals("C014", ModalRoundContent.CONSTRUCTION_ID)
        val retainedE025 = PresentOrderContent.tasks("E025").map { it.id }
        val currentE025 = PresentOrderContent.tasks("E025", includeModalConstruction = true).map { it.id }
        assertEquals(3, retainedE025.size)
        assertEquals(retainedE025, currentE025.take(retainedE025.size))
        assertEquals(
            setOf("ability-select-letter", "ability-select-danish", "ability-recall-letter", "ability-recall-danish"),
            listOf("E026", "E027").flatMap(AbilityContent::tasks).map { it.id }.toSet(),
        )
        assertEquals(
            (1..28).map { "E${it.toString().padStart(3, '0')}" }.toSet(),
            PilotContent.currentSnapshot.formats.map { it.id }.toSet(),
        )
        assertEquals(
            (1..27).map { "E${it.toString().padStart(3, '0')}" }.toSet(),
            PilotContent.pilotThirteenSnapshot.formats.map { it.id }.toSet(),
        )
    }

    @Test
    fun `modal archive spans choice positions and marker before shape grades`() {
        val engine = PilotEngine()
        val selectionPositions = listOf(0, 1, 2).map { seed ->
            val exercise = engine.generate(GenerationRequest("E028", "G030", seed, 0, "pilot-14", "generator-14"))
            exercise.prompt.choices.indexOf((exercise.source as ModalInterpretationExerciseSource).task.definition.selectedMeaning)
        }.toSet()
        assertEquals(setOf(0, 1, 2), selectionPositions)
        val interpretation = engine.generate(GenerationRequest("E028", "G030", 47, 0, "pilot-14", "generator-14"))
        val expected = (interpretation.source as ModalInterpretationExerciseSource).task.definition.selectedMeaning
        assertEquals(ResponseConstraintStatus.MET, engine.evaluate(interpretation, Answer.Text("  $expected  ")).responseConstraintStatus)
        val construction = (0..5).map {
            engine.generate(GenerationRequest("E025", "G005", 47, it, "pilot-14", "generator-14"))
        }.first { (it.source as PresentOrderExerciseSource).task is ModalConstructionTask }
        val canonical = (construction.source.answerKey as AnswerKey.TileOrder).canonical
        val answer = Answer.Order(canonical.map { identity ->
            requireNotNull(construction.inspector.tileSource).tokens.single {
                it.lexicalId == identity.lexicalId && it.form == identity.form
            }.blockId
        })
        assertEquals(ResponseConstraintStatus.MET, engine.evaluate(construction, answer).responseConstraintStatus)
    }

    @Test
    fun `pair fourteen projection distinguishes modal branches and private mutations`() {
        val engine = PilotEngine()
        val interpretation = engine.generate(GenerationRequest("E028", "G030", 47, 0, "pilot-14", "generator-14"))
        val interpretationSource = interpretation.source as ModalInterpretationExerciseSource
        val definition = interpretationSource.task.definition
        val inspector = requireNotNull(interpretation.inspector.modalSource)
        val original = PilotFourteenCompatibilityGolden.generationHash(interpretation)
        val mutations = listOf(
            mutate(interpretation, inspector = interpretation.inspector.copy(modalSource = inspector.copy(taskId = "changed"))),
            mutate(interpretation, inspector = interpretation.inspector.copy(modalSource = inspector.copy(modalSenseId = "changed"))),
            mutate(
                interpretation,
                source = interpretationSource.copy(
                    task = interpretationSource.task.copy(definition = definition.copy(selectedMeaning = "changed")),
                ),
            ),
            mutate(
                interpretation,
                source = interpretationSource.copy(
                    answerKey = AnswerKey.SuppliedChoice("changed", setOf("changed")),
                ),
            ),
        )
        mutations.forEach { assertNotEquals(original, PilotFourteenCompatibilityGolden.generationHash(it)) }

        val constructions = (0..5).map {
            engine.generate(GenerationRequest("E025", "G005", 47, it, "pilot-14", "generator-14"))
        }
        val modal = constructions.first { (it.source as PresentOrderExerciseSource).task is ModalConstructionTask }
        val retained = constructions.first { (it.source as PresentOrderExerciseSource).task is SubjectFirstOrderTask }
        assertNotEquals(
            PilotFourteenCompatibilityGolden.generationHash(retained),
            PilotFourteenCompatibilityGolden.generationHash(modal),
        )
        val modalSource = modal.source as PresentOrderExerciseSource
        val modalTask = modalSource.task as ModalConstructionTask
        val changed = mutate(modal, source = modalSource.copy(task = modalTask.copy(id = "changed")))
        assertNotEquals(
            PilotFourteenCompatibilityGolden.generationHash(modal),
            PilotFourteenCompatibilityGolden.generationHash(changed),
        )
    }

    private fun mutate(
        exercise: GeneratedExercise,
        inspector: ExerciseInspector = exercise.inspector,
        source: ExerciseSource = exercise.source,
    ): GeneratedExercise = GeneratedExercise(
        id = exercise.id,
        formatId = exercise.formatId,
        targetSkillId = exercise.targetSkillId,
        lexicalTarget = exercise.lexicalTarget,
        supportSkillIds = exercise.supportSkillIds,
        presentation = exercise.presentation,
        contentRevision = exercise.contentRevision,
        generatorRevision = exercise.generatorRevision,
        reproduction = exercise.reproduction,
        inspector = inspector,
        source = source,
        readingTarget = exercise.readingTarget,
        lexicalTargets = exercise.lexicalTargets,
    )

    @Test
    fun `export refuses to overwrite either immutable file`() {
        val directory = Files.createTempDirectory("pilot-fourteen-compatibility")
        val fixture = Files.createFile(directory.resolve("fixture.tsv"))
        val provenance = directory.resolve("fixture.provenance")
        assertFailsWith<IllegalArgumentException> { PilotFourteenCompatibilityGolden.write(fixture, provenance) }
    }

    private fun resource(path: String) =
        requireNotNull(javaClass.classLoader.getResourceAsStream(path)) { "Missing test resource $path" }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    private companion object {
        const val FIXTURE_PATH = "golden/pilot-14-generator-14-evaluation-14.tsv"
        const val PROVENANCE_PATH = "golden/pilot-14-generator-14-evaluation-14.provenance"
        const val FIXTURE_ROW_COUNT = 3291
        const val FIXTURE_SHA256 = "3ea4133c5a1b75eb224fec6b3f45c361e367bfb9a7e599f8cf685c70d300d29c"
        const val REVIEW_SHA256 = "65dc76c6cf823d2dafc667805ec576bd0ddfd82534918fdf7674b3b953fcf9dc"
        const val BOUNDARY_ROW_COUNT = 12
        const val MAX_INDEX = 1_000_000
        val BOUNDARY_SEEDS = listOf(0, 1, Int.MAX_VALUE)
        val PAIR_FOURTEEN_IDS = setOf("L011", "L034", "L035", "L036")
    }
}
