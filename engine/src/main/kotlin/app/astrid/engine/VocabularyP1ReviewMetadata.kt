package app.astrid.engine

internal object VocabularyP1ReviewMetadata {
    val records = listOf(
        record("L041", "skrive", "write a short text", mapOf("infinitive" to "skrive", "finite-present" to "skriver"),
            "P1 human subject with et brev only; other objects and senses excluded.", "30533"),
        record("L042", "computer", "computer device", mapOf("count-bundle" to "en computer"),
            "P1 possession and identification only; device-action inferences excluded.", "63924"),
        record("L043", "studere", "study as a learner", mapOf("infinitive" to "studere", "finite-present" to "studerer"),
            "P1 human subject without an object; institution and subject complements excluded.", "32395"),
        record("L044", "kursus", "course or class programme", mapOf("count-bundle" to "et kursus"),
            "P1 labelled meaning and fixed identification only; possession and scheduling excluded.", "41200"),
        record("L045", "drikke", "drink a beverage", mapOf("infinitive" to "drikke", "finite-present" to "drikker"),
            "P1 human subject with bare kaffe or vand only; other objects and senses excluded.", "30480"),
        record("L046", "kaffe", "coffee as beverage or substance", mapOf("mass-word" to "kaffe"),
            "Selected bare mass use in drikker kaffe; serving-count uses excluded.", "43580"),
        record("L047", "vand", "water as liquid", mapOf("mass-word" to "vand"),
            "Selected bare liquid mass use in drikker vand; serving and body-of-water senses excluded.", "41032"),
        record("L048", "hjemme", "at home as static location", mapOf("adverb" to "hjemme"),
            "Static P1 location with arbejde or studere; directional hjem excluded.", "10260"),
        record("L049", "om morgenen", "in the morning", mapOf("fixed-chunk" to "om morgenen"),
            "One supplied habitual day-part constituent; internal morphology and substitutions excluded.", "43995"),
    )

    private fun record(id: String, headword: String, sense: String, forms: Map<String, String>, constraints: String,
        corId: String) = LexicalReviewRecord(
        id,
        headword,
        sense,
        forms,
        constraints,
        listOf("https://ordregister.dk/id/COR.$corId"),
        PilotReviewMetadata.REVIEW_STATE,
    )
}
