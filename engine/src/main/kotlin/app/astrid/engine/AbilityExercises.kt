package app.astrid.engine

import java.util.Locale

internal data class AbilityTask(
    val id: String,
    val formatId: String,
    val frameId: String,
    val subject: AbilitySubject,
    val englishProposition: String,
    val complement: AbilityComplement,
) {
    val targetScopeId = AbilityContent.SCOPE_ID
    val answerExposureKeys = listOf(
        AbilityContent.BARE_INFINITIVE_KEY,
        AbilityContent.AT_INFINITIVE_KEY,
        AbilityContent.FINITE_PRESENT_KEY,
        AbilityContent.PATTERN_KEY,
    )
    val promptExposureKeys = listOf(AbilityContent.MODAL_PRESENT_KEY) +
        complement.lexicalIds.map(PresentOrderContent::componentKey) +
        if (formatId == "E026") {
            listOf(AbilityContent.BARE_INFINITIVE_KEY, AbilityContent.FINITE_PRESENT_KEY, AbilityContent.AT_INFINITIVE_KEY)
        } else {
            listOf(AbilityContent.AT_INFINITIVE_KEY, AbilityContent.BARE_INFINITIVE_KEY)
        }
    val lessonExposureKeys = AbilityContent.introductionExposureKeys()
    val hintExposureKeys = listOf(AbilityContent.PATTERN_KEY, AbilityContent.BARE_INFINITIVE_KEY)
    val feedbackExposureKeys = listOf(AbilityContent.PATTERN_KEY, AbilityContent.BARE_INFINITIVE_KEY)
}

internal data class AbilitySubject(val lexicalId: String, val text: String, val reference: String)

internal data class AbilityComplement(
    val text: String,
    val meaning: String,
    val lexicalIds: List<String>,
    val role: String,
)

internal data class AbilityExerciseSource(
    val task: AbilityTask,
    override val answerKey: AnswerKey,
    override val blockRoles: Map<String, String> = emptyMap(),
) : ExerciseSource

internal object AbilityContent {
    const val SCOPE_ID = "kan-bare-infinitive"
    const val PATTERN_KEY = "module-one:pattern:kan-bare-infinitive"
    const val MODAL_INFINITIVE_KEY = "module-one:verb-infinitive:L033"
    const val MODAL_PRESENT_KEY = "module-one:verb-present:L033"
    const val AT_INFINITIVE_KEY = "module-one:at-infinitive:L011"
    const val BARE_INFINITIVE_KEY = "module-one:bare-infinitive:L011"
    const val FINITE_PRESENT_KEY = "module-one:verb-present:L011"
    const val MODAL_LEXICAL_ID = "L033"
    const val READ_LEXICAL_ID = "L011"
    const val CONSTRUCTION_ID = "C014"
    val can = Verb(MODAL_LEXICAL_ID, "kunne", "kan", "be able to")

    fun tasks(formatId: String): List<AbilityTask> = when (formatId) {
        "E026" -> listOf(
            task(
                "ability-select-letter", formatId, "s10-kan-laese-letter",
                AbilitySubject("L004", "hun", "one stated woman"), "She can read a letter.",
                AbilityComplement("et brev", "a letter", listOf("L023", "L018"), "OBJECT"),
            ),
            task(
                "ability-select-danish", formatId, "s10-kan-laese-danish",
                AbilitySubject("L005", "vi", "the speaker and another person"), "We can read Danish.",
                AbilityComplement("dansk", "Danish", listOf("L031"), "LANGUAGE_COMPLEMENT"),
            ),
        )
        "E027" -> listOf(
            task(
                "ability-recall-letter", formatId, "s10-kan-laese-letter",
                AbilitySubject("L002", "du", "one addressee"), "You can read a letter.",
                AbilityComplement("et brev", "a letter", listOf("L023", "L018"), "OBJECT"),
            ),
            task(
                "ability-recall-danish", formatId, "s10-kan-laese-danish",
                AbilitySubject("L007", "de", "several other people"), "They can read Danish.",
                AbilityComplement("dansk", "Danish", listOf("L031"), "LANGUAGE_COMPLEMENT"),
            ),
        )
        else -> error("Unsupported ability format $formatId")
    }

    fun introduction() = listOf(
        IntroductionSection("Ability modal", "at kunne → kan", "da"),
        IntroductionSection("Reading verb", "at læse → læse efter kan", "da"),
        IntroductionSection(
            "Meanings",
            "Here, kan expresses ability (‘can’ or ‘be able to’), and læse means reading written language.",
        ),
        IntroductionSection(
            "Requested pattern",
            "In these ability statements, use kan followed by the bare infinitive læse: no at and no present form læser.",
        ),
        IntroductionSection("Worked example — letter", "Jeg kan læse et brev.", "da"),
        IntroductionSection("Worked meaning — letter", "I can read a letter."),
        IntroductionSection("Worked example — Danish", "Han kan læse dansk.", "da"),
        IntroductionSection("Worked meaning — Danish", "He can read Danish."),
        IntroductionSection("Answer form", "Choose or type only læse for the missing verb."),
    )

    fun introductionExposureKeys() = listOf(
        MODAL_INFINITIVE_KEY,
        MODAL_PRESENT_KEY,
        AT_INFINITIVE_KEY,
        BARE_INFINITIVE_KEY,
        FINITE_PRESENT_KEY,
        PATTERN_KEY,
        PresentOrderContent.componentKey("L023"),
        PresentOrderContent.componentKey("L018"),
        PresentOrderContent.componentKey("L031"),
    )

    private fun task(
        id: String,
        formatId: String,
        frameVariant: String,
        subject: AbilitySubject,
        englishProposition: String,
        complement: AbilityComplement,
    ) = AbilityTask(
        id, formatId, frameVariant, subject, englishProposition, complement,
    )
}

internal object AbilityExerciseGenerator {
    fun generate(request: GenerationRequest): GeneratedExercise {
        val tasks = AbilityContent.tasks(request.formatId)
        val taskIndex = Math.floorMod(request.seed.toLong() * SEED_FACTOR + request.index, tasks.size.toLong()).toInt()
        val task = tasks[taskIndex]
        val selection = request.formatId == "E026"
        val choices = if (selection) choices(request.seed, task.id) else emptyList()
        return GeneratedExercise(
            id = "${request.formatId}-${request.contentRevision}-${request.generatorRevision}-${task.id}-${request.seed}-${request.index}",
            formatId = request.formatId,
            targetSkillId = "G030",
            lexicalTarget = null,
            supportSkillIds = listOf("G001", "G002", "G003", "G004", "G005"),
            presentation = ExercisePresentation(
                instruction = if (selection) {
                    "Choose the verb form used after kan in this ability statement."
                } else {
                    "Type the verb form used after kan in this ability statement."
                },
                context = "English: ${task.englishProposition} The subject refers to ${task.subject.reference}. " +
                    "The supplied complement ${task.complement.text} means ${task.complement.meaning}.",
                prompt = ExercisePrompt(
                    before = task.subject.text.replaceFirstChar(Char::titlecase) + " kan ",
                    after = " ${task.complement.text}.",
                    choices = choices,
                    inputLabel = if (selection) null else "Danish verb after kan",
                    cue = "at læse",
                    cueLanguage = "da",
                    answerLanguage = "da",
                ),
                introduction = AbilityContent.introduction(),
                hint = "After kan, use the introduced bare infinitive læse without at.",
            ),
            contentRevision = request.contentRevision,
            generatorRevision = request.generatorRevision,
            reproduction = Reproduction(request.seed, request.index),
            inspector = ExerciseInspector(
                canonicalSentenceId = task.id,
                constructionId = AbilityContent.CONSTRUCTION_ID,
                lexicalIds = listOf(task.subject.lexicalId, AbilityContent.MODAL_LEXICAL_ID, AbilityContent.READ_LEXICAL_ID) +
                    task.complement.lexicalIds,
                clauseRoles = listOf("SUBJECT", "FINITE_VERB", "NONFINITE_VERB", task.complement.role),
                canonicalSentence = canonical(task),
                moduleOneSource = ModuleOneSourceDetails(
                    task.id,
                    AbilityContent.READ_LEXICAL_ID,
                    "læse",
                    listOf("at læse", "læser"),
                    "bare-infinitive-after-kan",
                    task.frameId,
                    "lowercase-mid-sentence",
                    choices.map { choice ->
                        ModuleOneChoiceRationale(choice, rationale(choice))
                    },
                ),
            ),
            source = AbilityExerciseSource(
                task,
                AnswerKey.ModuleOneForm("læse", setOf("læser", "at læse"), selection),
            ),
        )
    }

    private fun choices(seed: Int, taskId: String): List<String> {
        val choices = listOf("læse", "læser", "at læse")
        val offset = Math.floorMod(seed.toLong() + taskId.hashCode().toLong(), choices.size.toLong()).toInt()
        return choices.drop(offset) + choices.take(offset)
    }

    private fun rationale(choice: String): String = when (choice) {
        "læse" -> "Bare infinitive required after kan in the introduced ability frame."
        "læser" -> "Finite present form; kan already carries tense in this frame."
        else -> "At-marked infinitive; the introduced frame requires the infinitive without at after kan."
    }

    private fun canonical(task: AbilityTask) =
        "${task.subject.text} kan læse ${task.complement.text}.".replaceFirstChar(Char::titlecase)

    private const val SEED_FACTOR = 31L
}

internal object AbilityExerciseEvaluator {
    fun evaluate(exercise: GeneratedExercise, answer: Answer): Evaluation {
        val source = exercise.source as AbilityExerciseSource
        val raw = answer as? Answer.Text ?: return typeMismatch(answer)
        val normalized = normalizeText(raw.text)
        val selection = source.task.formatId == "E026"
        return when {
            normalized.isEmpty() -> result(
                answer, normalized, TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN,
                "EMPTY_RESPONSE", "Enter the introduced verb form used after kan.", ResponseConstraintStatus.NOT_MET,
            )
            normalized == "at læse" -> result(
                answer, normalized, TargetStatus.NOT_MET, LinguisticStatus.KNOWN_ISSUE,
                "INFINITIVE_MARKER_AFTER_MODAL", "Use the infinitive without at after kan.",
                if (selection) ResponseConstraintStatus.MET else ResponseConstraintStatus.NOT_MET,
            )
            normalized.any(Char::isWhitespace) -> result(
                answer, normalized, TargetStatus.UNEVALUABLE, LinguisticStatus.KNOWN_ISSUE,
                "ONE_WORD_RESPONSE_REQUIRED", "Enter exactly one word.", ResponseConstraintStatus.NOT_MET,
            )
            normalized == "læse" -> result(
                answer, normalized, TargetStatus.MET, LinguisticStatus.ACCEPTABLE,
                null, "Correct. After kan, this frame uses the bare infinitive læse.", ResponseConstraintStatus.MET,
            )
            normalized == "læser" -> result(
                answer, normalized, TargetStatus.NOT_MET, LinguisticStatus.KNOWN_ISSUE,
                "FINITE_VERB_AFTER_MODAL", "Use læse, not the finite present læser, after kan.",
                ResponseConstraintStatus.MET,
            )
            normalized.lowercase(Locale.ROOT) == "læse" -> result(
                answer, normalized, TargetStatus.MET, LinguisticStatus.KNOWN_ISSUE,
                "MID_SENTENCE_VERB_CASE_REQUIRED", "Use the lowercase mid-sentence form læse.",
                ResponseConstraintStatus.NOT_MET,
            )
            selection -> result(
                answer, normalized, TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN,
                "UNSUPPLIED_CHOICE", "Choose one of the supplied verb forms.", ResponseConstraintStatus.NOT_MET,
            )
            else -> result(
                answer, normalized, TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN,
                "UNRECOGNIZED_BARE_INFINITIVE", "That response is outside the introduced forms for this task.",
                ResponseConstraintStatus.MET,
            )
        }
    }

    private fun result(
        raw: Answer,
        normalized: String,
        target: TargetStatus,
        linguistic: LinguisticStatus,
        issueCode: String?,
        feedback: String,
        constraint: ResponseConstraintStatus,
    ) = Evaluation(
        target,
        linguistic,
        issueCode?.let { listOf(EvaluationIssue(it, feedback)) }.orEmpty(),
        feedback,
        if (linguistic == LinguisticStatus.UNKNOWN) null else "læse",
        raw,
        NormalizedAnswer.Text(normalized),
        LinguisticScope.DANISH_WORD_FORM,
        constraint,
        null,
    )

    private fun typeMismatch(answer: Answer) = Evaluation(
        TargetStatus.UNEVALUABLE,
        LinguisticStatus.UNKNOWN,
        listOf(EvaluationIssue("ANSWER_TYPE_MISMATCH", "The answer type does not match the exercise format.")),
        "The answer could not be evaluated for this exercise.",
        null,
        answer,
        normalized(answer),
        LinguisticScope.DANISH_WORD_FORM,
        ResponseConstraintStatus.NOT_MET,
        null,
    )

    private fun normalized(answer: Answer): NormalizedAnswer = when (answer) {
        is Answer.Text -> NormalizedAnswer.Text(normalizeText(answer.text))
        is Answer.Order -> NormalizedAnswer.Order(answer.blockIds)
        is Answer.Matching -> NormalizedAnswer.Matching(answer.pairs)
        is Answer.Gaps -> NormalizedAnswer.Gaps(answer.selections)
    }
}
