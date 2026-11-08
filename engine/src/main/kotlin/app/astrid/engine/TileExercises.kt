package app.astrid.engine

internal object TileExerciseGenerator {
    fun createWordChoiceExercise(request: GenerationRequest): GeneratedExercise {
        val senseIndex = permutedIndex(request.seed, request.index, PilotContent.wordChoiceSenses.size)
        val sense = PilotContent.wordChoiceSenses[senseIndex]
        val choices = shuffled(
            PilotContent.wordChoiceSenses.map { "${it.article} ${it.headword}" },
            mixed(request.seed xor CHOICE_SHUFFLE_XOR, request.index),
        )
        val expected = "${sense.article} ${sense.headword}"
        return GeneratedExercise(
            id = "E011-${request.contentRevision}-${request.generatorRevision}-${sense.senseId}-${request.seed}-${request.index}",
            formatId = "E011",
            targetSkillId = null,
            lexicalTarget = LexicalTarget(sense.entryId, sense.senseId, "meaning", "en-da"),
            supportSkillIds = emptyList(),
            presentation = ExercisePresentation(
                instruction = "Choose the introduced Danish word for the English meaning.",
                context = "The choices show each introduced singular noun together with its article.",
                prompt = ExercisePrompt(
                    before = "",
                    after = "",
                    choices = choices,
                    punctuation = "",
                    cue = sense.gloss,
                    cueLanguage = "en",
                    answerLanguage = "da",
                ),
                introduction = PilotContent.wordChoiceSenses.map { choice ->
                    IntroductionSection(
                        title = choice.gloss.replaceFirstChar { it.titlecase() },
                        body = "${choice.article} ${choice.headword}",
                        bodyLanguage = "da",
                    )
                },
                hint = "Choose one of the four supplied Danish noun phrases.",
            ),
            contentRevision = request.contentRevision,
            generatorRevision = request.generatorRevision,
            reproduction = Reproduction(request.seed, request.index),
            inspector = ExerciseInspector(
                lexicalIds = listOf(sense.entryId),
                clauseRoles = emptyList(),
                lexicalSource = LexicalSourceDetails(
                    sense.entryId,
                    sense.senseId,
                    sense.headword,
                    sense.gloss,
                    sense.article,
                ),
            ),
            source = LexicalSource(sense, AnswerKey.SuppliedChoice(expected, choices.toSet())),
        )
    }

    fun createTileExercise(request: GenerationRequest): GeneratedExercise {
        val sentenceIndex = permutedIndex(request.seed, request.index, PilotContent.sentences.size)
        val sentence = PilotContent.sentences[sentenceIndex]
        val withSpare = request.formatId == "E013"
        val canonical = sentenceTokens(sentence)
        val drafts = canonical + if (withSpare) {
            listOf(DraftToken(sentence.verb.lexicalId, sentence.verb.lemma, "infinitive"))
        } else {
            emptyList()
        }
        val tokens = shuffled(drafts, mixed(request.seed xor TILE_SHUFFLE_XOR, request.index)).mapIndexed { index, token ->
            GeneratedToken("b$index", token)
        }
        val canonicalIdentities = canonical.map(DraftToken::identity)
        val frontedIdentities = frontedTokens(sentence)?.map(DraftToken::identity)
        val infinitiveIdentities = if (withSpare) {
            canonical.map { token ->
                if (token.form == "finite-present") DraftToken(token.lexicalId, sentence.verb.lemma, "infinitive").identity
                else token.identity
            }
        } else {
            null
        }
        val licensedOrders = buildList {
            add(TileLicensedOrderDetails("subject-first", blockIdsFor(canonicalIdentities, tokens)))
            frontedIdentities?.let { add(TileLicensedOrderDetails("fronted", blockIdsFor(it, tokens))) }
        }
        val distractors = if (withSpare) {
            listOf(
                TileDistractorDetails(
                    tokens.single { it.draft.form == "infinitive" }.blockId,
                    "contrasts the infinitive with the required finite present form",
                ),
            )
        } else {
            emptyList()
        }
        val target = requireNotNull(request.targetSkillId)
        return GeneratedExercise(
            id = "${request.formatId}-${request.contentRevision}-${request.generatorRevision}-${sentence.id}-" +
                "${request.seed}-${request.index}",
            formatId = request.formatId,
            targetSkillId = target,
            lexicalTarget = null,
            supportSkillIds = tileSupportSkills(sentence, target),
            presentation = tilePresentation(sentence, tokens, withSpare, canonical.size),
            contentRevision = request.contentRevision,
            generatorRevision = request.generatorRevision,
            reproduction = Reproduction(request.seed, request.index),
            inspector = ExerciseInspector(
                canonicalSentenceId = sentence.id,
                constructionId = sentence.constructionId,
                lexicalIds = canonical.map(DraftToken::lexicalId),
                clauseRoles = canonical.map(DraftToken::form),
                canonicalSentence = realizeTexts(canonical.map(DraftToken::text), "."),
                tileSource = TileSourceDetails(
                    tokens.map { TileTokenDetails(it.blockId, it.draft.lexicalId, it.draft.form) },
                    licensedOrders,
                    distractors,
                ),
            ),
            source = TileExerciseSource(
                sentence,
                AnswerKey.TileOrder(canonicalIdentities, frontedIdentities, infinitiveIdentities),
            ),
        )
    }

    private fun tilePresentation(
        sentence: Sentence,
        tokens: List<GeneratedToken>,
        withSpare: Boolean,
        requiredTileCount: Int,
    ) = ExercisePresentation(
        instruction = if (withSpare) {
            "Arrange a subject-first Danish statement. Choose the finite present form and leave the extra word unused."
        } else {
            "Arrange all supplied Danish words to express the English meaning. Begin with the subject."
        },
        context = if (withSpare) {
            "Use exactly $requiredTileCount tiles; one supplied verb form is extra."
        } else {
            "Use every supplied tile exactly once."
        },
        prompt = ExercisePrompt(
            before = "",
            after = "",
            blocks = tokens.map { OrderBlock(it.blockId, it.draft.text) },
            punctuation = ".",
            cue = englishMeaning(sentence),
            cueLanguage = "en",
            answerLanguage = "da",
            tileMode = if (withSpare) "with-spares" else "use-all",
            requiredTileCount = requiredTileCount,
        ),
        introduction = tileIntroduction(sentence, withSpare),
        hint = if (withSpare) {
            "Begin with the subject and select the finite present verb rather than its infinitive."
        } else {
            "Begin with the subject, then the finite present verb, and keep an article before its noun."
        },
    )

    private fun tileIntroduction(sentence: Sentence, withSpare: Boolean): List<IntroductionSection> {
        val subjectId = sentence.subject.lexicalIds.single()
        val pronoun = (PilotContent.humanPronouns + PilotContent.identifyingPronoun).single { it.id == subjectId }
        return buildList {
            sentence.complement?.let { complement ->
                val nounId = complement.lexicalIds.last()
                val noun = PilotContent.nouns.single { it.id == nounId }
                add(
                    IntroductionSection(
                        "Article and noun: ${noun.gloss}",
                        complement.text.replace(" ", " · "),
                        "da",
                    ),
                )
            }
            add(
                IntroductionSection(
                    if (withSpare) "Verb forms: ${sentence.verb.gloss}" else "Finite present verb: ${sentence.verb.gloss}",
                    if (withSpare) "${sentence.verb.lemma} · ${sentence.verb.present}" else sentence.verb.present,
                    "da",
                ),
            )
            add(IntroductionSection("Pronoun: ${pronoun.gloss}", pronoun.text, "da"))
        }
    }

    private fun tileSupportSkills(sentence: Sentence, target: String): List<String> =
        (listOf("G001", "G002", "G003", "G004", "G005") +
            if (sentence.complement != null) listOf("G006") else emptyList()) - target

    private fun sentenceTokens(sentence: Sentence): List<DraftToken> = buildList {
        add(DraftToken(sentence.subject.lexicalIds.single(), sentence.subject.text, "subject"))
        add(DraftToken(sentence.verb.lexicalId, sentence.verb.present, "finite-present"))
        sentence.complement?.let { complement ->
            val words = complement.text.split(' ')
            add(DraftToken(complement.lexicalIds[0], words[0], "article"))
            add(DraftToken(complement.lexicalIds[1], words[1], "singular-headword"))
        }
    }

    private fun frontedTokens(sentence: Sentence): List<DraftToken>? {
        if (sentence.constructionId != "C002") return null
        val canonical = sentenceTokens(sentence)
        return canonical.drop(2) + canonical[1] + canonical[0]
    }

    private fun blockIdsFor(identities: List<TileIdentity>, tokens: List<GeneratedToken>): List<String> {
        val remaining = tokens.toMutableList()
        return identities.map { identity ->
            val index = remaining.indexOfFirst { it.draft.identity == identity }
            check(index >= 0)
            remaining.removeAt(index).blockId
        }
    }

    private fun englishMeaning(sentence: Sentence): String {
        val subjectId = sentence.subject.lexicalIds.single()
        val subject = ENGLISH_SUBJECTS.getValue(subjectId)
        val verbForms = ENGLISH_VERBS.getValue(sentence.verb.lexicalId)
        val verb = verbForms[if (subjectId in THIRD_PERSON_SUBJECT_IDS) 1 else 0]
        val nounId = sentence.complement?.lexicalIds?.lastOrNull()
        return "$subject $verb${nounId?.let { " ${ENGLISH_NOUNS.getValue(it)}" }.orEmpty()}."
    }

    private data class DraftToken(val lexicalId: String, val text: String, val form: String) {
        val identity: TileIdentity get() = TileIdentity(lexicalId, form)
    }

    private data class GeneratedToken(val blockId: String, val draft: DraftToken)

    private fun permutedIndex(seed: Int, index: Int, size: Int): Int {
        val start = Math.floorMod(mixed(seed, 0), size.toLong()).toInt()
        var step = Math.floorMod(mixed(seed, 1), (size - 1).toLong()).toInt() + 1
        while (greatestCommonDivisor(step, size) != 1) step = (step % (size - 1)) + 1
        return (start + (index % size).toLong() * step).mod(size.toLong()).toInt()
    }

    private fun greatestCommonDivisor(first: Int, second: Int): Int {
        var a = first
        var b = second
        while (b != 0) {
            val remainder = a % b
            a = b
            b = remainder
        }
        return a
    }

    private fun mixed(seed: Int, salt: Int): Long {
        var value = seed.toLong() xor (salt.toLong() * GOLDEN_GAMMA)
        value = (value xor (value ushr MIX_SHIFT_ONE)) * MIX_MULTIPLIER_ONE
        value = (value xor (value ushr MIX_SHIFT_TWO)) * MIX_MULTIPLIER_TWO
        return value xor (value ushr MIX_SHIFT_THREE)
    }

    private fun <T> shuffled(input: List<T>, entropy: Long): List<T> {
        val result = input.toMutableList()
        var state = entropy
        for (index in result.lastIndex downTo 1) {
            state = mixed((state xor (state ushr LONG_HALF_BITS)).toInt() and Int.MAX_VALUE, index)
            val swapIndex = Math.floorMod(state, (index + 1).toLong()).toInt()
            val temporary = result[index]
            result[index] = result[swapIndex]
            result[swapIndex] = temporary
        }
        return result
    }

    private const val CHOICE_SHUFFLE_XOR = 0x31415926
    private const val TILE_SHUFFLE_XOR = 0x27182818
    private const val GOLDEN_GAMMA = -7046029254386353131L
    private const val MIX_MULTIPLIER_ONE = -4658895280553007687L
    private const val MIX_MULTIPLIER_TWO = -7723592293110705685L
    private const val MIX_SHIFT_ONE = 30
    private const val MIX_SHIFT_TWO = 27
    private const val MIX_SHIFT_THREE = 31
    private const val LONG_HALF_BITS = 32
    private val THIRD_PERSON_SUBJECT_IDS = setOf("L003", "L004", "L008")
    private val ENGLISH_SUBJECTS = mapOf(
        "L001" to "I",
        "L002" to "You (one person)",
        "L003" to "He",
        "L004" to "She",
        "L005" to "We",
        "L006" to "You (more than one person)",
        "L007" to "They",
        "L008" to "It or that",
    )
    private val ENGLISH_VERBS = mapOf(
        "L009" to listOf("have", "has"),
        "L010" to listOf("is", "is"),
        "L011" to listOf("read", "reads"),
        "L012" to listOf("eat", "eats"),
        "L013" to listOf("sleep", "sleeps"),
    )
    private val ENGLISH_NOUNS = mapOf(
        "L014" to "a book",
        "L015" to "a newspaper",
        "L016" to "a car",
        "L017" to "a dog",
        "L018" to "a letter (written message)",
        "L019" to "a house",
        "L020" to "a table",
        "L021" to "an apple",
    )
}

internal object TileExerciseEvaluator {
    fun evaluateChoice(answer: Answer, key: AnswerKey.SuppliedChoice): Evaluation {
        val text = (answer as? Answer.Text)?.text?.let(::normalizeText)
            ?: return mismatch(answer, LinguisticScope.DANISH_CHOICE)
        return when (text) {
            key.expected -> choiceResult(
                TargetStatus.MET,
                MeaningSatisfaction.MET,
                LinguisticStatus.ACCEPTABLE,
                emptyList(),
                "Correct. You chose the introduced Danish word for this meaning.",
                answer,
                text,
                ResponseConstraintStatus.MET,
            )
            in key.suppliedChoices -> choiceResult(
                TargetStatus.NOT_MET,
                MeaningSatisfaction.NOT_MET,
                LinguisticStatus.ACCEPTABLE,
                listOf(EvaluationIssue("INTRODUCED_MEANING_REQUIRED", "The supplied choice expresses a different meaning.")),
                "That is an introduced Danish choice, but it expresses a different meaning.",
                answer,
                text,
                ResponseConstraintStatus.MET,
            )
            else -> choiceResult(
                TargetStatus.UNEVALUABLE,
                MeaningSatisfaction.UNKNOWN,
                LinguisticStatus.UNKNOWN,
                listOf(EvaluationIssue("SUPPLIED_CHOICE_REQUIRED", "The response must be one of the supplied Danish choices.")),
                "Choose one of the supplied Danish noun phrases.",
                answer,
                text,
                ResponseConstraintStatus.NOT_MET,
            )
        }
    }

    fun evaluateTiles(exercise: GeneratedExercise, answer: Answer, key: AnswerKey.TileOrder): Evaluation {
        val order = (answer as? Answer.Order)?.blockIds ?: return mismatch(answer, LinguisticScope.DANISH_SENTENCE)
        return evaluateTileOrder(exercise, answer, order, key)
    }

    private fun evaluateTileOrder(
        exercise: GeneratedExercise,
        answer: Answer,
        order: List<String>,
        key: AnswerKey.TileOrder,
    ): Evaluation {
        val source = requireNotNull(exercise.inspector.tileSource)
        val byId = source.tokens.associateBy(TileTokenDetails::blockId)
        val validIds = order.all(byId::containsKey) && order.toSet().size == order.size
        return if (!validIds || order.size != requireNotNull(exercise.prompt.requiredTileCount)) {
            tileResult(
                TargetStatus.UNEVALUABLE,
                MeaningSatisfaction.UNKNOWN,
                LinguisticStatus.UNKNOWN,
                listOf(EvaluationIssue("TILE_SELECTION_CONSTRAINT", "Select the required number of supplied tiles without repeats.")),
                "Use the required number of supplied tiles without repeating an occurrence.",
                answer,
                order,
                completedSentence(exercise, order, byId),
                ResponseConstraintStatus.NOT_MET,
            )
        } else {
            val identities = order.map { blockId -> byId.getValue(blockId).let { TileIdentity(it.lexicalId, it.form) } }
            if (exercise.formatId == "E012") {
                evaluateUseAll(exercise, answer, order, identities, key, byId)
            } else {
                evaluateWithSpares(exercise, answer, order, identities, key, byId)
            }
        }
    }

    private fun evaluateUseAll(
        exercise: GeneratedExercise,
        answer: Answer,
        order: List<String>,
        identities: List<TileIdentity>,
        key: AnswerKey.TileOrder,
        byId: Map<String, TileTokenDetails>,
    ): Evaluation = when (identities) {
        key.canonical -> tileResult(
            TargetStatus.MET, MeaningSatisfaction.MET, LinguisticStatus.ACCEPTABLE, emptyList(),
            "Correct. The sentence begins with the subject and expresses the requested meaning.", answer, order,
            completedSentence(exercise, order, byId), ResponseConstraintStatus.MET,
        )
        key.fronted -> tileResult(
            TargetStatus.NOT_MET, MeaningSatisfaction.MET, LinguisticStatus.ACCEPTABLE,
            listOf(EvaluationIssue("SUBJECT_FIRST_STATEMENT_REQUIRED", "The response must begin with the subject.")),
            "This is a licensed fronted statement, but the task asks you to begin with the subject.", answer, order,
            completedSentence(exercise, order, byId), ResponseConstraintStatus.MET,
        )
        else -> unknownTileOrder(exercise, answer, order, byId, ResponseConstraintStatus.MET)
    }

    private fun evaluateWithSpares(
        exercise: GeneratedExercise,
        answer: Answer,
        order: List<String>,
        identities: List<TileIdentity>,
        key: AnswerKey.TileOrder,
        byId: Map<String, TileTokenDetails>,
    ): Evaluation = when (identities) {
        key.canonical -> tileResult(
            TargetStatus.MET, MeaningSatisfaction.MET, LinguisticStatus.ACCEPTABLE, emptyList(),
            "Correct. The subject-first sentence uses the finite present form.", answer, order,
            completedSentence(exercise, order, byId), ResponseConstraintStatus.MET,
        )
        key.infinitive -> tileResult(
            TargetStatus.NOT_MET, MeaningSatisfaction.MET, LinguisticStatus.KNOWN_ISSUE,
            listOf(EvaluationIssue("INFINITIVE_USED", "The infinitive was used where the sentence needs a finite present verb.")),
            "Use the finite present form after the subject, not the infinitive.", answer, order,
            completedSentence(exercise, order, byId), ResponseConstraintStatus.MET,
        )
        key.fronted -> tileResult(
            TargetStatus.MET, MeaningSatisfaction.MET, LinguisticStatus.ACCEPTABLE,
            listOf(EvaluationIssue("SUBJECT_FIRST_RESPONSE_REQUIRED", "The response instruction requires subject-first order.")),
            "The finite form is correct and the fronted sentence is licensed, but this response must begin with the subject.",
            answer, order, completedSentence(exercise, order, byId), ResponseConstraintStatus.NOT_MET,
        )
        else -> unknownTileOrder(exercise, answer, order, byId, ResponseConstraintStatus.NOT_MET)
    }

    private fun unknownTileOrder(
        exercise: GeneratedExercise,
        answer: Answer,
        order: List<String>,
        byId: Map<String, TileTokenDetails>,
        constraint: ResponseConstraintStatus,
    ) = tileResult(
        TargetStatus.UNEVALUABLE,
        MeaningSatisfaction.UNKNOWN,
        LinguisticStatus.UNKNOWN,
        listOf(EvaluationIssue("TILE_ORDER_OUTSIDE_LICENSED_PATTERN", "This complete order is outside the licensed patterns.")),
        "Astrid cannot judge this tile order beyond the licensed patterns.",
        answer,
        order,
        completedSentence(exercise, order, byId),
        constraint,
    )

    private fun mismatch(answer: Answer, scope: LinguisticScope): Evaluation = Evaluation(
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

    private fun choiceResult(
        target: TargetStatus,
        meaning: MeaningSatisfaction,
        linguistic: LinguisticStatus,
        issues: List<EvaluationIssue>,
        feedback: String,
        raw: Answer,
        normalizedText: String,
        constraint: ResponseConstraintStatus,
    ) = Evaluation(
        target, linguistic, issues, feedback, null, raw, NormalizedAnswer.Text(normalizedText),
        LinguisticScope.DANISH_CHOICE, constraint, meaning,
    )

    private fun tileResult(
        target: TargetStatus,
        meaning: MeaningSatisfaction,
        linguistic: LinguisticStatus,
        issues: List<EvaluationIssue>,
        feedback: String,
        raw: Answer,
        order: List<String>,
        completed: String?,
        constraint: ResponseConstraintStatus,
    ) = Evaluation(
        target, linguistic, issues, feedback, completed, raw, NormalizedAnswer.Order(order),
        LinguisticScope.DANISH_SENTENCE, constraint, meaning,
    )

    private fun normalized(answer: Answer): NormalizedAnswer = when (answer) {
        is Answer.Text -> NormalizedAnswer.Text(normalizeText(answer.text))
        is Answer.Order -> NormalizedAnswer.Order(answer.blockIds)
        is Answer.Matching -> NormalizedAnswer.Matching(answer.pairs)
    }

    private fun completedSentence(
        exercise: GeneratedExercise,
        order: List<String>,
        byId: Map<String, TileTokenDetails>,
    ): String? {
        if (!order.all(byId::containsKey)) return null
        val textById = exercise.prompt.blocks.associate { it.id to it.text }
        return realizeTexts(order.map(textById::getValue), exercise.prompt.punctuation)
    }
}

private fun realizeTexts(texts: List<String>, punctuation: String): String =
    texts.mapIndexed { index, text ->
        when {
            text == "I" -> text
            index == 0 -> text.replaceFirstChar { it.titlecase() }
            else -> text.replaceFirstChar { it.lowercase() }
        }
    }.joinToString(" ") + punctuation
