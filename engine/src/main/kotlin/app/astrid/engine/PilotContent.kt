package app.astrid.engine

internal data class PronounEntry(
    val id: String,
    val text: String,
    val gloss: String,
)

internal enum class NounGender(val articleId: String, val article: String) {
    COMMON("L022", "en"),
    NEUTER("L023", "et"),
}

internal data class NounEntry(
    val id: String,
    val text: String,
    val gloss: String,
    val gender: NounGender,
)

internal data class FixedWordEntry(
    val id: String,
    val text: String,
    val gloss: String,
)

internal object PilotContent {
    const val CONTENT_REVISION = "pilot-1"
    const val GENERATOR_REVISION = "generator-1"
    const val RETAINED_CONTENT_REVISION = "pilot-2"
    const val RETAINED_GENERATOR_REVISION = "generator-2"
    const val RETAINED_THREE_CONTENT_REVISION = "pilot-3"
    const val RETAINED_THREE_GENERATOR_REVISION = "generator-3"
    const val RETAINED_FOUR_CONTENT_REVISION = "pilot-4"
    const val RETAINED_FOUR_GENERATOR_REVISION = "generator-4"
    const val RETAINED_FIVE_CONTENT_REVISION = "pilot-5"
    const val RETAINED_FIVE_GENERATOR_REVISION = "generator-5"
    const val RETAINED_SIX_CONTENT_REVISION = "pilot-6"
    const val RETAINED_SIX_GENERATOR_REVISION = "generator-6"
    const val RETAINED_SEVEN_CONTENT_REVISION = "pilot-7"
    const val RETAINED_SEVEN_GENERATOR_REVISION = "generator-7"
    const val RETAINED_EIGHT_CONTENT_REVISION = "pilot-8"
    const val RETAINED_EIGHT_GENERATOR_REVISION = "generator-8"
    const val RETAINED_NINE_CONTENT_REVISION = "pilot-9"
    const val RETAINED_NINE_GENERATOR_REVISION = "generator-9"
    const val RETAINED_TEN_CONTENT_REVISION = "pilot-10"
    const val RETAINED_TEN_GENERATOR_REVISION = "generator-10"
    const val RETAINED_ELEVEN_CONTENT_REVISION = "pilot-11"
    const val RETAINED_ELEVEN_GENERATOR_REVISION = "generator-11"
    const val RETAINED_TWELVE_CONTENT_REVISION = "pilot-12"
    const val RETAINED_TWELVE_GENERATOR_REVISION = "generator-12"
    const val RETAINED_THIRTEEN_CONTENT_REVISION = "pilot-13"
    const val RETAINED_THIRTEEN_GENERATOR_REVISION = "generator-13"
    const val RETAINED_FOURTEEN_CONTENT_REVISION = "pilot-14"
    const val RETAINED_FOURTEEN_GENERATOR_REVISION = "generator-14"
    const val CURRENT_CONTENT_REVISION = "pilot-15"
    const val CURRENT_GENERATOR_REVISION = "generator-15"
    const val LEGACY_EVALUATION_REVISION = "evaluation-1"
    const val RETAINED_EVALUATION_REVISION = "evaluation-2"
    const val RETAINED_THREE_EVALUATION_REVISION = "evaluation-3"
    const val RETAINED_FOUR_EVALUATION_REVISION = "evaluation-4"
    const val RETAINED_FIVE_EVALUATION_REVISION = "evaluation-5"
    const val RETAINED_SIX_EVALUATION_REVISION = "evaluation-6"
    const val RETAINED_SEVEN_EVALUATION_REVISION = "evaluation-7"
    const val RETAINED_EIGHT_EVALUATION_REVISION = "evaluation-8"
    const val RETAINED_NINE_EVALUATION_REVISION = "evaluation-9"
    const val RETAINED_TEN_EVALUATION_REVISION = "evaluation-10"
    const val RETAINED_ELEVEN_EVALUATION_REVISION = "evaluation-11"
    const val RETAINED_TWELVE_EVALUATION_REVISION = "evaluation-12"
    const val RETAINED_THIRTEEN_EVALUATION_REVISION = "evaluation-13"
    const val RETAINED_FOURTEEN_EVALUATION_REVISION = "evaluation-14"
    const val CURRENT_EVALUATION_REVISION = "evaluation-15"

    val humanPronouns =
        listOf(
            PronounEntry("L001", "jeg", "I"),
            PronounEntry("L002", "du", "you (one person)"),
            PronounEntry("L003", "han", "he"),
            PronounEntry("L004", "hun", "she"),
            PronounEntry("L005", "vi", "we"),
            PronounEntry("L006", "I", "you (more than one person)"),
            PronounEntry("L007", "de", "they"),
        )

    val identifyingPronoun = PronounEntry("L008", "det", "it or that")
    val inanimateReferencePronouns = listOf(PronounEntry("L026", "den", "it for one common-gender thing"))

    val have = Verb("L009", "have", "har", "have")
    val be = Verb("L010", "være", "er", "be")
    val read = Verb("L011", "læse", "læser", "read")
    val eat = Verb("L012", "spise", "spiser", "eat")
    val sleep = Verb("L013", "sove", "sover", "sleep")
    val verbs = listOf(have, be, read, eat, sleep)
    val beCalled = Verb("L028", "hedde", "hedder", "be called")
    val live = Verb("L029", "bo", "bor", "live or reside")
    val speak = Verb("L030", "tale", "taler", "speak")
    val personalIntroductionVerbs = listOf(beCalled, live, speak)
    val personalIntroductionFixedWords = listOf(
        FixedWordEntry("L031", "dansk", "Danish language"),
        FixedWordEntry("L032", "i", "in at a city location"),
    )
    val circumstanceFixedWords = listOf(
        FixedWordEntry("L037", "i dag", "today on the current calendar day"),
        FixedWordEntry("L038", "nu", "now at the stated time"),
    )

    val nouns =
        listOf(
            NounEntry("L014", "bog", "book", NounGender.COMMON),
            NounEntry("L015", "avis", "newspaper", NounGender.COMMON),
            NounEntry("L016", "bil", "car", NounGender.COMMON),
            NounEntry("L017", "hund", "dog", NounGender.COMMON),
            NounEntry("L018", "brev", "letter", NounGender.NEUTER),
            NounEntry("L019", "hus", "house", NounGender.NEUTER),
            NounEntry("L020", "bord", "table", NounGender.NEUTER),
            NounEntry("L021", "æble", "apple", NounGender.NEUTER),
        )

    val magazine = NounEntry("L024", "magasin", "magazine (printed publication)", NounGender.NEUTER)
    val currentNouns = nouns + magazine
    val teacher = NounEntry("L025", "lærer", "teacher", NounGender.COMMON)
    val moduleOneNouns = currentNouns + teacher

    val vocabularySenses =
        listOf(
            LexicalSense("L014", "L014-S01", "bog", "book", "en"),
            LexicalSense("L015", "L015-S01", "avis", "newspaper", "en"),
            LexicalSense("L018", "L018-S01", "brev", "letter (written message)", "et"),
        )

    val wordChoiceSenses =
        vocabularySenses + LexicalSense("L024", "L024-S01", "magasin", "magazine (printed publication)", "et")
    val teacherSense = LexicalSense("L025", "L025-S01", "lærer", "teacher", "en")

    val sentences: List<Sentence> = buildSentences()

    val skills =
        listOf(
            Skill(
                "G001",
                "Orthography and word units",
                "Danish has the distinct letters æ, ø, and å, as well as meaningful capitalization and word boundaries.",
                emptyList(),
            ),
            Skill(
                "G002",
                "Word classes and sentence roles",
                "Words have classes, while constituents have roles such as subject, finite verb, and object.",
                listOf("G001"),
            ),
            Skill(
                "G003",
                "Subject pronouns",
                "Use the introduced human subject pronouns and the fixed identifying pattern Det er ….",
                listOf("G002"),
            ),
            Skill(
                "G004",
                "Finite present verbs",
                "A statement needs a finite present form; Danish present verbs do not change with person or number.",
                listOf("G002"),
            ),
            Skill(
                "G005",
                "Simple subject-first statements",
                "Build the supported statements with the subject before the finite verb.",
                listOf("G003", "G004"),
            ),
            Skill(
                "G006",
                "Noun gender and articles",
                "Learn each singular count noun with its stored indefinite article, en or et.",
                listOf("G001", "G002"),
            ),
        )

    val formats =
        listOf(
            ExerciseFormat("E001", "Choose the present form", "G004"),
            ExerciseFormat("E002", "Supply the indefinite article", "G006"),
            ExerciseFormat("E003", "Arrange a subject-first statement", "G005"),
        )

    val currentFormats =
        formats +
            listOf(
                ExerciseFormat("E004", "Supply the present form", "G004"),
                ExerciseFormat("E005", "Repair the verb form", "G004"),
                ExerciseFormat("E006", "Repair the indefinite article", "G006"),
            )

    val vocabularyFormats =
        currentFormats +
            listOf(
                ExerciseFormat("E007", "Recognize the meaning", null, LexicalTask("meaning", "da-en")),
                ExerciseFormat("E008", "Recall the Danish headword", null, LexicalTask("singular-headword", "en-da")),
            )

    val writingReadingFormats =
        vocabularyFormats +
            listOf(
                ExerciseFormat("E009", "Write a Danish sentence", "G005"),
                ExerciseFormat(
                    "E010",
                    "Read a short text",
                    null,
                    readingTask = ReadingTask("participant-action-complement"),
                ),
            )

    val tileFormats =
        writingReadingFormats +
            listOf(
                ExerciseFormat("E011", "Choose the Danish word", null, LexicalTask("meaning", "en-da")),
                ExerciseFormat("E012", "Build a Danish sentence", "G005"),
                ExerciseFormat("E013", "Choose the present form with word tiles", "G004"),
            )

    val matchingFormats =
        tileFormats + ExerciseFormat("E014", "Match English and Danish words", null, LexicalTask("meaning", "en-da"))

    val gapFormats = matchingFormats + ExerciseFormat("E015", "Choose a present verb and article", "G004")

    val moduleOneFormats =
        gapFormats +
            listOf(
                ExerciseFormat("E016", "Choose a Danish subject word", "G003"),
                ExerciseFormat("E017", "Recall a Danish subject word", "G003"),
                ExerciseFormat("E018", "Choose the present form of være", "G004"),
                ExerciseFormat("E019", "Recall the present form of være", "G004"),
            )

    val inanimateReferenceFormats =
        moduleOneFormats +
            listOf(
                ExerciseFormat("E020", "Choose a reference word for one thing", "G038"),
                ExerciseFormat("E021", "Recall a reference word for one thing", "G038"),
            )

    val presentOrderFormats =
        inanimateReferenceFormats +
            listOf(
                ExerciseFormat("E022", "Choose a named present form", "G004"),
                ExerciseFormat("E023", "Recall a named present form", "G004"),
                ExerciseFormat("E024", "Arrange supplied constituents", "G005"),
                ExerciseFormat("E025", "Arrange supplied words", "G005"),
            )

    val abilityFormats =
        presentOrderFormats +
            listOf(
                ExerciseFormat("E026", "Choose the bare infinitive after kan", "G030"),
                ExerciseFormat("E027", "Recall the bare infinitive after kan", "G030"),
            )

    val modalRoundFormats =
        abilityFormats + ExerciseFormat("E028", "Interpret a modal in context", "G030")

    val circumstanceFormats =
        modalRoundFormats + ExerciseFormat("E029", "Add a supplied final circumstance", "G016")

    val snapshot =
        PilotContentSnapshot(
            contentRevision = CONTENT_REVISION,
            generatorRevision = GENERATOR_REVISION,
            skills = skills,
            formats = formats,
            humanPronouns = humanPronouns,
            identifyingPronoun = identifyingPronoun,
            verbs = verbs,
            nouns = nouns,
            sentences = sentences,
            lexicalReview = PilotReviewMetadata.records(humanPronouns, identifyingPronoun, verbs, nouns),
            vocabularySenses = emptyList(),
        )

    val pilotTwoSnapshot =
        PilotContentSnapshot(
            contentRevision = RETAINED_CONTENT_REVISION,
            generatorRevision = RETAINED_GENERATOR_REVISION,
            skills = skills,
            formats = currentFormats,
            humanPronouns = humanPronouns,
            identifyingPronoun = identifyingPronoun,
            verbs = verbs,
            nouns = nouns,
            sentences = sentences,
            lexicalReview = PilotReviewMetadata.records(humanPronouns, identifyingPronoun, verbs, nouns),
            vocabularySenses = emptyList(),
        )

    val pilotThreeSnapshot =
        PilotContentSnapshot(
            contentRevision = RETAINED_THREE_CONTENT_REVISION,
            generatorRevision = RETAINED_THREE_GENERATOR_REVISION,
            skills = skills,
            formats = vocabularyFormats,
            humanPronouns = humanPronouns,
            identifyingPronoun = identifyingPronoun,
            verbs = verbs,
            nouns = nouns,
            sentences = sentences,
            lexicalReview = PilotReviewMetadata.records(humanPronouns, identifyingPronoun, verbs, nouns),
            vocabularySenses = vocabularySenses,
        )

    val pilotFourSnapshot =
        PilotContentSnapshot(
            contentRevision = RETAINED_FOUR_CONTENT_REVISION,
            generatorRevision = RETAINED_FOUR_GENERATOR_REVISION,
            skills = skills,
            formats = writingReadingFormats,
            humanPronouns = humanPronouns,
            identifyingPronoun = identifyingPronoun,
            verbs = verbs,
            nouns = nouns,
            sentences = sentences,
            lexicalReview = PilotReviewMetadata.records(humanPronouns, identifyingPronoun, verbs, nouns),
            vocabularySenses = vocabularySenses,
        )

    val pilotFiveSnapshot =
        PilotContentSnapshot(
            contentRevision = RETAINED_FIVE_CONTENT_REVISION,
            generatorRevision = RETAINED_FIVE_GENERATOR_REVISION,
            skills = skills,
            formats = tileFormats,
            humanPronouns = humanPronouns,
            identifyingPronoun = identifyingPronoun,
            verbs = verbs,
            nouns = currentNouns,
            sentences = sentences,
            lexicalReview = PilotReviewMetadata.records(humanPronouns, identifyingPronoun, verbs, currentNouns),
            vocabularySenses = wordChoiceSenses,
        )

    val pilotSixSnapshot =
        PilotContentSnapshot(
            contentRevision = RETAINED_SIX_CONTENT_REVISION,
            generatorRevision = RETAINED_SIX_GENERATOR_REVISION,
            skills = skills,
            formats = matchingFormats,
            humanPronouns = humanPronouns,
            identifyingPronoun = identifyingPronoun,
            verbs = verbs,
            nouns = currentNouns,
            sentences = sentences,
            lexicalReview = PilotReviewMetadata.records(
                humanPronouns,
                identifyingPronoun,
                verbs,
                currentNouns,
                matching = true,
            ),
            vocabularySenses = wordChoiceSenses,
        )

    val pilotSevenSnapshot =
        pilotSixSnapshot.copy(
            contentRevision = RETAINED_SEVEN_CONTENT_REVISION,
            generatorRevision = RETAINED_SEVEN_GENERATOR_REVISION,
            formats = gapFormats,
        )

    val moduleOneSentences: List<Sentence> = buildModuleOneSentences()

    val pilotEightSnapshot =
        pilotSevenSnapshot.copy(
            contentRevision = RETAINED_EIGHT_CONTENT_REVISION,
            generatorRevision = RETAINED_EIGHT_GENERATOR_REVISION,
            formats = moduleOneFormats,
            nouns = moduleOneNouns,
            sentences = sentences + moduleOneSentences,
            lexicalReview = PilotReviewMetadata.records(
                humanPronouns,
                identifyingPronoun,
                verbs,
                moduleOneNouns,
                matching = true,
                moduleOne = true,
            ),
        )

    val pilotNineSnapshot =
        pilotEightSnapshot.copy(
            contentRevision = RETAINED_NINE_CONTENT_REVISION,
            generatorRevision = RETAINED_NINE_GENERATOR_REVISION,
            skills = skills + Skill(
                "G038",
                "Refer to one inanimate thing",
                "Use den or det for one previously mentioned thing when its grammatical gender is supplied.",
                listOf("G001", "G002", "G003", "G006"),
            ),
            formats = inanimateReferenceFormats,
            lexicalReview = PilotReviewMetadata.records(
                humanPronouns,
                identifyingPronoun,
                verbs,
                moduleOneNouns,
                matching = true,
                moduleOne = true,
                referencePronouns = inanimateReferencePronouns,
            ),
            referencePronouns = inanimateReferencePronouns,
        )

    val pilotTenSnapshot =
        pilotNineSnapshot.copy(
            contentRevision = RETAINED_TEN_CONTENT_REVISION,
            generatorRevision = RETAINED_TEN_GENERATOR_REVISION,
            formats = presentOrderFormats,
            verbs = verbs + PresentOrderContent.work,
            lexicalReview = PilotReviewMetadata.records(
                humanPronouns,
                identifyingPronoun,
                verbs + PresentOrderContent.work,
                moduleOneNouns,
                matching = true,
                moduleOne = true,
                referencePronouns = inanimateReferencePronouns,
            ),
        )

    val pilotElevenSnapshot =
        pilotTenSnapshot.copy(
            contentRevision = RETAINED_ELEVEN_CONTENT_REVISION,
            generatorRevision = RETAINED_ELEVEN_GENERATOR_REVISION,
        )

    val pilotTwelveSnapshot =
        pilotElevenSnapshot.copy(
            contentRevision = RETAINED_TWELVE_CONTENT_REVISION,
            generatorRevision = RETAINED_TWELVE_GENERATOR_REVISION,
            verbs = pilotElevenSnapshot.verbs + personalIntroductionVerbs,
            fixedWords = personalIntroductionFixedWords,
            lexicalReview = PilotReviewMetadata.records(
                humanPronouns,
                identifyingPronoun,
                pilotElevenSnapshot.verbs + personalIntroductionVerbs,
                moduleOneNouns,
                matching = true,
                moduleOne = true,
                referencePronouns = inanimateReferencePronouns,
                fixedWords = personalIntroductionFixedWords,
            ),
        )

    val pilotThirteenSnapshot =
        pilotTwelveSnapshot.copy(
            contentRevision = RETAINED_THIRTEEN_CONTENT_REVISION,
            generatorRevision = RETAINED_THIRTEEN_GENERATOR_REVISION,
            skills = pilotTwelveSnapshot.skills + Skill(
                "G030",
                "Bare infinitive after kan",
                "Use the bare infinitive without at after the finite ability modal kan in the introduced frame.",
                listOf("G001", "G002", "G003", "G004", "G005"),
            ),
            formats = abilityFormats,
            verbs = pilotTwelveSnapshot.verbs + AbilityContent.can,
            lexicalReview = PilotReviewMetadata.records(
                humanPronouns,
                identifyingPronoun,
                pilotTwelveSnapshot.verbs + AbilityContent.can,
                moduleOneNouns,
                matching = true,
                moduleOne = true,
                referencePronouns = inanimateReferencePronouns,
                fixedWords = personalIntroductionFixedWords,
            ),
        )

    val pilotFourteenSnapshot =
        pilotThirteenSnapshot.copy(
            contentRevision = RETAINED_FOURTEEN_CONTENT_REVISION,
            generatorRevision = RETAINED_FOURTEEN_GENERATOR_REVISION,
            skills = pilotThirteenSnapshot.skills.map { skill ->
                if (skill.id == "G030") {
                    skill.copy(
                        title = "Modal meanings and following infinitives",
                        explanation =
                            "Interpret the introduced modal meaning from its supplied context and use the bare infinitive " +
                                "after a supplied finite modal in the introduced frames.",
                    )
                } else {
                    skill
                }
            },
            formats = modalRoundFormats,
            verbs = pilotThirteenSnapshot.verbs + ModalRoundContent.definitions.map(ModalRoundDefinition::modal),
            lexicalReview = PilotReviewMetadata.records(
                humanPronouns,
                identifyingPronoun,
                pilotThirteenSnapshot.verbs + ModalRoundContent.definitions.map(ModalRoundDefinition::modal),
                moduleOneNouns,
                matching = true,
                moduleOne = true,
                referencePronouns = inanimateReferencePronouns,
                fixedWords = personalIntroductionFixedWords,
            ),
        )

    val currentSnapshot =
        pilotFourteenSnapshot.copy(
            contentRevision = CURRENT_CONTENT_REVISION,
            generatorRevision = CURRENT_GENERATOR_REVISION,
            skills = pilotFourteenSnapshot.skills + Skill(
                "G016",
                "Time, place, and manner adverbials",
                "Add one reviewed time or place circumstance after a supplied clause core in the introduced frames.",
                listOf("G005"),
            ),
            formats = circumstanceFormats,
            fixedWords = personalIntroductionFixedWords + circumstanceFixedWords,
            lexicalReview = PilotReviewMetadata.records(
                humanPronouns,
                identifyingPronoun,
                pilotFourteenSnapshot.verbs,
                moduleOneNouns,
                matching = true,
                moduleOne = true,
                referencePronouns = inanimateReferencePronouns,
                fixedWords = personalIntroductionFixedWords + circumstanceFixedWords,
            ),
        )

    init {
        PilotContentValidator.requireValid(snapshot)
        PilotContentValidator.requireValid(pilotTwoSnapshot)
        PilotContentValidator.requireValid(pilotThreeSnapshot)
        PilotContentValidator.requireValid(pilotFourSnapshot)
        PilotContentValidator.requireValid(pilotFiveSnapshot)
        PilotContentValidator.requireValid(pilotSixSnapshot)
        PilotContentValidator.requireValid(pilotSevenSnapshot)
        PilotContentValidator.requireValid(pilotEightSnapshot)
        PilotContentValidator.requireValid(pilotNineSnapshot)
        PilotContentValidator.requireValid(pilotTenSnapshot)
        PilotContentValidator.requireValid(pilotElevenSnapshot)
        PilotContentValidator.requireValid(pilotTwelveSnapshot)
        PilotContentValidator.requireValid(pilotThirteenSnapshot)
        PilotContentValidator.requireValid(pilotFourteenSnapshot)
        PilotContentValidator.requireValid(currentSnapshot)
    }

    private fun buildSentences(): List<Sentence> {
        val result = mutableListOf<Sentence>()
        humanPronouns.forEach { result += sentence("C001", it, sleep, null) }
        humanPronouns.forEach { subject ->
            nouns.forEach { noun -> result += sentence("C002", subject, have, noun) }
        }
        humanPronouns.forEach { subject ->
            nouns.filter { it.id in setOf("L014", "L015", "L018") }.forEach { noun ->
                result += sentence("C002", subject, read, noun)
            }
        }
        humanPronouns.forEach { subject ->
            result += sentence("C002", subject, eat, nouns.single { it.id == "L021" })
        }
        nouns.forEach { noun -> result += sentence("C003", identifyingPronoun, be, noun) }
        check(result.size == 99)
        return result
    }

    private fun sentence(
        constructionId: String,
        subject: PronounEntry,
        verb: Verb,
        noun: NounEntry?,
    ): Sentence {
        val complement = noun?.let {
            Constituent(
                lexicalIds = listOf(it.gender.articleId, it.id),
                text = "${it.gender.article} ${it.text}",
                role = if (constructionId == "C003") "PREDICATIVE" else "OBJECT",
            )
        }
        return Sentence(
            id = listOfNotNull(constructionId, subject.id, verb.lexicalId, noun?.id).joinToString("-"),
            constructionId = constructionId,
            subject = Constituent(listOf(subject.id), subject.text, "SUBJECT"),
            verb = verb,
            complement = complement,
        )
    }

    private fun buildModuleOneSentences(): List<Sentence> =
        buildList {
            humanPronouns.filter { it.id in setOf("L001", "L002", "L004") }.forEach { subject ->
                listOf("Anna", "Nora").forEach { name ->
                    add(
                        Sentence(
                            id = "C004-${subject.id}-L010-name-${name.lowercase()}",
                            constructionId = "C004",
                            subject = Constituent(listOf(subject.id), subject.text, "SUBJECT"),
                            verb = be,
                            complement = Constituent(emptyList(), name, "PREDICATIVE"),
                        ),
                    )
                }
            }
            humanPronouns.filter { it.id in setOf("L001", "L002", "L003", "L004") }.forEach { subject ->
                add(
                    Sentence(
                        id = "C004-${subject.id}-L010-L025",
                        constructionId = "C004",
                        subject = Constituent(listOf(subject.id), subject.text, "SUBJECT"),
                        verb = be,
                        complement = Constituent(listOf("L025"), "lærer", "PREDICATIVE"),
                    ),
                )
            }
        }.also { check(it.size == 10) }

}
