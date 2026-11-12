package app.astrid.engine

import java.util.Locale

internal data class VocabularyP1Lexeme(
    val entryId: String,
    val senseId: String,
    val kind: VocabularyLexicalKind,
    val forms: VocabularyLexicalForms,
    val gloss: String,
    val selectedUse: String,
) {
    val display: String get() = forms.present ?: forms.bundle ?: forms.form ?: error("Missing display form")
    val teachingDisplay: String get() = when {
        forms.infinitive != null -> "at ${forms.infinitive} → ${forms.present}"
        entryId == "L032" -> "i Aarhus / i Odense"
        else -> display
    }
    fun target(facet: String = "meaning", direction: String = "en-da") =
        LexicalTarget(entryId, senseId, facet, direction)
    fun details() = VocabularyLexicalSourceDetails(entryId, senseId, kind, forms, gloss, selectedUse)
}

internal enum class VocabularyP1TaskKind { CHOICE, MATCHING, RECALL, TILES, READING }

internal data class VocabularyP1GrammarTarget(val skillId: String, val scopeId: String, val mode: String)

internal data class VocabularyP1Task(
    val id: String,
    val formatId: String,
    val kind: VocabularyP1TaskKind,
    val cue: String,
    val passage: List<String> = emptyList(),
    val choices: List<String> = emptyList(),
    val expected: String = "",
    val targets: List<VocabularyP1Lexeme> = emptyList(),
    val matching: List<Pair<VocabularyP1Lexeme, Pair<String, String>>> = emptyList(),
    val tileWords: List<Pair<String, String>> = emptyList(),
    val canonicalWords: List<String> = emptyList(),
    val knownOffTargetOrder: List<String>? = null,
    val knownIssueOrder: List<String>? = null,
    val targetSkillId: String? = null,
    val targetScopeId: String? = null,
    val assessmentMode: String? = null,
    val responseShape: String = "choice",
    val factRole: String? = null,
    val supportFacts: List<VocabularyP1Fact> = emptyList(),
    val targetSupportFactId: String? = null,
)

internal data class VocabularyP1Fact(
    val id: String,
    val referentId: String,
    val referentForm: String,
    val predicateId: String,
    val valueId: String,
    val valueRole: String,
    val sentence: String,
    val exposureKeys: List<String>,
)

internal object VocabularyP1Content {
    const val CONTENT_REVISION = "pilot-17"
    const val GENERATOR_REVISION = "generator-17"
    const val EVALUATION_REVISION = "evaluation-17"
    private val P1_RECALL_SHAPES = setOf("present-form", "fixed-chunk", "mass-headword")

    val work = verb("L027", "L027-S01", "arbejde", "arbejder", "works", "human intransitive work")
    val read = verb("L011", "L011-S01", "læse", "læser", "reads", "read introduced written material")
    val book = count("L014", "L014-S01", "en bog", "a book", "common-gender book")
    val today = fixed("L037", "L037-S01", "i dag", "today", "current calendar day")
    val newspaper = count("L015", "L015-S01", "en avis", "a newspaper", "written publication")
    val letter = count("L018", "L018-S01", "et brev", "a written letter", "written message")
    val write = verb("L041", "L041-S01", "skrive", "skriver", "writes", "write a short text; et brev only")
    val computer = count("L042", "L042-S01", "en computer", "a computer", "computer device")
    val study = verb("L043", "L043-S01", "studere", "studerer", "studies (as a student)", "intransitive learner activity")
    val course = count("L044", "L044-S01", "et kursus", "a course", "course or class programme")
    val danish = bareLanguage("L031", "L031-S01", "dansk", "Danish (language)", "bare language noun")
    val now = adverb("L038", "L038-S01", "nu", "now", "current time")
    val drink = verb("L045", "L045-S01", "drikke", "drikker", "drinks", "drink kaffe or vand")
    val coffee = mass("L046", "L046-S01", "kaffe", "coffee", "beverage or substance mass use")
    val water = mass("L047", "L047-S01", "vand", "water", "liquid mass use")
    val city = fixed("L032", "L032-S01", "i Aarhus", "in Aarhus", "supplied i plus named city")
    val home = adverb("L048", "L048-S01", "hjemme", "at home", "static location")
    val morning = fixed("L049", "L049-S01", "om morgenen", "in the morning", "habitual day-part chunk")

    val placements = listOf(
        work, read, book, today, newspaper, letter, write, computer, study, course, danish, now,
        drink, coffee, water, city, home, morning,
    )
    val newLexemes = listOf(write, computer, study, course, drink, coffee, water, home, morning)

    private val sessionOneFacts = listOf(
        fact("p1-s1-f1", "p1:Anna", "Anna", work, today, "time", "Anna arbejder i dag."),
        fact("p1-s1-f2", "p1:Anna", "hun", read, book, "object", "Hun læser en bog."),
    )
    private val sessionTwoFacts = listOf(
        fact("p1-s2-f1", "p1:Nora", "Nora", write, letter, "object", "Nora skriver et brev."),
        fact("p1-s2-f2", "p1:Nora", "hun", "L009", computer, "object", "Hun har en computer.", "har"),
        fact("p1-s2-f3", "p1:Nora", "hun", read, newspaper, "object", "Hun læser en avis."),
    )
    private val sessionThreeFacts = listOf(
        predicateFact("p1-s3-f1", "p1:Nora", "Nora", study, "Nora studerer."),
        fact("p1-s3-f2", "p1:Nora", "hun", read, book, "object", "Hun læser en bog nu.", now),
    )
    private val sessionFourFacts = listOf(
        fact("p1-s4-f1", "p1:Anna", "Anna", drink, coffee, "object", "Anna drikker kaffe."),
        fact("p1-s4-f2", "p1:Nora", "Nora", drink, water, "object", "Nora drikker vand."),
    )
    private val sessionFiveFacts = listOf(
        fact("p1-s5-f1", "p1:Anna", "Anna", work, city, "place", "Anna arbejder i Aarhus."),
        fact("p1-s5-f2", "p1:Nora", "Nora", study, home, "place", "Nora studerer hjemme."),
        fact("p1-s5-f3", "p1:Nora", "Nora", work, morning, "time", "Nora arbejder om morgenen."),
    )

    val tasks: List<VocabularyP1Task> = listOf(
        choice("p1-1.1", "E031", "arbejder", listOf("works", "reads", "writes", "drinks"), "works", work),
        choice("p1-1.2", "E031", "en bog", listOf("a book", "a newspaper", "a written letter", "a computer"), "a book", book),
        choice("p1-1.3", "E031", "i dag", listOf("today", "now", "in the morning", "this evening"), "today", today),
        matching("p1-2.1", listOf(newspaper, letter, write, computer)),
        choice("p1-2.2", "E032", "Choose the introduced bundle for a newspaper.",
            listOf("en bog", "en avis", "et brev", "en computer"), "en avis", newspaper),
        reading("p1-1.4", sessionOneFacts, "p1-s1-f2", "What does Anna read?",
            listOf("a book", "a newspaper", "a written letter"), book, "object"),
        reading("p1-2.3", sessionTwoFacts, "p1-s2-f1", "What does Nora write?",
            listOf("a written letter", "a newspaper", "a book", "on a computer"), letter, "object"),
        reading("p1-2.4", sessionTwoFacts, "p1-s2-f2", "What does the text explicitly say Nora has?",
            listOf("a computer", "a newspaper", "a book"), computer, "object"),
        matching("p1-3.1", listOf(study, course, danish, now)),
        reading("p1-3.2", sessionThreeFacts, "p1-s3-f2", "What does Nora read?",
            listOf("a book", "a newspaper", "a written letter"), book, "object"),
        tiles("p1-3.3", "E037", "Write She reads a book now. Begin with the subject.",
            listOf("hun" to "L004", "læser" to "L011", "en" to "L022", "bog" to "L014", "nu" to "L038"),
            listOf("hun", "læser", "en", "bog", "nu"), listOf("nu", "læser", "hun", "en", "bog"), null,
            VocabularyP1GrammarTarget("G005", "p1-subject-first-supplied", "word-order")),
        recall("p1-3.4", "E034", "Type the introduced present form of at studere.", "studerer", study, "present-form"),
        recall("p1-3.5", "E035", "Type the introduced Danish expression for now.", "nu", now, "fixed-chunk"),
        matching("p1-4.1", listOf(drink, coffee, water)),
        reading("p1-4.2", sessionFourFacts, "p1-s4-f2", "What does Nora drink?",
            listOf("water", "coffee"), water, "object"),
        tiles("p1-4.3", "E038", "Write Nora drinks water. Begin with Nora. One word is extra.",
            listOf("Nora" to "p1:Nora", "drikker" to "L045", "drikke" to "L045", "vand" to "L047"),
            listOf("Nora", "drikker", "vand"), null, listOf("Nora", "drikke", "vand"),
            VocabularyP1GrammarTarget("G004", "p1-drikke-present", "selection")),
        recall("p1-4.4", "E034", "Type the introduced present form of at drikke.", "drikker", drink, "present-form"),
        recall("p1-4.5", "E036", "Type the introduced Danish word for water as liquid.", "vand", water, "mass-headword"),
        matching("p1-5.1", listOf(city, home, morning)),
        reading("p1-5.2", sessionFiveFacts, "p1-s5-f2", "Where does Nora study?",
            listOf("at home", "in Aarhus", "in Odense"), home, "place"),
        reading("p1-5.3", sessionFiveFacts, "p1-s5-f3", "When does Nora work?",
            listOf("in the morning", "today", "now"), morning, "time"),
        tiles("p1-5.4", "E037", "Write I work at home. Begin with the subject.",
            listOf("jeg" to "L001", "arbejder" to "L027", "hjemme" to "L048"),
            listOf("jeg", "arbejder", "hjemme"), null, null,
            VocabularyP1GrammarTarget("G005", "p1-subject-first-supplied", "word-order")),
        tiles("p1-5.5", "E037", "Write I work in Odense. Begin with the subject.",
            listOf("jeg" to "L001", "arbejder" to "L027", "i" to "L032", "Odense" to "p1:Odense"),
            listOf("jeg", "arbejder", "i", "Odense"), listOf("i", "Odense", "arbejder", "jeg"), null,
            VocabularyP1GrammarTarget("G005", "p1-subject-first-supplied", "word-order")),
    )

    val formats = listOf(
        ExerciseFormat("E031", "Recognize an introduced meaning", null, LexicalTask("meaning", "da-en")),
        ExerciseFormat("E032", "Choose an introduced Danish form", null, LexicalTask("meaning", "en-da")),
        ExerciseFormat("E033", "Match introduced meanings and forms", null, LexicalTask("meaning", "en-da")),
        ExerciseFormat("E034", "Recall an introduced present form", "G004"),
        ExerciseFormat("E035", "Recall an introduced fixed expression", null, LexicalTask("fixed-chunk", "en-da")),
        ExerciseFormat("E036", "Recall an introduced mass noun", null, LexicalTask("mass-headword", "en-da")),
        ExerciseFormat("E037", "Build a reviewed sentence", "G005"),
        ExerciseFormat("E038", "Choose the present form in a sentence", "G004"),
        ExerciseFormat("E039", "Read explicit facts", null, readingTask = ReadingTask("explicit-fact")),
    )
    val formatIds = formats.map(ExerciseFormat::id).toSet()

    init {
        require(tasks.size == P1_ACTIVITY_COUNT && tasks.map(VocabularyP1Task::id).distinct().size == tasks.size) {
            "P1 must contain $P1_ACTIVITY_COUNT uniquely identified activities"
        }
        require(tasks.map(VocabularyP1Task::formatId).toSet() == formats.map(ExerciseFormat::id).toSet()) {
            "Every P1 operation must have authored activity coverage"
        }
        tasks.forEach(::requireValidTask)
    }

    fun tasks(formatId: String) = tasks.filter { it.formatId == formatId }

    private fun requireValidTask(task: VocabularyP1Task) {
        require(task.targets.map { it.entryId to it.senseId }.distinct().size == task.targets.size) {
            "${task.id} has duplicate lexical targets"
        }
        when (task.kind) {
            VocabularyP1TaskKind.CHOICE -> requireValidChoice(task)
            VocabularyP1TaskKind.MATCHING -> requireValidMatching(task)
            VocabularyP1TaskKind.RECALL -> requireValidRecall(task)
            VocabularyP1TaskKind.TILES -> requireValidTiles(task)
            VocabularyP1TaskKind.READING -> requireValidReading(task)
        }
    }

    private fun requireValidChoice(task: VocabularyP1Task) = require(
        task.targets.size == 1 && task.expected in task.choices && task.supportFacts.isEmpty(),
    ) { "${task.id} must have one target and an in-bank answer" }

    private fun requireValidMatching(task: VocabularyP1Task) = require(
        task.targets.size in P1_MATCHING_MIN..P1_MATCHING_MAX && task.matching.size == task.targets.size &&
            task.matching.map { it.second.first }.distinct().size == task.matching.size &&
            task.matching.map { it.second.second }.distinct().size == task.matching.size,
    ) { "${task.id} must have three or four unique matching pairs" }

    private fun requireValidRecall(task: VocabularyP1Task) = require(
        task.targets.size == 1 && task.expected.isNotBlank() && task.responseShape in P1_RECALL_SHAPES,
    ) { "${task.id} must have one typed target and a supported isolated response shape" }

    private fun requireValidTiles(task: VocabularyP1Task) {
        val available = task.tileWords.groupingBy { it.first.lowercase(Locale.ROOT) }.eachCount()
        val required = task.canonicalWords.groupingBy { it.lowercase(Locale.ROOT) }.eachCount()
        require(required.all { (word, count) -> available.getOrDefault(word, 0) >= count }) {
            "${task.id} canonical order must use supplied occurrences"
        }
        require(task.targets.isEmpty() && task.canonicalWords.isNotEmpty() &&
            (task.formatId != SPARE_TILE_FORMAT || task.tileWords.size == task.canonicalWords.size + SPARE_TILE_COUNT)) {
            "${task.id} has an invalid tile response shape"
        }
    }

    private fun requireValidReading(task: VocabularyP1Task) {
        val targetFact = task.supportFacts.singleOrNull { it.id == task.targetSupportFactId }
        val target = task.targets.singleOrNull()
        require(targetFact != null && target != null && targetFact.valueId == target.entryId &&
            targetFact.valueRole == task.factRole && target.gloss in task.choices && task.expected.isEmpty() &&
            task.supportFacts.map(VocabularyP1Fact::id).distinct().size == task.supportFacts.size &&
            task.supportFacts.all { it.exposureKeys.isNotEmpty() }) {
            "${task.id} must derive its in-bank answer from one structured visible fact"
        }
    }

    private fun verb(id: String, sense: String, infinitive: String, present: String, gloss: String, use: String) =
        VocabularyP1Lexeme(id, sense, VocabularyLexicalKind.VERB_FORM, VocabularyLexicalForms(infinitive, present), gloss, use)
    private fun count(id: String, sense: String, bundle: String, gloss: String, use: String) =
        VocabularyP1Lexeme(id, sense, VocabularyLexicalKind.COUNT_BUNDLE, VocabularyLexicalForms(bundle = bundle), gloss, use)
    private fun mass(id: String, sense: String, form: String, gloss: String, use: String) =
        VocabularyP1Lexeme(id, sense, VocabularyLexicalKind.MASS_WORD, VocabularyLexicalForms(form = form), gloss, use)
    private fun fixed(id: String, sense: String, form: String, gloss: String, use: String) =
        VocabularyP1Lexeme(id, sense, VocabularyLexicalKind.FIXED_CHUNK, VocabularyLexicalForms(form = form), gloss, use)
    private fun bareLanguage(id: String, sense: String, form: String, gloss: String, use: String) =
        VocabularyP1Lexeme(id, sense, VocabularyLexicalKind.BARE_LANGUAGE, VocabularyLexicalForms(form = form), gloss, use)
    private fun adverb(id: String, sense: String, form: String, gloss: String, use: String) =
        VocabularyP1Lexeme(id, sense, VocabularyLexicalKind.ADVERB, VocabularyLexicalForms(form = form), gloss, use)
    private fun choice(id: String, format: String, cue: String, choices: List<String>, expected: String, target: VocabularyP1Lexeme) =
        VocabularyP1Task(id, format, VocabularyP1TaskKind.CHOICE, cue, choices = choices, expected = expected, targets = listOf(target))
    private fun matching(id: String, lexemes: List<VocabularyP1Lexeme>) = VocabularyP1Task(
        id, "E033", VocabularyP1TaskKind.MATCHING, "Match each English meaning to its introduced Danish form.",
        targets = lexemes, matching = lexemes.map { it to (it.gloss to it.display) }, responseShape = "matching",
    )
    private fun recall(id: String, format: String, cue: String, expected: String, target: VocabularyP1Lexeme, shape: String) =
        VocabularyP1Task(
            id, format, VocabularyP1TaskKind.RECALL, cue, expected = expected, targets = listOf(target), responseShape = shape,
            targetSkillId = if (format == "E034") "G004" else null,
            targetScopeId = if (format == "E034") {
                "p1-${requireNotNull(target.forms.infinitive).removePrefix("at ")}-present"
            } else {
                null
            },
            assessmentMode = if (format == "E034") "recall" else null,
        )
    private fun reading(id: String, facts: List<VocabularyP1Fact>, targetFactId: String, cue: String,
        choices: List<String>, target: VocabularyP1Lexeme, role: String) = VocabularyP1Task(
        id, "E039", VocabularyP1TaskKind.READING, cue, facts.map(VocabularyP1Fact::sentence), choices,
        targets = listOf(target), factRole = role, supportFacts = facts, targetSupportFactId = targetFactId,
    )
    private fun tiles(id: String, format: String, cue: String, words: List<Pair<String, String>>, canonical: List<String>,
        offTarget: List<String>?, knownIssue: List<String>?, target: VocabularyP1GrammarTarget) = VocabularyP1Task(
        id, format, VocabularyP1TaskKind.TILES, cue, tileWords = words, canonicalWords = canonical,
        knownOffTargetOrder = offTarget, knownIssueOrder = knownIssue, targetSkillId = target.skillId,
        targetScopeId = target.scopeId, assessmentMode = target.mode,
        responseShape = if (format == SPARE_TILE_FORMAT) "with-spares" else "use-all",
    )

    private fun fact(
        id: String,
        referentId: String,
        referentForm: String,
        predicate: VocabularyP1Lexeme,
        value: VocabularyP1Lexeme,
        role: String,
        sentence: String,
        vararg additionalVisible: VocabularyP1Lexeme,
    ) = VocabularyP1Fact(
        id, referentId, referentForm, predicate.entryId, value.entryId, role, sentence,
        listOf(referentExposureKey(referentId, referentForm)) +
            (listOf(predicate, value) + additionalVisible).map(::visibleFactExposureKey),
    )

    private fun fact(
        id: String,
        referentId: String,
        referentForm: String,
        predicateId: String,
        value: VocabularyP1Lexeme,
        role: String,
        sentence: String,
        predicateForm: String,
    ) = VocabularyP1Fact(
        id, referentId, referentForm, predicateId, value.entryId, role, sentence,
        listOf(referentExposureKey(referentId, referentForm), "vocabulary-p1:form:$predicateId:$predicateForm") +
            visibleExposureKeys(value),
    )

    private fun predicateFact(
        id: String,
        referentId: String,
        referentForm: String,
        predicate: VocabularyP1Lexeme,
        sentence: String,
    ) = VocabularyP1Fact(
        id, referentId, referentForm, predicate.entryId, predicate.entryId, "action", sentence,
        listOf(referentExposureKey(referentId, referentForm), visibleFactExposureKey(predicate)),
    )

    private fun visibleExposureKeys(lexeme: VocabularyP1Lexeme) = listOfNotNull(
        lexeme.forms.infinitive?.let { "vocabulary-p1:form:${lexeme.entryId}:$it" },
        lexeme.forms.present?.let { "vocabulary-p1:form:${lexeme.entryId}:$it" },
        lexeme.forms.bundle?.let { "vocabulary-p1:bundle:${lexeme.entryId}:$it" },
        lexeme.forms.form?.let { "vocabulary-p1:form:${lexeme.entryId}:$it" },
    )

    private fun visibleFactExposureKey(lexeme: VocabularyP1Lexeme): String = when {
        lexeme.forms.present != null -> "vocabulary-p1:form:${lexeme.entryId}:${lexeme.forms.present}"
        lexeme.forms.bundle != null -> "vocabulary-p1:bundle:${lexeme.entryId}:${lexeme.forms.bundle}"
        else -> "vocabulary-p1:form:${lexeme.entryId}:${requireNotNull(lexeme.forms.form)}"
    }

    private fun referentExposureKey(referentId: String, form: String) =
        "vocabulary-p1:support:referent:$referentId:$form"

    private const val P1_ACTIVITY_COUNT = 23
    private const val P1_MATCHING_MIN = 3
    private const val P1_MATCHING_MAX = 4
    private const val SPARE_TILE_FORMAT = "E038"
    private const val SPARE_TILE_COUNT = 1
}

internal object VocabularyP1ExerciseGenerator {
    fun generate(request: GenerationRequest): GeneratedExercise {
        val pool = VocabularyP1Content.tasks(request.formatId)
        val task = pool[Math.floorMod(request.seed.toLong() + request.index, pool.size.toLong()).toInt()]
        return when (task.kind) {
            VocabularyP1TaskKind.MATCHING -> matching(request, task)
            VocabularyP1TaskKind.TILES -> tiles(request, task)
            else -> text(request, task)
        }
    }

    private fun text(request: GenerationRequest, task: VocabularyP1Task): GeneratedExercise {
        val lexicalTarget = if (request.formatId in TEXT_LEXICAL_FORMATS) {
            task.targets.single().target(
                if (task.kind == VocabularyP1TaskKind.RECALL) task.responseShape else "meaning",
                if (request.formatId == "E031") "da-en" else "en-da",
            )
        } else {
            null
        }
        return exercise(
            request,
            task,
            ExercisePrompt(
            "",
            "",
            choices = shuffled(task.choices, request.seed xor request.index),
            punctuation = "",
            inputLabel = if (task.kind == VocabularyP1TaskKind.RECALL) "Danish answer" else null,
            cue = task.cue,
            cueLanguage = if (task.kind == VocabularyP1TaskKind.CHOICE && request.formatId == "E031") "da" else "en",
            answerLanguage = if (request.formatId == "E031" || task.kind == VocabularyP1TaskKind.READING) "en" else "da",
            passage = task.passage.ifEmpty { null },
            ),
            lexicalTarget = lexicalTarget,
            readingTarget = if (task.kind == VocabularyP1TaskKind.READING) ReadingTask("explicit-fact") else null,
        )
    }

    private fun matching(request: GenerationRequest, task: VocabularyP1Task): GeneratedExercise {
        val left = shuffled(task.matching, request.seed xor MATCHING_LEFT_SALT).mapIndexed { index, row ->
            row.first to MatchingItem(opaqueItemId("m", request.seed xor MATCHING_LEFT_SALT, index), row.second.first)
        }
        val right = shuffled(task.matching, request.seed xor MATCHING_RIGHT_SALT).mapIndexed { index, row ->
            row.first to MatchingItem(opaqueItemId("n", request.seed xor MATCHING_RIGHT_SALT, index), row.second.second)
        }
        val rightBySense = right.associate { it.first.senseId to it.second.id }
        val pairs = left.map {
            VocabularyMatchingPairSourceDetails(it.second.id, rightBySense.getValue(it.first.senseId), it.first.details())
        }
        return exercise(request, task, ExercisePrompt("", "", punctuation = "", cueLanguage = "en", answerLanguage = "da",
            matchingColumns = MatchingColumns(left.map { it.second }, right.map { it.second })),
            lexicalTargets = task.targets.map { it.target() }, matchingPairs = pairs)
    }

    private fun tiles(request: GenerationRequest, task: VocabularyP1Task): GeneratedExercise {
        val occurrences = task.tileWords.mapIndexed { index, row -> TileSeedOccurrence(index, row.first, row.second) }
        val displayOccurrences = shuffled(occurrences, request.seed xor request.index xor TILE_SALT).mapIndexed { index, row ->
            GeneratedTileOccurrence(
                opaqueItemId("t", request.seed xor request.index xor TILE_SALT, index),
                row.canonicalIndex,
                row.word,
                row.lexicalId,
            )
        }
        val originalIndexQueues = occurrences.groupBy(TileSeedOccurrence::word).mapValues {
            it.value.map(TileSeedOccurrence::canonicalIndex).toMutableList()
        }
        val idByOriginalIndex = displayOccurrences.associate { it.canonicalIndex to it.id }
        val canonical = task.canonicalWords.map { word ->
            idByOriginalIndex.getValue(originalIndexQueues.getValue(word).removeFirst())
        }
        val prompt = ExercisePrompt("", "", blocks = displayOccurrences.map { OrderBlock(it.id, it.word) }, punctuation = ".",
            cue = task.cue, cueLanguage = "en", answerLanguage = "da", tileMode = task.responseShape,
            requiredTileCount = task.canonicalWords.size)
        return exercise(request, task, prompt, acceptedOrders = listOf(canonical), tileOccurrences = displayOccurrences.map {
            VocabularyTileOccurrenceDetails(it.id, it.lexicalId, it.word)
        })
    }

    private fun exercise(
        request: GenerationRequest,
        task: VocabularyP1Task,
        prompt: ExercisePrompt,
        lexicalTarget: LexicalTarget? = null, lexicalTargets: List<LexicalTarget>? = null,
        readingTarget: ReadingTask? = null,
        matchingPairs: List<VocabularyMatchingPairSourceDetails> = emptyList(),
        acceptedOrders: List<List<String>> = emptyList(),
        tileOccurrences: List<VocabularyTileOccurrenceDetails> = emptyList(),
    ): GeneratedExercise {
        val sources = task.targets.map { it.details() }
        val facts = task.supportFacts.map { support ->
            val value = placementsById.getValue(support.valueId)
            VocabularySupportFactDetails(
                support.id,
                support.referentId,
                support.predicateId,
                support.valueId,
                value.gloss,
                support.valueRole,
                support.sentence,
                support.exposureKeys,
            )
        }
        val knownAlternatives = knownAlternatives(task)
        val reviewedOrders = reviewedOrders(task)
        return GeneratedExercise(
            id = "${task.formatId}-${request.contentRevision}-${request.generatorRevision}-${task.id}-${request.seed}-${request.index}",
            formatId = task.formatId, targetSkillId = task.targetSkillId, lexicalTarget = lexicalTarget,
            supportSkillIds = listOf("G001", "G002", "G003", "G004", "G005").filterNot { it == task.targetSkillId },
            presentation = ExercisePresentation(instruction(task), "Use only the material introduced in this lesson.", prompt,
                task.targets.map {
                    IntroductionSection(it.gloss.replaceFirstChar(Char::uppercase), it.teachingDisplay, "da")
                }, hint(task)),
            contentRevision = request.contentRevision, generatorRevision = request.generatorRevision,
            reproduction = Reproduction(request.seed, request.index),
            inspector = ExerciseInspector(lexicalIds = task.targets.map { it.entryId }, clauseRoles = listOfNotNull(task.factRole),
                vocabularySource = VocabularyExerciseSourceDetails(
                    taskId = task.id,
                    operation = task.formatId,
                    lexicalSources = sources,
                    expectedAnswer = task.expected.takeUnless { task.kind == VocabularyP1TaskKind.READING || it.isEmpty() },
                    suppliedChoices = task.choices,
                    supportFacts = facts,
                    matchingPairs = matchingPairs,
                    tileOccurrences = tileOccurrences,
                    acceptedOrders = acceptedOrders,
                    targetSupportFactId = task.targetSupportFactId,
                    questionRole = task.factRole,
                    normalizationPolicy = if (task.kind == VocabularyP1TaskKind.RECALL) NORMALIZATION_POLICY else null,
                    knownAlternatives = knownAlternatives,
                    reviewedOrders = reviewedOrders,
                )),
            source = VocabularyP1ExerciseSource(task, AnswerKey.MeaningChoice(task.expected, task.choices.toSet())),
            readingTarget = readingTarget,
            lexicalTargets = lexicalTargets,
        )
    }

    private val placementsById = VocabularyP1Content.placements.associateBy(VocabularyP1Lexeme::entryId)

    private fun instruction(task: VocabularyP1Task) = when (task.kind) {
        VocabularyP1TaskKind.CHOICE, VocabularyP1TaskKind.READING -> "Choose the answer supported by the prompt."
        VocabularyP1TaskKind.MATCHING -> "Match each English meaning to its introduced Danish form."
        VocabularyP1TaskKind.RECALL -> "Type the requested introduced Danish form."
        VocabularyP1TaskKind.TILES -> "Arrange the supplied words as requested."
    }
    private fun hint(task: VocabularyP1Task) = when (task.kind) {
        VocabularyP1TaskKind.MATCHING -> "Use each supplied item at most once."
        VocabularyP1TaskKind.TILES -> "Begin with the requested subject and use the required number of tiles."
        else -> "Use the introduced meaning and the stated response shape."
    }

    private fun knownAlternatives(task: VocabularyP1Task): List<VocabularyKnownAlternativeDetails> {
        if (task.kind != VocabularyP1TaskKind.RECALL) return emptyList()
        val target = task.targets.single()
        val introduced = recognizedForms(task).map { (lexeme, form) ->
                val oneWord = !normalizeText(form).contains(' ')
                val sameMeaning = lexeme.entryId == target.entryId && lexeme.senseId == target.senseId
                val targetInfinitive = sameMeaning && form == target.forms.infinitive
                VocabularyKnownAlternativeDetails(
                    form,
                    TargetStatus.NOT_MET,
                    LinguisticStatus.ACCEPTABLE,
                    if (sameMeaning) MeaningSatisfaction.MET else MeaningSatisfaction.NOT_MET,
                    if (oneWord) ResponseConstraintStatus.MET else ResponseConstraintStatus.NOT_MET,
                    when {
                        targetInfinitive -> "INFINITIVE_USED"
                        !oneWord -> "ONE_WORD_RESPONSE_REQUIRED"
                        else -> "REQUESTED_FORM_REQUIRED"
                    },
                )
        }.filterNot { it.text.equals(task.expected, ignoreCase = true) }
        val selectedMassExceptions = if (task.id == "p1-4.5") {
            listOf("en vand", "et vand").map {
            VocabularyKnownAlternativeDetails(
                it, TargetStatus.NOT_MET, LinguisticStatus.UNKNOWN, MeaningSatisfaction.UNKNOWN,
                ResponseConstraintStatus.NOT_MET, "SELECTED_MASS_SHAPE_REQUIRED",
            )
            }
        } else {
            emptyList()
        }
        return (introduced + selectedMassExceptions).distinctBy { it.text.lowercase(Locale.ROOT) }
    }

    private fun recognizedForms(task: VocabularyP1Task): List<Pair<VocabularyP1Lexeme, String>> = when (task.id) {
        "p1-3.4", "p1-3.5" -> listOf(
            VocabularyP1Content.study to "studere",
            VocabularyP1Content.study to "studerer",
            VocabularyP1Content.course to "et kursus",
            VocabularyP1Content.danish to "dansk",
            VocabularyP1Content.now to "nu",
            VocabularyP1Content.read to "læser",
            VocabularyP1Content.book to "en bog",
        )
        "p1-4.4", "p1-4.5" -> listOf(
            VocabularyP1Content.drink to "drikke",
            VocabularyP1Content.drink to "drikker",
            VocabularyP1Content.coffee to "kaffe",
            VocabularyP1Content.water to "vand",
        )
        else -> error("Unsupported P1 recall task ${task.id}")
    }

    private fun reviewedOrders(task: VocabularyP1Task): List<VocabularyReviewedOrderDetails> = buildList {
        add(
            VocabularyReviewedOrderDetails(
                task.canonicalWords,
                TargetStatus.MET,
                LinguisticStatus.ACCEPTABLE,
                MeaningSatisfaction.MET,
                ResponseConstraintStatus.MET,
                null,
            ),
        )
        task.knownOffTargetOrder?.let {
            add(
                VocabularyReviewedOrderDetails(
                    it, TargetStatus.NOT_MET, LinguisticStatus.ACCEPTABLE, MeaningSatisfaction.MET,
                    ResponseConstraintStatus.NOT_MET, "REQUESTED_ORDER_CONSTRAINT",
                ),
            )
        }
        task.knownIssueOrder?.let {
            add(
                VocabularyReviewedOrderDetails(
                    it, TargetStatus.NOT_MET, LinguisticStatus.KNOWN_ISSUE, MeaningSatisfaction.MET,
                    ResponseConstraintStatus.MET, "INFINITIVE_USED",
                ),
            )
        }
    }.filter { it.words.isNotEmpty() }
    private fun <T> shuffled(input: List<T>, salt: Int): List<T> = input.sortedBy { row ->
        var value = salt.toLong() xor row.hashCode().toLong() * SHUFFLE_MULTIPLIER
        value = (value xor (value ushr SHUFFLE_FIRST_SHIFT)) * SHUFFLE_MIXER
        value xor (value ushr SHUFFLE_SECOND_SHIFT)
    }

    private fun opaqueItemId(prefix: String, salt: Int, displayIndex: Int): String {
        var value = salt.toLong() xor displayIndex.toLong() * SHUFFLE_MULTIPLIER
        value = (value xor (value ushr SHUFFLE_FIRST_SHIFT)) * SHUFFLE_MIXER
        return "$prefix-${(value xor (value ushr SHUFFLE_SECOND_SHIFT)).toULong().toString(OPAQUE_ID_RADIX)}"
    }

    private data class TileSeedOccurrence(val canonicalIndex: Int, val word: String, val lexicalId: String)
    private data class GeneratedTileOccurrence(
        val id: String,
        val canonicalIndex: Int,
        val word: String,
        val lexicalId: String,
    )

    private const val NORMALIZATION_POLICY = "nfc-trim-root-casefold-isolated"
    private const val MATCHING_LEFT_SALT = 0x11223344
    private const val MATCHING_RIGHT_SALT = 0x55667788
    private const val TILE_SALT = 0x24681357
    private const val SHUFFLE_MULTIPLIER = -7046029254386353131L
    private const val SHUFFLE_MIXER = -4658895280553007687L
    private const val SHUFFLE_FIRST_SHIFT = 30
    private const val SHUFFLE_SECOND_SHIFT = 27
    private const val OPAQUE_ID_RADIX = 16
    private val TEXT_LEXICAL_FORMATS = setOf("E031", "E032", "E035", "E036")
}

internal object VocabularyP1ExerciseEvaluator {
    fun evaluate(exercise: GeneratedExercise, answer: Answer): Evaluation {
        val task = (exercise.source as VocabularyP1ExerciseSource).task
        return when (task.kind) {
            VocabularyP1TaskKind.MATCHING -> matching(exercise, answer, task)
            VocabularyP1TaskKind.TILES -> tiles(exercise, answer, task)
            VocabularyP1TaskKind.RECALL -> recall(exercise, answer, task)
            VocabularyP1TaskKind.CHOICE, VocabularyP1TaskKind.READING -> choice(exercise, answer, task)
        }
    }

    private fun choice(exercise: GeneratedExercise, answer: Answer, task: VocabularyP1Task): Evaluation {
        val text = (answer as? Answer.Text)?.text?.let(::normalizeText)
            ?: return choiceResult(answer, NormalizedAnswer.Text(""), TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN,
                ResponseConstraintStatus.NOT_MET, MeaningSatisfaction.UNKNOWN, "SUPPLIED_CHOICE_REQUIRED",
                "Choose a supplied answer.", choiceScope(task))
        val folded = text.lowercase(Locale.ROOT)
        val supplied = task.choices.associateBy { it.lowercase(Locale.ROOT) }
        val expected = if (task.kind == VocabularyP1TaskKind.READING) {
            val source = requireNotNull(exercise.inspector.vocabularySource)
            val fact = source.supportFacts.single { it.factId == source.targetSupportFactId }
            require(fact.valueRole == source.questionRole) { "Reading question role must match its supporting fact" }
            fact.valueText
        } else {
            task.expected
        }
        return when {
            folded == expected.lowercase(Locale.ROOT) -> choiceResult(answer, NormalizedAnswer.Text(text), TargetStatus.MET,
                LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.MET, MeaningSatisfaction.MET, null, "Correct.",
                choiceScope(task))
            folded in supplied -> choiceResult(answer, NormalizedAnswer.Text(text), TargetStatus.NOT_MET, LinguisticStatus.ACCEPTABLE,
                ResponseConstraintStatus.MET, MeaningSatisfaction.NOT_MET, "INTRODUCED_MEANING_REQUIRED",
                "That supplied answer expresses a different introduced fact or meaning.", choiceScope(task))
            else -> choiceResult(answer, NormalizedAnswer.Text(text), TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN,
                ResponseConstraintStatus.NOT_MET, MeaningSatisfaction.UNKNOWN, "SUPPLIED_CHOICE_REQUIRED",
                "Choose a supplied answer.", choiceScope(task))
        }
    }

    private fun choiceScope(task: VocabularyP1Task): LinguisticScope =
        if (task.formatId == "E032") LinguisticScope.DANISH_CHOICE else LinguisticScope.ENGLISH_CHOICE

    private fun choiceResult(
        raw: Answer,
        normalized: NormalizedAnswer,
        target: TargetStatus,
        language: LinguisticStatus,
        constraint: ResponseConstraintStatus,
        meaning: MeaningSatisfaction,
        issue: String?,
        feedback: String,
        scope: LinguisticScope,
    ): Evaluation = result(raw, normalized, target, language, constraint, meaning, issue, feedback)
        .copy(linguisticScope = scope)

    private fun recall(exercise: GeneratedExercise, answer: Answer, task: VocabularyP1Task): Evaluation =
        (answer as? Answer.Text)?.text?.let { evaluateRecall(exercise, answer, task, it) }
            ?: result(answer, NormalizedAnswer.Text(""), TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN,
                ResponseConstraintStatus.NOT_MET, MeaningSatisfaction.UNKNOWN, "TEXT_RESPONSE_REQUIRED", "Type one answer.")

    private fun evaluateRecall(exercise: GeneratedExercise, answer: Answer, task: VocabularyP1Task, raw: String): Evaluation {
        val text = normalizeText(raw)
        val folded = text.lowercase(Locale.ROOT)
        val expected = task.expected.lowercase(Locale.ROOT)
        if (folded == expected) {
            return result(answer, NormalizedAnswer.Text(text), TargetStatus.MET, LinguisticStatus.ACCEPTABLE,
                ResponseConstraintStatus.MET, MeaningSatisfaction.MET, null, "Correct. This is the requested introduced form.")
        }
        val source = requireNotNull(exercise.inspector.vocabularySource)
        val alternative = source.knownAlternatives.singleOrNull { normalizeText(it.text).lowercase(Locale.ROOT) == folded }
        return if (alternative != null) {
            result(answer, NormalizedAnswer.Text(text), alternative.targetStatus, alternative.linguisticStatus,
                alternative.responseConstraintStatus, alternative.meaningSatisfaction, alternative.issueCode,
                feedbackFor(alternative.issueCode))
        } else {
            result(answer, NormalizedAnswer.Text(text), TargetStatus.NOT_MET, LinguisticStatus.UNKNOWN,
                if (text.contains(' ')) ResponseConstraintStatus.NOT_MET else ResponseConstraintStatus.MET,
                MeaningSatisfaction.UNKNOWN,
                "UNRECOGNIZED_P1_FORM", "This response is outside the forms introduced in this lesson.")
        }
    }

    private fun matching(exercise: GeneratedExercise, answer: Answer, task: VocabularyP1Task): Evaluation {
        val pairs = (answer as? Answer.Matching)?.pairs ?: emptyList()
        val columns = requireNotNull(exercise.prompt.matchingColumns)
        val valid = validMatchingSelection(pairs, columns, task.matching.size)
        val source = requireNotNull(exercise.inspector.vocabularySource)
        val targetBySense = requireNotNull(exercise.lexicalTargets).associateBy { it.senseId }
        if (!valid) return result(answer, NormalizedAnswer.Matching(pairs), TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN,
            ResponseConstraintStatus.NOT_MET, MeaningSatisfaction.UNKNOWN, "MATCHING_SELECTION_INVALID",
            "Use supplied left and right IDs at most once.")
        val selected = pairs.associate { it.leftId to it.rightId }
        val rows = source.matchingPairs.map { expected -> matchingResult(expected, selected, targetBySense) }
        val wrong = rows.any { it.targetStatus == TargetStatus.NOT_MET }
        val complete = pairs.size == task.matching.size
        return Evaluation(if (wrong) TargetStatus.NOT_MET else if (complete) TargetStatus.MET else TargetStatus.UNEVALUABLE,
            if (pairs.isEmpty()) LinguisticStatus.UNKNOWN else LinguisticStatus.ACCEPTABLE, emptyList(),
            if (complete && !wrong) "All supplied meanings are matched." else "Review the pair feedback.", "", answer,
            NormalizedAnswer.Matching(pairs), LinguisticScope.DANISH_MATCHING,
            if (complete) ResponseConstraintStatus.MET else ResponseConstraintStatus.NOT_MET,
            if (wrong) MeaningSatisfaction.NOT_MET else if (complete) MeaningSatisfaction.MET else MeaningSatisfaction.UNKNOWN, rows)
    }

    private fun validMatchingSelection(pairs: List<MatchingPair>, columns: MatchingColumns, maximum: Int): Boolean =
        pairs.size <= maximum && pairs.map { it.leftId }.distinct().size == pairs.size &&
            pairs.map { it.rightId }.distinct().size == pairs.size && pairs.all { pair ->
                columns.left.any { it.id == pair.leftId } && columns.right.any { it.id == pair.rightId }
            }

    private fun matchingResult(
        expected: VocabularyMatchingPairSourceDetails,
        selected: Map<String, String>,
        targetBySense: Map<String, LexicalTarget>,
    ): MatchingPairResult {
        val right = selected[expected.leftId]
        val target = targetBySense.getValue(expected.lexicalSource.senseId)
        return when {
            right == null -> MatchingPairResult(expected.leftId, null, target, TargetStatus.UNEVALUABLE,
                MeaningSatisfaction.UNKNOWN, ResponseConstraintStatus.NOT_MET, LinguisticStatus.UNKNOWN,
                "Choose a supplied Danish form for this meaning.")
            right == expected.rightId -> MatchingPairResult(expected.leftId, right, target, TargetStatus.MET,
                MeaningSatisfaction.MET, ResponseConstraintStatus.MET, LinguisticStatus.ACCEPTABLE,
                "Correct. This pair matches.")
            else -> MatchingPairResult(expected.leftId, right, target, TargetStatus.NOT_MET,
                MeaningSatisfaction.NOT_MET, ResponseConstraintStatus.MET, LinguisticStatus.ACCEPTABLE,
                "That supplied form expresses another introduced meaning.")
        }
    }

    private fun tiles(exercise: GeneratedExercise, answer: Answer, task: VocabularyP1Task): Evaluation {
        val ids = (answer as? Answer.Order)?.blockIds ?: emptyList()
        val byId = exercise.prompt.blocks.associate { it.id to it.text }
        val valid = ids.distinct().size == ids.size && ids.all(byId::containsKey)
        if (!valid) return result(answer, NormalizedAnswer.Order(ids), TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN,
            ResponseConstraintStatus.NOT_MET, MeaningSatisfaction.UNKNOWN, "TILE_SELECTION_INVALID", "Use supplied tile IDs at most once.")
        val words = ids.map(byId::getValue)
        val shape = tileShapeMet(words, task)
        val canonical = sameWords(words, task.canonicalWords)
        val offTarget = task.knownOffTargetOrder?.let { sameWords(words, it) } == true
        val knownIssue = task.knownIssueOrder?.let { sameWords(words, it) } == true
        return when {
            canonical -> result(answer, NormalizedAnswer.Order(ids), TargetStatus.MET, LinguisticStatus.ACCEPTABLE,
                ResponseConstraintStatus.MET, MeaningSatisfaction.MET, null, "Correct. The requested sentence and order are met.",
                task.canonicalWords.joinToString(" ").replaceFirstChar(Char::uppercase) + ".")
            offTarget -> result(answer, NormalizedAnswer.Order(ids), TargetStatus.NOT_MET, LinguisticStatus.ACCEPTABLE,
                ResponseConstraintStatus.NOT_MET, MeaningSatisfaction.MET,
                "REQUESTED_ORDER_CONSTRAINT", "This is the reviewed fronted sentence, but the prompt requires the subject first.")
            knownIssue -> result(answer, NormalizedAnswer.Order(ids), TargetStatus.NOT_MET, LinguisticStatus.KNOWN_ISSUE,
                if (shape) ResponseConstraintStatus.MET else ResponseConstraintStatus.NOT_MET, MeaningSatisfaction.MET,
                "INFINITIVE_USED", "The supplied infinitive was used where this sentence requires the present form.")
            else -> result(answer, NormalizedAnswer.Order(ids), TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN,
                if (shape) ResponseConstraintStatus.MET else ResponseConstraintStatus.NOT_MET, MeaningSatisfaction.UNKNOWN,
                if (!shape) "REQUIRED_WORDS_MISSING" else "UNRECOGNIZED_P1_ORDER",
                "This word order is outside the sentences reviewed in this lesson.")
        }
    }

    private fun tileShapeMet(words: List<String>, task: VocabularyP1Task): Boolean =
        words.size == task.canonicalWords.size && words.firstOrNull()?.equals(task.canonicalWords.first(), true) == true &&
            (task.formatId != SPARE_TILE_FORMAT || words.count(::isDrinkForm) == REQUIRED_SELECTED_VERB_COUNT)

    private fun isDrinkForm(word: String): Boolean = word.equals("drikke", true) || word.equals("drikker", true)

    private fun sameWords(actual: List<String>, expected: List<String>): Boolean =
        actual.map { it.lowercase(Locale.ROOT) } == expected.map { it.lowercase(Locale.ROOT) }

    private fun feedbackFor(issueCode: String?): String = when (issueCode) {
        "INFINITIVE_USED" -> "That is the introduced infinitive, but the prompt asks for the present form."
        "ONE_WORD_RESPONSE_REQUIRED" -> "That introduced form does not meet the requested one-word response shape."
        "SELECTED_MASS_SHAPE_REQUIRED" ->
            "This lesson teaches bare vand for the selected liquid use; the one-word shape was missed."
        else -> "That is an introduced Danish form, but not the requested meaning or form."
    }

    private fun result(raw: Answer, normalized: NormalizedAnswer, target: TargetStatus, language: LinguisticStatus,
        constraint: ResponseConstraintStatus, meaning: MeaningSatisfaction, issue: String?, feedback: String,
        sentence: String? = null) = Evaluation(target, language,
        issue?.let { listOf(EvaluationIssue(it, feedback)) }.orEmpty(), feedback, sentence, raw, normalized,
        when (normalized) {
            is NormalizedAnswer.Order -> LinguisticScope.DANISH_SENTENCE
            is NormalizedAnswer.Matching -> LinguisticScope.DANISH_MATCHING
            else -> LinguisticScope.DANISH_WORD_FORM
        }, constraint, meaning)

    private const val SPARE_TILE_FORMAT = "E038"
    private const val REQUIRED_SELECTED_VERB_COUNT = 1
}
