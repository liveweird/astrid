package app.astrid.engine

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.Locale

object CumulativeReviewOneCompatibilityExportMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 2) { "Usage: CumulativeReviewOneCompatibilityExportMain <fixture.tsv> <fixture.provenance>" }
        CumulativeReviewOneCompatibilityFixture.write(Path.of(args[0]), Path.of(args[1]))
    }
}

internal object CumulativeReviewOneCompatibilityFixture {
    private val engine = PilotEngine()
    private val planner = CumulativeReviewPlanner(engine)
    private val practicePlanner = PracticePlanner(engine)

    fun rows(): List<String> = buildList {
        SOURCE_PLANS.forEachIndexed { index, source ->
            val pool = sources(source, index + 7)
            val selected = requireNotNull(planner.plan(47, pool))
            add(row("SOURCE_FAMILY", source.first, source.second, hashResult(selected)))
        }
        SEEDS.forEach { seed ->
            val fullPool = SOURCE_PLANS.flatMapIndexed { index, source -> sources(source, index + 31) }
            val result = requireNotNull(planner.plan(seed, fullPool, releaseRanks(fullPool)))
            add(row("FULL", seed.toString(), hashResult(result)))
        }
        val short = sources(SOURCE_PLANS.first(), 5).take(2)
        add(row("SHORT", hashResult(requireNotNull(planner.plan(0, short)))))
        val explicitNull = short.first().copy(policy = PracticeItemPolicy(null, null))
        add(row("EXPLICIT_NULL_POLICY", hashResult(requireNotNull(planner.plan(0, listOf(explicitNull))))))
        add(row("EMPTY", (planner.plan(0, emptyList()) == null).toString()))
    }

    fun fixtureBytes() = rows().joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)

    fun write(fixturePath: Path, provenancePath: Path) {
        require(!Files.exists(fixturePath) && !Files.exists(provenancePath)) {
            "Refusing to overwrite frozen cumulative review compatibility evidence"
        }
        val rows = rows()
        val bytes = rows.joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)
        Files.createDirectories(fixturePath.toAbsolutePath().parent)
        Files.createDirectories(provenancePath.toAbsolutePath().parent)
        Files.write(fixturePath, bytes, StandardOpenOption.CREATE_NEW)
        Files.writeString(
            provenancePath,
            buildString {
                appendLine("releaseSourceCommit=$RELEASE_SOURCE_COMMIT")
                appendLine("releaseVersion=0.21.0")
                appendLine("captureDate=$CAPTURE_DATE")
                appendLine("projectionSchema=cumulative-review-one-engine-v1")
                appendLine("planRevision=cumulative-review-1")
                appendLine("sourceFamilies=${SOURCE_PLANS.size}")
                appendLine("reviewState=compatibility-only-not-linguistic-approval")
                appendLine("rowCount=${rows.size}")
                appendLine("fixtureSha256=${bytes.sha256()}")
                appendLine(
                    "description=Published cumulative-review-1 planning over all twenty taught lesson families; " +
                        "short/full/empty pools, four boundary seeds, selection witnesses, original per-item revisions, " +
                        "private generation evidence, assessments, exposure and explicit null item policies.",
                )
            },
            Charsets.UTF_8,
            StandardOpenOption.CREATE_NEW,
        )
    }

    private fun sources(source: Pair<String, String>, seed: Int): List<CumulativeReviewSourceItem> {
        val plan = practicePlanner.plan(source.first, source.second, seed)
        return plan.items.map { item ->
            CumulativeReviewSourceItem(
                sourcePracticeSessionId = "${source.first}-$seed",
                sourceLessonId = source.first,
                sourcePosition = item.position,
                sourceFocusId = source.first,
                sourcePlanRevision = source.second,
                sourceIntroduction = plan.introduction,
                sourceIntroductionExposureKeys = plan.introductionExposureKeys.orEmpty(),
                item = item,
                policy = PracticeItemPolicy(plan.completionRuleId, plan.exposurePolicyId),
            )
        }
    }

    private fun releaseRanks(sources: List<CumulativeReviewSourceItem>) = sources.mapIndexed { index, source ->
        planner.candidateSemanticId(source.item) to if (index % 2 == 0) Long.MIN_VALUE + index else Long.MAX_VALUE - index
    }.toMap()

    private fun hashResult(result: CumulativeReviewResult) = FreezeHasher("cumulative-review-one-result-v1").apply {
        val plan = result.plan
        field("focusId", plan.focusId); field("planRevision", plan.planRevision); integer("seed", plan.seed)
        field("title", plan.title); strings("prerequisiteSkillIds", plan.prerequisiteSkillIds)
        field("contentRevision", plan.contentRevision); field("generatorRevision", plan.generatorRevision)
        field("evaluationRevision", plan.evaluationRevision); boolean("introductionRequired", plan.introductionRequired)
        nullable("completionRuleId", plan.completionRuleId); nullable("exposurePolicyId", plan.exposurePolicyId)
        nullableStrings("introductionExposureKeys", plan.introductionExposureKeys)
        integer("introduction.count", plan.introduction.size)
        plan.introduction.forEachIndexed { index, section ->
            field("introduction[$index].title", section.title); field("introduction[$index].body", section.body)
            nullable("introduction[$index].bodyLanguage", section.bodyLanguage)
        }
        integer("sourceLessonCount", result.sourceLessonCount)
        integer("selections.count", result.selections.size)
        result.selections.forEachIndexed { index, selection ->
            val p = "selections[$index]"
            field("$p.candidateKey", selection.candidateKey); field("$p.semanticId", selection.semanticId)
            field("$p.sourcePracticeSessionId", selection.sourcePracticeSessionId)
            integer("$p.sourcePosition", selection.sourcePosition); field("$p.sourceLessonId", selection.sourceLessonId)
        }
        integer("items.count", plan.items.size)
        plan.items.forEachIndexed { index, item -> item("items[$index]", item) }
    }.finish()

    private fun FreezeHasher.item(name: String, item: PracticePlanItem) {
        integer("$name.position", item.position); field("$name.generationHash", generationHash(item.exercise))
        descriptor("$name.target", item.targetDescriptor)
        strings("$name.answerExposureKeys", item.answerExposureKeys); strings("$name.promptExposureKeys", item.promptExposureKeys)
        strings("$name.lessonExposureKeys", item.lessonExposureKeys); strings("$name.hintExposureKeys", item.hintExposureKeys)
        strings("$name.feedbackExposureKeys", item.feedbackExposureKeys)
        nullable("$name.policy.presence", item.policy?.let { "present" })
        item.policy?.let {
            nullable("$name.policy.completionRuleId", it.completionRuleId)
            nullable("$name.policy.exposurePolicyId", it.exposurePolicyId)
        }
        integer("$name.assessments.count", item.assessments.size)
        item.assessments.forEachIndexed { index, assessment ->
            val p = "$name.assessments[$index]"
            nullable("$p.slotId", assessment.slotId); descriptor("$p.target", assessment.targetDescriptor)
            field("$p.role", assessment.assessmentRole); strings("$p.answerExposureKeys", assessment.answerExposureKeys)
        }
    }

    private fun FreezeHasher.descriptor(name: String, descriptor: PracticeTargetDescriptor) {
        field("$name.label", descriptor.label); nullable("$name.targetSkillId", descriptor.targetSkillId)
        nullable("$name.lexicalTarget", descriptor.lexicalTarget?.let {
            listOf(it.entryId, it.senseId, it.facet, it.direction).joinToString(":")
        })
        nullable("$name.assessmentMode", descriptor.assessmentMode); nullable("$name.targetScopeId", descriptor.targetScopeId)
        nullable("$name.readingTarget", descriptor.readingTarget?.facet); nullable("$name.readingRole", descriptor.readingRole)
        nullable("$name.matchingTarget", descriptor.matchingTarget?.let { "${it.facet}:${it.direction}" })
        field("$name.targetKey", descriptor.targetKey())
    }

    private fun generationHash(exercise: GeneratedExercise): String = when (exercise.contentRevision) {
        "pilot-5" -> PilotFiveCompatibilityGolden.generationHash(exercise)
        "pilot-8" -> PilotEightCompatibilityGolden.generationHash(exercise)
        "pilot-9" -> PilotNineCompatibilityGolden.generationHash(exercise)
        "pilot-10" -> PilotTenCompatibilityGolden.generationHash(exercise)
        "pilot-11" -> PilotElevenCompatibilityGolden.generationHash(exercise)
        "pilot-12" -> PilotTwelveCompatibilityGolden.generationHash(exercise)
        "pilot-13" -> PilotThirteenCompatibilityGolden.generationHash(exercise)
        "pilot-14" -> PilotFourteenCompatibilityGolden.generationHash(exercise)
        "pilot-15" -> PilotFifteenCompatibilityGolden.generationHash(exercise)
        "pilot-16" -> PilotSixteenCompatibilityGolden.generationHash(exercise)
        "pilot-18" -> PilotEighteenCompatibilityGolden.generationHash(exercise)
        else -> error("Unexpected cumulative review source pair ${exercise.contentRevision}")
    }

    private fun row(vararg fields: String) = fields.joinToString("\t")
    private fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this)
        .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    const val RELEASE_SOURCE_COMMIT = "eab86c6ddd44175e2d9b6dde7e2283f30943800e"
    private const val CAPTURE_DATE = "2026-11-12"
    private val SEEDS = listOf(0, 1, 47, Int.MAX_VALUE)
    val SOURCE_PLANS = listOf(
        "module-one-singular" to "module-one-singular-1", "module-one-groups" to "module-one-groups-1",
        "module-one-er" to "module-one-er-1", "module-one-things" to "module-one-things-1",
        "module-one-present" to "module-one-present-1", "module-one-order" to "module-one-order-1",
        "module-one-possession" to "module-one-possession-1", "module-one-reading-words" to "module-one-reading-words-1",
        "module-one-personal" to "module-one-personal-1", "module-one-ability" to "module-one-ability-1",
        "module-one-volition" to "module-one-volition-1", "module-one-obligation" to "module-one-obligation-1",
        "module-one-permission" to "module-one-permission-1", "module-one-circumstances" to "module-one-circumstances-1",
        "module-one-questions" to "module-one-questions-1", "vocabulary-p1-s1" to "vocabulary-p1-s1-2",
        "vocabulary-p1-s2" to "vocabulary-p1-s2-2", "vocabulary-p1-s3" to "vocabulary-p1-s3-2",
        "vocabulary-p1-s4" to "vocabulary-p1-s4-2", "vocabulary-p1-s5" to "vocabulary-p1-s5-2",
    )
}
