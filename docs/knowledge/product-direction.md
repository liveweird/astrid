# Product Direction

## Confirmed scope

Build a language learning app for English-speaking learners, with Danish as the first priority. Future target languages may follow, while English remains the learners' source language.

**6 November 2026 — Initial learner and purpose:** The user selected complete beginners and general everyday Danish as the initial content focus. Exact learning outcomes and the first implementation subset remain to be specified.

The initial version focuses on reading and writing, including grammar and vocabulary. Listening and speaking are postponed. Start with a web version; a mobile version is intended to follow.

The user clarified on 6 November 2026 that online mobile apps with reuse of the web interface are acceptable. Native UI and offline lessons are not current requirements. The mobile packaging and distribution technology remains undecided.

The initial development priority is the learning engine: exercise types, a vocabulary database, and a mechanism for exercise variability. Images, sounds, and other production media follow once the engine is satisfactory. The current phase is planning; the user explicitly requested no application code yet.

## Confirmed learning priorities

The user wants these advantages over their experience of existing apps:

1. **Exercises with a specific learning target.** For example, test tense choice using different tenses of the same verb as alternatives, rather than unrelated words that make the answer obvious.
2. **An understanding of sentence structure.** Represent subjects, predicates, objects, and other components so the engine can construct new sentences and vary exercises beyond a small fixed sentence collection.
3. **Substantial grammar practice.** Include word order, tense usage, and language-specific forms alongside vocabulary.
4. **A wider range of exercise types.** Reduce boredom and repetition through meaningful changes in the activity the learner performs.

These are product requirements, not independently verified claims about competing apps.

## Confirmed reuse preference

Reuse as much of the existing Flow project's stack, infrastructure, design approach, tools, conventions, checks, and guardrails as makes sense for Astrid. The user requested an assessment before adoption. The [Flow stack assessment](flow-stack-assessment.md) recommends a selective foundation port; technology and architecture choices remain proposals except where explicitly confirmed below.

## Confirmed infrastructure decisions

**6 November 2026 — Retain Kubernetes and Docker Compose.** The user explicitly selected both after reviewing the Flow assessment. Keep Docker Compose for the local full stack and integration testing, and retain Kubernetes deployment support as part of Astrid's infrastructure scope. This supersedes the suggestion to defer Kubernetes as an optional platform.

Reuse Flow's relevant container and deployment conventions, adapting configuration to Astrid. The Kubernetes environment/provider, ingress and TLS, database placement, secrets, backup/restore, rollout strategy, and automation remain open design points. Retaining these tools does not adopt Flow's complete topology, worker split, or runtime sizing.

## Confirmed visual identity requirements

Follow the shared UI style and layout of Lettuce, Toadie, Covenant, Astrolabe, and Flow. Give Astrid its own main color and unique logo within that family. The user's preferred color direction is red, inspired by the Danish flag. The logo must relate to Denmark or depict a Danish girl representing Astrid. Exact shades and the final logo concept remain open; see the [visual identity brief](visual-identity.md).

## Confirmed language foundations

The engine needs three connected resources:

- A thorough breakdown of Danish grammar topics, ordered logically from foundational to advanced knowledge.
- A thorough description of Danish sentence structure and its variants.
- A useful subset of Danish vocabulary sufficient to build the content database and support sentence generation.

See [Language foundations](language-foundations.md) for the proposed organization of these resources.

## Proposed success criteria

Evaluate whether learners can understand and produce previously unseen sentences using studied knowledge. Include delayed checks to distinguish immediate exercise success from retention. Engagement matters, but exercise completion alone does not establish learning.

## Open product questions

Track resolution in the central [open questions register](open-questions.md), especially Q01–Q04.

- What reading and writing outcomes should the first release support?
- What should a session look like, and how much control should learners have over topics and review?
- Which measurable outcomes will establish that the engine is satisfactory?
- What proficiency range and register of Danish should the first curriculum cover?
