# Project Knowledge Base

This is the repository record of the Danish learning app's requirements, design proposals, research, and unresolved questions. The private engine playground is implemented; language content remains a draft awaiting Danish teaching review. Records below distinguish delivered behavior from future proposals.

## Contents

- [Product direction](product-direction.md): confirmed scope, intended advantages, and product questions.
- [Learning engine](learning-engine.md): proposed exercise model, variability, evaluation, and progress tracking.
- [Language foundations](language-foundations.md): requirements for the grammar curriculum, sentence structures, and vocabulary database.
- [Research and sources](research-and-sources.md): reference material, potential reusable data, and validation needs.
- [Working agreement](working-agreement.md): standing documentation rule, decision conventions, and backlog statuses.
- [Assumptions and principles](assumptions-and-principles.md): working assumptions, confirmed product tenets, and proposed design principles.
- [Open questions](open-questions.md): central register of unresolved questions and other open points.
- [Flow stack assessment](flow-stack-assessment.md): evaluated donor stack, reuse boundaries, recommendations, and gaps.
- [Visual identity](visual-identity.md): shared sibling UI style/layout, preferred red accent, and Denmark/Astrid logo brief.
- [First engine milestone](first-engine-milestone.md): readiness recommendation, proposed design pass, engine playground, and completion criteria.
- [Danish grammar map](danish-grammar-map.md): 58 draft skills in eight proposed phases with prerequisite links.
- [Danish sentence catalogue](danish-sentence-catalogue.md): 30 phrase/clause constructions and generation constraints.
- [Danish vocabulary seed](danish-vocabulary-seed.md): 23 selected pilot entries, grammatical forms, senses, and combination allowlists.
- [Beginner slice specification](beginner-slice-spec.md): connected targets, three exercise formats, grading examples, variation rules, and content/API responsibilities.
- [Project backlog](../../BACKLOG.md): separate work plan using `[next]`, `[todo]`, `[new]`, `[parked]`, and `[blocked]`.

## Status conventions

**Confirmed requirements** come directly from the user's stated intentions. **Proposals** are ideas discussed with the assistant that have not been adopted as decisions. **Open questions** need further investigation or a product decision. Examples illustrate possibilities; they do not establish curriculum coverage or implementation commitments.

Update the relevant notes as planning continues. Promote a proposal to a confirmed decision only when the discussion supports that change, and preserve the reason for consequential decisions. Keep externally sourced linguistic facts separate from proposed teaching choices.

The user has established continuous documentation as a standing project rule. Capture assumptions, requirements, constraints, open points, questions, decisions, rules, conventions, principles, and tenets as they arise. Follow the [working agreement](working-agreement.md) for record locations and maintenance.

## Current implementation

The [backlog](../../BACKLOG.md) owns work status and priority. The first grammar map and construction catalogue are drafted, with a connected beginner seed and worked exercise specification. The authorized engine playground is delivered. Its scope and operational boundaries are recorded in [implementation decisions](implementation-decisions.md), with the concrete [Flow reuse manifest](flow-reuse-manifest.md). Linguistic teaching review remains separate from software validation.

The broad language map and the small validation subset serve different purposes. A complete language implementation is not a prerequisite for validating the engine.

- [Playground delivery](playground-delivery.md): observed baseline implementation and validation evidence.
- [Pilot hardening plan](pilot-hardening-plan.md): authorized overnight reliability, content-integrity and accessibility work.

- [Danish resource assessment](danish-resource-assessment.md): primary evidence for morphology, sense identity, access and published reuse terms; no imported data.

- [Proposed COR pilot crosswalk](cor-pilot-crosswalk.md): record-level candidate morphology identities and homograph safeguards; not an adopted import.
- [Pilot source review](pilot-source-review.md): bounded linguistic source checks, precise locator gaps and qualifications.
- [Hardening delivery](hardening-delivery.md): observed compatibility, transaction, accessibility and local Kubernetes evidence.
- [Foundation source review](foundation-source-review.md): consolidated later-map corrections, coverage boundaries and source limitations.
- [Pilot review guide](pilot-review-guide.md): a short proposed walkthrough for evaluating the engine before expanding it.
