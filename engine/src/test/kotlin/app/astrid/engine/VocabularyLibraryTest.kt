package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class VocabularyLibraryTest {
    @Test
    fun `catalog accounts for all entries with stable sense-scoped forms`() {
        val catalog = VocabularyLibrary.catalog()

        assertEquals(VocabularyLibrary.REVISION, catalog.libraryRevision)
        assertEquals("pilot-20", catalog.contentRevision)
        assertEquals((1..79).map { "L${it.toString().padStart(3, '0')}" }, catalog.items.map { it.entryId })
        assertEquals(79, catalog.items.map { it.entryId }.distinct().size)
        catalog.items.flatMap { it.senses }.forEach { sense ->
            assertTrue(sense.activeForms.all { it.formId.startsWith("${sense.senseId}-F") })
            assertEquals(sense.activeForms.size, sense.activeForms.map { it.formId }.distinct().size)
            assertEquals(PilotReviewMetadata.REVIEW_STATE, sense.reviewState)
            assertTrue(sense.examples.isNotEmpty() || sense.exampleNote != null)
            assertFalse(sense.restrictions.any { INTERNAL_LABEL.containsMatchIn(it) })
        }
    }

    @Test
    fun `request reference and spatial uses remain separate and bounded`() {
        val entries = VocabularyLibrary.catalog().items.associateBy { it.entryId }
        val have = entries.getValue("L009").senses.associateBy { it.senseId }
        assertEquals(listOf("have", "har"), have.getValue("L009-S01").activeForms.map { it.text })
        assertEquals(listOf("have"), have.getValue("L009-S02").activeForms.map { it.text })
        assertFalse(have.getValue("L009-S02").activeForms.any { it.text == "har" })

        val det = entries.getValue("L008").senses.associateBy { it.senseId }
        assertEquals(listOf("Det er"), det.getValue("L008-S01").activeForms.map { it.text })
        assertEquals(listOf("det"), det.getValue("L008-S02").activeForms.map { it.text })
        assertEquals(listOf("i Aarhus", "i Odense"), entries.getValue("L032").senses.single().activeForms.map { it.text })
        assertEquals(listOf("hjemme"), entries.getValue("L048").senses.single().activeForms.map { it.text })
        assertEquals(listOf("hjem"), entries.getValue("L069").senses.single().activeForms.map { it.text })
        assertEquals("til stationen", entries.getValue("L068").senses.single().activeForms.single().text)
        assertEquals("på stationen", entries.getValue("L076").senses.single().activeForms.single().text)
        assertEquals("til apoteket", entries.getValue("L079").senses.single().activeForms.single().text)
    }

    @Test
    fun `vehicular and rail examples retain their selecting scenes`() {
        val senses = VocabularyLibrary.catalog().items.flatMap { it.senses }.associateBy { it.senseId }
        val driving = senses.getValue("L074-S01").examples.single()
        val rail = senses.getValue("L077-S01").examples.single()

        assertEquals("Nora kører hjem.", driving.danish)
        assertTrue(requireNotNull(driving.scene).contains("car", ignoreCase = true))
        assertEquals("Anna skifter i Aarhus.", rail.danish)
        assertTrue(requireNotNull(rail.scene).contains("train", ignoreCase = true))
        assertNull(senses.getValue("L008-S02").examples.singleOrNull())
        assertNotNull(senses.getValue("L008-S02").exampleNote)
    }

    @Test
    fun `teaching claims require an exact witnessed plan pair and key`() {
        val valid = witness(
            "vocabulary-p3-s1", "vocabulary-p3-s1-1", "pilot-20", "generator-20",
            listOf("vocabulary-p3:bundle:L064:en cykel"),
        )
        val expectedFormId = VocabularyLibrary.catalog().items.single { it.entryId == "L064" }.senses.single()
            .activeForms.single().formId

        assertEquals(listOf(VocabularyTeachingClaim("L064", "L064-S01", false, listOf(expectedFormId))),
            VocabularyTeachingMapper.claims(listOf(valid)))
        assertEquals(emptyList(), VocabularyTeachingMapper.claims(listOf(valid.copy(planRevision = "vocabulary-p3-s1-2"))))
        assertEquals(emptyList(), VocabularyTeachingMapper.claims(listOf(valid.copy(contentRevision = "pilot-19"))))
        assertEquals(emptyList(), VocabularyTeachingMapper.claims(listOf(valid.copy(generatorRevision = "generator-19"))))
        assertEquals(emptyList(), VocabularyTeachingMapper.claims(listOf(valid.copy(
            introductionExposureKeys = listOf("arbitrary:L064", "vocabulary-p3:bundle:L999:en cykel"),
        ))))
    }

    @Test
    fun `finite review evidence does not infer an infinitive or unrelated lesson entry`() {
        val witness = witness(
            "vocabulary-p3-s5", "vocabulary-p3-s5-1", "pilot-20", "generator-20",
            listOf(
                "vocabulary-p3:meaning:L065:L065-S01", "vocabulary-p3:form:L065:cykler",
                "vocabulary-p3:form:L075:venter",
            ),
        )
        val claims = VocabularyTeachingMapper.claims(listOf(witness)).associateBy { it.entryId }
        val cycleForms = VocabularyLibrary.catalog().items.single { it.entryId == "L065" }.senses.single().activeForms

        assertEquals(listOf(cycleForms.single { it.text == "cykler" }.formId), claims.getValue("L065").formIds)
        assertTrue(claims.getValue("L065").meaningRecorded)
        assertFalse(cycleForms.single { it.text == "cykle" }.formId in claims.getValue("L065").formIds)
        assertEquals(setOf("L065", "L075"), claims.keys)

        val formOnly = witness.copy(introductionExposureKeys = listOf("vocabulary-p3:form:L065:cykler"))
        assertFalse(VocabularyTeachingMapper.claims(listOf(formOnly)).single().meaningRecorded)
    }

    @Test
    fun `request evidence cannot promote possession and duplicate witnesses collapse`() {
        val witness = witness(
            "vocabulary-p2-s6", "vocabulary-p2-s6-1", "pilot-19", "generator-19",
            listOf("vocabulary-p2:meaning:L009:L009-S02", "vocabulary-p2:form:L009:have"),
        )

        val claims = VocabularyTeachingMapper.claims(listOf(witness, witness))
        assertEquals(1, claims.size)
        assertEquals("L009-S02", claims.single().senseId)
        assertTrue(claims.single().meaningRecorded)
        val requestSense = VocabularyLibrary.catalog().items.single { it.entryId == "L009" }.senses.single { it.senseId == "L009-S02" }
        assertEquals(listOf(requestSense.activeForms.single().formId), claims.single().formIds)

        val possessionSupport = witness.copy(
            introductionExposureKeys = listOf("vocabulary-p2:support:possession-har"),
        )
        val possessionClaim = VocabularyTeachingMapper.claims(listOf(possessionSupport)).single()
        assertEquals("L009-S01", possessionClaim.senseId)
        assertEquals(listOf("L009-S01-F2"), possessionClaim.formIds)
    }

    @Test
    fun `legacy noun bundles and lesson-specific city surfaces map from their actual keys`() {
        val p1 = witness(
            "vocabulary-p1-s1", "vocabulary-p1-s1-2", "pilot-18", "generator-18",
            listOf("vocabulary-p1:bundle:L014:en bog"),
        )
        val personal = witness(
            "module-one-personal", "module-one-personal-1", "pilot-12", "generator-12",
            listOf("module-one:supplied-component:L032"),
        )

        assertEquals(listOf("L014-S01-F1"), VocabularyTeachingMapper.claims(listOf(p1)).single().formIds)
        assertEquals(
            listOf("L032-S01-F2"),
            VocabularyTeachingMapper.claims(listOf(personal)).single { it.entryId == "L032" }.formIds,
        )

        val rail = witness(
            "vocabulary-p3-s6", "vocabulary-p3-s6-1", "pilot-20", "generator-20",
            listOf("vocabulary-p3:form:L032:i Aarhus"),
        )
        assertEquals(
            listOf("L032-S01-F1"),
            VocabularyTeachingMapper.claims(listOf(rail)).single { it.entryId == "L032" }.formIds,
        )
    }

    @Test
    fun `every recognized source plan maps its actual saved introduction keys to a bounded entry set`() {
        val engine = PilotEngine()
        val plans = buildList {
            ModuleOnePracticePlanner.SPECIFICATIONS.forEach { add(ModuleOnePracticePlanner(engine).plan(it.focusId, it.planRevision, 0)) }
            add(InanimateReferencePracticePlanner(engine).plan(0))
            PresentOrderPracticePlanner.SPECIFICATIONS.forEach { add(PresentOrderPracticePlanner(engine).plan(it.focusId, 0)) }
            add(PossessionPracticePlanner(engine).plan(0))
            add(ReadingWordsPracticePlanner(engine).plan(0))
            add(PersonalIntroductionPracticePlanner(engine).plan(0))
            add(AbilityPracticePlanner(engine).plan(0))
            ModalRoundPracticePlanner.profiles().forEach { add(ModalRoundPracticePlanner(engine).plan(it.focusId, 0)) }
            add(CircumstancePracticePlanner(engine).plan(CircumstancePracticePlanner.FOCUS_ID, 0))
            add(QuestionPracticePlanner(engine).plan(QuestionPracticePlanner.FOCUS_ID, 0))
            VocabularyP1PracticePlanner.ALL_SPECIFICATIONS.forEach {
                add(VocabularyP1PracticePlanner(engine).plan(it.focusId, it.planRevision, 0))
            }
            VocabularyP2PracticePlanner.SPECIFICATIONS.forEach {
                add(VocabularyP2PracticePlanner(engine).plan(it.focusId, it.planRevision, 0))
            }
            VocabularyP3PracticePlanner.SPECIFICATIONS.forEach {
                add(VocabularyP3PracticePlanner(engine).plan(it.focusId, it.planRevision, 0))
            }
        }
        assertEquals(EXPECTED_PLAN_ENTRIES.keys, plans.map { "${it.focusId}/${it.planRevision}" }.toSet())

        plans.forEach { plan ->
            val claims = VocabularyTeachingMapper.claims(listOf(plan.toWitness()))
            assertEquals(
                EXPECTED_PLAN_ENTRIES.getValue("${plan.focusId}/${plan.planRevision}"),
                claims.map(VocabularyTeachingClaim::entryId).toSet(),
                "Unexpected claims for ${plan.focusId}/${plan.planRevision}",
            )
            claims.forEach { claim ->
                val sense = VocabularyLibrary.catalog().items.single { it.entryId == claim.entryId }
                    .senses.single { it.senseId == claim.senseId }
                assertTrue(claim.formIds.all { id -> sense.activeForms.any { it.formId == id } })
            }
        }
    }

    private fun PracticePlan.toWitness() = VocabularyTeachingWitness(
        focusId, planRevision, contentRevision, generatorRevision, introductionExposureKeys.orEmpty(),
    )

    private fun witness(
        focusId: String,
        planRevision: String,
        contentRevision: String,
        generatorRevision: String,
        keys: List<String>,
    ) = VocabularyTeachingWitness(focusId, planRevision, contentRevision, generatorRevision, keys)

    companion object {
        private val INTERNAL_LABEL = Regex("(?:C|E)\\d{3}|(?i:pair(?:-| )\\w+)|P[123]|Module 1")
        private val EXPECTED_PLAN_ENTRIES = buildMap {
            put("module-one-singular/module-one-singular-1", setOf("L001", "L002", "L003", "L004", "L010"))
            put("module-one-groups/module-one-groups-1", setOf("L001", "L002", "L005", "L006", "L007", "L010", "L013"))
            put("module-one-er/module-one-er-1", setOf("L001", "L008", "L010", "L014", "L018", "L022", "L023", "L025"))
            put("module-one-things/module-one-things-1", setOf("L008", "L010", "L014", "L018", "L022", "L023", "L026"))
            put("module-one-present/module-one-present-1", setOf("L011", "L013", "L027"))
            put("module-one-order/module-one-order-1", setOf("L002", "L003", "L007", "L011", "L013", "L018", "L023", "L027"))
            put("module-one-possession/module-one-possession-1", setOf("L001", "L009", "L014", "L022"))
            put("module-one-reading-words/module-one-reading-words-1", setOf("L014", "L015", "L018", "L024"))
            put("module-one-personal/module-one-personal-1", setOf("L028", "L029", "L030", "L031", "L032"))
            put("module-one-ability/module-one-ability-1", setOf("L011", "L018", "L023", "L031", "L033"))
            put("module-one-volition/module-one-volition-1", setOf("L001", "L011", "L014", "L022", "L034"))
            put("module-one-obligation/module-one-obligation-1", setOf("L002", "L011", "L018", "L023", "L035"))
            put("module-one-permission/module-one-permission-1", setOf("L003", "L011", "L015", "L022", "L036"))
            put("module-one-circumstances/module-one-circumstances-1", setOf("L032", "L037", "L038"))
            put("module-one-questions/module-one-questions-1", setOf("L039", "L040"))
            val p1 = listOf(
                setOf("L004", "L011", "L014", "L027", "L037"),
                setOf("L004", "L009", "L011", "L014", "L015", "L018", "L041", "L042"),
                setOf("L004", "L011", "L014", "L031", "L038", "L043", "L044"), setOf("L045", "L046", "L047"),
                setOf("L001", "L027", "L032", "L043", "L048", "L049"),
            )
            p1.forEachIndexed { index, ids ->
                put("vocabulary-p1-s${index + 1}/vocabulary-p1-s${index + 1}-1", ids)
                put("vocabulary-p1-s${index + 1}/vocabulary-p1-s${index + 1}-2", ids)
            }
            listOf(
                setOf("L001", "L012", "L021", "L050", "L051"),
                setOf("L001", "L021", "L045", "L046", "L047", "L050", "L052"),
                setOf("L012", "L050", "L053", "L054", "L055", "L056"),
                setOf("L001", "L012", "L045", "L055", "L057", "L058", "L059"),
                setOf("L034", "L053", "L054", "L055", "L058", "L060", "L061"),
                setOf("L001", "L009", "L034", "L046", "L047", "L060", "L061", "L062", "L063"),
            ).forEachIndexed { index, ids -> put("vocabulary-p2-s${index + 1}/vocabulary-p2-s${index + 1}-1", ids) }
            listOf(
                setOf("L008", "L009", "L016", "L038", "L064", "L065"),
                setOf("L008", "L066", "L067", "L068", "L069"),
                setOf("L008", "L027", "L029", "L032", "L048", "L070"),
                setOf("L008", "L009", "L071", "L072", "L073"),
                setOf("L016", "L065", "L068", "L069", "L074", "L075", "L076"),
                setOf("L001", "L032", "L067", "L069", "L072", "L075", "L076", "L077", "L078", "L079"),
            ).forEachIndexed { index, ids -> put("vocabulary-p3-s${index + 1}/vocabulary-p3-s${index + 1}-1", ids) }
        }
    }
}
