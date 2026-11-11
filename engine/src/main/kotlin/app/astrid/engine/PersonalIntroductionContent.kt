package app.astrid.engine

internal object PersonalIntroductionContent {
    fun tasks(formatId: String): List<NamedPresentTask> = when (formatId) {
        "E022" -> listOf(
            task(
                formatId,
                Frame("personal-select-name", "L001", "jeg", "the speaker", "I am called Anna.", "Anna", "the name Anna"),
                namePattern(),
            ),
            task(
                formatId,
                Frame(
                    "personal-select-residence", "L004", "hun", "one stated woman", "She lives in Aarhus.",
                    "i Aarhus", "in Aarhus",
                ),
                residencePattern(),
            ),
            task(
                formatId,
                Frame(
                    "personal-select-language", "L005", "vi", "the speaker and another person", "We speak Danish.",
                    "dansk", "Danish",
                ),
                languagePattern(),
            ),
        )
        "E023" -> listOf(
            task(
                formatId,
                Frame("personal-recall-name", "L002", "du", "one addressee", "You are called Nora.", "Nora", "the name Nora"),
                namePattern(),
            ),
            task(
                formatId,
                Frame(
                    "personal-recall-residence", "L007", "de", "several other people", "They live in Odense.",
                    "i Odense", "in Odense",
                ),
                residencePattern(),
            ),
            task(
                formatId,
                Frame(
                    "personal-recall-language", "L006", "I", "several addressees", "You speak Danish.",
                    "dansk", "Danish",
                ),
                languagePattern(),
            ),
        )
        else -> emptyList()
    }

    fun requiredTeaching(): List<IntroductionSection> =
        listOf(namePattern(), residencePattern(), languagePattern()).flatMap { pattern ->
            val verb = pattern.verb
            val teaching = pattern.teaching
            listOf(
                IntroductionSection("Dictionary and present forms", "at ${verb.lemma} → ${verb.present}", "da"),
                IntroductionSection("Meaning", teaching.meaning),
                IntroductionSection("Worked example", teaching.workedExample, "da"),
                IntroductionSection("English example", teaching.workedMeaning),
            )
        }

    private fun task(formatId: String, frame: Frame, pattern: Pattern) = NamedPresentTask(
        frame.id, formatId, frame.subjectId, frame.subject, frame.reference, pattern.verb,
        frame.complement, pattern.componentIds, PresentOrderContent.PERSONAL_SCOPE_ID,
        PresentOrderContent.PERSONAL_PATTERN_KEY, frame.englishProposition, frame.complementMeaning,
        pattern.constructionId, pattern.complementRole, PresentOrderContent.pairTwelveIntroducedPresents(),
        pattern.teaching, pattern.suppliedLabel,
    )

    private fun namePattern() = Pattern(
        PilotContent.beCalled, emptyList(), "C004", "PREDICATIVE", "name",
        NamedPresentTeaching(
            "The verb gives someone's name.", "Hun hedder Nora.", "She is called Nora.",
            "Use the stored one-word present form hedder from at hedde.",
        ),
    )

    private fun residencePattern() = Pattern(
        PilotContent.live, listOf("L032"), "C006", "LOCATION_COMPLEMENT", "location",
        NamedPresentTeaching(
            "The verb states where someone lives.", "Jeg bor i Odense.", "I live in Odense.",
            "Use the stored one-word present form bor from at bo.",
        ),
    )

    private fun languagePattern() = Pattern(
        PilotContent.speak, listOf("L031"), "C002", "LANGUAGE_COMPLEMENT", "language",
        NamedPresentTeaching(
            "The verb states which language someone speaks.", "Du taler dansk.", "You speak Danish.",
            "Use the stored one-word present form taler from at tale.",
        ),
    )

    private data class Frame(
        val id: String,
        val subjectId: String,
        val subject: String,
        val reference: String,
        val englishProposition: String,
        val complement: String,
        val complementMeaning: String,
    )

    private data class Pattern(
        val verb: Verb,
        val componentIds: List<String>,
        val constructionId: String,
        val complementRole: String,
        val suppliedLabel: String,
        val teaching: NamedPresentTeaching,
    )
}
