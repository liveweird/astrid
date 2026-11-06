package app.astrid.engine

internal object PilotEvaluator {
    fun evaluate(
        exercise: GeneratedExercise,
        answer: Answer,
    ): Evaluation =
        when (val key = exercise.source.answerKey) {
            is AnswerKey.PresentForm -> evaluatePresent(exercise, answer, key)
            is AnswerKey.Article -> evaluateArticle(exercise, answer, key)
            is AnswerKey.SubjectFirstOrder -> evaluateOrder(exercise, answer, key)
        }

    private fun evaluatePresent(
        exercise: GeneratedExercise,
        answer: Answer,
        key: AnswerKey.PresentForm,
    ): Evaluation {
        val text = answer.normalizedTextOrNull() ?: return typeMismatch(answer)
        val completed = completeTextExercise(exercise, text)
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
            else -> result(
                TargetStatus.NOT_MET,
                LinguisticStatus.UNKNOWN,
                listOf(issue("UNRECOGNIZED_FORM", "This form is not one of the introduced forms for the named verb.")),
                "That does not match the introduced finite present form. Astrid does not classify it as invalid Danish generally.",
                completed,
                answer,
                text,
            )
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
            },
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
}
