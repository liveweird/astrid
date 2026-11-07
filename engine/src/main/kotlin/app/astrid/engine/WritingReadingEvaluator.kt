package app.astrid.engine

import java.util.Locale

internal object WritingReadingEvaluator {
    fun evaluateReading(answer: Answer, key: AnswerKey.MeaningChoice): Evaluation {
        val text = answer.normalizedTextOrNull()
            ?: return lexicalTypeMismatch(answer)
        val folded = text.lowercase(Locale.ROOT)
        val expected = key.expected.lowercase(Locale.ROOT)
        val supplied = key.suppliedChoices.map { it.lowercase(Locale.ROOT) }.toSet()
        return when {
            folded == expected -> lexicalResult(
                TargetStatus.MET,
                LinguisticStatus.ACCEPTABLE,
                emptyList(),
                "Correct. The text explicitly links that participant and action to this object.",
                answer,
                text,
                ResponseConstraintStatus.MET,
            )
            folded in supplied -> lexicalResult(
                TargetStatus.NOT_MET,
                LinguisticStatus.ACCEPTABLE,
                listOf(issue("READING_EVIDENCE_MISMATCH", "The selected object belongs to a different fact in the text.")),
                "That object appears in the text, but it does not match both the participant and action in the question.",
                answer,
                text,
                ResponseConstraintStatus.MET,
            )
            else -> lexicalResult(
                TargetStatus.NOT_MET,
                LinguisticStatus.UNKNOWN,
                listOf(issue("UNRECOGNIZED_READING_CHOICE", "The response is not one of the supplied English choices.")),
                "Choose one of the supplied English answers.",
                answer,
                text,
                ResponseConstraintStatus.NOT_MET,
            )
        }
    }

    fun evaluateWriting(
        answer: Answer,
        expected: Sentence,
    ): Evaluation {
        val text = answer.normalizedTextOrNull()
        return when {
            text == null -> writingTypeMismatch(answer)
            !isOneStatement(text) -> writingResult(
                TargetStatus.UNEVALUABLE,
                MeaningSatisfaction.UNKNOWN,
                LinguisticStatus.UNKNOWN,
                listOf(issue("ONE_STATEMENT_REQUIRED", "The response must contain exactly one statement.")),
                "Enter exactly one Danish statement.",
                text,
                answer,
                ResponseConstraintStatus.NOT_MET,
            )
            else -> evaluateNormalizedWriting(text, answer, expected)
        }
    }

    private fun evaluateNormalizedWriting(
        text: String,
        answer: Answer,
        expected: Sentence,
    ): Evaluation {
        val recognized = recognizeWriting(text)
        return if (recognized == null) {
            writingResult(
                TargetStatus.UNEVALUABLE,
                MeaningSatisfaction.UNKNOWN,
                LinguisticStatus.UNKNOWN,
                listOf(
                    issue(
                        "WRITING_OUTSIDE_BOUNDED_DIAGNOSIS",
                        "This sentence is outside the bounded writing diagnoses.",
                    ),
                ),
                "Astrid cannot judge this Danish sentence beyond the introduced patterns.",
                text,
                answer,
                ResponseConstraintStatus.MET,
            )
        } else {
            evaluateRecognizedWriting(recognized, text, answer, expected)
        }
    }

    private fun evaluateRecognizedWriting(
        recognized: RecognizedWriting,
        text: String,
        answer: Answer,
        expected: Sentence,
    ): Evaluation {
        val meaning =
            if (recognized.sentence.id == expected.id) MeaningSatisfaction.MET else MeaningSatisfaction.NOT_MET
        val target = if (recognized.subjectFirst && recognized.issueCode != "INFINITIVE_USED") {
            TargetStatus.MET
        } else {
            TargetStatus.NOT_MET
        }
        val issues = buildList {
            if (target == TargetStatus.NOT_MET) {
                add(
                    issue(
                        "SUBJECT_FIRST_STATEMENT_REQUIRED",
                        "The response must begin with the subject and then use a finite verb.",
                    ),
                )
            }
            if (meaning == MeaningSatisfaction.NOT_MET) {
                add(issue("REQUESTED_MEANING_REQUIRED", "The response expresses a different introduced meaning."))
            }
            recognized.issueCode?.let { code -> add(issue(code, writingIssueMessage(code))) }
        }
        val language =
            if (recognized.issueCode == null) LinguisticStatus.ACCEPTABLE else LinguisticStatus.KNOWN_ISSUE
        val feedback =
            if (target == TargetStatus.MET && meaning == MeaningSatisfaction.MET &&
                language == LinguisticStatus.ACCEPTABLE
            ) {
                "Correct. The sentence begins with the subject and expresses the requested meaning."
            } else {
                "Review the subject-first target, requested meaning, and the bounded Danish diagnosis."
            }
        return writingResult(
            target,
            meaning,
            language,
            issues,
            feedback,
            text,
            answer,
            ResponseConstraintStatus.MET,
        )
    }
    private fun recognizeWriting(text: String): RecognizedWriting? {
        val surface = text.removeSuffix(".")
        val exact = PilotContent.sentences.firstNotNullOfOrNull { sentence ->
            val canonical = writingSurface(sentence, fronted = false)
            when {
                capitalizationEquivalent(surface, canonical) -> RecognizedWriting(sentence, subjectFirst = true, issueCode = null)
                sentence.constructionId == "C002" && capitalizationEquivalent(surface, writingSurface(sentence, fronted = true)) ->
                    RecognizedWriting(sentence, subjectFirst = false, issueCode = null)
                else -> null
            }
        }
        if (exact != null) return exact
        val deviations = PilotContent.sentences.flatMap { sentence ->
            listOf(false, true).filter { !it || sentence.constructionId == "C002" }.mapNotNull { fronted ->
                knownWritingDeviation(surface, sentence, fronted)
            }
        }
        return deviations.singleOrNull()
    }

    private fun knownWritingDeviation(
        surface: String,
        sentence: Sentence,
        fronted: Boolean,
    ): RecognizedWriting? {
        val canonical = writingSurface(sentence, fronted)
        val candidates = buildList {
            add(canonical.replaceWord(sentence.verb.present, sentence.verb.lemma) to "INFINITIVE_USED")
            sentence.complement?.let { complement ->
                val expected = complement.text.substringBefore(' ')
                val wrong = if (expected == "en") "et" else "en"
                add(canonical.replaceWord(expected, wrong) to "ARTICLE_GENDER_MISMATCH")
            }
            if (sentence.subject.lexicalIds.single() == "L006") {
                add(canonical.replaceWord("I", "i") to "PRONOUN_I_CASE")
            }
        }
        val matches = candidates.filter { (candidate, code) ->
            capitalizationEquivalent(
                surface,
                candidate,
                allowPluralILowercase = code == "PRONOUN_I_CASE",
            )
        }
        return matches.singleOrNull()?.let { (_, code) -> RecognizedWriting(sentence, !fronted, code) }
    }

    private fun writingSurface(sentence: Sentence, fronted: Boolean): String {
        val subject = sentence.subject.text
        val complement = sentence.complement?.text
        return if (fronted) {
            "${requireNotNull(complement)} ${sentence.verb.present} $subject"
        } else {
            listOfNotNull(subject, sentence.verb.present, complement).joinToString(" ")
        }
    }

    private fun capitalizationEquivalent(
        actual: String,
        expected: String,
        allowPluralILowercase: Boolean = false,
    ): Boolean {
        return if (actual == expected) {
            true
        } else if (actual.isEmpty() || expected.isEmpty() || actual.drop(1) != expected.drop(1)) {
            false
        } else {
            val actualFirst = actual.first()
            val expectedFirst = expected.first()
            val disallowedPluralCase = expectedFirst == 'I' && actualFirst == 'i' && !allowPluralILowercase
            !disallowedPluralCase && actualFirst.uppercaseChar() == expectedFirst.uppercaseChar()
        }
    }

    private fun String.replaceWord(expected: String, replacement: String): String =
        split(' ').joinToString(" ") { if (it == expected) replacement else it }

    private fun isOneStatement(text: String): Boolean {
        if (text.isBlank()) return false
        val withoutFinalPeriod = text.removeSuffix(".")
        return withoutFinalPeriod.isNotBlank() && '.' !in withoutFinalPeriod &&
            withoutFinalPeriod.none { it in "!?;:" }
    }

    private fun writingIssueMessage(code: String): String =
        when (code) {
            "INFINITIVE_USED" -> "The infinitive was used where the statement needs a finite present verb."
            "ARTICLE_GENDER_MISMATCH" -> "The article does not match the stored gender of the introduced noun."
            else -> "Plural-you I must remain uppercase in Danish."
        }

    private fun writingTypeMismatch(answer: Answer): Evaluation {
        val normalized = when (answer) {
            is Answer.Text -> NormalizedAnswer.Text(normalizeText(answer.text))
            is Answer.Order -> NormalizedAnswer.Order(answer.blockIds)
        }
        return Evaluation(
            TargetStatus.UNEVALUABLE,
            LinguisticStatus.UNKNOWN,
            listOf(issue("ANSWER_TYPE_MISMATCH", "The answer type does not match the exercise format.")),
            "The answer could not be evaluated for this writing exercise.",
            null,
            answer,
            normalized,
            LinguisticScope.DANISH_SENTENCE,
            ResponseConstraintStatus.NOT_MET,
            MeaningSatisfaction.UNKNOWN,
        )
    }

    private fun writingResult(
        target: TargetStatus,
        meaning: MeaningSatisfaction,
        linguistic: LinguisticStatus,
        issues: List<EvaluationIssue>,
        feedback: String,
        completed: String,
        raw: Answer,
        constraint: ResponseConstraintStatus,
    ) = Evaluation(
        target,
        linguistic,
        issues,
        feedback,
        completed,
        raw,
        NormalizedAnswer.Text(completed),
        LinguisticScope.DANISH_SENTENCE,
        constraint,
        meaning,
    )


    private fun lexicalTypeMismatch(answer: Answer): Evaluation {
        val normalized = when (answer) {
            is Answer.Text -> NormalizedAnswer.Text(normalizeText(answer.text))
            is Answer.Order -> NormalizedAnswer.Order(answer.blockIds)
        }
        return Evaluation(
            TargetStatus.UNEVALUABLE,
            LinguisticStatus.UNKNOWN,
            listOf(issue("ANSWER_TYPE_MISMATCH", "The answer type does not match the exercise format.")),
            "The answer could not be evaluated for this exercise.",
            null,
            answer,
            normalized,
            LinguisticScope.ENGLISH_CHOICE,
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
        constraint: ResponseConstraintStatus,
    ) = Evaluation(
        target,
        linguistic,
        issues,
        feedback,
        null,
        raw,
        NormalizedAnswer.Text(normalizedText),
        LinguisticScope.ENGLISH_CHOICE,
        constraint,
    )

    private fun Answer.normalizedTextOrNull(): String? =
        (this as? Answer.Text)?.text?.let(::normalizeText)

    private fun issue(code: String, message: String) = EvaluationIssue(code, message)

    private data class RecognizedWriting(
        val sentence: Sentence,
        val subjectFirst: Boolean,
        val issueCode: String?,
    )
}

