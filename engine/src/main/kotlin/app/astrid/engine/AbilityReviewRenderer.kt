package app.astrid.engine

internal object AbilityReviewRenderer {
    fun render(snapshot: PilotContentSnapshot, reviewSeed: Int): String = buildString {
        appendLine()
        appendLine("## Ability-after-kan task inventory")
        appendLine()
        appendLine("| Task | Format | Scope | Canonical sentence | Frame | Roles | Lexical IDs | Choices |")
        appendLine("| --- | --- | --- | --- | --- | --- | --- | --- |")
        listOf("E026", "E027").flatMap(AbilityContent::tasks).forEach { task ->
            val exercise = exerciseFor(snapshot, task, reviewSeed)
            appendLine(
                listOf(
                    task.id,
                    task.formatId,
                    task.targetScopeId,
                    exercise.inspector.canonicalSentence.orEmpty(),
                    task.frameId,
                    exercise.inspector.clauseRoles.joinToString(", "),
                    exercise.inspector.lexicalIds.joinToString(", "),
                    exercise.prompt.choices.joinToString(", "),
                ).joinToString(prefix = "| ", separator = " | ", postfix = " |", transform = ::escapeCell),
            )
        }
        appendLine()
    }

    private fun exerciseFor(
        snapshot: PilotContentSnapshot,
        task: AbilityTask,
        reviewSeed: Int,
    ): GeneratedExercise =
        (0 until AbilityContent.tasks(task.formatId).size).asSequence().map { index ->
            PilotEngine().generate(
                GenerationRequest(
                    task.formatId,
                    "G030",
                    reviewSeed,
                    index,
                    snapshot.contentRevision,
                    snapshot.generatorRevision,
                ),
            )
        }.first { (it.source as AbilityExerciseSource).task.id == task.id }

    private fun escapeCell(value: String): String =
        value.replace("|", "\\|").replace("\r", " ").replace("\n", " ")
}
