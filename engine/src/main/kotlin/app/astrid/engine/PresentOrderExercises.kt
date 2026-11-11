package app.astrid.engine

import java.util.Locale

internal sealed interface PresentOrderTask {
    val id: String
    val formatId: String
    val targetScopeId: String
    val answerExposureKeys: List<String>
    val promptExposureKeys: List<String>
    val lessonExposureKeys: List<String>
    val hintExposureKeys: List<String>
    val feedbackExposureKeys: List<String>
}

internal data class NamedPresentTask(
    override val id: String,
    override val formatId: String,
    val subjectId: String,
    val subject: String,
    val reference: String,
    val verb: Verb,
    val complement: String?,
    val complementLexicalIds: List<String>,
    val scopeId: String = PresentOrderContent.PRESENT_SCOPE_ID,
    val patternKey: String = PresentOrderContent.PRESENT_PATTERN_KEY,
    val englishProposition: String? = null,
    val complementMeaning: String? = null,
    val constructionId: String = if (complement == null) "C001" else "C002",
    val complementRole: String? = if (complement == null) null else "OBJECT",
    val knownPresentForms: Set<String> = PresentOrderContent.retainedIntroducedPresents(),
    val teaching: NamedPresentTeaching? = null,
    val suppliedLabel: String = "object",
) : PresentOrderTask {
    override val targetScopeId = scopeId
    override val answerExposureKeys = listOf(
        PresentOrderContent.presentKey(verb.lexicalId),
        PresentOrderContent.infinitiveKey(verb.lexicalId),
        patternKey,
    )
    override val promptExposureKeys = listOf(PresentOrderContent.infinitiveKey(verb.lexicalId)) +
        (if (formatId == "E022") listOf(PresentOrderContent.presentKey(verb.lexicalId)) else emptyList()) +
        if (scopeId in setOf(PresentOrderContent.POSSESSION_SCOPE_ID, PresentOrderContent.PERSONAL_SCOPE_ID)) {
            complementLexicalIds.map(PresentOrderContent::componentKey)
        } else {
            emptyList()
        }
    override val lessonExposureKeys = answerExposureKeys
    override val hintExposureKeys = listOf(patternKey) +
        if (scopeId == PresentOrderContent.PERSONAL_SCOPE_ID) {
            listOf(PresentOrderContent.infinitiveKey(verb.lexicalId), PresentOrderContent.presentKey(verb.lexicalId))
        } else {
            emptyList()
        }
    override val feedbackExposureKeys = listOf(PresentOrderContent.presentKey(verb.lexicalId))
}

internal data class NamedPresentTeaching(
    val meaning: String,
    val workedExample: String,
    val workedMeaning: String,
    val hint: String,
)

internal data class SuppliedOrderPart(
    val lexicalId: String,
    val text: String,
    val form: String,
    val lexicalIds: List<String> = listOf(lexicalId),
)

internal data class SubjectFirstOrderTask(
    override val id: String,
    override val formatId: String,
    val english: String,
    val reference: String,
    val parts: List<SuppliedOrderPart>,
) : PresentOrderTask {
    override val targetScopeId = PresentOrderContent.ORDER_SCOPE_ID
    override val promptExposureKeys = parts.flatMap { part ->
        if (part.form == "finite-present") {
            listOf(PresentOrderContent.presentKey(part.lexicalId))
        } else {
            part.lexicalIds.map(PresentOrderContent::componentKey)
        }
    }
    override val answerExposureKeys =
        listOf(PresentOrderContent.orderKey(id), PresentOrderContent.ORDER_PATTERN_KEY) + promptExposureKeys
    override val lessonExposureKeys = listOf(PresentOrderContent.ORDER_PATTERN_KEY)
    override val hintExposureKeys = listOf(PresentOrderContent.ORDER_PATTERN_KEY)
    override val feedbackExposureKeys = answerExposureKeys
}

internal object PresentOrderContent {
    const val PRESENT_SCOPE_ID = "named-three-verbs-present"
    const val ORDER_SCOPE_ID = "subject-first-supplied"
    const val PRESENT_PATTERN_KEY = "module-one:pattern:introduced-present-r"
    const val POSSESSION_SCOPE_ID = "have-possession-present"
    const val POSSESSION_PATTERN_KEY = "module-one:pattern:have-possession-present"
    const val PERSONAL_SCOPE_ID = "personal-introduction-present"
    const val PERSONAL_PATTERN_KEY = "module-one:pattern:personal-introduction-present"
    const val ORDER_PATTERN_KEY = "module-one:pattern:subject-first-finite-second"
    val work = Verb("L027", "arbejde", "arbejder", "work")
    private val retainedPresentVerbs = listOf(work, PilotContent.read, PilotContent.sleep)

    fun tasks(
        formatId: String,
        includePossession: Boolean = false,
        includePersonal: Boolean = false,
        includeModalConstruction: Boolean = false,
    ): List<PresentOrderTask> {
        val tasks = retainedTasks(formatId) +
            (if (includePossession) possessionTasks(formatId) else emptyList()) +
            (if (includePersonal) PersonalIntroductionContent.tasks(formatId) else emptyList()) +
            (if (includeModalConstruction && formatId == "E025") {
                ModalRoundContent.constructionTasks()
            } else {
                emptyList()
            })
        return if (includePersonal) tasks.map(::withPairTwelveForms) else tasks
    }

    private fun retainedTasks(formatId: String): List<PresentOrderTask> = when (formatId) {
        "E022" -> listOf(
            presentTask("present-select-work", formatId, "L001", "jeg", "the speaker", work),
            presentTask(
                "present-select-read", formatId, "L004", "hun", "one stated woman", PilotContent.read,
                "en avis", listOf("L022", "L015"),
            ),
            presentTask("present-select-sleep", formatId, "L005", "vi", "the speaker and another person", PilotContent.sleep),
        )
        "E023" -> listOf(
            presentTask("present-recall-work", formatId, "L007", "de", "several other people", work),
            presentTask(
                "present-recall-read", formatId, "L006", "I", "several addressees", PilotContent.read,
                "en bog", listOf("L022", "L014"),
            ),
            presentTask("present-recall-sleep", formatId, "L003", "han", "one stated man", PilotContent.sleep),
        )
        "E024" -> listOf(
            orderTask(
                OrderTaskSeed("order-constituents-we-work", "We work.", "the speaker and another person", "L005", "vi", work),
                formatId,
            ),
            orderTask(
                OrderTaskSeed("order-constituents-you-sleep", "You sleep.", "one addressee", "L002", "du", PilotContent.sleep),
                formatId,
            ),
            orderTask(
                OrderTaskSeed(
                    "order-constituents-he-reads-book", "He reads a book.", "one stated man", "L003", "han",
                    PilotContent.read, "en bog", listOf("L022", "L014"), bundleComplement = true,
                ),
                formatId,
            ),
        )
        "E025" -> listOf(
            orderTask(
                OrderTaskSeed(
                    "order-words-i-read-newspaper", "I read a newspaper.", "the speaker", "L001", "jeg",
                    PilotContent.read, "en avis", listOf("L022", "L015"),
                ),
                formatId,
            ),
            orderTask(
                OrderTaskSeed(
                    "order-words-you-read-letter", "You read a letter.", "several addressees", "L006", "I",
                    PilotContent.read, "et brev", listOf("L023", "L018"),
                ),
                formatId,
            ),
            orderTask(
                OrderTaskSeed(
                    "order-words-they-read-book", "They read a book.", "several other people", "L007", "de",
                    PilotContent.read, "en bog", listOf("L022", "L014"),
                ),
                formatId,
            ),
        )
        else -> error("Unsupported present/order format $formatId")
    }

    private fun possessionTasks(formatId: String): List<PresentOrderTask> = when (formatId) {
        "E022" -> listOf(
            possessionTask(
                "possession-select-letter", formatId, "L003", "han", "one stated man", "He has a letter.",
                "et brev", "a letter", listOf("L023", "L018"),
            ),
            possessionTask(
                "possession-select-newspaper", formatId, "L005", "vi", "the speaker and another person",
                "We have a newspaper.", "en avis", "a newspaper", listOf("L022", "L015"),
            ),
        )
        "E023" -> listOf(
            possessionTask(
                "possession-recall-book", formatId, "L002", "du", "one addressee", "You have a book.",
                "en bog", "a book", listOf("L022", "L014"),
            ),
            possessionTask(
                "possession-recall-letter", formatId, "L007", "de", "several other people", "They have a letter.",
                "et brev", "a letter", listOf("L023", "L018"),
            ),
        )
        else -> emptyList()
    }

    private fun withPairTwelveForms(task: PresentOrderTask): PresentOrderTask =
        if (task is NamedPresentTask) task.copy(knownPresentForms = pairTwelveIntroducedPresents()) else task

    private fun presentTask(
        id: String,
        formatId: String,
        subjectId: String,
        subject: String,
        reference: String,
        verb: Verb,
        complement: String? = null,
        complementIds: List<String> = emptyList(),
    ) = NamedPresentTask(id, formatId, subjectId, subject, reference, verb, complement, complementIds)

    private fun possessionTask(
        id: String,
        formatId: String,
        subjectId: String,
        subject: String,
        reference: String,
        englishProposition: String,
        complement: String,
        complementMeaning: String,
        complementIds: List<String>,
    ) = NamedPresentTask(
        id, formatId, subjectId, subject, reference, PilotContent.have, complement, complementIds,
        POSSESSION_SCOPE_ID, POSSESSION_PATTERN_KEY, englishProposition, complementMeaning,
    )

    private data class OrderTaskSeed(
        val id: String,
        val english: String,
        val reference: String,
        val subjectId: String,
        val subject: String,
        val verb: Verb,
        val complement: String? = null,
        val complementIds: List<String> = emptyList(),
        val bundleComplement: Boolean = false,
    )

    private fun orderTask(seed: OrderTaskSeed, formatId: String): SubjectFirstOrderTask {
        val parts = buildList {
            add(SuppliedOrderPart(seed.subjectId, seed.subject, "subject"))
            add(SuppliedOrderPart(seed.verb.lexicalId, seed.verb.present, "finite-present"))
            if (seed.complement != null && seed.bundleComplement) {
                add(SuppliedOrderPart(seed.complementIds.last(), seed.complement, "supplied-noun-bundle", seed.complementIds))
            } else if (seed.complement != null) {
                val words = seed.complement.split(' ')
                add(SuppliedOrderPart(seed.complementIds[0], words[0], "article"))
                add(SuppliedOrderPart(seed.complementIds[1], words[1], "singular-headword"))
            }
        }
        return SubjectFirstOrderTask(seed.id, formatId, seed.english, seed.reference, parts)
    }

    fun presentKey(lexicalId: String) = "module-one:verb-present:$lexicalId"
    fun infinitiveKey(lexicalId: String) = "module-one:verb-infinitive:$lexicalId"
    fun componentKey(lexicalId: String) = "module-one:supplied-component:$lexicalId"
    fun orderKey(taskId: String) = "module-one:exact-order:$taskId"
    fun retainedIntroducedPresents() = retainedPresentVerbs.mapTo(mutableSetOf(), Verb::present)
    fun pairTwelveIntroducedPresents() =
        (retainedPresentVerbs + PilotContent.have + PilotContent.be + PilotContent.personalIntroductionVerbs)
            .mapTo(mutableSetOf(), Verb::present)
}

internal object PresentOrderExerciseGenerator {
    fun generate(request: GenerationRequest): GeneratedExercise {
        val flags = when (request.contentRevision) {
            PilotContent.RETAINED_ELEVEN_CONTENT_REVISION -> Triple(true, false, false)
            PilotContent.RETAINED_TWELVE_CONTENT_REVISION,
            PilotContent.RETAINED_THIRTEEN_CONTENT_REVISION,
            -> Triple(true, true, false)
            PilotContent.CURRENT_CONTENT_REVISION -> Triple(true, true, true)
            else -> Triple(false, false, false)
        }
        val tasks = PresentOrderContent.tasks(request.formatId, flags.first, flags.second, flags.third)
        val offset = request.seed.toLong() * TASK_SEED_FACTOR + request.index.toLong() * TASK_INDEX_FACTOR
        val task = tasks[Math.floorMod(offset, tasks.size.toLong()).toInt()]
        return when (task) {
            is NamedPresentTask -> presentExercise(request, task)
            is SubjectFirstOrderTask -> orderExercise(request, task)
            is ModalConstructionTask -> ModalRoundExerciseGenerator.generateConstruction(request, task)
        }
    }

    private fun presentExercise(request: GenerationRequest, task: NamedPresentTask): GeneratedExercise {
        val selection = request.formatId == "E022"
        val choices = if (selection) {
            presentChoices(
                task,
                request.seed,
                request.index,
                request.contentRevision in setOf(
                    PilotContent.RETAINED_TWELVE_CONTENT_REVISION,
                    PilotContent.RETAINED_THIRTEEN_CONTENT_REVISION,
                    PilotContent.CURRENT_CONTENT_REVISION,
                ),
            )
        } else {
            emptyList()
        }
        val after = task.complement?.let { " $it." } ?: "."
        return GeneratedExercise(
            id = "${request.formatId}-${request.contentRevision}-${request.generatorRevision}-${task.id}-${request.seed}-${request.index}",
            formatId = request.formatId,
            targetSkillId = "G004",
            lexicalTarget = null,
            supportSkillIds = listOf("G001", "G002", "G003") +
                if (task.scopeId == PresentOrderContent.PERSONAL_SCOPE_ID) listOf("G005") else emptyList(),
            presentation = ExercisePresentation(
                instruction = if (selection) {
                    "Choose the one-word present form of the named Danish verb."
                } else {
                    "Type the one-word present form of the named Danish verb."
                },
                context = presentContext(task),
                prompt = ExercisePrompt(
                    task.subject.replaceFirstChar { it.titlecase() } + " ", after, choices,
                    inputLabel = if (selection) null else "Danish present verb",
                    cue = "at ${task.verb.lemma}", cueLanguage = "da", answerLanguage = "da",
                ),
                introduction = presentIntroduction(task),
                hint = task.teaching?.hint ?: if (task.scopeId == PresentOrderContent.POSSESSION_SCOPE_ID) {
                    "Use the stored one-word present form har from at have."
                } else "Use the introduced one-word present form ending in -r from the named pair.",
            ),
            contentRevision = request.contentRevision,
            generatorRevision = request.generatorRevision,
            reproduction = Reproduction(request.seed, request.index),
            inspector = ExerciseInspector(
                canonicalSentenceId = task.id,
                constructionId = task.constructionId,
                lexicalIds = listOf(task.subjectId, task.verb.lexicalId) + task.complementLexicalIds,
                clauseRoles = listOf("SUBJECT", "FINITE_VERB") + listOfNotNull(task.complementRole),
                canonicalSentence = canonical(task.subject, task.verb.present, task.complement),
                moduleOneSource = ModuleOneSourceDetails(
                    task.id,
                    task.verb.lexicalId,
                    task.verb.present,
                    (task.knownPresentForms + task.verb.lemma).filterNot { it == task.verb.present }.sorted(),
                    "finite-verb",
                    task.id,
                    "lowercase-mid-sentence",
                    choices.map {
                        ModuleOneChoiceRationale(
                            it,
                            if (it == task.verb.present) "finite present" else "dictionary infinitive",
                        )
                    },
                ),
            ),
            source = PresentOrderExerciseSource(
                task,
                AnswerKey.ModuleOneForm(
                    task.verb.present,
                    (task.knownPresentForms + task.verb.lemma) - task.verb.present,
                    selection,
                ),
            ),
        )
    }

    private fun orderExercise(request: GenerationRequest, task: SubjectFirstOrderTask): GeneratedExercise {
        val shuffledParts = shuffled(task.parts, request.seed, request.index)
        val tokens = shuffledParts.mapIndexed { index, part -> OrderBlock("b$index", part.text) }
        val tokenDetails = shuffledParts.mapIndexed { index, part -> TileTokenDetails("b$index", part.lexicalId, part.form) }
        val canonical = task.parts.map { part -> TileIdentity(part.lexicalId, part.form) }
        val canonicalIds = canonical.map { identity ->
            tokenDetails.single { it.lexicalId == identity.lexicalId && it.form == identity.form }.blockId
        }
        return GeneratedExercise(
            id = "${request.formatId}-${request.contentRevision}-${request.generatorRevision}-${task.id}-${request.seed}-${request.index}",
            formatId = request.formatId,
            targetSkillId = "G005",
            lexicalTarget = null,
            supportSkillIds = listOf("G001", "G002", "G003", "G004", "G006"),
            presentation = ExercisePresentation(
                instruction = if (request.formatId == "E024") {
                    "Arrange every supplied constituent to make the requested subject-first Danish statement."
                } else {
                    "Arrange the supplied words to make the requested subject-first Danish statement."
                },
                context = "The English subject refers to ${task.reference}. Use the supplied Danish material.",
                prompt = ExercisePrompt(
                    "", "", blocks = tokens, punctuation = ".", cue = task.english, cueLanguage = "en", answerLanguage = "da",
                    tileMode = if (request.formatId == "E025") "use-all" else null,
                    requiredTileCount = if (request.formatId == "E025") task.parts.size else null,
                ),
                introduction = orderIntroduction(),
                hint = "Put the supplied subject first and the finite verb second.",
            ),
            contentRevision = request.contentRevision,
            generatorRevision = request.generatorRevision,
            reproduction = Reproduction(request.seed, request.index),
            inspector = ExerciseInspector(
                canonicalSentenceId = task.id,
                constructionId = if (task.parts.size == 2) "C001" else "C002",
                lexicalIds = task.parts.flatMap(SuppliedOrderPart::lexicalIds),
                clauseRoles = listOf("SUBJECT", "FINITE_VERB") +
                    if (task.parts.size == 2) emptyList() else listOf("OBJECT"),
                canonicalSentence = sentence(task.parts.map(SuppliedOrderPart::text)),
                tileSource = TileSourceDetails(tokenDetails, listOf(TileLicensedOrderDetails("subject-first", canonicalIds)), emptyList()),
            ),
            source = PresentOrderExerciseSource(task, AnswerKey.TileOrder(canonical, null, null)),
        )
    }

    private fun presentIntroduction(task: NamedPresentTask) =
        if (task.teaching != null) {
            listOf(
                IntroductionSection("Dictionary and present forms", "at ${task.verb.lemma} → ${task.verb.present}", "da"),
                IntroductionSection("Meaning", task.teaching.meaning),
                IntroductionSection("Worked example", task.teaching.workedExample, "da"),
                IntroductionSection("Worked meaning", task.teaching.workedMeaning),
            )
        } else if (task.scopeId == PresentOrderContent.POSSESSION_SCOPE_ID) {
            listOf(
                IntroductionSection("Dictionary and present forms", "at have → har", "da"),
                IntroductionSection("Meaning", "The verb expresses possession here. Use the same present form with each subject."),
                IntroductionSection("Worked example", "Jeg har en bog.", "da"),
                IntroductionSection("Worked meaning", "I have a book."),
            )
        } else {
            listOf(
                IntroductionSection("Dictionary and present forms", "at ${task.verb.lemma} → ${task.verb.present}", "da"),
                IntroductionSection("Meaning", "The verb means to ${task.verb.gloss}."),
                IntroductionSection("Observed pattern", "For these introduced verbs, the one-word present form ends in -r."),
            )
        }

    private fun presentContext(task: NamedPresentTask): String = task.englishProposition?.let { proposition ->
        "English: $proposition The subject refers to ${task.reference}. The supplied ${task.suppliedLabel} " +
            "${task.complement} means ${task.complementMeaning}."
    } ?: "The subject refers to ${task.reference}. The named verb means to ${task.verb.gloss}."

    private fun orderIntroduction() = listOf(
        IntroductionSection("Constituents and words", "A constituent may contain one written word or an article-and-noun bundle."),
        IntroductionSection("Statement fields", "subject | finite verb | supplied object"),
        IntroductionSection(
            "Requested pattern",
            "In these statements, the subject is the first constituent and the finite verb is second.",
        ),
    )

    private fun canonical(subject: String, verb: String, complement: String?) = sentence(listOfNotNull(subject, verb, complement))
    private fun sentence(words: List<String>) = words.joinToString(" ").let {
        if (it.startsWith("I ")) it else it.replaceFirstChar(Char::titlecase)
    } + "."

    private fun <T> shuffled(values: List<T>, seed: Int, index: Int): List<T> =
        if ((seed xor index) and 1 == 0) values.reversed() else values.drop(1) + values.take(1)

    private fun presentChoices(
        task: NamedPresentTask,
        seed: Int,
        index: Int,
        independentOfIndex: Boolean,
    ): List<String> {
        val canonical = listOf(task.verb.lemma, task.verb.present)
        val parity = if (independentOfIndex) seed xor task.id.hashCode() else seed xor index
        return if (parity and 1 == 0) canonical else canonical.reversed()
    }

    private const val TASK_SEED_FACTOR = 31L
    private const val TASK_INDEX_FACTOR = 17L
}

internal object PresentOrderExerciseEvaluator {
    fun evaluate(exercise: GeneratedExercise, answer: Answer): Evaluation {
        val source = exercise.source as PresentOrderExerciseSource
        return when (val task = source.task) {
            is NamedPresentTask -> evaluatePresent(task, answer)
            is SubjectFirstOrderTask -> evaluateOrder(exercise, task, answer, source.answerKey as AnswerKey.TileOrder)
            is ModalConstructionTask -> ModalRoundExerciseEvaluator.evaluateConstruction(exercise, answer)
        }
    }

    private fun evaluatePresent(task: NamedPresentTask, answer: Answer): Evaluation {
        val raw = answer as? Answer.Text ?: return mismatch(answer, LinguisticScope.DANISH_WORD_FORM)
        val normalized = normalizeText(raw.text)
        val folded = normalized.lowercase(Locale.ROOT)
        return when {
            normalized.isEmpty() -> presentResult(
                task, answer, normalized, TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN,
                "EMPTY_RESPONSE", "Enter one introduced present form.", ResponseConstraintStatus.NOT_MET,
            )
            normalized.any(Char::isWhitespace) -> presentResult(
                task, answer, normalized, TargetStatus.UNEVALUABLE, LinguisticStatus.KNOWN_ISSUE,
                "ONE_WORD_RESPONSE_REQUIRED", "Enter exactly one word.", ResponseConstraintStatus.NOT_MET,
            )
            normalized == task.verb.present -> presentResult(
                task, answer, normalized, TargetStatus.MET, LinguisticStatus.ACCEPTABLE,
                null, "Correct. This is the named verb's present form.", ResponseConstraintStatus.MET,
            )
            normalized == task.verb.lemma -> presentResult(
                task, answer, normalized, TargetStatus.NOT_MET, LinguisticStatus.KNOWN_ISSUE,
                "INFINITIVE_USED", "Use the present form in this statement.", ResponseConstraintStatus.MET,
            )
            normalized in task.knownPresentForms -> presentResult(
                task, answer, normalized, TargetStatus.NOT_MET, LinguisticStatus.ACCEPTABLE,
                "NAMED_VERB_MISMATCH", "That is an introduced present form for a different verb.",
                ResponseConstraintStatus.MET,
            )
            folded == task.verb.present -> presentResult(
                task, answer, normalized, TargetStatus.MET, LinguisticStatus.KNOWN_ISSUE,
                "MID_SENTENCE_VERB_CASE_REQUIRED", "Use the lowercase mid-sentence form.",
                ResponseConstraintStatus.NOT_MET,
            )
            else -> presentResult(
                task, answer, normalized, TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN,
                "UNRECOGNIZED_PRESENT_FORM", "That response is outside the introduced forms for this task.",
                ResponseConstraintStatus.MET,
            )
        }
    }

    private fun presentResult(
        task: NamedPresentTask,
        raw: Answer,
        normalized: String,
        target: TargetStatus,
        linguistic: LinguisticStatus,
        code: String?,
        feedback: String,
        constraint: ResponseConstraintStatus,
    ) = Evaluation(
        target, linguistic, code?.let { listOf(EvaluationIssue(it, feedback)) }.orEmpty(), feedback,
        if (linguistic == LinguisticStatus.UNKNOWN) null else task.verb.present,
        raw, NormalizedAnswer.Text(normalized), LinguisticScope.DANISH_WORD_FORM, constraint,
        null,
    )

    // Each exit represents a distinct malformed, canonical, or bounded partial response class.
    @Suppress("ReturnCount")
    private fun evaluateOrder(
        exercise: GeneratedExercise,
        task: SubjectFirstOrderTask,
        answer: Answer,
        key: AnswerKey.TileOrder,
    ): Evaluation {
        val ids = (answer as? Answer.Order)?.blockIds ?: return mismatch(answer, LinguisticScope.DANISH_SENTENCE)
        val byId = requireNotNull(exercise.inspector.tileSource).tokens.associateBy(TileTokenDetails::blockId)
        if (ids.isEmpty() || ids.distinct().size != ids.size || !ids.all(byId::containsKey)) {
            return mismatch(answer, LinguisticScope.DANISH_SENTENCE)
        }
        val identities = ids.map { byId.getValue(it).let { token -> TileIdentity(token.lexicalId, token.form) } }
        val canonical = key.canonical
        val completed = exercise.inspector.canonicalSentence
        if (identities == canonical) return Evaluation(
            TargetStatus.MET, LinguisticStatus.ACCEPTABLE, emptyList(),
            "Correct. The supplied material forms the requested subject-first statement.", completed, answer,
            NormalizedAnswer.Order(ids), LinguisticScope.DANISH_SENTENCE, ResponseConstraintStatus.MET,
            MeaningSatisfaction.MET,
        )
        val partial = ids.size < canonical.size
        if (partial && task.formatId == "E024") return mismatch(answer, LinguisticScope.DANISH_SENTENCE)
        val target = if (partial) TargetStatus.UNEVALUABLE else placementStatus(identities)
        val code = if (partial) "REQUIRED_WORDS_MISSING" else "REQUESTED_ORDER_CONSTRAINT"
        val message = if (partial) {
            "Use every required word to complete the requested statement."
        } else {
            "Use the supplied material in the requested subject-first order."
        }
        return Evaluation(
            target, LinguisticStatus.UNKNOWN, listOf(EvaluationIssue(code, message)), message, completed,
            answer, NormalizedAnswer.Order(ids), LinguisticScope.DANISH_SENTENCE,
            ResponseConstraintStatus.NOT_MET, MeaningSatisfaction.UNKNOWN,
        )
    }

    private fun placementStatus(identities: List<TileIdentity>): TargetStatus = when {
        identities.size >= 2 && identities[0].form == "subject" && identities[1].form == "finite-present" -> TargetStatus.MET
        identities.indexOfFirst { it.form == "subject" } >= 0 &&
            identities.indexOfFirst { it.form == "finite-present" } >= 0 -> TargetStatus.NOT_MET
        else -> TargetStatus.UNEVALUABLE
    }

    private fun mismatch(answer: Answer, scope: LinguisticScope) = Evaluation(
        TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN,
        listOf(EvaluationIssue("ANSWER_TYPE_MISMATCH", "The answer type does not match the exercise format.")),
        "The answer could not be evaluated for this exercise.", null, answer, normalized(answer), scope,
        ResponseConstraintStatus.NOT_MET,
        if (scope == LinguisticScope.DANISH_WORD_FORM) null else MeaningSatisfaction.UNKNOWN,
    )

    private fun normalized(answer: Answer): NormalizedAnswer = when (answer) {
        is Answer.Text -> NormalizedAnswer.Text(normalizeText(answer.text))
        is Answer.Order -> NormalizedAnswer.Order(answer.blockIds)
        is Answer.Matching -> NormalizedAnswer.Matching(answer.pairs)
        is Answer.Gaps -> NormalizedAnswer.Gaps(answer.selections)
    }
}
