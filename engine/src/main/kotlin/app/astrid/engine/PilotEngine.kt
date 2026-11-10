package app.astrid.engine

class PilotEngine {
    fun catalog(): Catalog =
        Catalog(
            skills = PilotContent.currentSnapshot.skills,
            formats = PilotContent.currentSnapshot.formats,
            sentenceCount = PilotContent.currentSnapshot.sentences.size,
            contentRevision = PilotContent.CURRENT_CONTENT_REVISION,
            generatorRevision = PilotContent.CURRENT_GENERATOR_REVISION,
        )

    fun supportedRevisionPairs(): List<RevisionPair> = SUPPORTED_REVISION_PAIRS

    fun evaluationRevision(
        contentRevision: String,
        generatorRevision: String,
    ): String? =
        when (RevisionPair(contentRevision, generatorRevision)) {
            LEGACY_REVISION_PAIR -> PilotContent.LEGACY_EVALUATION_REVISION
            PILOT_TWO_REVISION_PAIR -> PilotContent.RETAINED_EVALUATION_REVISION
            PILOT_THREE_REVISION_PAIR -> PilotContent.RETAINED_THREE_EVALUATION_REVISION
            PILOT_FOUR_REVISION_PAIR -> PilotContent.RETAINED_FOUR_EVALUATION_REVISION
            PILOT_FIVE_REVISION_PAIR -> PilotContent.RETAINED_FIVE_EVALUATION_REVISION
            PILOT_SIX_REVISION_PAIR -> PilotContent.RETAINED_SIX_EVALUATION_REVISION
            PILOT_SEVEN_REVISION_PAIR -> PilotContent.RETAINED_SEVEN_EVALUATION_REVISION
            PILOT_EIGHT_REVISION_PAIR -> PilotContent.RETAINED_EIGHT_EVALUATION_REVISION
            CURRENT_REVISION_PAIR -> PilotContent.CURRENT_EVALUATION_REVISION
            else -> null
        }

    fun generate(request: GenerationRequest): GeneratedExercise {
        validate(request)
        return when {
            request.formatId in INANIMATE_REFERENCE_FORMAT_IDS -> InanimateReferenceExerciseGenerator.create(request)
            request.formatId in MODULE_ONE_FORMAT_IDS -> ModuleOneExerciseGenerator.create(request)
            request.formatId == MULTIPLE_GAP_FORMAT_ID -> MultipleGapExerciseGenerator.create(request)
            request.formatId == MATCHING_FORMAT_ID -> MatchingExerciseGenerator.create(request)
            request.formatId == WORD_CHOICE_FORMAT_ID -> TileExerciseGenerator.createWordChoiceExercise(request)
            request.formatId in TILE_FORMAT_IDS -> TileExerciseGenerator.createTileExercise(request)
            request.formatId == WRITING_FORMAT_ID -> WritingReadingGenerator.createWritingExercise(request)
            request.formatId == READING_FORMAT_ID -> WritingReadingGenerator.createReadingExercise(request)
            request.formatId in LEXICAL_FORMAT_IDS -> createLexicalExercise(request)
            else -> createSentenceExercise(request)
        }
    }

    private fun createSentenceExercise(request: GenerationRequest): GeneratedExercise {
        val candidates =
            when (request.formatId) {
                "E001", "E003", "E004", "E005" -> PilotContent.sentences
                "E002", "E006" -> PilotContent.sentences.filter { it.complement != null }
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

    fun isSuppliedChoice(exercise: GeneratedExercise, rawText: String): Boolean {
        val normalized = normalizeText(rawText)
        return when (val key = exercise.source.answerKey) {
            is AnswerKey.SuppliedChoice -> normalized in key.suppliedChoices
            is AnswerKey.ModuleOneForm -> key.selection && normalized in (key.knownAlternatives + key.expected)
            else -> false
        }
    }

    private fun validate(request: GenerationRequest) {
        require(request.seed >= 0) { "seed must be non-negative" }
        require(request.index in 0..MAX_INDEX) { "index must be between 0 and $MAX_INDEX" }
        val pair = RevisionPair(request.contentRevision, request.generatorRevision)
        require(pair in SUPPORTED_REVISION_PAIRS) {
            "Unsupported revision pair: ${request.contentRevision}/${request.generatorRevision}"
        }
        val expectedTarget =
            mapOf(
                "E001" to "G004",
                "E002" to "G006",
                "E003" to "G005",
                "E004" to "G004",
                "E005" to "G004",
                "E006" to "G006",
                WRITING_FORMAT_ID to "G005",
                "E012" to "G005",
                "E013" to "G004",
                MULTIPLE_GAP_FORMAT_ID to "G004",
                "E016" to "G003",
                "E017" to "G003",
                "E018" to "G004",
                "E019" to "G004",
                "E020" to "G038",
                "E021" to "G038",
            )[request.formatId]
        require(expectedTarget != null || request.formatId in LEXICAL_FORMAT_IDS || request.formatId == READING_FORMAT_ID) {
            "Unsupported format: ${request.formatId}"
        }
        val supportedFormats = supportedFormats(pair)
        require(request.formatId in supportedFormats) {
            "Format ${request.formatId} is unsupported by ${request.contentRevision}/${request.generatorRevision}"
        }
        if (request.formatId in LEXICAL_FORMAT_IDS || request.formatId == READING_FORMAT_ID) {
            require(request.targetSkillId == null) { "Format ${request.formatId} does not accept a grammar target" }
        } else {
            require(request.targetSkillId == expectedTarget) {
                "Format ${request.formatId} requires target $expectedTarget"
            }
        }
    }

    private fun supportedFormats(pair: RevisionPair): Set<String> =
        when (pair) {
            LEGACY_REVISION_PAIR -> LEGACY_FORMAT_IDS
            PILOT_TWO_REVISION_PAIR -> PILOT_TWO_FORMAT_IDS
            PILOT_THREE_REVISION_PAIR -> PILOT_THREE_FORMAT_IDS
            PILOT_FOUR_REVISION_PAIR -> PILOT_FOUR_FORMAT_IDS
            PILOT_FIVE_REVISION_PAIR -> PILOT_FIVE_FORMAT_IDS
            PILOT_SIX_REVISION_PAIR -> PILOT_SIX_FORMAT_IDS
            PILOT_SEVEN_REVISION_PAIR -> PILOT_SEVEN_FORMAT_IDS
            PILOT_EIGHT_REVISION_PAIR -> PILOT_EIGHT_FORMAT_IDS
            CURRENT_REVISION_PAIR -> CURRENT_FORMAT_IDS
            else -> emptySet()
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
                "E004" -> supplyPresentExercise(sentence, publicParts)
                "E005" -> repairPresentExercise(sentence, publicParts)
                "E006" -> repairArticleExercise(sentence, publicParts)
                else -> error("Unreachable format ${request.formatId}")
            }
        val lexicalIds =
            sentence.subject.lexicalIds + sentence.verb.lexicalId + sentence.complement.orEmptyLexicalIds()
        val targetSkillId = requireNotNull(request.targetSkillId)
        return GeneratedExercise(
            id = exerciseId(request, sentence),
            formatId = request.formatId,
            targetSkillId = request.targetSkillId,
            lexicalTarget = null,
            supportSkillIds =
                (listOf("G001", "G002", "G003", "G004", "G005") +
                    if (sentence.complement != null) listOf("G006") else emptyList()) - targetSkillId,
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
                SentenceSource(
                    sentence = sentence,
                    answerKey = sourceAndPrompt.answerKey,
                    blockRoles = sourceAndPrompt.blockRoles,
                ),
        )
    }

    private fun createLexicalExercise(request: GenerationRequest): GeneratedExercise {
        val targetIndex = permutedIndex(request.seed, request.index, PilotContent.vocabularySenses.size)
        val sense = PilotContent.vocabularySenses[targetIndex]
        val task = requireNotNull(PilotContent.vocabularyFormats.single { it.id == request.formatId }.lexicalTask)
        val recognition = request.formatId == "E007"
        val choices = if (recognition) {
            shuffled(
                PilotContent.vocabularySenses.map { it.gloss },
                mixed(request.seed xor LEXICAL_CHOICE_SHUFFLE_XOR, request.index),
            )
        } else {
            emptyList()
        }
        val answerKey: AnswerKey = if (recognition) {
            AnswerKey.MeaningChoice(sense.gloss, choices.toSet())
        } else {
            AnswerKey.Headword(sense)
        }
        val lexicalTarget = LexicalTarget(sense.entryId, sense.senseId, task.facet, task.direction)
        return GeneratedExercise(
            id =
                "${request.formatId}-${request.contentRevision}-${request.generatorRevision}-" +
                    "${sense.senseId}-${request.seed}-${request.index}",
            formatId = request.formatId,
            targetSkillId = null,
            lexicalTarget = lexicalTarget,
            supportSkillIds = emptyList(),
            presentation = lexicalPresentation(sense, recognition, choices),
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
            source = LexicalSource(sense, answerKey),
        )
    }

    private fun lexicalPresentation(
        sense: LexicalSense,
        recognition: Boolean,
        choices: List<String>,
    ) = ExercisePresentation(
        instruction = if (recognition) {
            "Choose the English meaning of the Danish headword."
        } else {
            "Type the introduced Danish singular headword for this meaning, without its article."
        },
        context = "Use the introduced meaning of this word.",
        prompt = ExercisePrompt(
            before = "",
            after = "",
            choices = choices,
            punctuation = "",
            inputLabel = if (recognition) null else "Danish singular headword",
            cue = if (recognition) sense.headword else sense.gloss,
            cueLanguage = if (recognition) "da" else "en",
            answerLanguage = if (recognition) "en" else "da",
        ),
        introduction = lexicalIntroduction(sense),
        hint = if (recognition) {
            "Choose one of the supplied English meanings."
        } else {
            "Enter the introduced singular headword as one word, without en or et."
        },
    )

    private fun lexicalIntroduction(sense: LexicalSense): List<IntroductionSection> =
        listOf(
            IntroductionSection(
                "Learn the word",
                "${sense.article} ${sense.headword} means ‘${sense.gloss}’. Learn the noun with its article.",
            ),
            IntroductionSection(
                "Practice the introduced meaning",
                if (sense.entryId == "L018") {
                    "Here, letter means a written message rather than an alphabet character."
                } else {
                    "Use this introduced noun and meaning in the exercise."
                },
            ),
        )

    private fun exerciseId(
        request: GenerationRequest,
        sentence: Sentence,
    ): String =
        if (RevisionPair(request.contentRevision, request.generatorRevision) == LEGACY_REVISION_PAIR) {
            "${request.formatId}-${sentence.id}-${request.seed}-${request.index}"
        } else {
            "${request.formatId}-${request.contentRevision}-${request.generatorRevision}-${sentence.id}-${request.seed}-${request.index}"
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

    private fun supplyPresentExercise(
        sentence: Sentence,
        parts: List<Constituent>,
    ): ExerciseParts {
        val verbIndex = parts.indexOfFirst { it.role == "FINITE_VERB" }
        return ExerciseParts(
            instruction = "Type the finite present form of ${sentence.verb.lemma} (‘to ${sentence.verb.gloss}’).",
            prompt =
                ExercisePrompt(
                    before = displayFragment(parts.take(verbIndex)),
                    after = displayFragment(parts.drop(verbIndex + 1), final = true),
                    inputLabel = "Present form",
                ),
            answerKey = AnswerKey.PresentForm(sentence.verb.lemma, sentence.verb.present),
        )
    }

    private fun repairPresentExercise(
        sentence: Sentence,
        parts: List<Constituent>,
    ): ExerciseParts {
        val verbIndex = parts.indexOfFirst { it.role == "FINITE_VERB" }
        return ExerciseParts(
            instruction =
                "The present statement contains one verb-form error. Keep the verb ${sentence.verb.lemma} " +
                    "(‘to ${sentence.verb.gloss}’) and type its replacement.",
            prompt =
                ExercisePrompt(
                    before = displayFragment(parts.take(verbIndex)),
                    after = displayFragment(parts.drop(verbIndex + 1), final = true),
                    inputLabel = "Replacement verb",
                    replacementWord = sentence.verb.lemma,
                ),
            answerKey = AnswerKey.PresentForm(sentence.verb.lemma, sentence.verb.present),
        )
    }

    private fun repairArticleExercise(
        sentence: Sentence,
        parts: List<Constituent>,
    ): ExerciseParts {
        val complement = requireNotNull(sentence.complement)
        val expectedArticle = complement.text.substringBefore(' ')
        val noun = complement.text.substringAfter(' ')
        val wrongArticle = if (expectedArticle == "en") "et" else "en"
        return ExerciseParts(
            instruction =
                "The statement contains one indefinite-article error. Type the replacement unstressed article.",
            prompt =
                ExercisePrompt(
                    before = displayFragment(parts.dropLast(1)),
                    after = " $noun.",
                    inputLabel = "Replacement article",
                    replacementWord = wrongArticle,
                ),
            answerKey = AnswerKey.Article(expectedArticle, noun),
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
        if (formatId in NEW_FORMAT_IDS) newIntroductionFor(formatId, sentence) else
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

    private fun newIntroductionFor(
        formatId: String,
        sentence: Sentence,
    ): List<IntroductionSection> =
        buildList {
            add(
                IntroductionSection(
                    "Meet the verb",
                    "${sentence.verb.lemma} means ‘to ${sentence.verb.gloss}’. " +
                        "Its finite present form is ${sentence.verb.present}.",
                ),
            )
            sentence.complement?.let { phrase ->
                val noun = PilotContent.nouns.single { it.id == phrase.lexicalIds.last() }
                add(
                    IntroductionSection(
                        "Learn the noun with its article",
                        "${phrase.text} means ‘a${if (noun.gloss.first() in "aeiou") "n" else ""} ${noun.gloss}’.",
                    ),
                )
            }
            add(
                when (formatId) {
                "E004" ->
                    IntroductionSection(
                        "Supply a finite verb",
                        "Use the named verb in the finite present. Danish present forms stay the same across subject pronouns.",
                    )
                "E005" ->
                    IntroductionSection(
                        "Repair one verb form",
                        "Replace only the marked verb. Keep the named verb and make it finite present.",
                    )
                else ->
                    IntroductionSection(
                        "Repair one article",
                        "Replace only the marked unstressed article. Recall whether the named noun is learned with en or et.",
                    )
                },
            )
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
            "E003" -> "Find the subject block first, then place the finite verb after it."
            "E004" -> "Use the finite present form of the named verb; it does not change with the subject."
            "E005" -> "Keep the same verb and replace its infinitive with the finite present form."
            else -> "Recall the noun together with its stored gender, then replace only the article."
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
        val LEGACY_REVISION_PAIR = RevisionPair(PilotContent.CONTENT_REVISION, PilotContent.GENERATOR_REVISION)
        val PILOT_TWO_REVISION_PAIR =
            RevisionPair(PilotContent.RETAINED_CONTENT_REVISION, PilotContent.RETAINED_GENERATOR_REVISION)
        val PILOT_THREE_REVISION_PAIR =
            RevisionPair(PilotContent.RETAINED_THREE_CONTENT_REVISION, PilotContent.RETAINED_THREE_GENERATOR_REVISION)
        val PILOT_FOUR_REVISION_PAIR =
            RevisionPair(PilotContent.RETAINED_FOUR_CONTENT_REVISION, PilotContent.RETAINED_FOUR_GENERATOR_REVISION)
        val PILOT_FIVE_REVISION_PAIR =
            RevisionPair(PilotContent.RETAINED_FIVE_CONTENT_REVISION, PilotContent.RETAINED_FIVE_GENERATOR_REVISION)
        val PILOT_SIX_REVISION_PAIR =
            RevisionPair(PilotContent.RETAINED_SIX_CONTENT_REVISION, PilotContent.RETAINED_SIX_GENERATOR_REVISION)
        val PILOT_SEVEN_REVISION_PAIR =
            RevisionPair(PilotContent.RETAINED_SEVEN_CONTENT_REVISION, PilotContent.RETAINED_SEVEN_GENERATOR_REVISION)
        val PILOT_EIGHT_REVISION_PAIR =
            RevisionPair(PilotContent.RETAINED_EIGHT_CONTENT_REVISION, PilotContent.RETAINED_EIGHT_GENERATOR_REVISION)
        val CURRENT_REVISION_PAIR =
            RevisionPair(PilotContent.CURRENT_CONTENT_REVISION, PilotContent.CURRENT_GENERATOR_REVISION)
        val SUPPORTED_REVISION_PAIRS =
            listOf(
                LEGACY_REVISION_PAIR,
                PILOT_TWO_REVISION_PAIR,
                PILOT_THREE_REVISION_PAIR,
                PILOT_FOUR_REVISION_PAIR,
                PILOT_FIVE_REVISION_PAIR,
                PILOT_SIX_REVISION_PAIR,
                PILOT_SEVEN_REVISION_PAIR,
                PILOT_EIGHT_REVISION_PAIR,
                CURRENT_REVISION_PAIR,
            )
        val LEGACY_FORMAT_IDS = setOf("E001", "E002", "E003")
        val PILOT_TWO_FORMAT_IDS = LEGACY_FORMAT_IDS + setOf("E004", "E005", "E006")
        val RETAINED_LEXICAL_FORMAT_IDS = setOf("E007", "E008")
        val LEXICAL_FORMAT_IDS = RETAINED_LEXICAL_FORMAT_IDS + setOf(WORD_CHOICE_FORMAT_ID, MATCHING_FORMAT_ID)
        val PILOT_THREE_FORMAT_IDS = PILOT_TWO_FORMAT_IDS + RETAINED_LEXICAL_FORMAT_IDS
        val PILOT_FOUR_FORMAT_IDS = PILOT_TWO_FORMAT_IDS + setOf("E007", "E008", "E009", "E010")
        val PILOT_FIVE_FORMAT_IDS = PILOT_FOUR_FORMAT_IDS + setOf("E011", "E012", "E013")
        val PILOT_SIX_FORMAT_IDS = PILOT_FIVE_FORMAT_IDS + MATCHING_FORMAT_ID
        val PILOT_SEVEN_FORMAT_IDS = PILOT_SIX_FORMAT_IDS + MULTIPLE_GAP_FORMAT_ID
        val MODULE_ONE_FORMAT_IDS = setOf("E016", "E017", "E018", "E019")
        val PILOT_EIGHT_FORMAT_IDS = PILOT_SEVEN_FORMAT_IDS + MODULE_ONE_FORMAT_IDS
        val INANIMATE_REFERENCE_FORMAT_IDS = setOf("E020", "E021")
        val CURRENT_FORMAT_IDS = PILOT_EIGHT_FORMAT_IDS + INANIMATE_REFERENCE_FORMAT_IDS
        val TILE_FORMAT_IDS = setOf("E012", "E013")
        val NEW_FORMAT_IDS = setOf("E004", "E005", "E006")
        const val MAX_INDEX = 1_000_000
        const val ORDER_SHUFFLE_XOR = 0x5a5a5a5a
        const val LEXICAL_CHOICE_SHUFFLE_XOR = 0x13579bdf
        const val WRITING_FORMAT_ID = "E009"
        const val READING_FORMAT_ID = "E010"
        const val WORD_CHOICE_FORMAT_ID = "E011"
        const val MATCHING_FORMAT_ID = "E014"
        const val MULTIPLE_GAP_FORMAT_ID = "E015"
        const val GOLDEN_GAMMA = -7046029254386353131L
        const val MIX_MULTIPLIER_ONE = -4658895280553007687L
        const val MIX_MULTIPLIER_TWO = -7723592293110705685L
        const val MIX_SHIFT_ONE = 30
        const val MIX_SHIFT_TWO = 27
        const val MIX_SHIFT_THREE = 31
        const val LONG_HALF_BITS = 32
    }
}
