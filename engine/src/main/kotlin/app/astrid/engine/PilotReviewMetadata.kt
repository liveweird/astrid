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
        moduleOne: Boolean = false,
        referencePronouns: List<PronounEntry> = emptyList(),
        fixedWords: List<FixedWordEntry> = emptyList(),
    ): List<LexicalReviewRecord> =
        pronounRecords(
            pronouns, identifyingPronoun, moduleOne,
            verbs.any { it.lexicalId == "L033" },
            fixedWords.any { it.id == "L037" },
        ) +
            referencePronounRecords(referencePronouns) +
            verbRecords(verbs, moduleOne, fixedWords.any { it.id == "L037" }) +
            nounRecords(nouns, matching) +
            articleRecords() + fixedWordRecords(fixedWords, verbs.any { it.lexicalId == "L033" })

    private fun referencePronounRecords(pronouns: List<PronounEntry>): List<LexicalReviewRecord> =
        pronouns.map { entry ->
            record(
                entry.id,
                entry.text,
                "singular common-gender inanimate reference",
                mapOf("subject-reference" to entry.text),
                "Standalone subject-reference form with an explicit singular common-gender antecedent; other uses excluded.",
                PRONOUN_SOURCE,
            )
        }

    private fun pronounRecords(
        pronouns: List<PronounEntry>,
        identifyingPronoun: PronounEntry,
        moduleOne: Boolean,
        includesAbilityFrames: Boolean,
        includesCircumstanceFrames: Boolean,
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
        val moduleOneConstraints =
            mapOf(
                "L001" to " Pair eight also licenses bounded C004 name and occupation subjects.",
                "L002" to " Pair eight also licenses bounded C004 name and occupation subjects.",
                "L003" to " Pair eight also licenses the bounded C004 occupation subject.",
                "L004" to " Pair eight also licenses bounded C004 name and occupation subjects.",
            )
        val records = pronouns.map { entry ->
            val detail = pronounDetails.getValue(entry.id)
            val extension = pronounScopeExtension(
                entry.id, includesAbilityFrames, includesCircumstanceFrames,
            )
            val constraints = detail.second + (if (moduleOne) moduleOneConstraints[entry.id].orEmpty() else "") + extension.first
            record(
                entry.id, entry.text, detail.first, mapOf("subject" to entry.text), constraints, PRONOUN_SOURCE,
                additionalSources =
                    ((if (entry.id == "L006") listOf(UPPERCASE_PRONOUN_SOURCE) else emptyList()) + extension.second).distinct(),
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

    private fun pronounScopeExtension(
        lexicalId: String,
        includesAbilityFrames: Boolean,
        includesCircumstanceFrames: Boolean,
    ): Pair<String, List<String>> {
        val ability = includesAbilityFrames && lexicalId in MODAL_SUBJECT_IDS
        val circumstance = includesCircumstanceFrames && lexicalId in CIRCUMSTANCE_SUBJECT_IDS
        val constraints = buildString {
            if (ability) append(" Pair thirteen additionally licenses the reviewed C014 modal subjects.")
            if (circumstance) append(" Pair fifteen additionally licenses the reviewed C007 circumstance subjects.")
        }
        val sources = buildList {
            if (ability) addAll(listOf(INFINITIVE_SOURCE, VERB_SOURCES.getValue("L033")))
            if (circumstance) add(FINAL_CIRCUMSTANCE_SOURCE)
        }.distinct()
        return constraints to sources
    }

    private fun verbRecords(
        verbs: List<Verb>,
        moduleOne: Boolean,
        includesCircumstanceFrames: Boolean,
    ): List<LexicalReviewRecord> {
        val includesAbilityFrame = verbs.any { it.lexicalId == AbilityContent.MODAL_LEXICAL_ID }
        val includesModalRound = verbs.any { it.lexicalId == "L034" }
        val readFrameExtensionLexicalIds =
            readFrameExtensionLexicalIds(includesAbilityFrame, includesModalRound, includesCircumstanceFrames)
        val details =
            mapOf(
                "L009" to ("possess or have available" to "C002 with a human subject and licensed count noun; auxiliary uses excluded."),
                "L010" to ("identify or classify" to "C003 fixed Det er … only; other copular and auxiliary uses excluded."),
                "L011" to ("read written text" to "C002 with bog, avis, or brev only."),
                "L012" to ("eat" to "C002 with æble only; intransitive and other food uses excluded."),
                "L013" to ("sleep" to "C001 human subject with no object."),
                "L027" to ("work" to "Pair ten authored human intransitive work clauses with no object or circumstance only."),
                "L028" to ("bear a stated name" to "Pair twelve authored C004 name frames only; other senses excluded."),
                "L029" to ("live or reside in a place" to "Pair twelve authored C006 i + city residence frames only."),
                "L030" to ("speak a language" to "Pair twelve authored C002-family dansk language-complement frames only."),
                "L033" to (
                    "be able to" to
                        "Pair thirteen authored C014 ability frames only; kan is the supplied finite modal and " +
                        "broader modality is excluded."
                ),
                "L034" to (
                    "L034-S01: personal wish or intention" to
                        "Pair fourteen authored C014 volition frames only; prediction, requests, past forms, and ellipsis excluded."
                ),
                "L035" to (
                    "L035-S01: external obligation" to
                        "Pair fourteen authored C014 requirement frames only; hearsay, prediction, past forms, and ellipsis excluded."
                ),
                "L036" to (
                    "L036-S01: permission" to
                        "Pair fourteen authored C014 permission frames only; epistemic readings, negation, " +
                            "past forms, and ellipsis excluded."
                ),
            )
        return verbs.map { verb ->
            val detail = details.getValue(verb.lexicalId)
            val constraints = when {
                moduleOne && verb.lexicalId == "L010" ->
                    detail.second.removeSuffix(" only; other copular and auxiliary uses excluded.") +
                        "; pair eight additionally licenses the ten enumerated C004 name and occupation clauses. " +
                        "Other copular and auxiliary uses remain excluded."
                verb.lexicalId in readFrameExtensionLexicalIds -> buildString {
                    append(detail.second.removeSuffix(" only."))
                    if (includesAbilityFrame) {
                        append("; pair thirteen additionally licenses the C014 written-language ability frames")
                    }
                    if (includesModalRound) {
                        append("; pair fourteen additionally licenses three contextual C014 written-language modal frames")
                    }
                    if (includesCircumstanceFrames) {
                        append(
                            "; pair fifteen additionally licenses the two authored C007 reading propositions " +
                                "and the separate worked reading example",
                        )
                    }
                    append(".")
                }
                includesCircumstanceFrames && verb.lexicalId == "L027" ->
                    detail.second.removeSuffix(" only.") +
                        "; pair fifteen additionally licenses the two authored C007 reading propositions."
                else -> detail.second
            }
            record(
                verb.lexicalId,
                verb.lemma,
                detail.first,
                mapOf("infinitive" to verb.lemma, "finite-present" to verb.present),
                constraints,
                VERB_SOURCES.getValue(verb.lexicalId),
                additionalSources = buildList {
                    if (verb.lexicalId == "L033") add(INFINITIVE_SOURCE)
                    if (includesCircumstanceFrames && verb.lexicalId in setOf("L011", "L027")) {
                        add(FINAL_CIRCUMSTANCE_SOURCE)
                    }
                },
            )
        }
    }

    private fun readFrameExtensionLexicalIds(
        includesAbilityFrame: Boolean,
        includesModalRound: Boolean,
        includesCircumstanceFrames: Boolean,
    ): Set<String> =
        if (includesAbilityFrame || includesModalRound || includesCircumstanceFrames) {
            setOf(AbilityContent.READ_LEXICAL_ID)
        } else {
            emptySet()
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
                "L025" to ("teacher" to "Supplied common-gender noun and bare occupational complement in the bounded C004 frame only."),
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

    private fun fixedWordRecords(words: List<FixedWordEntry>, ability: Boolean): List<LexicalReviewRecord> {
        val details = mapOf(
            "L031" to Triple(
                "Danish language",
                mapOf("language-complement" to "dansk"),
                if (ability) {
                    "Fixed supplied language complement in the tale frame and C014 written-language reading frame; " +
                        "adjective and nationality uses excluded."
                } else {
                    "Fixed supplied language complement in the pair-twelve tale frame; adjective and nationality uses excluded."
                },
            ),
            "L032" to Triple(
                "in at a city location",
                mapOf("location-preposition" to "i"),
                if (words.any { it.id == "L037" }) {
                    "Supplied location preposition in the reviewed residence frames and the scoped C007 " +
                        "final-place work frame with Aarhus."
                } else {
                    "Fixed supplied preposition before Aarhus or Odense in the pair-twelve residence frame only."
                },
            ),
            "L037" to Triple(
                "L037-S01: current calendar day",
                mapOf("time-circumstance" to "i dag"),
                "Pair fifteen scoped C007 final-time frames only; the meaning ‘nowadays’ is excluded.",
            ),
            "L038" to Triple(
                "L038-S01: temporal now",
                mapOf("time-circumstance" to "nu"),
                "Pair fifteen scoped C007 final-time frame only; discourse and conjunction uses are excluded.",
            ),
        )
        return words.map { word ->
            val detail = details.getValue(word.id)
            record(word.id, word.text, detail.first, detail.second, detail.third, FIXED_WORD_SOURCES.getValue(word.id))
        }
    }

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
            "L027" to "https://ordnet.dk/ddo/ordbog/arbejde",
            "L028" to "https://ordnet.dk/ddo/ordbog/hedde",
            "L029" to "https://ordnet.dk/ddo/ordbog/bo",
            "L030" to "https://ordnet.dk/ddo/ordbog/tale",
            "L033" to "https://ordnet.dk/ddo/ordbog/11028611",
            "L034" to "https://ordnet.dk/ddo/ordbog/ville",
            "L035" to "https://ordnet.dk/ddo/ordbog/skulle",
            "L036" to "https://ordnet.dk/ddo/ordbog/m%C3%A5tte",
        )
    private const val INFINITIVE_SOURCE = "https://lex.dk/infinitiv"
    private const val FINAL_CIRCUMSTANCE_SOURCE =
        "https://sproget.dk/sprogviden/spoergsmaal-og-svar/dansk-sprognaevn/sv00006990/"
    private val MODAL_SUBJECT_IDS = setOf("L001", "L002", "L003", "L004", "L005", "L007")
    private val CIRCUMSTANCE_SUBJECT_IDS = MODAL_SUBJECT_IDS
    private val FIXED_WORD_SOURCES = mapOf(
        "L031" to "https://ordnet.dk/ddo/ordbog/dansk",
        "L032" to "https://ordnet.dk/ddo/ordbog/i",
        "L037" to "https://ordnet.dk/ddo/ordbog/59001379",
        "L038" to "https://ordnet.dk/ddo/ordbog/nu",
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
            "L025" to "https://ordnet.dk/ddo/ordbog/l%C3%A6rer",
        )
}
