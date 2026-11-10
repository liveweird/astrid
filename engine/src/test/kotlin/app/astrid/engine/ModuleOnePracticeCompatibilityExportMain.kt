package app.astrid.engine

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.Locale

object ModuleOnePracticeCompatibilityExportMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 2) { "Usage: ModuleOnePracticeCompatibilityExportMain <fixture.tsv> <fixture.provenance>" }
        ModuleOnePracticeCompatibilityFixture.write(Path.of(args[0]), Path.of(args[1]))
    }
}

internal object ModuleOnePracticeCompatibilityFixture {
    private val planner = PracticePlanner()
    private val engine = PilotEngine()
    private val profiles = listOf(
        "module-one-singular" to "module-one-singular-1",
        "module-one-groups" to "module-one-groups-1",
        "module-one-er" to "module-one-er-1",
    )

    fun rows(): List<String> {
        requireLiteralPairEight()
        return buildList {
            val catalog = planner.profiles()
            val moduleOneCatalog = catalog.filter { profile -> profiles.any { it.first == profile.focusId } }
            require(moduleOneCatalog.size == profiles.size)
            add(row("CATALOG", catalogHash(moduleOneCatalog)))
            SEEDS.forEach { seed ->
                profiles.forEach { (focusId, revision) ->
                    val plan = planner.plan(focusId, revision, seed)
                    add(row("PLAN", focusId, seed.toString(), planHash(plan)))
                }
            }
            profiles.forEach { (focusId, revision) ->
                val plan = planner.plan(focusId, revision, SUMMARY_SEED)
                add(row("SUMMARY", focusId, summaryScenarioHash(plan)))
            }
            completionCases().forEach { (name, observation) ->
                add(
                    row(
                        "COMPLETION",
                        name,
                        PracticeCompletionPolicy.canAdvance("module-one-known-outcome-1", observation).toString(),
                    ),
                )
            }
        }
    }

    fun fixtureBytes(): ByteArray = rows().joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)

    fun write(fixturePath: Path, provenancePath: Path) {
        require(!Files.exists(fixturePath) && !Files.exists(provenancePath)) {
            "Refusing to overwrite frozen Module One practice compatibility evidence"
        }
        val bytes = fixtureBytes()
        Files.createDirectories(fixturePath.toAbsolutePath().parent)
        Files.createDirectories(provenancePath.toAbsolutePath().parent)
        Files.write(fixturePath, bytes, StandardOpenOption.CREATE_NEW)
        val provenance = buildString {
            appendLine("baselineCommit=$BASELINE_COMMIT")
            appendLine("releaseVersion=0.9.0")
            appendLine("captureDate=2026-11-10")
            appendLine("contentRevision=pilot-8")
            appendLine("generatorRevision=generator-8")
            appendLine("evaluationRevision=evaluation-8")
            appendLine("planRevisions=${profiles.joinToString(",") { it.second }}")
            appendLine("reviewState=compatibility-only-not-linguistic-approval")
            appendLine("rowCount=${rows().size}")
            appendLine("fixtureSha256=${bytes.sha256()}")
            appendLine(
                "description=All three published Module One plans at seeds 0, 1, 47 and Int.MAX_VALUE, full teaching " +
                    "and policy metadata, deterministic six-item scoped assessments, feedback exposure, first/latest " +
                    "non-success summaries and latest-unknown completion eligibility.",
            )
        }
        Files.writeString(provenancePath, provenance, Charsets.UTF_8, StandardOpenOption.CREATE_NEW)
    }

    private fun catalogHash(profiles: List<PracticeProfile>): String = CanonicalHasher("module-one-practice-catalog-v1").apply {
        integer("profiles.count", profiles.size)
        profiles.forEachIndexed { index, profile ->
            val prefix = "profiles[$index]"
            field("$prefix.focusId", profile.focusId)
            field("$prefix.planRevision", profile.planRevision)
            field("$prefix.title", profile.title)
            field("$prefix.description", profile.description)
            integer("$prefix.itemCount", profile.itemCount)
            strings("$prefix.prerequisiteSkillIds", profile.prerequisiteSkillIds)
        }
    }.finish()

    private fun planHash(plan: PracticePlan): String = CanonicalHasher("module-one-practice-plan-v2").apply {
        field("focusId", plan.focusId)
        field("planRevision", plan.planRevision)
        integer("seed", plan.seed)
        field("title", plan.title)
        strings("prerequisiteSkillIds", plan.prerequisiteSkillIds)
        integer("introduction.count", plan.introduction.size)
        plan.introduction.forEachIndexed { index, section ->
            field("introduction[$index].title", section.title)
            field("introduction[$index].body", section.body)
            nullable("introduction[$index].bodyLanguage", section.bodyLanguage)
        }
        field("contentRevision", plan.contentRevision)
        field("generatorRevision", plan.generatorRevision)
        field("evaluationRevision", plan.evaluationRevision)
        nullableStrings("introductionExposureKeys", plan.introductionExposureKeys)
        boolean("introductionRequired", plan.introductionRequired)
        nullable("completionRuleId", plan.completionRuleId)
        nullable("exposurePolicyId", plan.exposurePolicyId)
        integer("items.count", plan.items.size)
        plan.items.forEachIndexed { index, item ->
            val prefix = "items[$index]"
            integer("$prefix.position", item.position)
            field("$prefix.exerciseGenerationHash", PilotEightCompatibilityGolden.generationHash(item.exercise))
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

    private fun summaryScenarioHash(plan: PracticePlan): String {
        val observations = plan.items.mapIndexed { index, item ->
            val correct = engine.evaluate(item.exercise, Answer.Text(expected(item)))
            val attempts = if (index == 0) {
                listOf(summaryAttempt(correct, item, feedbackReleased = true), summaryAttempt(unknown(item), item, true))
            } else {
                listOf(summaryAttempt(nonSuccess(item), item, feedbackReleased = true))
            }
            val latest = attempts.last()
            val completion = PracticeCompletionAttemptObservation(
                latest.targetStatus,
                latest.linguisticStatus,
                latest.responseConstraintStatus,
                if (latest.linguisticStatus == LinguisticStatus.UNKNOWN) listOf("UNRECOGNIZED_PRESENT_FORM") else emptyList(),
            )
            PracticeSummaryItemObservation(
                item,
                advanced = PracticeCompletionPolicy.canAdvance(plan.completionRuleId, completion),
                lessonOpened = index == 0,
                hintOpened = index == 0,
                attempts = attempts,
            )
        }
        val summary = PracticeSummaryCalculator.calculate(plan, introductionOpened = true, observations)
        val firstItem = plan.items.first()
        val feedback = engine.evaluate(firstItem.exercise, Answer.Text(expected(firstItem).uppercase(Locale.ROOT)))
        val released = PracticeFeedbackExposureCalculator.releasedKeys(
            firstItem.feedbackExposureKeys,
            firstItem.assessments,
            listOf(
                PracticeFeedbackAttemptObservation(
                    feedback.targetStatus,
                    linguisticStatus = feedback.linguisticStatus,
                    responseConstraintStatus = feedback.responseConstraintStatus,
                    issueCodes = feedback.issues.map { it.code },
                ),
            ),
            plan.exposurePolicyId,
        )
        return CanonicalHasher("module-one-practice-summary-v2").apply {
            integer("plannedItems", summary.plannedItems)
            integer("attemptedItems", summary.attemptedItems)
            integer("completedItems", summary.completedItems)
            integer("attemptCount", summary.attemptCount)
            integer("retryCount", summary.retryCount)
            boolean("introductionOpened", summary.introductionOpened)
            integer("rows.count", summary.rows.size)
            summary.rows.forEachIndexed { index, summaryRow ->
                val prefix = "rows[$index]"
                descriptor(
                    prefix,
                    PracticeTargetDescriptor(
                        summaryRow.label,
                        summaryRow.targetSkillId,
                        summaryRow.lexicalTarget,
                        summaryRow.assessmentMode,
                        summaryRow.targetScopeId,
                    ),
                )
                integer("$prefix.plannedItems", summaryRow.plannedItems)
                integer("$prefix.attemptedItems", summaryRow.attemptedItems)
                integer("$prefix.advancedItems", summaryRow.advancedItems)
                integer("$prefix.attemptCount", summaryRow.attemptCount)
                integer("$prefix.retryCount", summaryRow.retryCount)
                outcomes("$prefix.first", summaryRow.first)
                outcomes("$prefix.latest", summaryRow.latest)
                integer("$prefix.firstWithSupport", summaryRow.firstWithSupport)
                integer("$prefix.firstWithPriorExposure", summaryRow.firstWithPriorExposure)
                integer("$prefix.lessonOpenedItems", summaryRow.lessonOpenedItems)
                integer("$prefix.hintOpenedItems", summaryRow.hintOpenedItems)
                nullable("$prefix.assessmentRole", summaryRow.assessmentRole)
            }
            strings("releasedFeedbackExposureKeys", released)
            boolean("latestUnknownSupersedesEligible", !observations.first().advanced)
        }.finish()
    }

    private fun summaryAttempt(
        evaluation: Evaluation,
        item: PracticePlanItem,
        feedbackReleased: Boolean,
    ): PracticeSummaryAttemptObservation {
        val context = PracticeAttemptContextCalculator.calculate(
            item,
            introductionOpened = true,
            observations = listOf(PracticeItemExposureObservation(item, true, true, true, feedbackReleased)),
            introductionExposureKeys = item.answerExposureKeys,
        )
        return PracticeSummaryAttemptObservation(
            evaluation.targetStatus,
            evaluation.linguisticStatus,
            evaluation.responseConstraintStatus,
            context,
            hintUsed = true,
        )
    }

    private fun nonSuccess(item: PracticePlanItem): Evaluation {
        val source = requireNotNull(item.exercise.inspector.moduleOneSource)
        val answer = source.knownAlternativeForms.firstOrNull() ?: expected(item).uppercase(Locale.ROOT)
        return engine.evaluate(item.exercise, Answer.Text(answer))
    }

    private fun unknown(item: PracticePlanItem): Evaluation = engine.evaluate(item.exercise, Answer.Text("ukendt"))
    private fun expected(item: PracticePlanItem): String = requireNotNull(item.exercise.inspector.moduleOneSource).expectedForm

    private fun completionCases(): List<Pair<String, PracticeCompletionAttemptObservation?>> = listOf(
        "missing" to null,
        "met" to completion(TargetStatus.MET, LinguisticStatus.ACCEPTABLE),
        "known-wrong" to completion(TargetStatus.NOT_MET, LinguisticStatus.KNOWN_ISSUE),
        "unknown" to completion(TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN),
        "case" to completion(
            TargetStatus.UNEVALUABLE,
            LinguisticStatus.KNOWN_ISSUE,
            ResponseConstraintStatus.NOT_MET,
            listOf("STANDALONE_PRONOUN_CASE_REQUIRED"),
        ),
        "multiword" to completion(
            TargetStatus.UNEVALUABLE,
            LinguisticStatus.KNOWN_ISSUE,
            ResponseConstraintStatus.NOT_MET,
            listOf("ONE_WORD_RESPONSE_REQUIRED"),
        ),
        "blank" to completion(
            TargetStatus.UNEVALUABLE,
            LinguisticStatus.UNKNOWN,
            ResponseConstraintStatus.NOT_MET,
            listOf("EMPTY_RESPONSE"),
        ),
    )

    private fun completion(
        target: TargetStatus,
        linguistic: LinguisticStatus,
        constraint: ResponseConstraintStatus = ResponseConstraintStatus.MET,
        issues: List<String> = emptyList(),
    ) = PracticeCompletionAttemptObservation(target, linguistic, constraint, issues)

    private fun requireLiteralPairEight() {
        require(PilotContent.pilotEightSnapshot.contentRevision == "pilot-8")
        require(PilotContent.pilotEightSnapshot.generatorRevision == "generator-8")
        require(engine.evaluationRevision("pilot-8", "generator-8") == "evaluation-8")
    }

    private fun row(vararg columns: String): String = columns.joinToString("\t")

    private class CanonicalHasher(schema: String) {
        private val digest = MessageDigest.getInstance("SHA-256")
        init { token(schema) }
        fun field(name: String, value: String) { token(name); token("value"); token(value) }
        fun nullable(name: String, value: String?) { token(name); token(if (value == null) "null" else "value"); value?.let(::token) }
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
            nullable("$name.lexicalTarget.presence", descriptor.lexicalTarget?.let { "present" })
            descriptor.lexicalTarget?.let {
                field("$name.lexicalTarget.entryId", it.entryId)
                field("$name.lexicalTarget.senseId", it.senseId)
                field("$name.lexicalTarget.facet", it.facet)
                field("$name.lexicalTarget.direction", it.direction)
            }
            nullable("$name.assessmentMode", descriptor.assessmentMode)
            nullable("$name.targetScopeId", descriptor.targetScopeId)
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
            digest.update(
                byteArrayOf(
                    (bytes.size ushr 24).toByte(),
                    (bytes.size ushr 16).toByte(),
                    (bytes.size ushr 8).toByte(),
                    bytes.size.toByte(),
                ),
            )
            digest.update(bytes)
        }
    }

    private fun ByteArray.sha256(): String = MessageDigest.getInstance("SHA-256").digest(this).toHex()
    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    private const val BASELINE_COMMIT = "8913dea94a6c3bc0332f464e56952b9a126118a3"
    private const val SUMMARY_SEED = 47
    private val SEEDS = listOf(0, 1, 47, Int.MAX_VALUE)
}
