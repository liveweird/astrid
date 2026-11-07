package app.astrid.engine

internal object WritingReadingGenerator {
    fun createWritingExercise(request: GenerationRequest): GeneratedExercise {
        val candidateIndex = permutedIndex(request.seed, request.index, PilotContent.sentences.size)
        val sentence = PilotContent.sentences[candidateIndex]
        val parts = publicParts(sentence)
        val target = requireNotNull(request.targetSkillId)
        val lexicalIds =
            sentence.subject.lexicalIds + sentence.verb.lexicalId + sentence.complement.orEmptyLexicalIds()
        return GeneratedExercise(
            id = writingExerciseId(request, sentence),
            formatId = request.formatId,
            targetSkillId = target,
            lexicalTarget = null,
            supportSkillIds =
                (listOf("G001", "G002", "G003", "G004", "G005") +
                    if (sentence.complement != null) listOf("G006") else emptyList()) - target,
            presentation = ExercisePresentation(
                instruction = "Write one Danish statement that expresses the English meaning and begins with the subject.",
                context = "Use only the introduced pronoun, verb, and noun meaning for this task.",
                prompt = ExercisePrompt(
                    before = "",
                    after = "",
                    punctuation = "",
                    inputLabel = "Danish sentence",
                    cue = englishMeaning(sentence),
                    cueLanguage = "en",
                    answerLanguage = "da",
                ),
                introduction = writingIntroduction(sentence),
                hint = "Begin with the subject, then use the finite present verb, followed by any noun phrase.",
            ),
            contentRevision = request.contentRevision,
            generatorRevision = request.generatorRevision,
            reproduction = Reproduction(request.seed, request.index),
            inspector = ExerciseInspector(
                canonicalSentenceId = sentence.id,
                constructionId = sentence.constructionId,
                lexicalIds = lexicalIds,
                clauseRoles = parts.map { it.role },
                canonicalSentence = realize(parts),
            ),
            source = SentenceSource(sentence, AnswerKey.SubjectFirstOrder(parts.map { it.role })),
        )
    }

    private fun writingIntroduction(sentence: Sentence): List<IntroductionSection> {
        val subjectId = sentence.subject.lexicalIds.single()
        val pronoun = (PilotContent.humanPronouns + PilotContent.identifyingPronoun).single { it.id == subjectId }
        return buildList {
            sentence.complement?.let {
                add(IntroductionSection("Noun phrase: ${nounMeaning(sentence)}", it.text, "da"))
            }
            add(IntroductionSection("Finite present verb: ${sentence.verb.gloss}", sentence.verb.present, "da"))
            add(IntroductionSection("Pronoun: ${pronoun.gloss}", pronoun.text, "da"))
        }
    }

    fun createReadingExercise(request: GenerationRequest): GeneratedExercise {
        val unitIndex = permutedIndex(request.seed, request.index, READING_UNIT_COUNT)
        val unit = readingUnit(unitIndex)
        val ordered = orderReadingPropositions(unit.propositions, request.seed, unitIndex)
        val answerPropositions = ordered.filter {
            it.subjectId == unit.questionSubjectId && it.verbId == unit.questionVerbId
        }
        check(answerPropositions.size == 1)
        val answerNounId = answerPropositions.single().objectId
        val answer = PilotContent.vocabularySenses.single { it.entryId == answerNounId }.gloss
        val choices = shuffled(
            PilotContent.vocabularySenses.map { it.gloss },
            mixed(request.seed xor READING_CHOICE_SHUFFLE_XOR, unitIndex),
        )
        val task = requireNotNull(PilotContent.writingReadingFormats.single { it.id == READING_FORMAT_ID }.readingTask)
        val details = ordered.map {
            ReadingPropositionDetails(it.sentence.id, it.subjectId, it.verbId, it.objectId)
        }
        val lexicalIds = ordered.flatMap { proposition ->
            proposition.sentence.subject.lexicalIds + proposition.sentence.verb.lexicalId +
                proposition.sentence.complement.orEmptyLexicalIds()
        }.distinct()
        return GeneratedExercise(
            id = readingExerciseId(request, unitIndex),
            formatId = READING_FORMAT_ID,
            targetSkillId = null,
            lexicalTarget = null,
            supportSkillIds = emptyList(),
            presentation = ExercisePresentation(
                instruction = "Read the short Danish text and choose the English answer supported by it.",
                context = "Each pronoun consistently refers to the same described participant in this text.",
                prompt = ExercisePrompt(
                    before = "",
                    after = "",
                    choices = choices,
                    punctuation = "",
                    cue = readingQuestion(unit.questionSubjectId, unit.questionVerbId),
                    cueLanguage = "en",
                    answerLanguage = "en",
                    passage = ordered.map { realize(publicParts(it.sentence)) },
                ),
                introduction = listOf(
                    IntroductionSection(
                        "Reading support",
                        ordered.joinToString(" ") { englishMeaning(it.sentence) },
                    ),
                ),
                hint = "Match the participant and action in the question to one sentence, then identify its object.",
            ),
            contentRevision = request.contentRevision,
            generatorRevision = request.generatorRevision,
            reproduction = Reproduction(request.seed, request.index),
            inspector = ExerciseInspector(
                lexicalIds = lexicalIds,
                clauseRoles = emptyList(),
                readingSource = ReadingSourceDetails(
                    details,
                    unit.questionSubjectId,
                    unit.questionVerbId,
                    answerNounId,
                    answer,
                ),
            ),
            source = ReadingSource(
                ordered,
                unit.questionSubjectId,
                unit.questionVerbId,
                answerNounId,
                AnswerKey.MeaningChoice(answer, choices.toSet()),
            ),
            readingTarget = task,
        )
    }

    private fun readingUnit(index: Int): ReadingUnit {
        val subjectIds = listOf("L003", "L004")
        val verbIds = listOf("L009", "L011")
        val nounPermutations = listOf(
            listOf("L014", "L015", "L018"),
            listOf("L014", "L018", "L015"),
            listOf("L015", "L014", "L018"),
            listOf("L015", "L018", "L014"),
            listOf("L018", "L014", "L015"),
            listOf("L018", "L015", "L014"),
        )
        val subjectId = subjectIds[index / (verbIds.size * nounPermutations.size)]
        val remainder = index % (verbIds.size * nounPermutations.size)
        val verbId = verbIds[remainder / nounPermutations.size]
        val nouns = nounPermutations[remainder % nounPermutations.size]
        val otherSubject = subjectIds.single { it != subjectId }
        val otherVerb = verbIds.single { it != verbId }
        val triples = listOf(
            Triple(subjectId, verbId, nouns[0]),
            Triple(otherSubject, verbId, nouns[1]),
            Triple(subjectId, otherVerb, nouns[2]),
        )
        return ReadingUnit(
            triples.map { (subject, verb, noun) -> readingProposition(subject, verb, noun) },
            subjectId,
            verbId,
        )
    }

    private fun readingExerciseId(request: GenerationRequest, unitIndex: Int): String =
        "$READING_FORMAT_ID-${request.contentRevision}-${request.generatorRevision}-" +
            "R${unitIndex + 1}-${request.seed}-${request.index}"

    private fun readingProposition(subjectId: String, verbId: String, nounId: String): ReadingProposition {
        val sentence = PilotContent.sentences.single {
            it.constructionId == "C002" && it.subject.lexicalIds == listOf(subjectId) &&
                it.verb.lexicalId == verbId && it.complement?.lexicalIds?.last() == nounId
        }
        return ReadingProposition(sentence, subjectId, verbId, nounId)
    }

    private fun orderReadingPropositions(
        propositions: List<ReadingProposition>,
        seed: Int,
        unitIndex: Int,
    ): List<ReadingProposition> {
        val answerPosition = Math.floorMod(seed + unitIndex, propositions.size)
        val distractors = propositions.drop(1).let { if (mixed(seed, unitIndex) and 1L == 0L) it else it.reversed() }
        val result = distractors.toMutableList()
        result.add(answerPosition, propositions.first())
        return result
    }

    private fun readingQuestion(subjectId: String, verbId: String): String {
        val participant = if (subjectId == "L003") "he" else "she"
        val action = if (verbId == "L009") "have" else "read"
        return "According to the text, what does $participant $action?"
    }

    private fun englishMeaning(sentence: Sentence): String {
        val subjectId = sentence.subject.lexicalIds.single()
        val subject = (PilotContent.humanPronouns + PilotContent.identifyingPronoun).single { it.id == subjectId }
        val subjectText = englishSubject(subjectId, subject)
        val verb = englishVerb(sentence.verb.lexicalId, subjectId in THIRD_PERSON_SUBJECT_IDS)
        val objectText = sentence.complement?.let { " ${englishIndefinite(nounMeaning(sentence))}" }.orEmpty()
        return "$subjectText $verb$objectText."
    }

    private fun englishSubject(subjectId: String, subject: PronounEntry): String =
        when (subjectId) {
            "L002" -> "You (one person)"
            "L006" -> "You (more than one person)"
            "L008" -> "It or that"
            else -> subject.gloss.replaceFirstChar { it.titlecase() }
        }

    private fun englishVerb(verbId: String, thirdPerson: Boolean): String =
        when (verbId) {
            "L009" -> if (thirdPerson) "has" else "have"
            "L010" -> "is"
            "L011" -> if (thirdPerson) "reads" else "read"
            "L012" -> if (thirdPerson) "eats" else "eat"
            else -> if (thirdPerson) "sleeps" else "sleep"
        }

    private fun nounMeaning(sentence: Sentence): String {
        val nounId = requireNotNull(sentence.complement).lexicalIds.last()
        return PilotContent.vocabularySenses.singleOrNull { it.entryId == nounId }?.gloss
            ?: PilotContent.nouns.single { it.id == nounId }.gloss
    }

    private fun englishIndefinite(noun: String): String = "a${if (noun.first() in "aeiou") "n" else ""} $noun"


    private fun writingExerciseId(request: GenerationRequest, sentence: Sentence): String =
        "${request.formatId}-${request.contentRevision}-${request.generatorRevision}-" +
            "${sentence.id}-${request.seed}-${request.index}"

    private fun publicParts(sentence: Sentence): List<Constituent> =
        buildList {
            add(sentence.subject)
            add(Constituent(listOf(sentence.verb.lexicalId), sentence.verb.present, "FINITE_VERB"))
            sentence.complement?.let(::add)
        }

    private fun realize(parts: List<Constituent>): String =
        parts.mapIndexed { index, part ->
            val text = if (part.text == "I") "I" else part.text.lowercase()
            if (index == 0 && text != "I") text.replaceFirstChar { it.titlecase() } else text
        }.joinToString(" ") + "."

    private fun Constituent?.orEmptyLexicalIds(): List<String> = this?.lexicalIds.orEmpty()

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

    private data class ReadingUnit(
        val propositions: List<ReadingProposition>,
        val questionSubjectId: String,
        val questionVerbId: String,
    )

    private const val READING_CHOICE_SHUFFLE_XOR = 0x2468ace0
    private const val READING_FORMAT_ID = "E010"
    private const val READING_UNIT_COUNT = 24
    private const val GOLDEN_GAMMA = -7046029254386353131L
    private const val MIX_MULTIPLIER_ONE = -4658895280553007687L
    private const val MIX_MULTIPLIER_TWO = -7723592293110705685L
    private const val MIX_SHIFT_ONE = 30
    private const val MIX_SHIFT_TWO = 27
    private const val MIX_SHIFT_THREE = 31
    private const val LONG_HALF_BITS = 32
    private val THIRD_PERSON_SUBJECT_IDS = setOf("L003", "L004", "L008")
}
