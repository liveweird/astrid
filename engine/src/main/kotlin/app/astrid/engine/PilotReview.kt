package app.astrid.engine

object PilotReview {
    fun renderMarkdown(): String = renderSnapshot(PilotContent.currentSnapshot)

    fun renderPilotEightMarkdown(): String = renderSnapshot(PilotContent.pilotEightSnapshot)

    fun renderPilotSevenMarkdown(): String = renderSnapshot(PilotContent.pilotSevenSnapshot)

    fun renderPilotSixMarkdown(): String = renderSnapshot(PilotContent.pilotSixSnapshot)

    fun renderPilotFiveMarkdown(): String = renderSnapshot(PilotContent.pilotFiveSnapshot)

    fun renderPilotFourMarkdown(): String = renderSnapshot(PilotContent.pilotFourSnapshot)

    fun renderPilotThreeMarkdown(): String = renderSnapshot(PilotContent.pilotThreeSnapshot)

    fun renderLegacyMarkdown(): String = renderSnapshot(PilotContent.snapshot)

    fun renderPilotTwoMarkdown(): String = renderSnapshot(PilotContent.pilotTwoSnapshot)

    private fun renderSnapshot(snapshot: PilotContentSnapshot): String {
        return buildString {
            appendLine("# Astrid pilot content review")
            appendLine()
            appendLine("- Content revision: `${snapshot.contentRevision}`")
            appendLine("- Generator revision: `${snapshot.generatorRevision}`")
            appendLine("- Review state: `${PilotReviewMetadata.REVIEW_STATE}`")
            appendLine("- Provenance: ${reviewProvenance(snapshot)}")
            appendLine("- Source review: ${sourceReview(snapshot)}")
            appendLine("- Inventory: ${snapshot.lexicalReview.size} lexical records; ${snapshot.sentences.size} sentences")
            appendLine()
            appendLexicalInventory(snapshot.lexicalReview)
            appendSentenceInventory(snapshot.sentences)
            appendExamples(snapshot)
            if (snapshot.contentRevision in setOf(
                    PilotContent.RETAINED_FOUR_CONTENT_REVISION,
                    PilotContent.RETAINED_FIVE_CONTENT_REVISION,
                    PilotContent.RETAINED_SIX_CONTENT_REVISION,
                    PilotContent.RETAINED_SEVEN_CONTENT_REVISION,
                    PilotContent.RETAINED_EIGHT_CONTENT_REVISION,
                    PilotContent.CURRENT_CONTENT_REVISION,
                )
            ) {
                appendWritingInventory(snapshot)
                appendReadingInventory(snapshot)
            }
            if (snapshot.contentRevision in setOf(
                    PilotContent.RETAINED_FIVE_CONTENT_REVISION,
                    PilotContent.RETAINED_SIX_CONTENT_REVISION,
                    PilotContent.RETAINED_SEVEN_CONTENT_REVISION,
                    PilotContent.RETAINED_EIGHT_CONTENT_REVISION,
                    PilotContent.CURRENT_CONTENT_REVISION,
                )
            ) {
                appendWordChoiceInventory(snapshot)
                appendTileInventory(snapshot, "E012", "All-word sentence tiles")
                appendTileInventory(snapshot, "E013", "Finite-form sentence tiles")
            }
            if (snapshot.contentRevision in setOf(
                    PilotContent.RETAINED_SIX_CONTENT_REVISION,
                    PilotContent.RETAINED_SEVEN_CONTENT_REVISION,
                    PilotContent.RETAINED_EIGHT_CONTENT_REVISION,
                    PilotContent.CURRENT_CONTENT_REVISION,
                )
            ) {
                appendMatchingInventory(snapshot)
            }
            if (snapshot.contentRevision in setOf(
                    PilotContent.RETAINED_SEVEN_CONTENT_REVISION,
                    PilotContent.RETAINED_EIGHT_CONTENT_REVISION,
                    PilotContent.CURRENT_CONTENT_REVISION,
                )
            ) appendGapInventory(snapshot)
            if (snapshot.contentRevision in setOf(
                    PilotContent.RETAINED_EIGHT_CONTENT_REVISION,
                    PilotContent.CURRENT_CONTENT_REVISION,
                )
            ) {
                appendModuleOneProfileInventory(snapshot.contentRevision == PilotContent.CURRENT_CONTENT_REVISION)
                appendModuleOneInventory()
            }
            if (snapshot.contentRevision == PilotContent.CURRENT_CONTENT_REVISION) {
                appendInanimateReferenceInventory()
            }
        }
    }

    private fun reviewProvenance(snapshot: PilotContentSnapshot): String =
        if (snapshot.contentRevision == PilotContent.CURRENT_CONTENT_REVISION) {
            "docs/knowledge/danish-vocabulary-seed.md (retained seed) plus " +
                "docs/knowledge/module-one-language-pack.md (L024 packet), " +
                "docs/knowledge/module-one-activation.md (L025, C004 and Module 1 tasks), and " +
                "docs/knowledge/inanimate-reference-activation.md (L026 and G038 reference tasks; 2026-11-10)"
        } else if (snapshot.contentRevision == PilotContent.RETAINED_EIGHT_CONTENT_REVISION) {
            "docs/knowledge/danish-vocabulary-seed.md (retained seed) plus " +
                "docs/knowledge/module-one-language-pack.md (L024 packet) and " +
                "docs/knowledge/module-one-activation.md (L025, C004 and Module 1 tasks; 2026-11-09)"
        } else if (snapshot.contentRevision in setOf(
                PilotContent.RETAINED_FIVE_CONTENT_REVISION,
                PilotContent.RETAINED_SIX_CONTENT_REVISION,
                PilotContent.RETAINED_SEVEN_CONTENT_REVISION,
            )
        ) {
            "docs/knowledge/danish-vocabulary-seed.md (retained seed) plus " +
                "docs/knowledge/module-one-language-pack.md (L024 authoring packet, 2026-11-08)"
        } else {
            PilotReviewMetadata.PROVENANCE
        }

    private fun sourceReview(snapshot: PilotContentSnapshot): String =
        if (snapshot.contentRevision == PilotContent.CURRENT_CONTENT_REVISION) {
            "2026-11-07 retained seed; 2026-11-08 pair-five extension; " +
                "2026-11-09 L025/C004/Module 1 source checks; 2026-11-10 L026/G038 source checks; see " +
                "docs/knowledge/pilot-source-review.md, docs/knowledge/module-one-language-pack.md, " +
                "docs/knowledge/module-one-activation.md and docs/knowledge/inanimate-reference-activation.md"
        } else if (snapshot.contentRevision == PilotContent.RETAINED_EIGHT_CONTENT_REVISION) {
            "2026-11-07 retained seed; 2026-11-08 pair-five extension; " +
                "2026-11-09 L025/C004/Module 1 source checks; see docs/knowledge/pilot-source-review.md, " +
                "docs/knowledge/module-one-language-pack.md and docs/knowledge/module-one-activation.md"
        } else if (snapshot.contentRevision in setOf(
                PilotContent.RETAINED_FIVE_CONTENT_REVISION,
                PilotContent.RETAINED_SIX_CONTENT_REVISION,
                PilotContent.RETAINED_SEVEN_CONTENT_REVISION,
            )
        ) {
            "2026-11-07 retained seed; 2026-11-08 pair-five extension; see " +
                "docs/knowledge/pilot-source-review.md and docs/knowledge/module-one-language-pack.md"
        } else {
            PilotReviewMetadata.SOURCE_REVIEW_DATE
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

    private fun promptSummary(exercise: GeneratedExercise): String {
        val passage = exercise.prompt.passage
        val matchingColumns = exercise.prompt.matchingColumns
        val gapPrompt = exercise.prompt.gapPrompt
        return when {
            gapPrompt != null -> gapPrompt.segments.zip(gapPrompt.slots).joinToString("") { (segment, slot) ->
                segment + "[${slot.label}: ${slot.choices.joinToString("/") { it.text }}]"
            } + gapPrompt.segments.last()
            matchingColumns != null ->
                "English: " + matchingColumns.left.joinToString("; ") { "${it.id}:${it.text}" } +
                    " | Danish: " + matchingColumns.right.joinToString("; ") { "${it.id}:${it.text}" }
            passage != null ->
                passage.joinToString(" ") + " ${exercise.prompt.cue} " +
                    "[${exercise.prompt.choices.joinToString(", ")}]"
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
    }

    private fun StringBuilder.appendReadingInventory(snapshot: PilotContentSnapshot) {
        val engine = PilotEngine()
        appendLine()
        appendLine("## Short-reading teaching units")
        appendLine()
        appendLine("All 24 semantic units below use seed 0 and expose the complete passage for review.")
        appendLine()
        appendLine("| Unit | Passage | Question | Choices in display order | Answer |")
        appendLine("| ---: | --- | --- | --- | --- |")
        for (index in 0 until READING_UNIT_COUNT) {
            val exercise = engine.generate(
                GenerationRequest(
                    "E010",
                    null,
                    0,
                    index,
                    snapshot.contentRevision,
                    snapshot.generatorRevision,
                ),
            )
            appendLine(
                listOf(
                    (index + 1).toString(),
                    requireNotNull(exercise.prompt.passage).joinToString(" "),
                    requireNotNull(exercise.prompt.cue),
                    exercise.prompt.choices.joinToString("; "),
                    requireNotNull(exercise.inspector.readingSource).answer,
                ).joinToString(prefix = "| ", separator = " | ", postfix = " |", transform = ::escapeCell),
            )
        }
    }

    private fun StringBuilder.appendWritingInventory(snapshot: PilotContentSnapshot) {
        val engine = PilotEngine()
        appendLine()
        appendLine("## Sentence-writing teaching units")
        appendLine()
        appendLine("All 99 licensed writing units below use seed 0 and expose the complete teaching prompt for review.")
        appendLine()
        appendLine("| Unit | Canonical ID | English cue | Optional language reference | Hint |")
        appendLine("| ---: | --- | --- | --- | --- |")
        for (index in 0 until WRITING_UNIT_COUNT) {
            val exercise = engine.generate(
                GenerationRequest(
                    "E009",
                    "G005",
                    0,
                    index,
                    snapshot.contentRevision,
                    snapshot.generatorRevision,
                ),
            )
            val reference = exercise.introduction.joinToString("; ") { section ->
                "${section.title}: ${section.body} [${section.bodyLanguage.orEmpty()}]"
            }
            appendLine(
                listOf(
                    (index + 1).toString(),
                    requireNotNull(exercise.inspector.canonicalSentenceId),
                    requireNotNull(exercise.prompt.cue),
                    reference,
                    exercise.hint,
                ).joinToString(prefix = "| ", separator = " | ", postfix = " |", transform = ::escapeCell),
            )
        }
    }

    private fun StringBuilder.appendWordChoiceInventory(snapshot: PilotContentSnapshot) {
        val engine = PilotEngine()
        appendLine()
        appendLine("## Danish word-choice teaching units")
        appendLine()
        appendLine("| Unit | English cue | Choices in display order | Answer |")
        appendLine("| ---: | --- | --- | --- |")
        for (index in 0 until WORD_CHOICE_UNIT_COUNT) {
            val exercise = engine.generate(
                GenerationRequest("E011", null, REVIEW_SEED, index, snapshot.contentRevision, snapshot.generatorRevision),
            )
            val key = exercise.source.answerKey as AnswerKey.SuppliedChoice
            appendLine(
                listOf(
                    (index + 1).toString(),
                    requireNotNull(exercise.prompt.cue),
                    exercise.prompt.choices.joinToString("; "),
                    key.expected,
                ).joinToString(prefix = "| ", separator = " | ", postfix = " |", transform = ::escapeCell),
            )
        }
    }

    private fun StringBuilder.appendTileInventory(
        snapshot: PilotContentSnapshot,
        formatId: String,
        heading: String,
    ) {
        val engine = PilotEngine()
        val target = snapshot.formats.single { it.id == formatId }.targetSkillId
        appendLine()
        appendLine("## $heading")
        appendLine()
        appendLine("| Unit | English cue | Tiles in display order | Licensed orders | Distractors |")
        appendLine("| ---: | --- | --- | --- | --- |")
        for (index in 0 until TILE_UNIT_COUNT) {
            val exercise = engine.generate(
                GenerationRequest(formatId, target, REVIEW_SEED, index, snapshot.contentRevision, snapshot.generatorRevision),
            )
            val source = requireNotNull(exercise.inspector.tileSource)
            val textById = exercise.prompt.blocks.associate { it.id to it.text }
            appendLine(
                listOf(
                    (index + 1).toString(),
                    requireNotNull(exercise.prompt.cue),
                    exercise.prompt.blocks.joinToString("; ") { "${it.id}:${it.text}" },
                    source.licensedOrders.joinToString("; ") { order ->
                        "${order.kind}=" + order.blockIds.joinToString(" ") { textById.getValue(it) }
                    },
                    source.distractors.joinToString("; ") { "${it.blockId}:${it.rationale}" },
                ).joinToString(prefix = "| ", separator = " | ", postfix = " |", transform = ::escapeCell),
            )
        }
    }

    private fun StringBuilder.appendMatchingInventory(snapshot: PilotContentSnapshot) {
        val engine = PilotEngine()
        appendLine()
        appendLine("## English–Danish matching boards")
        appendLine()
        appendLine("| Unit | English column | Danish column | Private correspondences |")
        appendLine("| ---: | --- | --- | --- |")
        for (index in 0 until MATCHING_UNIT_COUNT) {
            val exercise = engine.generate(
                GenerationRequest("E014", null, REVIEW_SEED, index, snapshot.contentRevision, snapshot.generatorRevision),
            )
            val columns = requireNotNull(exercise.prompt.matchingColumns)
            val source = requireNotNull(exercise.inspector.matchingSource)
            appendLine(
                listOf(
                    (index + 1).toString(),
                    columns.left.joinToString("; ") { "${it.id}:${it.text}" },
                    columns.right.joinToString("; ") { "${it.id}:${it.text}" },
                    source.pairs.joinToString("; ") { "${it.leftId}->${it.rightId}:${it.lexicalSource.senseId}" },
                ).joinToString(prefix = "| ", separator = " | ", postfix = " |", transform = ::escapeCell),
            )
        }
    }

    private fun StringBuilder.appendGapInventory(snapshot: PilotContentSnapshot) {
        val engine = PilotEngine()
        appendLine()
        appendLine("## Present-verb and article gap units")
        appendLine()
        appendLine("| Unit | Segments | Displayed slots | Private expected choices |")
        appendLine("| ---: | --- | --- | --- |")
        for (index in 0 until GAP_UNIT_COUNT) {
            val exercise = engine.generate(
                GenerationRequest("E015", "G004", REVIEW_SEED, index, snapshot.contentRevision, snapshot.generatorRevision),
            )
            val prompt = requireNotNull(exercise.prompt.gapPrompt)
            val source = requireNotNull(exercise.inspector.gapSource)
            appendLine(
                listOf(
                    (index + 1).toString(),
                    prompt.segments.joinToString(" | "),
                    prompt.slots.joinToString("; ") { slot ->
                        "${slot.id}:${slot.label}=" + slot.choices.joinToString("/") { "${it.id}:${it.text}" }
                    },
                    source.slots.joinToString("; ") { "${it.slotId}->${it.expectedChoiceId}:${it.expectedForm}" },
                ).joinToString(prefix = "| ", separator = " | ", postfix = " |", transform = ::escapeCell),
            )
        }
    }

    private fun StringBuilder.appendModuleOneInventory() {
        appendLine()
        appendLine("## Module 1 authored task inventory")
        appendLine()
        appendLine("Selection and recall use explicitly different authored situations. Danish answers remain teaching drafts.")
        appendLine()
        appendLine(
            "| Task | Format | Instruction | Scope | Cue | Context | Frame | Reference role | Case policy | Expected | Alternatives | " +
                "Introduction | Hint | Answer exposure | Prompt exposure | Lesson exposure | Hint exposure | " +
                "Feedback exposure | Choices and rationales |",
        )
        appendLine(
            "| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |",
        )
        ModuleOneContent.tasks.forEach { task ->
            appendLine(
                listOf(
                    task.id,
                    task.formatId,
                    ModuleOneContent.instruction(task.formatId),
                    task.targetScopeId,
                    task.cue,
                    task.context,
                    listOf(task.before, "___", task.after, task.sentenceGloss).joinToString(" "),
                    task.referenceRole.orEmpty(),
                    task.casePolicy,
                    task.expectedForm,
                    task.knownAlternativeForms.joinToString(", "),
                    task.introduction.joinToString("; ") { section ->
                        "${section.title} [${section.bodyLanguage ?: "en"}]: ${section.body}"
                    },
                    task.hint,
                    task.answerExposureKeys.joinToString(", "),
                    task.promptExposureKeys.joinToString(", "),
                    task.lessonExposureKeys.joinToString(", "),
                    task.hintExposureKeys.joinToString(", "),
                    task.feedbackExposureKeys.joinToString(", "),
                    task.choiceRationales.joinToString("; ") { "${it.choice}: ${it.rationale}" },
                ).joinToString(prefix = "| ", separator = " | ", postfix = " |", transform = ::escapeCell),
            )
        }
        appendLine()
    }

    private fun StringBuilder.appendModuleOneProfileInventory(includeInanimateReference: Boolean) {
        val planner = PracticePlanner()
        appendLine()
        appendLine("## Module 1 mandatory profile introductions")
        appendLine()
        appendLine(
            if (includeInanimateReference) {
                "Each pinned profile includes its complete mandatory introduction: six items for the earlier profiles, four for S04."
            } else {
                "Each pinned profile below includes the complete mandatory introduction shown before its six planned items."
            },
        )
        appendLine()
        appendLine(
            "| Profile | Plan revision | Title | Prerequisites | Formats and scopes | Introduction required | Completion policy | " +
                "Exposure policy | Introduction exposure | Full introduction |",
        )
        appendLine("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |")
        ModuleOnePracticePlanner.SPECIFICATIONS.forEach { specification ->
            val plan = planner.plan(specification.focusId, specification.planRevision, REVIEW_SEED)
            val formatsAndScopes = plan.items
                .map { item -> "${item.exercise.formatId}:${item.targetDescriptor.targetScopeId}" }
                .distinct()
                .joinToString(", ")
            val introduction = plan.introduction.joinToString("; ") { section ->
                "${section.title} [${section.bodyLanguage ?: "en"}]: ${section.body}"
            }
            appendLine(
                listOf(
                    plan.focusId,
                    plan.planRevision,
                    plan.title,
                    plan.prerequisiteSkillIds.joinToString(", "),
                    formatsAndScopes,
                    plan.introductionRequired.toString(),
                    plan.completionRuleId.orEmpty(),
                    plan.exposurePolicyId.orEmpty(),
                    plan.introductionExposureKeys.orEmpty().joinToString(", "),
                    introduction,
                ).joinToString(prefix = "| ", separator = " | ", postfix = " |", transform = ::escapeCell),
            )
        }
        if (!includeInanimateReference) {
            appendLine()
            return
        }
        val inanimatePlan = planner.plan(
            InanimateReferencePracticePlanner.FOCUS_ID,
            InanimateReferencePracticePlanner.PLAN_REVISION,
            REVIEW_SEED,
        )
        appendLine(
            listOf(
                inanimatePlan.focusId,
                inanimatePlan.planRevision,
                inanimatePlan.title,
                inanimatePlan.prerequisiteSkillIds.joinToString(", "),
                inanimatePlan.items.map { item ->
                    "${item.exercise.formatId}:${item.targetDescriptor.targetScopeId}"
                }.distinct().joinToString(", "),
                inanimatePlan.introductionRequired.toString(),
                inanimatePlan.completionRuleId.orEmpty(),
                inanimatePlan.exposurePolicyId.orEmpty(),
                inanimatePlan.introductionExposureKeys.orEmpty().joinToString(", "),
                inanimatePlan.introduction.joinToString("; ") { section ->
                    "${section.title} [${section.bodyLanguage ?: "en"}]: ${section.body}"
                },
            ).joinToString(prefix = "| ", separator = " | ", postfix = " |", transform = ::escapeCell),
        )
        appendLine()
    }

    private fun StringBuilder.appendInanimateReferenceInventory() {
        appendLine()
        appendLine("## Inanimate singular reference task inventory")
        appendLine()
        appendLine(
            "| Task | Format | Target entry | Target sense | Expected | Antecedent sense | Article | Gender | Number | " +
                "Requested role | Case policy | Context variant | Context | Choices and rationales |",
        )
        appendLine("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |")
        InanimateReferenceContent.tasks.forEach { task ->
            appendLine(
                listOf(
                    task.id,
                    task.formatId,
                    task.targetEntryId,
                    task.targetSenseId,
                    task.expectedForm,
                    task.antecedentSenseId,
                    task.antecedentArticle,
                    task.antecedentGender,
                    task.antecedentNumber,
                    task.requestedRole,
                    task.casePolicy,
                    task.contextVariantId,
                    task.context,
                    task.choiceRationales.joinToString("; ") { "${it.choice}: ${it.rationale}" },
                ).joinToString(prefix = "| ", separator = " | ", postfix = " |", transform = ::escapeCell),
            )
        }
        appendLine()
    }

    private fun realize(sentence: Sentence): String {
        val parts = listOfNotNull(sentence.subject.text, sentence.verb.present, sentence.complement?.text)
        return parts.joinToString(" ").replaceFirstChar { it.titlecase() } + "."
    }

    private fun escapeCell(value: String): String =
        value.replace("|", "\\|").replace("\r", " ").replace("\n", " ")

    private const val REVIEW_SEED = 47
    private const val READING_UNIT_COUNT = 24
    private const val WRITING_UNIT_COUNT = 99
    private const val WORD_CHOICE_UNIT_COUNT = 4
    private const val TILE_UNIT_COUNT = 99
    private const val MATCHING_UNIT_COUNT = 4
    private const val GAP_UNIT_COUNT = 92
}
