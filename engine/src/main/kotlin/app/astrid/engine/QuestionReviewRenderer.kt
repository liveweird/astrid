package app.astrid.engine

internal object QuestionReviewRenderer {
    fun appendTo(builder: StringBuilder, snapshot: PilotContentSnapshot, reviewSeed: Int) = with(builder) {
        appendLine("## Neutral yes/no question inventory")
        appendLine()
        appendLine(
            "| Task | Positive proposition | English question | Frame | Canonical | Roles | " +
                "Licensed kind | Answer keys | Prompt support | Feedback keys |",
        )
        appendLine("| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |")
        NeutralQuestionContent.tasks.forEachIndexed { index, task ->
            val exercise = NeutralQuestionExerciseGenerator.generate(
                GenerationRequest(
                    NeutralQuestionContent.FORMAT_ID,
                    NeutralQuestionContent.TARGET_SKILL_ID,
                    reviewSeed,
                    index,
                    snapshot.contentRevision,
                    snapshot.generatorRevision,
                ),
                task,
            )
            val licensedKind = requireNotNull(exercise.inspector.tileSource).licensedOrders.single().kind
            appendLine(
                "| ${task.id} | ${task.positiveProposition} | ${task.englishQuestion} | ${task.frameVariantId} | " +
                    "${exercise.inspector.canonicalSentence} | " +
                    "${exercise.inspector.clauseRoles.joinToString(", ")} | " +
                    "$licensedKind | ${task.answerExposureKeys.joinToString(", ")} | " +
                    "${task.promptExposureKeys.joinToString(", ")} | ${task.feedbackExposureKeys.joinToString(", ")} |",
            )
        }
        appendLine()
        appendLine("### Sources and boundaries")
        appendLine()
        appendLine(
            "- C010 neutral yes/no question order: " +
                "https://sproget.dk/sprogviden/spoergsmaal-og-svar/maal-og-maele/ledstilling-i-ja-nej-spoergsmaal/",
        )
        appendLine("- L039 / L039-S01: `ja`, positive short answer; https://ordnet.dk/ddo/ordbog/ja")
        appendLine("- L040 / L040-S01: `nej`, negative short answer; https://ordnet.dk/ddo/ordbog/nej")
        appendLine(
            "- Scope: four authored positive C010 frames only; modal, negative, echo, mon, and jo " +
                "questions are excluded.",
        )
        appendLine("- `ja` and `nej` are supplied support only; neither is an assessed lexical target.")
        appendLine()
        appendLine("### Required teaching")
        appendLine()
        appendLine("- Profile: `${QuestionPracticePlanner.FOCUS_ID}` / `${QuestionPracticePlanner.PLAN_REVISION}`")
        appendLine(
            "- Policy: `${PresentOrderPracticePlanner.ORDER_COMPLETION_RULE_ID}` / " +
                "`${PresentOrderPracticePlanner.ORDER_EXPOSURE_POLICY_ID}`",
        )
        NeutralQuestionContent.introduction().forEach { section ->
            appendLine("- ${section.title} (${section.bodyLanguage ?: "en"}): ${section.body}")
        }
        appendLine()
    }
}
