package app.astrid.engine

internal class ModalRoundPracticePlanner(private val engine: PilotEngine) {
    fun plan(focusId: String, seed: Int): PracticePlan {
        val definition = ModalRoundContent.definitions.singleOrNull { it.focusId == focusId }
            ?: throw IllegalArgumentException("Unsupported modal round profile: $focusId")
        val interpretation = findInterpretation(definition, seed)
        val construction = findConstruction(definition, seed)
        return PracticePlan(
            focusId = definition.focusId,
            planRevision = planRevision(definition),
            seed = seed,
            title = definition.title,
            prerequisiteSkillIds = PREREQUISITES,
            introduction = ModalRoundContent.introduction(definition),
            items = listOf(
                interpretationItem(0, interpretation),
                constructionItem(1, construction),
            ),
            contentRevision = CONTENT_REVISION,
            generatorRevision = GENERATOR_REVISION,
            evaluationRevision = EVALUATION_REVISION,
            introductionExposureKeys = ModalRoundContent.introductionExposureKeys(definition).distinct(),
            introductionRequired = true,
            completionRuleId = ModalRoundPolicies.COMPLETION_RULE_ID,
            exposurePolicyId = ModalRoundPolicies.EXPOSURE_POLICY_ID,
        )
    }

    private fun findInterpretation(definition: ModalRoundDefinition, seed: Int): GeneratedExercise =
        (0 until INTERPRETATION_SCAN_SIZE).asSequence().map { index ->
            engine.generate(
                GenerationRequest("E028", "G030", seed, index, CONTENT_REVISION, GENERATOR_REVISION),
            )
        }.first { (it.source as ModalInterpretationExerciseSource).task.definition.key == definition.key }

    private fun findConstruction(definition: ModalRoundDefinition, seed: Int): GeneratedExercise =
        (0 until CONSTRUCTION_SCAN_SIZE).asSequence().map { index ->
            engine.generate(
                GenerationRequest("E025", "G005", seed, index, CONTENT_REVISION, GENERATOR_REVISION),
            )
        }.first { generated ->
            (generated.source as? PresentOrderExerciseSource)?.task
                ?.let { it is ModalConstructionTask && it.definition.key == definition.key } == true
        }

    private fun interpretationItem(position: Int, exercise: GeneratedExercise): PracticePlanItem {
        val task = (exercise.source as ModalInterpretationExerciseSource).task
        val descriptor = PracticeTargetDescriptor(
            label = "Contextual ${task.definition.modal.present} meaning — selection",
            targetSkillId = "G030",
            lexicalTarget = null,
            assessmentMode = "selection",
            targetScopeId = task.targetScopeId,
        )
        return PracticePlanItem(
            position = position,
            exercise = exercise,
            targetDescriptor = descriptor,
            answerExposureKeys = task.answerExposureKeys,
            promptExposureKeys = task.promptExposureKeys.distinct(),
            lessonExposureKeys = task.lessonExposureKeys.distinct(),
            hintExposureKeys = task.hintExposureKeys.distinct(),
            feedbackExposureKeys = task.feedbackExposureKeys.distinct(),
            assessments = listOf(PracticeAssessment(null, descriptor, "primary", task.answerExposureKeys)),
        )
    }

    private fun constructionItem(position: Int, exercise: GeneratedExercise): PracticePlanItem {
        val task = (exercise.source as PresentOrderExerciseSource).task as ModalConstructionTask
        val descriptor = PracticeTargetDescriptor(
            label = "Supplied modal statement — word order",
            targetSkillId = "G005",
            lexicalTarget = null,
            assessmentMode = "word-order",
            targetScopeId = ModalRoundContent.CONSTRUCTION_SCOPE_ID,
        )
        return PracticePlanItem(
            position = position,
            exercise = exercise,
            targetDescriptor = descriptor,
            answerExposureKeys = task.answerExposureKeys,
            promptExposureKeys = task.promptExposureKeys.distinct(),
            lessonExposureKeys = task.lessonExposureKeys.distinct(),
            hintExposureKeys = task.hintExposureKeys.distinct(),
            feedbackExposureKeys = task.feedbackExposureKeys.distinct(),
            assessments = listOf(PracticeAssessment(null, descriptor, "primary", task.answerExposureKeys)),
        )
    }

    companion object {
        const val ITEM_COUNT = 2
        val PREREQUISITES = listOf("G001", "G002", "G003", "G004", "G005")
        const val CONTENT_REVISION = "pilot-14"
        const val GENERATOR_REVISION = "generator-14"
        const val EVALUATION_REVISION = "evaluation-14"
        private const val INTERPRETATION_SCAN_SIZE = 3
        private const val CONSTRUCTION_SCAN_SIZE = 6

        fun profiles(): List<PracticeProfile> = ModalRoundContent.definitions.map { definition ->
            PracticeProfile(
                focusId = definition.focusId,
                planRevision = planRevision(definition),
                title = definition.title,
                description = description(definition),
                itemCount = ITEM_COUNT,
                prerequisiteSkillIds = PREREQUISITES,
            )
        }

        fun planRevision(definition: ModalRoundDefinition) = "${definition.focusId}-1"

        fun description(definition: ModalRoundDefinition) = when (definition.key) {
            "volition" -> "Interpret vil as intention and build one supplied vil statement."
            "obligation" -> "Interpret skal as a requirement and build one supplied skal statement."
            else -> "Interpret må as permission and build one supplied må statement."
        }
    }
}
