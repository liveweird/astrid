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

internal object PilotContent {
    const val CONTENT_REVISION = "pilot-1"
    const val GENERATOR_REVISION = "generator-1"

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

    val have = Verb("L009", "have", "har", "have")
    val be = Verb("L010", "være", "er", "be")
    val read = Verb("L011", "læse", "læser", "read")
    val eat = Verb("L012", "spise", "spiser", "eat")
    val sleep = Verb("L013", "sove", "sover", "sleep")
    val verbs = listOf(have, be, read, eat, sleep)

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
        )

    init {
        PilotContentValidator.requireValid(snapshot)
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

}
