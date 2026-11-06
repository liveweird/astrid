package app.astrid.engine

class PilotEngine {
    fun catalog(): Catalog =
        Catalog(
            skills = PilotContent.skills,
            formats = PilotContent.formats,
            sentenceCount = PilotContent.sentences.size,
            contentRevision = PilotContent.CONTENT_REVISION,
            generatorRevision = PilotContent.GENERATOR_REVISION,
        )

    fun generate(request: GenerationRequest): GeneratedExercise {
        validate(request)
        val candidates =
            when (request.formatId) {
                "E001", "E003" -> PilotContent.sentences
                "E002" -> PilotContent.sentences.filter { it.complement != null }
                else -> error("Unreachable format ${request.formatId}")
            }
        val candidateIndex = permutedIndex(request.seed, request.index, candidates.size)
        val sentence = candidates[candidateIndex]
        return createExercise(request, sentence, candidateIndex)
    }

    fun evaluate(
        exercise: GeneratedExercise,
        answer: Answer,
    ): Evaluation = PilotEvaluator.evaluate(exercise, answer)

    private fun validate(request: GenerationRequest) {
        require(request.seed >= 0) { "seed must be non-negative" }
        require(request.index in 0..MAX_INDEX) { "index must be between 0 and $MAX_INDEX" }
        require(request.contentRevision == PilotContent.CONTENT_REVISION) {
            "Unsupported content revision: ${request.contentRevision}"
        }
        require(request.generatorRevision == PilotContent.GENERATOR_REVISION) {
            "Unsupported generator revision: ${request.generatorRevision}"
        }
        val expectedTarget = mapOf("E001" to "G004", "E002" to "G006", "E003" to "G005")[request.formatId]
        requireNotNull(expectedTarget) { "Unsupported format: ${request.formatId}" }
        require(request.targetSkillId == expectedTarget) {
            "Format ${request.formatId} requires target $expectedTarget"
        }
    }

    private fun createExercise(
        request: GenerationRequest,
        sentence: Sentence,
        candidateIndex: Int,
    ): GeneratedExercise {
        val publicParts = publicParts(sentence)
        val sourceAndPrompt =
            when (request.formatId) {
                "E001" -> presentExercise(sentence, publicParts, request.seed, candidateIndex)
                "E002" -> articleExercise(sentence, publicParts)
                "E003" -> orderExercise(publicParts, request.seed, candidateIndex)
                else -> error("Unreachable format ${request.formatId}")
            }
        val lexicalIds =
            sentence.subject.lexicalIds + sentence.verb.lexicalId + sentence.complement.orEmptyLexicalIds()
        return GeneratedExercise(
            id = "${request.formatId}-${sentence.id}-${request.seed}-${request.index}",
            formatId = request.formatId,
            targetSkillId = request.targetSkillId,
            supportSkillIds =
                (listOf("G001", "G002", "G003", "G004", "G005") +
                    if (sentence.complement != null) listOf("G006") else emptyList()) - request.targetSkillId,
            presentation =
                ExercisePresentation(
                    instruction = sourceAndPrompt.instruction,
                    context = contextFor(sentence),
                    prompt = sourceAndPrompt.prompt,
                    introduction = introductionFor(request.formatId, sentence),
                    hint = hintFor(request.formatId),
                ),
            contentRevision = request.contentRevision,
            generatorRevision = request.generatorRevision,
            reproduction = Reproduction(request.seed, request.index),
            inspector =
                ExerciseInspector(
                    canonicalSentenceId = sentence.id,
                    constructionId = sentence.constructionId,
                    lexicalIds = lexicalIds,
                    clauseRoles = publicParts.map { it.role },
                    canonicalSentence = realize(publicParts),
                ),
            source =
                ExerciseSource(
                    sentence = sentence,
                    answerKey = sourceAndPrompt.answerKey,
                    blockRoles = sourceAndPrompt.blockRoles,
                ),
        )
    }

    private fun presentExercise(
        sentence: Sentence,
        parts: List<Constituent>,
        seed: Int,
        candidateIndex: Int,
    ): ExerciseParts {
        val alternatives = listOf(sentence.verb.lemma, sentence.verb.present)
        val choices = if (mixed(seed, candidateIndex) and 1L == 0L) alternatives else alternatives.reversed()
        val verbIndex = parts.indexOfFirst { it.role == "FINITE_VERB" }
        return ExerciseParts(
            instruction = "Choose the finite present form to complete the statement.",
            prompt =
                ExercisePrompt(
                    before = displayFragment(parts.take(verbIndex)),
                    after = displayFragment(parts.drop(verbIndex + 1), final = true),
                    choices = choices,
                ),
            answerKey = AnswerKey.PresentForm(sentence.verb.lemma, sentence.verb.present),
        )
    }

    private fun articleExercise(
        sentence: Sentence,
        parts: List<Constituent>,
    ): ExerciseParts {
        val complement = requireNotNull(sentence.complement)
        val article = complement.text.substringBefore(' ')
        val noun = complement.text.substringAfter(' ')
        return ExerciseParts(
            instruction = "Type the unstressed indefinite article that completes the intended meaning.",
            prompt =
                ExercisePrompt(
                    before = displayFragment(parts.dropLast(1)),
                    after = " $noun.",
                ),
            answerKey = AnswerKey.Article(article, noun),
        )
    }

    private fun orderExercise(
        parts: List<Constituent>,
        seed: Int,
        candidateIndex: Int,
    ): ExerciseParts {
        val candidate = shuffled(parts, mixed(seed xor ORDER_SHUFFLE_XOR, candidateIndex))
        // An ordering prompt must require a change, including the two-block sleeping pattern.
        val shuffled = if (candidate == parts) candidate.drop(1) + candidate.first() else candidate
        val blocks = shuffled.mapIndexed { index, part -> OrderBlock("b$index", displayLexeme(part)) }
        return ExerciseParts(
            instruction = "Arrange the blocks to make a statement beginning with the subject.",
            prompt = ExercisePrompt(before = "", after = "", blocks = blocks),
            answerKey = AnswerKey.SubjectFirstOrder(parts.map { it.role }),
            blockRoles = blocks.zip(shuffled).associate { (block, part) -> block.id to part.role },
        )
    }

    private fun publicParts(sentence: Sentence): List<Constituent> =
        buildList {
            add(sentence.subject)
            add(Constituent(listOf(sentence.verb.lexicalId), sentence.verb.present, "FINITE_VERB"))
            sentence.complement?.let(::add)
        }

    private fun realize(parts: List<Constituent>): String =
        parts.mapIndexed { index, part ->
            val text = displayLexeme(part)
            if (index == 0 && text != "I") text.replaceFirstChar { it.titlecase() } else text
        }.joinToString(" ") + "."

    private fun displayFragment(
        parts: List<Constituent>,
        final: Boolean = false,
    ): String {
        if (parts.isEmpty()) return if (final) "." else ""
        val value = parts.map(::displayLexeme).joinToString(" ")
        return if (final) " $value." else value.replaceFirstChar { it.titlecase() } + " "
    }

    private fun displayLexeme(part: Constituent): String =
        if (part.text == "I") "I" else part.text.lowercase()

    private fun contextFor(sentence: Sentence): String {
        val subjectId = sentence.subject.lexicalIds.single()
        val pronoun = (PilotContent.humanPronouns + PilotContent.identifyingPronoun).single { it.id == subjectId }
        val nounId = sentence.complement?.lexicalIds?.lastOrNull()
        val noun = PilotContent.nouns.singleOrNull { it.id == nounId }
        return if (sentence.constructionId == "C003") {
            "Identify an item described as ${noun?.gloss ?: "the named object"}. Det means ‘it’ or ‘that’ here."
        } else {
            "The subject ${displayLexeme(sentence.subject)} means ‘${pronoun.gloss}’ in this statement" +
                (noun?.let { ", and the intended noun is ‘${it.gloss}’" } ?: "") + "."
        }
    }

    private fun introductionFor(formatId: String, sentence: Sentence): List<IntroductionSection> =
        buildList {
            val verb = sentence.verb
            add(
                IntroductionSection(
                    "Meet the words",
                    "${verb.lemma} means ‘to ${verb.gloss}’. Its finite present form is ${verb.present}; " +
                        "use that same present form with every subject pronoun.",
                ),
            )
            sentence.complement?.let { phrase ->
                val noun = PilotContent.nouns.single { it.id == phrase.lexicalIds.last() }
                add(
                    IntroductionSection(
                        "Learn the noun with its article",
                        "${phrase.text} means ‘a${if (noun.gloss.first() in "aeiou") "n" else ""} ${noun.gloss}’. " +
                            "${noun.text} is ${noun.gender.name.lowercase()} gender, so learn it together with ${noun.gender.article}.",
                    ),
                )
            }
            add(targetIntroduction(formatId))
        }

    private fun targetIntroduction(formatId: String): IntroductionSection =
        when (formatId) {
            "E001" ->
                    IntroductionSection(
                        "Present and infinitive",
                        "A finite present verb can complete a statement. The infinitive names the verb form.",
                    )
            "E002" ->
                    IntroductionSection(
                        "En or et",
                        "Danish singular count nouns are learned with their common-gender or neuter article.",
                    )
            else ->
                    IntroductionSection(
                        "Subject-first order",
                        "Example: Jeg (subject, ‘I’) + sover (finite verb, ‘sleep’) gives Jeg sover. " +
                            "Put the subject before the finite verb and keep any noun phrase together.",
                    )
        }

    private fun hintFor(formatId: String): String =
        when (formatId) {
            "E001" -> "Look for the introduced form that can be the finite verb in a present statement."
            "E002" -> "Recall the noun together with its stored gender; do not infer the article from its English meaning."
            else -> "Find the subject block first, then place the finite verb after it."
        }

    private fun Constituent?.orEmptyLexicalIds(): List<String> = this?.lexicalIds.orEmpty()

    private fun permutedIndex(
        seed: Int,
        index: Int,
        size: Int,
    ): Int {
        val start = Math.floorMod(mixed(seed, 0), size.toLong()).toInt()
        var step = Math.floorMod(mixed(seed, 1), (size - 1).toLong()).toInt() + 1
        while (greatestCommonDivisor(step, size) != 1) step = (step % (size - 1)) + 1
        return (start + (index % size).toLong() * step).mod(size.toLong()).toInt()
    }

    private fun greatestCommonDivisor(
        first: Int,
        second: Int,
    ): Int {
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

    private fun <T> shuffled(
        input: List<T>,
        entropy: Long,
    ): List<T> {
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

    private data class ExerciseParts(
        val instruction: String,
        val prompt: ExercisePrompt,
        val answerKey: AnswerKey,
        val blockRoles: Map<String, String> = emptyMap(),
    )

    private companion object {
        const val MAX_INDEX = 1_000_000
        const val ORDER_SHUFFLE_XOR = 0x5a5a5a5a
        const val GOLDEN_GAMMA = -7046029254386353131L
        const val MIX_MULTIPLIER_ONE = -4658895280553007687L
        const val MIX_MULTIPLIER_TWO = -7723592293110705685L
        const val MIX_SHIFT_ONE = 30
        const val MIX_SHIFT_TWO = 27
        const val MIX_SHIFT_THREE = 31
        const val LONG_HALF_BITS = 32
    }
}
