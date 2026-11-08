import { Button, Group, Paper, SimpleGrid, Stack, Text } from "@mantine/core";
import type { Ref } from "react";
import type { GapPromptData, GapSelection } from "../api/models";
import classes from "../theme.module.css";

function clearLabel(slotId: GapSelection["slotId"]) {
  return slotId === "g0" ? "Clear present verb selection" : "Clear article selection";
}

function slotRoleLabel(role: GapPromptData["slots"][number]["assessmentRole"]) {
  return role === "primary" ? "Present verb — Primary target" : "Indefinite article — Supporting grammar";
}

function selectedText(prompt: GapPromptData, selections: GapSelection[], slotId: GapSelection["slotId"]) {
  const selection = selections.find((candidate) => candidate.slotId === slotId);
  const slot = prompt.slots.find((candidate) => candidate.id === slotId);
  return slot?.choices.find((choice) => choice.id === selection?.choiceId)?.text;
}

export default function GapPrompt({ prompt, selections, disabled, focusRef, onSelections }: {
  prompt: GapPromptData;
  selections: GapSelection[];
  disabled: boolean;
  focusRef?: Ref<HTMLDivElement>;
  onSelections: (selections: GapSelection[]) => void;
}) {
  const choose = (selection: GapSelection) => {
    onSelections([...selections.filter((candidate) => candidate.slotId !== selection.slotId), selection]);
  };
  const clear = (slotId: GapSelection["slotId"]) => {
    onSelections(selections.filter((candidate) => candidate.slotId !== slotId));
  };

  return (
    <Stack ref={focusRef} tabIndex={-1} gap="md" role="group" aria-label="Present verb and article gaps">
      <Paper withBorder p="md" lang="da" className={classes.gapSentence}>
        <Text component="span">{prompt.segments[0]}</Text>
        <Text component="span" fw={650}>{selectedText(prompt, selections, "g0") ?? "…"}</Text>
        <Text component="span">{prompt.segments[1]}</Text>
        <Text component="span" fw={650}>{selectedText(prompt, selections, "g1") ?? "…"}</Text>
        <Text component="span">{prompt.segments[2]}</Text>
      </Paper>
      <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
        {prompt.slots.map((slot) => {
          const selected = selections.find((candidate) => candidate.slotId === slot.id);
          const roleLabel = slotRoleLabel(slot.assessmentRole);
          return (
            <Paper key={slot.id} withBorder p="md" className={classes.gapSlot}>
              <Stack gap="xs" role="group" aria-label={roleLabel}>
                <Text fw={650}>{roleLabel}</Text>
                <Group gap="xs">
                  {slot.choices.map((choice) => (
                    <Button
                      key={choice.id}
                      type="button"
                      variant={selected?.choiceId === choice.id ? "filled" : "default"}
                      disabled={disabled}
                      aria-pressed={selected?.choiceId === choice.id}
                      onClick={() => choose({ slotId: slot.id, choiceId: choice.id })}
                    >
                      <span lang="da">{choice.text}</span>
                    </Button>
                  ))}
                </Group>
                <Button
                  type="button"
                  variant="subtle"
                  color="gray"
                  size="compact-sm"
                  disabled={disabled || !selected}
                  onClick={() => clear(slot.id)}
                >
                  {clearLabel(slot.id)}
                </Button>
              </Stack>
            </Paper>
          );
        })}
      </SimpleGrid>
    </Stack>
  );
}
