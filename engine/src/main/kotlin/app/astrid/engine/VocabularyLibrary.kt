@file:Suppress("MagicNumber") // The finite L001-L079 inventory and form ordinals are part of the projection contract.

package app.astrid.engine

data class VocabularyLibraryCatalog(
    val libraryRevision: String,
    val contentRevision: String,
    val items: List<VocabularyLibraryEntry>,
)

data class VocabularyLibraryEntry(
    val entryId: String,
    val headword: String,
    val situationIds: List<String>,
    val senses: List<VocabularyLibrarySense>,
)

data class VocabularyLibrarySense(
    val senseId: String,
    val meaning: String,
    val activeForms: List<VocabularyLibraryForm>,
    val restrictions: List<String>,
    val examples: List<VocabularyLibraryExample>,
    val exampleNote: String?,
    val lessons: List<VocabularyLibraryLesson>,
    val sourceUrls: List<String>,
    val reviewState: String,
)

data class VocabularyLibraryForm(val formId: String, val label: String, val text: String)

data class VocabularyLibraryExample(
    val exampleId: String,
    val danish: String,
    val english: String?,
    val scene: String?,
)

data class VocabularyLibraryLesson(val focusId: String, val title: String)

/**
 * Passive learner-facing projection of the current lexical inventory.
 *
 * Sense IDs that already exist in assessment content retain their identity. The remaining `Lnnn-S01` identities are
 * library-local crosswalk identities and do not create an assessment target or change any published generator snapshot.
 */
object VocabularyLibrary {
    const val REVISION = "vocabulary-library-1"

    fun catalog(): VocabularyLibraryCatalog {
        val records = PilotContent.currentSnapshot.lexicalReview.associateBy(LexicalReviewRecord::id)
        require(records.keys == (1..79).map(::entryId).toSet())
        return VocabularyLibraryCatalog(
            libraryRevision = REVISION,
            contentRevision = PilotContent.currentSnapshot.contentRevision,
            items = records.values.sortedBy(LexicalReviewRecord::id).map(::entry),
        )
    }

    private fun entry(record: LexicalReviewRecord) = VocabularyLibraryEntry(
        entryId = record.id,
        headword = record.headword,
        situationIds = SITUATIONS.getValue(record.id),
        senses = if (record.id == "L008") {
            listOf(
                sense(record, "L008-S01", "fixed identification: this or it is", listOf("fixed identification" to "Det er")),
                sense(record, "L008-S02", "it referring to one neuter singular thing", listOf("subject-reference" to "det")),
            )
        } else if (record.id == "L009") {
            listOf(
                sense(record, "L009-S01", "possess or have available", listOf("infinitive" to "have", "finite-present" to "har")),
                sense(record, "L009-S02", "have or receive in the polite request frame", listOf("bare-infinitive" to "have")),
            )
        } else {
            sense(record, "${record.id}-S01", cleanMeaning(record.selectedSense), forms(record))
                .let(::listOf)
        },
    )

    private fun sense(
        record: LexicalReviewRecord,
        senseId: String,
        meaning: String,
        forms: List<Pair<String, String>>,
    ) = VocabularyLibrarySense(
        senseId = senseId,
        meaning = meaning,
        activeForms = forms.mapIndexed { index, (label, text) ->
            VocabularyLibraryForm("$senseId-F${index + 1}", label, text)
        },
        restrictions = RESTRICTIONS[senseId] ?: listOf(learnerRestriction(record.constraints)),
        examples = EXAMPLES[senseId].orEmpty(),
        exampleNote = if (EXAMPLES.containsKey(senseId)) null else EDITORIAL_GAP,
        lessons = LESSONS[senseId].orEmpty(),
        sourceUrls = record.sourceUrls,
        reviewState = record.reviewState,
    )

    private fun forms(record: LexicalReviewRecord): List<Pair<String, String>> = when (record.id) {
        "L032" -> listOf("city-location" to "i Aarhus", "city-location" to "i Odense")
        else -> record.activeForms.toList()
    }

    private fun cleanMeaning(value: String) = value.substringAfter(": ", value)

    private fun learnerRestriction(value: String) = value
        .replace(Regex("C\\d+(?:/C\\d+)?"), "the taught pattern")
        .replace(Regex("(?i)pair(?:-| )\\w+"), "the taught lesson")
        .replace(Regex("P[123]"), "the taught situation")
        .replace(Regex("E\\d+(?: and E\\d+)?"), "the taught activity")

    private const val EDITORIAL_GAP = "No reviewed bilingual library example is currently authored for this selected use."
    private const val MODULE_ONE = "module-one"
    private const val P1 = "vocabulary-p1"
    private const val P2 = "vocabulary-p2"
    private const val P3 = "vocabulary-p3"

    private val P1_IDS = setOf(
        "L001", "L004", "L009", "L011", "L014", "L015", "L018", "L022", "L027", "L031", "L032", "L037", "L038",
        "L041", "L042", "L043", "L044", "L045", "L046", "L047", "L048", "L049",
    )
    private val P2_IDS = setOf(
        "L001", "L009", "L012", "L021", "L034", "L045", "L046", "L047", "L050", "L051", "L052", "L053", "L054", "L055",
        "L056", "L057", "L058", "L059", "L060", "L061", "L062", "L063",
    )
    private val P3_IDS = setOf(
        "L001", "L008", "L009", "L010", "L016", "L027", "L029", "L032", "L038", "L048", "L064", "L065", "L066", "L067",
        "L068",
        "L069", "L070", "L071", "L072", "L073", "L074", "L075", "L076", "L077", "L078", "L079",
    )
    private val SITUATIONS: Map<String, List<String>> = (1..79).associate { number ->
        val id = entryId(number)
        val situations = buildList {
            if (number <= 40) add(MODULE_ONE)
            if (id in P1_IDS) add(P1)
            if (id in P2_IDS) add(P2)
            if (id in P3_IDS) add(P3)
        }
        id to situations
    }

    private fun entryId(number: Int) = "L${number.toString().padStart(3, '0')}"

    private val RESTRICTIONS = mapOf(
        "L008-S01" to listOf("Use only as the complete supplied Det er identification pattern; it is not the referential sense."),
        "L008-S02" to listOf("Standalone subject reference to one established neuter singular thing; identification uses are excluded."),
        "L009-S01" to listOf("Possession in the reviewed frames; request, auxiliary and other uses are excluded."),
        "L009-S02" to listOf(
            "Bare have only in the supplied vil gerne have request frames; possession har is not part of this sense.",
        ),
        "L010-S01" to listOf("Fixed Det er identification and the bounded authored identity frames only; auxiliary uses are excluded."),
        "L011-S01" to listOf("Reading written material in the authored basic, work-and-study and modal examples only."),
        "L012-S01" to listOf("Eating is limited to the authored apple, bread and bread-roll examples."),
        "L016-S01" to listOf("Car as a count noun, possession item and explicit selector of the authored vehicular scene."),
        "L027-S01" to listOf("Human work in the authored present, time, city and static-home examples; objects are excluded."),
        "L029-S01" to listOf("Human residence only with the supplied complete city surfaces i Aarhus and i Odense."),
        "L031-S01" to listOf(
            "Danish as a supplied language complement in the authored speaking and reading frames; " +
                "adjective and nationality uses are excluded.",
        ),
        "L032-S01" to listOf(
            "Only the complete city surfaces i Aarhus and i Odense, including the exact rail example skifter i Aarhus; " +
                "no productive preposition rule.",
        ),
        "L034-S01" to listOf(
            "Personal wish, intention or polite preference in the authored modal and polite-request frames; " +
                "prediction, other request uses and past forms are excluded.",
        ),
        "L037-S01" to listOf("Current calendar day in the authored final-time and work examples; the meaning ‘nowadays’ is excluded."),
        "L038-S01" to listOf("Temporal now in the authored time examples; discourse and conjunction uses are excluded."),
        "L045-S01" to listOf("Drinking is limited to the authored coffee, water and milk examples."),
        "L048-S01" to listOf("Static at-home location only; directional hjem is a separate entry and sense."),
        "L068-S01" to listOf("Whole destination chunk; no productive preposition or definiteness rule."),
        "L069-S01" to listOf("Directional home destination only; static hjemme is a separate entry and sense."),
        "L072-S01" to listOf("Transport-vehicle train sense and the supplied rail scene only."),
        "L074-S01" to listOf("Vehicular travel selected by the authored car scene; bicycle and rail readings are excluded."),
        "L076-S01" to listOf("Whole static-location chunk; no productive preposition or definiteness rule."),
        "L077-S01" to listOf("Changing trains or connections in the authored rail scene, in the exact surface skifter i Aarhus."),
        "L079-S01" to listOf("Whole pharmacy destination chunk; no productive preposition or definiteness rule."),
    )

    private fun lesson(focusId: String, title: String) = VocabularyLibraryLesson(focusId, title)
    private val LESSONS = buildMap<String, List<VocabularyLibraryLesson>> {
        fun link(ids: List<String>, focus: String, title: String) = ids.forEach { id ->
            put(id, get(id).orEmpty() + lesson(focus, title))
        }
        link(listOf("L001-S01", "L002-S01", "L003-S01", "L004-S01"), "module-one-singular", "Talking about one person")
        link(listOf("L005-S01", "L006-S01", "L007-S01", "L013-S01"), "module-one-groups", "Talking to and about groups")
        link(listOf("L008-S01", "L010-S01", "L014-S01", "L018-S01", "L025-S01"), "module-one-er", "Using the verb to be")
        link(listOf("L008-S02", "L026-S01"), "module-one-things", "Referring to one thing")
        link(listOf("L011-S01", "L013-S01", "L027-S01"), "module-one-present", "Using present verb forms")
        link(listOf("L009-S01", "L014-S01", "L022-S01"), "module-one-possession", "Saying what someone has")
        link(listOf("L014-S01", "L015-S01", "L018-S01", "L024-S01"), "module-one-reading-words", "Words for things you read")
        link(listOf("L028-S01", "L029-S01", "L030-S01", "L031-S01", "L032-S01"), "module-one-personal", "Names, homes and languages")
        link(listOf("L011-S01", "L033-S01"), "module-one-ability", "Using a verb after kan")
        link(listOf("L034-S01"), "module-one-volition", "Expressing an intention")
        link(listOf("L035-S01"), "module-one-obligation", "Understanding a requirement")
        link(listOf("L036-S01"), "module-one-permission", "Understanding permission")
        link(listOf("L032-S01", "L037-S01", "L038-S01"), "module-one-circumstances", "Adding when and where")
        link(listOf("L039-S01", "L040-S01"), "module-one-questions", "Asking yes or no questions")
        P1_LESSONS.forEachIndexed { index, (title, ids) -> link(ids, "vocabulary-p1-s${index + 1}", title) }
        P2_LESSONS.forEachIndexed { index, (title, ids) -> link(ids, "vocabulary-p2-s${index + 1}", title) }
        P3_LESSONS.forEachIndexed { index, (title, ids) -> link(ids, "vocabulary-p3-s${index + 1}", title) }
    }

    private val P1_LESSONS get() = listOf(
        "Work, reading and today" to listOf("L027-S01", "L011-S01", "L014-S01", "L037-S01"),
        "Reading and writing" to listOf("L015-S01", "L018-S01", "L041-S01", "L042-S01"),
        "Study, courses and Danish" to listOf("L043-S01", "L044-S01", "L031-S01", "L038-S01"),
        "Coffee and water" to listOf("L045-S01", "L046-S01", "L047-S01"),
        "Where and when" to listOf("L032-S01", "L048-S01", "L049-S01"),
    )
    private val P2_LESSONS get() = listOf(
        "A small breakfast" to listOf("L012-S01", "L021-S01", "L050-S01", "L051-S01"),
        "Drinks on the table" to listOf("L021-S01", "L045-S01", "L046-S01", "L047-S01", "L050-S01", "L052-S01"),
        "Getting attention and receiving a roll" to listOf("L012-S01", "L050-S01", "L053-S01", "L054-S01", "L055-S01", "L056-S01"),
        "Buying and paying" to listOf("L012-S01", "L045-S01", "L055-S01", "L057-S01", "L058-S01", "L059-S01"),
        "Marking a polite wish" to listOf("L034-S01", "L053-S01", "L054-S01", "L055-S01", "L058-S01", "L060-S01", "L061-S01"),
        "Requesting a supplied quantity" to listOf(
            "L009-S02", "L034-S01", "L046-S01", "L047-S01", "L060-S01", "L061-S01", "L062-S01", "L063-S01",
        ),
    )
    private val P3_LESSONS get() = listOf(
        "Two ways to travel" to listOf("L008-S01", "L009-S01", "L016-S01", "L038-S01", "L064-S01", "L065-S01"),
        "Walking to the station and home" to listOf("L008-S01", "L066-S01", "L067-S01", "L068-S01", "L069-S01"),
        "City, home and library" to listOf("L008-S01", "L027-S01", "L029-S01", "L032-S01", "L048-S01", "L070-S01"),
        "Ticket, bus and train" to listOf("L008-S01", "L009-S01", "L071-S01", "L072-S01", "L073-S01"),
        "Waiting at the station" to listOf(
            "L016-S01", "L065-S01", "L068-S01", "L069-S01", "L074-S01", "L075-S01", "L076-S01",
        ),
        "A change and the pharmacy" to listOf(
            "L032-S01", "L067-S01", "L069-S01", "L072-S01", "L075-S01", "L076-S01", "L077-S01", "L078-S01",
            "L079-S01",
        ),
    )

    private fun example(id: String, da: String, en: String?, scene: String? = null) =
        VocabularyLibraryExample(id, da, en, scene)
    private val EXAMPLES = mapOf(
        "L008-S01" to listOf(example("library-example-identification-book", "Det er en bog.", "This is a book.")),
        "L009-S01" to listOf(example("library-example-possession-book", "Jeg har en bog.", "I have a book.")),
        "L009-S02" to listOf(
            example(
                "library-example-request-coffee", "Jeg vil gerne have en kop kaffe, tak.",
                "I would like a cup of coffee, thank you.",
            ),
        ),
        "L011-S01" to listOf(example("library-example-reading-book", "Hun læser en bog.", "She reads a book.")),
        "L012-S01" to listOf(example("library-example-eating-bread", "Nora spiser brød.", "Nora eats bread.")),
        "L027-S01" to listOf(example("library-example-working-today", "Anna arbejder i dag.", "Anna works today.")),
        "L032-S01" to listOf(
            example(
                "library-example-rail-city", "Anna skifter i Aarhus.", "Anna changes trains/connections in Aarhus.",
                "Rail context: This journey is by train.",
            ),
        ),
        "L041-S01" to listOf(example("library-example-writing-letter", "Nora skriver et brev.", "Nora writes a letter.")),
        "L043-S01" to listOf(example("library-example-studying", "Nora studerer.", "Nora studies.")),
        "L045-S01" to listOf(example("library-example-drinking-milk", "Nora drikker mælk.", "Nora drinks milk.")),
        "L048-S01" to listOf(example("library-example-static-home", "Nora arbejder hjemme.", "Nora works at home.")),
        "L058-S01" to listOf(example("library-example-buying-roll", "Jeg køber en bolle i en café.", "I buy a bread roll in a café.")),
        "L059-S01" to listOf(example("library-example-paying", "Jeg betaler.", "I pay.")),
        "L064-S01" to listOf(example("library-example-bicycle-display", "Det er en cykel.", "This is a bicycle.", "Bicycle display")),
        "L065-S01" to listOf(example("library-example-cycling-now", "Nora cykler nu.", "Nora is travelling by bicycle now.")),
        "L067-S01" to listOf(example("library-example-walking-station", "Anna går til stationen.", "Anna walks to the station.")),
        "L068-S01" to listOf(example("library-example-destination-station", "Anna går til stationen.", "Anna walks to the station.")),
        "L069-S01" to listOf(example("library-example-homeward", "Nora går hjem.", "Nora walks home.")),
        "L072-S01" to listOf(example("library-example-train-display", "Det er et tog.", "This is a train.", "Vehicle B — train display")),
        "L074-S01" to listOf(
            example(
                "library-example-driving-home", "Nora kører hjem.", "Nora travels/drives home by vehicle.",
                "Car context: This journey is by car.",
            ),
        ),
        "L075-S01" to listOf(example("library-example-waiting-station", "Anna venter på stationen.", "Anna waits at the station.")),
        "L076-S01" to listOf(example("library-example-static-station", "Anna venter på stationen.", "Anna waits at the station.")),
        "L077-S01" to listOf(
            example(
                "library-example-changing-rail", "Anna skifter i Aarhus.", "Anna changes trains/connections in Aarhus.",
                "Rail context: This journey is by train.",
            ),
        ),
        "L079-S01" to listOf(example("library-example-destination-pharmacy", "Nora går til apoteket.", "Nora walks to the pharmacy.")),
    )
}
