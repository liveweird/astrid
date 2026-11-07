# Server Work During Database Outages

## Status

**7 November 2026 — Proposed follow-up, not an implemented deadline policy.** The private pilot has a 15-second browser request budget and exact-attempt recovery. The [outage experiments](runtime-recovery.md) do not establish bounded server SQL, cleanup or shutdown. This note defines the remaining engineering question without selecting budgets, a production SLO or a new deployment model.

## Established boundaries

The repository uses separate transactions for session authentication and attempt writing. A transaction timeout alone would therefore not cover an entire HTTP request. Attempt writes serialize on the owning session, deduplicate identity/payload and persist consecutive retries atomically; existing attempts replay before current evaluation/revision checks. Preserve these properties under any new policy.

Pinned binary inspection with `javap -p -c` found:

- **Exposed 1.6:** `rollbackLoggingException` and top-level transaction closure use `NonCancellable`. An enclosing coroutine timeout does not prove timely cleanup.
- **R2DBC pool 1.0.2:** logical connection close runs a release publisher that can include rollback. `dispose()` calls `disposeLater().block()` without an explicit duration. Neither proves physical transport termination within a deadline.
- **PostgreSQL driver 1.1.3:** statement/lock limits become backend settings, and cancellation opens a separate connection. A paused backend cannot execute those settings or immediately release its locks. Physical-close completion during faults needs runtime evidence.
- **Acquisition retries:** the pool builder defaults `acquireRetry` to 1. `ConnectionPool` wraps deferred, individually timed acquisition/preparation in `Mono.retry`; pinned Reactor 3.8.8 retries error signals without a type filter unless cancelled. Sustained acquisition failure can therefore consume two configured acquisition windows. This is binary-contract inference, not a measured timing guarantee, and does not retry SQL after a lease is handed out.

The previous rationale comparing the browser's 15 seconds with a total 10-second acquisition budget is superseded. Keep the browser budget as an independent UI choice.


## Measured saturation baseline

An isolated QA session row lock occupied all ten app connections. PostgreSQL showed one direct and nine transitive lock waiters; a recursive blocker graph verified all ten exact backend identities reached the owned lock. An eleventh request returned HTTP 500 after **20.033 seconds** while those ten remained blocked. After releasing the test lock, the first ten attempts saved consecutive retries 1–10 with byte-identical readback/replay. The eleventh ID returned 404 before its exact replay created retry 11; a fresh ID became retry 12. Readiness recovered, neither container process changed, and no forced lock cleanup was needed.

This is one observed saturation case consistent with the retry inference, not a general 20-second request bound. The pool-internal queue was inferred from pending HTTP work and the occupied connections, not directly instrumented. It does not test stalled acquired SQL or cleanup.

## Required invariants

Produce a terminal HTTP outcome within a chosen request budget while the scheduler runs; never report success before commit. Deadline expiry, disconnect or lost commit acknowledgement remains uncertain. A transient GET 404 cannot prove an earlier write will never commit.

Fence abandoned leases before reuse, prevent late cancellation affecting another borrower, and bound retained tasks, acquisition queues and readiness work. Preserve caller cancellation separately from normal failure responses.

Distinguish **local fencing** from **remote cleanup**. No application policy can promise immediate PostgreSQL transaction/lock release while that backend cannot execute. Observe cleanup after resumption, and define shutdown behavior independently.

## Candidate policies

| Proposal | Tradeoff to validate |
| --- | --- |
| Per-lease escalation: cooperative SQL/lock limits, request cutoff and cleanup grace, then physical termination/invalidation of the owned lease | Narrow fault isolation, but requires a verified Exposed/pool/driver ownership adapter and cancellation-race evidence. Do not infer a safe abort API from its signature. |
| Application fail-stop escalation: stop admission, end waiting calls, allow bounded cleanup, then use a supervisor-enforced stop/restart if lease disposal remains unproved | Less driver coupling, but interrupts every in-flight pilot session. Ordinary shutdown hooks alone do not establish the exit bound. Remote cleanup still waits for PostgreSQL. |

Neither proposal is selected. No schema migration is indicated; rollback must preserve stored identities and original responses.

## Evidence before implementation

| Fault | Required observations |
| --- | --- |
| Saturated pool or failed creation/validation | Total acquisition behavior including retries; cancelled waiters removed; fresh acquisition recovers |
| Stall before write, after session lock, after insert or during commit acknowledgement | HTTP completion; uncertain durable outcome; lease fenced; remote locks cleared after resume |
| Repeated timeouts and readiness calls | Bounded retained work; no poisoned lease reuse; readiness recovers |
| Cancellation racing commit/release | No cancellation of another borrower; stable exact replay; consecutive retries |
| Shutdown during each stall | Explicit local exit bound and separately observed post-resume cleanup/replay |

The coordinator should settle the fault model and evidence-based budgets first. A developer can then build an isolated real-HTTP/PostgreSQL failure matrix before implementing one chosen policy across repository, request and lifecycle paths. Independent review must assess race evidence and remaining limits. Existing cancellation-callback and already-cancelled-readiness tests establish their narrow contracts, not cancellation during stalled acquired SQL.
