# Danish Language Foundations

## Confirmed requirements

Create a logically ordered grammar topic breakdown, a thorough description of sentence structures and their variants, and a vocabulary subset that can support the content database. These resources must connect to one another so exercises can target grammar while generating varied language.

## Draft resources delivered

The first design pass has produced a [58-skill grammar map](danish-grammar-map.md), [30-construction sentence catalogue](danish-sentence-catalogue.md), [23-entry pilot vocabulary seed](danish-vocabulary-seed.md), and [connected beginner slice specification](beginner-slice-spec.md). These are source-backed drafts, not a complete language database or specialist-approved curriculum. Proposed skill order is separate from grammatical correctness and has no claimed official CEFR alignment.

## Proposed grammar curriculum

Create a broad topic inventory and divide it into small, assessable skills. Connect prerequisites explicitly, then derive a provisional learning sequence. Revisit earlier skills as they become part of more complex constructions.

For each skill, record:

- Its learning objective and prerequisites.
- An explanation for English-speaking learners, including relevant English–Danish differences.
- Correct examples, common mistakes, and important exceptions.
- Suitable exercises and criteria for judging an answer.
- Required sentence constructions and vocabulary.

For example, adjective agreement can be divided into gender, number, and definiteness rather than treated as one undifferentiated topic. Danish adjective inflection includes these distinctions. [Sproget.dk](https://sproget.dk/typiske-problemer/adjektiver-tillaegsord/)

The teaching sequence is a proposed pedagogical choice; a linguistic reference does not by itself establish the best order. Review it with a teacher experienced in Danish as an additional language.

## Proposed structural grammar

Describe noun phrases, verb constructions, complements, modifiers, and combinations of clauses as well as complete sentences. Inventory statements, questions, commands, negation, coordination, subordinate clauses, and more complex constructions.

For each construction, record its components and their roles, ordering and form constraints, permitted transformations, meaning or usage restrictions, examples, and exceptions. Distinguish the function a component serves from the kind of phrase that fills it.

Maintain coverage status for each construction: identified, described, reviewed, and eventually supported by the engine. Map advanced variants early while implementing a smaller range first. The exact boundary of “all variants,” including register and exceptional constructions, remains open.

## Proposed teaching vocabulary

Choose vocabulary using frequency, everyday usefulness, grammatical coverage, and compatibility with the selected constructions. Include function words and multiword expressions as needed, alongside nouns, verbs, adjectives, and other content words.

A proposed entry includes:

- Dictionary form, word class, and grammatical properties.
- Distinct meanings and context-appropriate English equivalents.
- Required inflected forms, accepted variants, and irregularities.
- Compatible verb constructions, complements, prepositions, and common combinations.
- Meaning categories and usage constraints that support natural generation.
- Examples, prerequisite or introduction links, source information, and review status.

Verb construction information should describe which complements a meaning accepts. A word's class alone is insufficient to determine where it can be substituted.

Select vocabulary by meaning as well as spelling. Measure sufficiency by whether the chosen words support useful, varied exercises for the curriculum; no final vocabulary size has been agreed.

## Connections and proposed working order

Each curriculum skill should reference constructions, and each construction should reference compatible vocabulary and prerequisites. This allows the engine to vary examples while preserving the intended target and difficulty.

Map topics and constructions together, establish dependencies and teaching order, select vocabulary, and manually assemble sample exercises. The first drafts now connect G001–G006 to C001–C003 and their licensed vocabulary, with three exercise formats and worked acceptance cases. Expand and review accuracy/naturalness before treating the content as approved for learners. Broader vocabulary, complete exceptions, and full construction coverage remain work.
