package app.astrid.engine

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.Locale

object CumulativeReviewTwoCompatibilityExportMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 2) { "Usage: CumulativeReviewTwoCompatibilityExportMain <fixture.tsv> <fixture.provenance>" }
        CumulativeReviewTwoCompatibilityFixture.write(Path.of(args[0]), Path.of(args[1]))
    }
}

internal object CumulativeReviewTwoCompatibilityFixture {
    private val engine = PilotEngine()
    private val planner = CumulativeReviewPlanner(engine, "cumulative-review-2")
    private val practicePlanner = PracticePlanner(engine)

    fun rows(): List<String> = buildList {
        SOURCE_PLANS.forEachIndexed { index, source ->
            val selected = requireNotNull(planner.plan(47, sources(source, index + 7)))
            add(row("SOURCE_FAMILY", source.first, source.second, hashResult(selected)))
        }
        SEEDS.forEach { seed ->
            val pool = SOURCE_PLANS.flatMapIndexed { index, source -> sources(source, index + 31) }
            val ranks = pool.mapIndexed { index, item ->
                planner.candidateSemanticId(item.item) to if (index % 2 == 0) Long.MIN_VALUE + index else Long.MAX_VALUE - index
            }.toMap()
            runCatching { requireNotNull(planner.plan(seed, pool, ranks)) }.fold(
                onSuccess = { add(row("FULL", seed.toString(), hashResult(it))) },
                onFailure = { add(row("FULL_FAILURE", seed.toString(), it::class.qualifiedName.orEmpty(), it.message.orEmpty())) },
            )
        }
        val short = sources(SOURCE_PLANS.last(), 5).take(2)
        add(row("SHORT", hashResult(requireNotNull(planner.plan(0, short)))))
        add(row("EXPLICIT_NULL_POLICY", hashResult(requireNotNull(planner.plan(
            0,
            listOf(short.first().copy(policy = PracticeItemPolicy(null, null))),
        )))))
        add(row("EMPTY", (planner.plan(0, emptyList()) == null).toString()))
    }

    fun fixtureBytes() = rows().joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)

    fun write(fixturePath: Path, provenancePath: Path) {
        require(!Files.exists(fixturePath) && !Files.exists(provenancePath)) {
            "Refusing to overwrite frozen cumulative-review-2 compatibility evidence"
        }
        val rows = rows(); val bytes = rows.joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)
        Files.createDirectories(fixturePath.toAbsolutePath().parent)
        Files.createDirectories(provenancePath.toAbsolutePath().parent)
        Files.write(fixturePath, bytes, StandardOpenOption.CREATE_NEW)
        Files.writeString(provenancePath, buildString {
            appendLine("releaseSourceCommit=$RELEASE_SOURCE_COMMIT")
            appendLine("releaseVersion=0.22.0")
            appendLine("captureDate=$CAPTURE_DATE")
            appendLine("projectionSchema=cumulative-review-two-engine-v1")
            appendLine("planRevision=cumulative-review-2")
            appendLine("sourceFamilies=${SOURCE_PLANS.size}")
            appendLine("reviewState=compatibility-only-not-linguistic-approval")
            appendLine("rowCount=${rows.size}")
            appendLine("fixtureSha256=${bytes.sha256()}")
            appendLine(
                "description=Published cumulative-review-2 planning over all twenty-six taught lesson families; " +
                    "short/full/empty pools, boundary seeds, exact selections, private per-item generation and policies.",
            )
        }, Charsets.UTF_8, StandardOpenOption.CREATE_NEW)
    }

    private fun sources(source: Pair<String, String>, seed: Int): List<CumulativeReviewSourceItem> {
        val plan = practicePlanner.plan(source.first, source.second, seed)
        return plan.items.map { item ->
            CumulativeReviewSourceItem(
                "${source.first}-$seed", source.first, item.position, source.first, source.second,
                plan.introduction, plan.introductionExposureKeys.orEmpty(), item,
                PracticeItemPolicy(plan.completionRuleId, plan.exposurePolicyId),
            )
        }
    }

    private fun hashResult(result: CumulativeReviewResult) = FreezeHasher("cumulative-review-two-result-v1").apply {
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
        integer("sourceLessonCount", result.sourceLessonCount); integer("selections.count", result.selections.size)
        result.selections.forEachIndexed { index, selection ->
            val p = "selections[$index]"
            field("$p.candidateKey", selection.candidateKey); field("$p.semanticId", selection.semanticId)
            field("$p.sourcePracticeSessionId", selection.sourcePracticeSessionId)
            integer("$p.sourcePosition", selection.sourcePosition); field("$p.sourceLessonId", selection.sourceLessonId)
        }
        integer("items.count", plan.items.size)
        plan.items.forEachIndexed { index, item ->
            val p = "items[$index]"
            integer("$p.position", item.position); field("$p.generationHash", generationHash(item.exercise))
            descriptor("$p.target", item.targetDescriptor)
            strings("$p.answerExposureKeys", item.answerExposureKeys); strings("$p.promptExposureKeys", item.promptExposureKeys)
            strings("$p.lessonExposureKeys", item.lessonExposureKeys); strings("$p.hintExposureKeys", item.hintExposureKeys)
            strings("$p.feedbackExposureKeys", item.feedbackExposureKeys)
            nullable("$p.policy.presence", item.policy?.let { "present" })
            item.policy?.let {
                nullable("$p.policy.completionRuleId", it.completionRuleId)
                nullable("$p.policy.exposurePolicyId", it.exposurePolicyId)
            }
            integer("$p.assessments.count", item.assessments.size)
            item.assessments.forEachIndexed { assessmentIndex, assessment ->
                val a = "$p.assessments[$assessmentIndex]"
                nullable("$a.slotId", assessment.slotId); descriptor("$a.target", assessment.targetDescriptor)
                field("$a.role", assessment.assessmentRole); strings("$a.answerExposureKeys", assessment.answerExposureKeys)
            }
        }
    }.finish()

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

    private fun generationHash(exercise: GeneratedExercise) = when (exercise.contentRevision) {
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
        "pilot-19" -> PilotNineteenCompatibilityGolden.generationHash(exercise)
        else -> error("Unexpected cumulative source ${exercise.contentRevision}")
    }

    private fun row(vararg fields: String) = fields.joinToString("\t")
    private fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this)
        .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    const val RELEASE_SOURCE_COMMIT = "844b88af33cc729e095d443adb72fcd72cd17534"
    private const val CAPTURE_DATE = "2026-11-13"
    private val SEEDS = listOf(0, 1, 47, Int.MAX_VALUE)
    val SOURCE_PLANS = CumulativeReviewOneCompatibilityFixture.SOURCE_PLANS + (1..6).map {
        "vocabulary-p2-s$it" to "vocabulary-p2-s$it-1"
    }
}
