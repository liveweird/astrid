package app.astrid.engine

internal data class LexicalReviewRecord(
    val id: String,
    val headword: String,
    val selectedSense: String,
    val activeForms: Map<String, String>,
    val constraints: String,
    val sourceUrls: List<String>,
    val reviewState: String,
)

internal object PilotReviewMetadata {
    const val REVIEW_STATE = "draft-not-teacher-reviewed"
    const val PROVENANCE = "docs/knowledge/danish-vocabulary-seed.md at baseline 3b462a9"
    const val SOURCE_REVIEW_DATE = "2026-11-07; see docs/knowledge/pilot-source-review.md"

    fun records(
        pronouns: List<PronounEntry>,
        identifyingPronoun: PronounEntry,
        verbs: List<Verb>,
        nouns: List<NounEntry>,
        matching: Boolean = false,
    ): List<LexicalReviewRecord> =
        pronounRecords(pronouns, identifyingPronoun) +
            verbRecords(verbs) + nounRecords(nouns, matching) + articleRecords()

    private fun pronounRecords(
        pronouns: List<PronounEntry>,
        identifyingPronoun: PronounEntry,
    ): List<LexicalReviewRecord> {
        val pronounDetails =
            mapOf(
                "L001" to ("human speaker subject" to "Human speaker; C001/C002 subject only."),
                "L002" to ("one informal addressee" to "Singular informal human addressee; C001/C002 subject only."),
                "L003" to ("established male human subject" to "Human reference must be established by the prompt."),
                "L004" to ("established female human subject" to "Human reference must be established by the prompt."),
                "L005" to ("human group including speaker" to "Human group; C001/C002 subject only."),
                "L006" to ("multiple addressees" to "Uppercase I is lexical; C001/C002 subject only."),
                "L007" to ("established plural human subject" to "Plural human reference; formal De is excluded."),
            )
        val records = pronouns.map { entry ->
            val detail = pronounDetails.getValue(entry.id)
            record(
                entry.id, entry.text, detail.first, mapOf("subject" to entry.text), detail.second, PRONOUN_SOURCE,
                additionalSources = if (entry.id == "L006") listOf(UPPERCASE_PRONOUN_SOURCE) else emptyList(),
            )
        }
        val identifying =
            record(
                identifyingPronoun.id,
                identifyingPronoun.text,
                "fixed identifying it or that",
                mapOf("subject" to identifyingPronoun.text),
                "C003 fixed Det er … subject only; not a general pronoun-reference rule.",
                PRONOUN_SOURCE,
            )
        return records + identifying
    }

    private fun verbRecords(verbs: List<Verb>): List<LexicalReviewRecord> {
        val details =
            mapOf(
                "L009" to ("possess or have available" to "C002 with a human subject and licensed count noun; auxiliary uses excluded."),
                "L010" to ("identify or classify" to "C003 fixed Det er … only; other copular and auxiliary uses excluded."),
                "L011" to ("read written text" to "C002 with bog, avis, or brev only."),
                "L012" to ("eat" to "C002 with æble only; intransitive and other food uses excluded."),
                "L013" to ("sleep" to "C001 human subject with no object."),
            )
        return verbs.map { verb ->
            val detail = details.getValue(verb.lexicalId)
            record(
                verb.lexicalId,
                verb.lemma,
                detail.first,
                mapOf("infinitive" to verb.lemma, "finite-present" to verb.present),
                detail.second,
                VERB_SOURCES.getValue(verb.lexicalId),
            )
        }
    }

    private fun nounRecords(nouns: List<NounEntry>, matching: Boolean): List<LexicalReviewRecord> {
        val details =
            mapOf(
                "L014" to ("book" to "Written text and ownable count noun; beech-mast homonym excluded."),
                "L015" to ("newspaper" to "Written text and ownable count noun."),
                "L016" to ("car" to "Vehicle and ownable count noun."),
                "L017" to ("dog" to "Animal count noun; money-slang homonym excluded."),
                "L018" to ("paper letter or message" to "Written text and ownable count noun; specialist senses excluded."),
                "L019" to ("house" to "Building and ownable count noun."),
                "L020" to ("table" to "Furniture count noun; ship-plank sense excluded."),
                "L021" to ("apple fruit" to "Food count noun; tree and metaphorical senses excluded."),
                "L024" to ("printed magazine" to if (matching) {
                    "Printed periodical count noun for E011 and E014 only; store, depot, broadcast and weapon-container senses excluded."
                } else {
                    "Printed periodical count noun for E011 only; store, depot, broadcast and weapon-container senses excluded."
                }),
            )
        return nouns.map { noun ->
            val detail = details.getValue(noun.id)
            record(
                noun.id,
                noun.text,
                detail.first,
                mapOf("indefinite-singular" to "${noun.gender.article} ${noun.text}"),
                detail.second,
                NOUN_SOURCES.getValue(noun.id),
            )
        }
    }

    private fun articleRecords(): List<LexicalReviewRecord> =
        listOf(
            record(
                "L022",
                "en",
                "unstressed common-gender singular indefinite article",
                mapOf("article" to "en"),
                "Before a licensed common-gender singular count noun; numeral en/én is a different use, with optional accent.",
                GENDER_SOURCE,
                additionalSources = listOf(ARTICLE_SOURCE, ACCENT_SOURCE),
            ),
            record(
                "L023",
                "et",
                "unstressed neuter singular indefinite article",
                mapOf("article" to "et"),
                "Before a licensed neuter singular count noun; numeral et/ét is a different use, with optional accent.",
                GENDER_SOURCE,
                additionalSources = listOf(ARTICLE_SOURCE, ACCENT_SOURCE),
            ),
        )

    private fun record(
        id: String,
        headword: String,
        sense: String,
        forms: Map<String, String>,
        constraints: String,
        sourceUrl: String,
        additionalSources: List<String> = emptyList(),
    ) = LexicalReviewRecord(id, headword, sense, forms, constraints, listOf(sourceUrl) + additionalSources, REVIEW_STATE)

    private const val PRONOUN_SOURCE = "https://sproget.dk/typiske-problemer/pronomener-stedord/"
    private const val UPPERCASE_PRONOUN_SOURCE = "https://ro.dsn.dk/?paragraf=13&type=rulesearch"
    private const val ACCENT_SOURCE = "https://ro.dsn.dk/?paragraf=5&type=rulesearch"
    private const val ARTICLE_SOURCE = "https://ordnet.dk/ddo/ordbog/en"
    private const val GENDER_SOURCE =
        "https://sproget.dk/sprogviden/spoergsmaal-og-svar/dansk-sprognaevn/sv00000096/"
    private val VERB_SOURCES =
        mapOf(
            "L009" to "https://ordnet.dk/ddo/ordbog/have",
            "L010" to "https://ordnet.dk/ddo/ordbog/v%C3%A6re",
            "L011" to "https://ordnet.dk/ddo/ordbog/l%C3%A6se",
            "L012" to "https://ordnet.dk/ddo/ordbog/spise",
            "L013" to "https://ordnet.dk/ddo/ordbog/sove",
        )
    private val NOUN_SOURCES =
        mapOf(
            "L014" to "https://ordnet.dk/ddo/ordbog/bog",
            "L015" to "https://ordnet.dk/ddo/ordbog/avis",
            "L016" to "https://ordnet.dk/ddo/ordbog/bil",
            "L017" to "https://ordnet.dk/ddo/ordbog/hund",
            "L018" to "https://ordnet.dk/ddo/ordbog/brev",
            "L019" to "https://ordnet.dk/ddo/ordbog/hus",
            "L020" to "https://ordnet.dk/ddo/ordbog/bord",
            "L021" to "https://ordnet.dk/ddo/ordbog/%C3%A6ble",
            "L024" to "https://ordnet.dk/ddo/ordbog/magasin",
        )
}
