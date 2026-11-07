# Danish Sentence and Phrase Construction Catalogue

## Purpose and scope

This catalogue gives Astrid a bounded structural inventory for English-speaking complete beginners learning everyday written Danish. It separates phrase structure from clause structure, records the order of whole constituents rather than individual words, and makes verb meaning and complement choice part of generation safety. It is a design reference, not a complete formal grammar, parser specification, or claim that every Danish variant has been enumerated.

Constructions C001–C003 are the proposed first engine pilot. Their initial generators are deliberately limited to short positive main-clause declaratives. The remaining entries map later coverage; they are not yet authorizations to generate those forms. All entries are **described and source-checked**, but **not yet reviewed by a Danish-language teaching specialist**. None is engine-supported at the time of writing.

## How to read the patterns

A **phrase** is a word or group of words built around a head, such as `en ny bog` or `på bordet`. A **finite clause** contains a finite verb and normally a subject; an imperative normally leaves its understood second-person subject unspoken. A phrase can fill one clause slot, and an entire subordinate clause can do the same. Sproget.dk likewise notes that one constituent may consist of a word, a group of words, or a clause, and that subordinate clauses can function as clause constituents. [Sproget.dk on clause constituents](https://sproget.dk/typiske-problemer/saetningsled/) and [subordinate clauses](https://sproget.dk/typiske-problemer/ledsaetninger/)

The notation describes **constituents**, not a sequence of tokens:

| Symbol | Meaning |
| --- | --- |
| `F` | Front field: one constituent before the finite verb in a main declarative |
| `Vf` | Finite verb, carrying tense or imperative form |
| `S` | Syntactic subject |
| `SA` | Sentence adverbial, for example `ikke`, `aldrig`, or `måske` |
| `Vi` | Non-finite verb or the remaining non-finite part of a compound verb |
| `O`, `IO` | Direct object and indirect object |
| `PRED` | Subject or object predicative complement |
| `PP` | Prepositional phrase |
| `A` | Other adverbial, such as time, place, manner, or reason |
| `SUB` | Subordinator or another subordinate-clause introducer |
| `(x)` | Optional slot; `/` separates alternatives |
| `REST` | Remaining frame-licensed complements after fronting/questioning; may be empty only when the frame allows it |
| `A*` | Zero or more separately ordered, reviewed circumstance adverbials |

The useful beginner main-clause model is:

`F | Vf | (S) | (SA) | (Vi) | REST | A*`

The postverbal `S` is required when a non-subject fills `F`; it is absent there when the subject itself fills `F`. `REST` contains the complements still realized after any fronted or questioned role has been filled. An empty block does not license dropping a complement required by the selected verb frame. The schemata show available positions; optional slots do not make arbitrary combinations or adverbial orders acceptable.

The finite verb is therefore in the **second constituent position**, not necessarily the second word. In `Den lille dreng læser bogen`, all three words `Den lille dreng` form one subject constituent, followed by `læser`. In `I den lille butik køber vi brød`, the four-word PP `I den lille butik` is one fronted constituent, followed by `køber`. Danish research describes this main-clause option as a front field before the finite verb and contrasts it with the usual subordinate pattern. [University of Copenhagen research on Danish word order](https://researchprofiles.ku.dk/en/publications/ordstilling-i-leds%C3%A6tninger-i-moderne-dansk-talesprog/)

The default subordinate-clause model used here is:

`SUB | S | (SA) | Vf | (Vi) | REST | A*`

This captures the beginner contrast `Hun kommer ikke` versus `fordi hun ikke kommer`. It is not universal: especially in spoken Danish, some subordinate clauses occur with main-clause-like verb-before-adverb order. Astrid should recognize that as real variation later, but should not generate it in the initial learner model. [Sproget.dk on the ikke test](https://sproget.dk/typiske-problemer/ledsaetninger/) and [University of Copenhagen study of subordinate-clause V2](https://researchprofiles.ku.dk/en/publications/foregrounding-of-subordinate-clauses-by-word-order-psycholinguist/)

## Roles, forms, and lexical licensing

Syntactic functions and semantic roles must remain separate. A subject is not always an agent: `Maja har en cykel` has a possessor subject, `Glasset faldt` has a changing entity as subject, and `Bogen blev læst` has the read item as subject. A predicative complement describes or identifies a subject or object; it is not a direct object. Sproget.dk distinguishes subjects, direct and indirect objects, predicatives, and adverbials on this basis. [Sproget.dk on clause constituents](https://sproget.dk/typiske-problemer/saetningsled/)

A pattern licenses positions, not arbitrary substitutions. Each generated clause also needs a **word sense and valency frame**:

- `sove` in its ordinary sense licenses a subject but no direct object: `Barnet sover`.
- `læse` can license a readable direct object: `Hun læser en avis`; a random concrete noun is not automatically natural.
- `bo` normally needs or readily takes a location expression: `Vi bor i Aarhus`.
- `give` licenses a giver, a recipient, and something transferred: `Jeg giver ham nøglen`.
- `være` can link a subject to an adjective, identifying noun phrase, location, or other complement; these complements are not interchangeable without changing meaning.

Naturalness also depends on definiteness, animacy, countability, register, information structure, idiom, and real-world plausibility. A structurally well-formed result can still be semantically odd. The generator must use reviewed combinations, not Cartesian products of words that share a word class.

## Proposed pilot constructions

### C001 Subject-first intransitive present main clause

- **Pattern:** `S | Vf`
- **Function:** State a simple action or condition involving one participant.
- **Dependencies:** G001 orthography; G002 word roles; G003 subject pronouns; G004 finite present verbs; G005 subject-first statements. G006 noun gender and indefinite singular is needed when a noun phrase replaces a pronoun.
- **Pilot boundary:** positive present tense; a first-, second-, or third-person subject pronoun; one reviewed intransitive verb sense; no object, negation, modifier, or fronting.
- **Examples:** `Jeg sover.` — “I am sleeping / I sleep.” `De sover.` — “They are sleeping / They sleep.”
- **Constraints and variation:** Danish present tense can receive an ongoing or habitual English translation according to context. Do not attach a direct object merely because another sense of the same spelling allows one.
- **Status:** implemented in the private engine pilot; described and source-checked; not specialist-reviewed. Broader variants remain unsupported.

### C002 Subject-first transitive present main clause

- **Pattern:** `S | Vf | O`
- **Function:** Present a simple two-participant event or relation.
- **Dependencies:** G001–G006, especially G006 for an indefinite singular noun object.
- **Pilot boundary:** positive present tense; subject pronoun; reviewed transitive verb sense; one licensed indefinite singular count-noun object.
- **Examples:** `Jeg har en bog.` — “I have a book.” `Hun læser en avis.` — “She reads / is reading a newspaper.” `Vi spiser et æble.` — “We eat / are eating an apple.”
- **Constraints and variation:** Object selection is sense-specific: `læse` needs readable content in this frame, and `spise` needs something edible. `have` expresses possession here, not an auxiliary use. Pronouns and definite or plural objects are later variations, not pilot substitutions.
- **Status:** implemented in the private engine pilot; described and source-checked; not specialist-reviewed. Broader variants remain unsupported.

### C003 Subject-first identifying copula with det

- **Pattern:** `S(det) | Vf(er) | PRED(NP-indefinite)`
- **Function:** Identify a visible or textually introduced item.
- **Dependencies:** G001–G006.
- **Pilot boundary:** fixed referential `det`, present `er`, and a reviewed indefinite singular count-noun complement. Use with context that makes an item available for identification.
- **Examples:** `Det er en bog.` — “It/That is a book.” `Det er et hus.` — “It/That is a house.”
- **Constraints and variation:** `en bog` and `et hus` are subject predicatives, not objects. The complement demonstrates noun gender. Do not generalize this pilot to arbitrary human or inanimate subjects, bare profession nouns, adjective complements, existential `der er`, or English weather/time “it.” Those belong to other frames.
- **Status:** implemented in the private engine pilot; described and source-checked; not specialist-reviewed. Broader variants remain unsupported.

## Main declaratives and complement frames

### C004 Copular clause with a descriptive or identifying complement

- **Pattern:** `S | Vf(copula) | PRED`
- **Meaning:** Describe or identify the subject.
- **Examples:** `Kaffen er varm.` — “The coffee is hot.” `Nora er læge.` — “Nora is a doctor.”
- **Constraints:** Predicative adjective agreement and the presence or absence of an article in profession/role nouns require separate lexical and grammar rules. Contrast the identifying pilot C003 with the bare profession noun here. `PRED` is not an object.
- **Status:** mapped for later; described and source-checked; unsupported.

### C005 Ditransitive clause

- **Pattern:** `S | Vf | IO | O`
- **Meaning:** Transfer something to or for a recipient.
- **Example:** `Sara giver barnet en banan.` — “Sara gives the child a banana.”
- **Constraints:** Only licensed transfer senses take both objects. Some meanings prefer a PP alternative (`til barnet`), and alternation is lexical rather than automatic.
- **Status:** mapped for later; described and source-checked; unsupported.

### C006 Verb with a required or selected PP complement

- **Pattern:** `S | Vf | PP`
- **Meaning:** Complete a verb meaning that selects a preposition or location phrase.
- **Examples:** `Vi bor i Odense.` — “We live in Odense.” `Hun venter på bussen.` — “She is waiting for the bus.”
- **Constraints:** The preposition belongs to the verb-sense frame (`vente på`); substituting another preposition can change meaning or become unidiomatic. Distinguish selected complements from freely added place or time adverbials.
- **Status:** mapped for later; described and source-checked; unsupported.

### C007 Subject-first main clause with a sentence or circumstance adverbial

- **Pattern:** `S | Vf | (SA) | (Vi) | REST | A*`
- **Meaning:** Add negation, stance, frequency, time, place, or manner without fronting it.
- **Examples:** `Hun kommer måske i morgen.` — “She may come tomorrow.” `Vi spiser hjemme i dag.` — “We are eating at home today.”
- **Constraints:** Adverbials do not all share one freely interchangeable position. Sentence adverbs such as `måske` follow the finite verb in this neutral main-clause pattern; circumstance adverbials have several information-structural options.
- **Status:** mapped for later; described and source-checked; unsupported.

### C008 Fronted main declarative with V2

- **Pattern:** `F(non-subject) | Vf | S | (SA) | (Vi) | REST | A*`
- **Meaning:** Put a time, place, object, or other constituent in the front field while retaining main-clause V2.
- **Examples:** `I dag læser hun avisen.` — “Today she is reading the newspaper.” `Den bog kender jeg.` — “That book I know.”
- **Constraints:** Exactly one constituent occupies `F` in the basic model. A fronted object or predicative already fills its complement role and must not be duplicated in `REST`. The subject follows `Vf` when another constituent is fronted. Fronting changes discourse emphasis and is not always pragmatically neutral; object fronting needs a context where the object is topical or contrastive.
- **Status:** mapped for later; described and source-checked; unsupported.

### C009 Negated main declarative

- **Pattern:** `F | Vf | (S) | ikke | (Vi) | REST | A*`
- **Meaning:** Negate the main-clause proposition.
- **Examples:** `Jeg drikker ikke kaffe.` — “I do not drink coffee.” `I dag arbejder hun ikke hjemme.` — “Today she is not working at home.”
- **Constraints:** With a simple subject-first clause, `ikke` follows the finite verb. With compound verbs it normally precedes the non-finite part: `Hun har ikke spist.` Scope-sensitive or contrastive uses of `ikke` require additional rules.
- **Status:** mapped for later; described and source-checked; unsupported.

## Questions and commands

### C010 Direct yes-no question

- **Pattern:** `Vf | S | (SA) | (Vi) | REST | A* ?`
- **Meaning:** Ask whether a proposition is true.
- **Examples:** `Bor du i København?` — “Do you live in Copenhagen?” `Har hun ikke spist?` — “Hasn’t she eaten?”
- **Constraints:** The ordinary neutral pattern has no front-field constituent before `Vf`. Danish also permits intonation questions with declarative order and questions with `mon`; those carry different pragmatic signals and are not beginner generator equivalents. [Sproget.dk on yes-no question order](https://sproget.dk/sprogviden/spoergsmaal-og-svar/maal-og-maele/ledstilling-i-ja-nej-spoergsmaal/)
- **Status:** mapped for later; described and source-checked; unsupported.

### C011 Direct non-subject wh-question

- **Pattern:** `WH(non-subject) | Vf | S | (SA) | (Vi) | REST | A* ?`
- **Meaning:** Ask for an object, place, time, manner, or reason.
- **Examples:** `Hvad læser du?` — “What are you reading?” `Hvor bor din søster?` — “Where does your sister live?”
- **Constraints:** Because `WH` is the fronted constituent, the subject normally follows `Vf`. The wh-form must match the missing role and any selected preposition; everyday Danish often leaves the preposition later (`Hvem taler du med?`).
- **Status:** mapped for later; described and source-checked; unsupported.

### C012 Direct subject wh-question

- **Pattern:** `WH(subject) | Vf | (SA) | (Vi) | REST | A* ?`
- **Meaning:** Ask who or what fills the subject role.
- **Examples:** `Hvem kommer i morgen?` — “Who is coming tomorrow?” `Hvad ligger på bordet?` — “What is lying on the table?”
- **Constraints:** Do not insert another subject after `Vf` in the subject-question analysis. Surface form alone does not always distinguish C011 from C012: `Hvem ser Anna?` can mean either “Who sees Anna?” or “Who does Anna see?” Generation needs context or a less ambiguous lexical choice.
- **Status:** mapped for later; described and source-checked; unsupported.

### C013 Imperative clause

- **Pattern:** `Vf(imperative) | REST | A*`
- **Meaning:** Give an instruction, request, invitation, or warning to one or more addressees.
- **Examples:** `Åbn døren.` — “Open the door.” `Kom indenfor.` — “Come in.”
- **Constraints:** The understood subject is second person and is normally omitted. The form and its particles/complements are lexical. Negated commands (`Gå ikke!`) and polite framing need separate pragmatic coverage. Sproget.dk explicitly treats the subject as omitted but understood in imperatives. [Sproget.dk on sentences](https://sproget.dk/typiske-problemer/saetninger/)
- **Status:** mapped for later; described and source-checked; unsupported.

## Compound verbs, coordination, and infinitives

### C014 Modal plus bare infinitive

- **Pattern:** `S | Vf(modal) | (SA) | Vi(bare infinitive) | REST | A*`
- **Meaning:** Express ability, obligation, intention, permission, or related modal meanings.
- **Examples:** `Jeg kan svømme.` — “I can swim.” `Vi skal købe mælk.` — “We must/are going to buy milk.”
- **Constraints:** The lexical verb normally has no `at` after a modal. Danish modals are polysemous: `skal` may convey obligation, plan, prediction, or reported information, so an English equivalent cannot be assigned from form alone.
- **Status:** mapped for later; described and source-checked; unsupported.

### C015 Perfect compound verb

- **Pattern:** `S | Vf(har/er) | (SA) | Vi(supine/participle) | REST | A*`
- **Meaning:** Relate a prior event or resulting state to a reference time.
- **Examples:** `Hun har købt brød.` — “She has bought bread.” `Toget er kørt.` — “The train has left.”
- **Constraints:** Auxiliary choice and participial form must be stored with the verb sense; `har` cannot simply be replaced by `er`. English tense translations depend on discourse context.
- **Status:** mapped for later; described and source-checked; unsupported.

### C016 Coordination of clauses or shared-subject predicates

- **Pattern:** `clause + coordinator + clause`, or `S + [finite predicate] + coordinator + [finite predicate]`
- **Meaning:** Add, contrast, or offer alternatives.
- **Examples:** `Jeg laver mad, og Lea dækker bord.` — “I cook, and Lea sets the table.” `Han åbner døren og går ind.` — “He opens the door and goes in.”
- **Constraints:** Each bracketed finite predicate includes its finite verb and licensed dependents, not a choice between verb and complement. Full clause coordination and coordination of predicates are different structures. Shared material may be omitted only where Danish permits it and the result is clear. A single subject can govern several coordinated finite verbs. [Sproget.dk on sentences](https://sproget.dk/typiske-problemer/saetninger/)
- **Status:** mapped for later; described and source-checked; unsupported.

### C017 Infinitival complement or purpose phrase

- **Pattern fragments:** `Vf + at + infinitive + REST`, or `for at + infinitive + REST`. These are selected frame tails, not complete clause schemata; the containing clause supplies its subject and other licensed material.
- **Meaning:** Complete a controlling verb or express purpose.
- **Examples:** `Hun prøver at åbne vinduet.` — “She tries to open the window.” `Vi går ud for at købe kaffe.` — “We go out to buy coffee.”
- **Constraints:** Some verbs select `at`-infinitives, some take bare infinitives, and some select clauses or prepositions instead. The understood infinitive subject is controlled by context and the higher verb; do not infer it from surface proximity alone. `for at` is a purpose marker, not a universal synonym for `at`.
- **Status:** mapped for later; described and source-checked; unsupported.

## Subordinate and relative clauses

### C018 Object content clause with at

- **Pattern:** `matrix clause + at | S | (SA) | Vf | (Vi) | REST | A*`
- **Meaning:** Make a proposition the object/content of a verb of saying, thinking, knowing, or perception.
- **Example:** `Jeg ved, at hun ikke kommer.` — “I know that she is not coming.”
- **Constraints:** The comma before a subordinate clause depends on the chosen Danish start-comma convention; Astrid must choose and apply one consistent orthographic policy. `at` may be omitted after some verbs in some registers, but beginner generation should retain it where natural. The subordinate clause as a whole fills the matrix object slot.
- **Status:** mapped for later; described and source-checked; unsupported.

### C019 Postposed adverbial subordinate clause

- **Pattern:** `matrix clause + SUB | S | (SA) | Vf | (Vi) | REST | A*`
- **Meaning:** Add reason, time, condition, concession, or another relation.
- **Examples:** `Vi bliver hjemme, fordi det regner.` — “We are staying home because it is raining.” `Jeg ringer, når jeg er færdig.` — “I’ll call when I am finished.”
- **Constraints:** Subordinator choice encodes meaning; `fordi`, `når`, `hvis`, and `selvom` are not structural synonyms. Tense and mood interpretations depend on that meaning. [Sproget.dk list of subordinators](https://sproget.dk/sprogviden/ordlister/underordningskonjunktioner-underordningsbindeord/)
- **Status:** mapped for later; described and source-checked; unsupported.

### C020 Fronted adverbial subordinate clause plus V2 matrix

- **Pattern:** `[SUB | S | (SA) | Vf ...]F , | matrix Vf | matrix S | ...`
- **Meaning:** Place a condition, time, reason, or concession before the matrix assertion.
- **Example:** `Når arbejdet slutter, tager vi hjem.` — “When work ends, we go home.”
- **Constraints:** The entire subordinate clause is one front-field constituent. The matrix finite verb therefore comes immediately after it and before the matrix subject. Do not insert another fronted matrix constituent before that verb. Sproget.dk demonstrates the same whole-clause movement test. [Sproget.dk on subordinate clauses](https://sproget.dk/typiske-problemer/ledsaetninger/)
- **Status:** mapped for later; described and source-checked; unsupported.

### C021 Embedded content question

- **Pattern:** `matrix clause + WH(non-subject)/om | S | (SA) | Vf | (Vi) | REST | A*`
- **Meaning:** Embed a wh-question or yes-no question as a constituent.
- **Examples:** `Jeg ved ikke, hvor hun bor.` — “I do not know where she lives.” `De spørger, om bussen kommer.` — “They ask whether the bus is coming.”
- **Constraints:** This pattern covers non-subject wh questions and `om` questions with an explicit subject. Embedded subject questions and their `der` construction are identified but deferred; see [Dansk Sprognævn on subject questions](https://sproget.dk/sprogviden/spoergsmaal-og-svar/dansk-sprognaevn/sv00016066/). Embedded questions normally use subordinate order, not direct-question inversion: not beginner-standard `*Jeg ved ikke hvor bor hun`. `om` introduces an embedded yes-no question. Punctuation marks the whole utterance: an embedded question inside a statement does not by itself require a question mark. [Danish Language Council question-mark rule](https://ro.dsn.dk/?paragraf=53&type=rulesearch)
- **Status:** mapped for later; described and source-checked; unsupported.

### C022 Subject relative clause

- **Pattern:** `antecedent + som/der | (SA) | Vf | (Vi) | REST | A*`
- **Meaning:** Modify a noun by a clause in which the antecedent supplies the subject role.
- **Example:** `Jeg kender kvinden, der bor ovenpå.` — “I know the woman who lives upstairs.”
- **Constraints:** There is no separate overt subject after subject-relative `der` in this basic pattern. `som` is also possible in many subject relatives. Restrictive and parenthetical relatives differ in information structure and punctuation.
- **Status:** mapped for later; described and source-checked; unsupported.

### C023 Non-subject relative clause

- **Pattern:** `antecedent + (som) | S | (SA) | Vf | (Vi) | REST | A*`
- **Meaning:** Modify a noun whose role inside the relative clause is object, indirect object, or prepositional complement.
- **Examples:** `Det er bogen, som jeg læser.` — “That is the book that I am reading.” `Det er den ven, jeg taler med.` — “That is the friend I am talking with.”
- **Constraints:** The antecedent fills the relativized role; `REST` must not duplicate it. `der` alone is not the general marker for an object relative. `som` can often be omitted in a restrictive object relative; it cannot be omitted in every relative type. Preposition stranding is common in everyday Danish, while formal alternatives need separate register handling. Dansk Sprognævn distinguishes subject and non-subject relative patterns and documents when `som` may be omitted. [Sproget.dk on som and der](https://sproget.dk/sprogviden/spoergsmaal-og-svar/dansk-sprognaevn/sv00000091/)
- **Status:** mapped for later; described and source-checked; unsupported.

## Existential, impersonal, passive, and complex complements

### C024 Existential or presentational der

- **Pattern:** `der(formal S) | Vf | NP(content S) | (A)`
- **Meaning:** Introduce the existence or presence of an entity.
- **Examples:** `Der er en café på hjørnet.` — “There is a café on the corner.” `Der står en kop på bordet.` — “There is a cup standing on the table.”
- **Constraints:** Formal `der` fills the subject position while the postverbal noun phrase carries the introduced content. Definiteness restrictions and verb choice are construction-sensitive. Do not confuse existential `der` with locative `der` meaning “there” or relative `der`.
- **Status:** mapped for later; described and source-checked; unsupported.

### C025 Impersonal det clause

- **Pattern:** `det(formal S) | Vf | frame-specific remainder`
- **Meaning:** Express weather, time, evaluation, or a proposition whose content appears later.
- **Examples:** `Det regner.` — “It is raining.” `Det er svært at vente.` — “It is hard to wait.”
- **Constraints:** The frame-specific remainder is empty in `Det regner`; in `Det er svært at vente` it contains both the predicative `svært` and infinitival content `at vente`. These are separately licensed variants, not freely interchangeable slots. Formal `det` is not necessarily referential and must not be generated by translating every English “it.” In extraposition, agreement and complement form follow the Danish frame. Keep this separate from referential identifying `det` in C003.
- **Status:** mapped for later; described and source-checked; unsupported.

### C026 Basic transitive s-passive

- **Pattern:** `patient S | Vf-s | (af-agent PP) | A*`
- **Meaning:** Present an event without making the agent the syntactic subject, often for rules, routines, or general processes.
- **Examples:** `Døren åbnes klokken otte.` — “The door is opened at eight.” `Billetter købes online.` — “Tickets are bought online.”
- **Constraints:** These examples cover simple transitive patient-subject passives; impersonal and other passive argument structures are deferred. An agent PP is headed by `af`; its ordering with adverbials needs a reviewed frame. Not every verb or tense is equally natural in the s-passive, and the construction may sound formal or procedural. Agent phrases with `af` are possible but often unnecessary. Keep lexical passive forms distinct from active verbs that merely end in `-s`.
- **Status:** mapped for later; described and source-checked; unsupported.

### C027 Basic transitive blive-passive

- **Pattern:** `patient S | Vf(blive) | (SA) | Vi(past participle) | (af-agent PP) | A*`
- **Meaning:** Present an event affecting the patient, often with an eventive or change-oriented reading.
- **Example:** `Pakken bliver leveret i morgen.` — “The package will be delivered tomorrow.”
- **Constraints:** These examples cover simple transitive patient-subject passives; impersonal and other passive argument structures are deferred. An agent PP is headed by `af`; its ordering with adverbials needs a reviewed frame. `blive`-passive, `være` plus participle, and s-passive are not automatically interchangeable; event, result/state, tense, and register matter. In these illustrated transitive patterns, the active object becomes subject and the former agent may be absent or expressed with `af`. [Danish Language Council handbook on passive](https://dsn.dk/wp-content/uploads/2021/01/Haandbog-i-klarsprog.pdf#page=36)
- **Status:** mapped for later; described and source-checked; unsupported.

### C028 Object predicative construction

- **Pattern:** `S | Vf | O | PRED(object)`
- **Meaning:** Attribute a property, name, role, or resulting state to the object.
- **Examples:** `De maler døren blå.` — “They paint the door blue.” `Hun kalder katten Milo.` — “She calls the cat Milo.”
- **Constraints:** The predicative describes the object and may show agreement; it is not a second object. Only licensed verb senses allow this frame. Compare `Døren er blå` (subject predicative) with `De maler døren blå` (object predicative). [Sproget.dk on predicatives](https://sproget.dk/typiske-problemer/saetningsled/)
- **Status:** mapped for later; described and source-checked; unsupported.

## Phrase constructions used by clause slots

### C029 Basic noun phrase

- **Pattern:** `(determiner/possessor) + (adjective) + noun + (postmodifier)`
- **Function:** Fill subject, object, predicative, or prepositional-complement slots.
- **Examples:** `en lille kop` — “a small cup”; `min nye cykel` — “my new bicycle”; `bogen på bordet` — “the book on the table.”
- **Constraints:** Gender, number, and definiteness control determiner and adjective form. In the ordinary preposed possessive/genitive patterns illustrated here, the head noun has no definite suffix or additional article. Reviewed regular adjectives take the definite form where that variant has been introduced (G013 plus the relevant G014/G015 support). Other determiner and genitive constructions require separate licensing. Postmodifiers may be PPs or relative clauses and remain inside the noun-phrase constituent for V2 counting.
- **Status:** mapped for later; described and source-checked; unsupported.

### C030 Basic prepositional phrase

- **Pattern:** `preposition + NP/clausal complement`
- **Function:** Fill a selected verb complement or an adverbial slot.
- **Examples:** `på bordet` — “on the table”; `med min ven` — “with my friend”; `efter arbejdet` — “after work.”
- **Constraints:** Preposition choice can be spatial, idiomatic, or selected by a particular word sense. Pronoun case and fixed combinations need lexical data. A multiword PP counts as one constituent when fronted: `På det store bord står koppen.`
- **Status:** mapped for later; described and source-checked; unsupported.

## Allowed variation and ambiguity safeguards

The catalogue permits variation only when the construction, grammar prerequisites, and lexical frame all license it. Before generation, a content record should establish at least:

- the verb sense and its required, optional, and prohibited complements;
- semantic restrictions on fillers, such as animacy, edibility, readability, location, or transferability;
- inflection and agreement features;
- whether a modifier is selected or freely added;
- the intended information structure when fronting or omitting material;
- register and punctuation policy;
- a context sufficient to resolve pronouns, demonstratives, ellipsis, and structurally ambiguous questions.

Astrid should avoid isolated examples whose intended analysis is not recoverable. `Hun ser hende` is grammatically clear but referentially under-specified. `Hvem ser Anna?` can be read with different participant assignments in isolation. `Det er en mus` is lexically ambiguous between an animal and a computer mouse. Such strings can become fair exercises only with context, a picture, or deliberately constrained answer options; initial reading and writing exercises should prefer unambiguous items.

## Coverage boundaries and review needs

This first catalogue covers high-value standard written patterns and flags selected spoken or register variation. It does not yet model every placement of adverbials, verb particles, discontinuous phrase, extraction pattern, ellipsis, comparison, presentational inversion, cleft, tag question, reported-speech pattern, idiom, dialect form, or construction licensed only by a narrow lexical class. Danish subordinate clauses with main-clause word order are acknowledged but intentionally excluded from beginner generation.

Finite content clauses functioning as subjects and embedded subject questions with `der` are identified but deferred; the latter are explicitly outside C021's current pattern. Clause-as-subject functions are documented in [DSN's comma grammar](https://sproget.dk/typiske-problemer/komma/kommagrammatik/). The [foundation source review](foundation-source-review.md) records these gaps and the later G↔C mapping work; no new runtime construction is implied.

Before any construction moves from “described” to “reviewed,” a qualified reviewer should check the naturalness of every example, the valency and meaning constraints, the claimed neutral order, and the learner-facing English equivalent. Before “engine-supported,” tests should demonstrate that generated variants preserve those constraints and never treat constituent order as token order.

## Source basis

- [Sproget.dk: Sætningsled](https://sproget.dk/typiske-problemer/saetningsled/) — constituent functions and the fact that constituents can be words, phrases, or clauses.
- [Sproget.dk: Sætninger](https://sproget.dk/typiske-problemer/saetninger/) — finite clauses, subjects, imperatives, and shared-subject coordination.
- [Sproget.dk: Ledsætninger](https://sproget.dk/typiske-problemer/ledsaetninger/) — subordinate-clause status, introducers, constituent tests, and placement of `ikke`.
- [Sproget.dk: Underordningskonjunktioner](https://sproget.dk/sprogviden/ordlister/underordningskonjunktioner-underordningsbindeord/) — semantic families of subordinators.
- [Dansk Sprognævn via Sproget.dk: som and der](https://sproget.dk/sprogviden/spoergsmaal-og-svar/dansk-sprognaevn/sv00000091/) — subject and non-subject relative patterns.
- [Dansk Sprognævn: question marks](https://ro.dsn.dk/?paragraf=53&type=rulesearch) — direct versus embedded questions in punctuation.
- [Dansk Sprognævn: Håndbog i klarsprog](https://dsn.dk/wp-content/uploads/2021/01/Haandbog-i-klarsprog.pdf#page=36) — passive form and argument remapping.
- [University of Copenhagen: Ordstilling i ledsætninger i moderne dansk talesprog](https://researchprofiles.ku.dk/en/publications/ordstilling-i-leds%C3%A6tninger-i-moderne-dansk-talesprog/) — main-clause front field, V2 ordering, default subordinate ordering, and spoken variation.

The examples in this catalogue are original teaching examples, not quotations from these sources.
