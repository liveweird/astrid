@file:Suppress("MagicNumber") // Literal published revision pairs are the compatibility allowlist.

package app.astrid.engine

data class VocabularyTeachingWitness(
    val focusId: String,
    val planRevision: String,
    val contentRevision: String,
    val generatorRevision: String,
    val introductionExposureKeys: List<String>,
)

data class VocabularyTeachingClaim(
    val entryId: String,
    val senseId: String,
    val meaningRecorded: Boolean,
    val formIds: List<String>,
)

object VocabularyTeachingMapper {
    fun claims(witnesses: List<VocabularyTeachingWitness>): List<VocabularyTeachingClaim> {
        val catalog = VocabularyLibrary.catalog()
        val forms = catalog.items.flatMap { entry ->
            entry.senses.flatMap { sense ->
                sense.activeForms.map { it.text to Evidence(entry.entryId, sense.senseId, formId = it.formId) }
            }
        }
        val formIndex = forms.groupBy({ it.first }, { it.second })
        val evidence = witnesses.flatMap { witness ->
            val plan = PLANS.singleOrNull { it.matches(witness) } ?: return@flatMap emptyList()
            witness.introductionExposureKeys.flatMap { key -> plan.keys[key].orEmpty() }
        }
        return evidence.groupBy { it.entryId to it.senseId }.map { (identity, fragments) ->
            val (entryId, senseId) = identity
            val allowedFormIds = catalog.items.single { it.entryId == entryId }.senses.single { it.senseId == senseId }
                .activeForms.map(VocabularyLibraryForm::formId).toSet()
            val directIds = fragments.mapNotNull(Evidence::formId)
            val textIds = fragments.flatMap { fragment ->
                fragment.formText?.let { text -> formIndex[text].orEmpty() }.orEmpty()
                    .filter { it.entryId == entryId && it.senseId == senseId }
                    .mapNotNull(Evidence::formId)
            }
            VocabularyTeachingClaim(
                entryId,
                senseId,
                fragments.any(Evidence::meaning),
                (directIds + textIds).filter(allowedFormIds::contains).distinct().sorted(),
            )
        }.filter { it.meaningRecorded || it.formIds.isNotEmpty() }
            .sortedWith(compareBy(VocabularyTeachingClaim::entryId, VocabularyTeachingClaim::senseId))
    }

    private data class Evidence(
        val entryId: String,
        val senseId: String,
        val meaning: Boolean = false,
        val formText: String? = null,
        val formId: String? = null,
    )

    private data class Plan(
        val focusId: String,
        val planRevision: String,
        val contentRevision: String,
        val generatorRevision: String,
        val keys: Map<String, List<Evidence>>,
    ) {
        fun matches(witness: VocabularyTeachingWitness) =
            witness.focusId == focusId && witness.planRevision == planRevision &&
                witness.contentRevision == contentRevision && witness.generatorRevision == generatorRevision
    }

    private class KeyBuilder {
        val values = mutableMapOf<String, MutableList<Evidence>>()

        fun key(
            key: String,
            entryId: String,
            senseId: String = "$entryId-S01",
            meaning: Boolean = false,
            form: String? = null,
            formId: String? = null,
        ) {
            values.getOrPut(key, ::mutableListOf) += Evidence(entryId, senseId, meaning, form, formId)
        }

        fun built() = values.mapValues { it.value.toList() }
    }

    private fun plan(
        focus: String,
        revision: String,
        content: String,
        generator: String,
        body: KeyBuilder.() -> Unit,
    ) = Plan(focus, revision, content, generator, KeyBuilder().apply(body).built())

    private fun modulePlan(focus: String, revision: String, content: Int, body: KeyBuilder.() -> Unit) =
        plan(focus, revision, "pilot-$content", "generator-$content", body)

    private val PLANS = buildList {
        add(modulePlan("module-one-singular", "module-one-singular-1", 8) {
            listOf("L001" to "jeg", "L002" to "du", "L003" to "han", "L004" to "hun").forEach { (id, form) ->
                key("module-one:subject-form:$id", id, meaning = true, form = form)
            }
            key("module-one:verb-present:L010", "L010", form = "er")
        })
        add(modulePlan("module-one-groups", "module-one-groups-1", 8) {
            listOf("L001" to "jeg", "L002" to "du", "L005" to "vi", "L006" to "I", "L007" to "de").forEach { (id, form) ->
                key("module-one:subject-form:$id", id, meaning = true, form = form)
            }
            key("module-one:verb-present:L010", "L010", form = "er")
            key("module-one:verb-present:L013", "L013", meaning = true, form = "sover")
        })
        add(modulePlan("module-one-er", "module-one-er-1", 8) {
            key("module-one:subject-form:L001", "L001", meaning = true, form = "jeg")
            key("module-one:verb-infinitive:L010", "L010", meaning = true, form = "være")
            key("module-one:verb-present:L010", "L010", meaning = true, form = "er")
            key("module-one:identifying-form:L008", "L008", "L008-S01", meaning = true, form = "Det er")
            listOf("L014" to "en bog", "L018" to "et brev", "L025" to "en lærer").forEach { (id, form) ->
                key("module-one:headword:$id-S01", id, meaning = true)
                key("module-one:article:$id", id, form = form)
            }
            key("module-one:article:L014", "L022", form = "en")
            key("module-one:article:L018", "L023", form = "et")
            key("module-one:article:L025", "L022", form = "en")
        })
        add(modulePlan("module-one-things", "module-one-things-1", 9) {
            key("module-one:verb-present:L010", "L010", form = "er")
            key("module-one:reference-sense:L026-S01", "L026", meaning = true)
            key("module-one:visible-form:den", "L026", form = "den")
            key("module-one:reference-sense:L008-S02", "L008", "L008-S02", meaning = true)
            key("module-one:visible-form:det", "L008", "L008-S02", form = "det")
            key("module-one:identifying-form:L008", "L008", "L008-S01", meaning = true, form = "Det er")
            listOf("L014" to "en bog", "L018" to "et brev").forEach { (id, form) ->
                key("module-one:headword:$id-S01", id, meaning = true)
                key("module-one:article:$id", id, form = form)
            }
            key("module-one:article:L014", "L022", form = "en")
            key("module-one:article:L018", "L023", form = "et")
        })
        add(modulePlan("module-one-present", "module-one-present-1", 10) {
            listOf(Triple("L027", "arbejde", "arbejder"), Triple("L011", "læse", "læser"), Triple("L013", "sove", "sover"))
                .forEach { (id, infinitive, present) ->
                    key("module-one:verb-infinitive:$id", id, meaning = true, form = infinitive)
                    key("module-one:verb-present:$id", id, meaning = true, form = present)
                }
        })
        add(modulePlan("module-one-order", "module-one-order-1", 10) {
            listOf("L027" to "arbejder", "L011" to "læser", "L013" to "sover").forEach { (id, form) ->
                key("module-one:verb-present:$id", id, meaning = true, form = form)
            }
            listOf("L002" to "du", "L003" to "han", "L007" to "de", "L018" to "et brev").forEach { (id, form) ->
                key("module-one:supplied-component:$id", id, form = form)
            }
            key("module-one:supplied-component:L023", "L023", form = "et")
        })
        add(modulePlan("module-one-possession", "module-one-possession-1", 11) {
            key("module-one:verb-infinitive:L009", "L009", meaning = true, form = "have")
            key("module-one:verb-present:L009", "L009", meaning = true, form = "har")
            key("module-one:supplied-component:L001", "L001", form = "jeg")
            key("module-one:supplied-component:L014", "L014", form = "en bog")
            key("module-one:supplied-component:L022", "L022", form = "en")
        })
        add(modulePlan("module-one-reading-words", "module-one-reading-words-1", 5) {
            listOf("L024" to "et magasin", "L014" to "en bog", "L015" to "en avis", "L018" to "et brev").forEach { (id, form) ->
                key("module-one:reading-meaning:$id-S01", id, meaning = true)
                key("module-one:reading-noun-bundle:$id-S01", id, form = form)
            }
        })
        add(modulePlan("module-one-personal", "module-one-personal-1", 12) {
            listOf(Triple("L028", "hedde", "hedder"), Triple("L029", "bo", "bor"), Triple("L030", "tale", "taler"))
                .forEach { (id, infinitive, present) ->
                    key("module-one:verb-infinitive:$id", id, meaning = true, form = infinitive)
                    key("module-one:verb-present:$id", id, meaning = true, form = present)
                }
            key("module-one:supplied-component:L031", "L031", meaning = true, form = "dansk")
            key("module-one:supplied-component:L032", "L032", meaning = true, form = "i Odense")
        })
        add(modulePlan("module-one-ability", "module-one-ability-1", 13) {
            key("module-one:verb-infinitive:L033", "L033", meaning = true, formId = "L033-S01-F1")
            key("module-one:verb-present:L033", "L033", meaning = true, formId = "L033-S01-F2")
            key("module-one:at-infinitive:L011", "L011", meaning = true, form = "læse")
            key("module-one:bare-infinitive:L011", "L011", meaning = true, form = "læse")
            key("module-one:verb-present:L011", "L011", form = "læser")
            key("module-one:supplied-component:L023", "L023", form = "et")
            key("module-one:supplied-component:L018", "L018", form = "et brev")
            key("module-one:supplied-component:L031", "L031", form = "dansk")
        })
        listOf(
            Triple("module-one-volition", "L034", "ville" to "vil"),
            Triple("module-one-obligation", "L035", "skulle" to "skal"),
            Triple("module-one-permission", "L036", "måtte" to "må"),
        ).forEach { (focus, id, forms) ->
            add(modulePlan(focus, "$focus-1", 14) {
                key("module-one:modal-meaning:$id:$id-S01", id, meaning = true)
                key("module-one:verb-infinitive:$id", id, form = forms.first)
                key("module-one:verb-present:$id", id, form = forms.second)
                key("module-one:verb-infinitive:L011", "L011", meaning = true, form = "læse")
                key("module-one:bare-infinitive:L011", "L011", meaning = true, form = "læse")
                val components = when (id) {
                    "L034" -> listOf("L001" to "jeg", "L022" to "en", "L014" to "en bog")
                    "L035" -> listOf("L002" to "du", "L023" to "et", "L018" to "et brev")
                    else -> listOf("L003" to "han", "L022" to "en", "L015" to "en avis")
                }
                components.forEach { (componentId, form) ->
                    key("module-one:supplied-component:$componentId", componentId, form = form)
                }
            })
        }
        add(modulePlan("module-one-circumstances", "module-one-circumstances-1", 15) {
            key("module-one:meaning:L037:L037-S01", "L037", meaning = true, form = "i dag")
            key("module-one:meaning:L038:L038-S01", "L038", meaning = true, form = "nu")
            key("module-one:supplied-component:L032", "L032", form = "i Aarhus")
        })
        add(modulePlan("module-one-questions", "module-one-questions-1", 16) {
            key("module-one:meaning:L039:L039-S01", "L039", meaning = true, form = "ja")
            key("module-one:meaning:L040:L040-S01", "L040", meaning = true, form = "nej")
        })
        addP1Plans()
        addP2Plans()
        addP3Plans()
    }

    private fun MutableList<Plan>.addP1Plans() {
        val lessons = listOf(
            listOf("L027", "L011", "L014", "L037"), listOf("L015", "L018", "L041", "L042"),
            listOf("L043", "L044", "L031", "L038"), listOf("L045", "L046", "L047"),
            listOf("L032", "L048", "L049"),
        )
        lessons.forEachIndexed { index, ids ->
            listOf(17 to 1, 18 to 2).forEach { (pair, revision) ->
                add(
                    plan(
                        "vocabulary-p1-s${index + 1}", "vocabulary-p1-s${index + 1}-$revision",
                        "pilot-$pair", "generator-$pair",
                    ) {
                        ids.forEach { addCatalogLexeme("vocabulary-p1", it) }
                        if (index == 1) {
                            key("vocabulary-p1:form:L011:læser", "L011", form = "læser")
                            key("vocabulary-p1:bundle:L014:en bog", "L014", form = "en bog")
                            key("vocabulary-p1:form:L009:har", "L009", form = "har")
                        }
                        if (index == 2) {
                            key("vocabulary-p1:form:L011:læser", "L011", form = "læser")
                            key("vocabulary-p1:bundle:L014:en bog", "L014", form = "en bog")
                        }
                        if (index <= 2) {
                            val referent = if (index == 0) "p1:Anna" else "p1:Nora"
                            key("vocabulary-p1:support:referent:$referent:hun", "L004", meaning = true, form = "hun")
                        }
                        if (index == 4) {
                            key("vocabulary-p1:form:L001:jeg", "L001", form = "jeg")
                            key("vocabulary-p1:form:L027:arbejder", "L027", form = "arbejder")
                            key("vocabulary-p1:form:L043:studerer", "L043", form = "studerer")
                        }
                    },
                )
            }
        }
    }

    private fun MutableList<Plan>.addP2Plans() {
        val lessons = listOf(
            listOf("L012", "L021", "L050", "L051"), listOf("L045", "L046", "L047", "L052", "L050", "L021"),
            listOf("L053", "L054", "L055", "L056", "L012", "L050"), listOf("L057", "L058", "L059", "L055", "L012", "L045"),
            listOf("L060", "L061", "L034", "L058", "L055", "L053", "L054"),
            listOf("L009", "L062", "L063", "L046", "L047", "L060", "L034", "L061"),
        )
        lessons.forEachIndexed { index, ids -> add(plan(
            "vocabulary-p2-s${index + 1}", "vocabulary-p2-s${index + 1}-1", "pilot-19", "generator-19",
        ) {
            ids.forEach { id -> addCatalogLexeme("vocabulary-p2", id, if (id == "L009") "L009-S02" else "$id-S01") }
            if (index in setOf(0, 1, 3, 5)) {
                key("vocabulary-p2:referent:p2:speaker:Jeg", "L001", meaning = true, form = "jeg")
            }
            if (index == 5) {
                key("vocabulary-p2:support:possession-har", "L009", "L009-S01", meaning = true, form = "har")
            }
        }) }
    }

    private fun MutableList<Plan>.addP3Plans() {
        val lessons = listOf(
            listOf("L016", "L064", "L065", "L038"), listOf("L066", "L067", "L068", "L069"),
            listOf("L029", "L032", "L048", "L070"), listOf("L071", "L072", "L073"),
            listOf("L016", "L074", "L075", "L076", "L069", "L068", "L065"),
            listOf("L032", "L072", "L077", "L078", "L079", "L067", "L069", "L075", "L076"),
        )
        lessons.forEachIndexed { index, ids -> add(plan(
            "vocabulary-p3-s${index + 1}", "vocabulary-p3-s${index + 1}-1", "pilot-20", "generator-20",
        ) {
            ids.forEach { id -> addP3Lexeme(index, id) }
            addP3Support(index)
        }) }
    }

    private fun KeyBuilder.addP3Lexeme(index: Int, id: String) {
        val finiteOnly = (index == 2 && id == "L027") || (index == 4 && id == "L065") ||
            (index == 5 && id in setOf("L067", "L075"))
        when {
            finiteOnly -> addFiniteP3(id)
            index == 5 && id == "L032" -> addP3Aarhus()
            else -> addCatalogLexeme("vocabulary-p3", id)
        }
    }

    private fun KeyBuilder.addP3Support(index: Int) {
        if (index == 2) {
            addFiniteP3("L027")
            key("vocabulary-p3:support:arbejder-meaning", "L027", meaning = true)
        }
        if (index in setOf(0, 3)) key("vocabulary-p3:form:L009:har", "L009", meaning = true, form = "har")
        if (index <= 3) key("vocabulary-p3:support:det-er", "L008", "L008-S01", meaning = true, form = "Det er")
        if (index == 5) key("vocabulary-p3:support:jeg", "L001", meaning = true, form = "jeg")
    }

    private fun KeyBuilder.addFiniteP3(id: String) {
        val present = VocabularyLibrary.catalog().items.single { it.entryId == id }.senses.single().activeForms
            .single { it.label == "finite-present" }.text
        key("vocabulary-p3:meaning:$id:$id-S01", id, meaning = true)
        key("vocabulary-p3:form:$id:$present", id, form = present)
    }

    private fun KeyBuilder.addP3Aarhus() {
        key("vocabulary-p3:meaning:L032:L032-S01", "L032", meaning = true)
        key("vocabulary-p3:mapping:L032:L032-S01", "L032", meaning = true)
        key("vocabulary-p3:form:L032:i Aarhus", "L032", form = "i Aarhus")
    }

    private fun KeyBuilder.addCatalogLexeme(prefix: String, id: String, senseId: String = "$id-S01") {
        val sense = VocabularyLibrary.catalog().items.single { it.entryId == id }.senses.single { it.senseId == senseId }
        val effectivePrefix = when {
            prefix == "vocabulary-p2" && id in setOf("L045", "L046", "L047") -> "vocabulary-p1"
            else -> prefix
        }
        if (prefix == "vocabulary-p2" && id == "L034") {
            key("module-one:modal-meaning:L034:L034-S01", id, meaning = true)
            key("module-one:verb-infinitive:L034", id, form = "ville")
            key("module-one:verb-present:L034", id, form = "vil")
            return
        }
        key("$effectivePrefix:meaning:$id:$senseId", id, senseId, meaning = true)
        key("$effectivePrefix:mapping:$id:$senseId", id, senseId, meaning = true)
        sense.activeForms.forEach { form ->
            val category = if (form.label in setOf("count-bundle", "indefinite-singular")) "bundle" else "form"
            key("$effectivePrefix:$category:$id:${form.text}", id, senseId, form = form.text)
        }
    }
}
