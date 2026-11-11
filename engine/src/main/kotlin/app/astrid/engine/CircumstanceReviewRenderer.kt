package app.astrid.engine

internal object CircumstanceReviewRenderer {
    fun appendTo(builder: StringBuilder, snapshot: PilotContentSnapshot, reviewSeed: Int) = with(builder) {
        appendLine("## Final-circumstance constituent-order inventory")
        appendLine()
        appendLine(
            "| Task | Frame | Canonical | Roles | Licensed kind | Answer keys | Prompt support | Feedback keys |",
        )
        appendLine("| --- | --- | --- | --- | --- | --- | --- | --- |")
        FinalCircumstanceContent.tasks.forEachIndexed { index, task ->
            val exercise = FinalCircumstanceExerciseGenerator.generate(
                GenerationRequest(
                    FinalCircumstanceContent.FORMAT_ID,
                    FinalCircumstanceContent.TARGET_SKILL_ID,
                    reviewSeed,
                    index,
                    snapshot.contentRevision,
                    snapshot.generatorRevision,
                ),
                task,
            )
            val licensedKind = requireNotNull(exercise.inspector.tileSource).licensedOrders.single().kind
            appendLine(
                "| ${task.id} | ${task.frameVariantId} | ${exercise.inspector.canonicalSentence} | " +
                    "${exercise.inspector.clauseRoles.joinToString(", ")} | $licensedKind | " +
                    "${task.answerExposureKeys.joinToString(", ")} | ${task.promptExposureKeys.joinToString(", ")} | " +
                    "${task.feedbackExposureKeys.joinToString(", ")} |",
            )
        }
        appendLine()
        appendLine("### Sources and boundaries")
        appendLine()
        appendLine(
            "- L037 / L037-S01: `i dag`, current calendar day; excludes ‘nowadays’; " +
                "https://ordnet.dk/ddo/ordbog/59001379",
        )
        appendLine(
            "- L038 / L038-S01: temporal `nu`; excludes discourse and conjunction uses; " +
                "https://ordnet.dk/ddo/ordbog/nu",
        )
        appendLine(
            "- C007 final-circumstance evidence: " +
                "https://sproget.dk/sprogviden/spoergsmaal-og-svar/dansk-sprognaevn/sv00006990/",
        )
        appendLine(
            "- Scope: one supplied final time/place circumstance only; no arbitrary placement, multiple " +
                "circumstances, fronting, or optional hjemme.",
        )
        appendLine()
        appendLine("### Required teaching")
        appendLine()
        appendLine(
            "- Profile: `${CircumstancePracticePlanner.FOCUS_ID}` / " +
                "`${CircumstancePracticePlanner.PLAN_REVISION}`",
        )
        appendLine(
            "- Policy: `${PresentOrderPracticePlanner.ORDER_COMPLETION_RULE_ID}` / " +
                "`${PresentOrderPracticePlanner.ORDER_EXPOSURE_POLICY_ID}`",
        )
        FinalCircumstanceContent.introduction().forEach { section ->
            appendLine("- ${section.title} (${section.bodyLanguage ?: "en"}): ${section.body}")
        }
        appendLine()
    }
}
