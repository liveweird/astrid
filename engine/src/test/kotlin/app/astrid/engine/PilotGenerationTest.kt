package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class PilotGenerationTest {
    private val engine = PilotEngine()

    @Test
    fun `ordering prompts never start in the solution order`() {
        listOf(0, 1, 3, 23, Int.MAX_VALUE).forEach { seed ->
            (0 until 99).forEach { index ->
                val exercise = engine.generate(request("E003", "G005", seed, index))
                val displayedRoles = exercise.prompt.blocks.map { exercise.source.blockRoles.getValue(it.id) }
                val solution = exercise.source.answerKey as AnswerKey.SubjectFirstOrder
                assertNotEquals(solution.canonicalRoles, displayedRoles, "seed=$seed index=$index")
            }
        }
    }

    @Test
    fun `introductions teach active forms meanings and articles before assessment`() {
        val reading = (0 until 99).asSequence()
            .map { engine.generate(request("E001", "G004", index = it)) }
            .first { it.inspector.canonicalSentence == "Hun læser en avis." }
        val introduction = reading.introduction.joinToString(" ") { it.body }

        assertTrue("læse means ‘to read’" in introduction)
        assertTrue("finite present form is læser" in introduction)
        assertTrue("en avis means ‘a newspaper’" in introduction)
        val apple = (0 until 92).asSequence()
            .map { engine.generate(request("E002", "G006", index = it)) }
            .first { "et æble" in it.inspector.canonicalSentence.orEmpty() }
        assertTrue(apple.introduction.any { "et æble means ‘an apple’" in it.body })
    }

    @Test
    fun `catalog exposes current skills formats and bounded sentence count`() {
        val catalog = engine.catalog()

        assertEquals((1..6).map { "G${it.toString().padStart(3, '0')}" } + listOf("G038", "G030", "G016"), catalog.skills.map { it.id })
        assertEquals((1..29).map { "E${it.toString().padStart(3, '0')}" }, catalog.formats.map { it.id })
        assertEquals(109, catalog.sentenceCount)
        assertEquals("pilot-15", catalog.contentRevision)
        assertEquals("generator-15", catalog.generatorRevision)
    }

    @Test
    fun `all 99 licensed sentences are generated once before repetition`() {
        listOf("E001" to "G004", "E003" to "G005").forEach { (format, target) ->
            val firstCycle = (0 until 99).map { engine.generate(request(format, target, seed = 47, index = it)) }
            assertEquals(99, firstCycle.map { it.inspector.canonicalSentenceId }.toSet().size)
            assertEquals(PilotContent.sentences.map { it.id }.toSet(), firstCycle.map { it.inspector.canonicalSentenceId }.toSet())
            assertEquals(firstCycle.first().inspector.canonicalSentenceId,
                engine.generate(request(format, target, seed = 47, index = 99)).inspector.canonicalSentenceId)
        }
    }

    @Test
    fun `article format covers exactly the 92 sentences containing nouns`() {
        val generated = (0 until 92).map { engine.generate(request("E002", "G006", seed = 31, index = it)) }

        assertEquals(92, generated.map { it.inspector.canonicalSentenceId }.toSet().size)
        assertTrue(generated.none { it.inspector.constructionId == "C001" })
        assertEquals(generated.first().inspector.canonicalSentenceId,
            engine.generate(request("E002", "G006", seed = 31, index = 92)).inspector.canonicalSentenceId)
    }

    @Test
    fun `generation is deterministic and seeds can select different permutations`() {
        val first = engine.generate(request("E001", "G004", seed = 9, index = 5))
        val repeated = engine.generate(request("E001", "G004", seed = 9, index = 5))
        val anotherSeed = engine.generate(request("E001", "G004", seed = 10, index = 5))

        assertEquals(first.id, repeated.id)
        assertEquals(first.inspector, repeated.inspector)
        assertEquals(first.prompt, repeated.prompt)
        assertNotEquals(first.inspector.canonicalSentenceId, anotherSeed.inspector.canonicalSentenceId)
        val firstSequence = (0 until 99).map {
            engine.generate(request("E001", "G004", seed = 9, index = it)).inspector.canonicalSentenceId
        }
        val anotherSequence = (0 until 99).map {
            engine.generate(request("E001", "G004", seed = 10, index = it)).inspector.canonicalSentenceId
        }
        assertNotEquals(firstSequence, anotherSequence)
    }

    @Test
    fun `all generated formats have distinct safe public controls`() {
        (0 until 99).forEach { index ->
            val choice = engine.generate(request("E001", "G004", seed = 3, index = index))
            assertEquals(2, choice.prompt.choices.toSet().size)
            assertTrue(choice.hint !in choice.prompt.choices)

            val order = engine.generate(request("E003", "G005", seed = 3, index = index))
            assertEquals(order.prompt.blocks.size, order.prompt.blocks.map { it.id }.toSet().size)
            assertEquals(order.prompt.blocks.size, order.prompt.blocks.map { it.text }.toSet().size)
            assertTrue(order.prompt.blocks.all { it.id.matches(Regex("b\\d+")) })
        }
    }

    @Test
    fun `support skills reflect whether a sentence contains a noun phrase`() {
        val sleep = (0 until 99).asSequence()
            .map { engine.generate(request("E001", "G004", index = it)) }
            .first { it.inspector.constructionId == "C001" }
        val transitive = (0 until 99).asSequence()
            .map { engine.generate(request("E001", "G004", index = it)) }
            .first { it.inspector.constructionId == "C002" }

        assertTrue("G006" !in sleep.supportSkillIds)
        assertTrue("G006" in transitive.supportSkillIds)
    }

    @Test
    fun `every licensed sentence uses all and only the 23 seed entries`() {
        val lexicalIds = PilotContent.sentences.flatMap { sentence ->
            sentence.subject.lexicalIds + sentence.verb.lexicalId + sentence.complement.orEmptyLexicalIds()
        }.toSet()

        assertEquals((1..23).map { "L${it.toString().padStart(3, '0')}" }.toSet(), lexicalIds)
    }

    @Test
    fun `catalogue has the independently specified construction and verb fixtures`() {
        val byConstructionAndVerb = PilotContent.sentences.groupingBy {
            it.constructionId to it.verb.lexicalId
        }.eachCount()

        assertEquals(7, byConstructionAndVerb["C001" to "L013"])
        assertEquals(56, byConstructionAndVerb["C002" to "L009"])
        assertEquals(21, byConstructionAndVerb["C002" to "L011"])
        assertEquals(7, byConstructionAndVerb["C002" to "L012"])
        assertEquals(8, byConstructionAndVerb["C003" to "L010"])

        val readNouns = PilotContent.sentences
            .filter { it.verb.lexicalId == "L011" }
            .map { it.complement?.lexicalIds?.last() }
            .toSet()
        val eatNouns = PilotContent.sentences
            .filter { it.verb.lexicalId == "L012" }
            .map { it.complement?.lexicalIds?.last() }
            .toSet()
        assertEquals(setOf("L014", "L015", "L018"), readNouns)
        assertEquals(setOf("L021"), eatNouns)
    }

    @Test
    fun `canonical sentence IDs are lexical identities rather than positions`() {
        val sentence = PilotContent.sentences.single {
            it.subject.lexicalIds == listOf("L004") &&
                it.verb.lexicalId == "L011" &&
                it.complement?.lexicalIds?.last() == "L015"
        }

        assertEquals("C002-L004-L011-L015", sentence.id)
    }

    @Test
    fun `invalid requests and unsupported revisions fail before generation`() {
        assertFailsWith<IllegalArgumentException> { engine.generate(request("E001", "G004", seed = -1)) }
        assertFailsWith<IllegalArgumentException> { engine.generate(request("E001", "G004", index = -1)) }
        assertFailsWith<IllegalArgumentException> { engine.generate(request("E001", "G004", index = 1_000_001)) }
        assertFailsWith<IllegalArgumentException> { engine.generate(request("E001", "G006")) }
        assertFailsWith<IllegalArgumentException> { engine.generate(request("E999", "G004")) }
        assertFailsWith<IllegalArgumentException> {
            engine.generate(request("E001", "G004").copy(contentRevision = "pilot-2"))
        }
        assertFailsWith<IllegalArgumentException> {
            engine.generate(request("E001", "G004").copy(generatorRevision = "generator-2"))
        }
    }

    private fun request(
        format: String,
        target: String,
        seed: Int = 0,
        index: Int = 0,
    ) = GenerationRequest(format, target, seed, index)

    private fun Constituent?.orEmptyLexicalIds() = this?.lexicalIds.orEmpty()
}
