@file:Suppress("MagicNumber", "MaxLineLength")

package app.astrid.engine

import java.util.Locale

internal data class VocabularyP3Lexeme(
    val entryId: String,
    val senseId: String,
    val kind: VocabularyLexicalKind,
    val forms: VocabularyLexicalForms,
    val gloss: String,
    val selectedUse: String,
) {
    val display: String get() = forms.present ?: forms.bundle ?: forms.form ?: error("Missing P3 display form")
    fun details() = VocabularyLexicalSourceDetails(entryId, senseId, kind, forms, gloss, selectedUse)
    fun target(direction: String = "da-en") = LexicalTarget(entryId, senseId, "meaning", direction)
}

internal data class VocabularyP3Scene(val id: String, val text: String, val exposureKeys: List<String>)

internal data class VocabularyP3SpatialMeaning(
    val form: String,
    val lexicalSource: VocabularyP3Lexeme,
    val classification: String,
)

internal data class VocabularyP3Fact(
    val id: String,
    val referentId: String,
    val referentDisplay: String,
    val referentAnswer: String,
    val predicate: VocabularyP3Lexeme,
    val actionAnswer: String,
    val complementSource: VocabularyP3Lexeme? = null,
    val complementRole: String? = null,
    val complementAnswer: String? = null,
    val sentence: String,
    val translation: String,
    val constructionId: String,
    val requiredSceneId: String? = null,
    val exposureKeys: List<String>,
)

internal enum class VocabularyP3TaskKind { CHOICE, MATCHING, RECALL, TILES, READING, CONTRAST }

internal data class VocabularyP3Tile(val text: String, val lexical: VocabularyP3Lexeme? = null)

internal data class VocabularyP3Task(
    val id: String,
    val formatId: String,
    val kind: VocabularyP3TaskKind,
    val cue: String,
    val choices: List<String> = emptyList(),
    val expected: String = "",
    val targets: List<VocabularyP3Lexeme> = emptyList(),
    val matching: List<Pair<String, VocabularyP3Lexeme>> = emptyList(),
    val passage: List<String> = emptyList(),
    val passageLabels: List<PassageLabel> = emptyList(),
    val facts: List<VocabularyP3Fact> = emptyList(),
    val targetFactId: String? = null,
    val questionRole: String? = null,
    val tiles: List<VocabularyP3Tile> = emptyList(),
    val targetScopeId: String? = null,
    val scene: VocabularyP3Scene? = null,
    val contrastId: String? = null,
    val spatialMeanings: List<VocabularyP3SpatialMeaning> = emptyList(),
    val choiceMappings: List<ChoiceMapping> = emptyList(),
)

internal object VocabularyP3Content {
    const val CONTENT_REVISION = "pilot-20"
    const val GENERATOR_REVISION = "generator-20"
    const val EVALUATION_REVISION = "evaluation-20"

    val car = count("L016", "L016-S01", "en bil", "a car", "one car in the reviewed possession and car-scene support")
    val now = fixed("L038", "L038-S01", "nu", "now", "final current-time constituent")
    val staticHome = adverb("L048", "L048-S01", "hjemme", "at home", "static home location")
    val live = verb("L029", "L029-S01", "bo", "bor", "lives/resides", "human residence in a supplied city")
    val city = fixed("L032", "L032-S01", "i Odense", "in Odense", "supplied named-city static location")
    private val cityAarhus = fixed("L032", "L032-S01", "i Aarhus", "in Aarhus", "supplied named-city static location")
    val bicycle = count("L064", "L064-S01", "en cykel", "a bicycle", "one bicycle in identification")
    val cycle = verb("L065", "L065-S01", "cykle", "cykler", "travels by bicycle", "human bicycle travel with final nu")
    val station = count("L066", "L066-S01", "en station", "a station", "transport station identification")
    val walk = verb("L067", "L067-S01", "gå", "går", "walks/goes on foot", "human walking in packet destinations")
    val toStation = fixed("L068", "L068-S01", "til stationen", "to the station", "whole destination chunk")
    val homeward = adverb("L069", "L069-S01", "hjem", "home/homeward", "directional home destination")
    val library = count("L070", "L070-S01", "et bibliotek", "a library", "library identification")
    val bus = count("L071", "L071-S01", "en bus", "a bus", "bus identification")
    val train = count("L072", "L072-S01", "et tog", "a train", "transport train identification and rail support")
    val ticket = count("L073", "L073-S01", "en billet", "a ticket", "travel-ticket possession")
    val drive = verb("L074", "L074-S01", "køre", "kører", "travels/drives by vehicle", "human vehicular travel in a car scene")
    val wait = verb("L075", "L075-S01", "vente", "venter", "waits/is waiting", "waiting in the static station frame")
    val atStation = fixed("L076", "L076-S01", "på stationen", "at the station", "whole static-location chunk")
    val change = verb("L077", "L077-S01", "skifte", "skifter", "changes trains/connections", "rail change in Aarhus")
    val pharmacy = count("L078", "L078-S01", "et apotek", "a pharmacy", "pharmacy identification")
    val toPharmacy = fixed("L079", "L079-S01", "til apoteket", "to the pharmacy", "whole destination chunk")
    private val haveLexeme = verb("L009", "L009-S01", "have", "har", "has", "possession in exact reviewed fact")
    private val identifyLexeme = verb("L010", "L010-S01", "være", "er", "is", "fixed Det er identification")
    private val workLexeme = verb("L027", "L027-S01", "arbejde", "arbejder", "works", "human work in reviewed frame")

    val newLexemes = listOf(
        bicycle, cycle, station, walk, toStation, homeward, library, bus, train, ticket, drive, wait, atStation, change,
        pharmacy, toPharmacy,
    )
    val placements = listOf(car, bicycle, cycle, now, station, walk, toStation, homeward, staticHome, live, city, library,
        bus, train, ticket, drive, wait, atStation, change, pharmacy, toPharmacy)

    val carScene = VocabularyP3Scene(
        "p3-assessment-car", "Car context: This journey is by car.", listOf("vocabulary-p3:scene:car-assessment"),
    )
    val railScene = VocabularyP3Scene(
        "p3-assessment-rail", "Rail context: This journey is by train.", listOf("vocabulary-p3:scene:rail-assessment"),
    )

    private val s1Facts = listOf(
        fact("p3-s1-f1", "p3:Anna", "Anna", "Anna", haveLexeme, "has", car, "object", "a car",
            "Anna har en bil.", "Anna has a car.", "p3-possession"),
        fact("p3-s1-f2", "p3:display-bicycle", "Bicycle display", "the bicycle display", identifyLexeme, "identifies",
            bicycle, "object", "a bicycle", "Det er en cykel.", "This is a bicycle.", "p3-identification"),
        fact("p3-s1-f3", "p3:Nora", "Nora", "Nora", cycle, "travels by bicycle", now, "time", "now",
            "Nora cykler nu.", "Nora is travelling by bicycle now.", "p3-cycle-now"),
    )
    private val s2Facts = listOf(
        fact("p3-s2-f1", "p3:display-station", "Display", "the display", identifyLexeme, "identifies", station, "object",
            "a station", "Det er en station.", "This is a station.", "p3-identification"),
        fact("p3-s2-f2", "p3:Anna", "Anna", "Anna", walk, "walks", toStation, "destination", "to the station",
            "Anna går til stationen.", "Anna walks to the station.", "p3-walk-destination"),
        fact("p3-s2-f3", "p3:Nora", "Nora", "Nora", walk, "walks", homeward, "destination", "home",
            "Nora går hjem.", "Nora walks home.", "p3-walk-home"),
    )
    private val s3Facts = listOf(
        fact("p3-s3-f1", "p3:Anna", "Anna", "Anna", live, "lives", city, "static-location", "in Odense",
            "Anna bor i Odense.", "Anna lives in Odense.", "p3-residence-city"),
        fact("p3-s3-f2", "p3:Nora", "Nora", "Nora", workLexeme, "works", staticHome, "static-location", "at home",
            "Nora arbejder hjemme.", "Nora works at home.", "p3-work-home"),
        fact("p3-s3-f3", "p3:display-library", "Display", "the display", identifyLexeme, "identifies", library, "object",
            "a library", "Det er et bibliotek.", "This is a library.", "p3-identification"),
    )
    private val s4Facts = listOf(
        fact("p3-s4-f1", "p3:Anna", "Anna", "Anna", haveLexeme, "has", ticket, "object", "a ticket",
            "Anna har en billet.", "Anna has a ticket.", "p3-possession"),
        fact("p3-s4-f2", "p3:vehicle-a", "Vehicle A", "Vehicle A", identifyLexeme, "identifies", bus, "object", "a bus",
            "Det er en bus.", "This is a bus.", "p3-identification"),
        fact("p3-s4-f3", "p3:vehicle-b", "Vehicle B", "Vehicle B", identifyLexeme, "identifies", train, "object", "a train",
            "Det er et tog.", "This is a train.", "p3-identification"),
    )
    private val s5Facts = listOf(
        fact("p3-s5-f1", "p3:Anna", "Anna", "Anna", wait, "waits", atStation, "static-location", "at the station",
            "Anna venter på stationen.", "Anna waits at the station.", "p3-wait-station"),
        fact("p3-s5-f2", "p3:Nora", "Nora", "Nora", drive, "travels by vehicle", homeward, "destination", "home",
            "Nora kører hjem.", "Nora travels/drives home by vehicle.", "p3-drive-home", carScene.id),
    )
    private val s6Facts = listOf(
        fact("p3-s6-f1", "p3:Anna", "Anna", "Anna", change, "changes trains/connections", cityAarhus, "static-location", "in Aarhus",
            "Anna skifter i Aarhus.", "Anna changes trains/connections in Aarhus.", "p3-rail-change", railScene.id),
        fact("p3-s6-f2", "p3:Nora", "Nora", "Nora", walk, "walks", toPharmacy, "destination", "to the pharmacy",
            "Nora går til apoteket.", "Nora walks to the pharmacy.", "p3-walk-pharmacy"),
        fact("p3-s6-f3", "p3:Anna", "Anna", "Anna", walk, "walks", homeward, "destination", "home",
            "Anna går hjem.", "Anna walks home.", "p3-walk-home"),
    )

    val tasks = listOf(
        choice("p3-1.1", "E045", "en cykel", listOf("a bicycle", "a car", "a station"), "a bicycle", bicycle),
        matching("p3-1.2", listOf("a car" to car, "a bicycle" to bicycle, "travels by bicycle" to cycle)),
        reading("p3-1.3", "Who is travelling by bicycle now?", listOf("Nora", "Anna"), s1Facts, "p3-s1-f3", "subject",
            labels = listOf(PassageLabel(1, "Bicycle display"))),
        tiles("p3-1.4", "Write Nora is travelling by bicycle now. Begin with Nora.",
            listOf(tile("Nora"), tile("cykler", cycle), tile("nu", now)), "p3-subject-first-supplied"),

        matching("p3-2.1", listOf("a station" to station, "walks/goes on foot" to walk,
            "to the station" to toStation, "home/homeward" to homeward)),
        reading("p3-2.2", "Where is Anna going?", listOf("to the station", "home"), s2Facts, "p3-s2-f2", "destination"),
        contrast("p3-2.3", "Nora går hjem.", listOf("destination/direction", "static location"),
            "destination/direction", "p3-home-direction", listOf(homeward), "p3-home-direction",
            listOf(VocabularyP3SpatialMeaning("hjem", homeward, "destination"))),
        tiles("p3-2.4", "Write Anna walks to the station. Begin with Anna.",
            listOf(tile("Anna"), tile("går", walk), tile("til stationen", toStation)), "p3-subject-first-supplied"),

        matching("p3-3.1", listOf("lives/resides" to live, "in Odense" to city, "at home" to staticHome,
            "a library" to library)),
        reading("p3-3.2", "Where does Anna live?", listOf("in Odense", "at home", "at the station"),
            s3Facts, "p3-s3-f1", "static-location"),
        reading("p3-3.3", "Where does Nora work?", listOf("at home", "in Odense"), s3Facts, "p3-s3-f2", "static-location"),
        pairedContrast("p3-3.4", "p3-home-spatial-contrast", HOME_CORRECT, HOME_REVERSED,
            listOf(staticHome to "static-location", homeward to "destination")),

        matching("p3-4.1", listOf("a bus" to bus, "a train" to train, "a ticket" to ticket)),
        reading("p3-4.2", "What does Anna have?", listOf("a ticket", "a bus", "a train"), s4Facts, "p3-s4-f1", "object",
            labels = listOf(PassageLabel(1, "Vehicle A"), PassageLabel(2, "Vehicle B"))),
        choice("p3-4.3", "E046", "Choose the introduced bundle for a train.", listOf("en bus", "et tog", "en billet"),
            "et tog", train),

        matching("p3-5.1", listOf("travels/drives by vehicle" to drive, "waits" to wait, "at the station" to atStation), carScene),
        reading("p3-5.2", "Where does Anna wait?", listOf("at the station", "to the station"), s5Facts,
            "p3-s5-f1", "static-location", carScene),
        pairedContrast("p3-5.3", "p3-station-spatial-contrast", STATION_CORRECT, STATION_REVERSED,
            listOf(toStation to "destination", atStation to "static-location")),
        recall("p3-5.4", "at køre", drive, carScene),

        matching("p3-6.1", listOf("changes trains/connections" to change, "a pharmacy" to pharmacy,
            "to the pharmacy" to toPharmacy), railScene),
        reading("p3-6.2", "What does Anna do in Aarhus?", listOf("changes trains/connections", "waits at the station", "walks home"),
            s6Facts, "p3-s6-f1", "action", railScene),
        reading("p3-6.3", "Where does Nora walk?", listOf("to the pharmacy", "home"), s6Facts,
            "p3-s6-f2", "destination", railScene),
        tiles("p3-6.4", "Write I walk to the pharmacy. Begin with the subject.",
            listOf(tile("Jeg"), tile("går", walk), tile("til apoteket", toPharmacy)), "p3-subject-first-supplied"),
    )

    val formats = listOf(
        ExerciseFormat("E045", "Recognize an introduced P3 Danish meaning", null, LexicalTask("meaning", "da-en")),
        ExerciseFormat("E046", "Choose an introduced P3 Danish bundle", null, LexicalTask("meaning", "en-da")),
        ExerciseFormat("E047", "Match introduced P3 meanings and forms", null, LexicalTask("meaning", "en-da")),
        ExerciseFormat("E048", "Recall an introduced P3 present form", "G004"),
        ExerciseFormat("E049", "Build a reviewed P3 sentence", "G005"),
        ExerciseFormat("E050", "Read an explicit P3 fact", null, readingTask = ReadingTask("explicit-fact")),
        ExerciseFormat("E051", "Choose a bounded P3 spatial meaning", "G016"),
    )
    val formatIds = formats.map(ExerciseFormat::id).toSet()

    init {
        require(tasks.size == 23 && tasks.map(VocabularyP3Task::id).distinct().size == tasks.size)
        require(tasks.map(VocabularyP3Task::formatId).toSet() == formatIds)
        tasks.forEach(::validate)
    }

    fun tasks(formatId: String) = tasks.filter { it.formatId == formatId }

    internal fun validate(task: VocabularyP3Task) {
        require(task.scene == null || task.facts.filter { it.requiredSceneId != null }.all { it.requiredSceneId == task.scene.id })
        when (task.kind) {
            VocabularyP3TaskKind.CHOICE -> require(task.targets.size == 1 && task.expected in task.choices)
            VocabularyP3TaskKind.MATCHING -> require(task.matching.size in 3..4)
            VocabularyP3TaskKind.RECALL -> require(task.targets.size == 1 && task.formatId == "E048")
            VocabularyP3TaskKind.TILES -> require(task.tiles.isNotEmpty() && task.targetScopeId == "p3-subject-first-supplied")
            VocabularyP3TaskKind.READING -> require(task.questionRole in setOf("subject", "object", "action", "destination", "static-location"))
            VocabularyP3TaskKind.CONTRAST -> require(task.contrastId != null && task.expected in task.choices)
        }
    }

    private fun choice(id: String, format: String, cue: String, choices: List<String>, expected: String,
        target: VocabularyP3Lexeme) = VocabularyP3Task(
        id, format, VocabularyP3TaskKind.CHOICE, cue, choices, expected, listOf(target),
        targetScopeId = null,
    )
    private fun matching(id: String, pairs: List<Pair<String, VocabularyP3Lexeme>>, scene: VocabularyP3Scene? = null) =
        VocabularyP3Task(id, "E047", VocabularyP3TaskKind.MATCHING,
            "Match each English meaning to its introduced Danish form.", targets = pairs.map { it.second }, matching = pairs, scene = scene)
    private fun recall(id: String, cue: String, target: VocabularyP3Lexeme, scene: VocabularyP3Scene) = VocabularyP3Task(
        id, "E048", VocabularyP3TaskKind.RECALL, cue, expected = requireNotNull(target.forms.present), targets = listOf(target),
        targetScopeId = "p3-kore-present", scene = scene,
    )
    private fun tiles(id: String, cue: String, values: List<VocabularyP3Tile>, scope: String) = VocabularyP3Task(
        id, "E049", VocabularyP3TaskKind.TILES, cue, tiles = values, targetScopeId = scope,
    )
    private fun reading(id: String, cue: String, choices: List<String>, facts: List<VocabularyP3Fact>, targetFact: String,
        role: String, scene: VocabularyP3Scene? = null, labels: List<PassageLabel> = emptyList()) = VocabularyP3Task(
        id, "E050", VocabularyP3TaskKind.READING, cue, choices = choices, passage = facts.map(VocabularyP3Fact::sentence),
        passageLabels = labels, facts = facts, targetFactId = targetFact, questionRole = role, scene = scene,
    )
    private fun contrast(id: String, cue: String, choices: List<String>, expected: String, contrastId: String,
        targets: List<VocabularyP3Lexeme>, scope: String, meanings: List<VocabularyP3SpatialMeaning>) = VocabularyP3Task(
        id, "E051", VocabularyP3TaskKind.CONTRAST, cue, choices, expected, targets, targetScopeId = scope, contrastId = contrastId,
        spatialMeanings = meanings,
    )
    private fun pairedContrast(id: String, contrastId: String, correct: String, reversed: String,
        meanings: List<Pair<VocabularyP3Lexeme, String>>): VocabularyP3Task {
        val spatial = meanings.map { VocabularyP3SpatialMeaning(it.first.display, it.first, it.second) }
        val labels = if (contrastId == "p3-home-spatial-contrast") {
            mapOf("static-location" to "static at home", "destination" to "homeward destination")
        } else {
            mapOf("static-location" to "static location", "destination" to "destination")
        }
        val mappings = listOf(
            ChoiceMapping(correct, spatial.map { BilingualMapping(it.form, labels.getValue(it.classification)) }),
            ChoiceMapping(reversed, spatial.reversed().mapIndexed { index, value ->
                BilingualMapping(spatial[index].form, labels.getValue(value.classification))
            }),
        )
        return VocabularyP3Task(
            id, "E051", VocabularyP3TaskKind.CONTRAST, "Choose the exact taught mapping.", listOf(correct, reversed), correct,
            meanings.map { it.first }, targetScopeId = contrastId, contrastId = contrastId,
            spatialMeanings = spatial, choiceMappings = mappings,
        )
    }
    private fun tile(text: String, lexical: VocabularyP3Lexeme? = null) = VocabularyP3Tile(text, lexical)
    @Suppress("LongParameterList")
    private fun fact(id: String, referentId: String, display: String, answer: String, predicate: VocabularyP3Lexeme,
        action: String, complement: VocabularyP3Lexeme?, role: String?, complementAnswer: String?, sentence: String,
        translation: String, construction: String, sceneId: String? = null) = VocabularyP3Fact(
        id, referentId, display, answer, predicate, action, complement, role, complementAnswer, sentence, translation,
        construction, sceneId, listOf("vocabulary-p3:fact:$id") +
            listOfNotNull(role?.takeIf { it in setOf("destination", "static-location") }
                ?.let { "vocabulary-p3:spatial:$id:$it" }),
    )
    private fun verb(id: String, sense: String, infinitive: String, present: String, gloss: String, use: String) =
        VocabularyP3Lexeme(id, sense, VocabularyLexicalKind.VERB_FORM, VocabularyLexicalForms(infinitive, present), gloss, use)
    private fun count(id: String, sense: String, bundle: String, gloss: String, use: String) =
        VocabularyP3Lexeme(id, sense, VocabularyLexicalKind.COUNT_BUNDLE, VocabularyLexicalForms(bundle = bundle), gloss, use)
    private fun fixed(id: String, sense: String, form: String, gloss: String, use: String) =
        VocabularyP3Lexeme(id, sense, VocabularyLexicalKind.FIXED_CHUNK, VocabularyLexicalForms(form = form), gloss, use)
    private fun adverb(id: String, sense: String, form: String, gloss: String, use: String) =
        VocabularyP3Lexeme(id, sense, VocabularyLexicalKind.ADVERB, VocabularyLexicalForms(form = form), gloss, use)

    private const val HOME_CORRECT = "hjemme = static at home; hjem = homeward destination"
    private const val HOME_REVERSED = "hjemme = homeward destination; hjem = static at home"
    private const val STATION_CORRECT = "til stationen = destination; på stationen = static location"
    private const val STATION_REVERSED = "til stationen = static location; på stationen = destination"
}

internal object VocabularyP3ExerciseGenerator {
    fun generate(request: GenerationRequest): GeneratedExercise {
        val candidates = VocabularyP3Content.tasks(request.formatId)
        val index = Math.floorMod(request.seed.toLong() + request.index.toLong(), candidates.size.toLong()).toInt()
        val task = candidates[index]
        return when (task.kind) {
            VocabularyP3TaskKind.MATCHING -> matching(request, task)
            VocabularyP3TaskKind.TILES -> tiles(request, task)
            else -> choice(request, task)
        }
    }

    private fun choice(request: GenerationRequest, task: VocabularyP3Task): GeneratedExercise {
        val choices = stableShuffle(task.choices, request.seed.toLong() xor task.id.hashCode().toLong()) { it }
        val expectedAnswer = requireNotNull(expectedAnswer(task))
        return exercise(request, task, Materials(
            ExercisePrompt(
                "", "", choices = choices, cue = task.cue,
                cueLanguage = cueLanguage(task),
                answerLanguage = if (task.kind == VocabularyP3TaskKind.RECALL || task.formatId == "E046") "da" else "en",
                passage = task.passage.takeIf(List<String>::isNotEmpty), passageLabels = task.passageLabels,
                choiceMappings = choices.mapNotNull { choice -> task.choiceMappings.singleOrNull { it.choice == choice } },
                inputLabel = if (task.kind == VocabularyP3TaskKind.RECALL) "Danish present form" else null,
            ),
            AnswerKey.MeaningChoice(expectedAnswer, task.choices.toSet()),
            lexicalTarget = task.targets.singleOrNull()?.takeUnless {
                task.kind in setOf(VocabularyP3TaskKind.READING, VocabularyP3TaskKind.CONTRAST, VocabularyP3TaskKind.RECALL)
            }?.target(if (task.formatId == "E046") "en-da" else "da-en"),
            readingTarget = if (task.kind == VocabularyP3TaskKind.READING) ReadingTask("explicit-fact") else null,
        ))
    }

    private fun matching(request: GenerationRequest, task: VocabularyP3Task): GeneratedExercise {
        val left = stableShuffle(task.matching.map { it.first }, request.seed.toLong() xor task.id.hashCode().toLong()) { it }
        val right = stableShuffle(task.matching.map { it.second }, request.seed.toLong() xor task.id.hashCode().toLong() xor MATCH_SALT) {
            "${it.entryId}:${it.senseId}"
        }
        val leftItems = left.mapIndexed { index, text -> MatchingItem(opaque("l", request.seed, index, text), text) }
        val rightItems = right.mapIndexed { index, value -> MatchingItem(opaque("r", request.seed, index, value.display), value.display) }
        val leftByText = leftItems.associateBy(MatchingItem::text)
        val rightBySense = right.zip(rightItems).associate { it.first.senseId to it.second }
        val pairs = task.matching.map { (meaning, lexeme) ->
            VocabularyMatchingPairSourceDetails(leftByText.getValue(meaning).id, rightBySense.getValue(lexeme.senseId).id, lexeme.details())
        }
        return exercise(request, task, Materials(
            ExercisePrompt("", "", cue = task.cue, cueLanguage = "en", answerLanguage = "da",
                matchingColumns = MatchingColumns(leftItems, rightItems)),
            AnswerKey.Matching(pairs.associate { it.leftId to it.rightId }),
            lexicalTargets = task.targets.map { it.target("en-da") }, matchingPairs = pairs,
        ))
    }

    private fun tiles(request: GenerationRequest, task: VocabularyP3Task): GeneratedExercise {
        val canonical = task.tiles.mapIndexed { index, tile -> GeneratedTile(index, opaque("b", request.seed, index, tile.text), tile) }
        val displayed = stableShuffle(canonical, request.seed.toLong() xor task.id.hashCode().toLong()) { "${it.index}:${it.id}:${it.tile.text}" }
        val occurrences = canonical.map { generated ->
            VocabularyTileOccurrenceDetails(generated.id, generated.tile.lexical?.entryId ?: "p3:support:${generated.index}",
                generated.tile.text, generated.tile.lexical?.details())
        }
        return exercise(request, task, Materials(
            ExercisePrompt("", "", blocks = displayed.map { OrderBlock(it.id, it.tile.text) }, cue = task.cue,
                cueLanguage = "en", answerLanguage = "da", tileMode = "use-all", requiredTileCount = canonical.size),
            AnswerKey.MeaningChoice("", emptySet()), tileOccurrences = occurrences,
            acceptedOrders = listOf(canonical.map(GeneratedTile::id)),
        ))
    }

    private fun exercise(request: GenerationRequest, task: VocabularyP3Task, materials: Materials): GeneratedExercise {
        val source = details(task, materials)
        val targetSkill = when (task.formatId) { "E048" -> "G004"; "E049" -> "G005"; "E051" -> "G016"; else -> null }
        return GeneratedExercise(
            "${request.contentRevision}:${request.formatId}:${task.id}:${request.seed}:${request.index}", request.formatId, targetSkill,
            materials.lexicalTarget, if (targetSkill == null) emptyList() else listOf("G001", "G002"),
            ExercisePresentation(instruction(task), task.scene?.text ?: "Use only the material introduced in this lesson.",
                materials.prompt, VocabularyP3Teaching.forTask(task.id).sections,
                if (task.kind == VocabularyP3TaskKind.TILES) "Use every supplied block once." else
                    "Use the introduced meaning and scene."),
            request.contentRevision, request.generatorRevision, Reproduction(request.seed, request.index),
            ExerciseInspector(
                constructionId = task.targetScopeId, lexicalIds = source.lexicalSources.map { it.entryId }, clauseRoles = emptyList(),
                canonicalSentence = task.tiles.takeIf(List<VocabularyP3Tile>::isNotEmpty)?.joinToString(" ") { it.text }
                    ?.replaceFirstChar(Char::uppercase)?.plus("."), vocabularySource = source,
            ),
            VocabularyP3ExerciseSource(task, materials.answerKey), materials.readingTarget, materials.lexicalTargets,
        )
    }

    private fun details(task: VocabularyP3Task, materials: Materials) = VocabularyExerciseSourceDetails(
        task.id, task.kind.name.lowercase(Locale.ROOT), task.targets.map(VocabularyP3Lexeme::details).distinctBy { it.entryId to it.senseId },
        expectedAnswer = expectedAnswer(task), suppliedChoices = task.choices,
        supportFacts = task.facts.map { fact ->
            VocabularySupportFactDetails(
                fact.id, fact.referentId, fact.predicate.entryId, when (task.questionRole) {
                    "subject" -> fact.referentId
                    "action" -> fact.predicate.entryId
                    else -> fact.complementSource?.entryId ?: fact.predicate.entryId
                },
                fact.answer(task.questionRole), task.questionRole ?: fact.complementRole ?: "support", fact.sentence, fact.exposureKeys,
                fact.referentDisplay, fact.predicate.details(), translation = fact.translation, actionText = fact.actionAnswer,
                complementSource = fact.complementSource?.details(), complementRole = fact.complementRole,
                constructionId = fact.constructionId, sceneId = fact.requiredSceneId,
            )
        },
        matchingPairs = materials.matchingPairs, tileOccurrences = materials.tileOccurrences,
        acceptedOrders = materials.acceptedOrders, targetSupportFactId = task.targetFactId, questionRole = task.questionRole,
        knownAlternatives = knownAlternatives(task), reviewedOrders = if (task.kind == VocabularyP3TaskKind.TILES) listOf(
            VocabularyReviewedOrderDetails(task.tiles.map { it.text }, TargetStatus.MET, LinguisticStatus.ACCEPTABLE,
                MeaningSatisfaction.MET, ResponseConstraintStatus.MET, null),
        ) else emptyList(),
        scene = task.scene?.let { VocabularySceneDetails(it.id, it.text, it.exposureKeys) }, contrastId = task.contrastId,
        spatialMeanings = task.spatialMeanings.map { VocabularySpatialMeaningDetails(it.form, it.lexicalSource.details(), it.classification) },
    )

    private fun VocabularyP3Fact.answer(role: String?) = when (role) {
        "subject" -> referentAnswer; "action" -> actionAnswer; else -> complementAnswer.orEmpty()
    }
    private fun expectedAnswer(task: VocabularyP3Task): String? = task.expected.takeIf(String::isNotEmpty) ?:
        task.targetFactId?.let { targetFactId ->
            task.facts.single { it.id == targetFactId }.answer(task.questionRole)
        }
    private fun knownAlternatives(task: VocabularyP3Task) = if (task.kind == VocabularyP3TaskKind.RECALL) listOf(
        known("køre", "PRESENT_FORM_REQUIRED"), known("cykler", "VEHICULAR_ACTION_REQUIRED"),
        known("venter", "VEHICULAR_ACTION_REQUIRED"),
    ) else emptyList()
    private fun known(text: String, issue: String) = VocabularyKnownAlternativeDetails(
        text, TargetStatus.NOT_MET, LinguisticStatus.ACCEPTABLE, MeaningSatisfaction.NOT_MET,
        ResponseConstraintStatus.MET, issue,
    )
    private fun instruction(task: VocabularyP3Task) = when (task.kind) {
        VocabularyP3TaskKind.MATCHING -> "Match each English meaning to its introduced Danish form."
        VocabularyP3TaskKind.TILES -> "Arrange all supplied blocks as requested."
        VocabularyP3TaskKind.RECALL ->
            "The person is travelling home by car. Type the introduced present form of this verb."
        else -> "Choose the answer supported by the prompt."
    }
    private fun cueLanguage(task: VocabularyP3Task) = when (task.formatId) {
        "E045", "E048" -> "da"
        "E046", "E050" -> "en"
        "E051" -> if (task.targetScopeId == "p3-home-direction") "da" else "en"
        else -> error("Unsupported P3 choice prompt language: ${task.formatId}")
    }
    private fun <T> stableShuffle(values: List<T>, salt: Long, stableKey: (T) -> String) = values.sortedWith(
        compareBy<T> { shuffleRank(stableKey(it), salt) }.thenBy(stableKey),
    )
    private fun shuffleRank(key: String, salt: Long): Long {
        var mixed = salt xor key.hashCode().toLong() * -7046029254386353131L
        mixed = (mixed xor (mixed ushr 30)) * -4658895280553007687L
        return mixed xor (mixed ushr 27)
    }
    private fun opaque(prefix: String, seed: Int, index: Int, value: String) =
        "$prefix-${(seed.toLong() xor index.toLong() * 0x9e3779b9L xor value.hashCode().toLong()).toULong().toString(16)}"
    private data class GeneratedTile(val index: Int, val id: String, val tile: VocabularyP3Tile)
    private data class Materials(
        val prompt: ExercisePrompt, val answerKey: AnswerKey, val lexicalTarget: LexicalTarget? = null,
        val lexicalTargets: List<LexicalTarget>? = null, val readingTarget: ReadingTask? = null,
        val matchingPairs: List<VocabularyMatchingPairSourceDetails> = emptyList(),
        val tileOccurrences: List<VocabularyTileOccurrenceDetails> = emptyList(), val acceptedOrders: List<List<String>> = emptyList(),
    )
    private const val MATCH_SALT = 0x55667788L
}

internal object VocabularyP3ExerciseEvaluator {
    fun evaluate(exercise: GeneratedExercise, answer: Answer): Evaluation {
        val task = (exercise.source as VocabularyP3ExerciseSource).task
        return when (task.kind) {
            VocabularyP3TaskKind.MATCHING -> matching(exercise, answer, task)
            VocabularyP3TaskKind.TILES -> tiles(exercise, answer, task)
            VocabularyP3TaskKind.RECALL -> recall(answer, task)
            else -> choice(exercise, answer, task)
        }
    }

    private fun choice(exercise: GeneratedExercise, answer: Answer, task: VocabularyP3Task): Evaluation {
        val text = (answer as? Answer.Text)?.text?.let(::normalizeText)
        val supplied = task.choices.associateBy { it.lowercase(Locale.ROOT) }
        val expected = requireNotNull(exercise.inspector.vocabularySource).expectedAnswer.orEmpty()
        return result(answer, NormalizedAnswer.Text(text.orEmpty()), task, when {
            text == null || text.lowercase(Locale.ROOT) !in supplied -> outcome(TargetStatus.UNEVALUABLE,
                LinguisticStatus.UNKNOWN, ResponseConstraintStatus.NOT_MET, MeaningSatisfaction.UNKNOWN,
                "SUPPLIED_CHOICE_REQUIRED", "Choose a supplied answer.")
            text.equals(expected, true) -> outcome(TargetStatus.MET, LinguisticStatus.ACCEPTABLE,
                ResponseConstraintStatus.MET, MeaningSatisfaction.MET, null, "Correct.")
            else -> outcome(TargetStatus.NOT_MET, LinguisticStatus.ACCEPTABLE,
                ResponseConstraintStatus.MET, MeaningSatisfaction.NOT_MET, "SUPPLIED_ANSWER_INCORRECT",
                "That supplied answer is not supported by the prompt.")
        })
    }

    private fun recall(answer: Answer, task: VocabularyP3Task): Evaluation {
        val text = (answer as? Answer.Text)?.text?.let(::normalizeText)
        val oneWord = text != null && text.isNotEmpty() && text.none(Char::isWhitespace)
        val value = when {
            text == task.expected -> outcome(TargetStatus.MET, LinguisticStatus.ACCEPTABLE,
                ResponseConstraintStatus.MET, MeaningSatisfaction.MET, null, "Correct.")
            text == "køre" -> outcome(TargetStatus.NOT_MET, LinguisticStatus.ACCEPTABLE,
                if (oneWord) ResponseConstraintStatus.MET else ResponseConstraintStatus.NOT_MET,
                MeaningSatisfaction.NOT_MET, "PRESENT_FORM_REQUIRED", "Use the introduced present form, not the infinitive.")
            text in setOf("cykler", "venter") -> outcome(TargetStatus.NOT_MET, LinguisticStatus.ACCEPTABLE,
                ResponseConstraintStatus.MET, MeaningSatisfaction.NOT_MET, "VEHICULAR_ACTION_REQUIRED",
                "Use the introduced present form for travel by car.")
            else -> outcome(TargetStatus.NOT_MET, LinguisticStatus.UNKNOWN,
                if (oneWord) ResponseConstraintStatus.MET else ResponseConstraintStatus.NOT_MET,
                MeaningSatisfaction.UNKNOWN, if (oneWord) "UNRECOGNIZED_P3_FORM" else "ONE_WORD_REQUIRED",
                if (oneWord) "That form is outside the reviewed material." else "Type one Danish word.")
        }
        return result(answer, NormalizedAnswer.Text(text.orEmpty()), task, value)
    }

    @Suppress("CyclomaticComplexMethod")
    private fun matching(exercise: GeneratedExercise, answer: Answer, task: VocabularyP3Task): Evaluation {
        val pairs = (answer as? Answer.Matching)?.pairs ?: emptyList()
        val columns = requireNotNull(exercise.prompt.matchingColumns)
        val valid = pairs.size <= task.matching.size && pairs.map { it.leftId }.distinct().size == pairs.size &&
            pairs.map { it.rightId }.distinct().size == pairs.size && pairs.all { pair ->
                columns.left.any { it.id == pair.leftId } && columns.right.any { it.id == pair.rightId }
            }
        if (!valid) return result(answer, NormalizedAnswer.Matching(pairs), task,
            outcome(TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN, ResponseConstraintStatus.NOT_MET,
                MeaningSatisfaction.UNKNOWN, "MATCHING_SELECTION_INVALID", "Use supplied IDs at most once."))
        val source = requireNotNull(exercise.inspector.vocabularySource)
        val targets = requireNotNull(exercise.lexicalTargets).associateBy(LexicalTarget::senseId)
        val selected = pairs.associate { it.leftId to it.rightId }
        val rows = source.matchingPairs.map { expected ->
            val right = selected[expected.leftId]; val target = targets.getValue(expected.lexicalSource.senseId)
            when {
                right == null -> MatchingPairResult(expected.leftId, null, target, TargetStatus.UNEVALUABLE,
                    MeaningSatisfaction.UNKNOWN, ResponseConstraintStatus.NOT_MET, LinguisticStatus.UNKNOWN, "Choose a form.")
                right == expected.rightId -> MatchingPairResult(expected.leftId, right, target, TargetStatus.MET,
                    MeaningSatisfaction.MET, ResponseConstraintStatus.MET, LinguisticStatus.ACCEPTABLE, "Correct.")
                else -> MatchingPairResult(expected.leftId, right, target, TargetStatus.NOT_MET,
                    MeaningSatisfaction.NOT_MET, ResponseConstraintStatus.MET, LinguisticStatus.ACCEPTABLE, "Another introduced meaning.")
            }
        }
        val complete = pairs.size == task.matching.size; val wrong = rows.any { it.targetStatus == TargetStatus.NOT_MET }
        return Evaluation(
            if (complete && !wrong) TargetStatus.MET else if (complete) TargetStatus.NOT_MET else TargetStatus.UNEVALUABLE,
            if (pairs.isEmpty()) LinguisticStatus.UNKNOWN else LinguisticStatus.ACCEPTABLE, emptyList(),
            if (complete && !wrong) "All supplied meanings are matched." else "Review the pair feedback.", "", answer,
            NormalizedAnswer.Matching(pairs), LinguisticScope.DANISH_MATCHING,
            if (complete) ResponseConstraintStatus.MET else ResponseConstraintStatus.NOT_MET,
            if (complete && !wrong) MeaningSatisfaction.MET else if (wrong) MeaningSatisfaction.NOT_MET else MeaningSatisfaction.UNKNOWN,
            rows,
        )
    }

    private fun tiles(exercise: GeneratedExercise, answer: Answer, task: VocabularyP3Task): Evaluation {
        val ids = (answer as? Answer.Order)?.blockIds ?: emptyList(); val blocks = exercise.prompt.blocks.associateBy(OrderBlock::id)
        val valid = ids.distinct().size == ids.size && ids.all(blocks::containsKey)
        if (!valid) return result(answer, NormalizedAnswer.Order(ids), task,
            outcome(TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN, ResponseConstraintStatus.NOT_MET,
                MeaningSatisfaction.UNKNOWN, "TILE_SELECTION_INVALID", "Use supplied block IDs at most once."))
        val correct = ids in requireNotNull(exercise.inspector.vocabularySource).acceptedOrders
        val complete = ids.size == task.tiles.size
        return result(answer, NormalizedAnswer.Order(ids), task, if (correct) {
            outcome(TargetStatus.MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.MET, MeaningSatisfaction.MET,
                null, "Correct.", requireNotNull(exercise.inspector.canonicalSentence))
        } else outcome(TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN,
            if (complete) ResponseConstraintStatus.MET else ResponseConstraintStatus.NOT_MET, MeaningSatisfaction.UNKNOWN,
            if (complete) "UNRECOGNIZED_P3_ORDER" else "REQUIRED_WORDS_MISSING",
            if (complete) "This order is outside the reviewed sentence." else "Use every supplied block once."))
    }

    private fun result(raw: Answer, normalized: NormalizedAnswer, task: VocabularyP3Task, value: Outcome) = Evaluation(
        value.target, value.language, value.issue?.let { listOf(EvaluationIssue(it, value.feedback)) }.orEmpty(), value.feedback,
        value.sentence, raw, normalized, when (task.kind) {
            VocabularyP3TaskKind.MATCHING -> LinguisticScope.DANISH_MATCHING
            VocabularyP3TaskKind.TILES -> LinguisticScope.DANISH_SENTENCE
            VocabularyP3TaskKind.RECALL -> LinguisticScope.DANISH_WORD_FORM
            VocabularyP3TaskKind.CHOICE -> if (task.formatId == "E046") LinguisticScope.DANISH_CHOICE else LinguisticScope.ENGLISH_CHOICE
            else -> LinguisticScope.ENGLISH_CHOICE
        }, value.constraint, value.meaning,
    )
    private fun outcome(target: TargetStatus, language: LinguisticStatus, constraint: ResponseConstraintStatus,
        meaning: MeaningSatisfaction, issue: String?, feedback: String, sentence: String = "") =
        Outcome(target, language, constraint, meaning, issue, feedback, sentence)
    private data class Outcome(
        val target: TargetStatus, val language: LinguisticStatus, val constraint: ResponseConstraintStatus,
        val meaning: MeaningSatisfaction, val issue: String?, val feedback: String, val sentence: String = "",
    )
}
