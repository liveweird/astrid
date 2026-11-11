package app.astrid.engine

import java.util.Locale

internal data class ModalRoundDefinition(
    val key: String,
    val focusId: String,
    val title: String,
    val modal: Verb,
    val senseId: String,
    val selectedMeaning: String,
    val sourceUrl: String,
    val frameVariantId: String,
    val interpretationContextVariantId: String,
    val constructionContextVariantId: String,
    val interpretTask: ModalInterpretationTaskSeed,
    val constructionTask: ModalConstructionTaskSeed,
    val workedDanish: String,
    val workedEnglish: String,
    val workedComponentLexicalIds: List<String>,
)

internal data class ModalInterpretationTaskSeed(
    val id: String,
    val subjectId: String,
    val subject: String,
    val complement: String,
    val complementLexicalIds: List<String>,
    val context: String,
)

internal data class ModalConstructionTaskSeed(
    val id: String,
    val subjectId: String,
    val subject: String,
    val subjectReference: String,
    val complement: String,
    val complementLexicalIds: List<String>,
    val intendedMeaning: String,
)

internal data class ModalInterpretationTask(
    val definition: ModalRoundDefinition,
) {
    val id = definition.interpretTask.id
    val targetScopeId = "modal-${definition.key}-meaning"
    val answerExposureKeys = listOf(ModalRoundContent.meaningKey(definition))
    val promptExposureKeys = listOf(
        ModalRoundContent.interpretationContextKey(definition),
        ModalRoundContent.presentKey(definition.modal.lexicalId),
        ModalRoundContent.BARE_INFINITIVE_KEY,
    ) + definition.interpretTask.complementLexicalIds.map(ModalRoundContent::componentKey)
    val lessonExposureKeys = ModalRoundContent.introductionExposureKeys(definition)
    val hintExposureKeys = listOf(ModalRoundContent.interpretationContextKey(definition))
    val feedbackExposureKeys = listOf(ModalRoundContent.meaningKey(definition))
}

internal data class ModalConstructionTask(
    val definition: ModalRoundDefinition,
    override val id: String = definition.constructionTask.id,
    override val formatId: String = "E025",
) : PresentOrderTask {
    override val targetScopeId = ModalRoundContent.CONSTRUCTION_SCOPE_ID
    val parts = ModalRoundContent.parts(definition)
    override val answerExposureKeys = listOf(
        ModalRoundContent.exactOrderKey(id),
        ModalRoundContent.ORDER_PATTERN_KEY,
    )
    override val promptExposureKeys = listOf(
        ModalRoundContent.constructionContextKey(definition),
        ModalRoundContent.presentKey(definition.modal.lexicalId),
        ModalRoundContent.BARE_INFINITIVE_KEY,
    ) + definition.constructionTask.complementLexicalIds.map(ModalRoundContent::componentKey)
    override val lessonExposureKeys = ModalRoundContent.introductionExposureKeys(definition)
    override val hintExposureKeys = listOf(ModalRoundContent.ORDER_PATTERN_KEY)
    override val feedbackExposureKeys = listOf(
        ModalRoundContent.exactOrderKey(id),
        ModalRoundContent.ORDER_PATTERN_KEY,
    )
}

internal data class ModalInterpretationExerciseSource(
    val task: ModalInterpretationTask,
    override val answerKey: AnswerKey,
    override val blockRoles: Map<String, String> = emptyMap(),
) : ExerciseSource

/** Public inspector shape to add to EngineModels.ExerciseInspector during integration. */
data class ModalSourceDetails(
    val taskId: String,
    val modalEntryId: String,
    val modalSenseId: String,
    val expectedInterpretation: String,
    val frameVariantId: String,
    val contextVariantId: String,
)

internal object ModalRoundContent {
    const val INTERPRETATION_FORMAT_ID = "E028"
    const val CONSTRUCTION_SCOPE_ID = "supplied-modal-order"
    const val MEANING_PATTERN_KEY = "module-one:pattern:contextual-modal-meaning"
    const val ORDER_PATTERN_KEY = "module-one:pattern:supplied-modal-order"
    const val BARE_INFINITIVE_KEY = "module-one:bare-infinitive:L011"
    const val READ_INFINITIVE_KEY = "module-one:verb-infinitive:L011"
    const val READ_ENTRY_ID = "L011"
    const val CONSTRUCTION_ID = "C014"

    val meaningChoices = listOf(
        "A personal wish or intention.",
        "An external requirement.",
        "Permission to do it.",
    )

    val definitions = listOf(
        ModalRoundDefinition(
            key = "volition",
            focusId = "module-one-volition",
            title = "Expressing an intention",
            modal = Verb("L034", "ville", "vil", "want or intend to"),
            senseId = "L034-S01",
            selectedMeaning = meaningChoices[0],
            sourceUrl = "https://ordnet.dk/ddo/ordbog/ville",
            frameVariantId = "s11-vil-volition-laese-noun",
            interpretationContextVariantId = "s11-volition-free-time-choice",
            constructionContextVariantId = "s11-volition-intended-newspaper",
            interpretTask = ModalInterpretationTaskSeed(
                "modal-volition-interpret-letter", "L004", "hun", "et brev", listOf("L023", "L018"),
                "Nora chooses her own free-time activity and states what she intends to do.",
            ),
            constructionTask = ModalConstructionTaskSeed(
                "modal-volition-build-newspaper", "L005", "vi", "the speaker and another person",
                "en avis", listOf("L022", "L015"), "We intend to read a newspaper. Use vil.",
            ),
            workedDanish = "Jeg vil læse en bog.",
            workedEnglish = "I intend to read a book.",
            workedComponentLexicalIds = listOf("L001", "L022", "L014"),
        ),
        ModalRoundDefinition(
            key = "obligation",
            focusId = "module-one-obligation",
            title = "Understanding a requirement",
            modal = Verb("L035", "skulle", "skal", "be required to"),
            senseId = "L035-S01",
            selectedMeaning = meaningChoices[1],
            sourceUrl = "https://ordnet.dk/ddo/ordbog/skulle",
            frameVariantId = "s11-skal-obligation-laese-noun",
            interpretationContextVariantId = "s11-obligation-teacher-assignment",
            constructionContextVariantId = "s11-obligation-required-newspaper",
            interpretTask = ModalInterpretationTaskSeed(
                "modal-obligation-interpret-book", "L007", "de", "en bog", listOf("L022", "L014"),
                "A teacher assigns this reading to the learners as required work.",
            ),
            constructionTask = ModalConstructionTaskSeed(
                "modal-obligation-build-newspaper", "L001", "jeg", "the speaker",
                "en avis", listOf("L022", "L015"), "I am required to read a newspaper. Use skal.",
            ),
            workedDanish = "Du skal læse et brev.",
            workedEnglish = "You are required to read a letter.",
            workedComponentLexicalIds = listOf("L002", "L023", "L018"),
        ),
        ModalRoundDefinition(
            key = "permission",
            focusId = "module-one-permission",
            title = "Understanding permission",
            modal = Verb("L036", "måtte", "må", "be permitted to"),
            senseId = "L036-S01",
            selectedMeaning = meaningChoices[2],
            sourceUrl = "https://ordnet.dk/ddo/ordbog/m%C3%A5tte",
            frameVariantId = "s11-maa-permission-laese-noun",
            interpretationContextVariantId = "s11-permission-owner-grant",
            constructionContextVariantId = "s11-permission-permitted-book",
            interpretTask = ModalInterpretationTaskSeed(
                "modal-permission-interpret-letter", "L002", "du", "et brev", listOf("L023", "L018"),
                "The letter's owner explicitly grants the addressee permission to read it.",
            ),
            constructionTask = ModalConstructionTaskSeed(
                "modal-permission-build-book", "L004", "hun", "one stated woman",
                "en bog", listOf("L022", "L014"), "She is permitted to read a book. Use må.",
            ),
            workedDanish = "Han må læse en avis.",
            workedEnglish = "He is permitted to read a newspaper.",
            workedComponentLexicalIds = listOf("L003", "L022", "L015"),
        ),
    )

    fun interpretationTasks() = definitions.map(::ModalInterpretationTask)
    fun constructionTasks() = definitions.map(::ModalConstructionTask)

    fun introduction(definition: ModalRoundDefinition) = listOf(
        IntroductionSection(
            "Dictionary and present forms",
            "at ${definition.modal.lemma} → ${definition.modal.present}",
            "da",
        ),
        IntroductionSection("Selected meaning", definition.selectedMeaning),
        IntroductionSection("Reading form", "at læse → læse efter ${definition.modal.present}", "da"),
        IntroductionSection(
            "Requested pattern",
            "In this context, use the supplied finite modal followed by the bare infinitive læse without at.",
        ),
        IntroductionSection("Worked example", definition.workedDanish, "da"),
        IntroductionSection("Worked meaning", definition.workedEnglish),
        IntroductionSection(
            "Boundaries",
            "This round practises only the stated contextual reading; prediction, hearsay, requests, " +
                "negation, past forms, and ellipsis are excluded.",
        ),
    )

    fun introductionExposureKeys(definition: ModalRoundDefinition) =
        lessonKeys(definition) + definition.workedComponentLexicalIds.map(::componentKey)

    fun lessonKeys(definition: ModalRoundDefinition) = listOf(
        meaningKey(definition),
        infinitiveKey(definition.modal.lexicalId),
        presentKey(definition.modal.lexicalId),
        READ_INFINITIVE_KEY,
        BARE_INFINITIVE_KEY,
        MEANING_PATTERN_KEY,
        ORDER_PATTERN_KEY,
    )

    fun parts(definition: ModalRoundDefinition): List<SuppliedOrderPart> {
        val task = definition.constructionTask
        val complementWords = task.complement.split(' ')
        return listOf(
            SuppliedOrderPart(task.subjectId, task.subject, "subject"),
            SuppliedOrderPart(definition.modal.lexicalId, definition.modal.present, "finite-modal"),
            SuppliedOrderPart(READ_ENTRY_ID, "læse", "bare-infinitive"),
            SuppliedOrderPart(task.complementLexicalIds[0], complementWords[0], "article"),
            SuppliedOrderPart(task.complementLexicalIds[1], complementWords[1], "singular-headword"),
        )
    }

    fun meaningKey(definition: ModalRoundDefinition) =
        "module-one:modal-meaning:${definition.modal.lexicalId}:${definition.senseId}"
    fun interpretationContextKey(definition: ModalRoundDefinition) =
        "module-one:modal-context:${definition.interpretationContextVariantId}"
    fun constructionContextKey(definition: ModalRoundDefinition) =
        "module-one:modal-context:${definition.constructionContextVariantId}"
    fun infinitiveKey(lexicalId: String) = "module-one:verb-infinitive:$lexicalId"
    fun presentKey(lexicalId: String) = "module-one:verb-present:$lexicalId"
    fun componentKey(lexicalId: String) = "module-one:supplied-component:$lexicalId"
    fun exactOrderKey(taskId: String) = "module-one:exact-order:$taskId"
}

internal object ModalRoundExerciseGenerator {
    private const val SEED_MULTIPLIER = 31L
    private const val INDEX_MULTIPLIER = 17L

    fun generateInterpretation(request: GenerationRequest): GeneratedExercise {
        val tasks = ModalRoundContent.interpretationTasks()
        val taskIndex =
            Math.floorMod(
                request.seed.toLong() * SEED_MULTIPLIER + request.index * INDEX_MULTIPLIER,
                tasks.size.toLong(),
            ).toInt()
        val task = tasks[taskIndex]
        val definition = task.definition
        val seed = definition.interpretTask
        val choices = rotatedChoices(request.seed)
        return GeneratedExercise(
            id =
                "E028-${request.contentRevision}-${request.generatorRevision}-${task.id}-" +
                    "${request.seed}-${request.index}",
            formatId = ModalRoundContent.INTERPRETATION_FORMAT_ID,
            targetSkillId = "G030",
            lexicalTarget = null,
            supportSkillIds = listOf("G001", "G002", "G003", "G004", "G005", "G006"),
            presentation = ExercisePresentation(
                instruction = "Choose what the modal expresses in this situation.",
                context = seed.context,
                prompt = ExercisePrompt(
                    before = "",
                    after = "",
                    choices = choices,
                    punctuation = "",
                    cue = sentence(seed.subject, definition.modal.present, seed.complement),
                    cueLanguage = "da",
                    answerLanguage = "en",
                ),
                introduction = ModalRoundContent.introduction(definition),
                hint = "Use the stated situation to choose among the three introduced modal meanings.",
            ),
            contentRevision = request.contentRevision,
            generatorRevision = request.generatorRevision,
            reproduction = Reproduction(request.seed, request.index),
            inspector = ExerciseInspector(
                canonicalSentenceId = task.id,
                constructionId = ModalRoundContent.CONSTRUCTION_ID,
                lexicalIds = listOf(seed.subjectId, definition.modal.lexicalId, ModalRoundContent.READ_ENTRY_ID) +
                    seed.complementLexicalIds,
                clauseRoles = listOf("SUBJECT", "FINITE_VERB", "NONFINITE_VERB", "OBJECT"),
                canonicalSentence = sentence(seed.subject, definition.modal.present, seed.complement),
                modalSource = modalSource(task.id, definition, definition.interpretationContextVariantId),
            ),
            source = ModalInterpretationExerciseSource(
                task,
                AnswerKey.SuppliedChoice(definition.selectedMeaning, choices.toSet()),
            ),
        )
    }

    fun generateConstruction(request: GenerationRequest, task: ModalConstructionTask): GeneratedExercise {
        val definition = task.definition
        val shuffled = shuffled(task.parts, request.seed, request.index)
        val tokens = shuffled.mapIndexed { index, part -> OrderBlock("b$index", part.text) }
        val tokenDetails = shuffled.mapIndexed { index, part -> TileTokenDetails("b$index", part.lexicalId, part.form) }
        val canonical = task.parts.map { TileIdentity(it.lexicalId, it.form) }
        val canonicalIds = canonical.map { identity ->
            tokenDetails.single { it.lexicalId == identity.lexicalId && it.form == identity.form }.blockId
        }
        val taskSeed = definition.constructionTask
        return GeneratedExercise(
            id =
                "E025-${request.contentRevision}-${request.generatorRevision}-${task.id}-" +
                    "${request.seed}-${request.index}",
            formatId = "E025",
            targetSkillId = "G005",
            lexicalTarget = null,
            supportSkillIds = listOf("G001", "G002", "G003", "G004", "G006"),
            presentation = ExercisePresentation(
                instruction = "Arrange all five supplied words to express the requested statement.",
                context =
                    "Intended meaning: ${taskSeed.intendedMeaning} " +
                        "The subject refers to ${taskSeed.subjectReference}.",
                prompt = ExercisePrompt(
                    before = "",
                    after = "",
                    blocks = tokens,
                    punctuation = ".",
                    cue = taskSeed.intendedMeaning,
                    cueLanguage = "en",
                    answerLanguage = "da",
                    tileMode = "use-all",
                    requiredTileCount = 5,
                ),
                introduction = ModalRoundContent.introduction(definition),
                hint = "Put the subject first, the supplied finite modal second, and læse third.",
            ),
            contentRevision = request.contentRevision,
            generatorRevision = request.generatorRevision,
            reproduction = Reproduction(request.seed, request.index),
            inspector = ExerciseInspector(
                canonicalSentenceId = task.id,
                constructionId = ModalRoundContent.CONSTRUCTION_ID,
                lexicalIds = task.parts.flatMap(SuppliedOrderPart::lexicalIds),
                clauseRoles = listOf("SUBJECT", "FINITE_VERB", "NONFINITE_VERB", "OBJECT"),
                canonicalSentence = sentence(taskSeed.subject, definition.modal.present, taskSeed.complement),
                tileSource = TileSourceDetails(
                    tokenDetails,
                    listOf(TileLicensedOrderDetails("subject-modal-infinitive-object", canonicalIds)),
                    emptyList(),
                ),
                modalSource = modalSource(task.id, definition, definition.constructionContextVariantId),
            ),
            source = PresentOrderExerciseSource(task, AnswerKey.TileOrder(canonical, null, null)),
        )
    }

    private fun rotatedChoices(seed: Int): List<String> {
        val choiceCount = ModalRoundContent.meaningChoices.size
        val offset = Math.floorMod(Math.floorDiv(seed, choiceCount), choiceCount)
        return ModalRoundContent.meaningChoices.drop(offset) + ModalRoundContent.meaningChoices.take(offset)
    }

    private fun <T> shuffled(values: List<T>, seed: Int, index: Int): List<T> {
        val offset =
            Math.floorMod(
                seed.toLong() * SEED_MULTIPLIER + index * INDEX_MULTIPLIER,
                values.size.toLong(),
            ).toInt()
        return values.drop(offset) + values.take(offset)
    }

    private fun modalSource(
        taskId: String,
        definition: ModalRoundDefinition,
        contextVariantId: String,
    ) = ModalSourceDetails(
        taskId,
        definition.modal.lexicalId,
        definition.senseId,
        definition.selectedMeaning,
        definition.frameVariantId,
        contextVariantId,
    )

    private fun sentence(subject: String, modal: String, complement: String) =
        "$subject $modal læse $complement.".replaceFirstChar(Char::titlecase)
}

internal object ModalRoundExerciseEvaluator {
    private const val REQUIRED_VERB_PREFIX_SIZE = 3

    fun evaluateInterpretation(exercise: GeneratedExercise, answer: Answer): Evaluation {
        val source = exercise.source as ModalInterpretationExerciseSource
        val task = source.task
        val raw = answer as? Answer.Text ?: return mismatch(answer, LinguisticScope.ENGLISH_CHOICE)
        val normalized = normalizeText(raw.text)
        return when {
            normalized == task.definition.selectedMeaning -> meaningResult(
                answer, normalized, TargetStatus.MET, MeaningSatisfaction.MET,
                emptyList(), "Correct. The context gives ${task.definition.selectedMeaning.lowercase(Locale.ROOT)}",
                ResponseConstraintStatus.MET,
            )
            normalized in ModalRoundContent.meaningChoices -> meaningResult(
                answer, normalized, TargetStatus.NOT_MET, MeaningSatisfaction.NOT_MET,
                listOf(
                    EvaluationIssue(
                        "CONTEXTUAL_MODAL_MEANING_REQUIRED",
                        "In this situation, the modal expresses " +
                            task.definition.selectedMeaning.lowercase(Locale.ROOT),
                    ),
                ),
                "The supplied interpretation does not match this context. " +
                    "Here it expresses ${task.definition.selectedMeaning.lowercase(Locale.ROOT)}",
                ResponseConstraintStatus.MET,
            )
            else -> meaningResult(
                answer, normalized, TargetStatus.UNEVALUABLE, MeaningSatisfaction.UNKNOWN,
                listOf(EvaluationIssue("UNSUPPLIED_MEANING_CHOICE", "Choose one of the supplied interpretations.")),
                "The response is outside the supplied interpretations.",
                ResponseConstraintStatus.NOT_MET,
            )
        }
    }

    // Each return represents malformed, complete, or missing supplied-word input.
    @Suppress("ReturnCount")
    fun evaluateConstruction(exercise: GeneratedExercise, answer: Answer): Evaluation {
        val source = exercise.source as PresentOrderExerciseSource
        val task = source.task as ModalConstructionTask
        val ids = (answer as? Answer.Order)?.blockIds ?: return mismatch(answer, LinguisticScope.DANISH_SENTENCE)
        val byId = requireNotNull(exercise.inspector.tileSource).tokens.associateBy(TileTokenDetails::blockId)
        if (ids.isEmpty() || ids.distinct().size != ids.size || !ids.all(byId::containsKey)) {
            return mismatch(answer, LinguisticScope.DANISH_SENTENCE)
        }
        val identities = ids.map { id -> byId.getValue(id).let { TileIdentity(it.lexicalId, it.form) } }
        val canonical = source.answerKey as AnswerKey.TileOrder
        val completed = exercise.inspector.canonicalSentence
        if (identities == canonical.canonical) {
            return Evaluation(
                TargetStatus.MET,
                LinguisticStatus.ACCEPTABLE,
                emptyList(),
                "Correct. The words follow the requested subject–modal–infinitive order.",
                completed,
                answer,
                NormalizedAnswer.Order(ids),
                LinguisticScope.DANISH_SENTENCE,
                ResponseConstraintStatus.MET,
                MeaningSatisfaction.MET,
            )
        }
        val missing = ids.size < canonical.canonical.size
        val target = if (missing) TargetStatus.UNEVALUABLE else modalPlacementStatus(identities)
        val code = if (missing) "REQUIRED_WORDS_MISSING" else "REQUESTED_ORDER_CONSTRAINT"
        val message = if (missing) {
            "Use every supplied word to complete the requested statement."
        } else {
            "Use the supplied words in the requested subject–modal–infinitive order."
        }
        return Evaluation(
            target,
            LinguisticStatus.UNKNOWN,
            listOf(EvaluationIssue(code, message)),
            message,
            completed,
            answer,
            NormalizedAnswer.Order(ids),
            LinguisticScope.DANISH_SENTENCE,
            ResponseConstraintStatus.NOT_MET,
            MeaningSatisfaction.UNKNOWN,
        )
    }

    private fun modalPlacementStatus(identities: List<TileIdentity>): TargetStatus =
        if (
            identities.take(REQUIRED_VERB_PREFIX_SIZE).map(TileIdentity::form) ==
            listOf("subject", "finite-modal", "bare-infinitive")
        ) {
            TargetStatus.MET
        } else {
            TargetStatus.NOT_MET
        }

    private fun meaningResult(
        raw: Answer,
        normalized: String,
        target: TargetStatus,
        meaning: MeaningSatisfaction,
        issues: List<EvaluationIssue>,
        feedback: String,
        constraint: ResponseConstraintStatus,
    ) = Evaluation(
        target,
        if (target == TargetStatus.UNEVALUABLE) LinguisticStatus.UNKNOWN else LinguisticStatus.ACCEPTABLE,
        issues,
        feedback,
        null,
        raw,
        NormalizedAnswer.Text(normalized),
        LinguisticScope.ENGLISH_CHOICE,
        constraint,
        meaning,
    )

    private fun mismatch(answer: Answer, scope: LinguisticScope) = Evaluation(
        TargetStatus.UNEVALUABLE,
        LinguisticStatus.UNKNOWN,
        listOf(EvaluationIssue("ANSWER_TYPE_MISMATCH", "The answer type does not match the exercise format.")),
        "The answer could not be evaluated for this exercise.",
        null,
        answer,
        normalized(answer),
        scope,
        ResponseConstraintStatus.NOT_MET,
        MeaningSatisfaction.UNKNOWN,
    )

    private fun normalized(answer: Answer): NormalizedAnswer = when (answer) {
        is Answer.Text -> NormalizedAnswer.Text(normalizeText(answer.text))
        is Answer.Order -> NormalizedAnswer.Order(answer.blockIds)
        is Answer.Matching -> NormalizedAnswer.Matching(answer.pairs)
        is Answer.Gaps -> NormalizedAnswer.Gaps(answer.selections)
    }
}
