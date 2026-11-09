package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ModuleOnePracticeTest {
    private val planner = PracticePlanner()

    @Test
    fun `three literal profiles each freeze six pair eight items and required policies`() {
        val expected = listOf(
            Triple("module-one-singular", "module-one-singular-1", "Talking about one person"),
            Triple("module-one-groups", "module-one-groups-1", "Talking to and about groups"),
            Triple("module-one-er", "module-one-er-1", "Using the verb to be"),
        )
        assertEquals(expected.map { it.first }, planner.profiles().takeLast(3).map { it.focusId })
        expected.forEach { (focus, revision, title) ->
            val plan = planner.plan(focus, revision, 47)
            assertEquals(title, plan.title)
            assertEquals(6, plan.items.size)
            assertEquals("pilot-8", plan.contentRevision)
            assertEquals("generator-8", plan.generatorRevision)
            assertEquals("evaluation-8", plan.evaluationRevision)
            assertTrue(plan.introductionRequired)
            assertEquals("module-one-known-outcome-1", plan.completionRuleId)
            assertEquals("module-one-exposure-1", plan.exposurePolicyId)
            assertTrue(plan.introduction.any { it.bodyLanguage == "da" })
            assertTrue(plan.introduction.filter { it.bodyLanguage == null }.all { it.body.isNotBlank() })
            assertTrue(plan.items.all { it.exercise.introduction.isNotEmpty() && it.exercise.hint.isNotBlank() })
            assertTrue(plan.items.all { it.targetDescriptor.targetScopeId == it.exercise.targetScopeId })
            assertTrue(plan.items.all { it.targetDescriptor.targetKey().split(':').size == 4 })
        }
    }

    @Test
    fun `every direct entry carries the same bounded word role and character foundation`() {
        listOf(
            "module-one-singular" to "module-one-singular-1",
            "module-one-groups" to "module-one-groups-1",
            "module-one-er" to "module-one-er-1",
        ).forEach { (focusId, revision) ->
            val introduction = planner.plan(focusId, revision, 0).introduction
            assertEquals("æ · ø · å", introduction.single { it.title == "Danish letters" }.body)
            assertEquals("da", introduction.single { it.title == "Danish letters" }.bodyLanguage)
            assertEquals("Jeg er Anna.", introduction.single { it.title == "A supplied statement" }.body)
            assertEquals("da", introduction.single { it.title == "A supplied statement" }.bodyLanguage)
            assertTrue(introduction.single { it.title == "Foundation for this profile" }.body.contains("not complete coverage"))
            assertTrue(introduction.single { it.title == "Words and complete statements" }.body.contains("separated"))
            val roles = introduction.single { it.title == "Roles in the supplied statement" }.body
            assertTrue("pronoun functioning as the subject" in roles)
            assertTrue("present verb" in roles)
            assertTrue("rather than an object" in roles)
            assertTrue(introduction.single { it.title == "Character entry" }.body.contains("not assessed"))
        }
        val erIntroduction = planner.plan("module-one-er", "module-one-er-1", 0).introduction
        assertTrue(erIntroduction.single { it.title == "Articles and nouns" }.body.contains("first written word is the article"))
        assertTrue(erIntroduction.single { it.title == "Present-form answer casing" }.body.contains("lowercase exactly"))
    }

    @Test
    fun `pair eight review includes all mandatory profile introductions and policies`() {
        val review = PilotReview.renderMarkdown()
        assertEquals(1, review.windowed("## Module 1 mandatory profile introductions".length).count {
            it == "## Module 1 mandatory profile introductions"
        })
        listOf(
            "module-one-singular" to "module-one-singular-1",
            "module-one-groups" to "module-one-groups-1",
            "module-one-er" to "module-one-er-1",
        ).forEach { (focusId, planRevision) ->
            val plan = planner.plan(focusId, planRevision, 47)
            assertTrue("| $focusId | $planRevision | ${plan.title} |" in review)
            assertTrue(plan.prerequisiteSkillIds.joinToString(", ") in review)
            assertTrue(requireNotNull(plan.completionRuleId) in review)
            assertTrue(requireNotNull(plan.exposurePolicyId) in review)
            requireNotNull(plan.introductionExposureKeys).forEach { key -> assertTrue(key in review) }
            plan.introduction.forEach { section ->
                assertTrue("${section.title} [${section.bodyLanguage ?: "en"}]: ${section.body}" in review)
            }
        }
        assertTrue("Danish letters [da]: æ · ø · å" in review)
        assertTrue("A supplied statement [da]: Jeg er Anna." in review)
        assertTrue("Standalone capitalization [en]:" in review)
        assertTrue("Articles and nouns [en]: In each noun bundle shown below" in review)
        assertTrue("Noun bundles [da]: en bog · et brev · en lærer" in review)
        assertTrue("E016:human-singular, E017:human-singular" in review)
        assertTrue("E016:human-group, E017:human-group" in review)
        assertTrue("E018:vaere-present, E019:vaere-present" in review)

        val retained = PilotReview.renderPilotSevenMarkdown()
        assertTrue("Module 1 mandatory profile introductions" !in retained)
    }

    @Test
    fun `singular plan selects all four roles then recalls two distinct roles in changed situations`() {
        listOf(0, 1, 47, Int.MAX_VALUE).forEach { seed ->
            val plan = planner.plan("module-one-singular", "module-one-singular-1", seed)
            assertEquals(listOf("E016", "E016", "E016", "E016", "E017", "E017"), plan.items.map { it.exercise.formatId })
            assertEquals(setOf("L001", "L002", "L003", "L004"), plan.items.take(4).map(::lexicalId).toSet())
            assertEquals(2, plan.items.takeLast(2).map(::lexicalId).toSet().size)
            plan.items.takeLast(2).forEach { recall ->
                val selection = plan.items.take(4).single { lexicalId(it) == lexicalId(recall) }
                assertNotEquals(selection.exercise.context, recall.exercise.context)
            }
        }
    }

    @Test
    fun `group plan checks all three roles once in each operation and includes singular contrast`() {
        val plan = planner.plan("module-one-groups", "module-one-groups-1", 47)
        assertEquals(listOf("E016", "E016", "E016", "E017", "E017", "E017"), plan.items.map { it.exercise.formatId })
        assertEquals(setOf("L005", "L006", "L007"), plan.items.take(3).map(::lexicalId).toSet())
        assertEquals(setOf("L005", "L006", "L007"), plan.items.takeLast(3).map(::lexicalId).toSet())
        assertTrue(plan.items.take(3).all { "du" in it.exercise.prompt.choices })
        assertTrue(requireNotNull(plan.introductionExposureKeys).contains("module-one:subject-form:L002"))
    }

    @Test
    fun `er plan has two selections two fixed recalls and two varying occupational recalls`() {
        val plans = (0..3).map { planner.plan("module-one-er", "module-one-er-1", it) }
        plans.forEach { plan ->
            assertEquals(listOf("E018", "E018", "E019", "E019", "E019", "E019"), plan.items.map { it.exercise.formatId })
            assertTrue(frame(plan.items[0]) in setOf("book", "letter"))
            assertTrue(frame(plan.items[1]).startsWith("identity-"))
            assertEquals(setOf("book", "letter"), plan.items.slice(2..3).map(::frame).toSet())
            assertEquals(2, plan.items.takeLast(2).map(::frame).toSet().size)
            assertTrue(plan.items.takeLast(2).all { frame(it).startsWith("teacher-") })
            assertTrue(plan.items.all { it.targetDescriptor.targetKey().startsWith("grammar:G004:vaere-present:") })
        }
        val observedOccupations = plans.flatMap { it.items.takeLast(2).map(::frame) }.toSet()
        assertTrue(observedOccupations.containsAll(listOf("teacher-jeg", "teacher-du", "teacher-han", "teacher-hun")))
    }

    @Test
    fun `completion policy uses only latest bounded outcomes and fails closed`() {
        val eligible = observation(TargetStatus.NOT_MET, LinguisticStatus.ACCEPTABLE)
        val unknown = observation(TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN)
        assertTrue(PracticeCompletionPolicy.canAdvance(null, eligible))
        assertFalse(PracticeCompletionPolicy.canAdvance(null, null))
        assertTrue(PracticeCompletionPolicy.canAdvance("module-one-known-outcome-1", eligible))
        assertFalse(PracticeCompletionPolicy.canAdvance("module-one-known-outcome-1", unknown))
        assertFalse(
            PracticeCompletionPolicy.canAdvance(
                "module-one-known-outcome-1",
                unknown.copy(
                    responseConstraintStatus = ResponseConstraintStatus.NOT_MET,
                    issueCodes = listOf("EMPTY_RESPONSE"),
                ),
            ),
        )
        assertTrue(
            PracticeCompletionPolicy.canAdvance(
                "module-one-known-outcome-1",
                observation(TargetStatus.NOT_MET, LinguisticStatus.KNOWN_ISSUE).copy(
                    responseConstraintStatus = ResponseConstraintStatus.NOT_MET,
                    issueCodes = listOf("PRESENT_FORM_CASE_REQUIRED"),
                ),
            ),
        )
        assertTrue(
            PracticeCompletionPolicy.canAdvance(
                "module-one-known-outcome-1",
                unknown.copy(
                    linguisticStatus = LinguisticStatus.KNOWN_ISSUE,
                    responseConstraintStatus = ResponseConstraintStatus.NOT_MET,
                    issueCodes = listOf("ONE_WORD_RESPONSE_REQUIRED"),
                ),
            ),
        )
        assertFalse(
            PracticeCompletionPolicy.canAdvance(
                "module-one-known-outcome-1",
                unknown.copy(responseConstraintStatus = ResponseConstraintStatus.NOT_MET, issueCodes = listOf("EMPTY_RESPONSE")),
            ),
        )
        assertFalse(PracticeCompletionPolicy.canAdvance("unknown-rule", eligible))
    }

    @Test
    fun `module one exposure releases accounted feedback and introduction keys only`() {
        val item = planner.plan("module-one-groups", "module-one-groups-1", 0).items.first { lexicalId(it) == "L006" }
        val unknown = PracticeFeedbackAttemptObservation(TargetStatus.UNEVALUABLE, linguisticStatus = LinguisticStatus.UNKNOWN)
        assertEquals(
            emptyList(),
            PracticeFeedbackExposureCalculator.releasedKeys(
                item.feedbackExposureKeys,
                item.assessments,
                listOf(unknown),
                "module-one-exposure-1",
            ),
        )
        assertEquals(
            item.feedbackExposureKeys,
            PracticeFeedbackExposureCalculator.releasedKeys(
                item.feedbackExposureKeys,
                item.assessments,
                listOf(
                    unknown.copy(
                        linguisticStatus = LinguisticStatus.KNOWN_ISSUE,
                        responseConstraintStatus = ResponseConstraintStatus.NOT_MET,
                        issueCodes = listOf("STANDALONE_PRONOUN_CASE_REQUIRED"),
                    ),
                ),
                "module-one-exposure-1",
            ),
        )
        assertEquals(
            item.feedbackExposureKeys,
            PracticeFeedbackExposureCalculator.releasedKeys(
                item.feedbackExposureKeys,
                item.assessments,
                listOf(
                    PracticeFeedbackAttemptObservation(
                        TargetStatus.NOT_MET,
                        linguisticStatus = LinguisticStatus.KNOWN_ISSUE,
                        responseConstraintStatus = ResponseConstraintStatus.NOT_MET,
                        issueCodes = listOf("PRESENT_FORM_CASE_REQUIRED"),
                    ),
                ),
                "module-one-exposure-1",
            ),
        )
        val observation = PracticeItemExposureObservation(item, true, false, false, false)
        val none = PracticeAttemptContextCalculator.calculate(item, true, listOf(observation), introductionExposureKeys = emptyList())
        assertTrue(none.priorExposure.isEmpty())
        val context = PracticeAttemptContextCalculator.calculate(
            item,
            true,
            listOf(observation),
            introductionExposureKeys = listOf(item.answerExposureKeys.single(), "unrelated"),
        )
        assertEquals(PracticeExposureSource.INTRODUCTION, context.priorExposure.single().source)
        assertEquals(-1, context.priorExposure.single().position)
        assertEquals(item.answerExposureKeys, context.priorExposure.single().keys)
        assertNull(item.exercise.lexicalTarget)
    }

    @Test
    fun `exposure metadata names only forms rendered by each surface`() {
        val groups = planner.plan("module-one-groups", "module-one-groups-1", 0)
        assertEquals(
            setOf(
                "module-one:subject-form:L001",
                "module-one:verb-present:L010",
                "module-one:subject-form:L002",
                "module-one:subject-form:L005",
                "module-one:subject-form:L006",
                "module-one:subject-form:L007",
                "module-one:verb-present:L013",
            ),
            requireNotNull(groups.introductionExposureKeys).toSet(),
        )
        val groupRecall = groups.items.first { it.exercise.formatId == "E017" && lexicalId(it) == "L005" }
        assertEquals(listOf("module-one:subject-form:L005"), groupRecall.answerExposureKeys)
        assertEquals(listOf("module-one:verb-present:L013"), groupRecall.promptExposureKeys)
        assertEquals(
            setOf(
                "module-one:subject-form:L002",
                "module-one:subject-form:L005",
                "module-one:subject-form:L006",
                "module-one:subject-form:L007",
            ),
            groupRecall.lessonExposureKeys.toSet(),
        )
        assertTrue(groupRecall.hintExposureKeys.isEmpty())

        val erPlan = planner.plan("module-one-er", "module-one-er-1", 0)
        assertEquals(
            setOf(
                "module-one:subject-form:L001",
                "module-one:verb-present:L010",
                "module-one:verb-infinitive:L010",
                "module-one:identifying-form:L008",
                "module-one:headword:L014-S01",
                "module-one:article:L014",
                "module-one:headword:L018-S01",
                "module-one:article:L018",
                "module-one:headword:L025-S01",
                "module-one:article:L025",
            ),
            requireNotNull(erPlan.introductionExposureKeys).toSet(),
        )
        assertTrue(requireNotNull(erPlan.introductionExposureKeys).none { it in setOf(
            "module-one:subject-form:L002",
            "module-one:subject-form:L003",
            "module-one:subject-form:L004",
        ) })
        val bookRecall = erPlan.items.single { it.exercise.formatId == "E019" && frame(it) == "book" }
        assertEquals(
            setOf(
                "module-one:identifying-form:L008",
                "module-one:headword:L014-S01",
                "module-one:article:L014",
            ),
            bookRecall.promptExposureKeys.toSet(),
        )
        assertEquals(
            setOf("module-one:verb-present:L010", "module-one:verb-infinitive:L010"),
            bookRecall.lessonExposureKeys.toSet(),
        )
        assertTrue(bookRecall.hintExposureKeys.isEmpty())
    }

    @Test
    fun `scoped target keys reject crossed or incomplete identities`() {
        assertEquals(
            "grammar:G003:human-singular:recall",
            PracticeTargetKeys.key("G003", null, "recall", "human-singular"),
        )
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            PracticeTargetKeys.key("G003", null, "repair", "human-singular")
        }
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            PracticeTargetKeys.key("G004", null, "recall", "human-group")
        }
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            PracticeTargetKeys.key("G003", null, null, "human-singular")
        }
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            PracticeTargetKeys.key("G004", null, "repair", "vaere-present")
        }
        assertEquals("grammar:G004:repair", PracticeTargetKeys.key("G004", null, "repair"))
    }

    private fun lexicalId(item: PracticePlanItem): String = requireNotNull(item.exercise.inspector.moduleOneSource).targetLexicalId

    private fun frame(item: PracticePlanItem): String = requireNotNull(item.exercise.inspector.moduleOneSource?.frameVariantId)

    private fun observation(target: TargetStatus, linguistic: LinguisticStatus) =
        PracticeCompletionAttemptObservation(target, linguistic, ResponseConstraintStatus.MET, emptyList())
}
