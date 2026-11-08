# Keyboard Navigation and Selection

## Scope

**7 November 2026 — Observed defects and implementation notes.** These changes belong to the authorized pilot accessibility verification, BL-027. They do not activate new exercise formats or alter language content, generation or grading.

## Mobile navigation row

An actual Chromium check at 390px reproduced a pointer-only row: after opening navigation with Enter, Tab passed from the theme control into the lesson rather than reaching “Engine playground.” Pinned Mantine rendered an anchor without `href` or `tabIndex`; the row had a close handler but no native keyboard activation. The burger also lacked expanded/control metadata.

The implementation gives the row its real local destination, `#main-content`, while retaining the close handler. The burger identifies the navigation region and its expanded state. Closed mobile navigation is hidden through responsive visibility styling, excluding descendants from focus and the accessibility tree; desktop navigation stays available regardless of the mobile-open state.

Regression coverage must open navigation with the keyboard, reach the row through native traversal, activate it with Enter, and verify focus reaches the main content with navigation closed. A separate desktop path guards against accidentally hiding the persistent sidebar. macOS WebKit uses its native Option-Tab policy. The initial source/unit checks were followed by the packaged acceptance below.

The packaged mobile, desktop and persisted-rail viewport-transition paths subsequently passed in both engines (six checks). Entering desktop now closes transient mobile-open state and preserves the stored collapsed rail; returning to mobile leaves its drawer closed. The rail toggle exposes its action, expanded state and controlled region directly, rather than depending on a hover tooltip.

## Intermittent format-selection Enter

After fixing the separate [accordion transition race](runtime-recovery.md#browser-portability-acceptance), native Chromium still intermittently failed to commit a searched format with Enter. Twenty independent instrumented trials produced **16 successes and 4 failures**.

In every failure, Enter reached the focused “Exercise type” input with the expected key code and no composition in progress. The dropdown was expanded and its sole matching option had both the selected DOM marker and active-descendant reference. Nevertheless Enter was not prevented, no option click occurred and no new exercise request was sent. Successful trials prevented Enter, produced the library's synthetic option click and generated the requested format.

Pinned Mantine's handler only prevents/submits Enter when its internal selected index is not `-1`. Together, the event evidence and source identify a mismatch between that internal state and the visible selected marker. The exact timer/reset ordering is not directly exposed by the public DOM. Waiting for an additional visible marker cannot reliably distinguish these states.

The initial `selectFirstOptionOnChange` experiment was insufficient: **5 of 40** subsequent trials still failed. Do not treat its passing component tests as resolution of the native race.

A second candidate also enabled `selectFirstOptionOnDropdownOpen`, bypassing the delayed active-option lookup on opening. A temporary active-first presentation partition preserved the current topic on plain reopen/Enter without mutating the public catalog. These were bounded experiments, not new learning features or permanent ordering requirements.

That two-property candidate passed **40/40** ordinary native trials. It remains insufficient: forty rapid Tab-away/reopen trials produced **32 successful selections, six focused-input Enter failures and two failures of the selected-option assertion before Enter**. This rejects the candidate as a complete lifecycle fix. A same-current selection is a separate contract: Enter dismisses without a new exercise request; the explicit Regenerate action creates another exercise. A test that expected regeneration merely from reselecting current E003 was corrected rather than treated as another defect.

## Controlled implementation

Use a small format-selector component composed from Mantine's existing input, dropdown, options and keyboard primitives, with its public [`useVirtualizedCombobox` store](https://mantine.dev/core/combobox/#virtualization). All three options render normally; no virtualization library or dependency is introduced. The store permits React-owned option indices without the ordinary Select's delayed selection/reset callbacks. Its optional timed focus helpers are not needed.

Own query and highlighted format together while editing, and clear that state synchronously on close. Preserve original catalog order and highlight the committed format when opening. Derive the rendered highlight, store index and active-descendant ID from the same state. Pointer and keyboard submissions share one callback; only a different enabled format changes the committed selection. Focus, searching, arrows, blur and same-current dismissal must not generate an exercise or clear an existing answer. A failed generation keeps the previous committed label.

This supersedes the two-property/partition workaround. It changes no API, grammar content, evaluation policy or release revision. Mantine still handles native arrow, Enter, Escape and composition events; the application does not implement an alternative key handler. Source verification is complete; observed delivery is recorded below.

The controlled component passed **40/40 startup and 40/40 rapid-reopen native trials**, including after its scrolling correction. All **30 macOS and 30 Linux browser journeys** then passed. Open-popup checks at 320px/32px font verify unexcluded Axe results, active-descendant identity, visible highlighted options and unchanged document scroll. The listbox itself owns scrolling; a layout adjustment changes only its scroll position.

A separate short-window probe found six persistent off-viewport highlights among 24 option transitions across eight browser/viewport/font cases. Its initial viewport-only oracle also missed options clipped by the listbox itself; acceptance now checks the list client box as well as the viewport. The viewport-height mode disables popup flipping, while viewport-based field columns can become too narrow beside the enlarged desktop sidebar. Those layout limits remain acceptance work: fit the popup to usable space and make constrained content reflow according to its available width. Passing the taller-viewport matrix does not resolve these short-window failures. A best-fit flip/size candidate again passed 40/40 startup and 40/40 rapid-reopen keyboard trials, but the focused browser regression still failed in three of four checks: Chromium reported an out-of-viewport option edge in the tall enlarged-text case, and both engines retained a clipped highlight in the short-window case. This rejects that candidate as final acceptance; the bounds checks remain intact.


The accepted local correction mounts the popup fresh on opening, uses stable explicit expanded/controlled-list metadata, and attaches visibility observation when the portaled list node actually mounts. A target-width popup could make a wrapped option taller than either available side of the input. The popup can therefore widen within viewport bounds, with padding capped at normal-size values and line height 1.4; font size stays unchanged. This combines supported Mantine primitives and sizing middleware without overlaying the focused input or scrolling ancestors.

After that correction, **40/40 startup and 40/40 rapid-reopen trials** passed again. The strengthened focused suite passed **10/10**, including all three committed formats at each short-window/font combination and open-popup Axe scans in both engines. The complete native suite passed **38/38**. All **42 frontend tests** passed, with coverage floors retained at 91/86/92/93. The later focus correction and final-image acceptance are recorded below; earlier passing matrices do not substitute for final-image gates.

Accept the change only after component coverage, repeated instrumented native keyboard trials, rapid close/reopen checks, the unchanged keyboard ordering journey and the full browser matrix pass. Also verify non-first current formats retain their topic on reopening without a search. Preserve focus assertions and report all trial outcomes. A passing single rerun does not resolve the observed intermittent failure. If the candidate does not remove it, investigate the remaining close/reset behavior before changing controls or dependencies.

## Focus after asynchronous results

Independent review found that keying the selector by value/disabled state replaced the focused input. A native Chromium check also confirmed that both successful and uncertain submissions left focus on the document body; the next Tab skipped their result actions and reached Generation controls. An old unit assertion was checking the detached selector rather than the live control.

The selector now preserves input identity and synchronously invalidates editing when controlled value/disabled state changes. Successful generation still focuses the new instruction. Generation/submission errors focus their error alert, uncertain outcomes focus their recovery alert, and confirmed attempts focus the feedback status. Request counters handle repeated identical outcomes. The next native traversal reaches the relevant recovery or practice actions; macOS WebKit retains Option-Tab. Unit assertions re-query live nodes and check identity, label and focus.

The final focus correction passed independent review, all **44 frontend tests**, the raised coverage floors **92/87/93/94**, and all **38 native browser journeys**. On that final image, **40/40 startup and 40/40 rapid-reopen selections** passed, followed by **38/38 Linux journeys with retries disabled** in the pinned Playwright container. That container uses Node 26; all five pinned-Node-24 [branch](https://github.com/liveweird/astrid/actions/runs/42340567827) and [master CI jobs](https://github.com/liveweird/astrid/actions/runs/42341038761) subsequently passed. The reviewed changes were merged/pushed through `3ca0533`; BL-027 is complete.
