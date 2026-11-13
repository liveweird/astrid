package app.astrid.engine

internal class VocabularyP2PracticePlanner(private val engine: PilotEngine) {
    fun plan(focusId: String, planRevision: String, seed: Int): PracticePlan {
        val specification = SPECIFICATIONS.singleOrNull { it.focusId == focusId && it.planRevision == planRevision }
            ?: throw IllegalArgumentException("Unsupported vocabulary P2 profile: $focusId/$planRevision")
        val items = specification.taskIds.mapIndexed { position, taskId ->
            val task = VocabularyP2Content.tasks.single { it.id == taskId }
            val exercise = generateTask(task, seed)
            practiceItem(position, task, exercise)
        }
        return PracticePlan(
            focusId = focusId,
            planRevision = planRevision,
            seed = seed,
            title = specification.title,
            prerequisiteSkillIds = PREREQUISITES,
            introduction = specification.introduction,
            items = items,
            contentRevision = VocabularyP2Content.CONTENT_REVISION,
            generatorRevision = VocabularyP2Content.GENERATOR_REVISION,
            evaluationRevision = VocabularyP2Content.EVALUATION_REVISION,
            introductionExposureKeys = specification.introductionKeys,
            introductionRequired = true,
            completionRuleId = PracticeCompletionPolicy.VOCABULARY_P2_RULE,
            exposurePolicyId = PracticeFeedbackExposureCalculator.VOCABULARY_P2_EXPOSURE_POLICY,
        )
    }

    private fun generateTask(task: VocabularyP2Task, seed: Int): GeneratedExercise =
        (0 until VocabularyP2Content.tasks(task.formatId).size).asSequence().map { index ->
            engine.generate(
                GenerationRequest(task.formatId, if (task.kind == VocabularyP2TaskKind.TILES) "G005" else null, seed, index,
                    VocabularyP2Content.CONTENT_REVISION, VocabularyP2Content.GENERATOR_REVISION),
            )
        }.first { requireNotNull(it.inspector.vocabularySource).taskId == task.id }

    private fun practiceItem(position: Int, task: VocabularyP2Task, exercise: GeneratedExercise): PracticePlanItem {
        val descriptor = descriptor(task, exercise)
        val assessments = if (task.kind == VocabularyP2TaskKind.MATCHING) {
            requireNotNull(exercise.inspector.vocabularySource).matchingPairs.map { pair ->
                val target = requireNotNull(exercise.lexicalTargets).single { it.senseId == pair.lexicalSource.senseId }
                PracticeAssessment(
                    pair.leftId,
                    PracticeTargetDescriptor(pair.lexicalSource.gloss, null, target, assessmentMode = "matching"),
                    "primary",
                    listOf(mappingKey(pair.lexicalSource.entryId, pair.lexicalSource.senseId)),
                )
            }
        } else listOf(PracticeAssessment(null, descriptor, "primary", answerKeys(task)))
        return PracticePlanItem(
            position, exercise, descriptor, answerKeys(task), promptKeys(task, exercise), lessonKeys(task),
            if (task.kind == VocabularyP2TaskKind.TILES) listOf(ORDER_SUPPORT_KEY) else emptyList(),
            answerKeys(task), assessments,
        )
    }

    private fun descriptor(task: VocabularyP2Task, exercise: GeneratedExercise): PracticeTargetDescriptor = when (task.kind) {
        VocabularyP2TaskKind.TILES -> PracticeTargetDescriptor(
            task.targetScopeId.orEmpty(), "G005", null, assessmentMode = "word-order", targetScopeId = task.targetScopeId,
        )
        VocabularyP2TaskKind.READING -> PracticeTargetDescriptor(
            "Explicit ${task.questionRole} fact", null, null, readingTarget = ReadingTask("explicit-fact"),
            readingRole = task.questionRole,
        )
        VocabularyP2TaskKind.MATCHING -> PracticeTargetDescriptor(
            "Vocabulary matching board (${task.matching.size} pairs)", null, null,
            matchingTarget = LexicalTask("meaning", "en-da"),
        )
        VocabularyP2TaskKind.CHOICE, VocabularyP2TaskKind.CONTEXT -> PracticeTargetDescriptor(
            task.targets.single().gloss, null, requireNotNull(exercise.lexicalTarget),
        )
    }

    private fun answerKeys(task: VocabularyP2Task): List<String> = when (task.kind) {
        VocabularyP2TaskKind.MATCHING -> task.targets.map { mappingKey(it.entryId, it.senseId) }
        VocabularyP2TaskKind.READING -> listOf(
            task.targetFactId?.let(::factKey) ?: chronologyKey(requireNotNull(task.targetChronologyId)),
        )
        VocabularyP2TaskKind.TILES -> listOf(orderKey(task.id))
        VocabularyP2TaskKind.CHOICE, VocabularyP2TaskKind.CONTEXT -> task.targets.map(::meaningKey)
    }

    private fun promptKeys(task: VocabularyP2Task, exercise: GeneratedExercise): List<String> = when (task.kind) {
        VocabularyP2TaskKind.MATCHING -> requireNotNull(exercise.inspector.vocabularySource).matchingPairs.map {
            formKey(it.lexicalSource.entryId, it.lexicalSource.forms.present ?: it.lexicalSource.forms.bundle ?:
                requireNotNull(it.lexicalSource.forms.form))
        }
        VocabularyP2TaskKind.TILES -> requireNotNull(exercise.inspector.vocabularySource).tileOccurrences.flatMap { occurrence ->
            listOfNotNull(
                occurrence.lexicalSource?.let { formKey(it.entryId, occurrence.form) },
                occurrence.quantitySource?.let { quantityKey(it.constructionId) },
            )
        }
        VocabularyP2TaskKind.READING -> task.facts.flatMap { it.exposureKeys + factKey(it.id) } +
            task.chronology.map { chronologyKey(it.id) }
        VocabularyP2TaskKind.CHOICE, VocabularyP2TaskKind.CONTEXT -> task.targets.map(::displayKey)
    }.distinct()

    private fun lessonKeys(task: VocabularyP2Task): List<String> = (
        task.targets.flatMap(::teachingKeys) + task.matching.map { mappingKey(it.second.entryId, it.second.senseId) } +
            task.facts.flatMap { it.exposureKeys + factKey(it.id) } + task.chronology.map { chronologyKey(it.id) } +
            task.tiles.mapNotNull { it.quantity?.let { quantity -> quantityKey(quantity.constructionId) } }
        ).distinct()

    internal data class Specification(
        val focusId: String,
        val planRevision: String,
        val title: String,
        val taskIds: List<String>,
        val introduction: List<IntroductionSection>,
        val introductionKeys: List<String>,
    ) {
        fun profile() = PracticeProfile(
            focusId, planRevision, title,
            "Learn and practise the vocabulary, reading, and sentence material in this lesson.",
            taskIds.size, PREREQUISITES,
        )
    }

    companion object {
        val PREREQUISITES = listOf("G001", "G002", "G003", "G004", "G005")
        const val ORDER_SUPPORT_KEY = "vocabulary-p2:support:reviewed-order"
        private const val ATTENTION_LESSON = 3
        private const val PURCHASE_LESSON = 4
        private const val WISH_LESSON = 5
        private const val REQUEST_LESSON = 6

        val SPECIFICATIONS = listOf(
            specification(1, "A small breakfast", listOf("p2-1.1", "p2-1.2", "p2-1.3"),
                listOf(VocabularyP2Content.eat, VocabularyP2Content.apple, VocabularyP2Content.bread, VocabularyP2Content.cheap),
                listOf("Jeg spiser et æble." to "I eat an apple.", "Nora spiser brød." to "Nora eats bread.")),
            specification(2, "Drinks on the table", listOf("p2-2.1", "p2-2.2", "p2-2.3", "p2-2.4"),
                listOf(VocabularyP2Content.drink, VocabularyP2Content.coffee, VocabularyP2Content.water, VocabularyP2Content.milk,
                    VocabularyP2Content.bread, VocabularyP2Content.apple),
                listOf("Anna drikker kaffe." to "Anna drinks coffee.", "Jeg drikker vand." to "I drink water.",
                    "Nora drikker mælk." to "Nora drinks milk.")),
            specification(3, "Getting attention and receiving a roll", listOf("p2-3.1", "p2-3.2", "p2-3.3", "p2-3.4"),
                listOf(VocabularyP2Content.excuseMe, VocabularyP2Content.hereYouAre, VocabularyP2Content.roll,
                    VocabularyP2Content.shop, VocabularyP2Content.eat, VocabularyP2Content.bread),
                listOf("Det er en butik." to "This is a shop.", "Nora spiser en bolle." to "Nora eats a bread roll.",
                    "Undskyld." to "Excuse me (getting attention).", "Værsgo." to "Here you are (handing something over).")),
            specification(4, "Buying and paying", listOf("p2-4.1", "p2-4.2", "p2-4.3"),
                listOf(VocabularyP2Content.cafe, VocabularyP2Content.buy, VocabularyP2Content.pay,
                    VocabularyP2Content.roll, VocabularyP2Content.eat, VocabularyP2Content.drink),
                listOf("Jeg køber en bolle i en café." to "I buy a bread roll in a café.", "Jeg betaler." to "I pay.")),
            specification(5, "Marking a polite wish", listOf("p2-5.1", "p2-5.2", "p2-5.3"),
                listOf(VocabularyP2Content.thanks, VocabularyP2Content.gladly, VocabularyP2Content.want,
                    VocabularyP2Content.buy, VocabularyP2Content.roll, VocabularyP2Content.excuseMe, VocabularyP2Content.hereYouAre),
                listOf("Jeg vil gerne købe en bolle." to "I would like to buy a bread roll.", "Tak." to "Thank you.")),
            specification(6, "Requesting a supplied quantity", listOf("p2-6.1", "p2-6.2", "p2-6.3", "p2-6.4"),
                listOf(VocabularyP2Content.haveRequest, VocabularyP2Content.cup, VocabularyP2Content.bottle,
                    VocabularyP2Content.coffee, VocabularyP2Content.water, VocabularyP2Content.thanks,
                    VocabularyP2Content.want, VocabularyP2Content.gladly),
                listOf("Jeg vil gerne have en kop kaffe, tak." to "I would like a cup of coffee, thank you.",
                    "Nora vil gerne have en flaske vand." to "Nora would like a bottle of water.",
                    "Jeg vil gerne have en flaske vand, tak." to "I would like a bottle of water, thank you.")),
        )

        private fun specification(number: Int, title: String, taskIds: List<String>, lexemes: List<VocabularyP2Lexeme>,
            examples: List<Pair<String, String>>): Specification {
            val focusId = "vocabulary-p2-s$number"
            val tasks = VocabularyP2Content.tasks.filter { it.id in taskIds }
            val introduction = lexemes.distinctBy { it.entryId to it.senseId }.map(::teachingSection) +
                examples.map { (danish, english) -> IntroductionSection("Reviewed example — $english", danish, "da") } +
                supportSections(number)
            val keys = lexemes.flatMap(::teachingKeys) + tasks.flatMap { task ->
                task.facts.flatMap { it.exposureKeys + factKey(it.id) } + task.chronology.map { chronologyKey(it.id) } +
                    task.matching.map { mappingKey(it.second.entryId, it.second.senseId) } +
                    task.tiles.mapNotNull { it.quantity?.let { quantity -> quantityKey(quantity.constructionId) } }
            } + additionalSupportKeys(number) + ORDER_SUPPORT_KEY
            return Specification(focusId, "$focusId-1", title, taskIds, introduction, keys.distinct())
        }

        private fun supportSections(number: Int): List<IntroductionSection> = when (number) {
            1 -> listOf(IntroductionSection("Support — I and names", "jeg = I; Nora is a name", "en"))
            2 -> listOf(IntroductionSection("Support — people and subject-first order", "jeg = I; Anna and Nora are names", "en"))
            ATTENTION_LESSON -> listOf(
                IntroductionSection("Attention scene", "Undskyld. — Excuse me when getting attention.", "en"),
                IntroductionSection("Handover scene", "Værsgo. — Here you are when handing over the item.", "en"),
            )
            PURCHASE_LESSON -> listOf(
                IntroductionSection("Supplied location", "i en café = in a café", "da"),
                IntroductionSection("Stated event order",
                    "First, the speaker buys a bread roll in a café. After that purchase, the speaker pays. Paying is last.", "en"),
            )
            WISH_LESSON -> listOf(
                IntroductionSection("Bare infinitive after vil", "Jeg | vil | gerne | købe | en bolle", "da"),
                IntroductionSection("Functional contrast",
                    "Undskyld gets attention; Værsgo accompanies a handover; Tak thanks the shopkeeper.", "en"),
            )
            REQUEST_LESSON -> listOf(
                IntroductionSection("A cup of coffee", "en kop kaffe", "da"),
                IntroductionSection("A bottle of water", "en flaske vand", "da"),
                IntroductionSection("Request and possession are different uses", "request have ≠ possession har = has", "da"),
                IntroductionSection("Request frame", "vil is finite; have remains the bare infinitive", "en"),
            )
            else -> emptyList()
        }

        private fun additionalSupportKeys(number: Int): List<String> = when (number) {
            PURCHASE_LESSON -> listOf("vocabulary-p2:support:i-en-cafe", "vocabulary-p2:support:buy-before-pay")
            WISH_LESSON -> listOf(
                "vocabulary-p2:support:bare-infinitive-after-vil",
                "vocabulary-p2:support:functional-contrast",
            )
            REQUEST_LESSON -> listOf(
                "vocabulary-p2:support:possession-har",
                "vocabulary-p2:support:request-have-not-possession-har",
                quantityKey(VocabularyP2Content.cupCoffee.constructionId),
                quantityKey(VocabularyP2Content.bottleWater.constructionId),
            )
            else -> emptyList()
        }

        private fun teachingSection(lexeme: VocabularyP2Lexeme): IntroductionSection {
            val display = when {
                lexeme.forms.infinitive != null && lexeme.forms.present != null ->
                    "at ${lexeme.forms.infinitive} → ${lexeme.forms.present}"
                else -> lexeme.display
            }
            return IntroductionSection(lexeme.gloss.replaceFirstChar(Char::uppercase), display, "da")
        }

        private fun teachingKeys(lexeme: VocabularyP2Lexeme) = listOf(meaningKey(lexeme)) + listOfNotNull(
            lexeme.forms.infinitive?.let { formKey(lexeme.entryId, it) },
            lexeme.forms.present?.let { formKey(lexeme.entryId, it) },
            lexeme.forms.bundle?.let { bundleKey(lexeme.entryId, it) },
            lexeme.forms.form?.let { formKey(lexeme.entryId, it) },
        )

        private fun displayKey(lexeme: VocabularyP2Lexeme): String = when {
            lexeme.forms.present != null -> formKey(lexeme.entryId, lexeme.forms.present)
            lexeme.forms.bundle != null -> bundleKey(lexeme.entryId, lexeme.forms.bundle)
            else -> formKey(lexeme.entryId, requireNotNull(lexeme.forms.form))
        }

        fun meaningKey(lexeme: VocabularyP2Lexeme) = when (lexeme.entryId) {
            "L045", "L046", "L047" -> VocabularyP1PracticePlanner.meaningKey(lexeme.entryId, lexeme.senseId)
            "L034" -> "module-one:modal-meaning:L034:L034-S01"
            else -> "vocabulary-p2:meaning:${lexeme.entryId}:${lexeme.senseId}"
        }
        fun formKey(entryId: String, form: String) = when (entryId) {
            "L045", "L046", "L047" -> VocabularyP1PracticePlanner.formKey(entryId, form)
            "L034" -> if (form == "ville") "module-one:verb-infinitive:L034" else "module-one:verb-present:L034"
            else -> "vocabulary-p2:form:$entryId:$form"
        }
        fun bundleKey(entryId: String, form: String) = "vocabulary-p2:bundle:$entryId:$form"
        fun mappingKey(entryId: String, senseId: String) = when (entryId) {
            "L045", "L046", "L047" -> VocabularyP1PracticePlanner.mappingKey(entryId, senseId)
            else -> "vocabulary-p2:mapping:$entryId:$senseId"
        }
        fun factKey(factId: String) = "vocabulary-p2:proposition:$factId"
        fun chronologyKey(relationId: String) = "vocabulary-p2:chronology:$relationId"
        fun quantityKey(constructionId: String) = "vocabulary-p2:quantity:$constructionId"
        fun orderKey(taskId: String) = "vocabulary-p2:order:$taskId"
    }
}
