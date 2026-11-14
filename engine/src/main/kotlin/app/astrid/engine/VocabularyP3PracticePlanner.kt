@file:Suppress("MagicNumber")

package app.astrid.engine

internal class VocabularyP3PracticePlanner(private val engine: PilotEngine) {
    fun plan(focusId: String, planRevision: String, seed: Int): PracticePlan {
        val specification = SPECIFICATIONS.singleOrNull { it.focusId == focusId && it.planRevision == planRevision }
            ?: throw IllegalArgumentException("Unsupported vocabulary P3 profile: $focusId/$planRevision")
        val items = specification.taskIds.mapIndexed { position, taskId ->
            val task = VocabularyP3Content.tasks.single { it.id == taskId }
            val exercise = generateTask(task, seed)
            item(position, task, exercise, specification.introductionKeys)
        }
        return PracticePlan(
            focusId, planRevision, seed, specification.title, PREREQUISITES, specification.introduction, items,
            VocabularyP3Content.CONTENT_REVISION, VocabularyP3Content.GENERATOR_REVISION,
            VocabularyP3Content.EVALUATION_REVISION, specification.introductionKeys, true,
            PracticeCompletionPolicy.VOCABULARY_P3_RULE, PracticeFeedbackExposureCalculator.VOCABULARY_P3_EXPOSURE_POLICY,
        )
    }

    private fun generateTask(task: VocabularyP3Task, seed: Int): GeneratedExercise =
        (0 until VocabularyP3Content.tasks(task.formatId).size).asSequence().map { index ->
            engine.generate(GenerationRequest(task.formatId, targetSkill(task), seed, index,
                VocabularyP3Content.CONTENT_REVISION, VocabularyP3Content.GENERATOR_REVISION))
        }.first { requireNotNull(it.inspector.vocabularySource).taskId == task.id }

    private fun item(position: Int, task: VocabularyP3Task, exercise: GeneratedExercise,
        introductionKeys: List<String>): PracticePlanItem {
        val descriptor = descriptor(task, exercise)
        val assessments = if (task.kind == VocabularyP3TaskKind.MATCHING) {
            requireNotNull(exercise.inspector.vocabularySource).matchingPairs.map { pair ->
                val target = requireNotNull(exercise.lexicalTargets).single { it.senseId == pair.lexicalSource.senseId }
                PracticeAssessment(pair.leftId,
                    PracticeTargetDescriptor(pair.lexicalSource.gloss, null, target, assessmentMode = "matching"),
                    "primary", listOf(mappingKey(pair.lexicalSource.entryId, pair.lexicalSource.senseId)))
            }
        } else listOf(PracticeAssessment(null, descriptor, "primary", answerKeys(task)))
        return PracticePlanItem(position, exercise, descriptor, answerKeys(task), promptKeys(task, exercise),
            introductionKeys, emptyList(),
            answerKeys(task), assessments)
    }

    private fun descriptor(task: VocabularyP3Task, exercise: GeneratedExercise) = when (task.kind) {
        VocabularyP3TaskKind.MATCHING -> PracticeTargetDescriptor("Vocabulary matching board (${task.matching.size} pairs)",
            null, null, matchingTarget = LexicalTask("meaning", "en-da"))
        VocabularyP3TaskKind.READING -> PracticeTargetDescriptor("Explicit ${task.questionRole} fact", null, null,
            readingTarget = ReadingTask("explicit-fact"), readingRole = task.questionRole)
        VocabularyP3TaskKind.TILES -> PracticeTargetDescriptor(task.targetScopeId.orEmpty(), "G005", null,
            assessmentMode = "word-order", targetScopeId = task.targetScopeId)
        VocabularyP3TaskKind.CONTRAST -> PracticeTargetDescriptor(task.targetScopeId.orEmpty(), "G016", null,
            assessmentMode = "selection", targetScopeId = task.targetScopeId)
        VocabularyP3TaskKind.RECALL -> PracticeTargetDescriptor(task.targetScopeId.orEmpty(), "G004",
            null, assessmentMode = "recall", targetScopeId = task.targetScopeId)
        VocabularyP3TaskKind.CHOICE -> PracticeTargetDescriptor(task.targets.single().gloss, null,
            requireNotNull(exercise.lexicalTarget))
    }

    private fun answerKeys(task: VocabularyP3Task): List<String> = when (task.kind) {
        VocabularyP3TaskKind.MATCHING -> task.targets.map { mappingKey(it.entryId, it.senseId) }
        VocabularyP3TaskKind.READING -> listOf(factKey(requireNotNull(task.targetFactId)))
        VocabularyP3TaskKind.TILES -> listOf(orderKey(task.id))
        VocabularyP3TaskKind.CONTRAST -> listOf(contrastKey(requireNotNull(task.contrastId)))
        VocabularyP3TaskKind.RECALL -> listOf(formKey(task.targets.single().entryId, task.expected))
        VocabularyP3TaskKind.CHOICE -> task.targets.map(::meaningKey)
    }

    private fun promptKeys(task: VocabularyP3Task, exercise: GeneratedExercise): List<String> = when (task.kind) {
        VocabularyP3TaskKind.MATCHING -> requireNotNull(exercise.inspector.vocabularySource).matchingPairs.map {
            formKey(it.lexicalSource.entryId, it.lexicalSource.forms.present ?: it.lexicalSource.forms.bundle ?:
                requireNotNull(it.lexicalSource.forms.form))
        }
        VocabularyP3TaskKind.READING -> task.facts.flatMap { it.exposureKeys + factKey(it.id) }
        VocabularyP3TaskKind.TILES -> requireNotNull(exercise.inspector.vocabularySource).tileOccurrences.map { occurrence ->
            occurrence.lexicalSource?.let { formKey(it.entryId, occurrence.form) } ?: "vocabulary-p3:support:${occurrence.lexicalId}"
        }
        VocabularyP3TaskKind.CONTRAST -> task.targets.map(::displayKey).distinct()
        VocabularyP3TaskKind.RECALL -> listOf(meaningKey(task.targets.single()))
        VocabularyP3TaskKind.CHOICE -> task.targets.map(::displayKey)
    } + listOfNotNull(task.scene?.exposureKeys?.singleOrNull())

    internal data class Specification(
        val focusId: String, val planRevision: String, val title: String, val taskIds: List<String>,
        val introduction: List<IntroductionSection>, val introductionKeys: List<String>,
    ) {
        fun profile() = PracticeProfile(focusId, planRevision, title,
            "Learn and practise the places, journeys, reading, and sentence material in this lesson.", taskIds.size, PREREQUISITES)
    }

    companion object {
        val PREREQUISITES = listOf("G001", "G002", "G003", "G004", "G005")
        const val ORDER_SUPPORT_KEY = "vocabulary-p3:support:reviewed-order"
        val SPECIFICATIONS = listOf(
            specification(1, "Two ways to travel", listOf("p3-1.1", "p3-1.2", "p3-1.3", "p3-1.4")),
            specification(2, "Walking to the station and home", listOf("p3-2.1", "p3-2.2", "p3-2.3", "p3-2.4")),
            specification(3, "City, home and library", listOf("p3-3.1", "p3-3.2", "p3-3.3", "p3-3.4")),
            specification(4, "Ticket, bus and train", listOf("p3-4.1", "p3-4.2", "p3-4.3")),
            specification(5, "Waiting at the station", listOf("p3-5.1", "p3-5.2", "p3-5.3", "p3-5.4")),
            specification(6, "A change and the pharmacy", listOf("p3-6.1", "p3-6.2", "p3-6.3", "p3-6.4")),
        )

        private fun specification(number: Int, title: String, taskIds: List<String>): Specification {
            val focusId = "vocabulary-p3-s$number"
            val teaching = VocabularyP3Teaching.forSession(number)
            return Specification(focusId, "$focusId-1", title, taskIds, teaching.sections, teaching.exposureKeys)
        }

        private fun displayKey(lexeme: VocabularyP3Lexeme) = when {
            lexeme.forms.present != null -> formKey(lexeme.entryId, lexeme.forms.present)
            lexeme.forms.bundle != null -> bundleKey(lexeme.entryId, lexeme.forms.bundle)
            else -> formKey(lexeme.entryId, requireNotNull(lexeme.forms.form))
        }
        fun meaningKey(lexeme: VocabularyP3Lexeme) = "vocabulary-p3:meaning:${lexeme.entryId}:${lexeme.senseId}"
        fun formKey(entryId: String, form: String) = "vocabulary-p3:form:$entryId:$form"
        fun bundleKey(entryId: String, form: String) = "vocabulary-p3:bundle:$entryId:$form"
        fun mappingKey(entryId: String, senseId: String) = "vocabulary-p3:mapping:$entryId:$senseId"
        fun factKey(factId: String) = "vocabulary-p3:proposition:$factId"
        fun spatialKey(entryId: String, classification: String) = "vocabulary-p3:spatial:$entryId:$classification"
        fun contrastKey(contrastId: String) = "vocabulary-p3:contrast:$contrastId"
        fun orderKey(taskId: String) = "vocabulary-p3:order:$taskId"
        private fun targetSkill(task: VocabularyP3Task) = when (task.formatId) {
            "E048" -> "G004"; "E049" -> "G005"; "E051" -> "G016"; else -> null
        }
    }
}
