package app.astrid.engine

internal object VocabularyP3Snapshot {
    fun extend(retained: PilotContentSnapshot): PilotContentSnapshot = retained.copy(
        contentRevision = VocabularyP3Content.CONTENT_REVISION,
        generatorRevision = VocabularyP3Content.GENERATOR_REVISION,
        formats = retained.formats + VocabularyP3Content.formats,
        verbs = retained.verbs + listOf(
            Verb("L065", "cykle", "cykler", "travel by bicycle"),
            Verb("L067", "gå", "går", "walk or go on foot"),
            Verb("L074", "køre", "kører", "travel or drive using a vehicle"),
            Verb("L075", "vente", "venter", "wait"),
            Verb("L077", "skifte", "skifter", "change trains or connections"),
        ),
        nouns = retained.nouns + listOf(
            NounEntry("L064", "cykel", "bicycle", NounGender.COMMON),
            NounEntry("L066", "station", "transport station", NounGender.COMMON),
            NounEntry("L070", "bibliotek", "library", NounGender.NEUTER),
            NounEntry("L071", "bus", "bus vehicle", NounGender.COMMON),
            NounEntry("L072", "tog", "train vehicle", NounGender.NEUTER),
            NounEntry("L073", "billet", "travel ticket", NounGender.COMMON),
            NounEntry("L078", "apotek", "pharmacy", NounGender.NEUTER),
        ),
        fixedWords = retained.fixedWords + listOf(
            FixedWordEntry("L068", "til stationen", "to the station destination"),
            FixedWordEntry("L069", "hjem", "homeward destination"),
            FixedWordEntry("L076", "på stationen", "at the station static location"),
            FixedWordEntry("L079", "til apoteket", "to the pharmacy destination"),
        ),
        lexicalReview = retained.lexicalReview + VocabularyP3ReviewMetadata.records,
    )
}

internal object VocabularyP3ReviewMetadata {
    val records = listOf(
        record("L064", "cykel", "one bicycle", mapOf("count-bundle" to "en cykel"),
            "Identification and bounded bicycle contexts only.", "45412"),
        record("L065", "cykle", "travel by bicycle", mapOf("infinitive" to "cykle", "finite-present" to "cykler"),
            "Human bicycle travel with supplied context.", "37334"),
        record("L066", "station", "transport station", mapOf("count-bundle" to "en station"),
            "Recognition; definite form only inside supplied chunks.", "69303"),
        record("L067", "gå", "walk or go on foot", mapOf("infinitive" to "gå", "finite-present" to "går"),
            "Only the packet's supplied destination frames.", "30234"),
        record("L068", "til stationen", "to the station", mapOf("fixed-chunk" to "til stationen"),
            "Whole destination chunk; no productive preposition or definiteness.", "00947", "69303"),
        record("L069", "hjem", "homeward destination", mapOf("adverb" to "hjem"),
            "Directional use with the packet's movement verbs.", "10109"),
        record("L070", "bibliotek", "library", mapOf("count-bundle" to "et bibliotek"),
            "Recognition and identification only.", "58895"),
        record("L071", "bus", "bus vehicle", mapOf("count-bundle" to "en bus"),
            "Recognition and identification only.", "63246"),
        record("L072", "tog", "train vehicle", mapOf("count-bundle" to "et tog"),
            "Transport vehicle sense and supplied rail context only.", "51979"),
        record("L073", "billet", "travel ticket", mapOf("count-bundle" to "en billet"),
            "Possession in the exact packet fact only.", "45414"),
        record("L074", "køre", "travel or drive using a vehicle",
            mapOf("infinitive" to "køre", "finite-present" to "kører"), "Human car-selected travel home.", "30695"),
        record("L075", "vente", "wait", mapOf("infinitive" to "vente", "finite-present" to "venter"),
            "Static station frame only.", "30094"),
        record("L076", "på stationen", "at the station", mapOf("fixed-chunk" to "på stationen"),
            "Whole static-location chunk; no productive preposition or definiteness.", "00190", "69303"),
        record("L077", "skifte", "change trains or connections",
            mapOf("infinitive" to "skifte", "finite-present" to "skifter"), "Rail scene and i Aarhus only.", "30102"),
        record("L078", "apotek", "pharmacy", mapOf("count-bundle" to "et apotek"),
            "Recognition; definite form only inside supplied destination.", "98580"),
        record("L079", "til apoteket", "to the pharmacy", mapOf("fixed-chunk" to "til apoteket"),
            "Whole destination chunk; no productive preposition or definiteness.", "00947", "98580"),
    )

    private fun record(id: String, headword: String, sense: String, forms: Map<String, String>, constraints: String,
        vararg corIds: String) = LexicalReviewRecord(
        id, headword, sense, forms, constraints,
        corIds.map { "https://ordregister.dk/id/COR.$it" }, PilotReviewMetadata.REVIEW_STATE,
    )
}
