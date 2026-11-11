package app.astrid.engine

internal data class FinalCircumstanceTask(
    val id: String,
    val englishCore: String,
    val circumstanceMeaning: String,
    val frameVariantId: String,
    val parts: List<SuppliedOrderPart>,
) {
    val targetScopeId = FinalCircumstanceContent.TARGET_SCOPE_ID
    val answerExposureKeys = listOf(
        FinalCircumstanceContent.FINAL_CIRCUMSTANCE_PATTERN_KEY,
        FinalCircumstanceContent.exactOrderKey(id),
    )
    val promptExposureKeys = parts.flatMap { part ->
        part.lexicalIds.map(FinalCircumstanceContent::componentKey)
    }.plus(
        parts.flatMap(SuppliedOrderPart::lexicalIds).mapNotNull(FinalCircumstanceContent::meaningKey),
    ).distinct()
    val lessonExposureKeys = FinalCircumstanceContent.LESSON_EXPOSURE_KEYS
    val hintExposureKeys = listOf(FinalCircumstanceContent.FINAL_CIRCUMSTANCE_PATTERN_KEY)
    val feedbackExposureKeys = listOf(
        FinalCircumstanceContent.exactOrderKey(id),
        FinalCircumstanceContent.FINAL_CIRCUMSTANCE_PATTERN_KEY,
    )
}

internal data class CircumstanceExerciseSource(
    val task: FinalCircumstanceTask,
    override val answerKey: AnswerKey,
    override val blockRoles: Map<String, String> = emptyMap(),
) : ExerciseSource

internal object FinalCircumstanceContent {
    const val FORMAT_ID = "E029"
    const val TARGET_SKILL_ID = "G016"
    const val TARGET_SCOPE_ID = "one-final-circumstance"
    const val CONSTRUCTION_ID = "C007"
    const val FINAL_CIRCUMSTANCE_PATTERN_KEY = "module-one:pattern:one-final-circumstance"
    const val TODAY_MEANING_KEY = "module-one:meaning:L037:L037-S01"
    const val NOW_MEANING_KEY = "module-one:meaning:L038:L038-S01"

    val LESSON_EXPOSURE_KEYS = listOf(
        FINAL_CIRCUMSTANCE_PATTERN_KEY,
        TODAY_MEANING_KEY,
        NOW_MEANING_KEY,
        componentKey("L032"),
        componentKey("L037"),
        componentKey("L038"),
    )

    val tasks = listOf(
        FinalCircumstanceTask(
            id = "circumstance-work-today",
            englishCore = "I work.",
            circumstanceMeaning = "today",
            frameVariantId = "s12-work-final-time",
            parts = listOf(
                part("L001", "jeg", "subject"),
                part("L027", "arbejder", "finite-verb"),
                part("L037", "i dag", "circumstance"),
            ),
        ),
        FinalCircumstanceTask(
            id = "circumstance-read-letter-now",
            englishCore = "She reads a letter.",
            circumstanceMeaning = "now",
            frameVariantId = "s12-read-final-time",
            parts = listOf(
                part("L004", "hun", "subject"),
                part("L011", "læser", "finite-verb"),
                part("L018", "et brev", "object", listOf("L023", "L018")),
                part("L038", "nu", "circumstance"),
            ),
        ),
        FinalCircumstanceTask(
            id = "circumstance-work-aarhus",
            englishCore = "We work.",
            circumstanceMeaning = "in Aarhus",
            frameVariantId = "s12-work-final-place",
            parts = listOf(
                part("L005", "vi", "subject"),
                part("L027", "arbejder", "finite-verb"),
                part("L032", "i Aarhus", "circumstance"),
            ),
        ),
        FinalCircumstanceTask(
            id = "circumstance-read-newspaper-today",
            englishCore = "They read a newspaper.",
            circumstanceMeaning = "today",
            frameVariantId = "s12-read-final-time",
            parts = listOf(
                part("L007", "de", "subject"),
                part("L011", "læser", "finite-verb"),
                part("L015", "en avis", "object", listOf("L022", "L015")),
                part("L037", "i dag", "circumstance"),
            ),
        ),
    )

    fun introduction() = listOf(
        IntroductionSection(
            "Clause core and one circumstance",
            "Keep the supplied clause core in its original order and add the supplied time or place " +
                "circumstance at the end.",
        ),
        IntroductionSection("Today", "i dag", "da"),
        IntroductionSection("Now", "nu", "da"),
        IntroductionSection("In Aarhus", "i Aarhus", "da"),
        IntroductionSection(
            "Word units",
            "The expression meaning today contains two written words and fills one sentence constituent.",
        ),
        IntroductionSection("Worked time example", "Du arbejder nu.", "da"),
        IntroductionSection("Worked time meaning", "You work now."),
        IntroductionSection("Worked two-word example", "Han læser en bog i dag.", "da"),
        IntroductionSection("Worked two-word meaning", "He reads a book today."),
        IntroductionSection("Worked place example", "Jeg arbejder i Aarhus.", "da"),
        IntroductionSection("Worked place meaning", "I work in Aarhus."),
        IntroductionSection(
            "Boundary",
            "This round requests one supplied final circumstance. It does not claim that Danish " +
                "circumstances always occur last.",
        ),
    )

    fun exactOrderKey(taskId: String) = "module-one:exact-order:$taskId"
    fun componentKey(lexicalId: String) = "module-one:supplied-component:$lexicalId"
    fun meaningKey(lexicalId: String) = when (lexicalId) {
        "L037" -> TODAY_MEANING_KEY
        "L038" -> NOW_MEANING_KEY
        else -> null
    }

    private fun part(
        lexicalId: String,
        text: String,
        form: String,
        lexicalIds: List<String> = listOf(lexicalId),
    ) = SuppliedOrderPart(lexicalId, text, form, lexicalIds)
}

internal object FinalCircumstanceExerciseGenerator {
    fun generate(request: GenerationRequest): GeneratedExercise {
        val taskIndex =
            Math.floorMod(
                request.seed.toLong() + request.index.toLong(),
                FinalCircumstanceContent.tasks.size.toLong(),
            ).toInt()
        val task = FinalCircumstanceContent.tasks[taskIndex]
        return generate(request, task)
    }

    fun generate(request: GenerationRequest, task: FinalCircumstanceTask): GeneratedExercise {
        val shuffledParts = rotate(task.parts, request.seed, request.index)
        val tokens = shuffledParts.mapIndexed { index, part -> OrderBlock("b$index", part.text) }
        val details = shuffledParts.mapIndexed { index, part -> TileTokenDetails("b$index", part.lexicalId, part.form) }
        val canonical = task.parts.map { TileIdentity(it.lexicalId, it.form) }
        val canonicalIds = canonical.map { identity ->
            details.single { it.lexicalId == identity.lexicalId && it.form == identity.form }.blockId
        }
        val blockRoles = details.associate { detail -> detail.blockId to clauseRole(detail.form) }
        return GeneratedExercise(
            id =
                "E029-${request.contentRevision}-${request.generatorRevision}-${task.id}-" +
                    "${request.seed}-${request.index}",
            formatId = FinalCircumstanceContent.FORMAT_ID,
            targetSkillId = FinalCircumstanceContent.TARGET_SKILL_ID,
            lexicalTarget = null,
            supportSkillIds = listOf("G001", "G002", "G003", "G004", "G005"),
            presentation = ExercisePresentation(
                instruction =
                    "Keep the supplied clause in its original order and add the time or place " +
                        "circumstance at the end.",
                context = "Supplied clause: ${task.englishCore} Supplied circumstance: ${task.circumstanceMeaning}.",
                prompt = ExercisePrompt(
                    before = "",
                    after = "",
                    blocks = tokens,
                    punctuation = ".",
                    cue = sentence(task.parts.dropLast(1).map(SuppliedOrderPart::text)),
                    cueLanguage = "da",
                    answerLanguage = "da",
                ),
                introduction = FinalCircumstanceContent.introduction(),
                hint = "Keep the clause core together and place the supplied circumstance last.",
            ),
            contentRevision = request.contentRevision,
            generatorRevision = request.generatorRevision,
            reproduction = Reproduction(request.seed, request.index),
            inspector = ExerciseInspector(
                canonicalSentenceId = task.id,
                constructionId = FinalCircumstanceContent.CONSTRUCTION_ID,
                lexicalIds = task.parts.flatMap(SuppliedOrderPart::lexicalIds),
                clauseRoles = task.parts.map { clauseRole(it.form) },
                canonicalSentence = sentence(task.parts.map(SuppliedOrderPart::text)),
                tileSource = TileSourceDetails(
                    details,
                    listOf(TileLicensedOrderDetails(task.frameVariantId, canonicalIds)),
                    emptyList(),
                ),
            ),
            source = CircumstanceExerciseSource(
                task,
                AnswerKey.TileOrder(canonical, null, null),
                blockRoles,
            ),
        )
    }

    private fun <T> rotate(values: List<T>, seed: Int, index: Int): List<T> {
        val offset = Math.floorMod(seed.toLong() * SEED_FACTOR + index * INDEX_FACTOR, values.size.toLong()).toInt()
        return values.drop(offset) + values.take(offset)
    }

    private fun sentence(parts: List<String>) = parts.joinToString(" ").replaceFirstChar(Char::titlecase) + "."

    private fun clauseRole(form: String) = when (form) {
        "subject" -> "SUBJECT"
        "finite-verb" -> "FINITE_VERB"
        "object" -> "OBJECT"
        "circumstance" -> "CIRCUMSTANCE"
        else -> error("Unsupported circumstance part form: $form")
    }

    private const val SEED_FACTOR = 31L
    private const val INDEX_FACTOR = 17L
}

internal object FinalCircumstanceExerciseEvaluator {
    @Suppress("ReturnCount")
    fun evaluate(exercise: GeneratedExercise, answer: Answer): Evaluation {
        val source = exercise.source as? CircumstanceExerciseSource
            ?: return mismatch(answer, "The exercise does not contain a final-circumstance task.")
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
                "Use every supplied constituent to complete the requested statement.",
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
                "Correct. The supplied circumstance is final and the clause core keeps its requested order.",
                exercise,
                answer,
                ids,
                ResponseConstraintStatus.MET,
                MeaningSatisfaction.MET,
            )
        }
        val circumstanceIsFinal = identities.lastOrNull()?.form == "circumstance"
        return result(
            if (circumstanceIsFinal) TargetStatus.MET else TargetStatus.NOT_MET,
            LinguisticStatus.UNKNOWN,
            "REQUESTED_ORDER_CONSTRAINT",
            if (circumstanceIsFinal) {
                "The circumstance is final, but keep the supplied clause core in its requested order."
            } else {
                "Place the supplied circumstance at the requested end position and keep the clause core in order."
            },
            exercise,
            answer,
            ids,
            ResponseConstraintStatus.NOT_MET,
            MeaningSatisfaction.UNKNOWN,
        )
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

    private fun canonicalIdentities(
        exercise: GeneratedExercise,
        source: CircumstanceExerciseSource,
    ): List<TileIdentity> {
        val tileSource = requireNotNull(exercise.inspector.tileSource)
        val tokensById = tileSource.tokens.associateBy(TileTokenDetails::blockId)
        val fingerprinted = tileSource.licensedOrders.single().blockIds.map { blockId ->
            val token = requireNotNull(tokensById[blockId]) { "Licensed circumstance order references unknown block $blockId" }
            TileIdentity(token.lexicalId, token.form)
        }
        val privateCanonical = (source.answerKey as AnswerKey.TileOrder).canonical
        require(privateCanonical == fingerprinted) {
            "Private circumstance answer key does not match the licensed inspector order"
        }
        return fingerprinted
    }
}
