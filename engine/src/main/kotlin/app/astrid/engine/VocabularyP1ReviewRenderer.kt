package app.astrid.engine

internal object VocabularyP1ReviewRenderer {
    fun appendTo(output: StringBuilder, seed: Int, snapshot: PilotContentSnapshot) {
        val engine = PilotEngine()
        output.appendLine("## P1 vocabulary unit: A day of work and study")
        output.appendLine()
        val evaluationRevision = PilotEngine().evaluationRevision(snapshot.contentRevision, snapshot.generatorRevision)
        output.appendLine("- Pair: `${snapshot.contentRevision}` / `${snapshot.generatorRevision}` / `$evaluationRevision`")
        output.appendLine("- Review state: `${PilotReviewMetadata.REVIEW_STATE}`")
        output.appendLine("- Source provenance: `docs/knowledge/vocabulary-course-blueprint.md` and official COR locators")
        output.appendLine("- Inventory: 18 selected placements; 9 new lexical records; 23 literal activities")
        output.appendLine("- Deterministic variation: choice, column and tile positions only; no additional semantic claims")
        output.appendLine()
        output.appendLine("### Typed lexical sources")
        output.appendLine()
        VocabularyP1Content.placements.forEach { lexeme ->
            output.appendLine(
                "- `${lexeme.entryId}` / `${lexeme.senseId}` · `${lexeme.kind.wireValue}` · " +
                    "`${lexeme.display}` · ${lexeme.gloss} · ${lexeme.selectedUse}",
            )
        }
        output.appendLine()
        output.appendLine("### Literal activities and private grading sources")
        output.appendLine()
        VocabularyP1Content.tasks.groupBy(VocabularyP1Task::formatId).forEach { (formatId, tasks) ->
            output.appendLine("#### $formatId (${tasks.size} semantic task${if (tasks.size == 1) "" else "s"})")
            output.appendLine()
            tasks.forEach { task ->
                val exercise = generateTask(engine, task, seed, snapshot)
                val source = requireNotNull(exercise.inspector.vocabularySource)
                output.appendLine("- `${task.id}` · cue: ${task.cue}")
                if (task.passage.isNotEmpty()) output.appendLine("  - Passage: ${task.passage.joinToString(" ")}")
                if (task.choices.isNotEmpty()) output.appendLine("  - Ordered bank: ${task.choices.joinToString(" | ")}")
                source.expectedAnswer?.let { output.appendLine("  - Expected: `$it`") }
                if (source.matchingPairs.isNotEmpty()) {
                    output.appendLine("  - Matching rows: ${source.matchingPairs.joinToString { it.lexicalSource.senseId }}")
                }
                if (source.tileOccurrences.isNotEmpty()) {
                    output.appendLine("  - Occurrences: ${source.tileOccurrences.joinToString { "${it.blockId}:${it.form}" }}")
                    output.appendLine("  - Accepted ID orders: ${source.acceptedOrders.joinToString { it.joinToString(" ") }}")
                }
                appendFacts(output, source)
                appendPrivateGrading(output, source)
            }
            output.appendLine()
        }
        output.appendLine("### Five required-teaching profiles")
        output.appendLine()
        val specifications = if (snapshot.contentRevision == PilotContent.RETAINED_SEVENTEEN_CONTENT_REVISION) {
            VocabularyP1PracticePlanner.RETAINED_SPECIFICATIONS
        } else {
            VocabularyP1PracticePlanner.SPECIFICATIONS
        }
        specifications.forEach { specification ->
            val plan = PracticePlanner(engine).plan(specification.focusId, specification.planRevision, seed)
            output.appendLine(
                "- `${plan.focusId}` / `${plan.planRevision}` · ${plan.title} · ${plan.items.size} activities · " +
                    "completion `${plan.completionRuleId}` · exposure `${plan.exposurePolicyId}`",
            )
            plan.introduction.forEach { section ->
                output.appendLine("  - ${section.title}: `${section.body}` (${section.bodyLanguage ?: "en"})")
            }
            output.appendLine("  - Introduction exposure: ${plan.introductionExposureKeys.orEmpty().joinToString(" | ")}")
        }
        output.appendLine()
    }

    private fun appendFacts(output: StringBuilder, source: VocabularyExerciseSourceDetails) {
        source.supportFacts.forEach { fact ->
            val target = if (fact.factId == source.targetSupportFactId) " (question target)" else ""
            output.appendLine(
                "  - Fact `${fact.factId}`$target: ${fact.referentId} / ${fact.predicateId} / " +
                    "${fact.valueRole}:${fact.valueId} · ${fact.sentence}",
            )
        }
    }

    private fun appendPrivateGrading(output: StringBuilder, source: VocabularyExerciseSourceDetails) {
        source.normalizationPolicy?.let { output.appendLine("  - Normalization: `$it`") }
        if (source.knownAlternatives.isNotEmpty()) {
            output.appendLine("  - Known alternatives:")
            source.knownAlternatives.forEach { alternative ->
                output.appendLine(
                    "    - `${alternative.text}` · ${alternative.targetStatus.wireValue} · " +
                        "${alternative.linguisticStatus.wireValue} · ${alternative.meaningSatisfaction.wireValue} · " +
                        "${alternative.responseConstraintStatus.wireValue} · ${alternative.issueCode}",
                )
            }
        }
        if (source.reviewedOrders.isNotEmpty()) {
            output.appendLine("  - Reviewed orders:")
            source.reviewedOrders.forEach { order ->
                output.appendLine(
                    "    - `${order.words.joinToString(" ")}` · ${order.targetStatus.wireValue} · " +
                        "${order.linguisticStatus.wireValue} · ${order.meaningSatisfaction.wireValue} · " +
                        "${order.responseConstraintStatus.wireValue} · ${order.issueCode}",
                )
            }
        }
    }

    private fun generateTask(
        engine: PilotEngine,
        task: VocabularyP1Task,
        seed: Int,
        snapshot: PilotContentSnapshot,
    ): GeneratedExercise =
        (0 until VocabularyP1Content.tasks(task.formatId).size).asSequence().map { index ->
            engine.generate(
                GenerationRequest(
                    task.formatId,
                    task.targetSkillId,
                    seed,
                    index,
                    snapshot.contentRevision,
                    snapshot.generatorRevision,
                ),
            )
        }.first { requireNotNull(it.inspector.vocabularySource).taskId == task.id }
}
