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
    ),
    introduction = introduction.map { IntroductionDto(it.title, it.body, it.bodyLanguage) },
    hint = hint,
    readingTarget = readingTarget?.let { ReadingTaskDto(it.facet) },
)

internal fun AttemptAnswerDto.toEngineAnswer(): Answer = when (this) {
    is AttemptAnswerDto.Text -> Answer.Text(text)
    is AttemptAnswerDto.Order -> Answer.Order(blockIds)
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
)

internal fun GeneratedExercise.reproductionFingerprint(
    promptForFingerprint: ExercisePrompt = prompt,
    lexicalTargetForFingerprint: LexicalTarget? = lexicalTarget,
    inspectorForFingerprint: ExerciseInspector = inspector,
    supportSkillIdsForFingerprint: List<String> = supportSkillIds,
    readingTargetForFingerprint: ReadingTask? = readingTarget,
    introductionForFingerprint: List<IntroductionSection> = introduction,
): String {
    val legacyCanonical = listOf(
        formatId,
        targetSkillId.orEmpty(),
        contentRevision,
        generatorRevision,
        reproduction.seed.toString(),
        reproduction.index.toString(),
        inspectorForFingerprint.canonicalSentenceId.orEmpty(),
        inspectorForFingerprint.constructionId.orEmpty(),
        inspectorForFingerprint.lexicalIds.joinToString(","),
        inspectorForFingerprint.clauseRoles.joinToString(","),
        inspectorForFingerprint.canonicalSentence.orEmpty(),
        instruction,
        context,
        promptForFingerprint.before,
        promptForFingerprint.after,
        promptForFingerprint.choices.joinToString(","),
        promptForFingerprint.blocks.joinToString(",") { "${it.id}:${it.text}" },
        promptForFingerprint.punctuation,
        introduction.joinToString("\u001e") { "${it.title}:${it.body}" },
        hint,
    )
    val pairTwoCanonical = legacyCanonical +
        listOf(promptForFingerprint.inputLabel.orEmpty(), promptForFingerprint.replacementWord.orEmpty())
    val pairThreeCanonical = run {
        val target = lexicalTargetForFingerprint
        val source = inspectorForFingerprint.lexicalSource
        pairTwoCanonical + listOf(
            target?.entryId.orEmpty(),
            target?.senseId.orEmpty(),
            target?.facet.orEmpty(),
            target?.direction.orEmpty(),
            promptForFingerprint.cue.orEmpty(),
            promptForFingerprint.cueLanguage.orEmpty(),
            promptForFingerprint.answerLanguage.orEmpty(),
            source?.entryId.orEmpty(),
            source?.senseId.orEmpty(),
            source?.headword.orEmpty(),
            source?.gloss.orEmpty(),
            source?.article.orEmpty(),
            supportSkillIdsForFingerprint.joinToString(","),
        )
    }
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
        else -> error("Unsupported fingerprint revision pair: $contentRevision/$generatorRevision")
    }
    return MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

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

private fun framedNullable(value: String?): String = if (value == null) "0" else "1${framed(value)}"

private fun framedList(values: List<String>): String = "${values.size}[${values.joinToString(separator = "", transform = ::framed)}]"

private fun framed(value: String): String = "${value.toByteArray(Charsets.UTF_8).size}:$value"
