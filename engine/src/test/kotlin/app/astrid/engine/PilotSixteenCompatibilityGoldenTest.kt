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

class PilotSixteenCompatibilityGoldenTest {
    @Test
    fun `published pilot sixteen generation and evaluation remain frozen`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(FIXTURE_ROW_COUNT, expected.size)
        assertEquals(expected, PilotSixteenCompatibilityGolden.rows())
    }

    @Test
    fun `archive covers all formats module one tasks answer kinds and boundaries`() {
        val rows = resource(FIXTURE_PATH).bufferedReader().useLines { lines ->
            lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
        }
        assertEquals((1..30).map { "E${it.toString().padStart(3, '0')}" }.toSet(), rows.map { it[1] }.toSet())
        val authoredCounts = mapOf(
            "E016" to 7, "E017" to 7, "E018" to 8, "E019" to 8, "E020" to 2, "E021" to 2,
            "E022" to 8, "E023" to 8, "E024" to 3, "E025" to 6, "E026" to 2, "E027" to 2,
            "E028" to 3,
            "E029" to 4,
            "E030" to 4,
        )
        authoredCounts.forEach { (formatId, taskCount) ->
            val formatRows = rows.filter { it[1] == formatId }
            assertEquals(taskCount + BOUNDARY_ROW_COUNT, formatRows.count { it[0] == "G" })
            assertTrue(setOf("T", "O", "M", "P").all { kind -> formatRows.any { it[0] == kind } })
            BOUNDARY_SEEDS.forEach { seed ->
                assertTrue(formatRows.any { it[0] == "G" && it[3] == seed.toString() && it[4] == MAX_INDEX.toString() })
            }
        }
        assertEquals(76, rows.count { it[0] == "O" && it[1] == "E029" })
        assertEquals(36, rows.count { it[0] == "O" && it[1] == "E030" })
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
        assertTrue("releaseSourceCommit=9bb10ae5633edb192651dc3d6956ff269b2a61eb" in provenance)
        assertTrue("releaseVersion=0.18.0" in provenance)
        assertTrue("captureDate=2026-11-11" in provenance)
        assertTrue("contentRevision=pilot-16" in provenance)
        assertTrue("generatorRevision=generator-16" in provenance)
        assertTrue("evaluationRevision=evaluation-16" in provenance)
        assertTrue("reviewState=compatibility-only-not-linguistic-approval" in provenance)
        assertTrue("rowCount=$FIXTURE_ROW_COUNT" in provenance)
        assertTrue("fixtureSha256=$FIXTURE_SHA256" in provenance)
        assertTrue("reviewSha256=$REVIEW_SHA256" in provenance)
        assertEquals(REVIEW_SHA256, publishedPairSixteenReviewBytes().sha256())
    }

    @Test
    fun `teaching review contains all fourteen released guided introductions`() {
        val review = publishedPairSixteenReviewBytes().toString(Charsets.UTF_8)
        val revisions = setOf(
            "module-one-singular-1", "module-one-groups-1", "module-one-er-1", "module-one-things-1",
            "module-one-present-1", "module-one-order-1", "module-one-possession-1",
            "module-one-personal-1",
            "module-one-ability-1",
            "module-one-volition-1", "module-one-obligation-1", "module-one-permission-1",
            "module-one-circumstances-1",
            "module-one-questions-1",
        )
        revisions.forEach { assertTrue(it in review, it) }
    }

    @Test
    fun `pair sixteen literal question task and metadata membership are included`() {
        val tasks = NeutralQuestionContent.tasks
        assertEquals(4, tasks.size)
        assertEquals(
            setOf(
                "question-read-newspaper", "question-work",
                "question-residence-plural", "question-possession-letter",
            ),
            tasks.map { it.id }.toSet(),
        )
        assertTrue(tasks.all { it.targetScopeId == "neutral-positive-yes-no" })
        assertTrue(tasks.all { it.feedbackExposureKeys.last() == "module-one:pattern:neutral-positive-yes-no" })
        assertEquals(
            PAIR_SIXTEEN_IDS,
            PilotContent.pilotSixteenSnapshot.lexicalReview.map { it.id }.filter { it in PAIR_SIXTEEN_IDS }.toSet(),
        )
        assertEquals("C010", NeutralQuestionContent.CONSTRUCTION_ID)
        assertTrue(PilotContent.pilotSixteenSnapshot.lexicalReview.single { it.id == "L032" }.constraints.contains("C010"))
        assertTrue(!PilotContent.pilotFifteenSnapshot.lexicalReview.single { it.id == "L032" }.constraints.contains("C010"))
        assertEquals(
            (1..30).map { "E${it.toString().padStart(3, '0')}" }.toSet(),
            PilotContent.pilotSixteenSnapshot.formats.map { it.id }.toSet(),
        )
        assertEquals(
            (1..29).map { "E${it.toString().padStart(3, '0')}" }.toSet(),
            PilotContent.pilotFifteenSnapshot.formats.map { it.id }.toSet(),
        )
    }

    @Test
    fun `circumstance archive spans all sixty complete permutations and target distinctions`() {
        val engine = PilotEngine()
        val exercises = (0..3).map {
            engine.generate(GenerationRequest("E029", "G016", 47, it, "pilot-16", "generator-16"))
        }
        assertEquals(4, exercises.map { it.inspector.canonicalSentenceId }.toSet().size)
        assertEquals(60, exercises.sumOf { exercise ->
            val count = requireNotNull(exercise.inspector.tileSource).tokens.size
            (1..count).fold(1) { total, value -> total * value }
        })
        val four = exercises.first { requireNotNull(it.inspector.tileSource).tokens.size == 4 }
        val canonical = requireNotNull(four.inspector.tileSource).licensedOrders.single().blockIds
        val targetLastCoreWrong = Answer.Order(listOf(canonical[1], canonical[0], canonical[2], canonical[3]))
        val targetNotLast = Answer.Order(listOf(canonical[0], canonical[1], canonical[3], canonical[2]))
        assertEquals(TargetStatus.MET, engine.evaluate(four, targetLastCoreWrong).targetStatus)
        assertEquals(ResponseConstraintStatus.NOT_MET, engine.evaluate(four, targetLastCoreWrong).responseConstraintStatus)
        assertEquals(TargetStatus.NOT_MET, engine.evaluate(four, targetNotLast).targetStatus)
        val source = four.source as CircumstanceExerciseSource
        assertEquals("da", four.prompt.cueLanguage)
        assertEquals("da", four.prompt.answerLanguage)
        assertEquals(
            source.task.parts.dropLast(1).joinToString(" ") { it.text }.replaceFirstChar(Char::titlecase) + ".",
            four.prompt.cue,
        )
        assertTrue(source.task.englishCore in requireNotNull(four.context))
        assertTrue(source.task.circumstanceMeaning in requireNotNull(four.context))
        assertTrue(FinalCircumstanceContent.introduction().filter { it.bodyLanguage == "da" }.size >= 6)
    }

    @Test
    fun `question archive spans all twenty complete permutations and target distinctions`() {
        val engine = PilotEngine()
        val exercises = (0..3).map {
            engine.generate(GenerationRequest("E030", "G018", 47, it, "pilot-16", "generator-16"))
        }
        assertEquals(4, exercises.map { it.inspector.canonicalSentenceId }.toSet().size)
        assertEquals(20, exercises.sumOf { exercise ->
            val count = requireNotNull(exercise.inspector.tileSource).tokens.size
            (1..count).fold(1) { total, value -> total * value }
        })
        val three = exercises.first { requireNotNull(it.inspector.tileSource).tokens.size == 3 }
        val canonical = requireNotNull(three.inspector.tileSource).licensedOrders.single().blockIds
        val targetPositionsMetRemainderWrong = Answer.Order(listOf(canonical[0], canonical[1], canonical[2]))
        val targetNotMet = Answer.Order(listOf(canonical[1], canonical[0], canonical[2]))
        assertEquals(TargetStatus.MET, engine.evaluate(three, targetPositionsMetRemainderWrong).targetStatus)
        assertEquals(ResponseConstraintStatus.MET, engine.evaluate(three, targetPositionsMetRemainderWrong).responseConstraintStatus)
        assertEquals(TargetStatus.NOT_MET, engine.evaluate(three, targetNotMet).targetStatus)
        val source = three.source as QuestionExerciseSource
        assertEquals(source.task.positiveProposition, three.prompt.cue)
        assertTrue(source.task.englishQuestion in requireNotNull(three.context))
        assertTrue(source.task.referenceContext in requireNotNull(three.context))
    }

    @Test
    fun `pair sixteen projection distinguishes private question mutations`() {
        val engine = PilotEngine()
        val exercise = engine.generate(GenerationRequest("E030", "G018", 47, 0, "pilot-16", "generator-16"))
        val source = exercise.source as QuestionExerciseSource
        val original = PilotSixteenCompatibilityGolden.generationHash(exercise)
        val mutations = listOf(
            mutate(exercise, source.copy(task = source.task.copy(positiveProposition = "changed"))),
            mutate(exercise, source.copy(task = source.task.copy(englishQuestion = "changed"))),
            mutate(exercise, source.copy(task = source.task.copy(referenceContext = "changed"))),
            mutate(exercise, source.copy(task = source.task.copy(frameVariantId = "changed"))),
            mutate(exercise, source.copy(task = source.task.copy(parts = source.task.parts.reversed()))),
            mutate(exercise, source.copy(blockRoles = source.blockRoles.mapValues { "changed" })),
            mutate(exercise, source.copy(answerKey = AnswerKey.TileOrder(emptyList(), null, null))),
        )
        mutations.forEach { assertNotEquals(original, PilotSixteenCompatibilityGolden.generationHash(it)) }
    }

    private fun mutate(exercise: GeneratedExercise, source: ExerciseSource): GeneratedExercise = GeneratedExercise(
        id = exercise.id,
        formatId = exercise.formatId,
        targetSkillId = exercise.targetSkillId,
        lexicalTarget = exercise.lexicalTarget,
        supportSkillIds = exercise.supportSkillIds,
        presentation = exercise.presentation,
        contentRevision = exercise.contentRevision,
        generatorRevision = exercise.generatorRevision,
        reproduction = exercise.reproduction,
        inspector = exercise.inspector,
        source = source,
        readingTarget = exercise.readingTarget,
        lexicalTargets = exercise.lexicalTargets,
    )

    @Test
    fun `export refuses to overwrite either immutable file`() {
        val directory = Files.createTempDirectory("pilot-sixteen-compatibility")
        val fixture = Files.createFile(directory.resolve("fixture.tsv"))
        val provenance = directory.resolve("fixture.provenance")
        assertFailsWith<IllegalArgumentException> { PilotSixteenCompatibilityGolden.write(fixture, provenance) }
    }

    private fun resource(path: String) =
        requireNotNull(javaClass.classLoader.getResourceAsStream(path)) { "Missing test resource $path" }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256")
        .digest(this)
        .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    private companion object {
        const val FIXTURE_PATH = "golden/pilot-16-generator-16-evaluation-16.tsv"
        const val PROVENANCE_PATH = "golden/pilot-16-generator-16-evaluation-16.provenance"
        const val FIXTURE_ROW_COUNT = 3_459
        const val FIXTURE_SHA256 = "c9805dd8cc2189d62efbbd7217fc987eb40eccef3889072b405cdc85b37d5ee8"
        const val REVIEW_SHA256 = "aca468c27942fc9cfe583ebb772b8cafe580cc18c4a5db0650c9f92f5be6be20"
        const val BOUNDARY_ROW_COUNT = 12
        const val MAX_INDEX = 1_000_000
        val BOUNDARY_SEEDS = listOf(0, 1, Int.MAX_VALUE)
        val PAIR_SIXTEEN_IDS = setOf("L039", "L040")
    }
}

