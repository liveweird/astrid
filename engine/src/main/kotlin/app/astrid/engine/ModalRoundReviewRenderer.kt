package app.astrid.engine

internal object ModalRoundReviewRenderer {
    fun render(snapshot: PilotContentSnapshot, reviewSeed: Int): String = buildString {
        appendLine()
        appendLine("## Contextual modal round inventory")
        appendLine()
        appendLine(
            "| Task | Format | Scope | Modal sense | Interpretation | Frame | Context | Sentence | Roles | " +
                "Choices or tokens | Answer exposure | Prompt exposure | Feedback exposure |",
        )
        appendLine("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |")
        ModalRoundContent.definitions.forEach { definition ->
            appendExerciseRow(interpretationExercise(snapshot, reviewSeed, definition))
            appendExerciseRow(constructionExercise(snapshot, reviewSeed, definition))
        }
        appendLine()
    }

    private fun StringBuilder.appendExerciseRow(exercise: GeneratedExercise) {
        val source = requireNotNull(exercise.inspector.modalSource)
        val exposure = when (val privateSource = exercise.source) {
            is ModalInterpretationExerciseSource -> Exposure(
                privateSource.task.targetScopeId,
                privateSource.task.answerExposureKeys,
                privateSource.task.promptExposureKeys,
                privateSource.task.feedbackExposureKeys,
            )
            is PresentOrderExerciseSource -> (privateSource.task as ModalConstructionTask).let { task ->
                Exposure(
                    task.targetScopeId,
                    task.answerExposureKeys,
                    task.promptExposureKeys,
                    task.feedbackExposureKeys,
                )
            }
            else -> error("Unexpected modal review source")
        }
        val choicesOrTokens = if (exercise.prompt.choices.isNotEmpty()) {
            exercise.prompt.choices.joinToString(" / ")
        } else {
            exercise.prompt.blocks.joinToString(" / ") { block -> "${block.id}:${block.text}" }
        }
        appendLine(
            listOf(
                source.taskId,
                exercise.formatId,
                exposure.scopeId,
                "${source.modalEntryId}/${source.modalSenseId}",
                source.expectedInterpretation,
                source.frameVariantId,
                source.contextVariantId,
                exercise.inspector.canonicalSentence.orEmpty(),
                exercise.inspector.clauseRoles.joinToString(", "),
                choicesOrTokens,
                exposure.answer.joinToString(", "),
                exposure.prompt.joinToString(", "),
                exposure.feedback.joinToString(", "),
            ).joinToString(prefix = "| ", separator = " | ", postfix = " |", transform = ::escapeCell),
        )
    }

    private fun interpretationExercise(
        snapshot: PilotContentSnapshot,
        seed: Int,
        definition: ModalRoundDefinition,
    ): GeneratedExercise =
        (0 until ModalRoundContent.definitions.size).asSequence().map { index ->
            PilotEngine().generate(
                GenerationRequest("E028", "G030", seed, index, snapshot.contentRevision, snapshot.generatorRevision),
            )
        }.first { (it.source as ModalInterpretationExerciseSource).task.definition.key == definition.key }

    private fun constructionExercise(
        snapshot: PilotContentSnapshot,
        seed: Int,
        definition: ModalRoundDefinition,
    ): GeneratedExercise =
        (0 until CONSTRUCTION_POOL_SIZE).asSequence().map { index ->
            PilotEngine().generate(
                GenerationRequest("E025", "G005", seed, index, snapshot.contentRevision, snapshot.generatorRevision),
            )
        }.first { exercise ->
            val task = (exercise.source as PresentOrderExerciseSource).task
            task is ModalConstructionTask && task.definition.key == definition.key
        }

    private fun escapeCell(value: String): String =
        value.replace("|", "\\|").replace("\r", " ").replace("\n", " ")

    private data class Exposure(
        val scopeId: String,
        val answer: List<String>,
        val prompt: List<String>,
        val feedback: List<String>,
    )

    private const val CONSTRUCTION_POOL_SIZE = 6
}
