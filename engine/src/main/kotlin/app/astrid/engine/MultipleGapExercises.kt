package app.astrid.engine

internal object MultipleGapExerciseGenerator {
    fun create(request: GenerationRequest): GeneratedExercise {
        val candidates = PilotContent.sentences.filter { it.complement != null }
        val sentence = candidates[permutedIndex(request.seed, request.index, candidates.size)]
        val complement = requireNotNull(sentence.complement)
        val noun = PilotContent.nouns.single { it.id == complement.lexicalIds.last() }
        val verbSlot = slot(
            id = VERB_SLOT_ID,
            label = "Present verb — Primary target",
            targetSkillId = "G004",
            role = GapAssessmentRole.PRIMARY,
            expected = sentence.verb.present,
            alternative = sentence.verb.lemma,
            entropy = mixed(request.seed xor VERB_SHUFFLE_XOR, request.index),
        )
        val articleSlot = slot(
            id = ARTICLE_SLOT_ID,
            label = "Indefinite article — Supporting grammar",
            targetSkillId = "G006",
            role = GapAssessmentRole.SUPPORT,
            expected = noun.gender.article,
            alternative = if (noun.gender.article == "en") "et" else "en",
            entropy = mixed(request.seed xor ARTICLE_SHUFFLE_XOR, request.index),
        )
        val sources = listOf(
            verbSlot.source(sentence.verb.lexicalId),
            articleSlot.source(noun.id),
        )
        val canonical = listOf(sentence.subject.text, sentence.verb.present, complement.text)
            .joinToString(" ").replaceFirstChar { it.titlecase() } + "."
        return exercise(request, sentence, noun, verbSlot, articleSlot, sources, canonical)
    }

    private fun exercise(
        request: GenerationRequest,
        sentence: Sentence,
        noun: NounEntry,
        verbSlot: GeneratedSlot,
        articleSlot: GeneratedSlot,
        sources: List<GapSlotSourceDetails>,
        canonical: String,
    ): GeneratedExercise {
        val complement = requireNotNull(sentence.complement)
        return GeneratedExercise(
            id = "E015-${request.contentRevision}-${request.generatorRevision}-${sentence.id}-${request.seed}-${request.index}",
            formatId = "E015",
            targetSkillId = "G004",
            lexicalTarget = null,
            supportSkillIds = listOf("G001", "G002", "G003", "G005", "G006"),
            presentation = ExercisePresentation(
                instruction = "Choose the finite present verb and the noun's indefinite article.",
                context = "Complete both labeled grammar gaps in the Danish statement.",
                prompt = ExercisePrompt(
                    before = "",
                    after = "",
                    choices = emptyList(),
                    blocks = emptyList(),
                    punctuation = "",
                    cueLanguage = "da",
                    answerLanguage = "da",
                    gapPrompt = GapPrompt(
                        segments = listOf(
                            sentence.subject.text.replaceFirstChar { it.titlecase() } + " ",
                            " ",
                            " ${noun.text}.",
                        ),
                        slots = listOf(verbSlot.slot, articleSlot.slot),
                    ),
                ),
                introduction = listOf(
                    IntroductionSection(
                        "Present verb — Primary target",
                        "${sentence.verb.lemma} → ${sentence.verb.present}",
                        "da",
                    ),
                    IntroductionSection(
                        "Indefinite article — Supporting grammar",
                        "${noun.gender.article} ${noun.text}",
                        "da",
                    ),
                ),
                hint = "Choose a finite present form for the primary gap and the stored noun article for the supporting gap.",
            ),
            contentRevision = request.contentRevision,
            generatorRevision = request.generatorRevision,
            reproduction = Reproduction(request.seed, request.index),
            inspector = ExerciseInspector(
                canonicalSentenceId = sentence.id,
                constructionId = sentence.constructionId,
                lexicalIds = sentence.subject.lexicalIds + sentence.verb.lexicalId + complement.lexicalIds,
                clauseRoles = listOf(sentence.subject.role, "FINITE_VERB", complement.role),
                canonicalSentence = canonical,
                gapSource = GapSourceDetails(sources),
            ),
            source = MultipleGapExerciseSource(
                sentence,
                AnswerKey.Gaps(sources.associate { it.slotId to it.expectedChoiceId }),
            ),
        )
    }

    private fun slot(
        id: String,
        label: String,
        targetSkillId: String,
        role: GapAssessmentRole,
        expected: String,
        alternative: String,
        entropy: Long,
    ): GeneratedSlot {
        val forms = if ((entropy and 1L) == 0L) listOf(expected, alternative) else listOf(alternative, expected)
        val choices = forms.mapIndexed { index, form -> GapChoice("c$index", form) }
        return GeneratedSlot(
            GapSlot(id, label, targetSkillId, role, choices),
            expected,
            alternative,
            choices.single { it.text == expected }.id,
        )
    }

    private fun GeneratedSlot.source(lexicalId: String) = GapSlotSourceDetails(
        slot.id,
        expectedChoiceId,
        lexicalId,
        expectedForm,
        alternativeForm,
    )

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

    private data class GeneratedSlot(
        val slot: GapSlot,
        val expectedForm: String,
        val alternativeForm: String,
        val expectedChoiceId: String,
    )

    private const val VERB_SLOT_ID = "g0"
    private const val ARTICLE_SLOT_ID = "g1"
    private const val VERB_SHUFFLE_XOR = 0x2468ace
    private const val ARTICLE_SHUFFLE_XOR = 0x13579bd
    private const val GOLDEN_GAMMA = -7046029254386353131L
    private const val MIX_MULTIPLIER_ONE = -4658895280553007687L
    private const val MIX_MULTIPLIER_TWO = -7723592293110705685L
    private const val MIX_SHIFT_ONE = 30
    private const val MIX_SHIFT_TWO = 27
    private const val MIX_SHIFT_THREE = 31
}

internal object MultipleGapExerciseEvaluator {
    fun evaluate(exercise: GeneratedExercise, answer: Answer, key: AnswerKey.Gaps): Evaluation {
        val selections = (answer as? Answer.Gaps)?.selections
        return when {
            selections == null -> invalid(exercise, answer, normalized(answer), "ANSWER_TYPE_MISMATCH")
            !isValid(exercise, selections) ->
                invalid(exercise, answer, NormalizedAnswer.Gaps(selections), "GAP_SELECTION_INVALID")
            else -> evaluateValid(exercise, answer, selections, key)
        }
    }

    private fun evaluateValid(
        exercise: GeneratedExercise,
        answer: Answer,
        selections: List<GapSelection>,
        key: AnswerKey.Gaps,
    ): Evaluation {
        val selectedBySlot = selections.associate { it.slotId to it.choiceId }
        val prompt = requireNotNull(exercise.prompt.gapPrompt)
        val sourceBySlot = requireNotNull(exercise.inspector.gapSource).slots.associateBy { it.slotId }
        val results = prompt.slots.map { slot ->
            val selected = selectedBySlot[slot.id]
            val expected = key.expectedBySlotId.getValue(slot.id)
            when {
                selected == null -> missing(slot)
                selected == expected -> correct(slot, selected)
                else -> wrong(slot, selected, sourceBySlot.getValue(slot.id))
            }
        }
        val primary = results.single { it.assessmentRole == GapAssessmentRole.PRIMARY }
        val complete = selections.size == prompt.slots.size
        val anyWrong = results.any { it.targetStatus == TargetStatus.NOT_MET }
        val linguistic = when {
            anyWrong -> LinguisticStatus.KNOWN_ISSUE
            !complete -> LinguisticStatus.UNKNOWN
            else -> LinguisticStatus.ACCEPTABLE
        }
        return Evaluation(
            targetStatus = primary.targetStatus,
            linguisticStatus = linguistic,
            issues = issues(results),
            feedback = overallFeedback(primary.targetStatus, complete, anyWrong),
            completedSentence = if (complete) completedSentence(exercise, selectedBySlot) else "",
            rawAnswer = answer,
            normalizedAnswer = NormalizedAnswer.Gaps(selections),
            linguisticScope = LinguisticScope.DANISH_GAPS,
            responseConstraintStatus = if (complete) ResponseConstraintStatus.MET else ResponseConstraintStatus.NOT_MET,
            gapResults = results,
        )
    }

    private fun isValid(exercise: GeneratedExercise, selections: List<GapSelection>): Boolean {
        val slots = requireNotNull(exercise.prompt.gapPrompt).slots
        val choiceIds = slots.associate { slot -> slot.id to slot.choices.map { it.id }.toSet() }
        return selections.size <= slots.size &&
            selections.map { it.slotId }.toSet().size == selections.size &&
            selections.all { it.slotId in choiceIds && it.choiceId in choiceIds.getValue(it.slotId) }
    }

    private fun correct(slot: GapSlot, choiceId: String) = GapResult(
        slot.id, choiceId, slot.targetSkillId, slot.assessmentRole,
        TargetStatus.MET, ResponseConstraintStatus.MET, LinguisticStatus.ACCEPTABLE,
        if (slot.assessmentRole == GapAssessmentRole.PRIMARY) {
            "Correct. The primary target uses the finite present form."
        } else {
            "Correct. The supporting grammar uses the noun's stored article."
        },
    )

    private fun wrong(slot: GapSlot, choiceId: String, source: GapSlotSourceDetails) = GapResult(
        slot.id, choiceId, slot.targetSkillId, slot.assessmentRole,
        TargetStatus.NOT_MET, ResponseConstraintStatus.MET, LinguisticStatus.KNOWN_ISSUE,
        if (slot.assessmentRole == GapAssessmentRole.PRIMARY) {
            "The primary target uses ${source.alternativeForm}; choose the finite present form ${source.expectedForm}."
        } else {
            "The supporting grammar article does not match the stored gender; choose ${source.expectedForm}."
        },
    )

    private fun missing(slot: GapSlot) = GapResult(
        slot.id, null, slot.targetSkillId, slot.assessmentRole,
        TargetStatus.UNEVALUABLE, ResponseConstraintStatus.NOT_MET, LinguisticStatus.UNKNOWN,
        if (slot.assessmentRole == GapAssessmentRole.PRIMARY) {
            "Choose a supplied form for the primary target."
        } else {
            "Choose a supplied article for the supporting grammar."
        },
    )

    private fun issues(results: List<GapResult>): List<EvaluationIssue> = buildList {
        if (results.any { it.assessmentRole == GapAssessmentRole.PRIMARY && it.targetStatus == TargetStatus.NOT_MET }) {
            add(EvaluationIssue("INFINITIVE_USED", "The infinitive was used where the primary target needs a finite verb."))
        }
        if (results.any { it.assessmentRole == GapAssessmentRole.SUPPORT && it.targetStatus == TargetStatus.NOT_MET }) {
            add(EvaluationIssue("ARTICLE_GENDER_MISMATCH", "The supporting article does not match the stored noun gender."))
        }
    }

    private fun overallFeedback(primary: TargetStatus, complete: Boolean, anyWrong: Boolean): String = when {
        primary == TargetStatus.MET && complete && !anyWrong -> "Good work. Both the primary target and supporting grammar are correct."
        anyWrong -> "Review the primary-target and supporting-grammar feedback, then try both gaps again."
        else -> "Complete both the primary target and supporting grammar gaps."
    }

    private fun completedSentence(exercise: GeneratedExercise, selectedBySlot: Map<String, String>): String {
        val prompt = requireNotNull(exercise.prompt.gapPrompt)
        val selectedText = prompt.slots.associate { slot ->
            slot.id to slot.choices.single { it.id == selectedBySlot.getValue(slot.id) }.text
        }
        return buildString {
            append(prompt.segments[0])
            append(selectedText.getValue(prompt.slots[0].id))
            append(prompt.segments[1])
            append(selectedText.getValue(prompt.slots[1].id))
            append(prompt.segments[2])
        }
    }

    private fun invalid(
        exercise: GeneratedExercise,
        answer: Answer,
        normalized: NormalizedAnswer,
        issueCode: String,
    ): Evaluation = Evaluation(
        TargetStatus.UNEVALUABLE,
        LinguisticStatus.UNKNOWN,
        listOf(EvaluationIssue(issueCode, "The gap answer could not be evaluated.")),
        "The gap answer could not be evaluated.",
        "",
        answer,
        normalized,
        LinguisticScope.DANISH_GAPS,
        ResponseConstraintStatus.NOT_MET,
        gapResults = requireNotNull(exercise.prompt.gapPrompt).slots.map(::missing),
    )

    private fun normalized(answer: Answer): NormalizedAnswer = when (answer) {
        is Answer.Text -> NormalizedAnswer.Text(normalizeText(answer.text))
        is Answer.Order -> NormalizedAnswer.Order(answer.blockIds)
        is Answer.Matching -> NormalizedAnswer.Matching(answer.pairs)
        is Answer.Gaps -> NormalizedAnswer.Gaps(answer.selections)
    }
}
