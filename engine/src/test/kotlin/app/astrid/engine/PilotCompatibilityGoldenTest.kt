package app.astrid.engine

import kotlin.test.Test
import kotlin.test.assertEquals

class PilotCompatibilityGoldenTest {
    private val engine = PilotEngine()

    @Test
    fun `published pilot outputs remain byte-for-byte compatible`() {
        val expected = readGoldenLines()
        val actual = buildGoldenLines()

        assertEquals(expected.size, actual.size, "golden row count")
        expected.indices.forEach { index ->
            assertEquals(expected[index], actual[index], "golden row ${index + 1}")
        }
    }

    @Test
    fun `golden provenance identifies the published revisions and baseline`() {
        val provenance = resource("golden/pilot-1-generator-1.provenance").bufferedReader().use { it.readText() }

        assertEquals(true, "baselineCommit=3b462a9" in provenance)
        assertEquals(true, "contentRevision=pilot-1" in provenance)
        assertEquals(true, "generatorRevision=generator-1" in provenance)
    }

    private fun buildGoldenLines(): List<String> =
        buildList {
            addCycle("E001", "G004", FIXED_SEED, 99)
            addCycle("E002", "G006", FIXED_SEED, 92)
            addCycle("E003", "G005", FIXED_SEED, 99)
            listOf(Triple("E001", "G004", 99), Triple("E002", "G006", 92), Triple("E003", "G005", 99))
                .forEach { (format, target, size) ->
                    BOUNDARY_SEEDS.forEach { seed ->
                        listOf(0, size - 1, size, MAX_INDEX).forEach { index ->
                            add("BOUNDARY\t${serialize(generate(format, target, seed, index))}")
                        }
                    }
                }
            addGradingCases()
        }

    private fun MutableList<String>.addCycle(
        format: String,
        target: String,
        seed: Int,
        size: Int,
    ) {
        repeat(size) { index -> add("CYCLE\t${serialize(generate(format, target, seed, index))}") }
    }

    private fun MutableList<String>.addGradingCases() {
        val read = find("E001", "G004", "Hun læser en avis.")
        addGrade("W001", read, Answer.Text("læser"))
        addGrade("W002", read, Answer.Text("læse"))
        addGrade("W009", read, Answer.Text("laeser"))

        val house = find("E002", "G006", "Jeg har et hus.")
        addGrade("W003", house, Answer.Text("et"))
        addGrade("W004", house, Answer.Text("en"))
        addGrade("W010", house, Answer.Text(" et "))
        addGrade("W011", house, Answer.Text("ét"))

        val order = find("E003", "G005", "Jeg har en bog.")
        val byText = order.prompt.blocks.associate { it.text to it.id }
        addGrade("W007", order, Answer.Order(listOf("jeg", "har", "en bog").map(byText::getValue)))
        addGrade("W008", order, Answer.Order(listOf("en bog", "har", "jeg").map(byText::getValue)))
    }

    private fun MutableList<String>.addGrade(
        caseId: String,
        exercise: GeneratedExercise,
        answer: Answer,
    ) {
        val result = engine.evaluate(exercise, answer)
        val raw =
            when (answer) {
                is Answer.Text -> "T:${answer.text}"
                is Answer.Order -> "O:${answer.blockIds.joinToString(",")}"
            }
        val issues = result.issues.joinToString("|") { "${it.code}:${it.message}" }
        add(
            listOf(
                "GRADE",
                field(caseId),
                field(exercise.id),
                field(raw),
                field(result.targetStatus.wireValue),
                field(result.linguisticStatus.wireValue),
                field(issues),
                field(result.feedback),
                field(result.completedSentence.orEmpty()),
            ).joinToString("\t"),
        )
    }

    private fun serialize(exercise: GeneratedExercise): String =
        listOf(
            exercise.formatId,
            exercise.reproduction.seed.toString(),
            exercise.reproduction.index.toString(),
            exercise.id,
            exercise.targetSkillId,
            exercise.supportSkillIds.joinToString(","),
            exercise.instruction,
            exercise.context,
            exercise.prompt.before,
            exercise.prompt.after,
            exercise.prompt.choices.joinToString(","),
            exercise.prompt.punctuation,
            exercise.contentRevision,
            exercise.generatorRevision,
            exercise.inspector.canonicalSentenceId,
            exercise.inspector.constructionId,
            exercise.inspector.lexicalIds.joinToString(","),
            exercise.inspector.clauseRoles.joinToString(","),
            exercise.inspector.canonicalSentence,
            exercise.hint,
            exercise.prompt.blocks.joinToString(",") { "${it.id}:${it.text}" },
            exercise.introduction.joinToString("|") { "${it.title}:${it.body}" },
        ).joinToString("\t", transform = ::field)

    private fun generate(
        format: String,
        target: String,
        seed: Int,
        index: Int,
    ) = engine.generate(GenerationRequest(format, target, seed, index))

    private fun find(
        format: String,
        target: String,
        canonical: String,
    ): GeneratedExercise {
        repeat(99) { index ->
            val exercise = generate(format, target, 0, index)
            if (exercise.inspector.canonicalSentence == canonical) return exercise
        }
        error("Golden fixture sentence not generated: $canonical")
    }

    private fun readGoldenLines(): List<String> {
        return resource("golden/pilot-1-generator-1.tsv").bufferedReader().use { it.readLines() }
    }

    private fun resource(path: String) =
        requireNotNull(javaClass.classLoader.getResourceAsStream(path)) { "Missing test resource $path" }

    private fun field(value: String) = "${value.length}:$value"

    private companion object {
        const val FIXED_SEED = 47
        const val MAX_INDEX = 1_000_000
        val BOUNDARY_SEEDS = listOf(0, 1, Int.MAX_VALUE)
    }
}
