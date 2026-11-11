package app.astrid.engine

internal data class NeutralQuestionTask(
    val id: String,
    val positiveProposition: String,
    val englishQuestion: String,
    val referenceContext: String,
    val frameVariantId: String,
    val parts: List<SuppliedOrderPart>,
) {
    val targetScopeId = NeutralQuestionContent.TARGET_SCOPE_ID
    val answerExposureKeys = listOf(
        NeutralQuestionContent.QUESTION_PATTERN_KEY,
        NeutralQuestionContent.exactOrderKey(id),
    )
    val promptExposureKeys =
        parts.flatMap { part -> part.lexicalIds.map(NeutralQuestionContent::componentKey) }.distinct()
    val lessonExposureKeys = NeutralQuestionContent.LESSON_EXPOSURE_KEYS
    val hintExposureKeys = listOf(NeutralQuestionContent.QUESTION_PATTERN_KEY)
    val feedbackExposureKeys = listOf(
        NeutralQuestionContent.exactOrderKey(id),
        NeutralQuestionContent.QUESTION_PATTERN_KEY,
    )
}

internal data class QuestionExerciseSource(
    val task: NeutralQuestionTask,
    override val answerKey: AnswerKey,
    override val blockRoles: Map<String, String> = emptyMap(),
) : ExerciseSource

internal object NeutralQuestionContent {
    const val FORMAT_ID = "E030"
    const val TARGET_SKILL_ID = "G018"
    const val TARGET_SCOPE_ID = "neutral-positive-yes-no"
    const val CONSTRUCTION_ID = "C010"
    const val QUESTION_PATTERN_KEY = "module-one:pattern:neutral-positive-yes-no"
    const val YES_MEANING_KEY = "module-one:meaning:L039:L039-S01"
    const val NO_MEANING_KEY = "module-one:meaning:L040:L040-S01"
    val SHORT_ANSWER_SUPPORT_KEYS = listOf(YES_MEANING_KEY, NO_MEANING_KEY)
    val LESSON_EXPOSURE_KEYS = listOf(QUESTION_PATTERN_KEY) + SHORT_ANSWER_SUPPORT_KEYS

    val tasks = listOf(
        NeutralQuestionTask(
            id = "question-read-newspaper",
            positiveProposition = "Du læser en avis.",
            englishQuestion = "Do you read a newspaper?",
            referenceContext = "The subject is one addressee.",
            frameVariantId = "s13-read-count-noun-question",
            parts = listOf(
                part("L011", "læser", "finite-verb"),
                part("L002", "du", "subject"),
                part("L015", "en avis", "object", listOf("L022", "L015")),
            ),
        ),
        NeutralQuestionTask(
            id = "question-work",
            positiveProposition = "Hun arbejder.",
            englishQuestion = "Does she work?",
            referenceContext = "The subject is one stated woman.",
            frameVariantId = "s13-work-question",
            parts = listOf(
                part("L027", "arbejder", "finite-verb"),
                part("L004", "hun", "subject"),
            ),
        ),
        NeutralQuestionTask(
            id = "question-residence-plural",
            positiveProposition = "I bor i Aarhus.",
            englishQuestion = "Do you all live in Aarhus?",
            referenceContext = "The subject is more than one addressee.",
            frameVariantId = "s13-residence-question",
            parts = listOf(
                part("L029", "bor", "finite-verb"),
                part("L006", "I", "subject"),
                part("L032", "i Aarhus", "location-complement"),
            ),
        ),
        NeutralQuestionTask(
            id = "question-possession-letter",
            positiveProposition = "De har et brev.",
            englishQuestion = "Do they have a letter?",
            referenceContext = "The subject is several other people; the formal singular address reading is excluded.",
            frameVariantId = "s13-possession-question",
            parts = listOf(
                part("L009", "har", "finite-verb"),
                part("L007", "de", "subject"),
                part("L018", "et brev", "object", listOf("L023", "L018")),
            ),
        ),
    )

    fun introduction() = listOf(
        IntroductionSection(
            "Requested question pattern",
            "finite verb | subject | supplied remainder",
        ),
        IntroductionSection(
            "Neutral positive questions",
            "Move the supplied finite verb before the subject. Danish uses no English do auxiliary in these frames.",
        ),
        IntroductionSection("Worked reading statement", "Han læser en bog.", "da"),
        IntroductionSection("Worked reading statement meaning", "He reads a book."),
        IntroductionSection("Worked reading question", "Læser han en bog?", "da"),
        IntroductionSection("Worked reading question meaning", "Does he read a book?"),
        IntroductionSection("Worked plural statement", "I arbejder.", "da"),
        IntroductionSection("Worked plural statement meaning", "You all work."),
        IntroductionSection("Worked plural question", "Arbejder I?", "da"),
        IntroductionSection("Worked plural question meaning", "Do you all work?"),
        IntroductionSection("Plural subject form", "I", "da"),
        IntroductionSection("Plural subject meaning", "you all"),
        IntroductionSection("Location preposition", "i", "da"),
        IntroductionSection("Location preposition meaning", "in"),
        IntroductionSection("Location phrase", "i Aarhus", "da"),
        IntroductionSection("Location phrase meaning", "in Aarhus"),
        IntroductionSection("Positive short answer", "ja", "da"),
        IntroductionSection("Positive short-answer meaning", "yes"),
        IntroductionSection("Negative short answer", "nej", "da"),
        IntroductionSection("Negative short-answer meaning", "no"),
        IntroductionSection(
            "Boundary",
            "This round requests only neutral positive yes/no questions. Modal, negative, echo, and " +
                "sentence-initial modal-particle questions are excluded.",
        ),
    )

    fun exactOrderKey(taskId: String) = "module-one:exact-order:$taskId"
    fun componentKey(lexicalId: String) = "module-one:supplied-component:$lexicalId"

    private fun part(
        lexicalId: String,
        text: String,
        form: String,
        lexicalIds: List<String> = listOf(lexicalId),
    ) = SuppliedOrderPart(lexicalId, text, form, lexicalIds)
}

internal object NeutralQuestionExerciseGenerator {
    fun generate(request: GenerationRequest): GeneratedExercise {
        val taskIndex =
            Math.floorMod(
                request.seed.toLong() + request.index.toLong(),
                NeutralQuestionContent.tasks.size.toLong(),
            ).toInt()
        return generate(request, NeutralQuestionContent.tasks[taskIndex])
    }

    fun generate(request: GenerationRequest, task: NeutralQuestionTask): GeneratedExercise {
        val shuffledParts = rotate(task.parts, request.seed, request.index)
        val blocks = shuffledParts.mapIndexed { index, part -> OrderBlock("b$index", part.text) }
        val tokens = shuffledParts.mapIndexed { index, part -> TileTokenDetails("b$index", part.lexicalId, part.form) }
        val canonical = task.parts.map { TileIdentity(it.lexicalId, it.form) }
        val canonicalIds = canonical.map { identity ->
            tokens.single { it.lexicalId == identity.lexicalId && it.form == identity.form }.blockId
        }
        val blockRoles = tokens.associate { token -> token.blockId to clauseRole(token.form) }
        return GeneratedExercise(
            id =
                "E030-${request.contentRevision}-${request.generatorRevision}-${task.id}-" +
                    "${request.seed}-${request.index}",
            formatId = NeutralQuestionContent.FORMAT_ID,
            targetSkillId = NeutralQuestionContent.TARGET_SKILL_ID,
            lexicalTarget = null,
            supportSkillIds = listOf("G001", "G002", "G003", "G004", "G005"),
            presentation = ExercisePresentation(
                instruction =
                    "Arrange every supplied constituent to ask the neutral positive yes/no question. " +
                        "Put the finite verb first and the subject second.",
                context =
                    "${task.referenceContext} English question: ${task.englishQuestion}",
                prompt = ExercisePrompt(
                    before = "",
                    after = "",
                    blocks = blocks,
                    punctuation = "?",
                    cue = task.positiveProposition,
                    cueLanguage = "da",
                    answerLanguage = "da",
                ),
                introduction = NeutralQuestionContent.introduction(),
                hint = "Place the finite verb first and the subject second; keep any supplied remainder intact.",
            ),
            contentRevision = request.contentRevision,
            generatorRevision = request.generatorRevision,
            reproduction = Reproduction(request.seed, request.index),
            inspector = ExerciseInspector(
                canonicalSentenceId = task.id,
                constructionId = NeutralQuestionContent.CONSTRUCTION_ID,
                lexicalIds = task.parts.flatMap(SuppliedOrderPart::lexicalIds),
                clauseRoles = task.parts.map { clauseRole(it.form) },
                canonicalSentence = question(task.parts.map(SuppliedOrderPart::text)),
                tileSource = TileSourceDetails(
                    tokens,
                    listOf(TileLicensedOrderDetails(task.frameVariantId, canonicalIds)),
                    emptyList(),
                ),
            ),
            source = QuestionExerciseSource(task, AnswerKey.TileOrder(canonical, null, null), blockRoles),
        )
    }

    private fun <T> rotate(values: List<T>, seed: Int, index: Int): List<T> {
        val offset = Math.floorMod(seed.toLong() * SEED_FACTOR + index * INDEX_FACTOR, values.size.toLong()).toInt()
        return values.drop(offset) + values.take(offset)
    }

    private fun question(parts: List<String>) = parts.joinToString(" ").replaceFirstChar(Char::titlecase) + "?"

    private fun clauseRole(form: String) = when (form) {
        "finite-verb" -> "FINITE_VERB"
        "subject" -> "SUBJECT"
        "object" -> "OBJECT"
        "location-complement" -> "LOCATION_COMPLEMENT"
        else -> error("Unsupported question part form: $form")
    }

    private const val SEED_FACTOR = 31L
    private const val INDEX_FACTOR = 17L
}

internal object NeutralQuestionExerciseEvaluator {
    // Early returns keep malformed-input handling separate from complete-order grading.
    @Suppress("ReturnCount")
    fun evaluate(exercise: GeneratedExercise, answer: Answer): Evaluation {
        val source = exercise.source as? QuestionExerciseSource
            ?: return mismatch(answer, "The exercise does not contain a neutral-question task.")
        val ids = (answer as? Answer.Order)?.blockIds
            ?: return mismatch(answer, "The answer type does not match the exercise format.")
        val tokens = requireNotNull(exercise.inspector.tileSource).tokens
        val byId = tokens.associateBy(TileTokenDetails::blockId)
        if (ids.isEmpty() || ids.distinct().size != ids.size || !ids.all(byId::containsKey)) {
            return mismatch(answer, "The supplied block identifiers are malformed.")
        }
        val canonical = canonicalIdentities(exercise, source)
        if (ids.size < canonical.size) {
            return result(
                TargetStatus.UNEVALUABLE,
                LinguisticStatus.UNKNOWN,
                "REQUIRED_WORDS_MISSING",
                "Use every supplied constituent to complete the requested question.",
                exercise,
                answer,
                ids,
                ResponseConstraintStatus.NOT_MET,
                MeaningSatisfaction.UNKNOWN,
            )
        }
        if (ids.size != canonical.size) {
            return mismatch(answer, "The supplied block identifiers are malformed.")
        }
        val identities = ids.map { id -> byId.getValue(id).let { TileIdentity(it.lexicalId, it.form) } }
        if (identities == canonical) {
            return result(
                TargetStatus.MET,
                LinguisticStatus.ACCEPTABLE,
                null,
                "Correct. The finite verb is first and the subject is second in the requested neutral question.",
                exercise,
                answer,
                ids,
                ResponseConstraintStatus.MET,
                MeaningSatisfaction.MET,
            )
        }
        val targetMet = identities.getOrNull(0)?.form == "finite-verb" && identities.getOrNull(1)?.form == "subject"
        return result(
            if (targetMet) TargetStatus.MET else TargetStatus.NOT_MET,
            LinguisticStatus.UNKNOWN,
            "REQUESTED_ORDER_CONSTRAINT",
            if (targetMet) {
                "The target positions are met, but keep the supplied remainder in its requested order."
            } else {
                "Put the supplied finite verb first and subject second for the requested neutral question."
            },
            exercise,
            answer,
            ids,
            ResponseConstraintStatus.NOT_MET,
            MeaningSatisfaction.UNKNOWN,
        )
    }

    private fun canonicalIdentities(
        exercise: GeneratedExercise,
        source: QuestionExerciseSource,
    ): List<TileIdentity> {
        val tileSource = requireNotNull(exercise.inspector.tileSource)
        val tokens = tileSource.tokens.associateBy(TileTokenDetails::blockId)
        val licensed = tileSource.licensedOrders.single().blockIds.map { blockId ->
            requireNotNull(tokens[blockId]) { "Licensed question order references unknown block $blockId" }
                .let { TileIdentity(it.lexicalId, it.form) }
        }
        val privateKey = (source.answerKey as AnswerKey.TileOrder).canonical
        require(privateKey == licensed) {
            "Private question answer key does not match the licensed inspector order"
        }
        return privateKey
    }

    private fun result(
        target: TargetStatus,
        linguistic: LinguisticStatus,
        issueCode: String?,
        feedback: String,
        exercise: GeneratedExercise,
        raw: Answer,
        ids: List<String>,
        constraint: ResponseConstraintStatus,
        meaning: MeaningSatisfaction,
    ) = Evaluation(
        targetStatus = target,
        linguisticStatus = linguistic,
        issues = issueCode?.let { listOf(EvaluationIssue(it, feedback)) }.orEmpty(),
        feedback = feedback,
        completedSentence = exercise.inspector.canonicalSentence,
        rawAnswer = raw,
        normalizedAnswer = NormalizedAnswer.Order(ids),
        linguisticScope = LinguisticScope.DANISH_SENTENCE,
        responseConstraintStatus = constraint,
        meaningSatisfaction = meaning,
    )

    private fun mismatch(answer: Answer, detail: String) = Evaluation(
        targetStatus = TargetStatus.UNEVALUABLE,
        linguisticStatus = LinguisticStatus.UNKNOWN,
        issues = listOf(EvaluationIssue("ANSWER_TYPE_MISMATCH", detail)),
        feedback = "The answer could not be evaluated for this exercise.",
        completedSentence = null,
        rawAnswer = answer,
        normalizedAnswer = when (answer) {
            is Answer.Text -> NormalizedAnswer.Text(normalizeText(answer.text))
            is Answer.Order -> NormalizedAnswer.Order(answer.blockIds)
            is Answer.Matching -> NormalizedAnswer.Matching(answer.pairs)
            is Answer.Gaps -> NormalizedAnswer.Gaps(answer.selections)
        },
        linguisticScope = LinguisticScope.DANISH_SENTENCE,
        responseConstraintStatus = ResponseConstraintStatus.NOT_MET,
        meaningSatisfaction = MeaningSatisfaction.UNKNOWN,
    )
}
