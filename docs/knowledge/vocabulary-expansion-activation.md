# Thematic Vocabulary Expansion: Authoring and Activation

## Selection and current boundary

**11 November 2026 — User-confirmed authoring milestone:** after the recommendation to
complete five session scripts and an implementation contract for **P1 — A day of work and
study**, the user said “Go for it” and requested enough useful work for the night until
**07:30 Europe/Warsaw on 12 November**, ahead of an 08:00 laptop shutdown. This is the new
work window; earlier overnight windows are historical. Do not alter power management.

The [selected vocabulary blueprint](vocabulary-course-blueprint.md#selected-expansion-authoring-plan--11-november-2026)
owns the eighteen P1 placements and the sixty-placement three-packet budget. Runtime app
0.19.0 remains at pair sixteen. S14 stays paused. This authoring milestone allocates no
stable lexical IDs, changes no published generator or grader, and supplies no Danish
specialist approval. Runtime implementation/release scope was separately offered for
clarification; an unanswered question does not authorize that additional phase.

Retain noun-only recall under its existing contract. Teach count nouns with articles;
mass uses, language nouns and chunks have explicitly different realizations. Q27 remains
open for a future article+noun assessment operation.

## Overnight work queue and stopping points

These are bounded execution packages within the accepted content direction, not a promise
that all packages or a runtime release fit the window. Work through them in dependency order;
independent source checks and code-readiness analysis may proceed alongside authoring.

| Package | Reviewable result | Completion boundary |
| --- | --- | --- |
| 1. P1 complete scripts | Five short sessions: teaching, readings, exact exercise slots, answers, distractors, exposure and feedback cases. | All eighteen placements accounted for; unsupported combinations and pending linguistic decisions explicit. |
| 2. P1 implementation contract | Exact supported operations/pools, prerequisites, saved-plan/exposure policy, compatibility changes and test matrix grounded in current code. | No fictional existing capability; minimal vertical slice and independent review findings reconciled. |
| 3. Source verification | Record-level COR form checks and primary reference consultation for new frame candidates. | Distinguish verified morphology from authored meanings/valency and teacher review. No bulk dataset adoption. |
| 4. P2 food/shopping authoring | Twenty-one selected placements with morphology/senses/frames and six short session outlines. | Recount reuse against current and P1 records; resolve duplicate senses and grammar prerequisites. |
| 5. P3 places/journeys authoring | Twenty-one selected placements with morphology/senses/frames and six short session outlines. | Distinguish location/direction and supplied chunks from productive grammar. |
| 6. Cross-packet readiness review | Exercise coverage, ambiguity, introduced-material eligibility, sentence variety and implementation dependencies across all three packets. | Findings resolved or preserved as precise open decisions; no frequency/mastery claim. |
| 7. Verified delivery and handoff | Focused commits, merge/push, knowledge index/backlog update and exact continuation instructions. | Full `./scripts/check.sh` with Docker before each commit; clean reviewed Git state and observed remote result. |

By **06:30**, prioritize closing active work and its verification. Do not begin a long
unbounded implementation or deployment near the cutoff. By **07:30**, leave the repository
at a safe stopping point with completed and unfinished work recorded. If runtime delivery
is selected, reserve time for exact-source CI/browser checks, a real backup restore,
app-only promotion and ownership-scoped QA cleanup; a partially verified build is not a
release. Documentation-only deliveries keep app 0.19.0 and do not redeploy the preview.

## Implementation contract

In preparation. The architecture pass must inspect actual lexical target types, choice
pool assumptions, reading/tile representations, practice summaries and saved exposure
before selecting operation reuse. “Same exercise family” does not prove a verb, mass noun
or multiword chunk fits a noun-headword operation. Keep current plans literally pinned.

The minimum design must specify what is newly taught per session, which later slots may
use it, how direct entry supplies missing teaching, and how unknown outcomes affect
continuation. Do not infer persistent cross-session learning history from an open lesson
or a browser-local completed-run link. Broader cumulative review and the Vocabulary library
remain BL-066 and BL-067.

## Source verification — 11 November 2026

A fresh independent lookup against the official documented COR JSON service confirmed
all nine P1 candidate lemma records and the exact active form IDs in the blueprint:
`30533` (skrive), `63924` (computer), `32395` (studere), `41200` (kursus),
`30480` (drikke), `43580` (kaffe), `41032` (vand), `10260` (hjemme) and
`43995` (morgen/morgenen). All selected forms returned normalization `N`; gender/form
analyses match the recorded noun/verb/adverb roles. Retrieval initially failed through
the web tool and sandbox DNS; scoped public network reads succeeded. This is a
reference check, not a dataset import or an application network dependency.
[Current COR release](https://ordregister.dk/) ·
[documented lookup service](https://ordregister.dk/doc/COR.Webservices.html)

Consulted DDO's **skrive** sense 1 (text production with optional object), **drikke**
verb sense 1 (beverage intake), **studere** sense 1 (education, with optional subject
of study), **hjemme** adverb sense 1 (static home location), **morgen** (the
`om morgenen` expression), and the separate noun/verb entries for **arbejde**.
These support reference distinctions; they do not certify every authored combination,
optimal teaching order or English cue. In particular, a bare “study” cue must not make
`læse` an allegedly invalid alternative to `studere`; P1 must establish the intended
selected use. No dictionary definitions or corpus sentences are imported.
[skrive](https://ordnet.dk/ddo/ordbog/skrive) ·
[drikke](https://ordnet.dk/ddo/ordbog/drikke) ·
[studere](https://ordnet.dk/ddo/ordbog/studere) ·
[hjemme](https://ordnet.dk/ddo/ordbog/hjemme) ·
[morgen](https://ordnet.dk/ddo/ordbog/morgen) ·
[arbejde](https://ordnet.dk/ddo/ordbog/arbejde)

The COR liquid-water record confirms neuter morphology. Its JSON response did not
expose the earlier audit's `væske` disambiguator: that selected liquid meaning is
authored content, not a new semantic finding from the morphology response. Likewise,
bare mass `kaffe/vand` and the full `om morgenen` chunk need explicit teaching/frame
review; an indefinite-form code does not require an indefinite article in every use.

## Observed delivery

No completed packet or runtime activation is claimed by this initial work plan. Record
actual source verification, reviews, checks and Git delivery here as packages finish.
