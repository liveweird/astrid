import { Badge, Box, Button, Divider, Group, Paper, Stack, Text, Title } from "@mantine/core";
import type { Attempt, GapSelection, PracticeSession } from "../../api/models";
import ExercisePrompt from "../../components/ExercisePrompt";
import FeedbackPanel from "../../components/FeedbackPanel";
import classes from "../../theme.module.css";
import { AttemptContextNote, PracticeHint } from "./PracticeMessages";
import SupportSections, { type PracticeSupportKind } from "./SupportSections";

function itemSupportDescription(attempt: Attempt) {
  return attempt.hintUsed ? "Item lesson or hint opened" : "No item lesson or hint opened";
}

const TARGET_SCOPE_LABELS: Record<string, string> = {
  "human-singular": "One-person subject words",
  "human-group": "Group subject words",
  "vaere-present": "Present verb form",
  "inanimate-singular-subject": "One-thing reference words",
  "named-three-verbs-present": "Present forms of three verbs",
  "subject-first-supplied": "Subject-first statement order",
  "have-possession-present": "Present possession with har",
  "personal-introduction-present": "Present forms for personal details",
  "kan-bare-infinitive": "Verb form after kan",
  "modal-volition-meaning": "Intention in context",
  "modal-obligation-meaning": "Requirement in context",
  "modal-permission-meaning": "Permission in context",
  "supplied-modal-order": "Supplied-modal statement order",
  "one-final-circumstance": "Time or place at the end",
  "neutral-positive-yes-no": "Neutral yes/no question order",
};

function targetBadge(exercise: NonNullable<PracticeSession["currentExercise"]>) {
  const scope = "targetScopeId" in exercise ? exercise.targetScopeId : undefined;
  return (scope ? TARGET_SCOPE_LABELS[scope] : undefined) ?? exercise.targetSkillId ?? "Vocabulary";
}

type ActivePracticeProps = {
  session: PracticeSession;
  blocked: boolean;
  busy: boolean;
  text: string;
  order: string[];
  gapSelections: GapSelection[];
  attempt: Attempt | null;
  feedbackRef: React.RefObject<HTMLDivElement | null>;
  gapFocusRef: React.RefObject<HTMLDivElement | null>;
  hintRef: React.RefObject<HTMLDivElement | null>;
  introductionRef: React.RefObject<HTMLParagraphElement | null>;
  introductionControlRef: React.RefObject<HTMLButtonElement | null>;
  itemRef: React.RefObject<HTMLHeadingElement | null>;
  lessonRef: React.RefObject<HTMLParagraphElement | null>;
  onText: (value: string) => void;
  onOrder: (value: string[]) => void;
  onGapSelections: (value: GapSelection[]) => void;
  onSupport: (kind: PracticeSupportKind, position?: number) => void;
  onSubmit: () => void;
  onAdvance: () => void;
  onRetry: () => void;
};

export default function ActivePractice({ session, blocked, busy, text, order, gapSelections, attempt, feedbackRef, gapFocusRef, hintRef, introductionRef, introductionControlRef, itemRef, lessonRef, onText, onOrder, onGapSelections, onSupport, onSubmit, onAdvance, onRetry }: ActivePracticeProps) {
  const exercise = session.currentExercise;
  const item = session.items.find((candidate) => candidate.position === session.cursor);
  if (!exercise || !item) return null;
  const answerReady = ["E003", "E024", "E029", "E030"].includes(exercise.formatId)
    ? order.length === exercise.prompt.blocks.length
    : exercise.formatId === "E025" ? order.length > 0
    : exercise.formatId === "E015" ? gapSelections.length > 0 : text.trim().length > 0;
  const introductionRequired = session.introductionRequired && !session.introductionOpened;
  if (introductionRequired) {
    return <SupportSections
      session={session}
      disabled={blocked}
      introductionOnly
      introductionContentRef={introductionRef}
      introductionControlRef={introductionControlRef}
      lessonContentRef={lessonRef}
      onSupport={onSupport}
    />;
  }
  return (
    <>
      <SupportSections session={session} disabled={blocked} introductionContentRef={introductionRef} introductionControlRef={introductionControlRef} lessonContentRef={lessonRef} onSupport={onSupport} />
      <Paper withBorder shadow="xs" className={classes.exerciseCard}>
        <Stack gap="lg">
          <Group justify="space-between"><Text fw={650}>Practice item {item.position + 1}</Text><Badge variant="light" color="astrid">{targetBadge(exercise)}</Badge></Group>
          <Divider />
          <Box><Title ref={itemRef} tabIndex={-1} order={2} size="h4">{exercise.instruction}</Title><Text c="dimmed" mt={4}>{exercise.context}</Text></Box>
          <ExercisePrompt
            exercise={exercise}
            text={text}
            order={order}
            gapSelections={gapSelections}
            gapFocusRef={gapFocusRef}
            disabled={blocked || Boolean(attempt)}
            onText={onText}
            onOrder={onOrder}
            onGapSelections={onGapSelections}
          />
          {item.hintOpened && <PracticeHint focusRef={hintRef} hint={exercise.hint} />}
          {attempt && <FeedbackPanel
            attempt={attempt}
            exercise={exercise}
            focusRef={feedbackRef}
            supportDescription={itemSupportDescription(attempt)}
            tileResponse={["E024", "E025", "E029", "E030"].includes(exercise.formatId)}
          />}
          <AttemptContextNote item={item} />
          <Group justify="space-between" className={classes.exerciseActions}>
            <Button
              variant="subtle"
              color="gray"
              disabled={blocked || item.hintOpened || (Boolean(attempt) && item.canAdvance !== false)}
              onClick={() => onSupport("hint", item.position)}
            >Show hint</Button>
            <Group className={classes.exercisePrimaryActions}>
              {attempt && <Button variant="default" disabled={blocked} onClick={onRetry}>Try again</Button>}
              {attempt
                ? <Button loading={busy} disabled={blocked || item.canAdvance === false} onClick={onAdvance}>Continue</Button>
                : <Button loading={busy} disabled={!answerReady || blocked} onClick={onSubmit}>
                    {exercise.formatId === "E015" ? "Check gaps" : "Check answer"}
                  </Button>}
            </Group>
          </Group>
          {attempt && item.canAdvance === false && (
            <Text size="sm" c="dimmed">This saved answer does not have a known outcome yet. Try again or open the lesson or hint for support.</Text>
          )}
        </Stack>
      </Paper>
    </>
  );
}
