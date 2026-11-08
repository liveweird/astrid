package app.astrid.engine

import java.security.MessageDigest
import java.util.Base64
import kotlin.test.Test
import kotlin.test.assertEquals

class PilotTwoCompatibilityGoldenTest {
    private val engine = PilotEngine()

    @Test
    fun `published pilot two generation and evaluation remain frozen`() {
        val expected = resource("golden/pilot-2-generator-2.tsv").bufferedReader().use { it.readLines() }
        val actual = expected.map(::replay)

        assertEquals(952, expected.size)
        assertEquals(expected, actual)
    }

    @Test
    fun `pilot two provenance identifies immutable release baseline`() {
        val provenance = resource("golden/pilot-2-generator-2.provenance").bufferedReader().use { it.readText() }

        assertEquals(true, "baselineCommit=86a9e07" in provenance)
        assertEquals(true, "rowCount=952" in provenance)
        assertEquals(true, "fixtureSha256=7f70cc10a075df85d3bc759a9241be0bb93a48f251b178161205ebb87d9ed635" in provenance)
    }

    private fun replay(row: String): String {
        val fields = row.split('\t')
        val kind = fields[0]
        val format = fields[1]
        val target = fields[2]
        val seed = fields[3].toInt()
        val index = fields[4].toInt()
        val exercise = engine.generate(
            GenerationRequest(format, target, seed, index, "pilot-2", "generator-2"),
        )
        val hash = when (kind) {
            "G" -> generationHash(exercise)
            "T" -> {
                val raw = String(Base64.getDecoder().decode(fields[5]), Charsets.UTF_8)
                evaluationHash(engine.evaluate(exercise, Answer.Text(raw)))
            }
            "O" -> {
                val raw = String(Base64.getDecoder().decode(fields[5]), Charsets.UTF_8)
                evaluationHash(engine.evaluate(exercise, Answer.Order(raw.split(','))))
            }
            else -> error("Unknown frozen row kind $kind")
        }
        return fields.dropLast(1).plus(hash).joinToString("\t")
    }

    private fun generationHash(exercise: GeneratedExercise): String {
        val prompt = exercise.prompt
        val inspector = exercise.inspector
        return hash(
            listOf(
                exercise.id,
                exercise.formatId,
                exercise.targetSkillId.orEmpty(),
                exercise.supportSkillIds.joinToString(","),
                exercise.contentRevision,
                exercise.generatorRevision,
                exercise.reproduction.seed.toString(),
                exercise.reproduction.index.toString(),
                exercise.instruction,
                exercise.context,
                prompt.before,
                prompt.after,
                prompt.choices.joinToString(","),
                prompt.blocks.joinToString(",") { "${it.id}:${it.text}" },
                prompt.punctuation,
                prompt.inputLabel ?: NULL,
                prompt.replacementWord ?: NULL,
                exercise.introduction.joinToString("\u001e") { "${it.title}:${it.body}" },
                exercise.hint,
                inspector.canonicalSentenceId ?: NULL,
                inspector.constructionId ?: NULL,
                inspector.lexicalIds.joinToString(","),
                inspector.clauseRoles.joinToString(","),
                inspector.canonicalSentence ?: NULL,
            ),
        )
    }

    private fun evaluationHash(evaluation: Evaluation): String {
        val normalized = when (val answer = evaluation.normalizedAnswer) {
            is NormalizedAnswer.Text -> "T:${answer.text}"
            is NormalizedAnswer.Order -> "O:${answer.blockIds.joinToString(",")}"
            is NormalizedAnswer.Matching -> error("Matching answer is unreachable in pilot two")
        }
        return hash(
            listOf(
                evaluation.targetStatus.wireValue,
                evaluation.linguisticStatus.wireValue,
                evaluation.issues.joinToString("\u001e") { "${it.code}:${it.message}" },
                evaluation.feedback,
                evaluation.completedSentence ?: NULL,
                normalized,
            ),
        )
    }

    private fun hash(fields: List<String>): String {
        val canonical = fields.joinToString("\t") { "${it.length}:$it" }
        return MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray()).joinToString("") {
            "%02x".format(it)
        }
    }

    private fun resource(path: String) =
        requireNotNull(javaClass.classLoader.getResourceAsStream(path)) { "Missing test resource $path" }

    private companion object {
        const val NULL = "<null>"
    }
}
