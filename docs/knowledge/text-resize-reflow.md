# Text Resizing and Narrow Layout

## Confirmed gap — 7 November 2026

A separate Chromium/WebKit check at 320 CSS pixels with a real `200%` root-font override exposed clipped header controls, truncated labels and a hidden skip link overlapping navigation. The page extended to 335 pixels. An open mobile navigation overlay correctly blocked underlying exercise interaction; that behavior was not classified as a defect.

The previous browser regression had not enlarged text: its initialization script accessed `document.documentElement.style` before the root existed. A controlled rerun reproduced a page error and a computed **16px** root font in both engines. Because the test checked neither page errors nor computed font size, it passed at the normal size.

## Implemented correction

- Express the shell/header height in `rem`, reduce narrow header padding and retain the visual mark when the optional wordmark cannot fit.
- Cap narrow content padding at its normal size while text grows, retaining more room for complete labels without reducing text or target sizes.
- Move the unfocused skip link above its full measured height, including a small rounding margin. Restore visibility and pointer interaction on keyboard focus.
- Let narrow exercise buttons grow and wrap, including feedback/recovery actions. Preserve text size and minimum target height.
- Wrap badge and accordion labels rather than clipping them with ellipses. Give button labels enough line height for the enlarged font's ink.

These are responsive presentation changes. They do not alter content, grading, support evidence or persisted attempts. No page-wide overflow hiding or font-size reduction is used.

## Regression and evidence

The corrected initialization installs the override once the document root exists. The journey asserts a computed **32px** root font and no page errors, verifies both header controls lie inside the header/viewport, and reaches the skip link through native keyboard traversal. macOS WebKit uses Option-Tab; other tested environments use Tab.

It opens and closes mobile navigation, then exercises lesson, hint, feedback, all three formats and revision recovery. Page-width assertions are supplemented by internal horizontal/vertical measurements of rendered button, badge and accordion labels: document width alone cannot detect nested clipping.

The focused packaged regression passed in Chromium and WebKit. At that stage all 29 frontend tests passed with the existing coverage floors intact. Subsequent [selector and short-window corrections](keyboard-navigation.md) passed all 44 frontend tests, all 38 native browser journeys and all 38 Linux-container journeys, with unchanged 32px text and strict list-client/viewport bounds. Independent review found the initial remaining ellipses and keyboard portability gap; those were corrected and reviewed. The complete local matrices passed; pinned-Node-24 GitHub CI and Git delivery remain pending while BL-028 is active.

A separate normal-UI probe submitted 128- and 512-character unbroken unknown answers and unsupported Unicode input at both 16px and 32px root sizes. All 12 browser/font/input cases displayed feedback without horizontal page overflow at 320px. These are display checks for accepted input, not claims that the entered strings are valid Danish.

The user-stylesheet override is a reproducible text-resizing approximation, not a physical-device or operating-system zoom certification. The agreed future mobile packaging remains deferred.
