import { Alert, Badge, Group, Paper, Stack, Text, ThemeIcon } from "@mantine/core";
import { IconAlertTriangle, IconCircleCheck } from "@tabler/icons-react";
import type { Ref } from "react";
import type {
  Attempt, Exercise, GapPromptData, GapResult, MatchingColumns, MatchingPairResult,
} from "../api/models";
import classes from "../theme.module.css";

function targetLabel(value: string) {
  if (value === "met") return "Target met";
  if (value === "unevaluable") return "Target not evaluated";
  return "Target to revisit";
}

function languageLabel(value: string) {
  if (value === "acceptable") return "Acceptable Danish";
  if (value === "known-issue") return "Known language issue";
  return "Language status unknown";
}

function meaningLabel(value: string) {
  if (value === "met") return "Meaning matched";
  if (value === "not-met") return "Meaning to revisit";
  return "Meaning not evaluated";
}

function lexicalLanguageLabel(attempt: Attempt) {
  const scope = attempt.linguisticScope;
  if (scope === "english-choice") {
    return attempt.linguisticStatus === "acceptable"
      ? "Recognized English choice"
      : "Unrecognized English choice";
  }
  if (scope === "danish-headword" && attempt.linguisticStatus === "acceptable") {
    return "Recognized Danish headword";
  }
  if (scope === "danish-choice") {
    return attempt.linguisticStatus === "acceptable"
      ? "Recognized Danish choice"
      : "Unrecognized Danish choice";
  }
  if (scope === "danish-matching") return "Supplied Danish noun phrases";
  if (scope === "danish-gaps") return "Danish gap selections";
  if (scope === "danish-word-form") {
    return attempt.linguisticStatus === "acceptable" ? "Recognized Danish word form" : languageLabel(attempt.linguisticStatus);
  }
  return languageLabel(attempt.linguisticStatus);
}

function feedbackTitle(
  goodWork: boolean,
  identifiedHeadword: boolean,
  identifiedWritingTarget: boolean,
  gapSupportNeedsReview: boolean,
  wordFormTitle: string | null,
) {
  if (goodWork) return "Good work";
  if (identifiedHeadword) return "Headword identified; adjust your answer.";
  if (identifiedWritingTarget) return "Target met; adjust the rest of your answer.";
  if (gapSupportNeedsReview) return "Verb target met; review the article.";
  if (wordFormTitle) return wordFormTitle;
  return "Look at the target once more";
}

function failedConstraintLabel(scope: string | null | undefined, tileResponse: boolean) {
  if (scope === "danish-matching") return "Complete four matches";
  if (scope === "danish-gaps") return "Complete both gaps";
  if (tileResponse) return "Response constraint to revisit";
  if (scope === "danish-headword") return "One-word response required";
  if (scope === "danish-word-form") return "One-word form required";
  if (scope === "danish-sentence") return "One statement required";
  return "Supplied choice required";
}

function metConstraintLabel(scope: string | null | undefined, tileResponse: boolean) {
  if (scope === "danish-matching") return "Four matches supplied";
  if (scope === "danish-gaps") return "Both gaps supplied";
  if (tileResponse) return "Response constraint met";
  if (scope === "danish-sentence") return "One statement supplied";
  if (scope === "danish-word-form") return "One-word form supplied";
  return "Supplied choice used";
}

function matchingEvidence(attempt: Attempt, exercise: Exercise | undefined) {
  if (attempt.linguisticScope !== "danish-matching" || exercise?.formatId !== "E014") return null;
  return { results: attempt.matchingResults, columns: exercise.prompt.matchingColumns };
}

function MatchingFeedback({ results, columns }: { results: MatchingPairResult[]; columns: MatchingColumns }) {
  return (
    <Stack gap="xs" role="group" aria-label="Matching feedback">
      {results.map((result) => {
        const left = columns.left.find((item) => item.id === result.leftId)?.text ?? result.leftId;
        const right = columns.right.find((item) => item.id === result.rightId)?.text;
        const correct = result.targetSatisfaction === "met" && result.meaningSatisfaction === "met";
        return (
          <Paper key={result.leftId} withBorder p="sm" role="group" aria-label={`Match for ${left}`}>
            <Group className={classes.matchingFeedbackRow} align="flex-start" wrap="nowrap">
              <ThemeIcon color={correct ? "teal" : "orange"} variant="light" aria-hidden="true">
                {correct ? <IconCircleCheck size={18} /> : <IconAlertTriangle size={18} />}
              </ThemeIcon>
              <Stack className={classes.matchingFeedbackBody} gap={2}>
                <Text fw={650}><span lang="en">{left}</span>{right ? <> → <span lang="da">{right}</span></> : " — no match selected"}</Text>
                <Text size="sm">{result.feedback}</Text>
              </Stack>
            </Group>
          </Paper>
        );
      })}
    </Stack>
  );
}

function gapEvidence(attempt: Attempt, exercise: Exercise | undefined) {
  if (attempt.linguisticScope !== "danish-gaps" || exercise?.formatId !== "E015") return null;
  return { results: attempt.gapResults, prompt: exercise.prompt.gapPrompt };
}

function GapFeedback({ results, prompt }: { results: GapResult[]; prompt: GapPromptData }) {
  return (
    <Stack gap="xs" role="group" aria-label="Gap feedback">
      {results.map((result) => {
        const slot = prompt.slots.find((candidate) => candidate.id === result.slotId);
        const choice = slot?.choices.find((candidate) => candidate.id === result.choiceId)?.text;
        const correct = result.targetSatisfaction === "met";
        const primary = result.assessmentRole === "primary";
        const label = primary ? "Present verb feedback" : "Indefinite article feedback";
        const roleLabel = primary ? "Present verb — Primary target" : "Indefinite article — Supporting grammar";
        return (
          <Paper key={result.slotId} withBorder p="sm" role="group" aria-label={label}>
            <Group className={classes.matchingFeedbackRow} align="flex-start" wrap="nowrap">
              <ThemeIcon color={correct ? "teal" : "orange"} variant="light" aria-hidden="true">
                {correct ? <IconCircleCheck size={18} /> : <IconAlertTriangle size={18} />}
              </ThemeIcon>
              <Stack className={classes.matchingFeedbackBody} gap={2}>
                <Text fw={650}>{roleLabel}{choice ? <>: <span lang="da">{choice}</span></> : ": no selection"}</Text>
                <Text size="sm">{result.feedback}</Text>
              </Stack>
            </Group>
          </Paper>
        );
      })}
    </Stack>
  );
}

function showDanishUnknownDisclaimer(attempt: Attempt) {
  return attempt.linguisticStatus === "unknown"
    && (attempt.linguisticScope == null
      || attempt.linguisticScope === "danish-sentence"
      || attempt.linguisticScope === "danish-word-form");
}

function wordFormFeedbackTitle(attempt: Attempt, targetMet: boolean, goodWork: boolean) {
  if (attempt.linguisticScope !== "danish-word-form") return null;
  if (targetMet && !goodWork) return "Word identified; use the taught spelling.";
  if (attempt.linguisticStatus === "known-issue" && attempt.responseConstraintStatus === "not-met") {
    return "Review the written form.";
  }
  return null;
}

export default function FeedbackPanel({ attempt, exercise, focusRef, supportDescription, tileResponse = false }: {
  attempt: Attempt;
  exercise?: Exercise;
  focusRef?: Ref<HTMLDivElement>;
  supportDescription?: string;
  tileResponse?: boolean;
}) {
  const scope = attempt.linguisticScope;
  const met = attempt.targetSatisfaction === "met";
  const matching = scope === "danish-matching";
  const gaps = scope === "danish-gaps";
  const requiresMeaning = ["danish-sentence", "danish-choice", "danish-matching"].includes(scope ?? "");
  const requiresConstraint = requiresMeaning || gaps;
  const constraintMet = requiresConstraint
    ? attempt.responseConstraintStatus === "met"
    : attempt.responseConstraintStatus == null || attempt.responseConstraintStatus === "met";
  const meaningSatisfaction = "meaningSatisfaction" in attempt ? attempt.meaningSatisfaction : undefined;
  const meaningMet = requiresMeaning
    ? meaningSatisfaction === "met"
    : meaningSatisfaction == null || meaningSatisfaction === "met";
  const goodWork = attempt.linguisticScope
    ? met && meaningMet && attempt.linguisticStatus === "acceptable" && constraintMet
    : met;
  const identifiedHeadword = scope === "danish-headword" && met && !goodWork;
  const identifiedWritingTarget = scope === "danish-sentence" && met && !goodWork;
  const gapSupportNeedsReview = gaps && met && !goodWork;
  const wordFormTitle = wordFormFeedbackTitle(attempt, met, goodWork);
  const matchingDetails = matchingEvidence(attempt, exercise);
  const gapDetails = gapEvidence(attempt, exercise);
  return (
    <Alert
      ref={focusRef}
      className={classes.feedbackAlert}
      tabIndex={-1}
      color={goodWork ? "teal" : "orange"}
      variant="light"
      icon={goodWork ? <IconCircleCheck size={20} /> : <IconAlertTriangle size={20} />}
      title={<Text c="var(--mantine-color-text)" fw={650}>
        {feedbackTitle(
          goodWork, identifiedHeadword, identifiedWritingTarget, gapSupportNeedsReview,
          wordFormTitle,
        )}
      </Text>}
      role="status"
    >
      <Stack gap="xs">
        <Group className={classes.feedbackBadges} gap="xs">
          <Badge className={classes.feedbackBadge} variant="default">
            {matching ? "Four lexical meanings" : targetLabel(attempt.targetSatisfaction)}
          </Badge>
          {meaningSatisfaction && (
            <Badge className={classes.feedbackBadge} variant="default">
              {meaningLabel(meaningSatisfaction)}
            </Badge>
          )}
          <Badge className={classes.feedbackBadge} variant="default">
            {attempt.linguisticScope ? lexicalLanguageLabel(attempt) : languageLabel(attempt.linguisticStatus)}
          </Badge>
          {(["danish-sentence", "danish-choice", "danish-word-form"].includes(scope ?? "") || matching || gaps)
            && attempt.responseConstraintStatus === "met" && (
            <Badge className={classes.feedbackBadge} variant="default">
              {metConstraintLabel(scope, tileResponse)}
            </Badge>
          )}
          {attempt.responseConstraintStatus === "not-met" && (
            <Badge className={classes.feedbackBadge} color="orange" variant="light">
              {failedConstraintLabel(scope, tileResponse)}
            </Badge>
          )}
        </Group>
        <Text>{attempt.feedback}</Text>
        {matchingDetails && <MatchingFeedback {...matchingDetails} />}
        {gapDetails && <GapFeedback {...gapDetails} />}
        {attempt.completedSentence && <Text lang="da" fw={650}>{attempt.completedSentence}</Text>}
        {showDanishUnknownDisclaimer(attempt) && (
          <Text size="sm">This answer is outside the pilot’s licensed patterns. It is not a general judgment about Danish.</Text>
        )}
        {attempt.issues.map((issue) => <Text size="sm" key={issue.code}>{issue.message}</Text>)}
        {(matching || gaps) && attempt.retryNumber > 1 && <Text size="xs" fw={650}>Retry after feedback</Text>}
        <Text size="xs" c="var(--mantine-color-text)">
          Try {attempt.retryNumber} · {supportDescription ?? (attempt.hintUsed ? "Supported practice" : "No support opened")}
        </Text>
      </Stack>
    </Alert>
  );
}
