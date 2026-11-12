package app.astrid.engine

internal object VocabularyP1Snapshot {
    fun extend(retained: PilotContentSnapshot): PilotContentSnapshot = retained.copy(
        contentRevision = PilotContent.CURRENT_CONTENT_REVISION,
        generatorRevision = PilotContent.CURRENT_GENERATOR_REVISION,
        formats = retained.formats + VocabularyP1Content.formats,
        verbs = retained.verbs + listOf(
            Verb("L041", "skrive", "skriver", "write a short text"),
            Verb("L043", "studere", "studerer", "study as a learner"),
            Verb("L045", "drikke", "drikker", "drink a beverage"),
        ),
        nouns = retained.nouns + listOf(
            NounEntry("L042", "computer", "computer device", NounGender.COMMON),
            NounEntry("L044", "kursus", "course or class programme", NounGender.NEUTER),
        ),
        fixedWords = retained.fixedWords + listOf(
            FixedWordEntry("L046", "kaffe", "coffee as beverage or substance"),
            FixedWordEntry("L047", "vand", "water as liquid"),
            FixedWordEntry("L048", "hjemme", "at home as static location"),
            FixedWordEntry("L049", "om morgenen", "in the morning as a fixed chunk"),
        ),
        lexicalReview = retained.lexicalReview + VocabularyP1ReviewMetadata.records,
    )
}
