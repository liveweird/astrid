package app.astrid.engine

object PilotReview {
    fun renderMarkdown(): String = renderSnapshot(PilotContent.currentSnapshot)

    fun renderLegacyMarkdown(): String = renderSnapshot(PilotContent.snapshot)

    fun renderPilotTwoMarkdown(): String = renderSnapshot(PilotContent.pilotTwoSnapshot)

    private fun renderSnapshot(snapshot: PilotContentSnapshot): String {
        return buildString {
            appendLine("# Astrid pilot content review")
            appendLine()
            appendLine("- Content revision: `${snapshot.contentRevision}`")
            appendLine("- Generator revision: `${snapshot.generatorRevision}`")
            appendLine("- Review state: `${PilotReviewMetadata.REVIEW_STATE}`")
            appendLine("- Provenance: ${PilotReviewMetadata.PROVENANCE}")
            appendLine("- Source review: ${PilotReviewMetadata.SOURCE_REVIEW_DATE}")
            appendLine("- Inventory: ${snapshot.lexicalReview.size} lexical records; ${snapshot.sentences.size} sentences")
            appendLine()
            appendLexicalInventory(snapshot.lexicalReview)
            appendSentenceInventory(snapshot.sentences)
            appendExamples(snapshot)
        }
    }

    private fun StringBuilder.appendLexicalInventory(records: List<LexicalReviewRecord>) {
        appendLine("## Lexical inventory")
        appendLine()
        appendLine("| ID | Headword | Selected sense | Active forms | Constraints | Sources | Review state |")
        appendLine("| --- | --- | --- | --- | --- | --- | --- |")
        records.forEach { record ->
            appendLine(
                listOf(
                    record.id,
                    record.headword,
                    record.selectedSense,
                    record.activeForms.entries.joinToString("; ") { "${it.key}=${it.value}" },
                    record.constraints,
                    record.sourceUrls.joinToString("; "),
                    record.reviewState,
                ).joinToString(prefix = "| ", separator = " | ", postfix = " |", transform = ::escapeCell),
            )
        }
        appendLine()
    }

    private fun StringBuilder.appendSentenceInventory(sentences: List<Sentence>) {
        appendLine("## Licensed sentence inventory")
        appendLine()
        appendLine("The order below is the published candidate enumeration order.")
        appendLine()
        appendLine("| # | Canonical ID | Construction | Lexical IDs | Sentence |")
        appendLine("| ---: | --- | --- | --- | --- |")
        sentences.forEachIndexed { index, sentence ->
            val lexicalIds =
                sentence.subject.lexicalIds + sentence.verb.lexicalId + sentence.complement?.lexicalIds.orEmpty()
            appendLine(
                "| ${index + 1} | ${sentence.id} | ${sentence.constructionId} | " +
                    "${lexicalIds.joinToString(", ")} | ${realize(sentence)} |",
            )
        }
        appendLine()
    }

    private fun StringBuilder.appendExamples(snapshot: PilotContentSnapshot) {
        val engine = PilotEngine()
        appendLine("## Reproducible exercise examples")
        appendLine()
        appendLine(
            "All examples use content `${snapshot.contentRevision}`, generator `${snapshot.generatorRevision}`, and seed 47.",
        )
        appendLine()
        appendLine("| Format | Index | Exercise ID | Canonical sentence | Prompt |")
        appendLine("| --- | ---: | --- | --- | --- |")
        snapshot.formats.map { it.id to it.targetSkillId }.forEach { (format, target) ->
            listOf(0, 1).forEach { index ->
                val exercise =
                    engine.generate(
                        GenerationRequest(
                            format,
                            target,
                            seed = REVIEW_SEED,
                            index = index,
                            contentRevision = snapshot.contentRevision,
                            generatorRevision = snapshot.generatorRevision,
                        ),
                    )
                appendLine(
                    listOf(
                        format,
                        index.toString(),
                        exercise.id,
                        exercise.inspector.canonicalSentence.orEmpty(),
                        promptSummary(exercise),
                    ).joinToString(prefix = "| ", separator = " | ", postfix = " |", transform = ::escapeCell),
                )
            }
        }
    }

    private fun promptSummary(exercise: GeneratedExercise): String =
        when {
            exercise.prompt.cue != null && exercise.prompt.choices.isNotEmpty() ->
                "Cue (${exercise.prompt.cueLanguage}): ${exercise.prompt.cue} " +
                    "[${exercise.prompt.choices.joinToString(", ")}]"
            exercise.prompt.cue != null ->
                "Cue (${exercise.prompt.cueLanguage}): ${exercise.prompt.cue} → " +
                    exercise.prompt.inputLabel.orEmpty()
            exercise.prompt.choices.isNotEmpty() ->
                "${exercise.prompt.before}___${exercise.prompt.after} [${exercise.prompt.choices.joinToString(", ")}]"
            exercise.prompt.blocks.isNotEmpty() ->
                exercise.prompt.blocks.joinToString(" / ") { "${it.id}:${it.text}" } + exercise.prompt.punctuation
            exercise.prompt.replacementWord != null ->
                "${exercise.prompt.before}[replace: ${exercise.prompt.replacementWord}]${exercise.prompt.after}"
            else -> "${exercise.prompt.before}___${exercise.prompt.after}"
        }

    private fun realize(sentence: Sentence): String {
        val parts = listOfNotNull(sentence.subject.text, sentence.verb.present, sentence.complement?.text)
        return parts.joinToString(" ").replaceFirstChar { it.titlecase() } + "."
    }

    private fun escapeCell(value: String): String =
        value.replace("|", "\\|").replace("\r", " ").replace("\n", " ")

    private const val REVIEW_SEED = 47
}
