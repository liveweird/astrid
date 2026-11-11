package app.astrid.engine

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.Locale

object CircumstancePracticeCompatibilityExportMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 2) { "Usage: CircumstancePracticeCompatibilityExportMain <fixture.tsv> <fixture.provenance>" }
        CircumstancePracticeCompatibilityFixture.write(Path.of(args[0]), Path.of(args[1]))
    }
}

internal object CircumstancePracticeCompatibilityFixture {
    private val planner = PracticePlanner()
    private val engine = PilotEngine()
    private val profiles = listOf(
        "module-one-circumstances" to "module-one-circumstances-1",
    )

    fun rows(): List<String> {
        requireLiteralPairs()
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
                exposureRows(plan).forEach(::add)
                evaluationRows(plan).forEach(::add)
            }
            completionCases().forEach { (policy, name, observation) ->
                add(
                    row(
                        "COMPLETION",
                        policy,
                        name,
                        PracticeCompletionPolicy.canAdvance(policy, observation).toString(),
                    ),
                )
            }
        }
    }

    fun fixtureBytes(): ByteArray = rows().joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)

    fun write(fixturePath: Path, provenancePath: Path) {
        require(!Files.exists(fixturePath) && !Files.exists(provenancePath)) {
            "Refusing to overwrite frozen Module One S12 circumstance practice compatibility evidence"
        }
        val bytes = fixtureBytes()
        Files.createDirectories(fixturePath.toAbsolutePath().parent)
        Files.createDirectories(provenancePath.toAbsolutePath().parent)
        Files.write(fixturePath, bytes, StandardOpenOption.CREATE_NEW)
        val provenance = buildString {
            appendLine("releaseSourceCommit=$RELEASE_SOURCE_COMMIT")
            appendLine("releaseVersion=0.17.0")
            appendLine("captureDate=$CAPTURE_DATE")
            appendLine("planPairs=${profiles.joinToString(",") { "${it.second}:pilot-15/generator-15/evaluation-15" }}")
            appendLine("planRevisions=${profiles.joinToString(",") { it.second }}")
            appendLine("reviewState=compatibility-only-not-linguistic-approval")
            appendLine("rowCount=${rows().size}")
            appendLine("fixtureSha256=${bytes.sha256()}")
            appendLine(
                "description=The published four-item S12 final-circumstance plan at four seeds, with the full task bank, " +
                    "required introduction, ordered exposure keys, prior-exposure/support/first-latest summaries, order-v1 " +
                    "policy boundaries, complete permutations, partial, malformed, wrong-kind and replayable evaluations.",
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
            field("$prefix.exerciseGenerationHash", PilotFifteenCompatibilityGolden.generationHash(item.exercise))
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
            val evaluations = if (index == 0) {
                listOf(engine.evaluate(item.exercise, canonicalAnswer(item)), unknown(item))
            } else {
                listOf(nonSuccess(item))
            }
            val attempts = evaluations.map { summaryAttempt(it, item, feedbackReleased = true) }
            val latest = evaluations.last()
            val completion = latest.completionObservation()
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
        val feedback = nonSuccess(firstItem)
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
        return engine.evaluate(item.exercise, constructionAnswer(item, "target-last-core-wrong"))
    }

    private fun unknown(item: PracticePlanItem): Evaluation = engine.evaluate(item.exercise, Answer.Text("ukendt"))

    private fun canonicalAnswer(item: PracticePlanItem): Answer = constructionAnswer(item, "canonical")

    private fun constructionAnswer(item: PracticePlanItem, shape: String): Answer {
        val key = item.exercise.source.answerKey as AnswerKey.TileOrder
        val tokens = requireNotNull(item.exercise.inspector.tileSource).tokens
        val canonical = key.canonical.map { identity ->
            tokens.single { it.lexicalId == identity.lexicalId && it.form == identity.form }.blockId
        }
        return when (shape) {
            "canonical" -> Answer.Order(canonical)
            "partial" -> Answer.Order(canonical.dropLast(1))
            "target-last-core-wrong" -> Answer.Order(listOf(canonical[1], canonical[0]) + canonical.drop(2))
            "target-not-last" -> Answer.Order(canonical.dropLast(2) + canonical.takeLast(2).reversed())
            "reversed" -> Answer.Order(canonical.reversed())
            "duplicate" -> Answer.Order(canonical + canonical.first())
            else -> error("Unsupported construction answer shape: $shape")
        }
    }

    private fun exposureRows(plan: PracticePlan): List<String> {
        return plan.items.flatMap { item ->
            val wrong = nonSuccess(item).feedbackObservation()
            val right = engine.evaluate(item.exercise, canonicalAnswer(item)).feedbackObservation()
            val cases = listOf("wrong" to listOf(wrong), "right" to listOf(right), "wrong-then-right" to listOf(wrong, right))
            cases.map { (name, attempts) ->
                row(
                    "EXPOSURE", plan.focusId, item.position.toString(), name,
                    PracticeFeedbackExposureCalculator.releasedKeys(
                        item.feedbackExposureKeys, item.assessments, attempts, plan.exposurePolicyId,
                    ).joinToString("\u001f").ifEmpty { "-" },
                )
            }
        }
    }

    private fun evaluationRows(plan: PracticePlan): List<String> = plan.items.flatMap { item ->
        val cases = listOf(
            "canonical" to canonicalAnswer(item),
            "target-last-core-wrong" to constructionAnswer(item, "target-last-core-wrong"),
            "target-not-last" to constructionAnswer(item, "target-not-last"),
            "partial-missing" to constructionAnswer(item, "partial"),
            "reversed" to constructionAnswer(item, "reversed"),
            "blank" to Answer.Order(emptyList()),
            "duplicate" to constructionAnswer(item, "duplicate"),
            "unknown" to Answer.Order(listOf("unknown")),
            "wrong-kind" to Answer.Text("ukendt"),
        )
        cases.map { (caseId, answer) ->
            row(
                "EVALUATION",
                item.position.toString(),
                item.exercise.formatId,
                caseId,
                PilotFifteenCompatibilityGolden.evaluationHash(engine.evaluate(item.exercise, answer)),
            )
        }
    }

    private fun Evaluation.feedbackObservation() = PracticeFeedbackAttemptObservation(
        targetStatus, gapResults.orEmpty(), linguisticStatus, responseConstraintStatus, issues.map { it.code },
    )

    private fun Evaluation.completionObservation() = PracticeCompletionAttemptObservation(
        targetStatus, linguisticStatus, responseConstraintStatus, issues.map { it.code },
    )

    private fun completionCases(): List<Triple<String, String, PracticeCompletionAttemptObservation?>> = listOf(
        Triple(PresentOrderPracticePlanner.ORDER_COMPLETION_RULE_ID, "missing-attempt", null),
        Triple(PresentOrderPracticePlanner.ORDER_COMPLETION_RULE_ID, "met", completion(TargetStatus.MET, LinguisticStatus.ACCEPTABLE)),
        Triple(PresentOrderPracticePlanner.ORDER_COMPLETION_RULE_ID, "target-last-core-wrong", completion(
            TargetStatus.MET, LinguisticStatus.UNKNOWN, ResponseConstraintStatus.NOT_MET,
            listOf("REQUESTED_ORDER_CONSTRAINT"),
        )),
        Triple(PresentOrderPracticePlanner.ORDER_COMPLETION_RULE_ID, "missing-correction", completion(
            TargetStatus.UNEVALUABLE,
            LinguisticStatus.UNKNOWN,
            ResponseConstraintStatus.NOT_MET,
            listOf("REQUIRED_WORDS_MISSING"),
        )),
        Triple(PresentOrderPracticePlanner.ORDER_COMPLETION_RULE_ID, "order-correction", completion(
            TargetStatus.NOT_MET,
            LinguisticStatus.UNKNOWN,
            ResponseConstraintStatus.NOT_MET,
            listOf("REQUESTED_ORDER_CONSTRAINT"),
        )),
        Triple(PresentOrderPracticePlanner.ORDER_COMPLETION_RULE_ID, "malformed", completion(
            TargetStatus.UNEVALUABLE, LinguisticStatus.UNKNOWN, ResponseConstraintStatus.NOT_MET,
            listOf("ANSWER_TYPE_MISMATCH"),
        )),
    )

    private fun completion(
        target: TargetStatus,
        linguistic: LinguisticStatus,
        constraint: ResponseConstraintStatus = ResponseConstraintStatus.MET,
        issues: List<String> = emptyList(),
    ) = PracticeCompletionAttemptObservation(target, linguistic, constraint, issues)

    private fun requireLiteralPairs() {
        require(PilotContent.pilotFifteenSnapshot.contentRevision == "pilot-15")
        require(PilotContent.pilotFifteenSnapshot.generatorRevision == "generator-15")
        require(engine.evaluationRevision("pilot-15", "generator-15") == "evaluation-15")
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

    private const val RELEASE_SOURCE_COMMIT = "0f1b322903fc243871b3b7293a26b52af80dd0dc"
    private const val CAPTURE_DATE = "2026-11-11"
    private const val SUMMARY_SEED = 47
    private val SEEDS = listOf(0, 1, 47, Int.MAX_VALUE)
}
