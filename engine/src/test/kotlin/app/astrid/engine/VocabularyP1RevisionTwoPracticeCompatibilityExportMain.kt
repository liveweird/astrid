package app.astrid.engine

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.Locale

object VocabularyP1RevisionTwoPracticeCompatibilityExportMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 2) {
            "Usage: VocabularyP1RevisionTwoPracticeCompatibilityExportMain <fixture.tsv> <fixture.provenance>"
        }
        VocabularyP1RevisionTwoPracticeCompatibilityFixture.write(Path.of(args[0]), Path.of(args[1]))
    }
}

internal object VocabularyP1RevisionTwoPracticeCompatibilityFixture {
    private val planner = PracticePlanner()
    private val engine = PilotEngine()
    internal val profiles = (1..5).map { "vocabulary-p1-s$it" to "vocabulary-p1-s$it-2" }

    fun rows(): List<String> {
        requireLiteralPairEighteen()
        return buildList {
            val selected = planner.profiles().filter { profile -> profiles.any { it.first == profile.focusId } }
            require(selected.map(PracticeProfile::focusId).toSet() == profiles.map { it.first }.toSet())
            add(row("CATALOG", catalogHash(selected)))
            SEEDS.forEach { seed ->
                profiles.forEach { (focus, revision) ->
                    val plan = planner.plan(focus, revision, seed)
                    add(row("PLAN", focus, seed.toString(), planHash(plan)))
                }
            }
            profiles.forEach { (focus, revision) ->
                val plan = planner.plan(focus, revision, SUMMARY_SEED)
                summaryRows(plan).forEach(::add)
                exposureRows(plan).forEach(::add)
                evaluationRows(plan).forEach(::add)
            }
            completionCases().forEach { case ->
                add(row("COMPLETION", case.first, PracticeCompletionPolicy.canAdvance(POLICY, case.second).toString()))
            }
        }
    }

    fun fixtureBytes(): ByteArray = rows().joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)

    fun write(fixturePath: Path, provenancePath: Path) {
        require(!Files.exists(fixturePath) && !Files.exists(provenancePath)) {
            "Refusing to overwrite frozen vocabulary P1 practice compatibility evidence"
        }
        val rows = rows()
        val bytes = rows.joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)
        Files.createDirectories(fixturePath.toAbsolutePath().parent)
        Files.createDirectories(provenancePath.toAbsolutePath().parent)
        Files.write(fixturePath, bytes, StandardOpenOption.CREATE_NEW)
        val provenance = buildString {
            appendLine("releaseSourceCommit=$RELEASE_SOURCE_COMMIT")
            appendLine("releaseVersion=0.20.1")
            appendLine("captureDate=$CAPTURE_DATE")
            appendLine("projectionSchema=p1-revision-two-engine-v1")
            appendLine("planPairs=${profiles.joinToString(",") { "${it.second}:pilot-18/generator-18/evaluation-18" }}")
            appendLine("planRevisions=${profiles.joinToString(",") { it.second }}")
            appendLine("reviewState=compatibility-only-not-linguistic-approval")
            appendLine("rowCount=${rows.size}")
            appendLine("fixtureSha256=${bytes.sha256()}")
            appendLine(
                    "description=Five published P1 plans at seeds 0/1/47/Int.MAX_VALUE with 4/4/5/5/5 literal tasks, full " +
                    "teaching, policies, descriptors, private assessments, ordered exposure, meaning and per-pair matching " +
                    "outcomes, first/latest/unknown/retry/support evidence, eligibility boundaries and actual feedback release.",
            )
        }
        Files.writeString(provenancePath, provenance, Charsets.UTF_8, StandardOpenOption.CREATE_NEW)
    }

    private fun catalogHash(profiles: List<PracticeProfile>) = FreezeHasher("vocabulary-p1-practice-catalog-v1").apply {
        integer("profiles.count", profiles.size)
        profiles.forEachIndexed { index, profile ->
            val p = "profiles[$index]"
            field("$p.focusId", profile.focusId); field("$p.planRevision", profile.planRevision)
            field("$p.title", profile.title); field("$p.description", profile.description)
            integer("$p.itemCount", profile.itemCount); strings("$p.prerequisiteSkillIds", profile.prerequisiteSkillIds)
        }
    }.finish()

    private fun planHash(plan: PracticePlan) = FreezeHasher("vocabulary-p1-practice-plan-v1").apply {
        field("focusId", plan.focusId); field("planRevision", plan.planRevision); integer("seed", plan.seed)
        field("title", plan.title); strings("prerequisiteSkillIds", plan.prerequisiteSkillIds)
        integer("introduction.count", plan.introduction.size)
        plan.introduction.forEachIndexed { index, section ->
            field("introduction[$index].title", section.title); field("introduction[$index].body", section.body)
            nullable("introduction[$index].bodyLanguage", section.bodyLanguage)
        }
        field("contentRevision", plan.contentRevision); field("generatorRevision", plan.generatorRevision)
        field("evaluationRevision", plan.evaluationRevision); nullableStrings("introductionExposureKeys", plan.introductionExposureKeys)
        boolean("introductionRequired", plan.introductionRequired); nullable("completionRuleId", plan.completionRuleId)
        nullable("exposurePolicyId", plan.exposurePolicyId); integer("items.count", plan.items.size)
        plan.items.forEachIndexed { index, item ->
            val p = "items[$index]"
            integer("$p.position", item.position)
            field("$p.exerciseGenerationHash", PilotEighteenCompatibilityGolden.generationHash(item.exercise))
            descriptor("$p.targetDescriptor", item.targetDescriptor)
            strings("$p.answerExposureKeys", item.answerExposureKeys); strings("$p.promptExposureKeys", item.promptExposureKeys)
            strings("$p.lessonExposureKeys", item.lessonExposureKeys); strings("$p.hintExposureKeys", item.hintExposureKeys)
            strings("$p.feedbackExposureKeys", item.feedbackExposureKeys); integer("$p.assessments.count", item.assessments.size)
            item.assessments.forEachIndexed { assessmentIndex, assessment ->
                val a = "$p.assessments[$assessmentIndex]"
                nullable("$a.slotId", assessment.slotId); descriptor("$a.targetDescriptor", assessment.targetDescriptor)
                field("$a.assessmentRole", assessment.assessmentRole); strings("$a.answerExposureKeys", assessment.answerExposureKeys)
            }
        }
    }.finish()

    private fun summaryRows(plan: PracticePlan): List<String> = listOf(
        "all-correct" to { item: PracticePlanItem -> listOf(evaluate(item, correctAnswer(item))) },
        "non-success" to { item: PracticePlanItem -> listOf(evaluate(item, nonSuccessAnswer(item).second)) },
        "retry-correct" to { item: PracticePlanItem ->
            listOf(evaluate(item, nonSuccessAnswer(item).second), evaluate(item, correctAnswer(item)))
        },
        "eligible-then-valid-nonsuccess" to { item: PracticePlanItem ->
            listOf(evaluate(item, correctAnswer(item)), evaluate(item, persistableUnknownOrPartialAnswer(item)))
        },
        "partial-or-valid-nonsuccess" to { item: PracticePlanItem ->
            listOf(evaluate(item, persistablePartialOrNonSuccessAnswer(item)))
        },
    ).map { (name, attemptsFor) ->
        val evaluationsByPosition = plan.items.associate { it.position to attemptsFor(it) }
        val observations = plan.items.mapIndexed { index, item ->
            val evaluations = evaluationsByPosition.getValue(item.position)
            val contexts = sequentialAttemptContexts(
                plan, item, evaluationsByPosition, evaluations, lesson = index == 0, hint = index == 0,
            )
            val attempts = evaluations.mapIndexed { attemptIndex, evaluation ->
                summaryAttempt(item, evaluation, contexts[attemptIndex], hintUsed = index == 0)
            }
            PracticeSummaryItemObservation(
                item,
                PracticeCompletionPolicy.canAdvance(plan.completionRuleId, completion(evaluations.last(), teachingSaved = true)),
                lessonOpened = index == 0,
                hintOpened = index == 0,
                attempts = attempts,
            )
        }
        val summary = PracticeSummaryCalculator.calculate(plan, introductionOpened = true, observations)
        row("SUMMARY", plan.focusId, name, summaryHash(summary))
    }

    private fun summaryHash(summary: PracticeSummary) = FreezeHasher("vocabulary-p1-practice-summary-v1").apply {
        integer("plannedItems", summary.plannedItems); integer("attemptedItems", summary.attemptedItems)
        integer("completedItems", summary.completedItems); integer("attemptCount", summary.attemptCount)
        integer("retryCount", summary.retryCount); boolean("introductionOpened", summary.introductionOpened)
        integer("rows.count", summary.rows.size)
        summary.rows.forEachIndexed { index, value ->
            val p = "rows[$index]"
            descriptor(p, PracticeTargetDescriptor(value.label, value.targetSkillId, value.lexicalTarget,
                value.assessmentMode, value.targetScopeId, value.readingTarget, value.readingRole, value.matchingTarget))
            integer("$p.plannedItems", value.plannedItems); integer("$p.attemptedItems", value.attemptedItems)
            integer("$p.advancedItems", value.advancedItems); integer("$p.attemptCount", value.attemptCount)
            integer("$p.retryCount", value.retryCount); outcomes("$p.first", value.first); outcomes("$p.latest", value.latest)
            integer("$p.firstWithSupport", value.firstWithSupport); integer("$p.firstWithPriorExposure", value.firstWithPriorExposure)
            integer("$p.lessonOpenedItems", value.lessonOpenedItems); integer("$p.hintOpenedItems", value.hintOpenedItems)
            nullable("$p.assessmentRole", value.assessmentRole)
        }
    }.finish()

    private fun exposureRows(plan: PracticePlan): List<String> = plan.items.flatMap { item ->
        val correct = evaluate(item, correctAnswer(item)).feedback()
        val (nonSuccessCase, nonSuccessAnswer) = nonSuccessAnswer(item)
        val wrong = evaluate(item, nonSuccessAnswer).feedback()
        val partial = evaluate(item, persistablePartialOrNonSuccessAnswer(item)).feedback()
        listOf(
            "correct" to listOf(correct), nonSuccessCase to listOf(wrong), "partial" to listOf(partial),
            "$nonSuccessCase-then-correct" to listOf(wrong, correct),
        ).map { (case, attempts) ->
            row(
                "EXPOSURE", plan.focusId, item.position.toString(), case,
                PracticeFeedbackExposureCalculator.releasedKeys(
                    item.feedbackExposureKeys, item.assessments, attempts, plan.exposurePolicyId,
                ).joinToString("\u001f").ifEmpty { "-" },
            )
        }
    }

    private fun evaluationRows(plan: PracticePlan): List<String> = plan.items.flatMap { item ->
        val nonSuccess = nonSuccessAnswer(item)
        listOf(
            "correct" to correctAnswer(item), nonSuccess,
            "partial" to partialAnswer(item), "unknown" to unknownAnswer(item), "wrong-kind" to wrongKindAnswer(item),
        ).map { (case, answer) ->
            val evaluation = evaluate(item, answer)
            row(
                "EVALUATION", plan.focusId, item.position.toString(), taskId(item), case,
                PilotEighteenCompatibilityGolden.evaluationHash(evaluation),
                PracticeCompletionPolicy.canAdvance(plan.completionRuleId, completion(evaluation, true)).toString(),
                evaluation.targetStatus.wireValue,
                evaluation.linguisticStatus.wireValue,
                evaluation.responseConstraintStatus?.wireValue ?: "-",
                evaluation.meaningSatisfaction?.wireValue ?: "-",
            )
        }
    }

    private fun correctAnswer(item: PracticePlanItem) = PilotEighteenCompatibilityGolden.canonicalAnswer(item.exercise)

    private fun nonSuccessAnswer(item: PracticePlanItem): Pair<String, Answer> {
        val source = item.exercise.source as VocabularyP1ExerciseSource
        val task = source.task
        return when (task.kind) {
            VocabularyP1TaskKind.CHOICE -> "known-wrong" to Answer.Text(task.choices.first { it != task.expected })
            VocabularyP1TaskKind.READING -> "known-wrong" to Answer.Text(task.choices.first { it != task.targets.single().gloss })
            VocabularyP1TaskKind.RECALL -> "known-wrong" to Answer.Text(
                requireNotNull(item.exercise.inspector.vocabularySource).knownAlternatives.first().text,
            )
            VocabularyP1TaskKind.MATCHING -> {
                val pairs = requireNotNull(item.exercise.inspector.vocabularySource).matchingPairs
                val rights = pairs.map { it.rightId }.toMutableList().also {
                    val first = it[0]
                    it[0] = it[1]
                    it[1] = first
                }
                "known-wrong" to Answer.Matching(pairs.mapIndexed { index, pair -> MatchingPair(pair.leftId, rights[index]) })
            }
            VocabularyP1TaskKind.TILES -> {
                val details = requireNotNull(item.exercise.inspector.vocabularySource)
                val reviewed = details.reviewedOrders.firstOrNull { it.targetStatus != TargetStatus.MET }
                val byWord = details.tileOccurrences.associateBy(VocabularyTileOccurrenceDetails::form)
                if (reviewed != null) {
                    "known-wrong" to Answer.Order(reviewed.words.map { byWord.getValue(it).blockId })
                } else {
                    "unsupported-order-unknown" to Answer.Order(details.acceptedOrders.single().reversed())
                }
            }
        }
    }

    private fun partialAnswer(item: PracticePlanItem): Answer = when (val answer = correctAnswer(item)) {
        is Answer.Text -> Answer.Text("")
        is Answer.Order -> Answer.Order(answer.blockIds.dropLast(1))
        is Answer.Matching -> Answer.Matching(answer.pairs.dropLast(1))
        is Answer.Gaps -> Answer.Gaps(answer.selections.dropLast(1))
    }

    private fun unknownAnswer(item: PracticePlanItem): Answer = when (correctAnswer(item)) {
        is Answer.Text -> Answer.Text("ukendt")
        is Answer.Order -> Answer.Order(listOf("unknown"))
        is Answer.Matching -> Answer.Matching(listOf(MatchingPair("unknown", "unknown")))
        is Answer.Gaps -> Answer.Gaps(listOf(GapSelection("unknown", "unknown")))
    }

    /** Malformed IDs are API-rejected and belong only in EVALUATION rows, never saved summary contexts. */
    private fun persistableUnknownOrPartialAnswer(item: PracticePlanItem): Answer {
        val task = (item.exercise.source as VocabularyP1ExerciseSource).task
        return when (task.kind) {
            VocabularyP1TaskKind.CHOICE, VocabularyP1TaskKind.READING -> nonSuccessAnswer(item).second
            VocabularyP1TaskKind.RECALL -> Answer.Text("ukendt")
            VocabularyP1TaskKind.MATCHING, VocabularyP1TaskKind.TILES -> partialAnswer(item)
        }
    }

    private fun persistablePartialOrNonSuccessAnswer(item: PracticePlanItem): Answer {
        val task = (item.exercise.source as VocabularyP1ExerciseSource).task
        return when (task.kind) {
            VocabularyP1TaskKind.CHOICE, VocabularyP1TaskKind.READING -> nonSuccessAnswer(item).second
            VocabularyP1TaskKind.RECALL, VocabularyP1TaskKind.MATCHING, VocabularyP1TaskKind.TILES -> partialAnswer(item)
        }
    }

    private fun wrongKindAnswer(item: PracticePlanItem): Answer = when (correctAnswer(item)) {
        is Answer.Text -> Answer.Order(emptyList())
        else -> Answer.Text("ukendt")
    }

    private fun evaluate(item: PracticePlanItem, answer: Answer) = engine.evaluate(item.exercise, answer)

    private fun exposureContext(
        plan: PracticePlan,
        item: PracticePlanItem,
        evaluationsByPosition: Map<Int, List<Evaluation>>,
        lesson: Boolean,
        hint: Boolean,
        currentFeedbackEvaluations: List<Evaluation> = emptyList(),
    ): PracticeAttemptContext {
        val observations = plan.items.map { candidate ->
            val feedbackEvaluations = when {
                candidate.position < item.position -> evaluationsByPosition.getValue(candidate.position)
                candidate.position == item.position -> currentFeedbackEvaluations
                else -> emptyList()
            }
            val frozenItem = if (feedbackEvaluations.isNotEmpty()) {
                PracticeExposureSnapshot(
                    candidate.position,
                    candidate.answerExposureKeys,
                    candidate.promptExposureKeys,
                    candidate.lessonExposureKeys,
                    candidate.hintExposureKeys,
                    frozenFeedbackExposureKeys(plan, candidate, feedbackEvaluations),
                )
            } else {
                candidate
            }
            PracticeItemExposureObservation(
                frozenItem, promptReleased = candidate.position <= item.position,
                lessonOpened = lesson && candidate.position == item.position,
                hintOpened = hint && candidate.position == item.position,
                feedbackReleased = candidate.position < item.position ||
                    (candidate.position == item.position && currentFeedbackEvaluations.isNotEmpty()),
            )
        }
        val frozenCurrentItem = observations.single { it.item.position == item.position }.item
        return PracticeAttemptContextCalculator.calculate(
            frozenCurrentItem, introductionOpened = true, observations = observations,
            introductionExposureKeys = plan.introductionExposureKeys,
        )
    }

    private fun sequentialAttemptContexts(
        plan: PracticePlan,
        item: PracticePlanItem,
        evaluationsByPosition: Map<Int, List<Evaluation>>,
        evaluations: List<Evaluation>,
        lesson: Boolean,
        hint: Boolean,
    ): List<PracticeAttemptContext> = evaluations.indices.map { attemptIndex ->
        exposureContext(
            plan, item, evaluationsByPosition, lesson, hint,
            currentFeedbackEvaluations = evaluations.take(attemptIndex),
        )
    }

    internal fun retryContexts(plan: PracticePlan, item: PracticePlanItem): List<PracticeAttemptContext> {
        val evaluations = listOf(evaluate(item, nonSuccessAnswer(item).second), evaluate(item, correctAnswer(item)))
        val evaluationsByPosition = plan.items.associate { candidate ->
            candidate.position to if (candidate.position == item.position) evaluations else listOf(
                evaluate(candidate, correctAnswer(candidate)),
            )
        }
        return sequentialAttemptContexts(plan, item, evaluationsByPosition, evaluations, lesson = false, hint = false)
    }

    private fun frozenFeedbackExposureKeys(
        plan: PracticePlan,
        item: PracticePlanItem,
        evaluations: List<Evaluation>,
    ): List<String> = PracticeFeedbackExposureCalculator.releasedKeys(
        item.feedbackExposureKeys,
        item.assessments,
        evaluations.map { it.feedback() },
        plan.exposurePolicyId,
    )

    private fun summaryAttempt(
        item: PracticePlanItem,
        evaluation: Evaluation,
        context: PracticeAttemptContext,
        hintUsed: Boolean,
    ): PracticeSummaryAttemptObservation {
        val matchingAssessments = item.assessments.filter { it.slotId != null && it.targetDescriptor.lexicalTarget != null }
        if (matchingAssessments.isNotEmpty()) {
            require(matchingAssessments.size == item.assessments.size)
            require(evaluation.matchingResults.orEmpty().size == matchingAssessments.size) {
                "Saved matching summary attempts must cover every frozen lexical assessment"
            }
            matchingAssessments.forEach { assessment ->
                require(evaluation.matchingResults.orEmpty().count { result ->
                    result.leftId == assessment.slotId &&
                        result.lexicalTarget == assessment.targetDescriptor.lexicalTarget
                } == 1) {
                    "Saved matching summary results must correspond to frozen assessment identity"
                }
            }
        }
        return PracticeSummaryAttemptObservation(
            evaluation.targetStatus, evaluation.linguisticStatus, evaluation.responseConstraintStatus,
            context, hintUsed, evaluation.gapResults.orEmpty(), evaluation.meaningSatisfaction,
            evaluation.matchingResults.orEmpty(),
        )
    }

    private fun Evaluation.feedback() = PracticeFeedbackAttemptObservation(
        targetStatus, gapResults.orEmpty(), linguisticStatus, responseConstraintStatus, issues.map { it.code },
        meaningSatisfaction, matchingResults.orEmpty(),
    )

    private fun completion(evaluation: Evaluation, teachingSaved: Boolean) = PracticeCompletionAttemptObservation(
        evaluation.targetStatus, evaluation.linguisticStatus, evaluation.responseConstraintStatus,
        evaluation.issues.map { it.code }, teachingSaved, evaluation.meaningSatisfaction, evaluation.matchingResults.orEmpty(),
    )

    private fun completionCases(): List<Pair<String, PracticeCompletionAttemptObservation?>> {
        val three = planner.plan("vocabulary-p1-s4", "vocabulary-p1-s4-2", SUMMARY_SEED).items.first()
        val four = planner.plan("vocabulary-p1-s2", "vocabulary-p1-s2-2", SUMMARY_SEED).items.first()
        val correctThree = evaluate(three, correctAnswer(three))
        val correctFour = evaluate(four, correctAnswer(four))
        val wrongFour = evaluate(four, nonSuccessAnswer(four).second)
        return listOf(
            "missing" to null,
            "correct-three-pair" to completion(correctThree, true),
            "correct-four-pair" to completion(correctFour, true),
            "complete-known-wrong-four-pair" to completion(wrongFour, true),
            "partial-matching" to completion(evaluate(four, partialAnswer(four)), true),
            "teaching-not-saved" to completion(correctFour, false),
            "unknown-latest" to PracticeCompletionAttemptObservation(
                TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN, ResponseConstraintStatus.NOT_MET,
                listOf("UNRECOGNIZED_P1_FORM"), true,
            ),
            "known-fronted-order" to PracticeCompletionAttemptObservation(
                TargetStatus.NOT_MET, LinguisticStatus.ACCEPTABLE, ResponseConstraintStatus.NOT_MET,
                listOf("REQUESTED_ORDER_CONSTRAINT"), true,
            ),
        )
    }

    private fun FreezeHasher.descriptor(name: String, descriptor: PracticeTargetDescriptor) {
        field("$name.label", descriptor.label); nullable("$name.targetSkillId", descriptor.targetSkillId)
        nullable("$name.lexicalTarget.presence", descriptor.lexicalTarget?.let { "present" })
        descriptor.lexicalTarget?.let { lexicalTarget("$name.lexicalTarget", it) }
        nullable("$name.assessmentMode", descriptor.assessmentMode); nullable("$name.targetScopeId", descriptor.targetScopeId)
        nullable("$name.readingTarget.facet", descriptor.readingTarget?.facet); nullable("$name.readingRole", descriptor.readingRole)
        nullable("$name.matchingTarget.facet", descriptor.matchingTarget?.facet)
        nullable("$name.matchingTarget.direction", descriptor.matchingTarget?.direction)
        field("$name.targetKey", descriptor.targetKey())
    }

    private fun FreezeHasher.outcomes(name: String, value: PracticeOutcomeCounts) {
        integer("$name.fullyMet", value.fullyMet); integer("$name.targetMet", value.targetMet)
        integer("$name.targetNotMet", value.targetNotMet); integer("$name.unevaluable", value.unevaluable)
        integer("$name.languageUnknown", value.languageUnknown); integer("$name.languageKnownIssue", value.languageKnownIssue)
        integer("$name.responseConstraintNotMet", value.responseConstraintNotMet)
        nullable("$name.meaningMet", value.meaningMet?.toString()); nullable("$name.meaningNotMet", value.meaningNotMet?.toString())
        nullable("$name.meaningUnknown", value.meaningUnknown?.toString())
    }

    private fun taskId(item: PracticePlanItem) = requireNotNull(item.exercise.inspector.vocabularySource).taskId
    private fun row(vararg values: String) = values.joinToString("\t")
    private fun requireLiteralPairEighteen() {
        require(PilotContent.currentSnapshot.contentRevision == "pilot-18")
        require(PilotContent.currentSnapshot.generatorRevision == "generator-18")
        require(engine.evaluationRevision("pilot-18", "generator-18") == "evaluation-18")
    }

    private fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this)
        .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    private const val RELEASE_SOURCE_COMMIT = "9eefcfed708670ce45dbdd9b11adc41f9fe17d25"
    private const val CAPTURE_DATE = "2026-11-12"
    private const val SUMMARY_SEED = 47
    private const val POLICY = "vocabulary-p1-outcome-1"
    private val SEEDS = listOf(0, 1, 47, Int.MAX_VALUE)
}

