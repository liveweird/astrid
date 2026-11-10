package app.astrid.engine

import java.net.URI

internal data class PilotContentSnapshot(
    val contentRevision: String,
    val generatorRevision: String,
    val skills: List<Skill>,
    val formats: List<ExerciseFormat>,
    val humanPronouns: List<PronounEntry>,
    val identifyingPronoun: PronounEntry,
    val verbs: List<Verb>,
    val nouns: List<NounEntry>,
    val sentences: List<Sentence>,
    val lexicalReview: List<LexicalReviewRecord>,
    val vocabularySenses: List<LexicalSense>,
    val referencePronouns: List<PronounEntry> = emptyList(),
)

internal data class ContentValidationIssue(
    val recordId: String,
    val message: String,
)

internal object PilotContentValidator {
    fun requireValid(snapshot: PilotContentSnapshot) {
        val issues = validate(snapshot)
        require(issues.isEmpty()) {
            issues.joinToString(prefix = "Invalid pilot content: ", separator = "; ") {
                "${it.recordId}: ${it.message}"
            }
        }
    }

    fun validate(snapshot: PilotContentSnapshot): List<ContentValidationIssue> =
        buildList {
            validateIdentities(snapshot)
            validateReferences(snapshot)
            validateForms(snapshot)
            validateSentences(snapshot)
            validateReviewMetadata(snapshot)
            validateVocabularySenses(snapshot)
        }

    private fun MutableList<ContentValidationIssue>.validateIdentities(snapshot: PilotContentSnapshot) {
        if (snapshot.contentRevision.isBlank()) add(issue("snapshot", "blank content revision"))
        if (snapshot.generatorRevision.isBlank()) add(issue("snapshot", "blank generator revision"))
        identityIssues("skill", snapshot.skills.map { it.id })
        identityIssues("format", snapshot.formats.map { it.id })
        identityIssues(
            "lexical",
            snapshot.humanPronouns.map { it.id } + snapshot.identifyingPronoun.id + snapshot.referencePronouns.map { it.id } +
                snapshot.verbs.map { it.lexicalId } + snapshot.nouns.map { it.id } +
                NounGender.entries.map { it.articleId },
        )
        identityIssues("sentence", snapshot.sentences.map { it.id })
        identityIssues("review", snapshot.lexicalReview.map { it.id })
        val expectedSkills = if (snapshot.contentRevision == PilotContent.CURRENT_CONTENT_REVISION) {
            EXPECTED_CURRENT_SKILL_IDS
        } else {
            EXPECTED_RETAINED_SKILL_IDS
        }
        requireIds("skill", snapshot.skills.map { it.id }, expectedSkills)
        val expectedFormats = expectedFormatIds(snapshot.contentRevision)
        requireIds("format", snapshot.formats.map { it.id }, expectedFormats)
        val lexicalIds =
            snapshot.humanPronouns.map { it.id } + snapshot.identifyingPronoun.id + snapshot.referencePronouns.map { it.id } +
                snapshot.verbs.map { it.lexicalId } + snapshot.nouns.map { it.id } +
                NounGender.entries.map { it.articleId }
        requireIds("lexical", lexicalIds, expectedLexicalIds(snapshot.contentRevision))
        requireIds("review", snapshot.lexicalReview.map { it.id }, lexicalIds.toSet())
        val expectedSentenceCount =
            if (snapshot.contentRevision in setOf(
                    PilotContent.RETAINED_EIGHT_CONTENT_REVISION,
                    PilotContent.CURRENT_CONTENT_REVISION,
                )
            ) CURRENT_SENTENCE_COUNT else RETAINED_SENTENCE_COUNT
        if (snapshot.sentences.size != expectedSentenceCount) {
            add(issue("snapshot", "expected $expectedSentenceCount sentences, found ${snapshot.sentences.size}"))
        }
    }

    private fun expectedFormatIds(contentRevision: String): Set<String> =
        when (contentRevision) {
            PilotContent.CONTENT_REVISION -> setOf("E001", "E002", "E003")
            PilotContent.RETAINED_CONTENT_REVISION -> setOf("E001", "E002", "E003", "E004", "E005", "E006")
            PilotContent.RETAINED_THREE_CONTENT_REVISION -> EXPECTED_PILOT_THREE_FORMAT_IDS
            PilotContent.RETAINED_FOUR_CONTENT_REVISION -> EXPECTED_PILOT_FOUR_FORMAT_IDS
            PilotContent.RETAINED_FIVE_CONTENT_REVISION -> EXPECTED_PILOT_FIVE_FORMAT_IDS
            PilotContent.RETAINED_SIX_CONTENT_REVISION -> EXPECTED_PILOT_SIX_FORMAT_IDS
            PilotContent.RETAINED_SEVEN_CONTENT_REVISION -> EXPECTED_PILOT_SEVEN_FORMAT_IDS
            PilotContent.RETAINED_EIGHT_CONTENT_REVISION -> EXPECTED_PILOT_EIGHT_FORMAT_IDS
            PilotContent.CURRENT_CONTENT_REVISION -> EXPECTED_CURRENT_FORMAT_IDS
            else -> emptySet()
        }

    private fun expectedLexicalIds(contentRevision: String): Set<String> =
        when {
            contentRevision in setOf(
                    PilotContent.RETAINED_FIVE_CONTENT_REVISION,
                    PilotContent.RETAINED_SIX_CONTENT_REVISION,
                    PilotContent.RETAINED_SEVEN_CONTENT_REVISION,
                ) -> EXPECTED_PILOT_SEVEN_LEXICAL_IDS
            contentRevision == PilotContent.RETAINED_EIGHT_CONTENT_REVISION -> EXPECTED_PILOT_EIGHT_LEXICAL_IDS
            contentRevision == PilotContent.CURRENT_CONTENT_REVISION -> EXPECTED_CURRENT_LEXICAL_IDS
            else -> EXPECTED_RETAINED_LEXICAL_IDS
        }

    private fun MutableList<ContentValidationIssue>.validateReferences(snapshot: PilotContentSnapshot) {
        val skillIds = snapshot.skills.map { it.id }.toSet()
        snapshot.skills.forEach { skill ->
            skill.prerequisiteIds.filterNot(skillIds::contains).forEach {
                add(issue(skill.id, "unknown prerequisite $it"))
            }
        }
        validateFormatReferences(snapshot, skillIds)
        prerequisiteCycles(snapshot.skills).forEach { cycle -> add(issue(cycle, "cyclic prerequisite")) }

        val lexicalIds =
            (snapshot.humanPronouns.map { it.id } + snapshot.identifyingPronoun.id + snapshot.referencePronouns.map { it.id } +
                snapshot.verbs.map { it.lexicalId } + snapshot.nouns.map { it.id } +
                NounGender.entries.map { it.articleId }).toSet()
        snapshot.sentences.forEach { sentence ->
            val references = sentence.subject.lexicalIds + sentence.verb.lexicalId + sentence.complement?.lexicalIds.orEmpty()
            references.filterNot(lexicalIds::contains).forEach {
                add(issue(sentence.id, "unknown lexical reference $it"))
            }
        }
    }

    private fun MutableList<ContentValidationIssue>.validateFormatReferences(
        snapshot: PilotContentSnapshot,
        skillIds: Set<String>,
    ) {
        snapshot.formats.forEach { format ->
            if (!hasExactlyOneTarget(format)) {
                add(issue(format.id, "requires exactly one grammar or lexical target, or one reading target"))
            }
            if (format.targetSkillId != null && format.targetSkillId !in skillIds) {
                add(issue(format.id, "unknown target ${format.targetSkillId}"))
            }
        }
        val expectedTargets =
            mapOf(
                "E001" to "G004",
                "E002" to "G006",
                "E003" to "G005",
                "E004" to "G004",
                "E005" to "G004",
                "E006" to "G006",
                "E009" to "G005",
                "E012" to "G005",
                "E013" to "G004",
                "E015" to "G004",
                "E016" to "G003",
                "E017" to "G003",
                "E018" to "G004",
                "E019" to "G004",
                "E020" to "G038",
                "E021" to "G038",
            )
        snapshot.formats.forEach { format ->
            expectedTargets[format.id]?.let { expected ->
                if (format.targetSkillId != expected) add(issue(format.id, "expected target $expected"))
            }
        }
        validateReadingTask(snapshot)
        val expectedLexicalTasks =
            mapOf(
                "E007" to LexicalTask("meaning", "da-en"),
                "E008" to LexicalTask("singular-headword", "en-da"),
                "E011" to LexicalTask("meaning", "en-da"),
                "E014" to LexicalTask("meaning", "en-da"),
            )
        snapshot.formats.forEach { format ->
            expectedLexicalTasks[format.id]?.let { expected ->
                if (format.lexicalTask != expected || format.targetSkillId != null) {
                    add(issue(format.id, "expected lexical task ${expected.facet}/${expected.direction}"))
                }
            }
        }
    }

    private fun hasExactlyOneTarget(format: ExerciseFormat): Boolean =
        listOf(format.targetSkillId, format.lexicalTask, format.readingTask).count { it != null } == 1

    private fun MutableList<ContentValidationIssue>.validateReadingTask(snapshot: PilotContentSnapshot) {
        val format = snapshot.formats.singleOrNull { it.id == "E010" } ?: return
        if (format.readingTask != ReadingTask("participant-action-complement") ||
            format.targetSkillId != null || format.lexicalTask != null
        ) {
            add(issue(format.id, "expected reading task participant-action-complement"))
        }
    }

    private fun MutableList<ContentValidationIssue>.validateForms(snapshot: PilotContentSnapshot) {
        snapshot.verbs.forEach { verb ->
            if (verb.lemma.isBlank()) add(issue(verb.lexicalId, "empty infinitive form"))
            if (verb.present.isBlank()) add(issue(verb.lexicalId, "empty finite present form"))
            if (verb.lemma == verb.present) add(issue(verb.lexicalId, "infinitive and finite present forms are identical"))
            if (verb.gloss.isBlank()) add(issue(verb.lexicalId, "empty English gloss"))
        }
        (snapshot.humanPronouns + snapshot.identifyingPronoun + snapshot.referencePronouns).forEach { pronoun ->
            if (pronoun.text.isBlank()) add(issue(pronoun.id, "empty active form"))
            if (pronoun.gloss.isBlank()) add(issue(pronoun.id, "empty English gloss"))
        }
        snapshot.nouns.forEach { noun ->
            if (noun.text.isBlank()) add(issue(noun.id, "empty active form"))
            if (noun.gloss.isBlank()) add(issue(noun.id, "empty English gloss"))
        }
    }

    private fun MutableList<ContentValidationIssue>.validateSentences(snapshot: PilotContentSnapshot) {
        val humanIds = snapshot.humanPronouns.map { it.id }.toSet()
        val pronounById = (snapshot.humanPronouns + snapshot.identifyingPronoun).associateBy { it.id }
        val nounById = snapshot.nouns.associateBy { it.id }
        val verbById = snapshot.verbs.associateBy { it.lexicalId }
        snapshot.sentences.forEach { sentence ->
            val subjectId = sentence.subject.lexicalIds.singleOrNull()
            val nounId = sentence.complement?.lexicalIds?.lastOrNull()
            val expectedId = listOfNotNull(sentence.constructionId, subjectId, sentence.verb.lexicalId, nounId).joinToString("-")
            if (sentence.constructionId != "C004" && sentence.id != expectedId) {
                add(issue(sentence.id, "canonical ID must be $expectedId"))
            }
            if (sentence.subject.role != ROLE_SUBJECT) add(issue(sentence.id, "invalid subject role ${sentence.subject.role}"))
            pronounById[subjectId]?.let { definition ->
                if (sentence.subject.text != definition.text) {
                    add(
                        issue(
                            sentence.id,
                            "subject text '${sentence.subject.text}' differs from ${definition.id} '${definition.text}'",
                        ),
                    )
                }
            }
            verbById[sentence.verb.lexicalId]?.let { definition ->
                if (sentence.verb != definition) add(issue(sentence.id, "verb data differs from ${definition.lexicalId}"))
            }
            if (sentence.constructionId != "C004") validateNounPhrase(sentence, nounById[nounId])
            if (!isLicensed(sentence, subjectId, nounId, humanIds, snapshot.identifyingPronoun.id)) {
                add(issue(sentence.id, "unlicensed construction or lexical combination"))
            }
        }
    }

    private fun MutableList<ContentValidationIssue>.validateReviewMetadata(snapshot: PilotContentSnapshot) {
        snapshot.lexicalReview.forEach { record ->
            if (record.headword.isBlank()) add(issue(record.id, "empty review headword"))
            if (record.selectedSense.isBlank()) add(issue(record.id, "empty selected sense"))
            if (record.constraints.isBlank()) add(issue(record.id, "empty review constraints"))
            if (record.activeForms.isEmpty()) add(issue(record.id, "missing active forms"))
            record.activeForms.forEach { (descriptor, form) ->
                if (descriptor.isBlank()) add(issue(record.id, "blank active-form descriptor"))
                if (form.isBlank()) add(issue(record.id, "blank active form for '$descriptor'"))
            }
            if (record.sourceUrls.isEmpty()) add(issue(record.id, "missing source reference"))
            record.sourceUrls.forEach { sourceUrl -> validateSourceUrl(record.id, sourceUrl) }
            if (record.reviewState != PilotReviewMetadata.REVIEW_STATE) {
                add(issue(record.id, "unsupported review state '${record.reviewState}'"))
            }
        }
    }

    private fun MutableList<ContentValidationIssue>.validateVocabularySenses(snapshot: PilotContentSnapshot) {
        val expected =
            when (snapshot.contentRevision) {
                PilotContent.RETAINED_THREE_CONTENT_REVISION,
                PilotContent.RETAINED_FOUR_CONTENT_REVISION,
                -> setOf("L014-S01", "L015-S01", "L018-S01")
                PilotContent.RETAINED_FIVE_CONTENT_REVISION,
                PilotContent.RETAINED_SIX_CONTENT_REVISION,
                PilotContent.RETAINED_SEVEN_CONTENT_REVISION,
                PilotContent.RETAINED_EIGHT_CONTENT_REVISION,
                PilotContent.CURRENT_CONTENT_REVISION,
                ->
                    setOf("L014-S01", "L015-S01", "L018-S01", "L024-S01")
                else -> emptySet()
            }
        identityIssues("vocabulary sense", snapshot.vocabularySenses.map { it.senseId })
        requireIds("vocabulary sense", snapshot.vocabularySenses.map { it.senseId }, expected)
        val nouns = snapshot.nouns.associateBy { it.id }
        snapshot.vocabularySenses.forEach { sense ->
            val noun = nouns[sense.entryId]
            if (noun == null) add(issue(sense.senseId, "unknown entry ${sense.entryId}"))
            if (sense.headword.isBlank()) add(issue(sense.senseId, "empty headword"))
            if (sense.gloss.isBlank()) add(issue(sense.senseId, "empty gloss"))
            if (noun != null && (sense.headword != noun.text || sense.article != noun.gender.article)) {
                add(issue(sense.senseId, "headword or article differs from ${sense.entryId}"))
            }
        }
    }

    private fun MutableList<ContentValidationIssue>.validateSourceUrl(
        recordId: String,
        sourceUrl: String,
    ) {
        val parsed = runCatching { URI(sourceUrl) }.getOrNull()
        if (parsed?.scheme != "https" || parsed.host.isNullOrBlank()) {
            add(issue(recordId, "malformed HTTPS source reference '$sourceUrl'"))
        } else if (sourceUrl !in KNOWN_SOURCE_URLS) {
            add(issue(recordId, "unknown source reference '$sourceUrl'"))
        }
    }

    private fun MutableList<ContentValidationIssue>.validateNounPhrase(
        sentence: Sentence,
        noun: NounEntry?,
    ) {
        val complement = sentence.complement ?: return
        if (complement.role !in setOf(ROLE_OBJECT, ROLE_PREDICATIVE)) {
            add(issue(sentence.id, "invalid complement role ${complement.role}"))
        }
        if (noun == null) return
        val expectedIds = listOf(noun.gender.articleId, noun.id)
        val expectedText = "${noun.gender.article} ${noun.text}"
        if (complement.lexicalIds != expectedIds) {
            add(issue(sentence.id, "noun phrase references ${complement.lexicalIds}; expected $expectedIds"))
        }
        if (complement.text != expectedText) {
            add(issue(sentence.id, "noun phrase text '${complement.text}'; expected '$expectedText'"))
        }
    }

    private fun isLicensed(
        sentence: Sentence,
        subjectId: String?,
        nounId: String?,
        humanIds: Set<String>,
        identifyingPronounId: String,
    ): Boolean = when (sentence.constructionId) {
            "C001" ->
                subjectId in humanIds && sentence.verb.lexicalId == "L013" && sentence.complement == null
            "C002" ->
                subjectId in humanIds && sentence.complement?.role == ROLE_OBJECT &&
                    nounLicensedForVerb(sentence.verb.lexicalId, nounId)
            "C003" ->
                subjectId == identifyingPronounId && sentence.verb.lexicalId == "L010" &&
                    nounId in BASE_SENTENCE_NOUN_IDS && sentence.complement?.role == ROLE_PREDICATIVE
            "C004" -> isLicensedModuleOneCopula(sentence, subjectId)
            else -> false
        }

    private fun isLicensedModuleOneCopula(sentence: Sentence, subjectId: String?): Boolean {
        val complement = sentence.complement
        val namedIdentity =
            subjectId in setOf("L001", "L002", "L004") && complement?.lexicalIds?.isEmpty() == true &&
                complement.text in setOf("Anna", "Nora")
        val occupation =
            subjectId in setOf("L001", "L002", "L003", "L004") && complement?.lexicalIds == listOf("L025") &&
                complement.text == "lærer"
        return sentence.verb.lexicalId == "L010" && complement?.role == ROLE_PREDICATIVE &&
            (namedIdentity || occupation)
    }

    private fun nounLicensedForVerb(
        verbId: String,
        nounId: String?,
    ): Boolean =
        when (verbId) {
            "L009" -> nounId in BASE_SENTENCE_NOUN_IDS
            "L011" -> nounId in setOf("L014", "L015", "L018")
            "L012" -> nounId == "L021"
            else -> false
        }

    private fun MutableList<ContentValidationIssue>.identityIssues(
        kind: String,
        ids: List<String>,
    ) {
        ids.filter(String::isBlank).forEach { add(issue(kind, "blank ID")) }
        ids.groupingBy { it }.eachCount().filterValues { it > 1 }.keys.forEach {
            add(issue(if (it.isBlank()) kind else it, "duplicate $kind ID"))
        }
    }

    private fun MutableList<ContentValidationIssue>.requireIds(
        kind: String,
        actual: List<String>,
        expected: Set<String>,
    ) {
        (expected - actual.toSet()).forEach { add(issue(it, "missing $kind ID")) }
        (actual.toSet() - expected).forEach { add(issue(it, "unexpected $kind ID")) }
    }

    private fun prerequisiteCycles(skills: List<Skill>): Set<String> {
        val graph = skills.associate { it.id to it.prerequisiteIds }
        val visiting = mutableSetOf<String>()
        val visited = mutableSetOf<String>()
        val cycles = mutableSetOf<String>()
        fun visit(id: String) {
            if (id in visiting) {
                cycles += id
                return
            }
            if (!visited.add(id)) return
            visiting += id
            graph[id].orEmpty().filter(graph::containsKey).forEach(::visit)
            visiting -= id
        }
        graph.keys.forEach(::visit)
        return cycles
    }

    private fun issue(recordId: String, message: String) = ContentValidationIssue(recordId, message)

    private const val RETAINED_SENTENCE_COUNT = 99
    private const val CURRENT_SENTENCE_COUNT = 109
    private const val LAST_SKILL_NUMBER = 6
    private const val LAST_RETAINED_LEXICAL_NUMBER = 23
    private const val LAST_PILOT_SEVEN_LEXICAL_NUMBER = 24
    private const val LAST_PILOT_EIGHT_LEXICAL_NUMBER = 25
    private const val LAST_CURRENT_LEXICAL_NUMBER = 26
    private const val ID_NUMBER_WIDTH = 3
    private val EXPECTED_RETAINED_SKILL_IDS =
        (1..LAST_SKILL_NUMBER).map { "G${it.toString().padStart(ID_NUMBER_WIDTH, '0')}" }.toSet()
    private val EXPECTED_CURRENT_SKILL_IDS = EXPECTED_RETAINED_SKILL_IDS + "G038"
    private val EXPECTED_RETAINED_LEXICAL_IDS =
        (1..LAST_RETAINED_LEXICAL_NUMBER).map { "L${it.toString().padStart(ID_NUMBER_WIDTH, '0')}" }.toSet()
    private val EXPECTED_PILOT_SEVEN_LEXICAL_IDS =
        (1..LAST_PILOT_SEVEN_LEXICAL_NUMBER).map { "L${it.toString().padStart(ID_NUMBER_WIDTH, '0')}" }.toSet()
    private val EXPECTED_PILOT_EIGHT_LEXICAL_IDS =
        (1..LAST_PILOT_EIGHT_LEXICAL_NUMBER).map { "L${it.toString().padStart(ID_NUMBER_WIDTH, '0')}" }.toSet()
    private val EXPECTED_CURRENT_LEXICAL_IDS =
        (1..LAST_CURRENT_LEXICAL_NUMBER).map { "L${it.toString().padStart(ID_NUMBER_WIDTH, '0')}" }.toSet()
    private val EXPECTED_PILOT_EIGHT_FORMAT_IDS =
        (1..LAST_PILOT_EIGHT_FORMAT_NUMBER).map { "E${it.toString().padStart(ID_NUMBER_WIDTH, '0')}" }.toSet()
    private val EXPECTED_CURRENT_FORMAT_IDS =
        (1..LAST_CURRENT_FORMAT_NUMBER).map { "E${it.toString().padStart(ID_NUMBER_WIDTH, '0')}" }.toSet()
    private val EXPECTED_PILOT_SIX_FORMAT_IDS =
        (1..LAST_PILOT_SIX_FORMAT_NUMBER).map { "E${it.toString().padStart(ID_NUMBER_WIDTH, '0')}" }.toSet()
    private val EXPECTED_PILOT_THREE_FORMAT_IDS =
        (1..LAST_PILOT_THREE_FORMAT_NUMBER).map { "E${it.toString().padStart(ID_NUMBER_WIDTH, '0')}" }.toSet()
    private val EXPECTED_PILOT_FOUR_FORMAT_IDS =
        (1..LAST_PILOT_FOUR_FORMAT_NUMBER).map { "E${it.toString().padStart(ID_NUMBER_WIDTH, '0')}" }.toSet()
    private val EXPECTED_PILOT_FIVE_FORMAT_IDS =
        (1..LAST_PILOT_FIVE_FORMAT_NUMBER).map { "E${it.toString().padStart(ID_NUMBER_WIDTH, '0')}" }.toSet()
    private val KNOWN_SOURCE_URLS =
        setOf(
            "https://sproget.dk/typiske-problemer/pronomener-stedord/",
            "https://ro.dsn.dk/?paragraf=13&type=rulesearch",
            "https://ro.dsn.dk/?paragraf=5&type=rulesearch",
            "https://ordnet.dk/ddo/ordbog/en",
            "https://sproget.dk/sprogviden/spoergsmaal-og-svar/dansk-sprognaevn/sv00000096/",
            "https://ordnet.dk/ddo/ordbog/have",
            "https://ordnet.dk/ddo/ordbog/v%C3%A6re",
            "https://ordnet.dk/ddo/ordbog/l%C3%A6se",
            "https://ordnet.dk/ddo/ordbog/spise",
            "https://ordnet.dk/ddo/ordbog/sove",
            "https://ordnet.dk/ddo/ordbog/bog",
            "https://ordnet.dk/ddo/ordbog/avis",
            "https://ordnet.dk/ddo/ordbog/bil",
            "https://ordnet.dk/ddo/ordbog/hund",
            "https://ordnet.dk/ddo/ordbog/brev",
            "https://ordnet.dk/ddo/ordbog/hus",
            "https://ordnet.dk/ddo/ordbog/bord",
            "https://ordnet.dk/ddo/ordbog/%C3%A6ble",
            "https://ordnet.dk/ddo/ordbog/magasin",
            "https://ordnet.dk/ddo/ordbog/l%C3%A6rer",
        )
    private const val ROLE_SUBJECT = "SUBJECT"
    private const val ROLE_OBJECT = "OBJECT"
    private const val ROLE_PREDICATIVE = "PREDICATIVE"
    private const val FIRST_BASE_NOUN_NUMBER = 14
    private const val LAST_BASE_NOUN_NUMBER = 21
    private val BASE_SENTENCE_NOUN_IDS =
        (FIRST_BASE_NOUN_NUMBER..LAST_BASE_NOUN_NUMBER)
            .map { "L${it.toString().padStart(ID_NUMBER_WIDTH, '0')}" }
            .toSet()
    private const val LAST_PILOT_THREE_FORMAT_NUMBER = 8
    private const val LAST_PILOT_FOUR_FORMAT_NUMBER = 10
    private const val LAST_PILOT_FIVE_FORMAT_NUMBER = 13
    private const val LAST_PILOT_SIX_FORMAT_NUMBER = 14
    private const val LAST_PILOT_SEVEN_FORMAT_NUMBER = 15
    private val EXPECTED_PILOT_SEVEN_FORMAT_IDS =
        (1..LAST_PILOT_SEVEN_FORMAT_NUMBER).map { "E${it.toString().padStart(ID_NUMBER_WIDTH, '0')}" }.toSet()
    private const val LAST_PILOT_EIGHT_FORMAT_NUMBER = 19
    private const val LAST_CURRENT_FORMAT_NUMBER = 21
}
