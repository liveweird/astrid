package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VocabularyLibraryPracticePlannerTest {
    private val engine = PilotEngine()

    @Test
    fun `saved P1 teaching admits only exact recognized senses and forms`() {
        listOf(1, 2).forEach { revision ->
            val claims = (1..4).flatMap { lesson ->
                claimsFor("vocabulary-p1-s$lesson", "vocabulary-p1-s$lesson-$revision")
            }

            assertEquals(VocabularyLibraryPracticeCounts(4, 2), VocabularyLibraryPracticePlanner.counts(claims))
            assertEquals(
                setOf("p1-1.1", "p1-1.2", "p1-1.3", "p1-2.2"),
                taskIds(plan(VocabularyLibraryPracticePlanner.FOCUS_RECOGNITION, 0, claims)),
            )
            assertEquals(
                setOf("p1-3.5", "p1-4.5"),
                taskIds(plan(VocabularyLibraryPracticePlanner.FOCUS_RECALL, 0, claims)),
            )
        }
    }

    @Test
    fun `meaning and the requested exact form are both required`() {
        val workForms = forms("L027")
        val finite = workForms.single { it.text == "arbejder" }.formId
        val infinitive = workForms.single { it.text == "arbejde" }.formId
        val exact = VocabularyTeachingClaim("L027", "L027-S01", true, listOf(finite))

        assertEquals(VocabularyLibraryPracticeCounts(1, 0), VocabularyLibraryPracticePlanner.counts(listOf(exact)))
        assertEquals(
            VocabularyLibraryPracticeCounts(0, 0),
            VocabularyLibraryPracticePlanner.counts(listOf(exact.copy(meaningRecorded = false))),
        )
        assertEquals(
            VocabularyLibraryPracticeCounts(0, 0),
            VocabularyLibraryPracticePlanner.counts(listOf(exact.copy(formIds = listOf(infinitive)))),
        )
        assertEquals(
            VocabularyLibraryPracticeCounts(0, 0),
            VocabularyLibraryPracticePlanner.counts(listOf(exact.copy(senseId = "L027-other"))),
        )
        assertNull(VocabularyLibraryPracticePlanner.plan(
            engine, VocabularyLibraryPracticePlanner.FOCUS_RECALL, 0, listOf(exact),
        ))
    }

    @Test
    fun `shared P2 teaching can admit the identical water sense and form`() {
        val claims = claimsFor("vocabulary-p2-s2", "vocabulary-p2-s2-1")
        val result = plan(VocabularyLibraryPracticePlanner.FOCUS_RECALL, 5, claims)

        assertEquals(VocabularyLibraryPracticeCounts(0, 1), VocabularyLibraryPracticePlanner.counts(claims))
        assertEquals(setOf("p1-4.5"), taskIds(result))
    }

    @Test
    fun `plans are deterministic and vary item order without changing the admitted pool`() {
        val claims = (1..2).flatMap { lesson -> claimsFor("vocabulary-p1-s$lesson", "vocabulary-p1-s$lesson-2") }
        val first = plan(VocabularyLibraryPracticePlanner.FOCUS_RECOGNITION, 0, claims)
        val repeated = plan(VocabularyLibraryPracticePlanner.FOCUS_RECOGNITION, 0, claims)
        val rotated = plan(VocabularyLibraryPracticePlanner.FOCUS_RECOGNITION, Int.MAX_VALUE, claims)

        assertEquals(first.copy(items = emptyList()), repeated.copy(items = emptyList()))
        assertEquals(
            first.items.map { it.exercise.id to it.exercise.prompt },
            repeated.items.map { it.exercise.id to it.exercise.prompt },
        )
        assertEquals(taskIds(first), taskIds(rotated))
        assertNotEquals(orderedTaskIds(first), orderedTaskIds(rotated))
        assertEquals(first.items.indices.toList(), first.items.map(PracticePlanItem::position))
        assertEquals(rotated.items.indices.toList(), rotated.items.map(PracticePlanItem::position))
        assertTrue((first.items + rotated.items).all {
            it.exercise.contentRevision == "pilot-18" && it.exercise.generatorRevision == "generator-18"
        })
    }

    @Test
    fun `selected exercises retain published grading and operation targets`() {
        val recognitionClaims = (1..2).flatMap { lesson ->
            claimsFor("vocabulary-p1-s$lesson", "vocabulary-p1-s$lesson-2")
        }
        val recognition = plan(VocabularyLibraryPracticePlanner.FOCUS_RECOGNITION, 9, recognitionClaims)
        assertEquals(setOf("E031", "E032"), recognition.items.map { it.exercise.formatId }.toSet())
        recognition.items.forEach { item ->
            val task = (item.exercise.source as VocabularyP1ExerciseSource).task
            assertEquals(TargetStatus.MET, engine.evaluate(item.exercise, Answer.Text(task.expected)).targetStatus)
            val wrong = task.choices.first { it != task.expected }
            assertEquals(TargetStatus.NOT_MET, engine.evaluate(item.exercise, Answer.Text(wrong)).targetStatus)
        }

        val recallClaims = (3..4).flatMap { lesson ->
            claimsFor("vocabulary-p1-s$lesson", "vocabulary-p1-s$lesson-2")
        }
        val recall = plan(VocabularyLibraryPracticePlanner.FOCUS_RECALL, 9, recallClaims)
        assertEquals(setOf("E035", "E036"), recall.items.map { it.exercise.formatId }.toSet())
        recall.items.forEach { item ->
            val task = (item.exercise.source as VocabularyP1ExerciseSource).task
            assertEquals(TargetStatus.MET, engine.evaluate(item.exercise, Answer.Text(task.expected)).targetStatus)
            assertEquals(TargetStatus.NOT_MET, engine.evaluate(item.exercise, Answer.Text("ukendt")).targetStatus)
        }
    }

    @Test
    fun `unsupported focus and negative seed are rejected`() {
        val claims = claimsFor("vocabulary-p1-s1", "vocabulary-p1-s1-2")
        assertFailsWith<IllegalArgumentException> {
            VocabularyLibraryPracticePlanner.plan(engine, "other", 0, claims)
        }
        assertFailsWith<IllegalArgumentException> {
            VocabularyLibraryPracticePlanner.plan(
                engine, VocabularyLibraryPracticePlanner.FOCUS_RECOGNITION, -1, claims,
            )
        }
    }

    private fun plan(focusId: String, seed: Int, claims: List<VocabularyTeachingClaim>) =
        assertNotNull(VocabularyLibraryPracticePlanner.plan(engine, focusId, seed, claims))

    private fun claimsFor(focusId: String, planRevision: String): List<VocabularyTeachingClaim> {
        val source = VocabularyP1PracticePlanner.ALL_SPECIFICATIONS.any {
            it.focusId == focusId && it.planRevision == planRevision
        }.let { isP1 ->
            if (isP1) VocabularyP1PracticePlanner(engine).plan(focusId, planRevision, 0)
            else VocabularyP2PracticePlanner(engine).plan(focusId, planRevision, 0)
        }
        return VocabularyTeachingMapper.claims(listOf(VocabularyTeachingWitness(
            source.focusId,
            source.planRevision,
            source.contentRevision,
            source.generatorRevision,
            source.introductionExposureKeys.orEmpty(),
        )))
    }

    private fun forms(entryId: String) =
        VocabularyLibrary.catalog().items.single { it.entryId == entryId }.senses.single().activeForms

    private fun orderedTaskIds(plan: PracticePlan) = plan.items.map { (it.exercise.source as VocabularyP1ExerciseSource).task.id }
    private fun taskIds(plan: PracticePlan) = orderedTaskIds(plan).toSet()
}
