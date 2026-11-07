package app.astrid.server

import app.astrid.engine.Answer
import app.astrid.engine.ExerciseInspector
import app.astrid.engine.ExercisePrompt
import app.astrid.engine.GeneratedExercise
import app.astrid.engine.LexicalTarget
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
    ),
    introduction = introduction.map { IntroductionDto(it.title, it.body) },
    hint = hint,
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
)

internal fun GeneratedExercise.reproductionFingerprint(
    promptForFingerprint: ExercisePrompt = prompt,
    lexicalTargetForFingerprint: LexicalTarget? = lexicalTarget,
    inspectorForFingerprint: ExerciseInspector = inspector,
    supportSkillIdsForFingerprint: List<String> = supportSkillIds,
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
    val canonical = when (contentRevision to generatorRevision) {
        CONTENT_REVISION to GENERATOR_REVISION -> legacyCanonical
        "pilot-2" to "generator-2" -> pairTwoCanonical
        CURRENT_CONTENT_REVISION to CURRENT_GENERATOR_REVISION -> {
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
        else -> error("Unsupported fingerprint revision pair: $contentRevision/$generatorRevision")
    }.joinToString("\u001f")
    return MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray())
        .joinToString("") { "%02x".format(it) }
}
