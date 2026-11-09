package app.astrid.engine

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest

object GuidedGapPracticeCompatibilityExportMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 2) {
            "Usage: GuidedGapPracticeCompatibilityExportMain <fixture.tsv> <fixture.provenance>"
        }
        GuidedGapPracticeCompatibilityFixture.write(Path.of(args[0]), Path.of(args[1]))
    }
}

internal object GuidedGapPracticeCompatibilityFixture {
    const val BASELINE_COMMIT = "0dfc00afdc2bd21f5f06899ff47554689f0ae0c9"
    const val RELEASE_SOURCE_COMMIT = "cdb6e066288550fb958efab1e103f538c03824a5"
    const val RELEASE_VERSION = "0.8.0"
    const val PLAN_REVISION = "verb-article-gaps-1"
    const val FOLLOW_UP_REVISION = "follow-up-3"

    private val engine = PilotEngine()
    private val planner = PracticePlanner(engine)
    private val followUpPlanner = PracticeFollowUpPlanner(engine)
    private val seeds = listOf(0, 1, 47, Int.MAX_VALUE)

    fun rows(): List<String> = buildList {
        seeds.forEach { seed ->
            add(row("PLAN", seed.toString(), planHash(plan(seed))))
        }
        val sourcePlan = plan(47)
        val source = sourcePlan.items.map {
            PracticeFollowUpSourceItem(it.exercise, it.targetDescriptor, it.assessments)
        }
        val selections = listOf(
            listOf("grammar:G004:selection"),
            listOf("grammar:G006:selection"),
            listOf("grammar:G004:selection", "grammar:G006:selection"),
        )
        listOf(0, Int.MAX_VALUE).forEach { seed ->
            selections.forEach { selected ->
                val key = selected.joinToString(",")
                add(
                    row(
                        "FOLLOW_UP",
                        seed.toString(),
                        key,
                        planHash(followUpPlanner.planGuidedGaps(FOLLOW_UP_REVISION, seed, selected, source)),
                    ),
                )
            }
        }
        val firstChild = followUpPlanner.planGuidedGaps(
            FOLLOW_UP_REVISION,
            0,
            listOf("grammar:G004:selection", "grammar:G006:selection"),
            source,
        )
        val recursiveSource = firstChild.items.map {
            PracticeFollowUpSourceItem(it.exercise, it.targetDescriptor, it.assessments)
        }
        val recursive = followUpPlanner.planGuidedGaps(
            FOLLOW_UP_REVISION,
            Int.MAX_VALUE,
            listOf("grammar:G004:selection", "grammar:G006:recall"),
            recursiveSource,
        )
        add(row("RECURSIVE_FOLLOW_UP", planHash(recursive)))

        val item = sourcePlan.items.first()
        val sourceSlots = requireNotNull(item.exercise.inspector.gapSource).slots.associateBy { it.slotId }
        val promptSlots = requireNotNull(item.exercise.prompt.gapPrompt).slots.associateBy { it.id }
        val states = listOf("missing", "correct", "wrong")
        states.forEach { verbState ->
            states.forEach { articleState ->
                val evaluation = evaluation(item, verbState, articleState, sourceSlots, promptSlots)
                val summary = summary(sourcePlan, item, listOf(observation(evaluation, emptyContext)))
                val exposure = PracticeFeedbackExposureCalculator.releasedKeys(
                    item.feedbackExposureKeys,
                    item.assessments,
                    listOf(
                        PracticeFeedbackAttemptObservation(
                            evaluation.targetStatus,
                            requireNotNull(evaluation.gapResults),
                        ),
                    ),
                )
                add(row("SLOTS", verbState, articleState, summaryHash(summary, exposure)))
            }
        }

        val first = evaluation(item, "wrong", "correct", sourceSlots, promptSlots)
        val latest = evaluation(item, "missing", "missing", sourceSlots, promptSlots)
        val releasedAfterFirst = PracticeFeedbackExposureCalculator.releasedKeys(
            item.feedbackExposureKeys,
            item.assessments,
            listOf(PracticeFeedbackAttemptObservation(first.targetStatus, requireNotNull(first.gapResults))),
        )
        val exposureItem = item.copy(feedbackExposureKeys = releasedAfterFirst)
        val latestContext = PracticeAttemptContextCalculator.calculate(
            exposureItem,
            introductionOpened = false,
            observations = listOf(
                PracticeItemExposureObservation(
                    exposureItem,
                    promptReleased = true,
                    lessonOpened = false,
                    hintOpened = false,
                    feedbackReleased = true,
                ),
            ),
        )
        val summary = summary(
            sourcePlan,
            item,
            listOf(observation(first, emptyContext), observation(latest, latestContext)),
        )
        val exposure = PracticeFeedbackExposureCalculator.releasedKeys(
            item.feedbackExposureKeys,
            item.assessments,
            listOf(
                PracticeFeedbackAttemptObservation(first.targetStatus, requireNotNull(first.gapResults)),
                PracticeFeedbackAttemptObservation(latest.targetStatus, requireNotNull(latest.gapResults)),
            ),
        )
        add(row("FIRST_LATEST", "wrong+correct", "missing+missing", summaryHash(summary, exposure)))
    }

    fun fixtureBytes(): ByteArray = rows().joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)

    fun provenance(bytes: ByteArray, rowCount: Int): String = buildString {
        appendLine("baselineCommit=$BASELINE_COMMIT")
        appendLine("releaseSourceCommit=$RELEASE_SOURCE_COMMIT")
        appendLine("releaseVersion=$RELEASE_VERSION")
        appendLine("captureDate=2026-11-09")
        appendLine("planRevision=$PLAN_REVISION")
        appendLine("followUpRevision=$FOLLOW_UP_REVISION")
        appendLine("contentRevision=pilot-7")
        appendLine("generatorRevision=generator-7")
        appendLine("evaluationRevision=evaluation-7")
        appendLine("reviewState=compatibility-only-not-linguistic-approval")
        appendLine("rowCount=$rowCount")
        appendLine("fixtureSha256=${bytes.sha256()}")
        appendLine(
            "description=Published 0.8.0 guided-gap plans and follow-ups at seed boundaries, all nine slot states, " +
                "conditional feedback exposure, and first/latest unknown summary behavior.",
        )
    }

    fun write(fixturePath: Path, provenancePath: Path) {
        val bytes = fixtureBytes()
        Files.write(fixturePath, bytes, StandardOpenOption.CREATE_NEW)
        Files.writeString(
            provenancePath,
            provenance(bytes, rows().size),
            Charsets.UTF_8,
            StandardOpenOption.CREATE_NEW,
        )
    }

    fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256").digest(this).toHex()

    private fun plan(seed: Int) = planner.plan("verb-article-gaps", PLAN_REVISION, seed)

    private fun planHash(plan: PracticePlan): String = CanonicalHasher("guided-gap-practice-plan-v1").apply {
        field("focusId", plan.focusId)
        field("planRevision", plan.planRevision)
        integer("seed", plan.seed)
        field("title", plan.title)
        strings("prerequisiteSkillIds", plan.prerequisiteSkillIds)
        integer("introduction.count", plan.introduction.size)
        plan.introduction.forEachIndexed { index, introduction ->
            field("introduction[$index].title", introduction.title)
            field("introduction[$index].body", introduction.body)
            nullable("introduction[$index].bodyLanguage", introduction.bodyLanguage)
        }
        field("contentRevision", plan.contentRevision)
        field("generatorRevision", plan.generatorRevision)
        field("evaluationRevision", plan.evaluationRevision)
        nullableStrings("introductionExposureKeys", plan.introductionExposureKeys)
        integer("items.count", plan.items.size)
        plan.items.forEachIndexed { index, item ->
            val prefix = "items[$index]"
            integer("$prefix.position", item.position)
            field("$prefix.exerciseGenerationHash", PilotSevenCompatibilityGolden.generationHash(item.exercise))
            descriptor("$prefix.targetDescriptor", item.targetDescriptor)
            strings("$prefix.answerExposureKeys", item.answerExposureKeys)
            strings("$prefix.promptExposureKeys", item.promptExposureKeys)
            strings("$prefix.lessonExposureKeys", item.lessonExposureKeys)
            strings("$prefix.hintExposureKeys", item.hintExposureKeys)
            strings("$prefix.feedbackExposureKeys", item.feedbackExposureKeys)
            integer("$prefix.assessments.count", item.assessments.size)
            item.assessments.forEachIndexed { assessmentIndex, assessment ->
                val assessmentPrefix = "$prefix.assessments[$assessmentIndex]"
                nullable("$assessmentPrefix.slotId", assessment.slotId)
                descriptor("$assessmentPrefix.targetDescriptor", assessment.targetDescriptor)
                field("$assessmentPrefix.assessmentRole", assessment.assessmentRole)
                strings("$assessmentPrefix.answerExposureKeys", assessment.answerExposureKeys)
            }
        }
    }.finish()

    private fun summaryHash(summary: PracticeSummary, exposure: List<String>): String =
        CanonicalHasher("guided-gap-practice-summary-v1").apply {
            integer("plannedItems", summary.plannedItems)
            integer("attemptedItems", summary.attemptedItems)
            integer("completedItems", summary.completedItems)
            integer("attemptCount", summary.attemptCount)
            integer("retryCount", summary.retryCount)
            boolean("introductionOpened", summary.introductionOpened)
            integer("rows.count", summary.rows.size)
            summary.rows.forEachIndexed { index, row ->
                val prefix = "rows[$index]"
                descriptor(
                    prefix,
                    PracticeTargetDescriptor(row.label, row.targetSkillId, row.lexicalTarget, row.assessmentMode),
                )
                integer("$prefix.plannedItems", row.plannedItems)
                integer("$prefix.attemptedItems", row.attemptedItems)
                integer("$prefix.advancedItems", row.advancedItems)
                integer("$prefix.attemptCount", row.attemptCount)
                integer("$prefix.retryCount", row.retryCount)
                outcomes("$prefix.first", row.first)
                outcomes("$prefix.latest", row.latest)
                integer("$prefix.firstWithSupport", row.firstWithSupport)
                integer("$prefix.firstWithPriorExposure", row.firstWithPriorExposure)
                integer("$prefix.lessonOpenedItems", row.lessonOpenedItems)
                integer("$prefix.hintOpenedItems", row.hintOpenedItems)
                nullable("$prefix.assessmentRole", row.assessmentRole)
            }
            strings("releasedFeedbackExposureKeys", exposure)
        }.finish()

    private fun evaluation(
        item: PracticePlanItem,
        verbState: String,
        articleState: String,
        source: Map<String, GapSlotSourceDetails>,
        prompt: Map<String, GapSlot>,
    ): Evaluation {
        val selections = listOfNotNull(
            selection("g0", verbState, source, prompt),
            selection("g1", articleState, source, prompt),
        )
        return engine.evaluate(item.exercise, Answer.Gaps(selections))
    }

    private fun selection(
        slotId: String,
        state: String,
        source: Map<String, GapSlotSourceDetails>,
        prompt: Map<String, GapSlot>,
    ): GapSelection? = when (state) {
        "missing" -> null
        "correct" -> GapSelection(slotId, source.getValue(slotId).expectedChoiceId)
        "wrong" -> GapSelection(
            slotId,
            prompt.getValue(slotId).choices.single { it.id != source.getValue(slotId).expectedChoiceId }.id,
        )
        else -> error("Unsupported state $state")
    }

    private fun observation(
        evaluation: Evaluation,
        context: PracticeAttemptContext,
    ) = PracticeSummaryAttemptObservation(
        evaluation.targetStatus,
        evaluation.linguisticStatus,
        evaluation.responseConstraintStatus,
        context,
        gapResults = requireNotNull(evaluation.gapResults),
    )

    private fun summary(
        plan: PracticePlan,
        item: PracticePlanItem,
        attempts: List<PracticeSummaryAttemptObservation>,
    ) = PracticeSummaryCalculator.calculate(
        plan,
        introductionOpened = true,
        plan.items.map { planned ->
            PracticeSummaryItemObservation(
                planned,
                advanced = planned.position == item.position,
                lessonOpened = planned.position == item.position,
                hintOpened = false,
                attempts = if (planned.position == item.position) attempts else emptyList(),
            )
        },
    )

    private class CanonicalHasher(schema: String) {
        val digest = MessageDigest.getInstance("SHA-256")

        init {
            token(schema)
        }

        fun field(name: String, value: String) {
            token(name)
            token("value")
            token(value)
        }

        fun nullable(name: String, value: String?) {
            token(name)
            token(if (value == null) "null" else "value")
            value?.let(::token)
        }

        fun integer(name: String, value: Int) = field(name, value.toString())

        fun boolean(name: String, value: Boolean) = field(name, value.toString())

        fun strings(name: String, values: List<String>) {
            integer("$name.count", values.size)
            values.forEachIndexed { index, value -> field("$name[$index]", value) }
        }

        fun nullableStrings(name: String, values: List<String>?) {
            nullable("$name.presence", values?.let { "present" })
            values?.let { strings(name, it) }
        }

        fun descriptor(name: String, descriptor: PracticeTargetDescriptor) {
            field("$name.label", descriptor.label)
            nullable("$name.targetSkillId", descriptor.targetSkillId)
            val lexical = descriptor.lexicalTarget
            nullable("$name.lexicalTarget.presence", lexical?.let { "present" })
            lexical?.let {
                field("$name.lexicalTarget.entryId", it.entryId)
                field("$name.lexicalTarget.senseId", it.senseId)
                field("$name.lexicalTarget.facet", it.facet)
                field("$name.lexicalTarget.direction", it.direction)
            }
            nullable("$name.assessmentMode", descriptor.assessmentMode)
            field("$name.targetKey", descriptor.targetKey())
        }

        fun outcomes(name: String, outcomes: PracticeOutcomeCounts) {
            integer("$name.fullyMet", outcomes.fullyMet)
            integer("$name.targetMet", outcomes.targetMet)
            integer("$name.targetNotMet", outcomes.targetNotMet)
            integer("$name.unevaluable", outcomes.unevaluable)
            integer("$name.languageUnknown", outcomes.languageUnknown)
            integer("$name.languageKnownIssue", outcomes.languageKnownIssue)
            integer("$name.responseConstraintNotMet", outcomes.responseConstraintNotMet)
        }

        fun finish(): String = digest.digest().toHex()

        private fun token(value: String) {
            val bytes = value.toByteArray(Charsets.UTF_8)
            digest.update(byteArrayOf(
                (bytes.size ushr 24).toByte(),
                (bytes.size ushr 16).toByte(),
                (bytes.size ushr 8).toByte(),
                bytes.size.toByte(),
            ))
            digest.update(bytes)
        }
    }

    private fun row(vararg fields: String) = fields.joinToString("\t")

    private fun ByteArray.toHex() = joinToString("") { byte -> "%02x".format(byte) }

    private val emptyContext = PracticeAttemptContext(false, false, false, emptyList())
}
