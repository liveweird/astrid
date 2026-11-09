package app.astrid.engine

import java.util.Locale

internal data class ModuleOneTask(
    val id: String,
    val formatId: String,
    val targetScopeId: String,
    val targetLexicalId: String,
    val expectedForm: String,
    val knownAlternativeForms: List<String>,
    val cue: String,
    val context: String,
    val before: String,
    val after: String,
    val sentenceGloss: String,
    val referenceRole: String?,
    val frameVariantId: String?,
    val casePolicy: String,
    val choices: List<String>,
    val choiceRationales: List<ModuleOneChoiceRationale>,
    val introduction: List<IntroductionSection>,
    val hint: String,
    val promptExposureKeys: List<String>,
    val lessonExposureKeys: List<String>,
    val hintExposureKeys: List<String>,
    val feedbackExposureKeys: List<String>,
) {
    val answerExposureKeys: List<String> = listOf(ModuleOneContent.subjectOrVerbKey(targetLexicalId))
}

internal object ModuleOneContent {
    const val SINGULAR_SCOPE = "human-singular"
    const val GROUP_SCOPE = "human-group"
    const val VAERE_SCOPE = "vaere-present"
    const val CASE_POLICY = "standalone-introduced-form-1"

    val tasks: List<ModuleOneTask> =
        pronounTasks("E016", selection = true) + pronounTasks("E017", selection = false) +
            verbTasks("E018", selection = true) + verbTasks("E019", selection = false)

    fun tasks(formatId: String): List<ModuleOneTask> = tasks.filter { it.formatId == formatId }

    fun subjectOrVerbKey(lexicalId: String): String =
        if (lexicalId == "L010") "module-one:verb-present:L010" else "module-one:subject-form:$lexicalId"

    fun instruction(formatId: String): String = when (formatId) {
        "E016" -> "Choose the introduced Danish subject word."
        "E017" -> "Type only the introduced subject word using its taught standalone spelling."
        "E018" -> "Choose the introduced present form."
        "E019" -> "Type only the missing present form."
        else -> error("Unsupported Module 1 format $formatId")
    }

    private fun pronounTasks(formatId: String, selection: Boolean): List<ModuleOneTask> {
        val forms = listOf(
            PronounTaskData("L001", "jeg", "I", SINGULAR_SCOPE, "speaker", "Anna", "Nora"),
            PronounTaskData("L002", "du", "you (one person)", SINGULAR_SCOPE, "one-addressee", "Nora", "Anna"),
            PronounTaskData("L003", "han", "he", SINGULAR_SCOPE, "male-referent", null, null),
            PronounTaskData("L004", "hun", "she", SINGULAR_SCOPE, "female-referent", null, null),
            PronounTaskData("L005", "vi", "we", GROUP_SCOPE, "speaker-including-group", null, null),
            PronounTaskData("L006", "I", "you (several people)", GROUP_SCOPE, "several-addressees", null, null),
            PronounTaskData("L007", "de", "they (several people)", GROUP_SCOPE, "mentioned-group", null, null),
        )
        return forms.map { data -> pronounTask(formatId, selection, data, forms) }
    }

    private fun pronounTask(
        formatId: String,
        selection: Boolean,
        data: PronounTaskData,
        all: List<PronounTaskData>,
    ): ModuleOneTask {
        val group = data.scope == GROUP_SCOPE
        val alternatives = if (group) listOf("vi", "I", "de", "du") else listOf("jeg", "du", "han", "hun")
        val contexts = pronounContexts(data, selection)
        val before = contexts.before
        val after = contexts.after
        val choices = if (selection) alternatives else emptyList()
        val rationales = choices.map { choice ->
            val lexical = all.single { it.form == choice }
            ModuleOneChoiceRationale(
                choice,
                if (choice == data.form) "Matches the explicitly stated reference role." else
                    "Names ${lexical.role}, which does not match the stated reference role.",
            )
        }
        val introducedKeys = alternatives.map { choice -> subjectOrVerbKey(all.single { it.form == choice }.lexicalId) }
        return ModuleOneTask(
            id = "${formatId.lowercase(Locale.ROOT)}-${data.lexicalId.lowercase(Locale.ROOT)}",
            formatId = formatId,
            targetScopeId = data.scope,
            targetLexicalId = data.lexicalId,
            expectedForm = data.form,
            knownAlternativeForms = alternatives.filterNot { it == data.form },
            cue = data.cue,
            context = contexts.context,
            before = before,
            after = after,
            sentenceGloss = contexts.gloss,
            referenceRole = data.role,
            frameVariantId = contexts.frameVariantId,
            casePolicy = CASE_POLICY,
            choices = choices,
            choiceRationales = rationales,
            introduction = pronounIntroduction(data.scope),
            hint = if (selection) {
                "Use the English situation to choose the introduced subject word."
            } else {
                "Enter one introduced subject word with its standalone capitalization."
            },
            promptExposureKeys = introducedKeys.takeIf { selection }.orEmpty() + contexts.suppliedKeys,
            lessonExposureKeys = introducedKeys,
            hintExposureKeys = emptyList(),
            feedbackExposureKeys = listOf(subjectOrVerbKey(data.lexicalId)),
        )
    }

    private fun pronounContexts(data: PronounTaskData, selection: Boolean): PronounContext {
        if (data.nameForSelection != null) {
            return namedPronounContext(data, selection)
        }
        return referencePronounContext(data, selection)
    }

    private fun namedPronounContext(data: PronounTaskData, selection: Boolean): PronounContext {
        val name = if (selection) requireNotNull(data.nameForSelection) else requireNotNull(data.nameForRecall)
        val context = if (data.lexicalId == "L001") {
            if (selection) "Anna is speaking and introduces herself. Choose only the Danish subject word for the speaker." else
                "Nora is speaking and introduces herself. Type only the Danish subject word for the speaker."
        } else if (selection) {
            "You are speaking directly to one person named Nora. Choose only the Danish subject word for that addressee."
        } else {
            "You are speaking directly to one person named Anna. Type only the Danish subject word for that addressee."
        }
        val gloss = if (data.lexicalId == "L001") "I am $name." else "You (one person) are $name."
        return PronounContext(
            context,
            "",
            " er $name.",
            gloss,
            "identity-$name",
            listOf("module-one:verb-present:L010"),
        )
    }

    private fun referencePronounContext(data: PronounTaskData, selection: Boolean): PronounContext =
        when (data.lexicalId) {
            "L003" -> PronounContext(
                if (selection) "One man is being discussed. Choose only the Danish subject word that refers to him." else
                    "A man mentioned earlier is being discussed again. Type only the Danish subject word that refers to him.",
                "", "", data.cue, "standalone-male-reference", emptyList(),
            )
            "L004" -> PronounContext(
                if (selection) "One woman is being discussed. Choose only the Danish subject word that refers to her." else
                    "A woman mentioned earlier is being discussed again. Type only the Danish subject word that refers to her.",
                "", "", data.cue, "standalone-female-reference", emptyList(),
            )
            "L005" -> PronounContext(
                if (selection) {
                    "The speaker and another person are asleep. Choose the subject word for the group including the speaker."
                } else {
                    "The speaker and two friends are asleep. Type the subject word for the group including the speaker."
                },
                "", " sover.", "${data.cue} sleep.", "sleep-speaker-group", listOf("module-one:verb-present:L013"),
            )
            "L006" -> PronounContext(
                if (selection) "The speaker addresses several people who are asleep. Choose the subject word for those addressees." else
                    "The speaker now addresses a different group of several sleeping people. Type the subject word for those addressees.",
                "", " sover.", "${data.cue} sleep.", "sleep-several-addressees", listOf("module-one:verb-present:L013"),
            )
            else -> PronounContext(
                if (selection) {
                    "The speaker discusses several other people who are asleep. Choose the subject word for that mentioned group."
                } else {
                    "The speaker discusses a different group of sleeping people. Type the subject word for that mentioned group."
                },
                "", " sover.", "${data.cue} sleep.", "sleep-mentioned-group", listOf("module-one:verb-present:L013"),
            )
        }

    private fun verbTasks(formatId: String, selection: Boolean): List<ModuleOneTask> {
        val frames = listOf(
            VerbTaskData("book", "Det ", " en bog.", "This is a book.", listOf("L008", "L014")),
            VerbTaskData("letter", "Det ", " et brev.", "This is a letter.", listOf("L008", "L018")),
            VerbTaskData("teacher-jeg", "Jeg ", " lærer.", "I am a teacher.", listOf("L001", "L025")),
            VerbTaskData("teacher-du", "Du ", " lærer.", "You (one person) are a teacher.", listOf("L002", "L025")),
            VerbTaskData("teacher-han", "Han ", " lærer.", "He is a teacher.", listOf("L003", "L025")),
            VerbTaskData("teacher-hun", "Hun ", " lærer.", "She is a teacher.", listOf("L004", "L025")),
            VerbTaskData("identity-jeg-anna", "Jeg ", " Anna.", "I am Anna.", listOf("L001")),
            VerbTaskData("identity-du-nora", "Du ", " Nora.", "You (one person) are Nora.", listOf("L002")),
        )
        return frames.map { frame ->
            val choices = if (selection) listOf("er", "være") else emptyList()
            ModuleOneTask(
                id = "${formatId.lowercase(Locale.ROOT)}-${frame.id}",
                formatId = formatId,
                targetScopeId = VAERE_SCOPE,
                targetLexicalId = "L010",
                expectedForm = "er",
                knownAlternativeForms = listOf("være"),
                cue = frame.gloss,
                context = if (selection) {
                    "Complete the Danish sentence with the introduced present form. The English meaning is shown in the cue."
                } else {
                    "Type only the missing present verb. The English meaning is shown in the cue."
                },
                before = frame.before,
                after = frame.after,
                sentenceGloss = frame.gloss,
                referenceRole = null,
                frameVariantId = frame.id,
                casePolicy = "exact-present-form-1",
                choices = choices,
                choiceRationales = choices.map { choice ->
                    val rationale = if (choice == "er") {
                        "The introduced finite present form."
                    } else {
                        "The dictionary infinitive, not the requested present form."
                    }
                    ModuleOneChoiceRationale(choice, rationale)
                },
                introduction = verbIntroduction(),
                hint = if (selection) "Choose the one-word present form, not the dictionary infinitive." else
                    "Enter the one-word present form used in the sentence.",
                promptExposureKeys = frame.exposureKeys + if (selection) listOf(
                    "module-one:verb-present:L010",
                    "module-one:verb-infinitive:L010",
                ) else emptyList(),
                lessonExposureKeys = listOf(
                    "module-one:verb-present:L010",
                    "module-one:verb-infinitive:L010",
                ),
                hintExposureKeys = emptyList(),
                feedbackExposureKeys = listOf("module-one:verb-present:L010"),
            )
        }
    }

    private fun pronounIntroduction(scope: String): List<IntroductionSection> =
        listOf(
            IntroductionSection(
                "Choose the subject word",
                "Use the stated speaker, addressee, or referent. Enter only the requested standalone subject word.",
            ),
            IntroductionSection(
                "Introduced forms",
                if (scope == SINGULAR_SCOPE) "jeg · du · han · hun" else "vi · I · de · du",
                "da",
            ),
        )

    private fun verbIntroduction(): List<IntroductionSection> =
        listOf(
            IntroductionSection(
                "Dictionary form and present form",
                "The dictionary bundle uses an infinitive; a statement uses the introduced present form.",
            ),
            IntroductionSection("Introduced forms", "at være → er", "da"),
        )

    private data class PronounTaskData(
        val lexicalId: String,
        val form: String,
        val cue: String,
        val scope: String,
        val role: String,
        val nameForSelection: String?,
        val nameForRecall: String?,
    )

    private data class PronounContext(
        val context: String,
        val before: String,
        val after: String,
        val gloss: String,
        val frameVariantId: String,
        val suppliedKeys: List<String>,
    )

    private data class VerbTaskData(
        val id: String,
        val before: String,
        val after: String,
        val gloss: String,
        val lexicalIds: List<String>,
    ) {
        val exposureKeys: List<String> = lexicalIds.flatMap(::exposureKeys)

        private fun exposureKeys(lexicalId: String): List<String> =
            when (lexicalId) {
                in "L001".."L007" -> listOf("module-one:subject-form:$lexicalId")
                "L008" -> listOf("module-one:identifying-form:L008")
                "L014" -> listOf("module-one:headword:L014-S01", "module-one:article:L014")
                "L018" -> listOf("module-one:headword:L018-S01", "module-one:article:L018")
                "L025" -> listOf("module-one:headword:L025-S01", "module-one:article:L025")
                else -> error("Unsupported Module 1 exposure lexical ID $lexicalId")
            }
    }
}

internal object ModuleOneExerciseGenerator {
    fun create(request: GenerationRequest): GeneratedExercise {
        val candidates = ModuleOneContent.tasks(request.formatId)
        val task = candidates[permutedIndex(request.seed, request.index, candidates.size)]
        val choices =
            if (task.choices.isEmpty()) emptyList() else shuffled(task.choices, request.seed, request.index)
        return GeneratedExercise(
            id = listOf(
                request.formatId,
                request.contentRevision,
                request.generatorRevision,
                task.id,
                request.seed,
                request.index,
            ).joinToString("-"),
            formatId = request.formatId,
            targetSkillId = request.targetSkillId,
            lexicalTarget = null,
            supportSkillIds = if (request.targetSkillId == "G003") {
                listOf("G001", "G002", "G004")
            } else {
                listOf("G001", "G002", "G003")
            },
            presentation = ExercisePresentation(
                instruction = ModuleOneContent.instruction(request.formatId),
                context = task.context,
                prompt = ExercisePrompt(
                    before = task.before,
                    after = task.after,
                    choices = choices,
                    punctuation = if (task.before.isEmpty() && task.after.isEmpty()) "" else ".",
                    inputLabel = when (request.formatId) {
                        "E017" -> "Danish subject word"
                        "E019" -> "Present verb form"
                        else -> null
                    },
                    cue = task.cue,
                    cueLanguage = "en",
                    answerLanguage = "da",
                ),
                introduction = task.introduction,
                hint = task.hint,
            ),
            contentRevision = request.contentRevision,
            generatorRevision = request.generatorRevision,
            reproduction = Reproduction(request.seed, request.index),
            inspector = ExerciseInspector(
                canonicalSentenceId = canonicalSentenceId(task),
                constructionId = constructionId(task),
                lexicalIds = listOf(task.targetLexicalId),
                clauseRoles = listOfNotNull(task.referenceRole),
                canonicalSentence = completedSentence(task, task.expectedForm),
                moduleOneSource = ModuleOneSourceDetails(
                    task.id,
                    task.targetLexicalId,
                    task.expectedForm,
                    task.knownAlternativeForms,
                    task.referenceRole,
                    task.frameVariantId,
                    task.casePolicy,
                    task.choiceRationales,
                ),
            ),
            source = ModuleOneExerciseSource(
                task,
                AnswerKey.ModuleOneForm(
                    task.expectedForm,
                    task.knownAlternativeForms.toSet(),
                    task.choices.isNotEmpty(),
                ),
            ),
        )
    }

    private fun permutedIndex(seed: Int, index: Int, size: Int): Int =
        Math.floorMod(seed.toLong() * SEED_FACTOR + index.toLong() * INDEX_FACTOR, size.toLong()).toInt()

    private fun shuffled(values: List<String>, seed: Int, index: Int): List<String> =
        if ((seed.toLong() + index.toLong()) and 1L == 0L) values else values.reversed()

    private fun constructionId(task: ModuleOneTask): String? = when {
        task.frameVariantId?.startsWith("sleep-") == true -> "C001"
        task.frameVariantId in setOf("book", "letter") -> "C003"
        task.frameVariantId?.startsWith("identity-") == true -> "C004"
        task.frameVariantId?.startsWith("teacher-") == true -> "C004"
        else -> null
    }

    private fun canonicalSentenceId(task: ModuleOneTask): String? = when (constructionId(task)) {
        "C001" -> "C001-${task.targetLexicalId}-L013"
        "C003" -> if (task.frameVariantId == "book") "C003-L008-L010-L014" else "C003-L008-L010-L018"
        "C004" -> c004SentenceId(task)
        else -> null
    }

    private fun c004SentenceId(task: ModuleOneTask): String {
        val frame = requireNotNull(task.frameVariantId)
        return if (frame.startsWith("teacher-")) {
            "C004-${task.targetLexicalId.takeIf { it != "L010" } ?: teacherSubjectId(frame)}-L010-L025"
        } else {
            val subjectId = task.targetLexicalId.takeIf { it != "L010" } ?: identitySubjectId(frame)
            val name = if (task.targetLexicalId == "L010") {
                frame.substringAfterLast('-')
            } else {
                task.after.substringAfter(" er ").removeSuffix(".")
            }
            "C004-$subjectId-L010-name-${name.lowercase()}"
        }
    }

    private fun teacherSubjectId(frame: String): String = when (frame) {
        "teacher-jeg" -> "L001"
        "teacher-du" -> "L002"
        "teacher-han" -> "L003"
        "teacher-hun" -> "L004"
        else -> error("Unsupported teacher frame $frame")
    }

    private fun identitySubjectId(frame: String): String = when {
        "-jeg-" in frame -> "L001"
        "-du-" in frame -> "L002"
        else -> error("Unsupported identity frame $frame")
    }

    private const val SEED_FACTOR = 31L
    private const val INDEX_FACTOR = 17L
}

internal object ModuleOneExerciseEvaluator {
    fun evaluate(exercise: GeneratedExercise, answer: Answer): Evaluation {
        val source = exercise.source as ModuleOneExerciseSource
        val key = source.answerKey as AnswerKey.ModuleOneForm
        val raw = answer as? Answer.Text ?: return typeMismatch(answer)
        val normalized = normalizeText(raw.text)
        return if (source.task.targetLexicalId == "L010") {
            evaluateVerb(source.task, key, answer, normalized)
        } else {
            evaluatePronoun(source.task, key, answer, normalized)
        }
    }

    private fun evaluatePronoun(
        task: ModuleOneTask,
        key: AnswerKey.ModuleOneForm,
        raw: Answer,
        text: String,
    ): Evaluation = when {
        text.isEmpty() -> unknown(
            raw,
            text,
            "EMPTY_RESPONSE",
            "Enter one introduced subject word.",
            ResponseConstraintStatus.NOT_MET,
        )
        text.any(Char::isWhitespace) -> constraintFailure(task, raw, text, "ONE_WORD_RESPONSE_REQUIRED")
        else -> evaluateSinglePronoun(task, key, raw, text)
    }

    private fun evaluateSinglePronoun(
        task: ModuleOneTask,
        key: AnswerKey.ModuleOneForm,
        raw: Answer,
        text: String,
    ): Evaluation {
        val expected = key.expected
        val knownCanonical = key.knownAlternatives + expected
        return when {
            text == expected -> correctPronoun(task, raw, text)
            text in key.knownAlternatives -> wrongKnownPronoun(task, raw, text)
            knownCanonical.any { it.lowercase(Locale.ROOT) == text.lowercase(Locale.ROOT) } -> {
                val canonical = knownCanonical.single { it.lowercase(Locale.ROOT) == text.lowercase(Locale.ROOT) }
                val ambiguous = text in setOf("i", "De")
                result(
                    task,
                    raw,
                    text,
                    when {
                        ambiguous -> TargetStatus.UNEVALUABLE
                        canonical == expected -> TargetStatus.MET
                        else -> TargetStatus.NOT_MET
                    },
                    LinguisticStatus.KNOWN_ISSUE,
                    listOf(issue("STANDALONE_PRONOUN_CASE_REQUIRED", "Use the introduced standalone capitalization.")),
                    ResponseConstraintStatus.NOT_MET,
                    "Use the introduced standalone capitalization shown in the completed answer.",
                )
            }
            key.selection -> unknown(raw, text, "UNSUPPLIED_CHOICE", "Choose one of the supplied subject words.")
            else -> unknown(
                raw,
                text,
                "UNRECOGNIZED_SUBJECT_WORD",
                "That response is outside the introduced alternatives for this task.",
            )
        }
    }

    private fun correctPronoun(task: ModuleOneTask, raw: Answer, text: String): Evaluation =
        result(
            task,
            raw,
            text,
            TargetStatus.MET,
            LinguisticStatus.ACCEPTABLE,
            emptyList(),
            ResponseConstraintStatus.MET,
            "Correct. The subject word matches the stated reference.",
        )

    private fun wrongKnownPronoun(task: ModuleOneTask, raw: Answer, text: String): Evaluation =
        result(
            task,
            raw,
            text,
            TargetStatus.NOT_MET,
            LinguisticStatus.ACCEPTABLE,
            listOf(issue("INTRODUCED_REFERENCE_REQUIRED", "The introduced word refers to a different stated role.")),
            ResponseConstraintStatus.MET,
            "That is an introduced subject word, but it does not match the stated reference.",
        )

    private fun evaluateVerb(
        task: ModuleOneTask,
        key: AnswerKey.ModuleOneForm,
        raw: Answer,
        text: String,
    ): Evaluation = when {
        text.isEmpty() -> unknown(
            raw,
            text,
            "EMPTY_RESPONSE",
            "Enter the introduced present form.",
            ResponseConstraintStatus.NOT_MET,
        )
        text.any(Char::isWhitespace) -> constraintFailure(task, raw, text, "ONE_WORD_RESPONSE_REQUIRED")
        text == key.expected -> correctVerb(task, raw, text)
        text in key.knownAlternatives -> infinitiveResult(task, raw, text)
        text.lowercase(Locale.ROOT) == key.expected -> verbCaseResult(task, raw, text)
        key.selection -> unknown(raw, text, "UNSUPPLIED_CHOICE", "Choose one of the supplied verb forms.")
        else -> unknown(
            raw,
            text,
            "UNRECOGNIZED_PRESENT_FORM",
            "That response is outside the introduced alternatives for this task.",
        )
    }

    private fun correctVerb(task: ModuleOneTask, raw: Answer, text: String): Evaluation =
        result(
            task, raw, text, TargetStatus.MET, LinguisticStatus.ACCEPTABLE, emptyList(),
            ResponseConstraintStatus.MET, "Correct. The sentence uses the introduced present form.",
        )

    private fun infinitiveResult(task: ModuleOneTask, raw: Answer, text: String): Evaluation =
        result(
            task,
            raw,
            text,
            TargetStatus.NOT_MET,
            LinguisticStatus.KNOWN_ISSUE,
            listOf(issue("INFINITIVE_USED", "The dictionary infinitive was used where the present form is required.")),
            ResponseConstraintStatus.MET,
            "Use the introduced present form shown in the completed answer.",
        )

    private fun verbCaseResult(task: ModuleOneTask, raw: Answer, text: String): Evaluation =
        result(
            task,
            raw,
            text,
            TargetStatus.NOT_MET,
            LinguisticStatus.KNOWN_ISSUE,
            listOf(issue("PRESENT_FORM_CASE_REQUIRED", "Use the introduced lowercase standalone form.")),
            ResponseConstraintStatus.NOT_MET,
            "Use the introduced lowercase form shown in the completed answer.",
        )

    private fun constraintFailure(task: ModuleOneTask, raw: Answer, text: String, code: String): Evaluation =
        result(
            task,
            raw,
            text,
            TargetStatus.UNEVALUABLE,
            LinguisticStatus.KNOWN_ISSUE,
            listOf(issue(code, "The task requires exactly one word.")),
            ResponseConstraintStatus.NOT_MET,
            "Enter exactly one introduced word; the completed answer shows the expected form.",
        )

    private fun unknown(
        raw: Answer,
        text: String,
        code: String,
        feedback: String,
        constraint: ResponseConstraintStatus = ResponseConstraintStatus.MET,
    ): Evaluation =
        Evaluation(
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

    private fun result(
        task: ModuleOneTask,
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
        feedbackCompletion(task),
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

    private fun feedbackCompletion(task: ModuleOneTask): String =
        if (task.targetLexicalId == "L010") completedSentence(task, task.expectedForm) else task.expectedForm
}

private fun completedSentence(task: ModuleOneTask, answer: String): String =
    if (task.before.isEmpty() && task.after.isEmpty()) answer else "${task.before}$answer${task.after}"
