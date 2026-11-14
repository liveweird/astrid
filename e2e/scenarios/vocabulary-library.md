# Vocabulary library

- **Spec**: [tests/vocabulary-library.spec.ts](../tests/vocabulary-library.spec.ts)
- **Actors**: a learner browsing reference material, an owner with saved lesson teaching and a separate owner
- **Owns**: only development-session-scoped teaching runs created by this spec; browsing is read-only

## Scenario: vocabulary reference is finite and passive without a learner cookie

1. Read the unpaged reference endpoint without a cookie. Verify all 79 unique entries, separate request and possession senses, home-direction form and absence of private grading fields.
2. Check no-store, no cookie creation, authenticated context rejection and the unchanged origin guard.

## Scenario: saved teaching claims remain owner scoped and browsing creates no new teaching

1. Create an owner and a P3 bicycle lesson. An unopened introduction creates no claim.
2. Save teaching and verify the bicycle article bundle is claimed; untaught waiting is absent.
3. Browse the library and scan accessibility. Verify exact teaching context, saved lesson state and cumulative preparation stay unchanged.
4. A separate owner has no claims from the first owner's lesson.

## Scenario: vocabulary search details and lesson links remain passive and keyboard accessible

1. Navigate from Lessons to Vocabulary; main content receives focus and all 79 records appear.
2. Search for home forms, expand homeward movement by keyboard, and verify separately marked Danish and English examples.
3. Search for the whole station destination chunk and follow its lesson link. The exact lesson row receives visible focus below the header.
4. Verify no mutation or learner cookie was created; scan expanded details for accessibility.

## Scenario: saved-teaching filter explains absent ownership without starting a session

1. Apply the saved-teaching filter before loading history; the empty state offers a next step.
2. Explicitly load teaching without a learner cookie. The page explains absent ownership and creates no session.
3. Remove the filter, recover all entries and scan accessibility.

## Scenario: vocabulary details filters and empty state reflow at 200 percent text in ${theme}

1. Use light and dark themes at 320px width with 200% text. Explicitly save a bicycle lesson’s teaching, load its recorded context, search for the article bundle and expand its example. Verify meaning/form badges remain readable without clipped labels.
2. Select a situation that excludes the search result and verify the empty state.
3. Open mobile navigation and verify Vocabulary is reachable. Scan all three states for accessibility and horizontal overflow.

## Scenario: vocabulary browsing preserves a pending practice creation without replaying it

1. Lose the response after the server commits a lesson creation. Keep the original serialized operation in the browser outbox.
2. Browse Vocabulary, search a complete phrase, open details and follow a passive lesson link. Verify there is no resend and all practice storage stays byte-identical.
3. Explicitly resume saved practice. Only then resend the exact original creation body and recover the original lesson.
