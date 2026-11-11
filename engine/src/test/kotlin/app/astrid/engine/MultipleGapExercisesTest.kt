package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MultipleGapExercisesTest {
    private val engine = PilotEngine()

    @Test
    fun `revision seven adds the grammar gap format and retains pair six`() {
        val catalog = engine.catalog()
        assertEquals("pilot-12", catalog.contentRevision)
        assertEquals("generator-12", catalog.generatorRevision)
        assertEquals("evaluation-7", engine.evaluationRevision("pilot-7", "generator-7"))
        assertEquals("evaluation-6", engine.evaluationRevision("pilot-6", "generator-6"))
        assertEquals(
            ExerciseFormat("E015", "Choose a present verb and article", "G004"),
            catalog.formats.single { it.id == "E015" },
        )
        assertFailsWith<IllegalArgumentException> {
            engine.generate(request(0).copy(contentRevision = "pilot-6", generatorRevision = "generator-6"))
        }
    }

    @Test
    fun `generation visits exactly the 92 complemented clauses before repeating`() {
        listOf(0, 1, Int.MAX_VALUE).forEach { seed ->
            val cycle = (0 until 92).map { generate(seed, it) }
            assertEquals(92, cycle.map { it.inspector.canonicalSentenceId }.toSet().size)
            assertTrue(cycle.all { it.targetSkillId == "G004" })
            assertTrue(cycle.all { it.supportSkillIds == listOf("G001", "G002", "G003", "G005", "G006") })
            assertEquals(cycle.first().inspector.canonicalSentenceId, generate(seed, 92).inspector.canonicalSentenceId)
        }
    }

    @Test
    fun `prompt carries three segments and independently shuffled opaque choices`() {
        val exercises = (0 until 92).map { generate(47, it) }
        exercises.forEach { exercise ->
            val prompt = requireNotNull(exercise.prompt.gapPrompt)
            val source = requireNotNull(exercise.inspector.gapSource)
            assertEquals(3, prompt.segments.size)
            assertEquals(listOf("g0", "g1"), prompt.slots.map { it.id })
            assertEquals(listOf("G004", "G006"), prompt.slots.map { it.targetSkillId })
            assertEquals(listOf(GapAssessmentRole.PRIMARY, GapAssessmentRole.SUPPORT), prompt.slots.map { it.assessmentRole })
            assertTrue(prompt.slots.all { it.choices.map(GapChoice::id) == listOf("c0", "c1") })
            assertEquals("da", exercise.prompt.cueLanguage)
            assertEquals("da", exercise.prompt.answerLanguage)
            assertEquals(listOf("g0", "g1"), source.slots.map { it.slotId })
            assertGapSourceMatchesLicensedSentence(exercise, prompt, source)
            assertEquals(
                listOf("Present verb — Primary target", "Indefinite article — Supporting grammar"),
                exercise.introduction.map { it.title },
            )
            assertTrue(exercise.introduction.all { it.bodyLanguage == "da" })
            assertEquals(
                "${source.slots[0].alternativeForm} → ${source.slots[0].expectedForm}",
                exercise.introduction[0].body,
            )
            val noun = PilotContent.nouns.single { it.id == source.slots[1].lexicalId }
            assertEquals("${source.slots[1].expectedForm} ${noun.text}", exercise.introduction[1].body)
            assertEquals("", exercise.prompt.before)
            assertEquals("", exercise.prompt.after)
            assertEquals("", exercise.prompt.punctuation)
            assertTrue(exercise.prompt.choices.isEmpty() && exercise.prompt.blocks.isEmpty())
            assertNull(exercise.lexicalTarget)
            assertNull(exercise.lexicalTargets)
            assertNull(exercise.readingTarget)
        }
        val positions = exercises.map { exercise ->
            requireNotNull(exercise.inspector.gapSource).slots.map { it.expectedChoiceId }
        }.toSet()
        assertEquals(setOf(listOf("c0", "c0"), listOf("c0", "c1"), listOf("c1", "c0"), listOf("c1", "c1")), positions)
    }

    @Test
    fun `fixed examples use only licensed verb and article forms`() {
        listOf(
            "Jeg læser en bog." to ("læse" to "læser"),
            "Jeg har et brev." to ("have" to "har"),
            "Det er et hus." to ("være" to "er"),
        ).forEach { (canonical, verbForms) ->
            val exercise = (0 until 92).map { generate(47, it) }.single { it.inspector.canonicalSentence == canonical }
            val prompt = requireNotNull(exercise.prompt.gapPrompt)
            assertEquals(setOf(verbForms.first, verbForms.second), prompt.slots[0].choices.map { it.text }.toSet())
            val article = canonical.split(' ')[2]
            assertEquals(setOf("en", "et"), prompt.slots[1].choices.map { it.text }.toSet())
            assertEquals(article, requireNotNull(exercise.inspector.gapSource).slots[1].expectedForm)
        }
    }

    @Test
    fun `every clause grades all nine semantic states and thirteen ordered arrays`() {
        (0 until 92).forEach { index ->
            val exercise = generate(47, index)
            val prompt = requireNotNull(exercise.prompt.gapPrompt)
            val sentence = licensedSentence(exercise)
            val noun = governedNoun(sentence)
            val expectedForms = mapOf("g0" to sentence.verb.present, "g1" to noun.gender.article)
            val correct = prompt.slots.associate { slot ->
                slot.id to slot.choices.single { it.text == expectedForms.getValue(slot.id) }.id
            }
            val wrong = prompt.slots.associate { slot ->
                slot.id to slot.choices.single { it.id != correct.getValue(slot.id) }.id
            }
            val states = listOf<String?>(null, "correct", "wrong")
            states.forEach { primary ->
                states.forEach { support ->
                    val selections = buildList {
                        primary?.let { add(GapSelection("g0", if (it == "correct") correct.getValue("g0") else wrong.getValue("g0"))) }
                        support?.let { add(GapSelection("g1", if (it == "correct") correct.getValue("g1") else wrong.getValue("g1"))) }
                    }
                    assertSemanticState(
                        exercise,
                        selections,
                        engine.evaluate(exercise, Answer.Gaps(selections)),
                        primary,
                        support,
                    )
                }
            }
            val ordered = orderedAnswers(prompt)
            assertEquals(13, ordered.size)
            ordered.forEach { selections ->
                val evaluation = engine.evaluate(exercise, Answer.Gaps(selections))
                assertEquals(selections, assertIs<NormalizedAnswer.Gaps>(evaluation.normalizedAnswer).selections)
            }
        }
    }

    @Test
    fun `grading ignores supplied order but preserves raw and normalized order`() {
        val exercise = generate(47, 0)
        val correct = requireNotNull(exercise.inspector.gapSource).slots.map { GapSelection(it.slotId, it.expectedChoiceId) }
        val forward = engine.evaluate(exercise, Answer.Gaps(correct))
        val reversed = engine.evaluate(exercise, Answer.Gaps(correct.reversed()))
        assertEquals(forward.targetStatus, reversed.targetStatus)
        assertEquals(forward.gapResults, reversed.gapResults)
        assertEquals(correct, assertIs<Answer.Gaps>(forward.rawAnswer).selections)
        assertEquals(correct.reversed(), assertIs<Answer.Gaps>(reversed.rawAnswer).selections)
        assertEquals(correct.reversed(), assertIs<NormalizedAnswer.Gaps>(reversed.normalizedAnswer).selections)
    }

    @Test
    fun `malformed and wrong-kind answers conservatively remain unevaluable`() {
        val exercise = generate(47, 0)
        val prompt = requireNotNull(exercise.prompt.gapPrompt)
        val selection = GapSelection("g0", prompt.slots[0].choices[0].id)
        val malformed = listOf<Answer>(
            Answer.Gaps(listOf(selection, selection)),
            Answer.Gaps(listOf(GapSelection("unknown", "c0"))),
            Answer.Gaps(listOf(GapSelection("g0", "unknown"))),
            Answer.Gaps(listOf(selection, GapSelection("g1", "c0"), GapSelection("g0", "c1"))),
            Answer.Text("rå\u00a0e\u0301"),
            Answer.Order(listOf("g0", "c0")),
            Answer.Matching(listOf(MatchingPair("g0", "c0"))),
        )
        malformed.forEach { answer ->
            val result = engine.evaluate(exercise, answer)
            assertEquals(TargetStatus.UNEVALUABLE, result.targetStatus)
            assertEquals(LinguisticStatus.UNKNOWN, result.linguisticStatus)
            assertEquals(ResponseConstraintStatus.NOT_MET, result.responseConstraintStatus)
            assertEquals("", result.completedSentence)
            assertNull(result.meaningSatisfaction)
            assertTrue(requireNotNull(result.gapResults).all { it.targetStatus == TargetStatus.UNEVALUABLE })
        }
    }

    @Test
    fun `seed and index boundaries are deterministic`() {
        listOf(0, 1, Int.MAX_VALUE).forEach { seed ->
            listOf(0, 1, 91, 92, 1_000_000).forEach { index ->
                val first = generate(seed, index)
                val second = generate(seed, index)
                assertEquals(first.id, second.id)
                assertEquals(first.presentation, second.presentation)
                assertEquals(first.prompt, second.prompt)
                assertEquals(first.inspector, second.inspector)
            }
        }
        assertNotEquals(generate(0, 0).id, generate(1, 0).id)
    }

    private fun assertSemanticState(
        exercise: GeneratedExercise,
        selections: List<GapSelection>,
        evaluation: Evaluation,
        primary: String?,
        support: String?,
    ) {
        val expectedPrimary = status(primary)
        assertEquals(expectedPrimary, evaluation.targetStatus)
        val complete = primary != null && support != null
        assertEquals(if (complete) ResponseConstraintStatus.MET else ResponseConstraintStatus.NOT_MET, evaluation.responseConstraintStatus)
        assertEquals(expectedWholeLanguage(primary, support), evaluation.linguisticStatus)
        assertNull(evaluation.meaningSatisfaction)
        assertEquals(LinguisticScope.DANISH_GAPS, evaluation.linguisticScope)
        assertCompletedSentence(exercise, selections, complete, evaluation.completedSentence)
        assertEquals(listOf(primary, support).all { it == "correct" }, evaluation.feedback.startsWith("Good work"))
        assertGapRows(selections, listOf(primary, support), requireNotNull(evaluation.gapResults))
        assertEquals(expectedDiagnosisCodes(primary, support), evaluation.issues.map { it.code }.toSet())
    }

    private fun assertCompletedSentence(
        exercise: GeneratedExercise,
        selections: List<GapSelection>,
        complete: Boolean,
        actual: String?,
    ) {
        val expected = if (complete) completedSentence(exercise, selections) else ""
        assertEquals(expected, actual)
    }

    private fun assertGapRows(
        selections: List<GapSelection>,
        states: List<String?>,
        rows: List<GapResult>,
    ) {
        assertEquals(listOf("g0", "g1"), rows.map { it.slotId })
        val expectedSkills = listOf("G004", "G006")
        val expectedRoles = listOf(GapAssessmentRole.PRIMARY, GapAssessmentRole.SUPPORT)
        states.forEachIndexed { index, state ->
            val row = rows[index]
            val selection = selections.singleOrNull { it.slotId == row.slotId }
            assertEquals(expectedSkills[index], row.targetSkillId)
            assertEquals(expectedRoles[index], row.assessmentRole)
            assertEquals(selection?.choiceId, row.choiceId)
            assertEquals(status(state), row.targetStatus)
            assertEquals(
                if (state == null) ResponseConstraintStatus.NOT_MET else ResponseConstraintStatus.MET,
                row.responseConstraintStatus,
            )
            assertEquals(expectedRowLanguage(state), row.linguisticStatus)
        }
    }

    private fun expectedWholeLanguage(primary: String?, support: String?): LinguisticStatus = when {
        "wrong" in listOf(primary, support) -> LinguisticStatus.KNOWN_ISSUE
        listOf(primary, support).all { it != null } -> LinguisticStatus.ACCEPTABLE
        else -> LinguisticStatus.UNKNOWN
    }

    private fun expectedRowLanguage(state: String?): LinguisticStatus = when (state) {
        null -> LinguisticStatus.UNKNOWN
        "wrong" -> LinguisticStatus.KNOWN_ISSUE
        else -> LinguisticStatus.ACCEPTABLE
    }

    private fun expectedDiagnosisCodes(primary: String?, support: String?): Set<String> =
        listOfNotNull(
            "INFINITIVE_USED".takeIf { primary == "wrong" },
            "ARTICLE_GENDER_MISMATCH".takeIf { support == "wrong" },
        ).toSet()

    private fun assertGapSourceMatchesLicensedSentence(
        exercise: GeneratedExercise,
        prompt: GapPrompt,
        source: GapSourceDetails,
    ) {
        val sentence = licensedSentence(exercise)
        val noun = governedNoun(sentence)
        val expected = listOf(
            SourceOracle("g0", sentence.verb.lexicalId, sentence.verb.present, sentence.verb.lemma),
            SourceOracle(
                "g1",
                noun.id,
                noun.gender.article,
                if (noun.gender.article == "en") "et" else "en",
            ),
        )
        source.slots.zip(expected).forEach { (actual, oracle) ->
            assertEquals(oracle.slotId, actual.slotId)
            assertEquals(oracle.lexicalId, actual.lexicalId)
            assertEquals(oracle.expectedForm, actual.expectedForm)
            assertEquals(oracle.alternativeForm, actual.alternativeForm)
            val promptSlot = prompt.slots.single { it.id == oracle.slotId }
            assertEquals(oracle.expectedForm, promptSlot.choices.single { it.id == actual.expectedChoiceId }.text)
            assertEquals(
                oracle.alternativeForm,
                promptSlot.choices.single { it.id != actual.expectedChoiceId }.text,
            )
        }
    }

    private fun completedSentence(exercise: GeneratedExercise, selections: List<GapSelection>): String {
        val prompt = requireNotNull(exercise.prompt.gapPrompt)
        val selectedBySlot = selections.associate { it.slotId to it.choiceId }
        val selectedForms = prompt.slots.map { slot ->
            slot.choices.single { it.id == selectedBySlot.getValue(slot.id) }.text
        }
        return prompt.segments[0] + selectedForms[0] + prompt.segments[1] + selectedForms[1] + prompt.segments[2]
    }

    private fun licensedSentence(exercise: GeneratedExercise): Sentence =
        PilotContent.sentences.single { it.id == exercise.inspector.canonicalSentenceId }

    private fun governedNoun(sentence: Sentence): NounEntry {
        val nounId = requireNotNull(sentence.complement).lexicalIds.last()
        return PilotContent.nouns.single { it.id == nounId }
    }

    private data class SourceOracle(
        val slotId: String,
        val lexicalId: String,
        val expectedForm: String,
        val alternativeForm: String,
    )

    private fun status(state: String?): TargetStatus = when (state) {
        null -> TargetStatus.UNEVALUABLE
        "correct" -> TargetStatus.MET
        else -> TargetStatus.NOT_MET
    }

    private fun orderedAnswers(prompt: GapPrompt): List<List<GapSelection>> = buildList {
        add(emptyList())
        prompt.slots.forEach { slot -> slot.choices.forEach { add(listOf(GapSelection(slot.id, it.id))) } }
        prompt.slots[0].choices.forEach { first ->
            prompt.slots[1].choices.forEach { second ->
                val selections = listOf(GapSelection("g0", first.id), GapSelection("g1", second.id))
                add(selections)
                add(selections.reversed())
            }
        }
    }

    private fun generate(seed: Int, index: Int) = engine.generate(request(index, seed))

    private fun request(index: Int, seed: Int = 47) =
        GenerationRequest("E015", "G004", seed, index, "pilot-7", "generator-7")
}
