package app.astrid.server.exercises

import app.astrid.engine.Answer
import app.astrid.engine.ExerciseInspector
import app.astrid.engine.ExercisePrompt
import app.astrid.engine.GeneratedExercise
import app.astrid.engine.IntroductionSection
import app.astrid.engine.LexicalTarget
import app.astrid.engine.ReadingTask
import java.security.MessageDigest

internal fun GeneratedExercise.toResponse(id: String) = ExerciseResponse(
    id = id,
    formatId = formatId,
    targetSkillId = targetSkillId,
    lexicalTarget = lexicalTarget?.let { LexicalTargetDto(it.entryId, it.senseId, it.facet, it.direction) },
    supportSkillIds = supportSkillIds,
    contentRevision = contentRevision,
    generatorRevision = generatorRevision,
    instruction = instruction,
    context = context,
    prompt = ExercisePromptDto(
        before = prompt.before,
        after = prompt.after,
        choices = prompt.choices,
        blocks = prompt.blocks.map { PromptBlockDto(it.id, it.text) },
        punctuation = prompt.punctuation,
        inputLabel = prompt.inputLabel,
        replacementWord = prompt.replacementWord,
        cue = prompt.cue,
        cueLanguage = prompt.cueLanguage,
        answerLanguage = prompt.answerLanguage,
        passage = prompt.passage,
        tileMode = prompt.tileMode,
        requiredTileCount = prompt.requiredTileCount,
        matchingColumns = prompt.matchingColumns?.let { columns ->
            MatchingColumnsDto(
                left = columns.left.map { MatchingItemDto(it.id, it.text) },
                right = columns.right.map { MatchingItemDto(it.id, it.text) },
            )
        },
        gapPrompt = prompt.gapPrompt?.let { gapPrompt ->
            GapPromptDto(
                segments = gapPrompt.segments,
                slots = gapPrompt.slots.map { slot ->
                    GapSlotDto(
                        id = slot.id,
                        label = slot.label,
                        targetSkillId = slot.targetSkillId,
                        assessmentRole = slot.assessmentRole.wireValue,
                        choices = slot.choices.map { GapChoiceDto(it.id, it.text) },
                    )
                },
            )
        },
    ),
    introduction = introduction.map { IntroductionDto(it.title, it.body, it.bodyLanguage) },
    hint = hint,
    readingTarget = readingTarget?.let { ReadingTaskDto(it.facet) },
    lexicalTargets = lexicalTargets?.map { LexicalTargetDto(it.entryId, it.senseId, it.facet, it.direction) },
    targetScopeId = targetScopeId,
)

internal fun AttemptAnswerDto.toEngineAnswer(): Answer = when (this) {
    is AttemptAnswerDto.Text -> Answer.Text(text)
    is AttemptAnswerDto.Order -> Answer.Order(blockIds)
    is AttemptAnswerDto.Matching -> Answer.Matching(pairs.map { app.astrid.engine.MatchingPair(it.leftId, it.rightId) })
    is AttemptAnswerDto.Gaps -> Answer.Gaps(selections.map { app.astrid.engine.GapSelection(it.slotId, it.choiceId) })
}

internal fun GeneratedExercise.toInspector(id: String) = ExerciseInspectorDto(
    exercise = toResponse(id),
    seed = reproduction.seed,
    index = reproduction.index,
    canonicalSentenceId = inspector.canonicalSentenceId,
    constructionId = inspector.constructionId,
    lexicalIds = inspector.lexicalIds,
    clauseRoles = inspector.clauseRoles,
    canonicalSentence = inspector.canonicalSentence,
    lexicalSource = inspector.lexicalSource?.let {
        LexicalSourceDto(it.entryId, it.senseId, it.headword, it.gloss, it.article)
    },
    readingSource = inspector.readingSource?.let { source ->
        ReadingSourceDetailsDto(
            propositions = source.propositions.map {
                ReadingPropositionDetailsDto(it.sentenceId, it.subjectId, it.verbId, it.objectId)
            },
            questionSubjectId = source.questionSubjectId,
            questionVerbId = source.questionVerbId,
            answerNounId = source.answerNounId,
            answer = source.answer,
        )
    },
    tileSource = inspector.tileSource?.let { source ->
        TileSourceDetailsDto(
            tokens = source.tokens.map { TileTokenDetailsDto(it.blockId, it.lexicalId, it.form) },
            licensedOrders = source.licensedOrders.map { TileLicensedOrderDetailsDto(it.kind, it.blockIds) },
            distractors = source.distractors.map { TileDistractorDetailsDto(it.blockId, it.rationale) },
        )
    },
    matchingSource = inspector.matchingSource?.let { source ->
        MatchingSourceDetailsDto(
            source.pairs.map { pair ->
                MatchingPairSourceDetailsDto(
                    pair.leftId,
                    pair.rightId,
                    pair.lexicalSource.let {
                        LexicalSourceDto(it.entryId, it.senseId, it.headword, it.gloss, it.article)
                    },
                )
            },
        )
    },
    gapSource = inspector.gapSource?.let { source ->
        GapSourceDto(
            slots = source.slots.map { slot ->
                GapSlotSourceDto(
                    slotId = slot.slotId,
                    expectedChoiceId = slot.expectedChoiceId,
                    lexicalId = slot.lexicalId,
                    expectedForm = slot.expectedForm,
                    alternativeForm = slot.alternativeForm,
                )
            },
        )
    },
    moduleOneSource = inspector.moduleOneSource?.let { source ->
        ModuleOneSourceDetailsDto(
            source.taskId,
            source.targetLexicalId,
            source.expectedForm,
            source.knownAlternativeForms,
            source.referenceRole,
            source.frameVariantId,
            source.casePolicy,
            source.choiceRationales.map { ModuleOneChoiceRationaleDto(it.choice, it.rationale) },
        )
    },
    inanimateReferenceSource = inspector.inanimateReferenceSource?.toDto(),
)

private fun app.astrid.engine.InanimateReferenceSourceDetails.toDto() = InanimateReferenceSourceDetailsDto(
    taskId = taskId,
    targetEntryId = targetEntryId,
    targetSenseId = targetSenseId,
    expectedForm = expectedForm,
    antecedentEntryId = antecedentEntryId,
    antecedentSenseId = antecedentSenseId,
    antecedentArticle = antecedentArticle,
    antecedentGender = antecedentGender,
    antecedentNumber = antecedentNumber,
    requestedRole = requestedRole,
    casePolicy = casePolicy,
    contextVariantId = contextVariantId,
    knownAlternativeForms = knownAlternativeForms,
    choiceRationales = choiceRationales.map { ModuleOneChoiceRationaleDto(it.choice, it.rationale) },
)

internal fun GeneratedExercise.reproductionFingerprint(
    promptForFingerprint: ExercisePrompt = prompt,
    lexicalTargetForFingerprint: LexicalTarget? = lexicalTarget,
    inspectorForFingerprint: ExerciseInspector = inspector,
    supportSkillIdsForFingerprint: List<String> = supportSkillIds,
    readingTargetForFingerprint: ReadingTask? = readingTarget,
    introductionForFingerprint: List<IntroductionSection> = introduction,
    lexicalTargetsForFingerprint: List<LexicalTarget>? = lexicalTargets,
    targetScopeIdForFingerprint: String? = targetScopeId,
): String {
    val legacyCanonical = legacyFingerprintCanonical(promptForFingerprint, inspectorForFingerprint)
    val pairTwoCanonical = legacyCanonical +
        listOf(promptForFingerprint.inputLabel.orEmpty(), promptForFingerprint.replacementWord.orEmpty())
    val pairThreeCanonical = pairThreeFingerprintCanonical(
        pairTwoCanonical,
        promptForFingerprint,
        lexicalTargetForFingerprint,
        inspectorForFingerprint,
        supportSkillIdsForFingerprint,
    )
    val canonical = when (contentRevision to generatorRevision) {
        CONTENT_REVISION to GENERATOR_REVISION -> legacyCanonical.joinToString("\u001f")
        "pilot-2" to "generator-2" -> pairTwoCanonical.joinToString("\u001f")
        "pilot-3" to "generator-3" -> pairThreeCanonical.joinToString("\u001f")
        "pilot-4" to "generator-4" -> pairFourFingerprintCanonical(
            promptForFingerprint,
            lexicalTargetForFingerprint,
            inspectorForFingerprint,
            supportSkillIdsForFingerprint,
            readingTargetForFingerprint,
            introductionForFingerprint,
        )
        "pilot-5" to "generator-5" -> pairFiveFingerprintCanonical(
            promptForFingerprint,
            lexicalTargetForFingerprint,
            inspectorForFingerprint,
            supportSkillIdsForFingerprint,
            readingTargetForFingerprint,
            introductionForFingerprint,
        )
        "pilot-6" to "generator-6" -> pairSixFingerprintCanonical(
            promptForFingerprint,
            lexicalTargetForFingerprint,
            inspectorForFingerprint,
            supportSkillIdsForFingerprint,
            readingTargetForFingerprint,
            introductionForFingerprint,
            lexicalTargetsForFingerprint,
        )
        "pilot-7" to "generator-7" -> pairSevenFingerprintCanonical(
            promptForFingerprint,
            lexicalTargetForFingerprint,
            inspectorForFingerprint,
            supportSkillIdsForFingerprint,
            readingTargetForFingerprint,
            introductionForFingerprint,
            lexicalTargetsForFingerprint,
        )
        "pilot-8" to "generator-8" -> pairEightFingerprintCanonical(
            promptForFingerprint,
            lexicalTargetForFingerprint,
            inspectorForFingerprint,
            supportSkillIdsForFingerprint,
            readingTargetForFingerprint,
            introductionForFingerprint,
            lexicalTargetsForFingerprint,
            targetScopeIdForFingerprint,
            inspectorForFingerprint.moduleOneSource,
        )
        "pilot-9" to "generator-9", "pilot-10" to "generator-10" -> pairNineFingerprintCanonical(
            promptForFingerprint,
            lexicalTargetForFingerprint,
            inspectorForFingerprint,
            supportSkillIdsForFingerprint,
            readingTargetForFingerprint,
            introductionForFingerprint,
            lexicalTargetsForFingerprint,
            targetScopeIdForFingerprint,
        ).let { if (contentRevision == "pilot-10") framedList(listOf("pair-ten", it)) else it }
        else -> error("Unsupported fingerprint revision pair: $contentRevision/$generatorRevision")
    }
    return MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

private fun pairThreeFingerprintCanonical(
    pairTwoCanonical: List<String>,
    prompt: ExercisePrompt,
    target: LexicalTarget?,
    inspector: ExerciseInspector,
    supportSkillIds: List<String>,
): List<String> = pairTwoCanonical + listOf(
    target?.entryId.orEmpty(),
    target?.senseId.orEmpty(),
    target?.facet.orEmpty(),
    target?.direction.orEmpty(),
    prompt.cue.orEmpty(),
    prompt.cueLanguage.orEmpty(),
    prompt.answerLanguage.orEmpty(),
    inspector.lexicalSource?.entryId.orEmpty(),
    inspector.lexicalSource?.senseId.orEmpty(),
    inspector.lexicalSource?.headword.orEmpty(),
    inspector.lexicalSource?.gloss.orEmpty(),
    inspector.lexicalSource?.article.orEmpty(),
    supportSkillIds.joinToString(","),
)

private fun GeneratedExercise.legacyFingerprintCanonical(
    prompt: ExercisePrompt,
    inspector: ExerciseInspector,
): List<String> = listOf(
    formatId,
    targetSkillId.orEmpty(),
    contentRevision,
    generatorRevision,
    reproduction.seed.toString(),
    reproduction.index.toString(),
    inspector.canonicalSentenceId.orEmpty(),
    inspector.constructionId.orEmpty(),
    inspector.lexicalIds.joinToString(","),
    inspector.clauseRoles.joinToString(","),
    inspector.canonicalSentence.orEmpty(),
    instruction,
    context,
    prompt.before,
    prompt.after,
    prompt.choices.joinToString(","),
    prompt.blocks.joinToString(",") { "${it.id}:${it.text}" },
    prompt.punctuation,
    introduction.joinToString("\u001e") { "${it.title}:${it.body}" },
    hint,
)

private fun GeneratedExercise.pairFourFingerprintCanonical(
    prompt: ExercisePrompt,
    lexicalTarget: LexicalTarget?,
    inspector: ExerciseInspector,
    supportSkillIds: List<String>,
    readingTarget: ReadingTask?,
    introduction: List<IntroductionSection>,
): String = framedList(
    listOf(
        formatId,
        framedNullable(targetSkillId),
        contentRevision,
        generatorRevision,
        reproduction.seed.toString(),
        reproduction.index.toString(),
        framedNullable(inspector.canonicalSentenceId),
        framedNullable(inspector.constructionId),
        framedList(inspector.lexicalIds),
        framedList(inspector.clauseRoles),
        framedNullable(inspector.canonicalSentence),
        instruction,
        context,
        prompt.before,
        prompt.after,
        framedList(prompt.choices),
        framedList(prompt.blocks.map { framedList(listOf(it.id, it.text)) }),
        prompt.punctuation,
        framedList(introduction.map { framedList(listOf(it.title, it.body, framedNullable(it.bodyLanguage))) }),
        hint,
        framedNullable(prompt.inputLabel),
        framedNullable(prompt.replacementWord),
        framedNullable(lexicalTarget?.let {
            framedList(listOf(it.entryId, it.senseId, it.facet, it.direction))
        }),
        framedNullable(prompt.cue),
        framedNullable(prompt.cueLanguage),
        framedNullable(prompt.answerLanguage),
        framedNullable(inspector.lexicalSource?.let {
            framedList(listOf(it.entryId, it.senseId, it.headword, it.gloss, it.article))
        }),
        framedList(supportSkillIds),
        framedNullable(readingTarget?.let { framedList(listOf(it.facet)) }),
        framedNullable(prompt.passage?.let(::framedList)),
        framedNullable(inspector.readingSource?.let { source ->
            framedList(
                listOf(
                    framedList(source.propositions.map { proposition ->
                        framedList(
                            listOf(
                                proposition.sentenceId,
                                proposition.subjectId,
                                proposition.verbId,
                                proposition.objectId,
                            ),
                        )
                    }),
                    source.questionSubjectId,
                    source.questionVerbId,
                    source.answerNounId,
                    source.answer,
                ),
            )
        }),
    ),
)

private fun GeneratedExercise.pairFiveFingerprintCanonical(
    prompt: ExercisePrompt,
    lexicalTarget: LexicalTarget?,
    inspector: ExerciseInspector,
    supportSkillIds: List<String>,
    readingTarget: ReadingTask?,
    introduction: List<IntroductionSection>,
): String = framedList(
    listOf(
        pairFourFingerprintCanonical(prompt, lexicalTarget, inspector, supportSkillIds, readingTarget, introduction),
        framedNullable(prompt.tileMode),
        framedNullable(prompt.requiredTileCount?.toString()),
        framedNullable(inspector.tileSource?.let { source ->
            framedList(
                listOf(
                    framedList(source.tokens.map { token ->
                        framedList(listOf(token.blockId, token.lexicalId, token.form))
                    }),
                    framedList(source.licensedOrders.map { order ->
                        framedList(listOf(order.kind, framedList(order.blockIds)))
                    }),
                    framedList(source.distractors.map { distractor ->
                        framedList(listOf(distractor.blockId, distractor.rationale))
                    }),
                ),
            )
        }),
    ),
)

private fun GeneratedExercise.pairSixFingerprintCanonical(
    prompt: ExercisePrompt,
    lexicalTarget: LexicalTarget?,
    inspector: ExerciseInspector,
    supportSkillIds: List<String>,
    readingTarget: ReadingTask?,
    introduction: List<IntroductionSection>,
    lexicalTargets: List<LexicalTarget>?,
): String = framedList(
    listOf(
        pairFiveFingerprintCanonical(prompt, lexicalTarget, inspector, supportSkillIds, readingTarget, introduction),
        framedNullable(lexicalTargets?.let { targets ->
            framedList(targets.map { target ->
                framedList(listOf(target.entryId, target.senseId, target.facet, target.direction))
            })
        }),
        framedNullable(prompt.matchingColumns?.let { columns ->
            framedList(
                listOf(
                    framedList(columns.left.map { item -> framedList(listOf(item.id, item.text)) }),
                    framedList(columns.right.map { item -> framedList(listOf(item.id, item.text)) }),
                ),
            )
        }),
        framedNullable(inspector.matchingSource?.let { source ->
            framedList(source.pairs.map { pair ->
                framedList(
                    listOf(
                        pair.leftId,
                        pair.rightId,
                        pair.lexicalSource.entryId,
                        pair.lexicalSource.senseId,
                        pair.lexicalSource.headword,
                        pair.lexicalSource.gloss,
                        pair.lexicalSource.article,
                    ),
                )
            })
        }),
    ),
)

private fun GeneratedExercise.pairSevenFingerprintCanonical(
    prompt: ExercisePrompt,
    lexicalTarget: LexicalTarget?,
    inspector: ExerciseInspector,
    supportSkillIds: List<String>,
    readingTarget: ReadingTask?,
    introduction: List<IntroductionSection>,
    lexicalTargets: List<LexicalTarget>?,
): String = framedList(
    listOf(
        pairSixFingerprintCanonical(
            prompt,
            lexicalTarget,
            inspector,
            supportSkillIds,
            readingTarget,
            introduction,
            lexicalTargets,
        ),
        framedNullable(prompt.gapPrompt?.let { gapPrompt ->
            framedList(
                listOf(
                    framedList(gapPrompt.segments),
                    framedList(gapPrompt.slots.map { slot ->
                        framedList(
                            listOf(
                                slot.id,
                                slot.label,
                                slot.targetSkillId,
                                slot.assessmentRole.wireValue,
                                framedList(slot.choices.map { choice ->
                                    framedList(listOf(choice.id, choice.text))
                                }),
                            ),
                        )
                    }),
                ),
            )
        }),
        framedNullable(inspector.gapSource?.let { source ->
            framedList(source.slots.map { slot ->
                framedList(
                    listOf(
                        slot.slotId,
                        slot.expectedChoiceId,
                        slot.lexicalId,
                        slot.expectedForm,
                        slot.alternativeForm,
                    ),
                )
            })
        }),
    ),
)

private fun GeneratedExercise.pairEightFingerprintCanonical(
    prompt: ExercisePrompt,
    lexicalTarget: LexicalTarget?,
    inspector: ExerciseInspector,
    supportSkillIds: List<String>,
    readingTarget: ReadingTask?,
    introduction: List<IntroductionSection>,
    lexicalTargets: List<LexicalTarget>?,
    targetScopeId: String?,
    moduleOneSource: app.astrid.engine.ModuleOneSourceDetails?,
): String = framedList(
    listOf(
        pairSevenFingerprintCanonical(
            prompt,
            lexicalTarget,
            inspector,
            supportSkillIds,
            readingTarget,
            introduction,
            lexicalTargets,
        ),
        framedNullable(targetScopeId),
        framedNullable(moduleOneSource?.let { source ->
            framedList(
                listOf(
                    source.taskId,
                    source.targetLexicalId,
                    source.expectedForm,
                    framedList(source.knownAlternativeForms),
                    framedNullable(source.referenceRole),
                    framedNullable(source.frameVariantId),
                    source.casePolicy,
                    framedList(source.choiceRationales.map { rationale ->
                        framedList(listOf(rationale.choice, rationale.rationale))
                    }),
                ),
            )
        }),
    ),
)

private fun GeneratedExercise.pairNineFingerprintCanonical(
    prompt: ExercisePrompt,
    lexicalTarget: LexicalTarget?,
    inspector: ExerciseInspector,
    supportSkillIds: List<String>,
    readingTarget: ReadingTask?,
    introduction: List<IntroductionSection>,
    lexicalTargets: List<LexicalTarget>?,
    targetScopeId: String?,
): String = framedList(
    listOf(
        pairEightFingerprintCanonical(
            prompt,
            lexicalTarget,
            inspector,
            supportSkillIds,
            readingTarget,
            introduction,
            lexicalTargets,
            targetScopeId,
            inspector.moduleOneSource,
        ),
        framedNullable(inspector.inanimateReferenceSource?.let { source ->
            framedList(
                listOf(
                    source.taskId,
                    source.targetEntryId,
                    source.targetSenseId,
                    source.expectedForm,
                    framedList(source.knownAlternativeForms),
                    source.antecedentEntryId,
                    source.antecedentSenseId,
                    source.antecedentArticle,
                    source.antecedentGender,
                    source.antecedentNumber,
                    source.requestedRole,
                    source.casePolicy,
                    source.contextVariantId,
                    framedList(source.choiceRationales.map { rationale ->
                        framedList(listOf(rationale.choice, rationale.rationale))
                    }),
                ),
            )
        }),
    ),
)

private fun framedNullable(value: String?): String = if (value == null) "0" else "1${framed(value)}"

private fun framedList(values: List<String>): String = "${values.size}[${values.joinToString(separator = "", transform = ::framed)}]"

private fun framed(value: String): String = "${value.toByteArray(Charsets.UTF_8).size}:$value"
