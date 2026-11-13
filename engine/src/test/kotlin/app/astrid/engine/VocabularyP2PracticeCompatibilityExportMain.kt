package app.astrid.engine

import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardOpenOption
import java.security.MessageDigest
import java.util.Locale

object VocabularyP2PracticeCompatibilityExportMain {
    @JvmStatic
    fun main(args: Array<String>) {
        require(args.size == 2) { "Usage: VocabularyP2PracticeCompatibilityExportMain <fixture.tsv> <fixture.provenance>" }
        VocabularyP2PracticeCompatibilityFixture.write(Path.of(args[0]), Path.of(args[1]))
    }
}

internal object VocabularyP2PracticeCompatibilityFixture {
    private val engine = PilotEngine()
    private val planner = PracticePlanner(engine)
    val profiles = (1..6).map { "vocabulary-p2-s$it" to "vocabulary-p2-s$it-1" }

    fun rows(): List<String> = buildList {
        val selected = planner.profiles().filter { profile -> profiles.any { it.first == profile.focusId } }
        add(row("CATALOG", FreezeHasher("p2-practice-catalog-v1").apply {
            integer("count", selected.size)
            selected.forEachIndexed { index, profile ->
                field("[$index].focusId", profile.focusId); field("[$index].planRevision", profile.planRevision)
                field("[$index].title", profile.title); field("[$index].description", profile.description)
                integer("[$index].itemCount", profile.itemCount); strings("[$index].prerequisiteSkillIds", profile.prerequisiteSkillIds)
            }
        }.finish()))
        SEEDS.forEach { seed -> profiles.forEach { (focus, revision) ->
            runCatching { planner.plan(focus, revision, seed) }.fold(
                onSuccess = { add(row("PLAN", focus, seed.toString(), planHash(it))) },
                onFailure = { add(row("PLAN_FAILURE", focus, seed.toString(), it::class.qualifiedName.orEmpty(), it.message.orEmpty())) },
            )
        } }
        profiles.forEach { (focus, revision) ->
            val plan = planner.plan(focus, revision, 47)
            plan.items.forEach { item ->
                val correct = engine.evaluate(item.exercise, PilotNineteenCompatibilityGolden.canonicalAnswer(item.exercise))
                val wrong = engine.evaluate(item.exercise, wrongAnswer(item.exercise))
                val partial = engine.evaluate(item.exercise, partialAnswer(item.exercise))
                listOf("correct" to correct, "wrong" to wrong, "partial" to partial).forEach { (case, evaluation) ->
                    add(row(
                        "EVALUATION", focus, item.position.toString(), taskId(item.exercise), case,
                        PilotNineteenCompatibilityGolden.evaluationHash(evaluation),
                        PracticeCompletionPolicy.canAdvance(plan.completionRuleId, completion(evaluation, true)).toString(),
                    ))
                    add(row(
                        "EXPOSURE", focus, item.position.toString(), case,
                        PracticeFeedbackExposureCalculator.releasedKeys(
                            item.feedbackExposureKeys, item.assessments, listOf(feedback(evaluation)), plan.exposurePolicyId,
                        ).joinToString("\u001f").ifEmpty { "-" },
                    ))
                }
                add(row(
                    "RETRY_EXPOSURE", focus, item.position.toString(),
                    PracticeFeedbackExposureCalculator.releasedKeys(
                        item.feedbackExposureKeys, item.assessments, listOf(feedback(wrong), feedback(correct)),
                        plan.exposurePolicyId,
                    ).joinToString("\u001f").ifEmpty { "-" },
                ))
                add(row(
                    "TEACHING_REQUIRED", focus, item.position.toString(),
                    PracticeCompletionPolicy.canAdvance(plan.completionRuleId, completion(correct, false)).toString(),
                ))
            }
        }
        add(row("MISSING_LATEST", PracticeCompletionPolicy.canAdvance("vocabulary-p2-outcome-1", null).toString()))
    }

    fun fixtureBytes() = rows().joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)

    fun write(fixturePath: Path, provenancePath: Path) {
        require(!Files.exists(fixturePath) && !Files.exists(provenancePath)) {
            "Refusing to overwrite frozen vocabulary P2 practice compatibility evidence"
        }
        val rows = rows(); val bytes = rows.joinToString("\n", postfix = "\n").toByteArray(Charsets.UTF_8)
        Files.createDirectories(fixturePath.toAbsolutePath().parent)
        Files.createDirectories(provenancePath.toAbsolutePath().parent)
        Files.write(fixturePath, bytes, StandardOpenOption.CREATE_NEW)
        Files.writeString(provenancePath, buildString {
            appendLine("releaseSourceCommit=$RELEASE_SOURCE_COMMIT")
            appendLine("releaseVersion=0.22.0")
            appendLine("captureDate=$CAPTURE_DATE")
            appendLine("projectionSchema=p2-practice-engine-v1")
            appendLine("planPairs=${profiles.joinToString(",") { "${it.second}:pilot-19/generator-19/evaluation-19" }}")
            appendLine("planRevisions=${profiles.joinToString(",") { it.second }}")
            appendLine("reviewState=compatibility-only-not-linguistic-approval")
            appendLine("rowCount=${rows.size}")
            appendLine("fixtureSha256=${bytes.sha256()}")
            appendLine(
                "description=Six published P2 plans at boundary seeds with literal tasks, teaching, policies, private " +
                    "assessments, correct/wrong/partial grading, ordered exposure, retries and completion eligibility.",
            )
        }, Charsets.UTF_8, StandardOpenOption.CREATE_NEW)
    }

    private fun planHash(plan: PracticePlan) = FreezeHasher("p2-practice-plan-v1").apply {
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
            val p = "items[$index]"; integer("$p.position", item.position)
            field("$p.generationHash", PilotNineteenCompatibilityGolden.generationHash(item.exercise))
            descriptor("$p.target", item.targetDescriptor)
            strings("$p.answerExposureKeys", item.answerExposureKeys); strings("$p.promptExposureKeys", item.promptExposureKeys)
            strings("$p.lessonExposureKeys", item.lessonExposureKeys); strings("$p.hintExposureKeys", item.hintExposureKeys)
            strings("$p.feedbackExposureKeys", item.feedbackExposureKeys); integer("$p.assessments.count", item.assessments.size)
            item.assessments.forEachIndexed { assessmentIndex, assessment ->
                val a = "$p.assessments[$assessmentIndex]"; nullable("$a.slotId", assessment.slotId)
                descriptor("$a.target", assessment.targetDescriptor); field("$a.role", assessment.assessmentRole)
                strings("$a.answerExposureKeys", assessment.answerExposureKeys)
            }
        }
    }.finish()

    private fun FreezeHasher.descriptor(name: String, value: PracticeTargetDescriptor) {
        field("$name.label", value.label); nullable("$name.targetSkillId", value.targetSkillId)
        nullable("$name.lexicalTarget", value.lexicalTarget?.let {
            listOf(it.entryId, it.senseId, it.facet, it.direction).joinToString(":")
        })
        nullable("$name.assessmentMode", value.assessmentMode); nullable("$name.targetScopeId", value.targetScopeId)
        nullable("$name.readingTarget", value.readingTarget?.facet); nullable("$name.readingRole", value.readingRole)
        nullable("$name.matchingTarget", value.matchingTarget?.let { "${it.facet}:${it.direction}" })
        field("$name.targetKey", value.targetKey())
    }

    private fun wrongAnswer(exercise: GeneratedExercise): Answer {
        val task = (exercise.source as VocabularyP2ExerciseSource).task
        return when (task.kind) {
            VocabularyP2TaskKind.CHOICE, VocabularyP2TaskKind.CONTEXT, VocabularyP2TaskKind.READING -> {
                val correct = (PilotNineteenCompatibilityGolden.canonicalAnswer(exercise) as Answer.Text).text
                Answer.Text(task.choices.first { it != correct })
            }
            VocabularyP2TaskKind.MATCHING -> {
                val pairs = requireNotNull(exercise.inspector.vocabularySource).matchingPairs
                val rights = pairs.map { it.rightId }.toMutableList().also { java.util.Collections.swap(it, 0, 1) }
                Answer.Matching(pairs.mapIndexed { index, pair -> MatchingPair(pair.leftId, rights[index]) })
            }
            VocabularyP2TaskKind.TILES -> {
                val correct = PilotNineteenCompatibilityGolden.canonicalAnswer(exercise) as Answer.Order
                Answer.Order(correct.blockIds.reversed())
            }
        }
    }

    private fun partialAnswer(exercise: GeneratedExercise): Answer = when (
        val answer = PilotNineteenCompatibilityGolden.canonicalAnswer(exercise)
    ) {
        is Answer.Text -> Answer.Text("")
        is Answer.Order -> Answer.Order(answer.blockIds.dropLast(1))
        is Answer.Matching -> Answer.Matching(answer.pairs.dropLast(1))
        is Answer.Gaps -> Answer.Gaps(answer.selections.dropLast(1))
    }

    private fun feedback(value: Evaluation) = PracticeFeedbackAttemptObservation(
        value.targetStatus, value.gapResults.orEmpty(), value.linguisticStatus, value.responseConstraintStatus,
        value.issues.map { it.code }, value.meaningSatisfaction, value.matchingResults.orEmpty(),
    )
    private fun completion(value: Evaluation, taught: Boolean) = PracticeCompletionAttemptObservation(
        value.targetStatus, value.linguisticStatus, value.responseConstraintStatus, value.issues.map { it.code }, taught,
        value.meaningSatisfaction, value.matchingResults.orEmpty(),
    )
    private fun taskId(exercise: GeneratedExercise) = requireNotNull(exercise.inspector.vocabularySource).taskId
    private fun row(vararg values: String) = values.joinToString("\t")
    private fun ByteArray.sha256() = MessageDigest.getInstance("SHA-256").digest(this)
        .joinToString("") { "%02x".format(Locale.ROOT, it.toInt() and 0xff) }

    private const val RELEASE_SOURCE_COMMIT = "844b88af33cc729e095d443adb72fcd72cd17534"
    private const val CAPTURE_DATE = "2026-11-13"
    private val SEEDS = listOf(0, 1, 47, Int.MAX_VALUE)
}
