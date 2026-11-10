# First-step owner continuity

- **Spec**: [tests/module-one-continuity.spec.ts](../tests/module-one-continuity.spec.ts)
- **Actors**: an anonymous beginner whose development-session cookie becomes unavailable
- **Owns**: two development-session-scoped first-step runs and one pending browser recovery operation

## Scenario: first steps keep pending teaching with its unavailable owner until a new run is explicitly chosen

1. Start “Talking about one person” and lose the required-introduction support response before it reaches the server.
   - Expected: the exact support operation remains pending; teaching and assessment stay hidden while persistence is uncertain.
2. Remove the development-session cookie and retry the pending support operation.
   - Expected: the owner read answers 401 before mutation replay, the exact serialized support body remains stored for its old owner, recovery is focused, and no session is renewed or run created automatically.
3. Explicitly choose to start the same first-step profile again.
   - Expected: exactly one new development session and one new run are created with a different owner and run id; its required introduction is unopened and it has no attempts.
4. Inspect continuity after renewal.
   - Expected: the old device-level run and pending-operation records remain byte-identical, while the new owner receives 404 for the old owner's run instead of replaying or reassociating it.

## Not covered here

The fixed server-side TTL boundary is covered by `ModuleOneLifecycleTest`; this browser scenario deliberately proves missing-cookie 401 recovery and does not describe cookie removal as elapsed-time expiry.
