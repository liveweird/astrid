package app.astrid.engine

import java.util.Locale

internal data class InanimateReferenceTask(
    val id: String,
    val formatId: String,
    val targetScopeId: String,
    val targetEntryId: String,
    val targetSenseId: String,
    val expectedForm: String,
    val knownAlternativeForms: List<String>,
    val antecedentEntryId: String,
    val antecedentSenseId: String,
    val antecedentArticle: String,
    val antecedentHeadword: String,
    val antecedentMeaning: String,
    val antecedentGender: String,
    val antecedentNumber: String,
    val requestedRole: String,
    val casePolicy: String,
    val contextVariantId: String,
    val context: String,
    val choiceRationales: List<ModuleOneChoiceRationale>,
) {
    val choices: List<String> = if (formatId == "E020") listOf("den", "det") else emptyList()
    val answerExposureKeys: List<String> = listOf(referenceSenseKey(targetSenseId), visibleFormKey(expectedForm))
    val promptExposureKeys: List<String> = antecedentExposureKeys() + if (choices.isNotEmpty()) ALL_REFERENCE_KEYS else emptyList()
    val lessonExposureKeys: List<String> = ALL_REFERENCE_KEYS + ALL_SUPPLIED_AND_IDENTIFYING_KEYS
    val hintExposureKeys: List<String> = emptyList()
    val feedbackExposureKeys: List<String> = answerExposureKeys

    private fun antecedentExposureKeys(): List<String> = listOf(
        "module-one:headword:$antecedentSenseId",
        "module-one:article:$antecedentEntryId",
    )
}

internal object InanimateReferenceContent {
    const val TARGET_SCOPE_ID = "inanimate-singular-subject"
    const val CASE_POLICY = "standalone-introduced-form-1"
    const val REQUESTED_ROLE = "subject"
    const val ANTECEDENT_NUMBER = "singular"

    private val BOOK = Antecedent("book", "L026", "L026-S01", "den", "L014", "L014-S01", "en", "bog", "book", "common")
    private val LETTER = Antecedent(
        "letter", "L008", "L008-S02", "det", "L018", "L018-S01", "et", "brev", "letter (written message)", "neuter",
    )

    val tasks: List<InanimateReferenceTask> =
        listOf(referenceTask("E020", BOOK), referenceTask("E020", LETTER)) +
            listOf(referenceTask("E021", BOOK), referenceTask("E021", LETTER))

    fun tasks(formatId: String): List<InanimateReferenceTask> = tasks.filter { it.formatId == formatId }

    fun instruction(formatId: String): String = when (formatId) {
        "E020" -> "Choose the introduced Danish word that refers to the same one thing as the subject."
        "E021" -> "Type only the introduced Danish word for the same one thing, using standalone lowercase spelling."
        else -> error("Unsupported inanimate-reference format $formatId")
    }

    private fun referenceTask(formatId: String, antecedent: Antecedent): InanimateReferenceTask {
        val selection = formatId == "E020"
        val operation = if (selection) "selection" else "recall"
        val context = when (antecedent.entryId to operation) {
            "L014" to "selection" ->
                "A book was mentioned earlier. The supplied noun bundle means a book and has common gender. " +
                    "Choose the subject word for that same book in the next statement."
            "L018" to "selection" ->
                "A letter (a written message) was mentioned earlier. The supplied noun bundle has neuter gender. " +
                    "Choose the subject word for that same letter in the next statement."
            "L014" to "recall" ->
                "You continue discussing the same previously mentioned book. " +
                    "The supplied noun bundle means a book and has common gender. " +
                    "Type the standalone subject word that refers back to it."
            else ->
                "You continue discussing the same previously mentioned letter (a written message). " +
                    "The supplied noun bundle has neuter gender. " +
                    "Type the standalone subject word that refers back to it."
        }
        val alternatives = listOf("den", "det").filterNot { it == antecedent.form }
        val rationales = if (selection) {
            listOf("den", "det").map { choice ->
                ModuleOneChoiceRationale(
                    choice,
                    if (choice == antecedent.form) {
                        "Matches the supplied grammatical gender of the singular antecedent."
                    } else {
                        "Refers to a singular thing of the other supplied grammatical gender."
                    },
                )
            }
        } else {
            emptyList()
        }
        return InanimateReferenceTask(
            id = "${formatId.lowercase(Locale.ROOT)}-${antecedent.id}",
            formatId = formatId,
            targetScopeId = TARGET_SCOPE_ID,
            targetEntryId = antecedent.targetEntryId,
            targetSenseId = antecedent.targetSenseId,
            expectedForm = antecedent.form,
            knownAlternativeForms = alternatives,
            antecedentEntryId = antecedent.entryId,
            antecedentSenseId = antecedent.senseId,
            antecedentArticle = antecedent.article,
            antecedentHeadword = antecedent.headword,
            antecedentMeaning = antecedent.meaning,
            antecedentGender = antecedent.gender,
            antecedentNumber = ANTECEDENT_NUMBER,
            requestedRole = REQUESTED_ROLE,
            casePolicy = CASE_POLICY,
            contextVariantId = "$operation-${antecedent.id}-reference",
            context = context,
            choiceRationales = rationales,
        )
    }

    private data class Antecedent(
        val id: String,
        val targetEntryId: String,
        val targetSenseId: String,
        val form: String,
        val entryId: String,
        val senseId: String,
        val article: String,
        val headword: String,
        val meaning: String,
        val gender: String,
    )

}

internal object InanimateReferenceExerciseGenerator {
    fun create(request: GenerationRequest): GeneratedExercise {
        val candidates = InanimateReferenceContent.tasks(request.formatId)
        val task = candidates[permutedIndex(request.seed, request.index, candidates.size)]
        val choices = if (((request.seed.toLong() + request.index) ushr 1) and 1L == 0L) {
            task.choices
        } else {
            task.choices.reversed()
        }
        return GeneratedExercise(
            id = listOf(request.formatId, request.contentRevision, request.generatorRevision, task.id, request.seed, request.index)
                .joinToString("-"),
            formatId = request.formatId,
            targetSkillId = request.targetSkillId,
            lexicalTarget = null,
            supportSkillIds = listOf("G001", "G002", "G003", "G006"),
            presentation = ExercisePresentation(
                instruction = InanimateReferenceContent.instruction(request.formatId),
                context = task.context,
                prompt = ExercisePrompt(
                    before = "",
                    after = "",
                    choices = choices,
                    punctuation = "",
                    inputLabel = if (request.formatId == "E021") "Danish reference word" else null,
                    cue = "${task.antecedentArticle} ${task.antecedentHeadword}",
                    cueLanguage = "da",
                    answerLanguage = "da",
                ),
                introduction = introduction(task),
                hint = if (request.formatId == "E020") {
                    "Use the supplied gender to choose the standalone reference word."
                } else {
                    "Type one lowercase word: use the form taught for the supplied gender."
                },
            ),
            contentRevision = request.contentRevision,
            generatorRevision = request.generatorRevision,
            reproduction = Reproduction(request.seed, request.index),
            inspector = ExerciseInspector(
                lexicalIds = listOf(task.targetEntryId, task.antecedentEntryId),
                clauseRoles = listOf(task.requestedRole),
                inanimateReferenceSource = task.details(),
            ),
            source = InanimateReferenceExerciseSource(
                task,
                AnswerKey.ModuleOneForm(task.expectedForm, task.knownAlternativeForms.toSet(), task.choices.isNotEmpty()),
            ),
        )
    }

    private fun introduction(task: InanimateReferenceTask): List<IntroductionSection> = listOf(
        IntroductionSection(
            "Refer to the same thing",
            "In the English statement “It is here”, “it” is the subject. " +
                "Here the requested subject word refers to the same mentioned thing.",
        ),
        IntroductionSection("Supplied noun", "${task.antecedentArticle} ${task.antecedentHeadword}", "da"),
        IntroductionSection(
            "Supplied meaning and gender",
            "The supplied bundle means a ${task.antecedentMeaning}; it has ${task.antecedentGender} gender.",
        ),
        IntroductionSection("Reference forms", "en bog → den · et brev → det", "da"),
        IntroductionSection(
            "Standalone spelling",
            "Use the lowercase forms shown above. Capitals at the start of a complete sentence are normal, " +
                "but this task requests one standalone word.",
        ),
        IntroductionSection("A different identifying pattern", "Det er …", "da"),
        IntroductionSection(
            "Reference and identification",
            "The pattern above means this or it is …. It identifies something; the activity here instead refers back to a mentioned thing.",
        ),
    )

    private fun InanimateReferenceTask.details() = InanimateReferenceSourceDetails(
        id,
        targetEntryId,
        targetSenseId,
        expectedForm,
        knownAlternativeForms,
        antecedentEntryId,
        antecedentSenseId,
        antecedentArticle,
        antecedentGender,
        antecedentNumber,
        requestedRole,
        casePolicy,
        contextVariantId,
        choiceRationales,
    )

    private fun permutedIndex(seed: Int, index: Int, size: Int): Int =
        Math.floorMod(seed.toLong() * SEED_FACTOR + index.toLong() * INDEX_FACTOR, size.toLong()).toInt()

    private const val SEED_FACTOR = 31L
    private const val INDEX_FACTOR = 17L
}

internal object InanimateReferenceExerciseEvaluator {
    fun evaluate(exercise: GeneratedExercise, answer: Answer): Evaluation {
        val source = exercise.source as InanimateReferenceExerciseSource
        val key = source.answerKey as AnswerKey.ModuleOneForm
        val raw = answer as? Answer.Text ?: return typeMismatch(answer)
        val text = normalizeText(raw.text)
        return when {
            text.isEmpty() -> unknown(
                answer,
                text,
                "EMPTY_RESPONSE",
                "Enter one introduced reference word.",
                ResponseConstraintStatus.NOT_MET,
            )
            text.any(Char::isWhitespace) -> constraintFailure(source.task, answer, text)
            text == key.expected -> result(
                source.task, answer, text, TargetStatus.MET, LinguisticStatus.ACCEPTABLE, emptyList(),
                ResponseConstraintStatus.MET, "Correct. The reference word matches the supplied antecedent gender.",
            )
            text in key.knownAlternatives -> result(
                source.task, answer, text, TargetStatus.NOT_MET, LinguisticStatus.ACCEPTABLE,
                listOf(issue("INTRODUCED_REFERENCE_REQUIRED", "The introduced word refers to the other supplied grammatical gender.")),
                ResponseConstraintStatus.MET, "That is an introduced reference word, but it does not match the supplied antecedent gender.",
            )
            text.lowercase(Locale.ROOT) in (key.knownAlternatives + key.expected) -> {
                val target = if (text.lowercase(Locale.ROOT) == key.expected) TargetStatus.MET else TargetStatus.NOT_MET
                result(
                    source.task, answer, text, target, LinguisticStatus.KNOWN_ISSUE,
                    listOf(issue("STANDALONE_PRONOUN_CASE_REQUIRED", "Use the introduced lowercase standalone form.")),
                    ResponseConstraintStatus.NOT_MET, "Use the introduced lowercase standalone form shown in the completed answer.",
                )
            }
            key.selection -> unknown(answer, text, "UNSUPPLIED_CHOICE", "Choose one of the supplied reference words.")
            else -> unknown(
                answer,
                text,
                "UNRECOGNIZED_REFERENCE_WORD",
                "That response is outside the introduced alternatives for this task.",
            )
        }
    }

    private fun constraintFailure(task: InanimateReferenceTask, raw: Answer, text: String): Evaluation = result(
        task,
        raw,
        text,
        TargetStatus.UNEVALUABLE,
        LinguisticStatus.KNOWN_ISSUE,
        listOf(issue("ONE_WORD_RESPONSE_REQUIRED", "The task requires exactly one word.")),
        ResponseConstraintStatus.NOT_MET,
        "Enter exactly one introduced word; the completed answer shows the expected form.",
    )

    private fun result(
        task: InanimateReferenceTask,
        raw: Answer,
        text: String,
        target: TargetStatus,
        linguistic: LinguisticStatus,
        issues: List<EvaluationIssue>,
        constraint: ResponseConstraintStatus,
        feedback: String,
    ): Evaluation = Evaluation(
        target,
        linguistic,
        issues,
        feedback,
        task.expectedForm,
        raw,
        NormalizedAnswer.Text(text),
        LinguisticScope.DANISH_WORD_FORM,
        constraint,
    )

    private fun unknown(
        raw: Answer,
        text: String,
        code: String,
        feedback: String,
        constraint: ResponseConstraintStatus = ResponseConstraintStatus.MET,
    ): Evaluation = Evaluation(
        TargetStatus.UNEVALUABLE,
        LinguisticStatus.UNKNOWN,
        listOf(issue(code, "The response cannot be classified within the introduced alternatives.")),
        feedback,
        null,
        raw,
        NormalizedAnswer.Text(text),
        LinguisticScope.DANISH_WORD_FORM,
        constraint,
    )

    private fun typeMismatch(answer: Answer): Evaluation = Evaluation(
        TargetStatus.UNEVALUABLE,
        LinguisticStatus.UNKNOWN,
        listOf(issue("ANSWER_TYPE_MISMATCH", "The answer type does not match this word-form exercise.")),
        "The answer could not be evaluated for this exercise.",
        null,
        answer,
        when (answer) {
            is Answer.Text -> NormalizedAnswer.Text(normalizeText(answer.text))
            is Answer.Order -> NormalizedAnswer.Order(answer.blockIds)
            is Answer.Matching -> NormalizedAnswer.Matching(answer.pairs)
            is Answer.Gaps -> NormalizedAnswer.Gaps(answer.selections)
        },
        LinguisticScope.DANISH_WORD_FORM,
        ResponseConstraintStatus.NOT_MET,
    )

    private fun issue(code: String, message: String) = EvaluationIssue(code, message)
}

private val ALL_REFERENCE_KEYS = listOf(
    referenceSenseKey("L026-S01"),
    referenceSenseKey("L008-S02"),
    visibleFormKey("den"),
    visibleFormKey("det"),
)

private val ALL_SUPPLIED_AND_IDENTIFYING_KEYS = listOf(
    "module-one:headword:L014-S01",
    "module-one:article:L014",
    "module-one:headword:L018-S01",
    "module-one:article:L018",
    "module-one:identifying-form:L008",
    "module-one:verb-present:L010",
)

private fun referenceSenseKey(senseId: String) = "module-one:reference-sense:$senseId"

private fun visibleFormKey(form: String) = "module-one:visible-form:$form"
