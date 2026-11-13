package app.astrid.engine

import java.nio.file.Files
import java.security.MessageDigest
import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PilotEighteenCompatibilityGoldenTest {
    @Test
    fun `canonical retained answers meet their target across every inherited format cycle`() {
        val statuses = PilotEighteenCompatibilityGolden.inheritedCanonicalStatuses()
        assertEquals((1..30).map(::formatId).toSet(), statuses.map { it.first }.toSet())
        assertTrue(statuses.isNotEmpty())
        assertTrue(statuses.all { (_, status) -> status == TargetStatus.MET })
    }

    @Test
    fun `published pilot eighteen generation and evaluation remain frozen`() {
        val bytes = resource(FIXTURE_PATH).use { it.readBytes() }
        assertEquals(FIXTURE_SHA256, bytes.sha256(), "The frozen fixture must never be regenerated")
        val expected = bytes.toString(Charsets.UTF_8).lineSequence().filter(String::isNotBlank).toList()
        assertEquals(FIXTURE_ROW_COUNT, expected.size)
        val actual = PilotEighteenCompatibilityGolden.rows()
        val firstDifference = expected.indices.firstOrNull { expected[it] != actual.getOrNull(it) }
        assertEquals(expected.size, actual.size, "pair-eighteen row count")
        assertNull(
            firstDifference,
            firstDifference?.let { "first pair-eighteen difference at row $it\nexpected=${expected[it]}\nactual=${actual[it]}" },
        )
    }

    @Test
    fun `archive explicitly covers all formats P1 tasks answer families and boundaries`() {
        val rows = fixtureRows()
        assertEquals((1..39).map(::formatId).toSet(), rows.map { it[1] }.toSet())
        assertEquals(P1_COUNTS, PilotEighteenCompatibilityGolden.P1_TASK_COUNTS)
        P1_COUNTS.forEach { (format, count) ->
            val generation = rows.filter { it[0] == "G" && it[1] == format }
            assertEquals(count + BOUNDARY_ROW_COUNT, generation.size, format)
            BOUNDARY_SEEDS.forEach { seed ->
                assertTrue(generation.any { it[3] == seed.toString() && it[4] == MAX_INDEX.toString() }, "$format/$seed")
            }
        }
        val answerKinds = rows.filter { it[0] != "G" && it[1] in P1_COUNTS }.map { it[0] }.toSet()
        assertEquals(setOf("T", "O", "M", "P"), answerKinds)
        assertEquals(LITERAL_P1_TASK_IDS, generatedP1Exercises().map { taskId(it) }.toSet())
    }

    @Test
    fun `matching archive contains every partial bijection and malformed boundary`() {
        generatedP1Exercises().filter { it.formatId == "E033" }.forEach { exercise ->
            val size = requireNotNull(exercise.prompt.matchingColumns).left.size
            val expected = if (size == 3) 34 else 209
            val matchingAnswers = PilotEighteenCompatibilityGolden.answers(exercise).filterIsInstance<Answer.Matching>()
            assertTrue(matchingAnswers.size >= expected + 6, taskId(exercise))
            val valid = matchingAnswers.filter { answer ->
                answer.pairs.map(MatchingPair::leftId).distinct().size == answer.pairs.size &&
                    answer.pairs.map(MatchingPair::rightId).distinct().size == answer.pairs.size &&
                    answer.pairs.all { pair ->
                        pair.leftId in requireNotNull(exercise.prompt.matchingColumns).left.map(MatchingItem::id) &&
                            pair.rightId in requireNotNull(exercise.prompt.matchingColumns).right.map(MatchingItem::id)
                    }
            }
            val semanticBijections = valid.map { answer ->
                answer.pairs.map { it.leftId to it.rightId }.sortedBy { it.first }
            }.toSet()
            assertEquals(expected, semanticBijections.size)
            assertTrue(valid.size > semanticBijections.size, "ordered arrays must be frozen independently for ${taskId(exercise)}")
            val correct = requireNotNull(exercise.inspector.vocabularySource).matchingPairs
                .map { MatchingPair(it.leftId, it.rightId) }
            assertTrue(matchingAnswers.any { it.pairs == correct })
            assertTrue(matchingAnswers.any { it.pairs == correct.reversed() })
            assertTrue(matchingAnswers.any { it.pairs == correct.dropLast(1).reversed() })
        }
    }

    @Test
    fun `typed and tile archives cover literal targets alternatives Unicode prose mass shape and every order`() {
        val exercises = generatedP1Exercises()
        val typed = exercises.filter { it.formatId in setOf("E034", "E035", "E036") }
        assertEquals(setOf("studerer", "nu", "drikker", "vand"), typed.map {
            (it.source as VocabularyP1ExerciseSource).task.expected
        }.toSet())
        typed.forEach { exercise ->
            val task = (exercise.source as VocabularyP1ExerciseSource).task
            val text = PilotEighteenCompatibilityGolden.answers(exercise).filterIsInstance<Answer.Text>().map(Answer.Text::text)
            assertTrue(task.expected in text)
            assertTrue("  ${task.expected}  " in text)
            assertTrue("${task.expected} ekstra" in text)
            assertTrue(text.any { it != task.expected && it.uppercase(Locale.ROOT) == task.expected.uppercase(Locale.ROOT) })
            assertTrue(text.any { java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFC) != it })
            val unicode = text.filter { it.any { character -> character.code > 127 } }
            assertTrue(unicode.map { java.text.Normalizer.normalize(it, java.text.Normalizer.Form.NFC) }.distinct().size < unicode.size)
            requireNotNull(exercise.inspector.vocabularySource).knownAlternatives.forEach { assertTrue(it.text in text) }
        }
        val mass = typed.single { taskId(it) == "p1-4.5" }
        val massAnswers = PilotEighteenCompatibilityGolden.answers(mass).filterIsInstance<Answer.Text>().map(Answer.Text::text)
        assertTrue(setOf("en vand", "et vand").all { it in massAnswers })
        exercises.filter { it.formatId in setOf("E037", "E038") }.forEach { exercise ->
            val count = exercise.prompt.blocks.size
            val archivedOrders = PilotEighteenCompatibilityGolden.answers(exercise).filterIsInstance<Answer.Order>()
            val completeOrders = archivedOrders
                .count { it.blockIds.size == count && it.blockIds.toSet().size == count }
            assertEquals(factorial(count), completeOrders, taskId(exercise))
            val details = requireNotNull(exercise.inspector.vocabularySource)
            details.acceptedOrders.forEach { accepted ->
                assertTrue(archivedOrders.any { it.blockIds == accepted }, "accepted ${taskId(exercise)}/$accepted")
            }
            details.reviewedOrders.forEach { reviewedOrder ->
                val blockIds = tileIdsForWords(details, reviewedOrder.words)
                assertTrue(archivedOrders.any { it.blockIds == blockIds }, "reviewed ${taskId(exercise)}/${reviewedOrder.words}")
                val evaluation = PilotEngine().evaluate(exercise, Answer.Order(blockIds))
                assertEquals(reviewedOrder.targetStatus, evaluation.targetStatus)
                assertEquals(reviewedOrder.linguisticStatus, evaluation.linguisticStatus)
                assertEquals(reviewedOrder.meaningSatisfaction, evaluation.meaningSatisfaction)
                assertEquals(reviewedOrder.responseConstraintStatus, evaluation.responseConstraintStatus)
                reviewedOrder.issueCode?.let { issue -> assertTrue(evaluation.issues.any { it.code == issue }) }
            }
        }
        val spareTile = exercises.single { it.formatId == "E038" }
        val spareTileDetails = requireNotNull(spareTile.inspector.vocabularySource)
        assertTrue(spareTileDetails.acceptedOrders.all { it.size == 3 })
        assertTrue(spareTileDetails.reviewedOrders.all { it.words.size == 3 })
        val reviewed = exercises.associateBy(::taskId)
        assertEquals(2, requireNotNull(reviewed.getValue("p1-3.3").inspector.vocabularySource).reviewedOrders.size)
        assertEquals(2, requireNotNull(reviewed.getValue("p1-5.5").inspector.vocabularySource).reviewedOrders.size)
        assertTrue(requireNotNull(reviewed.getValue("p1-4.3").inspector.vocabularySource).reviewedOrders.any {
            it.issueCode == "INFINITIVE_USED"
        })
    }

    @Test
    fun `private task source and inspector mutations are independently visible`() {
        val original = generatedP1Exercises().single { taskId(it) == "p1-3.1" }
        val source = original.source as VocabularyP1ExerciseSource
        val inspector = requireNotNull(original.inspector.vocabularySource)
        val hash = PilotEighteenCompatibilityGolden.generationHash(original)
        val privateTaskMutation = mutate(original, source = source.copy(task = source.task.copy(cue = "changed private cue")))
        val privateKeyMutation = mutate(
            original,
            source = source.copy(answerKey = AnswerKey.MeaningChoice("changed", setOf("changed"))),
        )
        val inspectorMutation = mutate(
            original,
            inspector = original.inspector.copy(vocabularySource = inspector.copy(operation = "changed inspector operation")),
        )
        assertNotEquals(hash, PilotEighteenCompatibilityGolden.generationHash(privateTaskMutation))
        assertNotEquals(hash, PilotEighteenCompatibilityGolden.generationHash(privateKeyMutation))
        assertNotEquals(hash, PilotEighteenCompatibilityGolden.generationHash(inspectorMutation))
    }

    @Test
    fun `provenance pins published source revisions review and bytes`() {
        val provenance = resource(PROVENANCE_PATH).bufferedReader().use { it.readText() }
        assertTrue("releaseSourceCommit=9eefcfed708670ce45dbdd9b11adc41f9fe17d25" in provenance)
        assertTrue("releaseVersion=0.20.1" in provenance)
        assertTrue("captureDate=$CAPTURE_DATE" in provenance)
        assertTrue("projectionSchema=pair18-engine-v1" in provenance)
        assertTrue("contentRevision=pilot-18" in provenance)
        assertTrue("generatorRevision=generator-18" in provenance)
        assertTrue("evaluationRevision=evaluation-18" in provenance)
        assertTrue("reviewState=compatibility-only-not-linguistic-approval" in provenance)
        assertTrue("rowCount=$FIXTURE_ROW_COUNT" in provenance)
        assertTrue("fixtureSha256=$FIXTURE_SHA256" in provenance)
        assertTrue("reviewSha256=$REVIEW_SHA256" in provenance)
        assertEquals(REVIEW_SHA256, publishedPairEighteenReviewBytes().sha256())
    }

    @Test
    fun `export refuses fixture and provenance collisions independently`() {
        val fixtureCollisionDir = Files.createTempDirectory("pilot-eighteen-fixture-collision")
        val fixture = Files.createFile(fixtureCollisionDir.resolve("fixture.tsv"))
        assertFailsWith<IllegalArgumentException> {
            PilotEighteenCompatibilityGolden.write(fixture, fixtureCollisionDir.resolve("fixture.provenance"))
        }
        val provenanceCollisionDir = Files.createTempDirectory("pilot-eighteen-provenance-collision")
        val provenance = Files.createFile(provenanceCollisionDir.resolve("fixture.provenance"))
        assertFailsWith<IllegalArgumentException> {
            PilotEighteenCompatibilityGolden.write(provenanceCollisionDir.resolve("fixture.tsv"), provenance)
        }
    }

    private fun generatedP1Exercises(): List<GeneratedExercise> = VocabularyP1Content.tasks.map { task ->
        (0 until VocabularyP1Content.tasks(task.formatId).size).asSequence().map { index ->
            PilotEngine().generate(GenerationRequest(task.formatId, task.targetSkillId, 47, index, "pilot-18", "generator-18"))
        }.first { taskId(it) == task.id }
    }

    private fun taskId(exercise: GeneratedExercise) = requireNotNull(exercise.inspector.vocabularySource).taskId

    private fun tileIdsForWords(details: VocabularyExerciseSourceDetails, words: List<String>): List<String> {
        val idsByForm = details.tileOccurrences.groupBy(VocabularyTileOccurrenceDetails::form).mapValues { (_, occurrences) ->
            ArrayDeque(occurrences.map(VocabularyTileOccurrenceDetails::blockId))
        }
        return words.map { word ->
            requireNotNull(idsByForm[word]?.removeFirstOrNull()) { "No unused tile occurrence for reviewed word $word" }
        }
    }

    private fun mutate(
        exercise: GeneratedExercise,
        source: ExerciseSource = exercise.source,
        inspector: ExerciseInspector = exercise.inspector,
    ) = GeneratedExercise(
        exercise.id, exercise.formatId, exercise.targetSkillId, exercise.lexicalTarget, exercise.supportSkillIds,
        exercise.presentation, exercise.contentRevision, exercise.generatorRevision, exercise.reproduction, inspector, source,
        exercise.readingTarget, exercise.lexicalTargets,
    )

    private fun fixtureRows() = resource(FIXTURE_PATH).bufferedReader().useLines { lines ->
        lines.filter(String::isNotBlank).map { it.split('\t') }.toList()
    }

    private fun resource(path: String) =
        requireNotNull(javaClass.classLoader.getResourceAsStream(path)) { "Missing test resource $path" }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256").digest(this)
        .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    private fun formatId(number: Int) = "E${number.toString().padStart(3, '0')}"
    private fun factorial(value: Int) = (1..value).fold(1, Int::times)

    private companion object {
        const val FIXTURE_PATH = "golden/pilot-18-generator-18-evaluation-18.tsv"
        const val PROVENANCE_PATH = "golden/pilot-18-generator-18-evaluation-18.provenance"
        const val FIXTURE_ROW_COUNT = 3_591 // Captured from v0.20.1 in two fresh JVMs.
        const val FIXTURE_SHA256 = "c6a353ecc205ddbc8d4dee5e93d78cf2b7c3d507e9a7bf6b2b585401dbf36af8"
        const val REVIEW_SHA256 = "ef964f9d049d62dcfb24fb7c071a8b05b3a12f34bb39abcd73d042d7b0cd8f6a"
        const val CAPTURE_DATE = "2026-11-12"
        const val BOUNDARY_ROW_COUNT = 12
        const val MAX_INDEX = 1_000_000
        val BOUNDARY_SEEDS = setOf(0, 1, Int.MAX_VALUE)
        val P1_COUNTS = mapOf(
            "E031" to 3, "E032" to 1, "E033" to 4, "E034" to 2, "E035" to 1,
            "E036" to 1, "E037" to 3, "E038" to 1, "E039" to 7,
        )
        val LITERAL_P1_TASK_IDS = setOf(
            "p1-1.1", "p1-1.2", "p1-1.3", "p1-1.4",
            "p1-2.1", "p1-2.2", "p1-2.3", "p1-2.4",
            "p1-3.1", "p1-3.2", "p1-3.3", "p1-3.4", "p1-3.5",
            "p1-4.1", "p1-4.2", "p1-4.3", "p1-4.4", "p1-4.5",
            "p1-5.1", "p1-5.2", "p1-5.3", "p1-5.4", "p1-5.5",
        )
    }
}

