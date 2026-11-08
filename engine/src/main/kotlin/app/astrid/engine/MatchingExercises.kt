package app.astrid.engine

internal object MatchingExerciseGenerator {
    fun create(request: GenerationRequest): GeneratedExercise {
        val senses = PilotContent.wordChoiceSenses
        val leftSenses = shuffled(senses, mixed(request.seed xor LEFT_SHUFFLE_XOR, request.index))
        val rightSenses = shuffled(senses, mixed(request.seed xor RIGHT_SHUFFLE_XOR, request.index))
        val left = leftSenses.mapIndexed { index, sense -> sense to MatchingItem("l$index", sense.gloss) }
        val right = rightSenses.mapIndexed { index, sense -> sense to MatchingItem("r$index", "${sense.article} ${sense.headword}") }
        val rightIdBySense = right.associate { (sense, item) -> sense.senseId to item.id }
        val sourcePairs = left.map { (sense, item) ->
            MatchingPairSourceDetails(item.id, rightIdBySense.getValue(sense.senseId), sense.toSourceDetails())
        }
        return GeneratedExercise(
            id = "E014-${request.contentRevision}-${request.generatorRevision}-${request.seed}-${request.index}",
            formatId = "E014",
            targetSkillId = null,
            lexicalTarget = null,
            supportSkillIds = emptyList(),
            presentation = ExercisePresentation(
                instruction = "Match each English meaning to its introduced Danish noun phrase.",
                context = "Choose one item from each column to build up to four pairs.",
                prompt = ExercisePrompt(
                    before = "",
                    after = "",
                    punctuation = "",
                    cueLanguage = "en",
                    answerLanguage = "da",
                    matchingColumns = MatchingColumns(left.map { it.second }, right.map { it.second }),
                ),
                introduction = senses.map { sense ->
                    IntroductionSection(sense.gloss, "${sense.article} ${sense.headword}", "da")
                },
                hint = "Match every English meaning with the Danish noun phrase introduced for it.",
            ),
            contentRevision = request.contentRevision,
            generatorRevision = request.generatorRevision,
            reproduction = Reproduction(request.seed, request.index),
            inspector = ExerciseInspector(
                lexicalIds = senses.map { it.entryId },
                clauseRoles = emptyList(),
                matchingSource = MatchingSourceDetails(sourcePairs),
            ),
            source = MatchingExerciseSource(AnswerKey.Matching(sourcePairs.associate { it.leftId to it.rightId })),
            lexicalTargets = senses.map { it.toTarget() },
        )
    }

    private fun LexicalSense.toTarget() = LexicalTarget(entryId, senseId, "meaning", "en-da")

    private fun LexicalSense.toSourceDetails() = LexicalSourceDetails(entryId, senseId, headword, gloss, article)

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

    private const val LEFT_SHUFFLE_XOR = 0x10203040
    private const val RIGHT_SHUFFLE_XOR = 0x50607080
    private const val GOLDEN_GAMMA = -7046029254386353131L
    private const val MIX_MULTIPLIER_ONE = -4658895280553007687L
    private const val MIX_MULTIPLIER_TWO = -7723592293110705685L
    private const val MIX_SHIFT_ONE = 30
    private const val MIX_SHIFT_TWO = 27
    private const val MIX_SHIFT_THREE = 31
    private const val LONG_HALF_BITS = 32
}

internal object MatchingExerciseEvaluator {
    fun evaluate(exercise: GeneratedExercise, answer: Answer, key: AnswerKey.Matching): Evaluation {
        val supplied = (answer as? Answer.Matching)?.pairs
        if (supplied == null || !isValid(exercise, supplied)) return invalid(exercise, answer, supplied.orEmpty())

        val results = pairResults(exercise, supplied, key)
        val anyWrong = results.any { it.targetStatus == TargetStatus.NOT_MET }
        val complete = supplied.size == MATCH_COUNT
        val target = when {
            anyWrong -> TargetStatus.NOT_MET
            complete -> TargetStatus.MET
            else -> TargetStatus.UNEVALUABLE
        }
        val meaning = when {
            anyWrong -> MeaningSatisfaction.NOT_MET
            complete -> MeaningSatisfaction.MET
            else -> MeaningSatisfaction.UNKNOWN
        }
        val language = if (supplied.isEmpty()) LinguisticStatus.UNKNOWN else LinguisticStatus.ACCEPTABLE
        return Evaluation(
            targetStatus = target,
            linguisticStatus = language,
            issues = emptyList(),
            feedback = boardFeedback(complete, anyWrong),
            completedSentence = "",
            rawAnswer = answer,
            normalizedAnswer = NormalizedAnswer.Matching(supplied),
            linguisticScope = LinguisticScope.DANISH_MATCHING,
            responseConstraintStatus = if (complete) ResponseConstraintStatus.MET else ResponseConstraintStatus.NOT_MET,
            meaningSatisfaction = meaning,
            matchingResults = results,
        )
    }

    private fun boardFeedback(complete: Boolean, anyWrong: Boolean): String = when {
        complete && !anyWrong -> "All four English meanings match their introduced Danish noun phrases."
        anyWrong -> "Review the pair feedback and try the supplied Danish noun phrases again."
        else -> "Match the remaining English meanings to check the whole board."
    }

    private fun pairResults(
        exercise: GeneratedExercise,
        pairs: List<MatchingPair>,
        key: AnswerKey.Matching,
    ): List<MatchingPairResult> {
        val selectedByLeft = pairs.associate { it.leftId to it.rightId }
        val source = requireNotNull(exercise.inspector.matchingSource)
        val targetByLeft = requireNotNull(exercise.lexicalTargets).associateBy { it.senseId }
        return source.pairs.map { expected ->
            val selected = selectedByLeft[expected.leftId]
            val target = targetByLeft.getValue(expected.lexicalSource.senseId)
            when {
                selected == null -> MatchingPairResult(
                    expected.leftId, null, target, TargetStatus.UNEVALUABLE, MeaningSatisfaction.UNKNOWN,
                    ResponseConstraintStatus.NOT_MET, LinguisticStatus.UNKNOWN,
                    "Choose a supplied Danish noun phrase for this meaning.",
                )
                selected == key.expectedByLeftId.getValue(expected.leftId) -> MatchingPairResult(
                    expected.leftId, selected, target, TargetStatus.MET, MeaningSatisfaction.MET,
                    ResponseConstraintStatus.MET, LinguisticStatus.ACCEPTABLE,
                    "Correct. This supplied Danish noun phrase matches the English meaning.",
                )
                else -> MatchingPairResult(
                    expected.leftId, selected, target, TargetStatus.NOT_MET, MeaningSatisfaction.NOT_MET,
                    ResponseConstraintStatus.MET, LinguisticStatus.ACCEPTABLE,
                    "That supplied Danish noun phrase expresses a different introduced meaning.",
                )
            }
        }
    }

    private fun isValid(exercise: GeneratedExercise, pairs: List<MatchingPair>): Boolean {
        val columns = requireNotNull(exercise.prompt.matchingColumns)
        val leftIds = columns.left.map { it.id }.toSet()
        val rightIds = columns.right.map { it.id }.toSet()
        return pairs.size <= MATCH_COUNT &&
            pairs.all { it.leftId in leftIds && it.rightId in rightIds } &&
            pairs.map { it.leftId }.toSet().size == pairs.size &&
            pairs.map { it.rightId }.toSet().size == pairs.size
    }

    private fun invalid(exercise: GeneratedExercise, answer: Answer, pairs: List<MatchingPair>): Evaluation {
        val source = requireNotNull(exercise.inspector.matchingSource)
        val targetByLeft = requireNotNull(exercise.lexicalTargets).associateBy { it.senseId }
        val results = source.pairs.map { expected ->
            MatchingPairResult(
                expected.leftId,
                null,
                targetByLeft.getValue(expected.lexicalSource.senseId),
                TargetStatus.UNEVALUABLE,
                MeaningSatisfaction.UNKNOWN,
                ResponseConstraintStatus.NOT_MET,
                LinguisticStatus.UNKNOWN,
                "The matching selection could not be evaluated.",
            )
        }
        return Evaluation(
            TargetStatus.UNEVALUABLE,
            LinguisticStatus.UNKNOWN,
            listOf(EvaluationIssue("MATCHING_SELECTION_INVALID", "Use supplied left and right IDs at most once.")),
            "The matching selection could not be evaluated.",
            "",
            answer,
            NormalizedAnswer.Matching(pairs),
            LinguisticScope.DANISH_MATCHING,
            ResponseConstraintStatus.NOT_MET,
            MeaningSatisfaction.UNKNOWN,
            results,
        )
    }

    private const val MATCH_COUNT = 4
}
