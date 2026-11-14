@file:Suppress("MagicNumber")

package app.astrid.engine

/** Authored direct-entry teaching. Exposure belongs to the material rendered by each section. */
internal object VocabularyP3Teaching {
    data class Material(val sections: List<IntroductionSection>, val exposureKeys: List<String>)

    fun forTask(taskId: String): Material = forSession(
        taskId.removePrefix("p3-").substringBefore('.').toInt(),
    )

    fun forSession(number: Int): Material = when (number) {
        1 -> first()
        2 -> second()
        3 -> third()
        4 -> fourth()
        5 -> fifth()
        6 -> sixth()
        else -> error("Unsupported P3 teaching session: $number")
    }

    private fun first() = build {
        names()
        word(VocabularyP3Content.car)
        countBundles()
        possession()
        english("Bicycle display", "Bicycle display is an English label for the displayed item.",
            "vocabulary-p3:support:bicycle-display")
        word(VocabularyP3Content.bicycle)
        identification()
        word(VocabularyP3Content.cycle, "To travel by bicycle → travels/is travelling by bicycle")
        english("Travelling by bicycle", "The action here is travelling by bicycle. Use it with the shown person and time.")
        word(VocabularyP3Content.now)
        english("Now", "This time expression means the current time and comes last in the shown cycling statement.")
        example("p3-s1-f1")
        english("Bicycle display — reading label", "The next sentence identifies the displayed bicycle.",
            "vocabulary-p3:support:bicycle-display")
        example("p3-s1-f2")
        example("p3-s1-f3")
        order("p3-1.4", "Keep the shown person's name first, followed by the cycling action and the time.")
    }

    private fun second() = build {
        names()
        word(VocabularyP3Content.station)
        countBundles()
        identification()
        word(VocabularyP3Content.walk, "To walk/go on foot → walks/goes on foot")
        english("Walking", "Use this walking action with the complete destination expressions shown here.")
        spatial(VocabularyP3Content.toStation, "destination", "To the station — destination")
        english("A complete destination phrase", "Learn the station destination as one complete phrase. " +
            "Keep its preposition and noun ending as shown; other places need their own examples.")
        spatial(VocabularyP3Content.homeward, "destination", "Home/homeward — destination")
        english("Homeward movement", "The homeward expression is a movement destination, rather than a statement " +
            "that someone is already at home.", "vocabulary-p3:teaching:home-direction",
            VocabularyP3PracticePlanner.contrastKey("p3-home-direction"))
        example("p3-s2-f1")
        example("p3-s2-f2")
        example("p3-s2-f3")
        order("p3-2.4", "Keep the shown person's name first. The station destination stays together as one block.")
    }

    private fun third() = build {
        names()
        word(VocabularyP3Content.live, "To live/reside → lives/resides")
        english("Living in a named city", "Use this residence action with the supplied city phrases. " +
            "Each complete phrase names a static location.")
        surface(VocabularyP3Content.city, "i Aarhus", "In Aarhus — static city location")
        surface(VocabularyP3Content.city, "i Odense", "In Odense — static city location")
        keys(VocabularyP3PracticePlanner.spatialKey("L032", "static-location"))
        spatial(VocabularyP3Content.staticHome, "static-location", "Static at home")
        danish("Works/is working — reviewed form", "arbejder",
            VocabularyP3PracticePlanner.formKey("L027", "arbejder"), "vocabulary-p3:support:arbejder-meaning")
        english("Working at a location", "The supported work statement tells us where someone works; it describes a location.")
        spatial(VocabularyP3Content.homeward, "destination", "Homeward destination")
        english("Compare the two home expressions", "The static expression says where someone is. " +
            "The homeward expression gives a movement destination.",
            VocabularyP3PracticePlanner.contrastKey("p3-home-spatial-contrast"),
            "vocabulary-p3:teaching:home-spatial-contrast")
        word(VocabularyP3Content.library)
        countBundles()
        identification()
        example("p3-s3-f1")
        example("p3-s3-f2")
        example("p3-s3-f3")
        english("City and library boundaries", "Keep the named-city phrases intact in the residence examples. " +
            "The library sentence identifies a place; it supplies no new destination phrase.")
    }

    private fun fourth() = build {
        names()
        word(VocabularyP3Content.bus)
        word(VocabularyP3Content.train)
        word(VocabularyP3Content.ticket)
        countBundles()
        english("The selected train meaning", "The train word here names a transport vehicle.")
        possession()
        identification()
        english("Vehicle A and Vehicle B", "Vehicle A and Vehicle B are English display labels. " +
            "Each identifies a separate displayed vehicle.", "vocabulary-p3:support:vehicle-a", "vocabulary-p3:support:vehicle-b")
        example("p3-s4-f1")
        english("Vehicle A — reading label", "The next sentence identifies Vehicle A.", "vocabulary-p3:support:vehicle-a")
        example("p3-s4-f2")
        english("Vehicle B — reading label", "The next sentence identifies Vehicle B.", "vocabulary-p3:support:vehicle-b")
        example("p3-s4-f3")
        english("Possession and identification", "Anna has the ticket. The labelled displays identify the vehicles; " +
            "they say nothing about her ownership or which vehicle her ticket is for.")
    }

    private fun fifth() = build {
        names()
        english("Car teaching scene", "Nora is travelling home by car.", "vocabulary-p3:teaching-scene:car")
        word(VocabularyP3Content.car)
        countBundles()
        word(VocabularyP3Content.drive, "To travel/drive using a vehicle → travels/drives by vehicle")
        english("The car-selected action", "The supplied car situation selects the vehicular action in this lesson.")
        word(VocabularyP3Content.wait, "To wait → waits/is waiting")
        spatial(VocabularyP3Content.atStation, "static-location", "At the station — static location")
        english("A complete station phrase", "Learn the static station location as one complete phrase. " +
            "Keep its preposition and noun ending as shown when describing the waiting situation.")
        spatial(VocabularyP3Content.homeward, "destination", "Home/homeward — destination")
        spatial(VocabularyP3Content.toStation, "destination", "To the station — destination")
        danish("Travels by bicycle — reviewed present form", "cykler",
            VocabularyP3PracticePlanner.formKey("L065", "cykler"), VocabularyP3PracticePlanner.meaningKey(VocabularyP3Content.cycle))
        english("Compare the two travel actions", "One action is selected by the car situation; " +
            "the reviewed bicycle action describes travelling by bicycle.")
        english("Compare the station expressions", "The destination expression says where someone is going. " +
            "The static expression says where someone waits. Both remain complete phrases.",
            VocabularyP3PracticePlanner.contrastKey("p3-station-spatial-contrast"),
            "vocabulary-p3:teaching:station-spatial-contrast")
        example("p3-s5-f1")
        example("p3-s5-f2", "Nora travels/drives home by vehicle.")
        english("Car situation and translation", "The English car situation identifies the vehicle. " +
            "The Danish statement itself uses the selected vehicular action without naming a vehicle phrase.")
    }

    private fun sixth() = build {
        names()
        english("Rail teaching scene", "Anna is travelling by train and changes trains/connections in Aarhus.",
            "vocabulary-p3:teaching-scene:rail")
        word(VocabularyP3Content.train)
        surface(VocabularyP3Content.city, "i Aarhus", "In Aarhus — reviewed city phrase")
        word(VocabularyP3Content.change, "To change trains/connections → changes trains/connections")
        english("The rail-selected meaning", "The English train situation supplies the travel context for the change. " +
            "Use the complete change-in-the-city example shown here.")
        word(VocabularyP3Content.pharmacy)
        countBundles()
        spatial(VocabularyP3Content.toPharmacy, "destination", "To the pharmacy — destination")
        english("A complete pharmacy phrase", "Learn the pharmacy destination as one complete phrase. " +
            "Keep its preposition and noun ending as shown; it supplies no general rule for other places.")
        danish("Walks/goes on foot — reviewed present form", "går",
            VocabularyP3PracticePlanner.formKey("L067", "går"), VocabularyP3PracticePlanner.meaningKey(VocabularyP3Content.walk))
        spatial(VocabularyP3Content.homeward, "destination", "Home/homeward — reviewed destination")
        danish("Waits at the station — reviewed whole phrase", "venter på stationen",
            VocabularyP3PracticePlanner.formKey("L075", "venter"), VocabularyP3PracticePlanner.formKey("L076", "på stationen"),
            "vocabulary-p3:support:waiting-station-meaning", VocabularyP3PracticePlanner.spatialKey("L076", "static-location"))
        english("Waiting phrase", "The reviewed whole phrase describes waiting at a static station location.")
        danish("I — supplied subject word", "jeg", "vocabulary-p3:support:jeg")
        example("p3-s6-f1")
        example("p3-s6-f2")
        example("p3-s6-f3")
        english("Separate facts", "These are separate facts. Their display order does not state an event chronology.")
        danish("Supplied first-person example", "Jeg går til apoteket.",
            "vocabulary-p3:teaching:supplied-pharmacy-subject", VocabularyP3PracticePlanner.orderKey("p3-6.4"))
        english("Supplied first-person translation", "I walk to the pharmacy.")
        order("p3-6.4", "Only the supplied subject changes in the final activity. " +
            "Keep the walking action and complete pharmacy destination as shown.")
    }

    private fun build(body: Builder.() -> Unit) = Builder().apply(body).material()

    private class Builder {
        private val sections = mutableListOf<IntroductionSection>()
        private val exposureKeys = mutableListOf<String>()

        fun english(title: String, text: String, vararg keys: String) {
            sections += IntroductionSection(title, text, "en")
            exposureKeys += keys
        }

        fun danish(title: String, text: String, vararg keys: String) {
            sections += IntroductionSection(title, text, "da")
            exposureKeys += keys
        }

        fun keys(vararg keys: String) { exposureKeys += keys }

        fun names() = english("People in the examples", "Anna and Nora are names.",
            "vocabulary-p3:support:Anna", "vocabulary-p3:support:Nora")

        fun possession() = danish("Has — reviewed possession form", "har",
            VocabularyP3PracticePlanner.formKey("L009", "har"), "vocabulary-p3:support:har-possession")

        fun identification() = danish("This is — supplied identification", "Det er",
            "vocabulary-p3:support:det-er")

        fun countBundles() = english("Keep the article with the noun", "Learn each count noun together with its " +
            "indefinite article. Keep that complete article-and-noun unit together in these examples.")

        fun word(lexeme: VocabularyP3Lexeme, meaning: String = lexeme.gloss) {
            val infinitive = lexeme.forms.infinitive
            val present = lexeme.forms.present
            val display = if (infinitive != null && present != null) "at $infinitive → $present" else lexeme.display
            danish("Meaning — $meaning", display,
                VocabularyP3PracticePlanner.meaningKey(lexeme),
                VocabularyP3PracticePlanner.mappingKey(lexeme.entryId, lexeme.senseId))
            if (infinitive != null && present != null) {
                keys(VocabularyP3PracticePlanner.formKey(lexeme.entryId, infinitive),
                    VocabularyP3PracticePlanner.formKey(lexeme.entryId, present))
            } else if (lexeme.forms.bundle != null) {
                keys(VocabularyP3PracticePlanner.bundleKey(lexeme.entryId, lexeme.display))
            } else keys(VocabularyP3PracticePlanner.formKey(lexeme.entryId, lexeme.display))
        }

        fun surface(lexeme: VocabularyP3Lexeme, display: String, meaning: String) {
            danish("Meaning — $meaning", display, VocabularyP3PracticePlanner.formKey(lexeme.entryId, display),
                VocabularyP3PracticePlanner.meaningKey(lexeme), VocabularyP3PracticePlanner.mappingKey(lexeme.entryId, lexeme.senseId))
        }

        fun spatial(lexeme: VocabularyP3Lexeme, classification: String, meaning: String) {
            word(lexeme, meaning)
            keys(VocabularyP3PracticePlanner.spatialKey(lexeme.entryId, classification))
        }

        fun example(id: String, translation: String? = null) {
            val fact = VocabularyP3Content.tasks.flatMap(VocabularyP3Task::facts).first { it.id == id }
            sections += IntroductionSection("Reviewed statement", fact.sentence, "da")
            exposureKeys += fact.exposureKeys
            exposureKeys += VocabularyP3PracticePlanner.factKey(id)
            english("Statement translation", translation ?: fact.translation)
        }

        fun order(taskId: String, text: String) = english("Use the supplied subject first", text,
            VocabularyP3PracticePlanner.ORDER_SUPPORT_KEY, VocabularyP3PracticePlanner.orderKey(taskId))

        fun material() = Material(sections.toList(), exposureKeys.distinct())
    }
}
