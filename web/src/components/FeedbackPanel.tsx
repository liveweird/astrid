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

function lexicalLanguageLabel(attempt: Attempt) {
  if (attempt.linguisticScope === "english-choice") {
    return attempt.linguisticStatus === "acceptable"
      ? "Recognized English choice"
      : "Unrecognized English choice";
  }
  if (attempt.linguisticScope === "danish-headword" && attempt.linguisticStatus === "acceptable") {
    return "Recognized Danish headword";
  }
  return languageLabel(attempt.linguisticStatus);
}

export default function FeedbackPanel({ attempt, focusRef }: { attempt: Attempt; focusRef?: Ref<HTMLDivElement> }) {
  const met = attempt.targetSatisfaction === "met";
  const constraintMet = attempt.responseConstraintStatus == null || attempt.responseConstraintStatus === "met";
  const goodWork = attempt.linguisticScope
    ? met && attempt.linguisticStatus === "acceptable" && constraintMet
    : met;
  const identifiedHeadword = attempt.linguisticScope === "danish-headword" && met && !goodWork;
  return (
    <Alert
      ref={focusRef}
      className={classes.feedbackAlert}
      tabIndex={-1}
      color={goodWork ? "teal" : "orange"}
      variant="light"
      icon={goodWork ? <IconCircleCheck size={20} /> : <IconAlertTriangle size={20} />}
      title={<Text c="var(--mantine-color-text)" fw={650}>
        {goodWork ? "Good work" : identifiedHeadword ? "Headword identified; adjust your answer." : "Look at the target once more"}
      </Text>}
      role="status"
    >
      <Stack gap="xs">
        <Group className={classes.feedbackBadges} gap="xs">
          <Badge className={classes.feedbackBadge} variant="default">{targetLabel(attempt.targetSatisfaction)}</Badge>
          <Badge className={classes.feedbackBadge} variant="default">
            {attempt.linguisticScope ? lexicalLanguageLabel(attempt) : languageLabel(attempt.linguisticStatus)}
          </Badge>
          {attempt.responseConstraintStatus === "not-met" && (
            <Badge className={classes.feedbackBadge} color="orange" variant="light">
              {attempt.linguisticScope === "danish-headword" ? "One-word response required" : "Supplied choice required"}
            </Badge>
          )}
        </Group>
        <Text>{attempt.feedback}</Text>
        {attempt.completedSentence && <Text lang="da" fw={650}>{attempt.completedSentence}</Text>}
        {attempt.linguisticStatus === "unknown" && !attempt.linguisticScope && (
          <Text size="sm">This answer is outside the pilot’s licensed patterns. It is not a general judgment about Danish.</Text>
        )}
        {attempt.issues.map((issue) => <Text size="sm" key={issue.code}>{issue.message}</Text>)}
        <Text size="xs" c="var(--mantine-color-text)">
          Try {attempt.retryNumber}{attempt.hintUsed ? " · Supported practice" : " · No support opened"}
        </Text>
      </Stack>
    </Alert>
  );
}
