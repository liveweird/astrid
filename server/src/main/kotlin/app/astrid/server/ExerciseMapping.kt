package app.astrid.server

import app.astrid.engine.Answer
import app.astrid.engine.GeneratedExercise
import java.security.MessageDigest

internal fun GeneratedExercise.toResponse(id: String) = ExerciseResponse(
    id = id,
    formatId = formatId,
    targetSkillId = targetSkillId,
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
)

internal fun GeneratedExercise.reproductionFingerprint(): String {
    val canonical = listOf(
        formatId,
        targetSkillId,
        contentRevision,
        generatorRevision,
        reproduction.seed.toString(),
        reproduction.index.toString(),
        inspector.canonicalSentenceId,
        inspector.constructionId,
        inspector.lexicalIds.joinToString(","),
        inspector.clauseRoles.joinToString(","),
        inspector.canonicalSentence,
        instruction,
        context,
        prompt.before,
        prompt.after,
        prompt.choices.joinToString(","),
        prompt.blocks.joinToString(",") { "${it.id}:${it.text}" },
        prompt.punctuation,
        introduction.joinToString("\u001e") { "${it.title}:${it.body}" },
        hint,
    ).joinToString("\u001f")
    return MessageDigest.getInstance("SHA-256").digest(canonical.toByteArray())
        .joinToString("") { "%02x".format(it) }
}
