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

export default function ActivePractice({ session, blocked, busy, text, order, gapSelections, attempt, feedbackRef, gapFocusRef, hintRef, introductionRef, itemRef, lessonRef, onText, onOrder, onGapSelections, onSupport, onSubmit, onAdvance, onRetry }: ActivePracticeProps) {
  const exercise = session.currentExercise;
  const item = session.items.find((candidate) => candidate.position === session.cursor);
  if (!exercise || !item) return null;
  const answerReady = exercise.formatId === "E003"
    ? order.length === exercise.prompt.blocks.length
    : exercise.formatId === "E015" ? gapSelections.length > 0 : text.trim().length > 0;
  return (
    <>
      <SupportSections session={session} disabled={blocked} introductionContentRef={introductionRef} lessonContentRef={lessonRef} onSupport={onSupport} />
      <Paper withBorder shadow="xs" className={classes.exerciseCard}>
        <Stack gap="lg">
          <Group justify="space-between"><Text fw={650}>Practice item {item.position + 1}</Text><Badge variant="light" color="astrid">{exercise.targetSkillId ?? "Vocabulary"}</Badge></Group>
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
          {attempt && <FeedbackPanel attempt={attempt} exercise={exercise} focusRef={feedbackRef} supportDescription={itemSupportDescription(attempt)} />}
          <AttemptContextNote item={item} />
          <Group justify="space-between" className={classes.exerciseActions}>
            <Button variant="subtle" color="gray" disabled={blocked || item.hintOpened || Boolean(attempt)} onClick={() => onSupport("hint", item.position)}>Show hint</Button>
            <Group className={classes.exercisePrimaryActions}>
              {attempt && <Button variant="default" disabled={blocked} onClick={onRetry}>Try again</Button>}
              {attempt
                ? <Button loading={busy} disabled={blocked} onClick={onAdvance}>Continue</Button>
                : <Button loading={busy} disabled={!answerReady || blocked} onClick={onSubmit}>
                    {exercise.formatId === "E015" ? "Check gaps" : "Check answer"}
                  </Button>}
            </Group>
          </Group>
        </Stack>
      </Paper>
    </>
  );
}
