package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModuleOneExercisesTest {
    private val engine = PilotEngine()

    @Test
    fun `pair eight publishes four bounded formats while retaining every older pair`() {
        assertEquals((1..28).map { "E${it.toString().padStart(3, '0')}" }, engine.catalog().formats.map { it.id })
        assertEquals(109, engine.catalog().sentenceCount)
        assertEquals("pilot-14", engine.catalog().contentRevision)
        assertEquals("generator-14", engine.catalog().generatorRevision)
        assertEquals(14, engine.supportedRevisionPairs().size)
        assertEquals("evaluation-7", engine.evaluationRevision("pilot-7", "generator-7"))
        assertEquals("evaluation-8", engine.evaluationRevision("pilot-8", "generator-8"))
        assertEquals(36, PilotContent.currentSnapshot.lexicalReview.size)
        assertEquals(109, PilotContent.currentSnapshot.sentences.size)
        assertEquals(99, PilotContent.pilotSevenSnapshot.sentences.size)
        assertFailsWith<IllegalArgumentException> {
            engine.generate(request("E016", "G003").copy(contentRevision = "pilot-7", generatorRevision = "generator-7"))
        }
    }

    @Test
    fun `authored pronoun tasks pin literal cues forms scopes and changed recall situations`() {
        val expected = listOf(
            Oracle("L001", "jeg", "I", "human-singular"),
            Oracle("L002", "du", "you (one person)", "human-singular"),
            Oracle("L003", "han", "he", "human-singular"),
            Oracle("L004", "hun", "she", "human-singular"),
            Oracle("L005", "vi", "we", "human-group"),
            Oracle("L006", "I", "you (several people)", "human-group"),
            Oracle("L007", "de", "they (several people)", "human-group"),
        )
        listOf("E016", "E017").forEach { formatId ->
            val tasks = ModuleOneContent.tasks(formatId)
            assertEquals(expected.map { it.lexicalId }.toSet(), tasks.map { it.targetLexicalId }.toSet())
            expected.forEach { oracle ->
                val task = tasks.single { it.targetLexicalId == oracle.lexicalId }
                assertEquals(oracle.form, task.expectedForm)
                assertEquals(oracle.cue, task.cue)
                assertEquals(oracle.scope, task.targetScopeId)
                assertTrue(task.context.isNotBlank())
                assertTrue(task.sentenceGloss.isNotBlank())
                assertEquals(ModuleOneContent.CASE_POLICY, task.casePolicy)
            }
        }
        expected.forEach { oracle ->
            val selection = ModuleOneContent.tasks("E016").single { it.targetLexicalId == oracle.lexicalId }
            val recall = ModuleOneContent.tasks("E017").single { it.targetLexicalId == oracle.lexicalId }
            assertNotEquals(selection.context, recall.context, oracle.lexicalId)
            assertNotEquals(selection.id, recall.id)
            assertTrue(selection.choices.isNotEmpty())
            assertTrue(recall.choices.isEmpty())
        }
    }

    @Test
    fun `generation cycles every logical task deterministically and derives scope from the task`() {
        mapOf("E016" to 7, "E017" to 7, "E018" to 8, "E019" to 8).forEach { (formatId, size) ->
            val target = if (formatId in setOf("E016", "E017")) "G003" else "G004"
            listOf(0, 47, Int.MAX_VALUE).forEach { seed ->
                val cycle = (0 until size).map { engine.generate(request(formatId, target, seed, it)) }
                assertEquals(size, cycle.map { requireNotNull(it.inspector.moduleOneSource).taskId }.toSet().size)
                assertEquals(
                    cycle.first().inspector.moduleOneSource?.taskId,
                    engine.generate(request(formatId, target, seed, size)).inspector.moduleOneSource?.taskId,
                )
                cycle.forEach { exercise ->
                    val source = assertNotNull(exercise.inspector.moduleOneSource)
                    assertEquals(source.targetLexicalId, exercise.inspector.lexicalIds.single())
                    assertEquals(source.expectedForm, (exercise.source.answerKey as AnswerKey.ModuleOneForm).expected)
                    assertTrue(exercise.targetScopeId in setOf("human-singular", "human-group", "vaere-present"))
                    assertEquals("en", exercise.prompt.cueLanguage)
                    assertEquals("da", exercise.prompt.answerLanguage)
                    assertTrue(exercise.introduction.isNotEmpty())
                    assertTrue(exercise.hint.isNotBlank())
                }
            }
        }
        val first = engine.generate(request("E016", "G003", 47, 3))
        assertEquals(first.id, engine.generate(request("E016", "G003", 47, 3)).id)
        assertEquals(first.presentation, engine.generate(request("E016", "G003", 47, 3)).presentation)
        assertFailsWith<IllegalArgumentException> { engine.generate(request("E016", "G004")) }
        assertFailsWith<IllegalArgumentException> { engine.generate(request("E018", "G003")) }
    }

    @Test
    fun `selection and recall grade only introduced word forms with conservative unknowns`() {
        val sheChoice = find("E016", "L004")
        assertEquals(setOf("jeg", "du", "han", "hun"), sheChoice.prompt.choices.toSet())
        assertTrue(engine.isSuppliedChoice(sheChoice, " hun "))
        assertFalse(engine.isSuppliedChoice(sheChoice, "vi"))
        assertGrade(sheChoice, "hun", TargetStatus.MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.MET)
        val wrong = grade(sheChoice, "han")
        assertEquals(TargetStatus.NOT_MET, wrong.targetStatus)
        assertEquals(LinguisticStatus.ACCEPTABLE, wrong.linguisticStatus)
        assertEquals("hun", wrong.completedSentence)
        val forged = grade(sheChoice, "vi")
        assertEquals(TargetStatus.UNEVALUABLE, forged.targetStatus)
        assertEquals(LinguisticStatus.UNKNOWN, forged.linguisticStatus)

        val pluralYou = find("E017", "L006")
        val trimmed = grade(pluralYou, " I ")
        assertEquals(TargetStatus.MET, trimmed.targetStatus)
        assertEquals(Answer.Text(" I "), trimmed.rawAnswer)
        assertEquals(NormalizedAnswer.Text("I"), trimmed.normalizedAnswer)
        val lowerI = grade(pluralYou, "i")
        assertEquals(TargetStatus.UNEVALUABLE, lowerI.targetStatus)
        assertEquals(LinguisticStatus.KNOWN_ISSUE, lowerI.linguisticStatus)
        assertEquals(ResponseConstraintStatus.NOT_MET, lowerI.responseConstraintStatus)
        assertEquals(listOf("STANDALONE_PRONOUN_CASE_REQUIRED"), lowerI.issues.map { it.code })
        assertEquals("I", lowerI.completedSentence)

        val they = find("E017", "L007")
        val capitalDe = grade(they, "De")
        assertEquals(TargetStatus.UNEVALUABLE, capitalDe.targetStatus)
        assertEquals("de", capitalDe.completedSentence)
        val multiword = grade(they, "de sover")
        assertEquals(TargetStatus.UNEVALUABLE, multiword.targetStatus)
        assertEquals(LinguisticStatus.KNOWN_ISSUE, multiword.linguisticStatus)
        assertEquals(listOf("ONE_WORD_RESPONSE_REQUIRED"), multiword.issues.map { it.code })
        val unknown = grade(they, "dem")
        assertEquals(TargetStatus.UNEVALUABLE, unknown.targetStatus)
        assertEquals(LinguisticStatus.UNKNOWN, unknown.linguisticStatus)
        assertNull(unknown.completedSentence)
        assertTrue(unknown.feedback.contains("outside the introduced alternatives"))
        val blank = grade(they, "  ")
        assertEquals(TargetStatus.UNEVALUABLE, blank.targetStatus)
        assertEquals(LinguisticStatus.UNKNOWN, blank.linguisticStatus)
        assertEquals(ResponseConstraintStatus.NOT_MET, blank.responseConstraintStatus)
        assertEquals(listOf("EMPTY_RESPONSE"), blank.issues.map { it.code })
        assertNull(blank.completedSentence)
    }

    @Test
    fun `pronoun case deviations preserve bounded reference evidence without wrong-person claims`() {
        val speaker = find("E017", "L001")
        val result = grade(speaker, "Jeg")
        assertEquals(TargetStatus.MET, result.targetStatus)
        assertEquals(LinguisticStatus.KNOWN_ISSUE, result.linguisticStatus)
        assertEquals(ResponseConstraintStatus.NOT_MET, result.responseConstraintStatus)
        assertEquals("jeg", result.completedSentence)
        assertTrue(result.feedback.contains("capitalization"))
        assertTrue("jeg" !in result.feedback)
    }

    @Test
    fun `present form grading keeps normalization narrow and expected Danish in completed sentence`() {
        val recall = findFrame("E019", "letter")
        assertGrade(recall, " er ", TargetStatus.MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.MET)
        val decomposed = grade(recall, "e\u0072")
        assertEquals(TargetStatus.MET, decomposed.targetStatus)
        val infinitive = grade(recall, "være")
        assertEquals(TargetStatus.NOT_MET, infinitive.targetStatus)
        assertEquals(LinguisticStatus.KNOWN_ISSUE, infinitive.linguisticStatus)
        assertEquals("Det er et brev.", infinitive.completedSentence)
        assertTrue("er" !in infinitive.feedback.split(Regex("[^A-Za-zæøåÆØÅ]+")))
        val capitalized = grade(recall, "Er")
        assertEquals(TargetStatus.NOT_MET, capitalized.targetStatus)
        assertEquals(LinguisticStatus.KNOWN_ISSUE, capitalized.linguisticStatus)
        assertEquals(ResponseConstraintStatus.NOT_MET, capitalized.responseConstraintStatus)
        assertEquals(listOf("PRESENT_FORM_CASE_REQUIRED"), capitalized.issues.map { it.code })
        val allCaps = grade(recall, "ER")
        assertEquals(TargetStatus.NOT_MET, allCaps.targetStatus)
        assertEquals(LinguisticStatus.KNOWN_ISSUE, allCaps.linguisticStatus)
        assertEquals("Det er et brev.", allCaps.completedSentence)
        val blank = grade(recall, "")
        assertEquals(ResponseConstraintStatus.NOT_MET, blank.responseConstraintStatus)
        assertEquals(listOf("EMPTY_RESPONSE"), blank.issues.map { it.code })
        assertNull(blank.completedSentence)
        val unknown = grade(recall, "var")
        assertEquals(TargetStatus.UNEVALUABLE, unknown.targetStatus)
        assertEquals(LinguisticStatus.UNKNOWN, unknown.linguisticStatus)
        assertNull(unknown.completedSentence)
        val shape = grade(recall, "vi er")
        assertEquals(listOf("ONE_WORD_RESPONSE_REQUIRED"), shape.issues.map { it.code })
        val mismatch = engine.evaluate(recall, Answer.Order(emptyList()))
        assertEquals(TargetStatus.UNEVALUABLE, mismatch.targetStatus)
        assertEquals(ResponseConstraintStatus.NOT_MET, mismatch.responseConstraintStatus)
        assertEquals(LinguisticScope.DANISH_WORD_FORM, mismatch.linguisticScope)
    }

    @Test
    fun `pair eight catalog has exactly ten separately licensed C004 clauses`() {
        val c004 = PilotContent.currentSnapshot.sentences.filter { it.constructionId == "C004" }
        assertEquals(10, c004.size)
        assertEquals(
            setOf("jeg|Anna", "jeg|Nora", "du|Anna", "du|Nora", "hun|Anna", "hun|Nora"),
            c004.filter { it.complement?.lexicalIds?.isEmpty() == true }
                .map { "${it.subject.text}|${it.complement?.text}" }.toSet(),
        )
        assertEquals(
            setOf("jeg|lærer", "du|lærer", "han|lærer", "hun|lærer"),
            c004.filter { it.complement?.lexicalIds == listOf("L025") }
                .map { "${it.subject.text}|${it.complement?.text}" }.toSet(),
        )
        assertTrue(PilotContent.sentences.none { it.constructionId == "C004" })
        assertEquals(99, PilotContent.sentences.size)
    }

    @Test
    fun `prompt exposure uses explicit identifying noun and article namespaces`() {
        val book = ModuleOneContent.tasks("E018").single { it.frameVariantId == "book" }
        assertEquals(
            setOf(
                "module-one:identifying-form:L008",
                "module-one:headword:L014-S01",
                "module-one:article:L014",
                "module-one:verb-present:L010",
                "module-one:verb-infinitive:L010",
            ),
            book.promptExposureKeys.toSet(),
        )
        assertTrue(book.promptExposureKeys.none { it == "module-one:subject-form:L008" })
        assertTrue(book.promptExposureKeys.none { it.endsWith(":L022") || it.endsWith(":L023") })
    }

    @Test
    fun `pair eight review exposes complete task policy without changing retained review`() {
        val current = PilotReview.renderMarkdown()
        assertTrue("Reference role" in current)
        assertTrue("Case policy" in current)
        assertTrue("Answer exposure" in current)
        assertTrue("Prompt exposure" in current)
        assertTrue("Lesson exposure" in current)
        assertTrue("Hint exposure" in current)
        assertTrue("Feedback exposure" in current)
        assertTrue("standalone-introduced-form-1" in current)
        assertTrue("Introduced forms [da]: jeg · du · han · hun" in current)
        assertTrue("Use the English situation to choose the introduced subject word." in current)
        assertTrue("Type only the introduced subject word using its taught standalone spelling." in current)
        assertTrue("module-one:subject-form:L001" in current)
        val retained = PilotReview.renderPilotSevenMarkdown()
        assertTrue("Module 1 authored task inventory" !in retained)
        assertTrue("ten enumerated C004" !in retained)
    }

    @Test
    fun `recall instruction refers only to the profile taught standalone spelling`() {
        val recalls = (0 until 7).map { index -> engine.generate(request("E017", "G003", 47, index)) }
        assertEquals(
            setOf("Type only the introduced subject word using its taught standalone spelling."),
            recalls.map { exercise -> exercise.instruction }.toSet(),
        )
        assertFalse(recalls.first().instruction.contains("jeg/du/han/hun/vi/de"))
    }

    private fun find(formatId: String, lexicalId: String): GeneratedExercise =
        (0 until 16).asSequence().map {
            val target = if (formatId == "E016" || formatId == "E017") "G003" else "G004"
            engine.generate(request(formatId, target, 47, it))
        }
            .first { it.inspector.moduleOneSource?.targetLexicalId == lexicalId }

    private fun findFrame(formatId: String, prefix: String): GeneratedExercise =
        (0 until 16).asSequence().map { engine.generate(request(formatId, "G004", 47, it)) }
            .first { it.inspector.moduleOneSource?.frameVariantId?.startsWith(prefix) == true }

    private fun grade(exercise: GeneratedExercise, text: String): Evaluation = engine.evaluate(exercise, Answer.Text(text))

    private fun assertGrade(
        exercise: GeneratedExercise,
        text: String,
        target: TargetStatus,
        linguistic: LinguisticStatus,
        constraint: ResponseConstraintStatus,
    ) {
        val result = grade(exercise, text)
        assertEquals(target, result.targetStatus)
        assertEquals(linguistic, result.linguisticStatus)
        assertEquals(constraint, result.responseConstraintStatus)
        assertEquals(LinguisticScope.DANISH_WORD_FORM, result.linguisticScope)
    }

    private fun request(formatId: String, target: String, seed: Int = 0, index: Int = 0) =
        GenerationRequest(formatId, target, seed, index, "pilot-8", "generator-8")

    private data class Oracle(val lexicalId: String, val form: String, val cue: String, val scope: String)
}
