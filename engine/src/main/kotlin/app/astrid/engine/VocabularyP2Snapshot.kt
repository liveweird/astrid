package app.astrid.engine

internal object VocabularyP2Snapshot {
    fun extend(retained: PilotContentSnapshot): PilotContentSnapshot = retained.copy(
        contentRevision = VocabularyP2Content.CONTENT_REVISION,
        generatorRevision = VocabularyP2Content.GENERATOR_REVISION,
        formats = retained.formats + VocabularyP2Content.formats,
        verbs = retained.verbs + listOf(
            Verb("L058", "købe", "køber", "purchase the selected item"),
            Verb("L059", "betale", "betaler", "settle the stated purchase"),
        ),
        nouns = retained.nouns + listOf(
            NounEntry("L055", "bolle", "bread roll", NounGender.COMMON),
            NounEntry("L056", "butik", "retail shop", NounGender.COMMON),
            NounEntry("L057", "café", "café venue", NounGender.COMMON),
            NounEntry("L062", "kop", "cup or quantity head", NounGender.COMMON),
            NounEntry("L063", "flaske", "bottle or quantity head", NounGender.COMMON),
        ),
        fixedWords = retained.fixedWords + listOf(
            FixedWordEntry("L050", "brød", "bread as food"),
            FixedWordEntry("L051", "billig", "inexpensive product label"),
            FixedWordEntry("L052", "mælk", "milk beverage or substance"),
            FixedWordEntry("L053", "undskyld", "polite attention-getter"),
            FixedWordEntry("L054", "værsgo", "handover response"),
            FixedWordEntry("L060", "tak", "thanking response"),
            FixedWordEntry("L061", "gerne", "willing wish or polite preference"),
        ),
        lexicalReview = retained.lexicalReview + VocabularyP2ReviewMetadata.records,
    )
}

internal object VocabularyP2ReviewMetadata {
    val records = listOf(
        record(
            "L050", "brød", "bread as food", mapOf("mass-word" to "brød"),
            "Selected bare mass use; count and loaf uses excluded.", "42096",
        ),
        record(
            "L051", "billig", "inexpensive product label", mapOf("adjective" to "billig"),
            "Supplied base-form recognition only; agreement excluded.", "15843",
        ),
        record(
            "L052", "mælk", "milk beverage or substance", mapOf("mass-word" to "mælk"),
            "Selected bare mass use; serving-count uses excluded.", "40408",
        ),
        record(
            "L053", "undskyld", "polite attention-getter", mapOf("interjection" to "undskyld"),
            "Attention scene only; apology and verb uses excluded.", "03908",
        ),
        record(
            "L054", "værsgo", "handover response", mapOf("interjection" to "værsgo"),
            "Stated item-handover scene only.", "03709",
        ),
        record(
            "L055", "bolle", "bread roll", mapOf("count-bundle" to "en bolle"),
            "Food noun use only; verb and unrelated noun senses excluded.", "61066",
        ),
        record(
            "L056", "butik", "retail shop", mapOf("count-bundle" to "en butik"),
            "Recognition and supplied location only.", "46584",
        ),
        record(
            "L057", "café", "café venue", mapOf("count-bundle" to "en café"),
            "Preferred accented spelling and supplied location only.", "77252",
        ),
        record(
            "L058", "købe", "purchase the selected item",
            mapOf("infinitive" to "købe", "finite-present" to "køber"), "P2 supplied objects only.", "30649",
        ),
        record(
            "L059", "betale", "settle the stated purchase",
            mapOf("infinitive" to "betale", "finite-present" to "betaler"),
            "Initially intransitive in the stated sequence.", "30300",
        ),
        record(
            "L060", "tak", "thanking response", mapOf("interjection" to "tak"),
            "Standalone or final supplied request support only.", "43870",
        ),
        record(
            "L061", "gerne", "willing wish or polite preference", mapOf("adverb" to "gerne"),
            "Selected polite-wish frame only.", "10180",
        ),
        record(
            "L062", "kop", "cup or quantity head", mapOf("count-bundle" to "en kop"),
            "Supplied en kop kaffe construction only.", "40370",
        ),
        record(
            "L063", "flaske", "bottle or quantity head", mapOf("count-bundle" to "en flaske"),
            "Supplied en flaske vand construction only.", "41536",
        ),
    )

    private fun record(id: String, headword: String, sense: String, forms: Map<String, String>, constraints: String,
        corId: String) = LexicalReviewRecord(
        id, headword, sense, forms, constraints,
        listOf("https://ordregister.dk/id/COR.$corId"), PilotReviewMetadata.REVIEW_STATE,
    )
}
