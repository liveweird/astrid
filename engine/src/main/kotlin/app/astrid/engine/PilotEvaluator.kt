package app.astrid.engine

import java.util.Locale

internal object PilotEvaluator {
    fun evaluate(
        exercise: GeneratedExercise,
        answer: Answer,
    ): Evaluation = if (exercise.formatId in VocabularyP3Content.formatIds) {
        VocabularyP3ExerciseEvaluator.evaluate(exercise, answer)
    } else if (exercise.formatId in VocabularyP2Content.formatIds) {
        VocabularyP2ExerciseEvaluator.evaluate(exercise, answer)
    } else if (exercise.formatId in VocabularyP1Content.formatIds) {
        VocabularyP1ExerciseEvaluator.evaluate(exercise, answer)
    } else if (exercise.formatId == "E030") {
        NeutralQuestionExerciseEvaluator.evaluate(exercise, answer)
    } else {
        evaluateRetainedFormats(exercise, answer)
    }

    private fun evaluateRetainedFormats(
        exercise: GeneratedExercise,
        answer: Answer,
    ): Evaluation = when (exercise.formatId) {
        "E029" -> FinalCircumstanceExerciseEvaluator.evaluate(exercise, answer)
        "E028" -> ModalRoundExerciseEvaluator.evaluateInterpretation(exercise, answer)
        "E026", "E027" -> AbilityExerciseEvaluator.evaluate(exercise, answer)
        "E022", "E023", "E024", "E025" -> PresentOrderExerciseEvaluator.evaluate(exercise, answer)
        "E020", "E021" -> InanimateReferenceExerciseEvaluator.evaluate(exercise, answer)
        "E016", "E017", "E018", "E019" -> ModuleOneExerciseEvaluator.evaluate(exercise, answer)
        "E015" -> MultipleGapExerciseEvaluator.evaluate(
            exercise,
            answer,
            exercise.source.answerKey as AnswerKey.Gaps,
        )
        "E014" -> MatchingExerciseEvaluator.evaluate(
            exercise,
            answer,
            exercise.source.answerKey as AnswerKey.Matching,
        )
        "E011" -> TileExerciseEvaluator.evaluateChoice(
            answer,
            exercise.source.answerKey as AnswerKey.SuppliedChoice,
        )
        "E012", "E013" -> TileExerciseEvaluator.evaluateTiles(
            exercise,
            answer,
            exercise.source.answerKey as AnswerKey.TileOrder,
        )
        "E009" -> WritingReadingEvaluator.evaluateWriting(
            answer,
            (exercise.source as SentenceSource).sentence,
        )
        "E010" -> WritingReadingEvaluator.evaluateReading(
            answer,
            exercise.source.answerKey as AnswerKey.MeaningChoice,
        )
        else -> evaluateRetained(exercise, answer)
    }

    private fun evaluateRetained(exercise: GeneratedExercise, answer: Answer): Evaluation =
        when (val key = exercise.source.answerKey) {
            is AnswerKey.PresentForm -> evaluatePresent(exercise, answer, key)
            is AnswerKey.Article -> evaluateArticle(exercise, answer, key)
            is AnswerKey.SubjectFirstOrder -> evaluateOrder(exercise, answer, key)
            is AnswerKey.MeaningChoice -> evaluateMeaning(answer, key)
            is AnswerKey.Headword -> evaluateHeadword(answer, key)
            is AnswerKey.SuppliedChoice, is AnswerKey.TileOrder, is AnswerKey.Matching, is AnswerKey.Gaps,
            is AnswerKey.ModuleOneForm,
            ->
                error("New answer key reached retained evaluator")
        }

    private fun evaluateMeaning(
        answer: Answer,
        key: AnswerKey.MeaningChoice,
    ): Evaluation {
        val text = answer.normalizedTextOrNull()
            ?: return lexicalTypeMismatch(answer, LinguisticScope.ENGLISH_CHOICE)
        val folded = text.lowercase(Locale.ROOT)
        val expected = key.expected.lowercase(Locale.ROOT)
        val supplied = key.suppliedChoices.associateBy { it.lowercase(Locale.ROOT) }
        return when {
            folded == expected -> lexicalResult(
                TargetStatus.MET,
                LinguisticStatus.ACCEPTABLE,
                emptyList(),
                "Correct. You recognized the introduced English meaning.",
                answer,
                text,
                LinguisticScope.ENGLISH_CHOICE,
                ResponseConstraintStatus.MET,
            )
            folded in supplied -> lexicalResult(
                TargetStatus.NOT_MET,
                LinguisticStatus.ACCEPTABLE,
                listOf(issue("INTRODUCED_MEANING_REQUIRED", "The selected choice is not the introduced meaning of this headword.")),
                "That is a recognized English choice, but it is not the introduced meaning.",
                answer,
                text,
                LinguisticScope.ENGLISH_CHOICE,
                ResponseConstraintStatus.MET,
            )
            else -> lexicalResult(
                TargetStatus.NOT_MET,
                LinguisticStatus.UNKNOWN,
                listOf(issue("UNRECOGNIZED_MEANING_CHOICE", "The response is not one of the supplied English choices.")),
                "Choose one of the supplied English meanings.",
                answer,
                text,
                LinguisticScope.ENGLISH_CHOICE,
                ResponseConstraintStatus.NOT_MET,
            )
        }
    }

    private fun evaluateHeadword(
        answer: Answer,
        key: AnswerKey.Headword,
    ): Evaluation =
        answer.normalizedTextOrNull()?.let { text ->
            parseKnownHeadword(text.lowercase(Locale.ROOT))?.let { recognized ->
                evaluateKnownHeadword(answer, text, key, recognized)
            } ?: evaluateUnknownHeadword(answer, text)
        } ?: lexicalTypeMismatch(answer, LinguisticScope.DANISH_HEADWORD)

    private fun parseKnownHeadword(folded: String): RecognizedHeadword? {
        val knownNouns = PilotContent.nouns.associateBy { it.text.lowercase(Locale.ROOT) }
        val direct = knownNouns[folded]
        val parts = folded.split(' ')
        val article = parts.firstOrNull().takeIf { parts.size == ARTICLE_PHRASE_SIZE && it in setOf("en", "et") }
        val phrase = article?.let { supplied -> knownNouns[parts[1]]?.let { RecognizedHeadword(it, supplied) } }
        return direct?.let { RecognizedHeadword(it, null) } ?: phrase
    }

    private fun evaluateUnknownHeadword(
        answer: Answer,
        text: String,
    ): Evaluation {
        val constraint =
            if (text.isNotEmpty() && text.none(Char::isWhitespace)) {
                ResponseConstraintStatus.MET
            } else {
                ResponseConstraintStatus.NOT_MET
            }
        val issues = buildList {
            add(issue("UNRECOGNIZED_HEADWORD", "This is not one of the introduced Danish seed nouns."))
            if (constraint == ResponseConstraintStatus.NOT_MET) {
                add(issue("HEADWORD_ONLY_REQUIRED", "The response must be one word without an article."))
            }
        }
        return lexicalResult(
            TargetStatus.NOT_MET,
            LinguisticStatus.UNKNOWN,
            issues,
            "That does not match the introduced headword. Astrid does not determine whether it could be " +
                "valid Danish or an alternative translation.",
            answer,
            text,
            LinguisticScope.DANISH_HEADWORD,
            constraint,
        )
    }

    private fun evaluateKnownHeadword(
        answer: Answer,
        text: String,
        key: AnswerKey.Headword,
        recognized: RecognizedHeadword,
    ): Evaluation {
        val noun = recognized.noun
        val targetMet = noun.id == key.expected.entryId
        val hasArticle = recognized.article != null
        val articleMismatch = recognized.article != null && recognized.article != noun.gender.article
        val issues = knownHeadwordIssues(noun, targetMet, hasArticle, articleMismatch)
        val feedback = knownHeadwordFeedback(noun, targetMet, hasArticle, articleMismatch)
        return lexicalResult(
            if (targetMet) TargetStatus.MET else TargetStatus.NOT_MET,
            if (articleMismatch) LinguisticStatus.KNOWN_ISSUE else LinguisticStatus.ACCEPTABLE,
            issues,
            feedback,
            answer,
            text,
            LinguisticScope.DANISH_HEADWORD,
            if (hasArticle) ResponseConstraintStatus.NOT_MET else ResponseConstraintStatus.MET,
        )
    }

    private fun knownHeadwordIssues(
        noun: NounEntry,
        targetMet: Boolean,
        hasArticle: Boolean,
        articleMismatch: Boolean,
    ): List<EvaluationIssue> =
        buildList {
            if (!targetMet) add(issue("INTRODUCED_HEADWORD_REQUIRED", "The response uses a different introduced Danish noun."))
            if (hasArticle) add(issue("HEADWORD_ONLY_REQUIRED", "The response must contain only the headword, without an article."))
            if (articleMismatch) {
                add(issue("ARTICLE_GENDER_MISMATCH", "The article does not match the stored gender of ${noun.text}."))
            }
        }

    private fun knownHeadwordFeedback(
        noun: NounEntry,
        targetMet: Boolean,
        hasArticle: Boolean,
        articleMismatch: Boolean,
    ): String =
        when {
            targetMet && hasArticle && articleMismatch ->
                "You identified the introduced headword. Enter it without the article, and use its stored " +
                    "article ${noun.gender.article} when learning the noun phrase."
            targetMet && hasArticle -> "You identified the introduced headword. Enter it without the article."
            targetMet -> "Correct. You recalled the introduced Danish headword."
            else -> "That is a recognized Danish headword, but the task asks for a different introduced headword."
        }

    private fun evaluatePresent(
        exercise: GeneratedExercise,
        answer: Answer,
        key: AnswerKey.PresentForm,
    ): Evaluation {
        val text = answer.normalizedTextOrNull() ?: return typeMismatch(answer)
        val completed = completeTextExercise(exercise, text)
        val evaluatesNamedVerb = exercise.formatId == "E004" || exercise.formatId == "E005"
        return when (text) {
            key.present -> result(
                TargetStatus.MET,
                LinguisticStatus.ACCEPTABLE,
                emptyList(),
                "Correct. The statement uses the finite present form.",
                completed,
                answer,
                text,
            )
            key.infinitive -> result(
                TargetStatus.NOT_MET,
                LinguisticStatus.KNOWN_ISSUE,
                listOf(issue("INFINITIVE_USED", "The infinitive was used where this statement needs a finite verb.")),
                "Use the finite present form after the subject, not the infinitive.",
                completed,
                answer,
                text,
            )
            else ->
                if (evaluatesNamedVerb && isLicensedFiniteSubstitution(exercise, text)) {
                    result(
                        TargetStatus.NOT_MET,
                        LinguisticStatus.ACCEPTABLE,
                        listOf(issue("NAMED_VERB_REQUIRED", "The replacement makes a licensed statement but uses a different verb.")),
                        "This is an acceptable licensed statement, but the task asks for the named verb.",
                        completed,
                        answer,
                        text,
                    )
                } else {
                    unrecognizedPresentForm(completed, answer, text)
                }
        }
    }

    private fun unrecognizedPresentForm(
        completed: String,
        answer: Answer,
        text: String,
    ): Evaluation =
        result(
            TargetStatus.NOT_MET,
            LinguisticStatus.UNKNOWN,
            listOf(issue("UNRECOGNIZED_FORM", "This form is not one of the introduced forms for the named verb.")),
            "That does not match the introduced finite present form. Astrid does not classify it as invalid Danish generally.",
            completed,
            answer,
            text,
        )

    private fun isLicensedFiniteSubstitution(
        exercise: GeneratedExercise,
        replacement: String,
    ): Boolean {
        val original = (exercise.source as SentenceSource).sentence
        return PilotContent.sentences.any { candidate ->
            candidate.id != original.id &&
                candidate.constructionId == original.constructionId &&
                candidate.subject == original.subject &&
                candidate.complement == original.complement &&
                candidate.verb.present == replacement
        }
    }

    private fun evaluateArticle(
        exercise: GeneratedExercise,
        answer: Answer,
        key: AnswerKey.Article,
    ): Evaluation {
        val text = answer.normalizedTextOrNull() ?: return typeMismatch(answer)
        val completed = completeTextExercise(exercise, text)
        val otherArticle = if (key.expected == "en") "et" else "en"
        val expectedNumeral = if (key.expected == "en") "én" else "ét"
        val otherNumeral = if (key.expected == "en") "ét" else "én"
        return when (text) {
            key.expected -> result(
                TargetStatus.MET,
                LinguisticStatus.ACCEPTABLE,
                emptyList(),
                "Correct. This noun uses the selected indefinite article.",
                completed,
                answer,
                text,
            )
            expectedNumeral -> result(
                TargetStatus.NOT_MET,
                LinguisticStatus.ACCEPTABLE,
                listOf(issue("NUMERAL_NOT_ARTICLE", "The stressed numeral ‘one’ is not the requested article.")),
                "The sentence can be grammatical with this numeral, but the task asks for the unstressed indefinite article.",
                completed,
                answer,
                text,
            )
            otherArticle, otherNumeral -> result(
                TargetStatus.NOT_MET,
                LinguisticStatus.KNOWN_ISSUE,
                listOf(issue("ARTICLE_GENDER_MISMATCH", "The article does not match the stored gender of ${key.noun}.")),
                "Learn this noun together with its stored indefinite article.",
                completed,
                answer,
                text,
            )
            else -> result(
                TargetStatus.NOT_MET,
                LinguisticStatus.UNKNOWN,
                listOf(issue("UNRECOGNIZED_DETERMINER", "This answer is not a supported article diagnosis.")),
                "The answer does not meet the article target. Astrid cannot make a broader Danish judgment about it.",
                completed,
                answer,
                text,
            )
        }
    }

    // Guard returns keep malformed transport input separate from a valid but off-target ordering attempt.
    @Suppress("ReturnCount")
    private fun evaluateOrder(
        exercise: GeneratedExercise,
        answer: Answer,
        key: AnswerKey.SubjectFirstOrder,
    ): Evaluation {
        val order = (answer as? Answer.Order)?.blockIds ?: return typeMismatch(answer)
        val expectedIds = exercise.prompt.blocks.map { it.id }.toSet()
        if (order.size != expectedIds.size || order.toSet() != expectedIds) {
            return result(
                TargetStatus.UNEVALUABLE,
                LinguisticStatus.UNKNOWN,
                listOf(issue("INVALID_BLOCK_SET", "The answer must contain every supplied block exactly once.")),
                "The block answer could not be evaluated.",
                null,
                answer,
                NormalizedAnswer.Order(order),
            )
        }
        val roles = order.map { exercise.source.blockRoles.getValue(it) }
        val completed = realizeOrderedBlocks(exercise, order)
        if (roles == key.canonicalRoles) {
            return result(
                TargetStatus.MET,
                LinguisticStatus.ACCEPTABLE,
                emptyList(),
                "Correct. The statement begins with the subject and then uses the finite verb.",
                completed,
                answer,
                NormalizedAnswer.Order(order),
            )
        }
        val frontedRoles =
            key.canonicalRoles
                .takeIf { it.size == THREE_PART_ORDER_SIZE }
                ?.let { listOf(it[2], it[1], it[0]) }
        return if (roles == frontedRoles) {
            result(
                TargetStatus.NOT_MET,
                LinguisticStatus.ACCEPTABLE,
                listOf(issue("VALID_FRONTING_OUTSIDE_TARGET", "The complement is fronted and the finite verb remains second.")),
                "This is an acceptable fronted Danish statement, but it does not begin with the requested subject.",
                completed,
                answer,
                NormalizedAnswer.Order(order),
            )
        } else {
            result(
                TargetStatus.NOT_MET,
                LinguisticStatus.UNKNOWN,
                listOf(issue("ORDER_OUTSIDE_SUPPORTED_PATTERN", "This order is not one of the patterns evaluated by this pilot.")),
                "The order does not meet the subject-first target. Astrid does not make a language-wide judgment about it.",
                completed,
                answer,
                NormalizedAnswer.Order(order),
            )
        }
    }

    private fun typeMismatch(answer: Answer): Evaluation =
        result(
            TargetStatus.UNEVALUABLE,
            LinguisticStatus.UNKNOWN,
            listOf(issue("ANSWER_TYPE_MISMATCH", "The answer type does not match the exercise format.")),
            "The answer could not be evaluated for this exercise.",
            null,
            answer,
            when (answer) {
                is Answer.Text -> NormalizedAnswer.Text(normalizeText(answer.text))
                is Answer.Order -> NormalizedAnswer.Order(answer.blockIds)
                is Answer.Matching -> NormalizedAnswer.Matching(answer.pairs)
                is Answer.Gaps -> NormalizedAnswer.Gaps(answer.selections)
            },
        )

    private fun lexicalTypeMismatch(
        answer: Answer,
        scope: LinguisticScope,
    ): Evaluation {
        val normalized = when (answer) {
            is Answer.Text -> NormalizedAnswer.Text(normalizeText(answer.text))
            is Answer.Order -> NormalizedAnswer.Order(answer.blockIds)
            is Answer.Matching -> NormalizedAnswer.Matching(answer.pairs)
            is Answer.Gaps -> NormalizedAnswer.Gaps(answer.selections)
        }
        return Evaluation(
            TargetStatus.UNEVALUABLE,
            LinguisticStatus.UNKNOWN,
            listOf(issue("ANSWER_TYPE_MISMATCH", "The answer type does not match the exercise format.")),
            "The answer could not be evaluated for this exercise.",
            null,
            answer,
            normalized,
            scope,
            ResponseConstraintStatus.NOT_MET,
        )
    }

    private fun lexicalResult(
        target: TargetStatus,
        linguistic: LinguisticStatus,
        issues: List<EvaluationIssue>,
        feedback: String,
        raw: Answer,
        normalizedText: String,
        scope: LinguisticScope,
        constraint: ResponseConstraintStatus,
    ) = Evaluation(
        target,
        linguistic,
        issues,
        feedback,
        null,
        raw,
        NormalizedAnswer.Text(normalizedText),
        scope,
        constraint,
    )

    private fun result(
        target: TargetStatus,
        linguistic: LinguisticStatus,
        issues: List<EvaluationIssue>,
        feedback: String,
        completed: String?,
        raw: Answer,
        normalizedText: String,
    ) = result(target, linguistic, issues, feedback, completed, raw, NormalizedAnswer.Text(normalizedText))

    private fun result(
        target: TargetStatus,
        linguistic: LinguisticStatus,
        issues: List<EvaluationIssue>,
        feedback: String,
        completed: String?,
        raw: Answer,
        normalized: NormalizedAnswer,
    ) = Evaluation(target, linguistic, issues, feedback, completed, raw, normalized)

    private fun completeTextExercise(exercise: GeneratedExercise, answer: String): String =
        "${exercise.prompt.before}$answer${exercise.prompt.after}".trim().replaceFirstChar { it.titlecase() }

    private fun realizeOrderedBlocks(
        exercise: GeneratedExercise,
        order: List<String>,
    ): String {
        val byId = exercise.prompt.blocks.associateBy { it.id }
        return order.map { byId.getValue(it).text }.mapIndexed { index, text ->
            when {
                text == "I" -> text
                index == 0 -> text.replaceFirstChar { it.titlecase() }
                else -> text.replaceFirstChar { it.lowercase() }
            }
        }.joinToString(" ") + exercise.prompt.punctuation
    }

    private fun Answer.normalizedTextOrNull(): String? =
        (this as? Answer.Text)?.text?.let(::normalizeText)

    private fun issue(code: String, message: String) = EvaluationIssue(code, message)

    private const val THREE_PART_ORDER_SIZE = 3
    private const val ARTICLE_PHRASE_SIZE = 2

    private data class RecognizedHeadword(
        val noun: NounEntry,
        val article: String?,
    )
}
