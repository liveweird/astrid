package app.astrid.engine

import java.nio.file.Path
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull

class VocabularyP1MatchingCompatibilityRepairTest {
    private val engine = PilotEngine()

    @Test
    fun `restores all four pair seventeen matching boards from their saved presentation`() {
        (0..3).forEach { index ->
            val request = request(index = index)
            val original = engine.generate(request)
            val originalColumns = assertNotNull(original.prompt.matchingColumns)
            val savedColumns = MatchingColumns(
                left = rotateTexts(originalColumns.left),
                right = rotateTexts(originalColumns.right),
            )

            val restored = engine.restoreLegacyVocabularyMatching(request, savedColumns)
            val source = assertNotNull(restored.inspector.vocabularySource)
            val taskId = assertNotNull(original.inspector.vocabularySource).taskId
            val expectedLabels = EXPECTED_MATCHES.getValue(taskId)
            val savedRightIdsByLabel = savedColumns.right.associate { it.text to it.id }
            val oraclePairs = savedColumns.left.map { left ->
                MatchingPair(left.id, savedRightIdsByLabel.getValue(expectedLabels.getValue(left.text)))
            }

            assertEquals(savedColumns, restored.prompt.matchingColumns)
            assertEquals(savedColumns.left.map(MatchingItem::id), source.matchingPairs.map { it.leftId })
            assertEquals(oraclePairs.map(MatchingPair::rightId), source.matchingPairs.map { it.rightId })
            assertEquals(original.id, restored.id)
            assertEquals(original.formatId, restored.formatId)
            assertEquals(original.targetSkillId, restored.targetSkillId)
            assertEquals(original.lexicalTarget, restored.lexicalTarget)
            assertEquals(original.supportSkillIds, restored.supportSkillIds)
            assertEquals(
                original.presentation.copy(prompt = original.prompt.copy(matchingColumns = savedColumns)),
                restored.presentation,
            )
            assertEquals(original.contentRevision, restored.contentRevision)
            assertEquals(original.generatorRevision, restored.generatorRevision)
            assertEquals(original.reproduction, restored.reproduction)
            assertEquals(original.lexicalTargets, restored.lexicalTargets)
            assertEquals(original.readingTarget, restored.readingTarget)
            assertEquals(original.targetScopeId, restored.targetScopeId)
            assertEquals(original.source, restored.source)
            assertEquals(original.inspector.copy(vocabularySource = source), restored.inspector)
            assertNotEquals(originalColumns, savedColumns)

            val evaluation = engine.evaluate(restored, Answer.Matching(oraclePairs))
            assertEquals(TargetStatus.MET, evaluation.targetStatus)
            assertEquals(source.matchingPairs.size, assertNotNull(evaluation.matchingResults).size)
        }
    }

    @Test
    fun `rejects nonlegacy requests and malformed saved matching columns`() {
        val request = request(index = 0)
        val columns = assertNotNull(engine.generate(request).prompt.matchingColumns)

        listOf(
            request.copy(contentRevision = "pilot-18", generatorRevision = "generator-18"),
            request.copy(formatId = "E032"),
            request.copy(contentRevision = "pilot-16", generatorRevision = "generator-16"),
        ).forEach { invalid ->
            assertFailsWith<IllegalArgumentException> {
                engine.restoreLegacyVocabularyMatching(invalid, columns)
            }
        }

        val malformed = listOf(
            columns.copy(left = columns.left.dropLast(1)),
            columns.copy(right = columns.right + columns.right.last()),
            columns.copy(left = columns.left.mapIndexed { index, item ->
                if (index == 0) item.copy(text = "not authored") else item
            }),
            columns.copy(right = columns.right.mapIndexed { index, item ->
                if (index == 0) item.copy(text = "ikke forfattet") else item
            }),
            columns.copy(left = columns.left.mapIndexed { index, item ->
                if (index == 1) item.copy(id = columns.left.first().id) else item
            }),
            columns.copy(right = columns.right.mapIndexed { index, item ->
                if (index == 0) item.copy(id = "n-invalid") else item
            }),
        )
        malformed.forEach { invalid ->
            assertFailsWith<IllegalArgumentException> {
                engine.restoreLegacyVocabularyMatching(request, invalid)
            }
        }
    }

    @Test
    fun `retains revision one plans on pair seventeen and advertises revision two on pair eighteen`() {
        val planner = PracticePlanner(engine)
        VocabularyP1PracticePlanner.SPECIFICATIONS.forEach { current ->
            val retained = planner.plan(current.focusId, "${current.focusId}-1", 47)
            val latest = planner.plan(current.focusId, current.planRevision, 47)
            assertEquals("pilot-17", retained.contentRevision)
            assertEquals("generator-17", retained.generatorRevision)
            assertEquals("evaluation-17", retained.evaluationRevision)
            assertEquals("pilot-18", latest.contentRevision)
            assertEquals("generator-18", latest.generatorRevision)
            assertEquals("evaluation-18", latest.evaluationRevision)
            assertEquals(current.planRevision, planner.profiles().single { it.focusId == current.focusId }.planRevision)
        }
    }

    @Test
    fun `pair eighteen matching is stable across fresh JVM enum identity histories`() {
        val baseline = runProbe(0)
        val perturbed = runProbe(20_000)

        assertNotEquals(baseline.getValue("IDENTITIES"), perturbed.getValue("IDENTITIES"))
        assertEquals(baseline.getValue("P18"), perturbed.getValue("P18"))
        assertNotEquals(baseline.getValue("P17"), perturbed.getValue("P17"))
    }

    private fun request(index: Int) = GenerationRequest(
        formatId = "E033",
        targetSkillId = null,
        seed = 0,
        index = index,
        contentRevision = "pilot-17",
        generatorRevision = "generator-17",
    )

    private fun rotateTexts(items: List<MatchingItem>): List<MatchingItem> {
        val texts = items.map(MatchingItem::text).drop(1) + items.first().text
        return items.zip(texts) { item, text -> item.copy(text = text) }
    }

    private companion object {
        val EXPECTED_MATCHES = mapOf(
            "p1-2.1" to mapOf(
                "a newspaper" to "en avis",
                "a written letter" to "et brev",
                "writes" to "skriver",
                "a computer" to "en computer",
            ),
            "p1-3.1" to mapOf(
                "studies (as a student)" to "studerer",
                "a course" to "et kursus",
                "Danish (language)" to "dansk",
                "now" to "nu",
            ),
            "p1-4.1" to mapOf(
                "drinks" to "drikker",
                "coffee" to "kaffe",
                "water" to "vand",
            ),
            "p1-5.1" to mapOf(
                "in Aarhus" to "i Aarhus",
                "at home" to "hjemme",
                "in the morning" to "om morgenen",
            ),
        )
    }

    private fun runProbe(identityPerturbations: Int): Map<String, String> {
        val java = Path.of(System.getProperty("java.home"), "bin", "java").toString()
        val classPath = listOf(
            VocabularyP1MatchingDeterminismProbeMain::class.java,
            PilotEngine::class.java,
            Unit::class.java,
        ).map { type -> Path.of(requireNotNull(type.protectionDomain.codeSource).location.toURI()).toString() }
            .distinct()
            .joinToString(System.getProperty("path.separator"))
        val process = ProcessBuilder(
            java,
            "-cp",
            classPath,
            "app.astrid.engine.VocabularyP1MatchingDeterminismProbeMain",
            identityPerturbations.toString(),
        ).redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        assertEquals(0, process.waitFor(), output)
        return output.lineSequence().filter(String::isNotBlank).associate { line ->
            line.substringBefore('\t') to line.substringAfter('\t')
        }
    }
}

object VocabularyP1MatchingDeterminismProbeMain {
    @JvmStatic
    fun main(args: Array<String>) {
        repeat(args.single().toInt()) { System.identityHashCode(Any()) }
        println("IDENTITIES\t" + VocabularyLexicalKind.entries.joinToString(",") { System.identityHashCode(it).toString() })
        println("P18\t" + projection("pilot-18", "generator-18"))
        println("P17\t" + projection("pilot-17", "generator-17"))
    }

    private fun projection(contentRevision: String, generatorRevision: String): String {
        val engine = PilotEngine()
        val generated = listOf(0, 1, 47, Int.MAX_VALUE).flatMap { seed ->
            listOf(0, 1, 2, 3, 4, 1_000_000).map { index ->
                val exercise = engine.generate(
                    GenerationRequest("E033", null, seed, index, contentRevision, generatorRevision),
                )
                listOf(
                    exercise.id,
                    exercise.formatId,
                    exercise.targetSkillId,
                    exercise.lexicalTarget,
                    exercise.supportSkillIds,
                    exercise.presentation,
                    exercise.contentRevision,
                    exercise.generatorRevision,
                    exercise.reproduction,
                    exercise.inspector,
                    exercise.source,
                    exercise.readingTarget,
                    exercise.lexicalTargets,
                    exercise.targetScopeId,
                ).joinToString("\u001f")
            }
        }.joinToString("\u001e")
        return Base64.getEncoder().encodeToString(generated.toByteArray(Charsets.UTF_8))
    }
}
