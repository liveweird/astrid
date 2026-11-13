package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class CumulativeReviewPlannerTest {
    private val engine = PilotEngine()
    private val planner = CumulativeReviewPlanner(engine)
    private val plannerV2 = CumulativeReviewPlanner(engine, "cumulative-review-2")

    @Test
    fun `empty history has no plan and short pools stay short`() {
        assertNull(planner.plan(0, emptyList()))
        val source = source("module-one-singular", "module-one-singular-1", 0)
        val short = assertNotNull(planner.plan(0, source.take(3)))
        assertEquals(3, short.plan.items.size)
        assertEquals(1, short.sourceLessonCount)
        assertEquals((0..2).toList(), short.plan.items.map { it.position })
    }

    @Test
    fun `mixed selection is deterministic unique and preserves per item revisions`() {
        val sources = source("module-one-singular", "module-one-singular-1", 7) +
            source("module-one-present", "module-one-present-1", 11) +
            source("vocabulary-p1-s4", "vocabulary-p1-s4-1", 13)
        val first = assertNotNull(planner.plan(Int.MAX_VALUE, sources))
        val second = assertNotNull(planner.plan(Int.MAX_VALUE, sources.reversed()))

        assertEquals(first.selections.map { it.candidateKey }, second.selections.map { it.candidateKey })
        assertEquals(8, first.plan.items.size)
        assertEquals(8, first.selections.map { it.candidateKey }.distinct().size)
        assertEquals("per-item", first.plan.contentRevision)
        assertEquals("per-item", first.plan.generatorRevision)
        assertEquals("per-item", first.plan.evaluationRevision)
        assertTrue(first.plan.items.any { it.exercise.contentRevision == "pilot-8" })
        assertTrue(first.plan.items.any { it.exercise.contentRevision == "pilot-10" })
        assertTrue(first.plan.items.all { it.policy != null })
        assertEquals(3, first.sourceLessonCount)
    }

    @Test
    fun `retained P1 matching is remapped to the pair eighteen board`() {
        val source = source("vocabulary-p1-s4", "vocabulary-p1-s4-1", 11).first()
        val selected = assertNotNull(planner.plan(19, listOf(source))).plan.items.single()
        val columns = assertNotNull(selected.exercise.prompt.matchingColumns)
        assertEquals("pilot-18", selected.exercise.contentRevision)
        assertEquals(columns.left.map { it.id }.toSet(), selected.assessments.mapNotNull { it.slotId }.toSet())
        assertEquals(3, selected.assessments.size)
    }

    @Test
    fun `cumulative one ignores P2 while cumulative two admits every taught P2 family`() {
        val p2Sources = (1..6).flatMap { lesson ->
            source("vocabulary-p2-s$lesson", "vocabulary-p2-s$lesson-1", lesson)
        }
        assertNull(planner.plan(47, p2Sources))
        assertFailsWith<IllegalArgumentException> { planner.candidateSemanticId(p2Sources.first().item) }
        val selected = assertNotNull(plannerV2.plan(47, p2Sources))
        assertEquals("cumulative-review-2", selected.plan.planRevision)
        assertTrue(selected.plan.items.isNotEmpty())
        assertTrue(selected.plan.items.all { it.exercise.contentRevision == "pilot-19" })
        assertTrue(selected.selections.all { it.sourceLessonId.startsWith("vocabulary-p2-s") })
        selected.selections.zip(selected.plan.items).forEach { (selection, item) ->
            val original = p2Sources.single {
                it.sourcePracticeSessionId == selection.sourcePracticeSessionId &&
                    it.sourcePosition == selection.sourcePosition
            }.item
            assertEquals(plannerV2.candidateSemanticId(original), plannerV2.candidateSemanticId(item))
        }
    }

    @Test
    fun `P2 matching regeneration rebuilds assessment slots against the displayed board`() {
        val source = source("vocabulary-p2-s1", "vocabulary-p2-s1-1", 11).first()
        val originalSlots = source.item.assessments.mapNotNull { it.slotId }.toSet()
        val selected = assertNotNull(plannerV2.plan(73, listOf(source))).plan.items.single()
        val columns = assertNotNull(selected.exercise.prompt.matchingColumns)
        assertEquals(columns.left.map { it.id }.toSet(), selected.assessments.mapNotNull { it.slotId }.toSet())
        assertEquals(columns.left.size, selected.assessments.size)
        assertTrue(selected.assessments.all { assessment ->
            assessment.slotId != null && assessment.targetDescriptor.lexicalTarget != null
        })
        if (columns.left.map { it.id }.toSet() != originalSlots) {
            assertTrue(selected.assessments.none { it.slotId in originalSlots })
        }
    }

    @Test
    fun `retained pair ten recall keeps its original grading vocabulary`() {
        val result = assertNotNull(
            planner.plan(13, source("module-one-present", "module-one-present-1", 11)),
        )
        val recall = result.plan.items.first { it.exercise.formatId == "E023" }
        val evaluation = engine.evaluate(recall.exercise, Answer.Text("hedder"))

        assertEquals("pilot-10", recall.exercise.contentRevision)
        assertEquals("generator-10", recall.exercise.generatorRevision)
        assertEquals("evaluation-10", engine.evaluationRevision(recall.exercise.contentRevision, recall.exercise.generatorRevision))
        assertEquals(LinguisticStatus.UNKNOWN, evaluation.linguisticStatus)
        assertEquals(
            false,
            PracticeCompletionPolicy.canAdvance(
                recall.policy?.completionRuleId,
                PracticeCompletionAttemptObservation(
                    evaluation.targetStatus,
                    evaluation.linguisticStatus,
                    evaluation.responseConstraintStatus,
                    evaluation.issues.map { it.code },
                ),
            ),
        )
    }

    @Test
    fun `selection prefers the least recently released task after the pool has all been seen`() {
        val sources = source("module-one-singular", "module-one-singular-1", 5)
        val baseline = assertNotNull(planner.plan(23, sources))
        val ranks = baseline.selections.reversed().mapIndexed { index, selection -> selection.semanticId to index.toLong() }.toMap()
        val repeated = assertNotNull(planner.plan(23, sources, ranks))
        assertEquals(baseline.selections.last().semanticId, repeated.selections.first().semanticId)
    }

    @Test
    fun `all seen selection still balances lessons before release age`() {
        val singular = source("module-one-singular", "module-one-singular-1", 5)
        val present = source("module-one-present", "module-one-present-1", 7)
        val sources = singular + present
        val ranks = sources.associate { source ->
            planner.candidateSemanticId(source.item) to
                if (source.sourceLessonId == "module-one-singular") source.sourcePosition.toLong() else 100L + source.sourcePosition
        }

        val selected = assertNotNull(planner.plan(23, sources, ranks)).selections

        assertEquals(2, selected.take(2).map { it.sourceLessonId }.distinct().size)
    }

    @Test
    fun `every taught lesson family preserves authored semantics and literal revision pairs`() {
        ELIGIBLE_SOURCE_PLANS.forEachIndexed { index, (focus, revision) ->
            val sources = source(focus, revision, index)
            listOf(0, Int.MAX_VALUE).forEach { seed ->
                val result = assertNotNull(planner.plan(seed, sources), "$focus at seed $seed")
                assertTrue(result.plan.items.isNotEmpty(), focus)
                result.selections.zip(result.plan.items).forEach { (selection, selected) ->
                    val original = sources.single { it.sourcePosition == selection.sourcePosition }.item
                    assertEquals(planner.candidateSemanticId(original), planner.candidateSemanticId(selected), focus)
                    if (focus.startsWith("vocabulary-p1-")) {
                        assertEquals("pilot-18", selected.exercise.contentRevision, focus)
                    } else {
                        assertEquals(original.exercise.contentRevision, selected.exercise.contentRevision, focus)
                        assertEquals(original.exercise.generatorRevision, selected.exercise.generatorRevision, focus)
                    }
                }
            }
        }
    }

    @Test
    fun `duplicate authored candidates choose a stable witness and retain distinct source introductions`() {
        val originals = source("module-one-singular", "module-one-singular-1", 3)
        val duplicate = listOf(
            originals.first().copy(sourcePracticeSessionId = "z-run"),
            originals.first().copy(sourcePracticeSessionId = "a-run"),
        )
        val deduped = assertNotNull(planner.plan(0, duplicate))
        assertEquals(1, deduped.plan.items.size)
        assertEquals("a-run", deduped.selections.single().sourcePracticeSessionId)

        val firstIntroduction = originals[0].sourceIntroduction.first()
        val distinctRuns = listOf(
            originals[0].copy(
                sourcePracticeSessionId = "first-run",
                sourceIntroduction = listOf(firstIntroduction.copy(title = "First source teaching")),
                sourceIntroductionExposureKeys = listOf("first:introduction"),
            ),
            originals[1].copy(
                sourcePracticeSessionId = "second-run",
                sourceIntroduction = listOf(firstIntroduction.copy(title = "Second source teaching")),
                sourceIntroductionExposureKeys = listOf("second:introduction"),
            ),
        )
        val combined = assertNotNull(planner.plan(0, distinctRuns)).plan
        assertEquals(setOf("First source teaching", "Second source teaching"), combined.introduction.map { it.title }.toSet())
        assertEquals(setOf("first:introduction", "second:introduction"), combined.introductionExposureKeys?.toSet())
    }

    @Test
    fun `invalid seeds fail and unmatched retained P1 tasks fail closed`() {
        assertFailsWith<IllegalArgumentException> {
            CumulativeReviewPlanner(engine, "cumulative-review-unknown")
        }
        assertFailsWith<IllegalArgumentException> {
            planner.plan(-1, source("module-one-singular", "module-one-singular-1", 0))
        }
        val retained = source("vocabulary-p1-s4", "vocabulary-p1-s4-1", 11).first()
        assertNull(planner.plan(0, listOf(retained.copy(sourceFocusId = "vocabulary-p1-s1"))))
        val unsupportedP1 = retained.copy(
            item = retained.item.copy(
                exercise = retained.item.exercise.withSupportSkills(listOf("unexpected-support")),
            ),
        )
        assertNull(planner.plan(0, listOf(unsupportedP1)))
    }

    @Test
    fun `legacy released formats retain stable semantic identities without becoming eligible sources`() {
        val legacySources = listOf(
            "reading-basics" to "practice-1",
            "present-verbs" to "present-verbs-1",
            "verb-article-gaps" to "verb-article-gaps-1",
        ).flatMapIndexed { index, (focus, revision) -> source(focus, revision, index + 3) }

        val first = legacySources.map { planner.candidateSemanticId(it.item) }
        val second = legacySources.reversed().map { planner.candidateSemanticId(it.item) }.reversed()

        assertEquals(first, second)
        assertTrue(first.all(String::isNotBlank))
        legacySources.forEach { source ->
            assertNotNull(planner.plan(5, listOf(source)))
        }
        legacySources.mapNotNull { source ->
            source.item.exercise.mismatchedFoundationSource()?.let { mismatch ->
                source.copy(item = source.item.copy(exercise = source.item.exercise.withSource(mismatch)))
            }
        }.distinctBy { it.item.exercise.source::class }.forEach { source ->
            assertEquals(source.item.exercise, assertNotNull(planner.plan(5, listOf(source))).plan.items.single().exercise)
        }
    }

    @Test
    fun `foundation release identities cover every persisted exercise source`() {
        val original = source("module-one-singular", "module-one-singular-1", 3).first().item
        val subject = Constituent(listOf("subject"), "Hun", "subject")
        val verb = Verb("verb", "læse", "læser", "read")
        val sentence = Sentence("sentence-1", "construction-1", subject, verb, null)
        val answer = AnswerKey.ModuleOneForm("hun", emptySet(), true)
        fun identity(source: ExerciseSource, canonicalSentenceId: String? = null) = planner.candidateSemanticId(
            original.copy(
                exercise = original.exercise.withSource(
                    source,
                    original.exercise.inspector.copy(canonicalSentenceId = canonicalSentenceId),
                ),
            ),
        )

        assertTrue(identity(LexicalSource(LexicalSense("entry", "sense", "ord", "word", "et"), answer)).contains("entry:sense"))
        assertTrue(identity(SentenceSource(sentence, answer)).contains("sentence-1"))
        val reading = ReadingSource(
            listOf(ReadingProposition(sentence, "subject", "verb", "object")),
            "subject",
            "verb",
            "object",
            answer,
        )
        assertTrue(identity(reading).contains("sentence-1"))
        assertTrue(identity(reading, "reading-canonical").contains("reading-canonical"))
        assertTrue(identity(TileExerciseSource(sentence, answer), "tile-canonical").contains("tile-canonical"))
        assertFailsWith<IllegalStateException> { identity(TileExerciseSource(sentence, answer)) }
        assertTrue(identity(MatchingExerciseSource(answer)).contains(original.targetDescriptor.targetKey()))
        assertTrue(identity(MatchingExerciseSource(answer), "matching-canonical").contains("matching-canonical"))
        assertTrue(identity(MultipleGapExerciseSource(sentence, answer)).contains("sentence-1"))
    }

    @Test
    fun `foundation generators verify their authored sources during same pair regeneration`() {
        val template = source("module-one-singular", "module-one-singular-1", 3).first()
        val requests = listOf(
            GenerationRequest("E009", "G005", 47, 5, "pilot-4", "generator-4"),
            GenerationRequest("E010", null, 47, 5, "pilot-4", "generator-4"),
            GenerationRequest("E012", "G005", 47, 5, "pilot-5", "generator-5"),
            GenerationRequest("E014", null, 0, 0, "pilot-6", "generator-6"),
            GenerationRequest("E015", "G004", 47, 5, "pilot-7", "generator-7"),
        )

        requests.forEach { request ->
            val generated = engine.generate(request)
            val candidate = template.copy(item = template.item.copy(exercise = generated))
            assertNotNull(planner.plan(request.seed, listOf(candidate)), request.formatId)
            generated.mismatchedFoundationSource()?.let { mismatch ->
                val mismatched = candidate.copy(item = candidate.item.copy(exercise = generated.withSource(mismatch)))
                assertEquals(
                    mismatched.item.exercise,
                    assertNotNull(planner.plan(request.seed, listOf(mismatched))).plan.items.single().exercise,
                )
            }
        }
    }

    @Test
    fun `same pair regeneration falls back to the verified source recipe when no variant is equivalent`() {
        val source = source("module-one-present", "module-one-present-1", 11).first()
        val verified = source.copy(
            item = source.item.copy(
                exercise = source.item.exercise.withSupportSkills(listOf("verified-source-support")),
            ),
        )

        val selected = assertNotNull(planner.plan(47, listOf(verified))).plan.items.single()

        assertEquals(verified.item.exercise, selected.exercise)
    }

    @Test
    fun `release rank boundaries remain deterministic without arithmetic overflow`() {
        val sources = source("module-one-present", "module-one-present-1", 7) +
            source("module-one-order", "module-one-order-1", 9)
        val ranks = sources.mapIndexed { index, source ->
            planner.candidateSemanticId(source.item) to if (index % 2 == 0) Long.MIN_VALUE else Long.MAX_VALUE
        }.toMap()

        val first = assertNotNull(planner.plan(Int.MAX_VALUE, sources, ranks))
        val second = assertNotNull(planner.plan(Int.MAX_VALUE, sources.reversed(), ranks))

        assertEquals(first.selections.map { it.candidateKey }, second.selections.map { it.candidateKey })
    }

    private fun source(focus: String, revision: String, seed: Int): List<CumulativeReviewSourceItem> {
        val plan = PracticePlanner(engine).plan(focus, revision, seed)
        return plan.items.map { item ->
            CumulativeReviewSourceItem(
                "$focus-$seed",
                focus,
                item.position,
                focus,
                revision,
                plan.introduction,
                plan.introductionExposureKeys.orEmpty(),
                item,
                PracticeItemPolicy(plan.completionRuleId, plan.exposurePolicyId),
            )
        }
    }

    private fun GeneratedExercise.withSupportSkills(skills: List<String>) = GeneratedExercise(
        id,
        formatId,
        targetSkillId,
        lexicalTarget,
        skills,
        presentation,
        contentRevision,
        generatorRevision,
        reproduction,
        inspector,
        source,
        readingTarget,
        lexicalTargets,
    )

    private fun GeneratedExercise.withSource(source: ExerciseSource, inspector: ExerciseInspector) = GeneratedExercise(
        id,
        formatId,
        targetSkillId,
        lexicalTarget,
        supportSkillIds,
        presentation,
        contentRevision,
        generatorRevision,
        reproduction,
        inspector,
        source,
        readingTarget,
        lexicalTargets,
    )

    private fun GeneratedExercise.withSource(source: ExerciseSource) = withSource(source, inspector)

    private fun GeneratedExercise.mismatchedFoundationSource(): ExerciseSource? = when (val value = source) {
        is LexicalSource -> value.copy(sense = value.sense.copy(senseId = value.sense.senseId + "-mismatch"))
        is SentenceSource -> value.copy(sentence = value.sentence.copy(id = value.sentence.id + "-mismatch"))
        is ReadingSource -> value.copy(questionSubjectId = value.questionSubjectId + "-mismatch")
        is TileExerciseSource -> value.copy(sentence = null)
        is MultipleGapExerciseSource -> value.copy(sentence = value.sentence.copy(id = value.sentence.id + "-mismatch"))
        else -> null
    }

    private companion object {
        val ELIGIBLE_SOURCE_PLANS = listOf(
            "module-one-singular", "module-one-groups", "module-one-er", "module-one-things",
            "module-one-present", "module-one-order", "module-one-possession", "module-one-reading-words",
            "module-one-personal", "module-one-ability", "module-one-volition", "module-one-obligation",
            "module-one-permission", "module-one-circumstances", "module-one-questions",
        ).map { it to "$it-1" } + (1..5).map { lesson ->
            "vocabulary-p1-s$lesson" to "vocabulary-p1-s$lesson-1"
        }
    }
}
