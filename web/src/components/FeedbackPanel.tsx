import { Alert, Badge, Group, Stack, Text } from "@mantine/core";
import { IconAlertTriangle, IconCircleCheck } from "@tabler/icons-react";
import type { Ref } from "react";
import type { Attempt } from "../api/models";
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
  if (attempt.linguisticScope === "english-choice") {
    return attempt.linguisticStatus === "acceptable"
      ? "Recognized English choice"
      : "Unrecognized English choice";
  }
  if (attempt.linguisticScope === "danish-headword" && attempt.linguisticStatus === "acceptable") {
    return "Recognized Danish headword";
  }
  if (attempt.linguisticScope === "danish-choice") {
    return attempt.linguisticStatus === "acceptable"
      ? "Recognized Danish choice"
      : "Unrecognized Danish choice";
  }
  return languageLabel(attempt.linguisticStatus);
}

function feedbackTitle(goodWork: boolean, identifiedHeadword: boolean, identifiedWritingTarget: boolean) {
  if (goodWork) return "Good work";
  if (identifiedHeadword) return "Headword identified; adjust your answer.";
  if (identifiedWritingTarget) return "Target met; adjust the rest of your answer.";
  return "Look at the target once more";
}

function failedConstraintLabel(scope: Attempt["linguisticScope"], tileResponse: boolean) {
  if (tileResponse) return "Response constraint to revisit";
  if (scope === "danish-headword") return "One-word response required";
  if (scope === "danish-sentence") return "One statement required";
  return "Supplied choice required";
}

function metConstraintLabel(scope: Attempt["linguisticScope"], tileResponse: boolean) {
  if (tileResponse) return "Response constraint met";
  if (scope === "danish-sentence") return "One statement supplied";
  return "Supplied choice used";
}

export default function FeedbackPanel({ attempt, focusRef, supportDescription, tileResponse = false }: {
  attempt: Attempt;
  focusRef?: Ref<HTMLDivElement>;
  supportDescription?: string;
  tileResponse?: boolean;
}) {
  const met = attempt.targetSatisfaction === "met";
  const writing = attempt.linguisticScope === "danish-sentence";
  const requiresAllFacets = writing || attempt.linguisticScope === "danish-choice";
  const constraintMet = requiresAllFacets
    ? attempt.responseConstraintStatus === "met"
    : attempt.responseConstraintStatus == null || attempt.responseConstraintStatus === "met";
  const meaningMet = requiresAllFacets
    ? attempt.meaningSatisfaction === "met"
    : attempt.meaningSatisfaction == null || attempt.meaningSatisfaction === "met";
  const goodWork = attempt.linguisticScope
    ? met && meaningMet && attempt.linguisticStatus === "acceptable" && constraintMet
    : met;
  const identifiedHeadword = attempt.linguisticScope === "danish-headword" && met && !goodWork;
  const identifiedWritingTarget = attempt.linguisticScope === "danish-sentence" && met && !goodWork;
  return (
    <Alert
      ref={focusRef}
      className={classes.feedbackAlert}
      tabIndex={-1}
      color={goodWork ? "teal" : "orange"}
      variant="light"
      icon={goodWork ? <IconCircleCheck size={20} /> : <IconAlertTriangle size={20} />}
      title={<Text c="var(--mantine-color-text)" fw={650}>
        {feedbackTitle(goodWork, identifiedHeadword, identifiedWritingTarget)}
      </Text>}
      role="status"
    >
      <Stack gap="xs">
        <Group className={classes.feedbackBadges} gap="xs">
          <Badge className={classes.feedbackBadge} variant="default">{targetLabel(attempt.targetSatisfaction)}</Badge>
          {attempt.meaningSatisfaction && (
            <Badge className={classes.feedbackBadge} variant="default">
              {meaningLabel(attempt.meaningSatisfaction)}
            </Badge>
          )}
          <Badge className={classes.feedbackBadge} variant="default">
            {attempt.linguisticScope ? lexicalLanguageLabel(attempt) : languageLabel(attempt.linguisticStatus)}
          </Badge>
          {(attempt.linguisticScope === "danish-sentence" || attempt.linguisticScope === "danish-choice")
            && attempt.responseConstraintStatus === "met" && (
            <Badge className={classes.feedbackBadge} variant="default">
              {metConstraintLabel(attempt.linguisticScope, tileResponse)}
            </Badge>
          )}
          {attempt.responseConstraintStatus === "not-met" && (
            <Badge className={classes.feedbackBadge} color="orange" variant="light">
              {failedConstraintLabel(attempt.linguisticScope, tileResponse)}
            </Badge>
          )}
        </Group>
        <Text>{attempt.feedback}</Text>
        {attempt.completedSentence && <Text lang="da" fw={650}>{attempt.completedSentence}</Text>}
        {attempt.linguisticStatus === "unknown"
          && (attempt.linguisticScope == null || attempt.linguisticScope === "danish-sentence") && (
          <Text size="sm">This answer is outside the pilot’s licensed patterns. It is not a general judgment about Danish.</Text>
        )}
        {attempt.issues.map((issue) => <Text size="sm" key={issue.code}>{issue.message}</Text>)}
        <Text size="xs" c="var(--mantine-color-text)">
          Try {attempt.retryNumber} · {supportDescription ?? (attempt.hintUsed ? "Supported practice" : "No support opened")}
        </Text>
      </Stack>
    </Alert>
  );
}
