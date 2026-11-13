package app.astrid.engine

import java.util.Locale

internal data class VocabularyP2Lexeme(
    val entryId: String,
    val senseId: String,
    val kind: VocabularyLexicalKind,
    val forms: VocabularyLexicalForms,
    val gloss: String,
    val selectedUse: String,
) {
    val display: String get() = forms.present ?: forms.bundle ?: forms.form ?: error("Missing P2 display form")
    fun details() = VocabularyLexicalSourceDetails(entryId, senseId, kind, forms, gloss, selectedUse)
    fun target(direction: String = "da-en") = LexicalTarget(entryId, senseId, "meaning", direction)
}

internal data class VocabularyP2Quantity(
    val constructionId: String,
    val display: String,
    val head: VocabularyP2Lexeme,
    val beverage: VocabularyP2Lexeme,
) {
    fun details() = VocabularyQuantitySourceDetails(
        constructionId,
        display,
        head.details(),
        listOf(head.details(), beverage.details()),
    )
}

internal data class VocabularyP2Fact(
    val id: String,
    val referentId: String,
    val referentDisplay: String,
    val referentAnswer: String,
    val predicate: VocabularyP2Lexeme,
    val objectSource: VocabularyP2Lexeme? = null,
    val quantitySource: VocabularyP2Quantity? = null,
    val sentence: String,
    val translation: String,
    val exposureKeys: List<String>,
)

internal data class VocabularyP2Chronology(val id: String, val beforeFactId: String, val afterFactId: String)

internal enum class VocabularyP2TaskKind { CHOICE, CONTEXT, MATCHING, TILES, READING }

internal data class VocabularyP2Tile(val text: String, val lexical: VocabularyP2Lexeme? = null, val quantity: VocabularyP2Quantity? = null)

internal data class VocabularyP2Task(
    val id: String,
    val formatId: String,
    val kind: VocabularyP2TaskKind,
    val cue: String,
    val choices: List<String> = emptyList(),
    val expected: String = "",
    val targets: List<VocabularyP2Lexeme> = emptyList(),
    val matching: List<Pair<String, VocabularyP2Lexeme>> = emptyList(),
    val passage: List<String> = emptyList(),
    val facts: List<VocabularyP2Fact> = emptyList(),
    val targetFactId: String? = null,
    val questionRole: String? = null,
    val chronology: List<VocabularyP2Chronology> = emptyList(),
    val targetChronologyId: String? = null,
    val tiles: List<VocabularyP2Tile> = emptyList(),
    val targetScopeId: String? = null,
    val fixedCommaBeforeLast: Boolean = false,
)

internal object VocabularyP2Content {
    const val CONTENT_REVISION = "pilot-19"
    const val GENERATOR_REVISION = "generator-19"
    const val EVALUATION_REVISION = "evaluation-19"

    val eat = verb("L012", "L012-S01", "spise", "spiser", "eats/is eating", "eat the selected food")
    val apple = count("L021", "L021-S01", "et æble", "an apple", "one apple fruit")
    val bread = mass("L050", "L050-S01", "brød", "bread", "bread as food")
    val cheap = adjective("L051", "L051-S01", "billig", "inexpensive", "supplied base-form product label")
    val milk = mass("L052", "L052-S01", "mælk", "milk", "milk beverage or substance")
    val excuseMe = interjection("L053", "L053-S01", "undskyld", "excuse me (getting attention)", "polite attention-getter")
    val hereYouAre = interjection("L054", "L054-S01", "værsgo", "here you are (handing something over)", "handover response")
    val roll = count("L055", "L055-S01", "en bolle", "a bread roll", "one bread roll")
    val shop = count("L056", "L056-S01", "en butik", "a shop", "retail shop")
    val cafe = count("L057", "L057-S01", "en café", "a café", "café venue")
    val buy = verb("L058", "L058-S01", "købe", "køber", "buys", "purchase the selected item")
    val pay = verb("L059", "L059-S01", "betale", "betaler", "pays", "settle the stated purchase")
    val thanks = interjection("L060", "L060-S01", "tak", "thank you", "standalone thanking response")
    val gladly = adverb("L061", "L061-S01", "gerne", "a willing wish or polite preference", "polite-wish frame")
    val cup = count("L062", "L062-S01", "en kop", "a cup", "one cup or quantity head")
    val bottle = count("L063", "L063-S01", "en flaske", "a bottle", "one bottle or quantity head")
    val drink = verb("L045", "L045-S01", "drikke", "drikker", "drinks/is drinking", "drink the selected beverage")
    val coffee = mass("L046", "L046-S01", "kaffe", "coffee", "coffee beverage or substance")
    val water = mass("L047", "L047-S01", "vand", "water", "water as liquid")
    val want = verb("L034", "L034-S01", "ville", "vil", "wants/would like", "personal wish or intention")
    val haveRequest = VocabularyP2Lexeme(
        "L009", "L009-S02", VocabularyLexicalKind.VERB_FORM,
        VocabularyLexicalForms(infinitive = "have", form = "have"),
        "have/receive in the request frame", "bare infinitive in Jeg vil gerne have X",
    )

    val placements = listOf(
        eat, apple, bread, cheap, drink, coffee, water, milk, excuseMe, hereYouAre, roll, shop,
        cafe, buy, pay, thanks, gladly, want, haveRequest, cup, bottle,
    )
    val newLexemes = listOf(bread, cheap, milk, excuseMe, hereYouAre, roll, shop, cafe, buy, pay, thanks, gladly, cup, bottle)
    val cupCoffee = VocabularyP2Quantity("p2-quantity-cup-coffee", "en kop kaffe", cup, coffee)
    val bottleWater = VocabularyP2Quantity("p2-quantity-bottle-water", "en flaske vand", bottle, water)

    private val s1Facts = listOf(
        fact("p2-s1-f1", "p2:speaker", "Jeg", "the speaker (jeg)", eat, apple, "Jeg spiser et æble.", "I eat an apple."),
        fact("p2-s1-f2", "p2:Nora", "Nora", "Nora", eat, bread, "Nora spiser brød.", "Nora eats bread."),
    )
    private val s2Facts = listOf(
        fact("p2-s2-f1", "p2:Anna", "Anna", "Anna", drink, coffee, "Anna drikker kaffe.", "Anna drinks coffee."),
        fact("p2-s2-f2", "p2:speaker", "Jeg", "the speaker (jeg)", drink, water, "Jeg drikker vand.", "I drink water."),
        fact("p2-s2-f3", "p2:Nora", "Nora", "Nora", drink, milk, "Nora drikker mælk.", "Nora drinks milk."),
    )
    private val s3Facts = listOf(
        fact("p2-s3-f1", "p2:Nora", "Nora", "Nora", eat, roll, "Nora spiser en bolle.", "Nora eats a bread roll."),
    )
    private val s4Facts = listOf(
        fact("p2-s4-f1", "p2:speaker", "Jeg", "the speaker (jeg)", buy, roll,
            "Jeg køber en bolle i en café.", "The speaker buys a bread roll in a café."),
        fact("p2-s4-f2", "p2:speaker", "Jeg", "the speaker (jeg)", pay, null, "Jeg betaler.", "The speaker pays."),
    )
    private val s6Facts = listOf(
        quantityFact("p2-s6-f1", "p2:speaker", "Jeg", "the speaker (jeg)", cupCoffee,
            "Jeg vil gerne have en kop kaffe, tak.", "The speaker requests a cup of coffee."),
        quantityFact("p2-s6-f2", "p2:Nora", "Nora", "Nora", bottleWater,
            "Nora vil gerne have en flaske vand.", "Nora requests a bottle of water."),
    )

    val tasks = listOf(
        matching("p2-1.1", listOf("an apple" to apple, "bread" to bread, "eats/is eating" to eat)),
        choice("p2-1.2", "E040", "billig", listOf("inexpensive", "expensive", "fresh"), "inexpensive", cheap),
        reading("p2-1.3", "What does Nora eat?", listOf("bread", "an apple"), s1Facts, "p2-s1-f2", "object", bread),

        choice("p2-2.1", "E040", "mælk", listOf("coffee", "bread", "milk", "apple"), "milk", milk),
        matching("p2-2.2", listOf("coffee" to coffee, "water" to water, "milk" to milk, "drinks/is drinking" to drink)),
        reading("p2-2.3", "Who drinks water?", listOf("Anna", "the speaker (jeg)", "Nora"), s2Facts, "p2-s2-f2", "subject", water),
        tiles(
            "p2-2.4", "Write Nora drinks milk. Begin with Nora.",
            listOf(tile("Nora"), tile("drikker", drink), tile("mælk", milk)), "p2-subject-first-supplied",
        ),

        matching("p2-3.1", listOf(
            "excuse me (getting attention)" to excuseMe, "here you are (handing something over)" to hereYouAre,
            "a bread roll" to roll, "a shop" to shop,
        )),
        context("p2-3.2", "A customer politely wants the shopkeeper's attention.", listOf("Undskyld.", "Værsgo."), "Undskyld.", excuseMe),
        reading("p2-3.3", "What does Nora eat?", listOf("a bread roll", "bread"), s3Facts, "p2-s3-f1", "object", roll),
        context("p2-3.4", "The shopkeeper hands the stated roll to the customer.", listOf("Værsgo.", "Undskyld."), "Værsgo.", hereYouAre),

        choice("p2-4.1", "E040", "køber", listOf("buys", "eats", "drinks", "pays"), "buys", buy),
        readingLast("p2-4.2", "What happens last in this stated situation?",
            listOf("the speaker buys a bread roll", "the speaker pays"), s4Facts,
            VocabularyP2Chronology("p2-s4-buy-before-pay", "p2-s4-f1", "p2-s4-f2")),
        tiles(
            "p2-4.3", "Write I buy a bread roll. Begin with the subject.",
            listOf(tile("Jeg"), tile("køber", buy), tile("en bolle", roll)), "p2-subject-first-supplied",
        ),

        choice("p2-5.1", "E040", "Jeg vil gerne købe en bolle.",
            listOf("a willing wish or polite preference", "a future-time label", "payment"),
            "a willing wish or polite preference", gladly),
        tiles("p2-5.2", "Write I would like to buy a bread roll in the taught frame. Begin with the subject.",
            listOf(tile("Jeg"), tile("vil", want), tile("gerne", gladly), tile("købe", buy), tile("en bolle", roll)),
            "p2-polite-wish"),
        context("p2-5.3", "The customer intends to thank the shopkeeper.",
            listOf("Undskyld.", "Tak.", "Værsgo."), "Tak.", thanks),

        matching("p2-6.1", listOf("a cup" to cup, "a bottle" to bottle)),
        reading("p2-6.2", "Who requests water in the two parallel notes?",
            listOf("the speaker (jeg)", "Nora"), s6Facts, "p2-s6-f2", "subject", water),
        requestTiles("p2-6.3", "Write I would like a cup of coffee, thank you in the taught request frame.", cupCoffee),
        requestTiles("p2-6.4", "Write I would like a bottle of water, thank you in the taught request frame.", bottleWater),
    )

    val formats = listOf(
        ExerciseFormat("E040", "Recognize an introduced Danish meaning", null, LexicalTask("meaning", "da-en")),
        ExerciseFormat("E041", "Choose a Danish response for a stated context", null, LexicalTask("meaning", "en-da")),
        ExerciseFormat("E042", "Match introduced meanings and Danish forms", null, LexicalTask("meaning", "en-da")),
        ExerciseFormat("E043", "Build a reviewed P2 sentence", "G005"),
        ExerciseFormat("E044", "Read an explicit P2 fact", null, readingTask = ReadingTask("explicit-fact")),
    )
    val formatIds = formats.map(ExerciseFormat::id).toSet()

    init {
        require(tasks.size == EXPECTED_TASK_COUNT && tasks.map(VocabularyP2Task::id).distinct().size == tasks.size)
        require(tasks.map(VocabularyP2Task::formatId).toSet() == formatIds)
        tasks.forEach(::validate)
    }

    fun tasks(formatId: String) = tasks.filter { it.formatId == formatId }

    private fun validate(task: VocabularyP2Task) {
        when (task.kind) {
            VocabularyP2TaskKind.CHOICE, VocabularyP2TaskKind.CONTEXT ->
                require(task.targets.size == 1 && task.expected in task.choices)
            VocabularyP2TaskKind.MATCHING -> require(
                task.matching.size in MIN_MATCHING_SIZE..MAX_MATCHING_SIZE &&
                    task.matching.map { it.first }.distinct().size == task.matching.size,
            )
            VocabularyP2TaskKind.TILES -> require(
                task.tiles.isNotEmpty() &&
                    task.targetScopeId in setOf("p2-subject-first-supplied", "p2-polite-wish", "p2-polite-request"),
            )
            VocabularyP2TaskKind.READING -> require(
                task.questionRole in setOf("object", "subject", "last-event") && task.facts.isNotEmpty() &&
                    (task.targetFactId != null || task.targetChronologyId != null),
            )
        }
    }

    private fun verb(id: String, sense: String, infinitive: String, present: String, gloss: String, use: String) =
        VocabularyP2Lexeme(id, sense, VocabularyLexicalKind.VERB_FORM, VocabularyLexicalForms(infinitive, present), gloss, use)
    private fun count(id: String, sense: String, bundle: String, gloss: String, use: String) =
        VocabularyP2Lexeme(id, sense, VocabularyLexicalKind.COUNT_BUNDLE, VocabularyLexicalForms(bundle = bundle), gloss, use)
    private fun mass(id: String, sense: String, form: String, gloss: String, use: String) =
        VocabularyP2Lexeme(id, sense, VocabularyLexicalKind.MASS_WORD, VocabularyLexicalForms(form = form), gloss, use)
    private fun adjective(id: String, sense: String, form: String, gloss: String, use: String) =
        VocabularyP2Lexeme(id, sense, VocabularyLexicalKind.ADJECTIVE, VocabularyLexicalForms(form = form), gloss, use)
    private fun adverb(id: String, sense: String, form: String, gloss: String, use: String) =
        VocabularyP2Lexeme(id, sense, VocabularyLexicalKind.ADVERB, VocabularyLexicalForms(form = form), gloss, use)
    private fun interjection(id: String, sense: String, form: String, gloss: String, use: String) =
        VocabularyP2Lexeme(id, sense, VocabularyLexicalKind.INTERJECTION, VocabularyLexicalForms(form = form), gloss, use)
    private fun tile(text: String, lexical: VocabularyP2Lexeme? = null) = VocabularyP2Tile(text, lexical)
    private fun choice(id: String, format: String, cue: String, choices: List<String>, expected: String, target: VocabularyP2Lexeme) =
        VocabularyP2Task(id, format, VocabularyP2TaskKind.CHOICE, cue, choices, expected, listOf(target))
    private fun context(id: String, cue: String, choices: List<String>, expected: String, target: VocabularyP2Lexeme) =
        VocabularyP2Task(id, "E041", VocabularyP2TaskKind.CONTEXT, cue, choices, expected, listOf(target))
    private fun matching(id: String, pairs: List<Pair<String, VocabularyP2Lexeme>>) = VocabularyP2Task(
        id, "E042", VocabularyP2TaskKind.MATCHING, "Match each English meaning to its introduced Danish form.",
        targets = pairs.map { it.second }, matching = pairs,
    )
    private fun reading(id: String, cue: String, choices: List<String>, facts: List<VocabularyP2Fact>, targetFact: String,
        role: String, target: VocabularyP2Lexeme) = VocabularyP2Task(
        id, "E044", VocabularyP2TaskKind.READING, cue, choices = choices, targets = listOf(target),
        passage = facts.map(VocabularyP2Fact::sentence), facts = facts, targetFactId = targetFact, questionRole = role,
    )
    private fun readingLast(
        id: String,
        cue: String,
        choices: List<String>,
        facts: List<VocabularyP2Fact>,
        relation: VocabularyP2Chronology,
    ) = VocabularyP2Task(
        id, "E044", VocabularyP2TaskKind.READING, cue, choices = choices,
        passage = facts.map(VocabularyP2Fact::sentence), facts = facts, questionRole = "last-event",
        chronology = listOf(relation), targetChronologyId = relation.id,
    )
    private fun tiles(id: String, cue: String, tiles: List<VocabularyP2Tile>, scope: String) =
        VocabularyP2Task(id, "E043", VocabularyP2TaskKind.TILES, cue, tiles = tiles, targetScopeId = scope)
    private fun requestTiles(id: String, cue: String, quantity: VocabularyP2Quantity) = tiles(
        id, cue,
        listOf(
            tile("Jeg"), tile("vil", want), tile("gerne", gladly), tile("have", haveRequest),
            VocabularyP2Tile(quantity.display, quantity = quantity), tile("tak", thanks),
        ),
        "p2-polite-request",
    ).copy(fixedCommaBeforeLast = true)
    private fun fact(id: String, referentId: String, display: String, answer: String, predicate: VocabularyP2Lexeme,
        objectSource: VocabularyP2Lexeme?, sentence: String, translation: String) = VocabularyP2Fact(
        id, referentId, display, answer, predicate, objectSource, null, sentence, translation,
        listOf("vocabulary-p2:referent:$referentId:$display", meaningKey(predicate), objectSource?.let(::meaningKey)).filterNotNull(),
    )
    private fun quantityFact(id: String, referentId: String, display: String, answer: String, quantity: VocabularyP2Quantity,
        sentence: String, translation: String) = VocabularyP2Fact(
        id, referentId, display, answer, haveRequest, null, quantity, sentence, translation,
        listOf("vocabulary-p2:referent:$referentId:$display", "vocabulary-p2:construction:${quantity.constructionId}"),
    )
    private fun meaningKey(lexeme: VocabularyP2Lexeme) = "vocabulary-p2:meaning:${lexeme.entryId}:${lexeme.senseId}"

    private const val EXPECTED_TASK_COUNT = 21
    private const val MIN_MATCHING_SIZE = 2
    private const val MAX_MATCHING_SIZE = 4
}

internal object VocabularyP2ExerciseGenerator {
    fun generate(request: GenerationRequest): GeneratedExercise {
        val candidates = VocabularyP2Content.tasks(request.formatId)
        val task = candidates[Math.floorMod(request.seed + request.index, candidates.size)]
        return when (task.kind) {
            VocabularyP2TaskKind.MATCHING -> matching(request, task)
            VocabularyP2TaskKind.TILES -> tiles(request, task)
            VocabularyP2TaskKind.CHOICE, VocabularyP2TaskKind.CONTEXT, VocabularyP2TaskKind.READING -> choice(request, task)
        }
    }

    private fun choice(request: GenerationRequest, task: VocabularyP2Task): GeneratedExercise {
        val choices = shuffled(task.choices, request.seed xor task.id.hashCode()) { it }
        val expected = expectedChoice(task)
        return exercise(
            request,
            task,
            ExerciseMaterials(
                prompt = ExercisePrompt(
                    "", "", choices = choices, cue = task.cue,
                    cueLanguage = if (task.kind in ENGLISH_CUE_KINDS) "en" else "da",
                    answerLanguage = if (task.kind == VocabularyP2TaskKind.CONTEXT) "da" else "en",
                    passage = task.passage.takeIf { it.isNotEmpty() },
                ),
                answerKey = AnswerKey.MeaningChoice(expected, task.choices.toSet()),
                lexicalTarget = task.targets.singleOrNull()
                    ?.takeUnless { task.kind == VocabularyP2TaskKind.READING }
                    ?.target(if (task.kind == VocabularyP2TaskKind.CONTEXT) "en-da" else "da-en"),
                readingTarget = if (task.kind == VocabularyP2TaskKind.READING) ReadingTask("explicit-fact") else null,
            ),
        )
    }

    private fun matching(request: GenerationRequest, task: VocabularyP2Task): GeneratedExercise {
        val left = shuffled(task.matching.map { it.first }, request.seed xor task.id.hashCode()) { it }
        val right = shuffled(
            task.matching.map { it.second },
            request.seed xor task.id.hashCode() xor MATCHING_COLUMN_SALT,
        ) { lexeme -> "${lexeme.entryId}:${lexeme.senseId}" }
        val leftItems = left.mapIndexed { index, text -> MatchingItem(opaque("l", request.seed, index, text), text) }
        val rightItems = right.mapIndexed { index, lexeme ->
            MatchingItem(opaque("r", request.seed, index, lexeme.display), lexeme.display)
        }
        val rightBySense = rightItems.associateBy { item -> right.single { it.display == item.text }.senseId }
        val leftByText = leftItems.associateBy(MatchingItem::text)
        val pairs = task.matching.map { (meaning, lexeme) ->
            VocabularyMatchingPairSourceDetails(leftByText.getValue(meaning).id, rightBySense.getValue(lexeme.senseId).id, lexeme.details())
        }
        return exercise(
            request,
            task,
            ExerciseMaterials(
                prompt = ExercisePrompt(
                    "", "", cue = task.cue, cueLanguage = "en", answerLanguage = "da",
                    matchingColumns = MatchingColumns(leftItems, rightItems),
                ),
                answerKey = AnswerKey.Matching(pairs.associate { it.leftId to it.rightId }),
                lexicalTargets = task.targets.map { it.target("en-da") },
                matchingPairs = pairs,
            ),
        )
    }

    private fun tiles(request: GenerationRequest, task: VocabularyP2Task): GeneratedExercise {
        val canonical = task.tiles.mapIndexed { index, tile -> GeneratedTile(index, opaque("b", request.seed, index, tile.text), tile) }
        val displayed = shuffled(canonical, request.seed xor task.id.hashCode()) { generated ->
            "${generated.index}:${generated.id}:${generated.tile.text}"
        }
        val punctuation = if (task.fixedCommaBeforeLast) {
            listOf(FixedPunctuation(canonical.last().id, ","))
        } else emptyList()
        val occurrences = canonical.map { generated ->
            VocabularyTileOccurrenceDetails(
                generated.id,
                generated.tile.lexical?.entryId ?: generated.tile.quantity?.head?.entryId ?: "p2:support:${generated.index}",
                generated.tile.text,
                generated.tile.lexical?.details(),
                generated.tile.quantity?.details(),
            )
        }
        return exercise(
            request,
            task,
            ExerciseMaterials(
                prompt = ExercisePrompt(
                    "", "", blocks = displayed.map { OrderBlock(it.id, it.tile.text) }, cue = task.cue,
                    cueLanguage = "en", answerLanguage = "da", tileMode = "use-all",
                    requiredTileCount = canonical.size, fixedPunctuation = punctuation,
                ),
                answerKey = AnswerKey.MeaningChoice("", emptySet()),
                tileOccurrences = occurrences,
                acceptedOrders = listOf(canonical.map(GeneratedTile::id)),
            ),
        )
    }

    private fun exercise(
        request: GenerationRequest,
        task: VocabularyP2Task,
        materials: ExerciseMaterials,
    ): GeneratedExercise {
        val sourceDetails = sourceDetails(task, materials)
        return GeneratedExercise(
            id = "${request.contentRevision}:${request.formatId}:${task.id}:${request.seed}:${request.index}",
            formatId = request.formatId,
            targetSkillId = if (task.kind == VocabularyP2TaskKind.TILES) "G005" else null,
            lexicalTarget = materials.lexicalTarget,
            supportSkillIds = if (task.kind == VocabularyP2TaskKind.TILES) listOf("G001", "G004") else emptyList(),
            presentation = presentation(task, materials.prompt),
            contentRevision = request.contentRevision,
            generatorRevision = request.generatorRevision,
            reproduction = Reproduction(request.seed, request.index),
            inspector = ExerciseInspector(
                constructionId = task.targetScopeId,
                lexicalIds = sourceDetails.lexicalSources.map(VocabularyLexicalSourceDetails::entryId),
                clauseRoles = emptyList(),
                canonicalSentence = canonicalSentence(task),
                vocabularySource = sourceDetails,
            ),
            source = VocabularyP2ExerciseSource(task, materials.answerKey),
            readingTarget = materials.readingTarget,
            lexicalTargets = materials.lexicalTargets,
        )
    }

    private fun sourceDetails(
        task: VocabularyP2Task,
        materials: ExerciseMaterials,
    ): VocabularyExerciseSourceDetails {
        val facts = supportFacts(task)
        return VocabularyExerciseSourceDetails(
            task.id,
            task.kind.name.lowercase(Locale.ROOT),
            task.targets.map(VocabularyP2Lexeme::details).distinctBy { it.entryId to it.senseId },
            expectedAnswer = when (task.kind) {
                VocabularyP2TaskKind.CHOICE, VocabularyP2TaskKind.CONTEXT -> task.expected
                VocabularyP2TaskKind.READING -> expectedChoice(task)
                else -> null
            },
            suppliedChoices = task.choices,
            supportFacts = facts,
            matchingPairs = materials.matchingPairs,
            tileOccurrences = materials.tileOccurrences,
            acceptedOrders = materials.acceptedOrders,
            targetSupportFactId = task.targetFactId,
            questionRole = task.questionRole,
            reviewedOrders = reviewedOrders(task),
            chronologyRelations = task.chronology.map {
                VocabularyChronologyRelationDetails(it.id, it.beforeFactId, it.afterFactId)
            },
            targetChronologyRelationId = task.targetChronologyId,
        )
    }

    private fun supportFacts(task: VocabularyP2Task): List<VocabularySupportFactDetails> = task.facts.map { fact ->
        VocabularySupportFactDetails(
            fact.id,
            fact.referentId,
            fact.predicate.entryId,
            fact.objectSource?.entryId ?: fact.quantitySource?.constructionId ?: fact.referentId,
            fact.answerValue(task.questionRole),
            task.questionRole ?: "support",
            fact.sentence,
            fact.exposureKeys,
            fact.referentDisplay,
            fact.predicate.details(),
            fact.objectSource?.details(),
            fact.quantitySource?.details(),
            fact.translation,
        )
    }

    private fun VocabularyP2Fact.answerValue(questionRole: String?) = when (questionRole) {
        "subject" -> referentAnswer
        "last-event" -> translation.removeSuffix(".").replaceFirstChar(Char::lowercase)
        else -> objectSource?.gloss ?: quantitySource?.display.orEmpty()
    }

    private fun reviewedOrders(task: VocabularyP2Task) = if (task.kind == VocabularyP2TaskKind.TILES) {
        listOf(
            VocabularyReviewedOrderDetails(
                task.tiles.map(VocabularyP2Tile::text), TargetStatus.MET, LinguisticStatus.ACCEPTABLE,
                MeaningSatisfaction.MET, ResponseConstraintStatus.MET, null,
            ),
        )
    } else emptyList()

    private fun presentation(task: VocabularyP2Task, prompt: ExercisePrompt) = ExercisePresentation(
        instruction = when (task.kind) {
            VocabularyP2TaskKind.MATCHING -> "Match each English meaning to its introduced Danish form."
            VocabularyP2TaskKind.TILES -> "Arrange all supplied blocks as requested."
            else -> "Choose the answer supported by the prompt."
        },
        context = "Use only the material introduced in this lesson.",
        prompt = prompt,
        introduction = emptyList(),
        hint = if (task.kind == VocabularyP2TaskKind.TILES) {
            "Use every supplied block once."
        } else {
            "Use the introduced meaning and scene."
        },
    )

    private fun canonicalSentence(task: VocabularyP2Task) = task.tiles.takeIf { it.isNotEmpty() }
        ?.joinToString(" ") { it.text }
        ?.let { sentenceWithPunctuation(it, task.fixedCommaBeforeLast) }

    private fun expectedChoice(task: VocabularyP2Task): String {
        if (task.kind != VocabularyP2TaskKind.READING) return task.expected
        val fact = if (task.questionRole == "last-event") {
            val relation = task.chronology.single { it.id == task.targetChronologyId }
            task.facts.single { it.id == relation.afterFactId }
        } else task.facts.single { it.id == task.targetFactId }
        return when (task.questionRole) {
            "subject" -> fact.referentAnswer
            "last-event" -> fact.translation.removeSuffix(".").replaceFirstChar(Char::lowercase)
            else -> requireNotNull(fact.objectSource).gloss
        }
    }

    private fun sentenceWithPunctuation(words: String, comma: Boolean) =
        (if (comma) words.replace(" tak", ", tak") else words).replaceFirstChar(Char::uppercase) + "."

    private fun <T> shuffled(values: List<T>, salt: Int, stableKey: (T) -> String): List<T> = values.sortedWith(
        compareBy<T> { value -> shuffleRank(stableKey(value), salt) }.thenBy(stableKey),
    )

    private fun shuffleRank(stableKey: String, salt: Int): Long {
        var mixed = salt.toLong() xor stableKey.hashCode().toLong() * MIX_MULTIPLIER_ONE
        mixed = (mixed xor (mixed ushr MIX_SHIFT_ONE)) * MIX_MULTIPLIER_TWO
        return mixed xor (mixed ushr MIX_SHIFT_TWO)
    }

    private fun opaque(prefix: String, seed: Int, index: Int, value: String): String =
        "$prefix-${(seed.toLong() xor index.toLong() * OPAQUE_FACTOR xor value.hashCode().toLong())
            .toULong().toString(HEX_RADIX)}"

    private data class ExerciseMaterials(
        val prompt: ExercisePrompt,
        val answerKey: AnswerKey,
        val lexicalTarget: LexicalTarget? = null,
        val lexicalTargets: List<LexicalTarget>? = null,
        val readingTarget: ReadingTask? = null,
        val matchingPairs: List<VocabularyMatchingPairSourceDetails> = emptyList(),
        val tileOccurrences: List<VocabularyTileOccurrenceDetails> = emptyList(),
        val acceptedOrders: List<List<String>> = emptyList(),
    )

    private data class GeneratedTile(val index: Int, val id: String, val tile: VocabularyP2Tile)

    private val ENGLISH_CUE_KINDS = setOf(VocabularyP2TaskKind.CONTEXT, VocabularyP2TaskKind.READING)
    private const val MATCHING_COLUMN_SALT = 0x55667788
    private const val OPAQUE_FACTOR = 0x9e3779b9L
    private const val MIX_MULTIPLIER_ONE = -7046029254386353131L
    private const val MIX_MULTIPLIER_TWO = -4658895280553007687L
    private const val MIX_SHIFT_ONE = 30
    private const val MIX_SHIFT_TWO = 27
    private const val HEX_RADIX = 16
}

internal object VocabularyP2ExerciseEvaluator {
    fun evaluate(exercise: GeneratedExercise, answer: Answer): Evaluation {
        val task = (exercise.source as VocabularyP2ExerciseSource).task
        return when (task.kind) {
            VocabularyP2TaskKind.MATCHING -> matching(exercise, answer, task)
            VocabularyP2TaskKind.TILES -> tiles(exercise, answer, task)
            VocabularyP2TaskKind.CHOICE, VocabularyP2TaskKind.CONTEXT, VocabularyP2TaskKind.READING -> choice(exercise, answer, task)
        }
    }

    private fun choice(exercise: GeneratedExercise, answer: Answer, task: VocabularyP2Task): Evaluation {
        val text = (answer as? Answer.Text)?.text?.let(::normalizeText)
        val supplied = task.choices.associateBy { it.lowercase(Locale.ROOT) }
        val expected = requireNotNull(exercise.inspector.vocabularySource).expectedAnswer.orEmpty()
        val outcome = when {
            text == null || text.lowercase(Locale.ROOT) !in supplied -> EvaluationOutcome(
                TargetStatus.UNEVALUABLE,
                LinguisticStatus.UNKNOWN,
                ResponseConstraintStatus.NOT_MET,
                MeaningSatisfaction.UNKNOWN,
                "SUPPLIED_CHOICE_REQUIRED",
                "Choose a supplied answer.",
            )
            text.equals(expected, true) -> EvaluationOutcome(
                TargetStatus.MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.MET,
                MeaningSatisfaction.MET, null, "Correct.",
            )
            else -> EvaluationOutcome(
                TargetStatus.NOT_MET,
                LinguisticStatus.ACCEPTABLE,
                ResponseConstraintStatus.MET,
                MeaningSatisfaction.NOT_MET,
                "INTRODUCED_MEANING_REQUIRED",
                "That supplied answer expresses another introduced meaning or fact.",
            )
        }
        return result(answer, NormalizedAnswer.Text(text.orEmpty()), task, outcome)
    }

    private fun matching(exercise: GeneratedExercise, answer: Answer, task: VocabularyP2Task): Evaluation {
        val pairs = (answer as? Answer.Matching)?.pairs ?: emptyList()
        val columns = requireNotNull(exercise.prompt.matchingColumns)
        if (!validMatchingSelection(pairs, columns, task.matching.size)) {
            return result(
                answer,
                NormalizedAnswer.Matching(pairs),
                task,
                EvaluationOutcome(
                    TargetStatus.UNEVALUABLE,
                    LinguisticStatus.UNKNOWN,
                    ResponseConstraintStatus.NOT_MET,
                    MeaningSatisfaction.UNKNOWN,
                    "MATCHING_SELECTION_INVALID",
                    "Use supplied left and right IDs at most once.",
                ),
            )
        }
        val source = requireNotNull(exercise.inspector.vocabularySource)
        val targetBySense = requireNotNull(exercise.lexicalTargets).associateBy(LexicalTarget::senseId)
        val selected = pairs.associate { it.leftId to it.rightId }
        val rows = source.matchingPairs.map { matchingResult(it, selected[it.leftId], targetBySense) }
        val complete = pairs.size == task.matching.size
        val wrong = rows.any { it.targetStatus == TargetStatus.NOT_MET }
        return Evaluation(
            if (complete && !wrong) TargetStatus.MET else if (complete) TargetStatus.NOT_MET else TargetStatus.UNEVALUABLE,
            if (pairs.isEmpty()) LinguisticStatus.UNKNOWN else LinguisticStatus.ACCEPTABLE,
            emptyList(), if (complete && !wrong) "All supplied meanings are matched." else "Review the pair feedback.", "", answer,
            NormalizedAnswer.Matching(pairs), LinguisticScope.DANISH_MATCHING,
            if (complete) ResponseConstraintStatus.MET else ResponseConstraintStatus.NOT_MET,
            if (complete && !wrong) MeaningSatisfaction.MET else if (wrong) MeaningSatisfaction.NOT_MET else MeaningSatisfaction.UNKNOWN,
            rows,
        )
    }

    private fun validMatchingSelection(
        pairs: List<MatchingPair>,
        columns: MatchingColumns,
        maximumSize: Int,
    ) = pairs.size <= maximumSize && pairs.map { it.leftId }.distinct().size == pairs.size &&
        pairs.map { it.rightId }.distinct().size == pairs.size && pairs.all { pair ->
            columns.left.any { it.id == pair.leftId } && columns.right.any { it.id == pair.rightId }
        }

    private fun matchingResult(
        expected: VocabularyMatchingPairSourceDetails,
        selectedRight: String?,
        targetBySense: Map<String, LexicalTarget>,
    ): MatchingPairResult {
        val target = targetBySense.getValue(expected.lexicalSource.senseId)
        return when {
            selectedRight == null -> MatchingPairResult(
                expected.leftId, null, target, TargetStatus.UNEVALUABLE, MeaningSatisfaction.UNKNOWN,
                ResponseConstraintStatus.NOT_MET, LinguisticStatus.UNKNOWN,
                "Choose a supplied Danish form for this meaning.",
            )
            selectedRight == expected.rightId -> MatchingPairResult(
                expected.leftId, selectedRight, target, TargetStatus.MET, MeaningSatisfaction.MET,
                ResponseConstraintStatus.MET, LinguisticStatus.ACCEPTABLE, "Correct. This pair matches.",
            )
            else -> MatchingPairResult(
                expected.leftId, selectedRight, target, TargetStatus.NOT_MET, MeaningSatisfaction.NOT_MET,
                ResponseConstraintStatus.MET, LinguisticStatus.ACCEPTABLE,
                "That supplied form expresses another introduced meaning.",
            )
        }
    }

    private fun tiles(exercise: GeneratedExercise, answer: Answer, task: VocabularyP2Task): Evaluation {
        val ids = (answer as? Answer.Order)?.blockIds ?: emptyList()
        val blocks = exercise.prompt.blocks.associateBy(OrderBlock::id)
        val valid = ids.distinct().size == ids.size && ids.all(blocks::containsKey)
        if (!valid) return result(
            answer,
            NormalizedAnswer.Order(ids),
            task,
            EvaluationOutcome(
                TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN, ResponseConstraintStatus.NOT_MET,
                MeaningSatisfaction.UNKNOWN, "TILE_SELECTION_INVALID", "Use supplied block IDs at most once.",
            ),
        )
        val source = requireNotNull(exercise.inspector.vocabularySource)
        val correct = ids in source.acceptedOrders
        val complete = ids.size == task.tiles.size
        val outcome = if (correct) {
            EvaluationOutcome(
                TargetStatus.MET,
                LinguisticStatus.ACCEPTABLE,
                ResponseConstraintStatus.MET,
                MeaningSatisfaction.MET,
                null,
                "Correct. The requested sentence and order are met.",
                requireNotNull(exercise.inspector.canonicalSentence),
            )
        } else {
            EvaluationOutcome(
                TargetStatus.UNEVALUABLE,
                LinguisticStatus.UNKNOWN,
                if (complete) ResponseConstraintStatus.MET else ResponseConstraintStatus.NOT_MET,
                MeaningSatisfaction.UNKNOWN,
                if (complete) "UNRECOGNIZED_P2_ORDER" else "REQUIRED_WORDS_MISSING",
                if (complete) {
                    "This order is outside the sentence reviewed in this lesson."
                } else {
                    "Use every supplied block once."
                },
            )
        }
        return result(answer, NormalizedAnswer.Order(ids), task, outcome)
    }

    private fun result(
        raw: Answer,
        normalized: NormalizedAnswer,
        task: VocabularyP2Task,
        outcome: EvaluationOutcome,
    ) = Evaluation(
        outcome.target,
        outcome.language,
        outcome.issue?.let { listOf(EvaluationIssue(it, outcome.feedback)) }.orEmpty(),
        outcome.feedback,
        outcome.sentence,
        raw,
        normalized,
        when (task.kind) {
            VocabularyP2TaskKind.CONTEXT -> LinguisticScope.DANISH_CHOICE
            VocabularyP2TaskKind.TILES -> LinguisticScope.DANISH_SENTENCE
            VocabularyP2TaskKind.MATCHING -> LinguisticScope.DANISH_MATCHING
            else -> LinguisticScope.ENGLISH_CHOICE
        },
        outcome.constraint,
        outcome.meaning,
    )

    private data class EvaluationOutcome(
        val target: TargetStatus,
        val language: LinguisticStatus,
        val constraint: ResponseConstraintStatus,
        val meaning: MeaningSatisfaction,
        val issue: String?,
        val feedback: String,
        val sentence: String = "",
    )
}
