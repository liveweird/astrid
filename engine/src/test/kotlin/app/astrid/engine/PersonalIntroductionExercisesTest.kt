package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PersonalIntroductionExercisesTest {
    private val engine = PilotEngine()

    @Test
    fun `pair twelve publishes literal personal cards and keeps earlier task pools bounded`() {
        assertEquals("pilot-20", engine.catalog().contentRevision)
        assertEquals("generator-20", engine.catalog().generatorRevision)
        assertEquals("evaluation-11", engine.evaluationRevision("pilot-11", "generator-11"))
        assertEquals("evaluation-12", engine.evaluationRevision("pilot-12", "generator-12"))
        assertEquals(109, engine.catalog().sentenceCount)
        assertEquals(3, PresentOrderContent.tasks("E022").size)
        assertEquals(5, PresentOrderContent.tasks("E022", includePossession = true).size)
        assertEquals(8, PresentOrderContent.tasks("E022", includePossession = true, includePersonal = true).size)

        val exercises = PERSONAL_TASK_IDS.map { taskId -> pairTwelveExercise(taskId) }
        assertEquals(
            listOf(
                "Jeg hedder Anna.", "Du hedder Nora.", "Hun bor i Aarhus.",
                "De bor i Odense.", "Vi taler dansk.", "I taler dansk.",
            ),
            exercises.map { it.inspector.canonicalSentence },
        )
        assertEquals(listOf("C004", "C004", "C006", "C006", "C002", "C002"), exercises.map {
            it.inspector.constructionId
        })
        assertEquals(
            listOf(
                "PREDICATIVE", "PREDICATIVE", "LOCATION_COMPLEMENT", "LOCATION_COMPLEMENT",
                "LANGUAGE_COMPLEMENT", "LANGUAGE_COMPLEMENT",
            ),
            exercises.map { it.inspector.clauseRoles.last() },
        )
        assertEquals(
            listOf(
                listOf("L001", "L028"), listOf("L002", "L028"),
                listOf("L004", "L029", "L032"), listOf("L007", "L029", "L032"),
                listOf("L005", "L030", "L031"), listOf("L006", "L030", "L031"),
            ),
            exercises.map { it.inspector.lexicalIds },
        )
        assertEquals(
            listOf("at hedde", "at hedde", "at bo", "at bo", "at tale", "at tale"),
            exercises.map { it.prompt.cue },
        )
        assertTrue(exercises.all { it.targetScopeId == "personal-introduction-present" })
        assertTrue(exercises.all { it.targetSkillId == "G004" })
        assertTrue(exercises.all { it.supportSkillIds == listOf("G001", "G002", "G003", "G005") })
        assertTrue(exercises.all { it.inspector.moduleOneSource?.frameVariantId == it.inspector.canonicalSentenceId })
        assertEquals(
            setOf("named-three-verbs-present", "have-possession-present", "personal-introduction-present"),
            listOf("E022", "E023").flatMap(::pairTwelveExercises).mapNotNull { it.targetScopeId }.toSet(),
        )
        assertEquals(5, pairElevenExercises("E022").size)
        assertFalse(pairElevenExercises("E022").any { it.targetScopeId == "personal-introduction-present" })
    }

    @Test
    fun `personal cards expose exact proposition teaching and fixed lexical components`() {
        val selection = listOf(
            pairTwelveExercise("personal-select-name"),
            pairTwelveExercise("personal-select-residence"),
            pairTwelveExercise("personal-select-language"),
        )
        assertEquals(
            listOf(
                "English: I am called Anna. The subject refers to the speaker. The supplied name Anna means the name Anna.",
                "English: She lives in Aarhus. The subject refers to one stated woman. " +
                    "The supplied location i Aarhus means in Aarhus.",
                "English: We speak Danish. The subject refers to the speaker and another person. " +
                    "The supplied language dansk means Danish.",
            ),
            selection.map { it.context },
        )
        assertEquals(
            listOf("Hun hedder Nora.", "Jeg bor i Odense.", "Du taler dansk."),
            selection.map { exercise -> exercise.introduction.single { it.title == "Worked example" }.body },
        )
        assertEquals(
            listOf(emptyList(), listOf(PresentOrderContent.componentKey("L032")), listOf(PresentOrderContent.componentKey("L031"))),
            selection.map { exercise ->
                val source = exercise.source as PresentOrderExerciseSource
                source.task.promptExposureKeys.filter { it.startsWith("module-one:supplied-component:") }
            },
        )
        selection.forEach { exercise ->
            val source = requireNotNull(exercise.inspector.moduleOneSource)
            assertTrue("er" in source.knownAlternativeForms)
            assertTrue("har" in source.knownAlternativeForms)
            assertTrue(PresentOrderContent.PERSONAL_PATTERN_KEY in (exercise.source as PresentOrderExerciseSource).task.answerExposureKeys)
        }
    }

    @Test
    fun `personal grading keeps exact known mismatch case shape and unknown boundaries`() {
        val exercise = pairTwelveExercise("personal-select-name")
        val cases = listOf(
            "hedder" to Expected(TargetStatus.MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.MET, null, "hedder"),
            "hedde" to Expected(
                TargetStatus.NOT_MET, LinguisticStatus.KNOWN_ISSUE, ResponseConstraintStatus.MET,
                "INFINITIVE_USED", "hedder",
            ),
            "bor" to Expected(
                TargetStatus.NOT_MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.MET,
                "NAMED_VERB_MISMATCH", "hedder",
            ),
            "har" to Expected(
                TargetStatus.NOT_MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.MET,
                "NAMED_VERB_MISMATCH", "hedder",
            ),
            "er" to Expected(
                TargetStatus.NOT_MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.MET,
                "NAMED_VERB_MISMATCH", "hedder",
            ),
            "Hedder" to Expected(
                TargetStatus.MET, LinguisticStatus.KNOWN_ISSUE, ResponseConstraintStatus.NOT_MET,
                "MID_SENTENCE_VERB_CASE_REQUIRED", "hedder",
            ),
            "hedder nu" to Expected(
                TargetStatus.UNEVALUABLE, LinguisticStatus.KNOWN_ISSUE, ResponseConstraintStatus.NOT_MET,
                "ONE_WORD_RESPONSE_REQUIRED", "hedder",
            ),
            "hed" to Expected(
                TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN, ResponseConstraintStatus.MET,
                "UNRECOGNIZED_PRESENT_FORM", null,
            ),
            "hedder." to Expected(
                TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN, ResponseConstraintStatus.MET,
                "UNRECOGNIZED_PRESENT_FORM", null,
            ),
            "" to Expected(TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN, ResponseConstraintStatus.NOT_MET, "EMPTY_RESPONSE", null),
        )
        cases.forEach { (answer, expected) ->
            val result = engine.evaluate(exercise, Answer.Text(answer))
            assertEquals(expected.target, result.targetStatus, answer)
            assertEquals(expected.linguistic, result.linguisticStatus, answer)
            assertEquals(expected.constraint, result.responseConstraintStatus, answer)
            assertEquals(expected.issue, result.issues.singleOrNull()?.code, answer)
            assertEquals(expected.correction, result.completedSentence, answer)
            assertEquals(Answer.Text(answer), result.rawAnswer, answer)
            assertNull(result.meaningSatisfaction, answer)
        }
        val wrongKind = engine.evaluate(exercise, Answer.Order(emptyList()))
        assertEquals("ANSWER_TYPE_MISMATCH", wrongKind.issues.single().code)
        assertEquals(Answer.Order(emptyList()), wrongKind.rawAnswer)
    }

    @Test
    fun `pair twelve selection choice parity varies by seed independently for every task`() {
        val tasks = PresentOrderContent.tasks("E022", includePossession = true, includePersonal = true)
        tasks.forEach { task ->
            val positions = (0..3).map { seed ->
                val exercise = pairTwelveExercise(task.id, seed)
                exercise.prompt.choices.indexOf((task as NamedPresentTask).verb.present)
            }.toSet()
            assertEquals(setOf(0, 1), positions, task.id)
        }
        (0..3).forEach { seed ->
            assertEquals(
                2,
                tasks.map { task ->
                    pairTwelveExercise(task.id, seed).prompt.choices.indexOf((task as NamedPresentTask).verb.present)
                }.toSet().size,
                "seed=$seed",
            )
        }
    }

    @Test
    fun `pair twelve lexical inventory adds only the five reviewed personal entries`() {
        assertEquals(
            (1..32).map { "L${it.toString().padStart(3, '0')}" }.toSet(),
            PilotContent.pilotTwelveSnapshot.lexicalReview.map { it.id }.toSet(),
        )
        assertEquals(32, PilotContent.pilotTwelveSnapshot.lexicalReview.size)
        assertEquals(listOf("L028", "L029", "L030"), PilotContent.personalIntroductionVerbs.map { it.lexicalId })
        assertEquals(listOf("L031", "L032"), PilotContent.pilotTwelveSnapshot.fixedWords.map { it.id })
        assertEquals(109, PilotContent.currentSnapshot.sentences.size)
        assertEquals(109, PilotContent.pilotElevenSnapshot.sentences.size)
    }

    private fun pairTwelveExercises(formatId: String) =
        PresentOrderContent.tasks(formatId, includePossession = true, includePersonal = true).map { task ->
            pairTwelveExercise(task.id)
        }

    private fun pairElevenExercises(formatId: String) =
        PresentOrderContent.tasks(formatId, includePossession = true).map { task ->
            findExercise(formatId, task.id, 47, "pilot-11", "generator-11", 5)
        }

    private fun pairTwelveExercise(taskId: String, seed: Int = 47): GeneratedExercise {
        val formatId = if ("-select-" in taskId) "E022" else "E023"
        return findExercise(formatId, taskId, seed, "pilot-12", "generator-12", 8)
    }

    private fun findExercise(
        formatId: String,
        taskId: String,
        seed: Int,
        contentRevision: String,
        generatorRevision: String,
        poolSize: Int,
    ) = (0 until poolSize).asSequence().map { index ->
        engine.generate(GenerationRequest(formatId, "G004", seed, index, contentRevision, generatorRevision))
    }.first { (it.source as PresentOrderExerciseSource).task.id == taskId }

    private data class Expected(
        val target: TargetStatus,
        val linguistic: LinguisticStatus,
        val constraint: ResponseConstraintStatus,
        val issue: String?,
        val correction: String?,
    )

    private companion object {
        val PERSONAL_TASK_IDS = listOf(
            "personal-select-name", "personal-recall-name",
            "personal-select-residence", "personal-recall-residence",
            "personal-select-language", "personal-recall-language",
        )
    }
}
