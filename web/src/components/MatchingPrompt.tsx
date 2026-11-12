import { Button, Paper, SimpleGrid, Stack, Text } from "@mantine/core";
import { useMemo, useState, type Ref } from "react";
import type { MatchingColumns, MatchingPair } from "../api/models";
import classes from "../theme.module.css";

type LeftId = MatchingColumns["left"][number]["id"];
type RightId = MatchingColumns["right"][number]["id"];

function itemText<T extends { id: string; text: string }>(items: T[], id: T["id"]) {
  return items.find((item) => item.id === id)?.text ?? id;
}

export default function MatchingPrompt({ columns, pairs, disabled, focusRef, onPairs }: {
  columns: MatchingColumns;
  pairs: MatchingPair[];
  disabled: boolean;
  focusRef?: Ref<HTMLDivElement>;
  onPairs: (pairs: MatchingPair[]) => void;
}) {
  const [selectedLeftId, setSelectedLeftId] = useState<LeftId | null>(null);
  const pairedLeftIds = useMemo(() => new Set(pairs.map((pair) => pair.leftId)), [pairs]);
  const pairedRightIds = useMemo(() => new Set(pairs.map((pair) => pair.rightId)), [pairs]);
  const activeLeftId = selectedLeftId && !pairedLeftIds.has(selectedLeftId) ? selectedLeftId : null;

  const pairWith = (rightId: RightId) => {
    if (!activeLeftId || pairedRightIds.has(rightId)) return;
    onPairs([...pairs, { leftId: activeLeftId, rightId }]);
    setSelectedLeftId(null);
  };

  return (
    <Stack gap="md" ref={focusRef} tabIndex={-1} role="group" aria-label="Match English and Danish words">
      <Text size="sm" c="dimmed">Choose an English meaning, then choose its Danish match.</Text>
      <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="md">
        <Stack gap="xs" role="group" aria-label="English words">
          <Text fw={650}>English</Text>
          {columns.left.map((item) => (
            <Button
              key={item.id}
              type="button"
              variant={activeLeftId === item.id ? "filled" : "default"}
              disabled={disabled || pairedLeftIds.has(item.id)}
              aria-pressed={activeLeftId === item.id}
              onClick={() => setSelectedLeftId(item.id)}
            >
              <span lang="en">{item.text}</span>
            </Button>
          ))}
        </Stack>
        <Stack gap="xs" role="group" aria-label="Danish words">
          <Text fw={650}>Danish</Text>
          {columns.right.map((item) => (
            <Button
              key={item.id}
              type="button"
              variant="default"
              disabled={disabled || !activeLeftId || pairedRightIds.has(item.id)}
              onClick={() => pairWith(item.id)}
            >
              <span lang="da">{item.text}</span>
            </Button>
          ))}
        </Stack>
      </SimpleGrid>
      {pairs.length > 0 && (
        <Stack gap="xs" role="group" aria-label="Chosen matches">
          <Text fw={650}>Chosen matches</Text>
          {pairs.map((pair) => {
            const left = itemText(columns.left, pair.leftId);
            return (
              <Paper key={pair.leftId} withBorder p="xs" className={classes.matchingPair} role="group" aria-label={`Chosen match for ${left}`}>
                <Text lang="en">{left}</Text>
                <Text aria-hidden="true">
                  <span className={classes.matchingArrowWide}>→</span>
                  <span className={classes.matchingArrowNarrow}>↓</span>
                </Text>
                <Text lang="da">{itemText(columns.right, pair.rightId)}</Text>
                <Button
                  type="button"
                  size="compact-sm"
                  variant="subtle"
                  color="gray"
                  disabled={disabled}
                  aria-label={`Remove match for ${left}`}
                  onClick={() => onPairs(pairs.filter((candidate) => candidate.leftId !== pair.leftId))}
                >
                  Remove
                </Button>
              </Paper>
            );
          })}
        </Stack>
      )}
    </Stack>
  );
}
