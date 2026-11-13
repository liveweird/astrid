package app.astrid.engine

internal object PilotReviewVersions {
    fun hasWriting(snapshot: PilotContentSnapshot) = snapshot.contentRevision in WRITING_REVISIONS
    fun hasWordChoice(snapshot: PilotContentSnapshot) = snapshot.contentRevision in WORD_CHOICE_REVISIONS
    fun hasMatching(snapshot: PilotContentSnapshot) = snapshot.contentRevision in MATCHING_REVISIONS
    fun hasGaps(snapshot: PilotContentSnapshot) = snapshot.contentRevision in GAP_REVISIONS
    fun hasModuleOne(snapshot: PilotContentSnapshot) = snapshot.contentRevision in MODULE_ONE_REVISIONS
    fun hasInanimateReference(snapshot: PilotContentSnapshot) = snapshot.contentRevision in REFERENCE_REVISIONS
    fun hasPresentOrder(snapshot: PilotContentSnapshot) = snapshot.contentRevision in PRESENT_ORDER_REVISIONS

    fun provenance(snapshot: PilotContentSnapshot): String = when (snapshot.contentRevision) {
        PilotContent.CURRENT_CONTENT_REVISION ->
            "docs/knowledge/vocabulary-food-shopping-activation.md and " +
                "docs/knowledge/vocabulary-food-shopping-packet.md (P2 L050-L063, L009-S02 and 21 literal activities; " +
                "2026-11-12), plus retained pair-eighteen sources"
        in setOf(PilotContent.RETAINED_SEVENTEEN_CONTENT_REVISION, PilotContent.RETAINED_EIGHTEEN_CONTENT_REVISION) ->
            "docs/knowledge/vocabulary-course-blueprint.md and docs/knowledge/vocabulary-expansion-activation.md " +
                "(P1 L041-L049 and 23 literal activities; 2026-11-12), plus retained pair-sixteen sources"
        PilotContent.RETAINED_SIXTEEN_CONTENT_REVISION ->
            "docs/knowledge/danish-vocabulary-seed.md (retained seed) plus " +
                "docs/knowledge/module-one-language-pack.md (L024 packet), " +
                "docs/knowledge/module-one-activation.md (L025, C004 and Module 1 tasks), " +
                "docs/knowledge/inanimate-reference-activation.md (L026 and G038 reference tasks), and " +
                "docs/knowledge/module-one-course-slice.md (L027-L040 and S05-S13 tasks; 2026-11-11)"
        PilotContent.RETAINED_FIFTEEN_CONTENT_REVISION ->
            "docs/knowledge/danish-vocabulary-seed.md (retained seed) plus " +
                "docs/knowledge/module-one-language-pack.md (L024 packet), " +
                "docs/knowledge/module-one-activation.md (L025, C004 and Module 1 tasks), " +
                "docs/knowledge/inanimate-reference-activation.md (L026 and G038 reference tasks), and " +
                "docs/knowledge/module-one-course-slice.md (L027-L038 and S05-S12 tasks; 2026-11-11)"
        PilotContent.RETAINED_FOURTEEN_CONTENT_REVISION ->
            "docs/knowledge/danish-vocabulary-seed.md (retained seed) plus " +
                "docs/knowledge/module-one-language-pack.md (L024 packet), " +
                "docs/knowledge/module-one-activation.md (L025, C004 and Module 1 tasks), " +
                "docs/knowledge/inanimate-reference-activation.md (L026 and G038 reference tasks), and " +
                "docs/knowledge/module-one-course-slice.md (L027-L036 and S05-S11 tasks; 2026-11-11)"
        PilotContent.RETAINED_THIRTEEN_CONTENT_REVISION ->
            "docs/knowledge/danish-vocabulary-seed.md (retained seed) plus " +
                "docs/knowledge/module-one-language-pack.md (L024 packet), " +
                "docs/knowledge/module-one-activation.md (L025, C004 and Module 1 tasks), " +
                "docs/knowledge/inanimate-reference-activation.md (L026 and G038 reference tasks), and " +
                "docs/knowledge/module-one-course-slice.md (L027-L033 and S05-S10 tasks; 2026-11-11)"
        PilotContent.RETAINED_TWELVE_CONTENT_REVISION ->
            "docs/knowledge/danish-vocabulary-seed.md (retained seed) plus " +
                "docs/knowledge/module-one-language-pack.md (L024 packet), " +
                "docs/knowledge/module-one-activation.md (L025, C004 and Module 1 tasks), " +
                "docs/knowledge/inanimate-reference-activation.md (L026 and G038 reference tasks), and " +
                "docs/knowledge/module-one-course-slice.md (L027-L032 and S05-S09 tasks; 2026-11-11)"
        PilotContent.RETAINED_ELEVEN_CONTENT_REVISION ->
            "docs/knowledge/danish-vocabulary-seed.md (retained seed) plus " +
                "docs/knowledge/module-one-language-pack.md (L024 packet), " +
                "docs/knowledge/module-one-activation.md (L025, C004 and Module 1 tasks), " +
                "docs/knowledge/inanimate-reference-activation.md (L026 and G038 reference tasks), and " +
                "docs/knowledge/module-one-course-slice.md (L027 and S05-S07 tasks; 2026-11-11)"
        PilotContent.RETAINED_TEN_CONTENT_REVISION ->
            "docs/knowledge/danish-vocabulary-seed.md (retained seed) plus " +
                "docs/knowledge/module-one-language-pack.md (L024 packet), " +
                "docs/knowledge/module-one-activation.md (L025, C004 and Module 1 tasks), " +
                "docs/knowledge/inanimate-reference-activation.md (L026 and G038 reference tasks), and " +
                "docs/knowledge/module-one-course-slice.md (L027 and S05-S06 tasks; 2026-11-10)"
        PilotContent.RETAINED_NINE_CONTENT_REVISION ->
            "docs/knowledge/danish-vocabulary-seed.md (retained seed) plus " +
                "docs/knowledge/module-one-language-pack.md (L024 packet), " +
                "docs/knowledge/module-one-activation.md (L025, C004 and Module 1 tasks), and " +
                "docs/knowledge/inanimate-reference-activation.md (L026 and G038 reference tasks; 2026-11-10)"
        PilotContent.RETAINED_EIGHT_CONTENT_REVISION ->
            "docs/knowledge/danish-vocabulary-seed.md (retained seed) plus " +
                "docs/knowledge/module-one-language-pack.md (L024 packet) and " +
                "docs/knowledge/module-one-activation.md (L025, C004 and Module 1 tasks; 2026-11-09)"
        in setOf(
            PilotContent.RETAINED_FIVE_CONTENT_REVISION,
            PilotContent.RETAINED_SIX_CONTENT_REVISION,
            PilotContent.RETAINED_SEVEN_CONTENT_REVISION,
        ) -> "docs/knowledge/danish-vocabulary-seed.md (retained seed) plus " +
            "docs/knowledge/module-one-language-pack.md (L024 authoring packet, 2026-11-08)"
        else -> PilotReviewMetadata.PROVENANCE
    }

    fun sourceReview(snapshot: PilotContentSnapshot): String = when (snapshot.contentRevision) {
        PilotContent.CURRENT_CONTENT_REVISION ->
            "2026-11-12 official COR form checks and bounded authored P2 frame review; see " +
                "docs/knowledge/vocabulary-food-shopping-activation.md and docs/knowledge/vocabulary-food-shopping-packet.md"
        in setOf(PilotContent.RETAINED_SEVENTEEN_CONTENT_REVISION, PilotContent.RETAINED_EIGHTEEN_CONTENT_REVISION) ->
            "2026-11-12 official COR form checks and bounded authored P1 frame review; see " +
                "docs/knowledge/vocabulary-expansion-activation.md and docs/knowledge/vocabulary-work-study-packet.md"
        PilotContent.RETAINED_SIXTEEN_CONTENT_REVISION ->
            "2026-11-07 retained seed; 2026-11-08 pair-five extension; " +
                "2026-11-09 L025/C004/Module 1 source checks; 2026-11-10 L026/G038 and L027/S05-S06 source checks; " +
                "2026-11-11 S07-S13 source checks; see docs/knowledge/pilot-source-review.md, " +
                "docs/knowledge/module-one-language-pack.md, docs/knowledge/module-one-activation.md, " +
                "docs/knowledge/inanimate-reference-activation.md and docs/knowledge/module-one-course-slice.md"
        PilotContent.RETAINED_FIFTEEN_CONTENT_REVISION ->
            "2026-11-07 retained seed; 2026-11-08 pair-five extension; " +
                "2026-11-09 L025/C004/Module 1 source checks; 2026-11-10 L026/G038 and L027/S05-S06 source checks; " +
                "2026-11-11 S07-S12 source checks; see docs/knowledge/pilot-source-review.md, " +
                "docs/knowledge/module-one-language-pack.md, docs/knowledge/module-one-activation.md, " +
                "docs/knowledge/inanimate-reference-activation.md and docs/knowledge/module-one-course-slice.md"
        PilotContent.RETAINED_FOURTEEN_CONTENT_REVISION ->
            "2026-11-07 retained seed; 2026-11-08 pair-five extension; " +
                "2026-11-09 L025/C004/Module 1 source checks; 2026-11-10 L026/G038 and L027/S05-S06 source checks; " +
                "2026-11-11 S07-S11 source checks; see docs/knowledge/pilot-source-review.md, " +
                "docs/knowledge/module-one-language-pack.md, docs/knowledge/module-one-activation.md, " +
                "docs/knowledge/inanimate-reference-activation.md and docs/knowledge/module-one-course-slice.md"
        PilotContent.RETAINED_THIRTEEN_CONTENT_REVISION ->
            "2026-11-07 retained seed; 2026-11-08 pair-five extension; " +
                "2026-11-09 L025/C004/Module 1 source checks; 2026-11-10 L026/G038 and L027/S05-S06 source checks; " +
                "2026-11-11 S07-S10 source checks; see docs/knowledge/pilot-source-review.md, " +
                "docs/knowledge/module-one-language-pack.md, docs/knowledge/module-one-activation.md, " +
                "docs/knowledge/inanimate-reference-activation.md and docs/knowledge/module-one-course-slice.md"
        PilotContent.RETAINED_TWELVE_CONTENT_REVISION ->
            "2026-11-07 retained seed; 2026-11-08 pair-five extension; " +
                "2026-11-09 L025/C004/Module 1 source checks; 2026-11-10 L026/G038 and L027/S05-S06 source checks; " +
                "2026-11-11 S07-S09 source checks; see docs/knowledge/pilot-source-review.md, " +
                "docs/knowledge/module-one-language-pack.md, docs/knowledge/module-one-activation.md, " +
                "docs/knowledge/inanimate-reference-activation.md and docs/knowledge/module-one-course-slice.md"
        PilotContent.RETAINED_ELEVEN_CONTENT_REVISION ->
            "2026-11-07 retained seed; 2026-11-08 pair-five extension; " +
                "2026-11-09 L025/C004/Module 1 source checks; 2026-11-10 L026/G038 and L027/S05-S06 source checks; " +
                "2026-11-11 S07 possession source checks; see docs/knowledge/pilot-source-review.md, " +
                "docs/knowledge/module-one-language-pack.md, docs/knowledge/module-one-activation.md, " +
                "docs/knowledge/inanimate-reference-activation.md and docs/knowledge/module-one-course-slice.md"
        PilotContent.RETAINED_TEN_CONTENT_REVISION ->
            "2026-11-07 retained seed; 2026-11-08 pair-five extension; " +
                "2026-11-09 L025/C004/Module 1 source checks; 2026-11-10 L026/G038 and L027/S05-S06 source checks; see " +
                "docs/knowledge/pilot-source-review.md, docs/knowledge/module-one-language-pack.md, " +
                "docs/knowledge/module-one-activation.md, docs/knowledge/inanimate-reference-activation.md and " +
                "docs/knowledge/module-one-course-slice.md"
        PilotContent.RETAINED_NINE_CONTENT_REVISION ->
            "2026-11-07 retained seed; 2026-11-08 pair-five extension; " +
                "2026-11-09 L025/C004/Module 1 source checks; 2026-11-10 L026/G038 source checks; see " +
                "docs/knowledge/pilot-source-review.md, docs/knowledge/module-one-language-pack.md, " +
                "docs/knowledge/module-one-activation.md and docs/knowledge/inanimate-reference-activation.md"
        PilotContent.RETAINED_EIGHT_CONTENT_REVISION ->
            "2026-11-07 retained seed; 2026-11-08 pair-five extension; " +
                "2026-11-09 L025/C004/Module 1 source checks; see docs/knowledge/pilot-source-review.md, " +
                "docs/knowledge/module-one-language-pack.md and docs/knowledge/module-one-activation.md"
        in setOf(
            PilotContent.RETAINED_FIVE_CONTENT_REVISION,
            PilotContent.RETAINED_SIX_CONTENT_REVISION,
            PilotContent.RETAINED_SEVEN_CONTENT_REVISION,
        ) -> "2026-11-07 retained seed; 2026-11-08 pair-five extension; see " +
            "docs/knowledge/pilot-source-review.md and docs/knowledge/module-one-language-pack.md"
        else -> PilotReviewMetadata.SOURCE_REVIEW_DATE
    }

    private val WRITING_REVISIONS = setOf(
        PilotContent.RETAINED_FOUR_CONTENT_REVISION,
        PilotContent.RETAINED_FIVE_CONTENT_REVISION,
        PilotContent.RETAINED_SIX_CONTENT_REVISION,
        PilotContent.RETAINED_SEVEN_CONTENT_REVISION,
        PilotContent.RETAINED_EIGHT_CONTENT_REVISION,
        PilotContent.RETAINED_NINE_CONTENT_REVISION,
        PilotContent.RETAINED_TEN_CONTENT_REVISION,
        PilotContent.RETAINED_ELEVEN_CONTENT_REVISION,
        PilotContent.RETAINED_TWELVE_CONTENT_REVISION,
        PilotContent.RETAINED_THIRTEEN_CONTENT_REVISION,
        PilotContent.RETAINED_FOURTEEN_CONTENT_REVISION,
        PilotContent.RETAINED_FIFTEEN_CONTENT_REVISION,
        PilotContent.RETAINED_SIXTEEN_CONTENT_REVISION,
        PilotContent.RETAINED_SEVENTEEN_CONTENT_REVISION,
        PilotContent.RETAINED_EIGHTEEN_CONTENT_REVISION,
        PilotContent.CURRENT_CONTENT_REVISION,
    )
    private val WORD_CHOICE_REVISIONS = WRITING_REVISIONS - PilotContent.RETAINED_FOUR_CONTENT_REVISION
    private val MATCHING_REVISIONS = WORD_CHOICE_REVISIONS - PilotContent.RETAINED_FIVE_CONTENT_REVISION
    private val GAP_REVISIONS = MATCHING_REVISIONS - PilotContent.RETAINED_SIX_CONTENT_REVISION
    private val MODULE_ONE_REVISIONS = GAP_REVISIONS - PilotContent.RETAINED_SEVEN_CONTENT_REVISION
    private val REFERENCE_REVISIONS = MODULE_ONE_REVISIONS - PilotContent.RETAINED_EIGHT_CONTENT_REVISION
    private val PRESENT_ORDER_REVISIONS = REFERENCE_REVISIONS - PilotContent.RETAINED_NINE_CONTENT_REVISION
}
