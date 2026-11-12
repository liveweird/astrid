package app.astrid.engine

internal class VocabularyP1PracticePlanner(private val engine: PilotEngine) {
    fun plan(focusId: String, planRevision: String, seed: Int): PracticePlan {
        val specification = SPECIFICATIONS.singleOrNull { it.focusId == focusId && it.planRevision == planRevision }
            ?: throw IllegalArgumentException("Unsupported vocabulary P1 profile: $focusId/$planRevision")
        val items = specification.taskIds.mapIndexed { position, taskId ->
            val task = VocabularyP1Content.tasks.single { it.id == taskId }
            val exercise = generateTask(task, seed)
            practiceItem(position, task, exercise)
        }
        val introduction = specification.introduction + specification.review
        return PracticePlan(
            focusId = focusId,
            planRevision = planRevision,
            seed = seed,
            title = specification.title,
            prerequisiteSkillIds = PREREQUISITES,
            introduction = introduction,
            items = items,
            contentRevision = VocabularyP1Content.CONTENT_REVISION,
            generatorRevision = VocabularyP1Content.GENERATOR_REVISION,
            evaluationRevision = VocabularyP1Content.EVALUATION_REVISION,
            introductionExposureKeys = specification.introductionKeys,
            introductionRequired = true,
            completionRuleId = PracticeCompletionPolicy.VOCABULARY_P1_RULE,
            exposurePolicyId = PracticeFeedbackExposureCalculator.VOCABULARY_P1_EXPOSURE_POLICY,
        )
    }

    private fun generateTask(task: VocabularyP1Task, seed: Int): GeneratedExercise =
        (0 until VocabularyP1Content.tasks(task.formatId).size).asSequence().map { index ->
            engine.generate(
                GenerationRequest(
                    task.formatId,
                    task.targetSkillId,
                    seed,
                    index,
                    VocabularyP1Content.CONTENT_REVISION,
                    VocabularyP1Content.GENERATOR_REVISION,
                ),
            )
        }.first { requireNotNull(it.inspector.vocabularySource).taskId == task.id }

    private fun practiceItem(position: Int, task: VocabularyP1Task, exercise: GeneratedExercise): PracticePlanItem {
        val descriptor = descriptor(task, exercise)
        val assessments = if (task.kind == VocabularyP1TaskKind.MATCHING) {
            requireNotNull(exercise.inspector.vocabularySource).matchingPairs.map { pair ->
                val lexicalTarget = requireNotNull(exercise.lexicalTargets).single { it.senseId == pair.lexicalSource.senseId }
                PracticeAssessment(
                    pair.leftId,
                    PracticeTargetDescriptor(
                        pair.lexicalSource.gloss,
                        null,
                        lexicalTarget,
                        assessmentMode = "matching",
                    ),
                    "primary",
                    listOf(mappingKey(pair.lexicalSource.entryId, pair.lexicalSource.senseId)),
                )
            }
        } else {
            listOf(PracticeAssessment(null, descriptor, "primary", answerKeys(task)))
        }
        return PracticePlanItem(
            position,
            exercise,
            descriptor,
            answerKeys(task),
            promptKeys(task, exercise),
            lessonKeys(task),
            hintKeys(task),
            feedbackKeys(task),
            assessments,
        )
    }

    private fun descriptor(task: VocabularyP1Task, exercise: GeneratedExercise): PracticeTargetDescriptor = when {
        task.targetSkillId != null -> PracticeTargetDescriptor(
            label = when (task.targetSkillId) {
                "G004" -> "${task.targetScopeId} — ${task.assessmentMode}"
                else -> "Subject-first supplied order"
            },
            targetSkillId = task.targetSkillId,
            lexicalTarget = null,
            assessmentMode = task.assessmentMode,
            targetScopeId = task.targetScopeId,
        )
        task.kind == VocabularyP1TaskKind.READING -> PracticeTargetDescriptor(
            label = "Explicit ${task.factRole} fact",
            targetSkillId = null,
            lexicalTarget = null,
            readingTarget = ReadingTask("explicit-fact"),
            readingRole = task.factRole,
        )
        task.kind == VocabularyP1TaskKind.MATCHING -> PracticeTargetDescriptor(
            label = "Vocabulary matching board (${task.matching.size} pairs)",
            targetSkillId = null,
            lexicalTarget = null,
            matchingTarget = LexicalTask("meaning", "en-da"),
        )
        else -> PracticeTargetDescriptor(
            label = task.targets.single().gloss,
            targetSkillId = null,
            lexicalTarget = requireNotNull(exercise.lexicalTarget),
        )
    }

    private fun answerKeys(task: VocabularyP1Task): List<String> = when (task.kind) {
        VocabularyP1TaskKind.MATCHING -> task.targets.map { mappingKey(it.entryId, it.senseId) }
        VocabularyP1TaskKind.READING -> listOf(factKey(requireNotNull(task.targetSupportFactId)))
        VocabularyP1TaskKind.TILES -> listOf(orderKey(task.id))
        VocabularyP1TaskKind.CHOICE -> task.targets.map { meaningKey(it.entryId, it.senseId) }
        VocabularyP1TaskKind.RECALL -> {
            val target = task.targets.single()
            listOf(meaningKey(target.entryId, target.senseId), formKey(target.entryId, task.expected))
        }
    }

    private fun promptKeys(task: VocabularyP1Task, exercise: GeneratedExercise): List<String> = when (task.kind) {
        VocabularyP1TaskKind.CHOICE -> when (task.formatId) {
            "E031" -> listOf(displayKey(task.targets.single()))
            "E032" -> exercise.prompt.choices.map { choice ->
                displayKey(VocabularyP1Content.placements.single { it.display == choice })
            }
            else -> emptyList()
        }
        VocabularyP1TaskKind.MATCHING -> {
            val source = requireNotNull(exercise.inspector.vocabularySource)
            val pairByRightId = source.matchingPairs.associateBy(VocabularyMatchingPairSourceDetails::rightId)
            requireNotNull(exercise.prompt.matchingColumns).right.map { item ->
                displayKey(pairByRightId.getValue(item.id).lexicalSource)
            }
        }
        VocabularyP1TaskKind.RECALL -> if (task.formatId == "E034") {
            val target = task.targets.single()
            listOf(formKey(target.entryId, requireNotNull(target.forms.infinitive)))
        } else {
            emptyList()
        }
        VocabularyP1TaskKind.READING -> task.supportFacts.flatMap { fact ->
            fact.exposureKeys + factKey(fact.id)
        }
        VocabularyP1TaskKind.TILES -> requireNotNull(exercise.inspector.vocabularySource).tileOccurrences.map {
            formKey(it.lexicalId, it.form)
        }
    }.distinct()

    private fun hintKeys(task: VocabularyP1Task): List<String> = when (task.kind) {
        VocabularyP1TaskKind.TILES -> listOf(P1_ORDER_SUPPORT_KEY)
        else -> emptyList()
    }

    private fun feedbackKeys(task: VocabularyP1Task): List<String> = answerKeys(task)
    private fun lessonKeys(task: VocabularyP1Task): List<String> = (
        task.targets.flatMap(::teachingKeys) + if (task.kind == VocabularyP1TaskKind.MATCHING) {
            task.targets.map { mappingKey(it.entryId, it.senseId) }
        } else {
            emptyList()
        }
    ).distinct()

    private fun lexicalKeys(lexeme: VocabularyP1Lexeme) = listOf(meaningKey(lexeme.entryId, lexeme.senseId)) + visibleFormKeys(lexeme)
    private fun teachingKeys(lexeme: VocabularyP1Lexeme) = lexicalKeys(lexeme) +
        if (lexeme.entryId == "L032") listOf(formKey(lexeme.entryId, "i Odense")) else emptyList()
    private fun visibleFormKeys(lexeme: VocabularyP1Lexeme) = buildList {
        lexeme.forms.infinitive?.let { add(formKey(lexeme.entryId, it)) }
        lexeme.forms.present?.let { add(formKey(lexeme.entryId, it)) }
        lexeme.forms.bundle?.let { add(bundleKey(lexeme.entryId, it)) }
        lexeme.forms.form?.let { add(formKey(lexeme.entryId, it)) }
    }
    private fun displayKey(lexeme: VocabularyP1Lexeme): String = when {
        lexeme.forms.present != null -> formKey(lexeme.entryId, lexeme.forms.present)
        lexeme.forms.bundle != null -> bundleKey(lexeme.entryId, lexeme.forms.bundle)
        else -> formKey(lexeme.entryId, requireNotNull(lexeme.forms.form))
    }
    private fun displayKey(source: VocabularyLexicalSourceDetails): String = when {
        source.forms.present != null -> formKey(source.entryId, source.forms.present)
        source.forms.bundle != null -> bundleKey(source.entryId, source.forms.bundle)
        else -> formKey(source.entryId, requireNotNull(source.forms.form))
    }

    internal data class Specification(
        val focusId: String,
        val planRevision: String,
        val title: String,
        val taskIds: List<String>,
        val introduction: List<IntroductionSection>,
        val review: List<IntroductionSection>,
        val introductionKeys: List<String>,
    ) {
        fun profile() = PracticeProfile(
            focusId,
            planRevision,
            title,
            "Learn and practise the vocabulary, reading, and sentence material in this lesson.",
            taskIds.size,
            PREREQUISITES,
        )
    }

    companion object {
        val PREREQUISITES = listOf("G001", "G002", "G003", "G004", "G005")
        const val P1_ORDER_SUPPORT_KEY = "vocabulary-p1:support:subject-first-order"

        val SPECIFICATIONS = listOf(
            specification(1, "Work, reading and today", listOf("p1-1.1", "p1-1.2", "p1-1.3", "p1-1.4"),
                listOf(VocabularyP1Content.work, VocabularyP1Content.read, VocabularyP1Content.book, VocabularyP1Content.today),
                listOf("Anna arbejder i dag." to "Anna works today.", "Hun læser en bog." to "She reads a book.")),
            specification(2, "Reading and writing", listOf("p1-2.1", "p1-2.2", "p1-2.3", "p1-2.4"),
                listOf(VocabularyP1Content.newspaper, VocabularyP1Content.letter, VocabularyP1Content.write,
                    VocabularyP1Content.computer),
                listOf(
                    "Nora skriver et brev." to "Nora writes a letter.",
                    "Hun har en computer." to "She has a computer.",
                    "Hun læser en avis." to "She reads a newspaper.",
                )),
            specification(3, "Study, courses and Danish", listOf("p1-3.1", "p1-3.2", "p1-3.3", "p1-3.4", "p1-3.5"),
                listOf(VocabularyP1Content.study, VocabularyP1Content.course, VocabularyP1Content.danish, VocabularyP1Content.now),
                listOf("Nora studerer." to "Nora studies.", "Hun læser en bog nu." to "She reads a book now.")),
            specification(4, "Coffee and water", listOf("p1-4.1", "p1-4.2", "p1-4.3", "p1-4.4", "p1-4.5"),
                listOf(VocabularyP1Content.drink, VocabularyP1Content.coffee, VocabularyP1Content.water),
                listOf("Anna drikker kaffe." to "Anna drinks coffee.", "Nora drikker vand." to "Nora drinks water.")),
            specification(5, "Where and when", listOf("p1-5.1", "p1-5.2", "p1-5.3", "p1-5.4", "p1-5.5"),
                listOf(VocabularyP1Content.city, VocabularyP1Content.home, VocabularyP1Content.morning),
                listOf(
                    "Anna arbejder i Aarhus." to "Anna works in Aarhus.",
                    "Nora studerer hjemme." to "Nora studies at home.",
                    "Nora arbejder om morgenen." to "Nora works in the morning.",
                )),
        )

        private fun specification(number: Int, title: String, taskIds: List<String>, lexemes: List<VocabularyP1Lexeme>,
            examples: List<Pair<String, String>>): Specification {
            val focus = "vocabulary-p1-s$number"
            val teaching = lexemes.map(::teachingSection)
            val review = supportReview(number) + examples.map { (danish, english) ->
                IntroductionSection("Reviewed example — $english", danish, "da")
            }
            val sessionFacts = VocabularyP1Content.tasks.filter { it.id.startsWith("p1-$number.") }
                .flatMap(VocabularyP1Task::supportFacts).distinctBy(VocabularyP1Fact::id)
            val visiblyPaired = VocabularyP1Content.tasks.filter { it.id in taskIds && it.kind == VocabularyP1TaskKind.MATCHING }
                .flatMap(VocabularyP1Task::targets)
                .filter { target -> lexemes.any { it.entryId == target.entryId && it.senseId == target.senseId } }
            val keys = lexemes.flatMap { listOf(meaningKey(it.entryId, it.senseId)) + listOfNotNull(
                it.forms.infinitive?.let { form -> formKey(it.entryId, form) },
                it.forms.present?.let { form -> formKey(it.entryId, form) },
                it.forms.bundle?.let { form -> bundleKey(it.entryId, form) },
                it.forms.form?.let { form -> formKey(it.entryId, form) },
            ) + if (it.entryId == "L032") listOf(formKey(it.entryId, "i Odense")) else emptyList() } +
                visiblyPaired.map { mappingKey(it.entryId, it.senseId) } +
                sessionFacts.flatMap { it.exposureKeys + factKey(it.id) } +
                additionalReviewKeys(number) +
                P1_ORDER_SUPPORT_KEY
            return Specification(focus, "$focus-1", title, taskIds, teaching, review, keys)
        }

        private fun teachingSection(lexeme: VocabularyP1Lexeme): IntroductionSection {
            return IntroductionSection(lexeme.gloss, lexeme.teachingDisplay, "da")
        }

        private fun supportReview(number: Int): List<IntroductionSection> = when (number) {
            1 -> listOf(IntroductionSection("Support — she", "hun", "da"))
            2 -> listOf(
                IntroductionSection("Review — reads / a book", "læser / en bog", "da"),
                IntroductionSection("Support — has", "har", "da"),
            )
            SESSION_THREE -> listOf(
                IntroductionSection("Support — she", "hun", "da"),
                IntroductionSection("Review — begin with the subject", "hun læser …", "da"),
            )
            SESSION_FOUR -> listOf(
                IntroductionSection("Support — people", "Anna / Nora", "da"),
                IntroductionSection("Review — begin with the subject", "Nora drikker …", "da"),
            )
            SESSION_FIVE -> listOf(
                IntroductionSection("Review — I work / Nora studies", "jeg arbejder / Nora studerer", "da"),
                IntroductionSection("Review — city choices", "i Aarhus / i Odense", "da"),
            )
            else -> error("Unsupported vocabulary session $number")
        }

        private fun additionalReviewKeys(number: Int): List<String> = when (number) {
            2 -> listOf(formKey("L011", "læser"), bundleKey("L014", "en bog"))
            SESSION_FIVE -> listOf(formKey("L001", "jeg"), formKey("L032", "i Odense"))
            else -> emptyList()
        }

        fun meaningKey(entryId: String, senseId: String) = "vocabulary-p1:meaning:$entryId:$senseId"
        fun formKey(entryId: String, form: String) = "vocabulary-p1:form:$entryId:$form"
        fun bundleKey(entryId: String, form: String) = "vocabulary-p1:bundle:$entryId:$form"
        fun mappingKey(entryId: String, senseId: String) = "vocabulary-p1:mapping:$entryId:$senseId"
        fun factKey(taskId: String) = "vocabulary-p1:proposition:$taskId"
        fun orderKey(taskId: String) = "vocabulary-p1:order:$taskId"

        private const val SESSION_THREE = 3
        private const val SESSION_FOUR = 4
        private const val SESSION_FIVE = 5
    }
}
