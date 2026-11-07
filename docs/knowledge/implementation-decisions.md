# Engine Playground Implementation Decisions

## Authorized scope

The user authorized implementation on 7 November 2026. Build a private development playground for complete beginners learning everyday Danish through reading and writing. The [beginner slice](beginner-slice-spec.md) supplies six skills, three constructions, 23 lexical entries and three exercise formats. Broader curriculum, learner accounts, progression, listening, speaking, production media and mobile distribution remain deferred.

## Architecture

Use a modular monolith: dependency-free language domain in Kotlin `engine/`, Ktor HTTP/persistence in `server/`, and React/Mantine in `web/`. PostgreSQL stores development sessions, generated exercise evidence and immutable attempts. Flyway owns forward-only migrations. `e2e/` exercises the assembled application. Ports are 8088 (server), 5180 (Vite), and 5438 (optional local PostgreSQL).

Pure generation uses explicit content/generator revisions, seed and index. Enumerate only licensed combinations; select from a deterministic permutation without sentence repeats until the eligible pool is exhausted. Ordering IDs are assigned after shuffle. Public DTOs explicitly omit private answer keys and grammatical roles; inspection is a separate authenticated development endpoint.

## Assessment and persistence invariants

- Record target satisfaction separately from linguistic status. Valid fronting or numeral use can be linguistically acceptable while missing an exercise's instruction. Unsupported writing remains unknown.
- Preserve raw input, normalize NFC and harmless whitespace, retain Danish letters and lexical uppercase `I`.
- Record hints and retries as evidence; do not infer mastery or adaptive scheduling.
- Per-item teaching references are available before the prompt but initially collapsed. Opening one records supported practice, just like opening a hint. Support evidence stays attached across retries of that exercise. This playground does not establish independent or delayed mastery.
- Ordering prompts deterministically avoid the canonical solution order. Assign block IDs only after choosing the final displayed order.
- Store only hashes of random session-cookie tokens; scope exercises and attempts to their owning session.
- Serialize attempt creation under a session-row lock. Replaying the same attempt ID and payload returns its stored response; changed payload returns a conflict. Check replay before current revisions, so historical results remain retrievable.
- Evaluation failure produces an unavailable result and persists no graded learner error. Never silently regrade old attempts under new rules.

## Operational boundary

Development sessions require explicit `ASTRID_DEVELOPMENT_MODE=true`. Mutations require JSON and `X-Astrid-Client: playground`; validate Host and any supplied Origin without enabling CORS. Inspector additionally requires explicit enablement and a separate key of at least 32 characters; keep the key out of builds and browser persistent storage.

Compose publishes only loopback ports. Kubernetes uses ClusterIP and local port-forwarding without ingress. This is a private playground, not a public authentication system. The container runs unprivileged with a read-only application filesystem. PostgreSQL volumes survive ordinary restarts; deleting volumes is an explicit destructive operation.

## Open points and assumptions

The pilot remains draft linguistic content awaiting appropriate Danish teaching review. The final Astrid logo, production hosting/authentication, retention policy and future course/progression model remain open. One application replica is sufficient for this milestone; correctness relies on database locks rather than an in-memory idempotency cache. See the [reuse manifest](flow-reuse-manifest.md) for donor boundaries.

## Recall and repair revision contract

The user authorized typed present recall and guided single-word verb/article repair on 7 November 2026. BL-034 adds E004–E006 on `pilot-2/generator-2/evaluation-2`, retaining exact legacy `pilot-1/generator-1/evaluation-1` behavior and the same language inventory. The [extension contract](exercise-extension-proposal.md) defines controlled corruption, narrow licensed alternatives, optional prompt metadata, unchanged historical fingerprints/replay, and the compatible-reader rollback requirement. The [delivery record](recall-repair-delivery.md) records the verified implementation and retained legacy behavior.
