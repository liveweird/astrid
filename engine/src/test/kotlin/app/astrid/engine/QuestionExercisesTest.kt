package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

class QuestionExercisesTest {
    @Test
    fun `pair sixteen adds only the question skill format and support words to pair fifteen`() {
        val retained = PilotContent.pilotFifteenSnapshot
        val current = PilotContent.pilotSixteenSnapshot

        assertEquals(109, retained.sentences.size)
        assertEquals(retained.sentences, current.sentences)
        assertEquals(retained.formats.map(ExerciseFormat::id) + "E030", current.formats.map(ExerciseFormat::id))
        assertEquals(retained.skills.map(Skill::id) + "G018", current.skills.map(Skill::id))
        assertEquals(listOf("G005"), current.skills.single { it.id == "G018" }.prerequisiteIds)
        assertEquals(
            retained.fixedWords.map(FixedWordEntry::id) + listOf("L039", "L040"),
            current.fixedWords.map(FixedWordEntry::id),
        )

        val engine = PilotEngine()
        assertEquals("pilot-19", engine.catalog().contentRevision)
        assertEquals("generator-19", engine.catalog().generatorRevision)
        assertEquals(44, engine.catalog().formats.size)
        assertEquals("evaluation-15", engine.evaluationRevision("pilot-15", "generator-15"))
        assertEquals("evaluation-16", engine.evaluationRevision("pilot-16", "generator-16"))
        val retainedLocation = retained.lexicalReview.single { it.id == "L032" }.constraints
        val currentLocation = current.lexicalReview.single { it.id == "L032" }.constraints
        assertTrue("C007" in retainedLocation && "C010" !in retainedLocation)
        assertTrue("C007" in currentLocation && "C010" in currentLocation)
        val retainedSubject = retained.lexicalReview.single { it.id == "L002" }
        val currentSubject = current.lexicalReview.single { it.id == "L002" }
        assertTrue("C007 circumstance subjects" in retainedSubject.constraints)
        assertFalse("C010" in retainedSubject.constraints)
        assertTrue("C010 neutral-question subjects" in currentSubject.constraints)
        assertTrue("ledstilling-i-ja-nej-spoergsmaal" in currentSubject.sourceUrls.single {
            "ledstilling-i-ja-nej-spoergsmaal" in it
        })
    }

    @Test
    fun `pair sixteen publishes four exact C010 questions with intact constituents`() {
        val exercises = NeutralQuestionContent.tasks.mapIndexed(::exercise)
        assertEquals(
            listOf(
                "Læser du en avis?",
                "Arbejder hun?",
                "Bor I i Aarhus?",
                "Har de et brev?",
            ),
            exercises.map { it.inspector.canonicalSentence },
        )
        assertEquals(
            listOf(
                "Du læser en avis.",
                "Hun arbejder.",
                "I bor i Aarhus.",
                "De har et brev.",
            ),
            NeutralQuestionContent.tasks.map(NeutralQuestionTask::positiveProposition),
        )
        assertEquals(
            listOf(
                "s13-read-count-noun-question",
                "s13-work-question",
                "s13-residence-question",
                "s13-possession-question",
            ),
            exercises.map { requireNotNull(it.inspector.tileSource).licensedOrders.single().kind },
        )
        assertTrue(exercises.all { it.inspector.constructionId == "C010" })
        assertTrue(exercises.all { it.prompt.punctuation == "?" })
        assertEquals(NeutralQuestionContent.tasks.map(NeutralQuestionTask::positiveProposition), exercises.map { it.prompt.cue })
        assertTrue(exercises.all { it.prompt.cueLanguage == "da" })
        assertTrue(exercises.all { exercise ->
            NeutralQuestionContent.tasks.none { it.positiveProposition in exercise.context }
        })
        assertTrue(exercises.all { it.prompt.tileMode == null && it.prompt.requiredTileCount == null })
        assertEquals(
            listOf(
                listOf("FINITE_VERB", "SUBJECT", "OBJECT"),
                listOf("FINITE_VERB", "SUBJECT"),
                listOf("FINITE_VERB", "SUBJECT", "LOCATION_COMPLEMENT"),
                listOf("FINITE_VERB", "SUBJECT", "OBJECT"),
            ),
            exercises.map { it.inspector.clauseRoles },
        )
        assertTrue(exercises.flatMap { it.prompt.blocks }.any { it.text == "I" })
        assertTrue(exercises.flatMap { it.prompt.blocks }.any { it.text == "i Aarhus" })
        assertTrue(exercises.flatMap { it.prompt.blocks }.none { it.text == "i" || it.text == "Aarhus" })
    }

    @Test
    fun `all twenty complete permutations separate target positions from exact requested order`() {
        var permutationCount = 0
        NeutralQuestionContent.tasks.forEachIndexed { index, _ ->
            val exercise = exercise(index)
            val tokens = requireNotNull(exercise.inspector.tileSource).tokens.associateBy(TileTokenDetails::blockId)
            val canonical = requireNotNull(exercise.inspector.tileSource).licensedOrders.single().blockIds
            permutations(canonical).forEach { order ->
                permutationCount += 1
                val answer = Answer.Order(order)
                val result = NeutralQuestionExerciseEvaluator.evaluate(exercise, answer)
                assertSame(answer, result.rawAnswer)
                assertEquals(NormalizedAnswer.Order(order), result.normalizedAnswer)
                if (order == canonical) {
                    assertEquals(TargetStatus.MET, result.targetStatus)
                    assertEquals(LinguisticStatus.ACCEPTABLE, result.linguisticStatus)
                    assertEquals(ResponseConstraintStatus.MET, result.responseConstraintStatus)
                    assertEquals(MeaningSatisfaction.MET, result.meaningSatisfaction)
                    assertTrue(result.issues.isEmpty())
                } else {
                    val forms = order.map { tokens.getValue(it).form }
                    val targetMet = forms.getOrNull(0) == "finite-verb" && forms.getOrNull(1) == "subject"
                    assertEquals(if (targetMet) TargetStatus.MET else TargetStatus.NOT_MET, result.targetStatus)
                    assertEquals(LinguisticStatus.UNKNOWN, result.linguisticStatus)
                    assertEquals(ResponseConstraintStatus.NOT_MET, result.responseConstraintStatus)
                    assertEquals(MeaningSatisfaction.UNKNOWN, result.meaningSatisfaction)
                    assertEquals(listOf("REQUESTED_ORDER_CONSTRAINT"), result.issues.map(EvaluationIssue::code))
                }
            }
        }
        assertEquals(20, permutationCount)
    }

    @Test
    fun `declarative order fails the requested neutral question target without global invalidity claim`() {
        NeutralQuestionContent.tasks.forEachIndexed { index, task ->
            val exercise = exercise(index)
            val tokens = requireNotNull(exercise.inspector.tileSource).tokens
            val byForm = tokens.associateBy(TileTokenDetails::form)
            val declarative = buildList {
                add(byForm.getValue("subject").blockId)
                add(byForm.getValue("finite-verb").blockId)
                tokens.singleOrNull { it.form in setOf("object", "location-complement") }?.let { add(it.blockId) }
            }
            val result = NeutralQuestionExerciseEvaluator.evaluate(exercise, Answer.Order(declarative))
            assertEquals(TargetStatus.NOT_MET, result.targetStatus, task.id)
            assertEquals(LinguisticStatus.UNKNOWN, result.linguisticStatus, task.id)
            assertEquals(ResponseConstraintStatus.NOT_MET, result.responseConstraintStatus, task.id)
            assertTrue("requested neutral question" in result.feedback, task.id)
        }
    }

    @Test
    fun `incomplete and malformed answers stay safe and preserve raw IDs`() {
        val exercise = exercise(0)
        val canonical = requireNotNull(exercise.inspector.tileSource).licensedOrders.single().blockIds
        val cases = listOf(
            Answer.Order(canonical.dropLast(1)) to "REQUIRED_WORDS_MISSING",
            Answer.Order(emptyList()) to "ANSWER_TYPE_MISMATCH",
            Answer.Order(canonical.dropLast(1) + canonical.first()) to "ANSWER_TYPE_MISMATCH",
            Answer.Order(canonical.dropLast(1) + "foreign") to "ANSWER_TYPE_MISMATCH",
            Answer.Text("Læser du en avis") to "ANSWER_TYPE_MISMATCH",
            Answer.Matching(emptyList()) to "ANSWER_TYPE_MISMATCH",
            Answer.Gaps(emptyList()) to "ANSWER_TYPE_MISMATCH",
        )
        cases.forEach { (answer, issueCode) ->
            val result = NeutralQuestionExerciseEvaluator.evaluate(exercise, answer)
            assertSame(answer, result.rawAnswer)
            assertEquals(TargetStatus.UNEVALUABLE, result.targetStatus)
            assertEquals(LinguisticStatus.UNKNOWN, result.linguisticStatus)
            assertEquals(ResponseConstraintStatus.NOT_MET, result.responseConstraintStatus)
            assertEquals(MeaningSatisfaction.UNKNOWN, result.meaningSatisfaction)
            assertEquals(listOf(issueCode), result.issues.map(EvaluationIssue::code))
        }
    }

    @Test
    fun `wrong source and over supplied known IDs remain unevaluable`() {
        val question = exercise(0)
        val tileSource = requireNotNull(question.inspector.tileSource)
        val extraToken = TileTokenDetails("extra", "L040", "support")
        val expanded =
            GeneratedExercise(
                question.id,
                question.formatId,
                question.targetSkillId,
                question.lexicalTarget,
                question.supportSkillIds,
                question.presentation,
                question.contentRevision,
                question.generatorRevision,
                question.reproduction,
                question.inspector.copy(tileSource = tileSource.copy(tokens = tileSource.tokens + extraToken)),
                question.source,
                question.readingTarget,
                question.lexicalTargets,
            )
        val oversized = tileSource.licensedOrders.single().blockIds + extraToken.blockId
        val oversizedResult = NeutralQuestionExerciseEvaluator.evaluate(expanded, Answer.Order(oversized))
        assertEquals(TargetStatus.UNEVALUABLE, oversizedResult.targetStatus)
        assertEquals(listOf("ANSWER_TYPE_MISMATCH"), oversizedResult.issues.map(EvaluationIssue::code))

        val circumstance =
            FinalCircumstanceExerciseGenerator.generate(
                GenerationRequest("E029", "G016", 47, 0, "pilot-15", "generator-15"),
            )
        val wrongSourceResult = NeutralQuestionExerciseEvaluator.evaluate(circumstance, Answer.Order(emptyList()))
        assertEquals(TargetStatus.UNEVALUABLE, wrongSourceResult.targetStatus)
        assertEquals(listOf("ANSWER_TYPE_MISMATCH"), wrongSourceResult.issues.map(EvaluationIssue::code))
    }

    @Test
    fun `seed and index boundaries are deterministic and reach all tasks`() {
        val indices = listOf(0, 3, 4, 1_000_000, Int.MAX_VALUE)
        listOf(0, 3, 4, 1_000_000, Int.MAX_VALUE).forEach { seed ->
            val first = indices.map { index -> generated(seed, index) }
            val second = indices.map { index -> generated(seed, index) }
            assertEquals(first.map(GeneratedExercise::id), second.map(GeneratedExercise::id))
        }
        assertEquals(
            NeutralQuestionContent.tasks.map(NeutralQuestionTask::id).toSet(),
            (0 until 4).map { (generated(0, it).source as QuestionExerciseSource).task.id }.toSet(),
        )
    }

    @Test
    fun `grading rejects private key or licensed inspector drift`() {
        val exercise = exercise(0)
        val canonical = requireNotNull(exercise.inspector.tileSource).licensedOrders.single().blockIds
        val source = exercise.source as QuestionExerciseSource
        val key = source.answerKey as AnswerKey.TileOrder
        val privateDrift = copy(
            exercise,
            source = source.copy(answerKey = key.copy(canonical = key.canonical.reversed())),
        )
        val licensed = requireNotNull(exercise.inspector.tileSource)
        val inspectorDrift = copy(
            exercise,
            inspector = exercise.inspector.copy(
                tileSource = licensed.copy(
                    licensedOrders = listOf(licensed.licensedOrders.single().copy(blockIds = canonical.reversed())),
                ),
            ),
        )
        listOf(privateDrift, inspectorDrift).forEach { changed ->
            val failure = assertFailsWith<IllegalArgumentException> {
                NeutralQuestionExerciseEvaluator.evaluate(changed, Answer.Order(canonical))
            }
            assertEquals("Private question answer key does not match the licensed inspector order", failure.message)
        }
    }

    @Test
    fun `short answers are support only and structural feedback releases pattern plus exact order`() {
        NeutralQuestionContent.tasks.forEach { task ->
            assertEquals(
                listOf(NeutralQuestionContent.QUESTION_PATTERN_KEY, NeutralQuestionContent.exactOrderKey(task.id)),
                task.answerExposureKeys,
            )
            assertTrue(task.promptExposureKeys.none(NeutralQuestionContent.SHORT_ANSWER_SUPPORT_KEYS::contains))
            assertTrue(task.answerExposureKeys.none(NeutralQuestionContent.SHORT_ANSWER_SUPPORT_KEYS::contains))
            assertTrue(task.lessonExposureKeys.containsAll(NeutralQuestionContent.SHORT_ANSWER_SUPPORT_KEYS))
            assertEquals(
                listOf(NeutralQuestionContent.exactOrderKey(task.id), NeutralQuestionContent.QUESTION_PATTERN_KEY),
                task.feedbackExposureKeys,
            )
        }
    }

    private fun exercise(index: Int, task: NeutralQuestionTask = NeutralQuestionContent.tasks[index]) =
        NeutralQuestionExerciseGenerator.generate(request(47, index), task)

    private fun generated(seed: Int, index: Int) = NeutralQuestionExerciseGenerator.generate(request(seed, index))

    private fun copy(
        exercise: GeneratedExercise,
        inspector: ExerciseInspector = exercise.inspector,
        source: ExerciseSource = exercise.source,
    ) = GeneratedExercise(
        exercise.id,
        exercise.formatId,
        exercise.targetSkillId,
        exercise.lexicalTarget,
        exercise.supportSkillIds,
        exercise.presentation,
        exercise.contentRevision,
        exercise.generatorRevision,
        exercise.reproduction,
        inspector,
        source,
        exercise.readingTarget,
        exercise.lexicalTargets,
    )

    private fun request(seed: Int, index: Int) =
        GenerationRequest("E030", "G018", seed, index, "pilot-16", "generator-16")

    private fun <T> permutations(values: List<T>): List<List<T>> =
        if (values.size <= 1) {
            listOf(values)
        } else {
            values.flatMap { value -> permutations(values - value).map { listOf(value) + it } }
        }
}
