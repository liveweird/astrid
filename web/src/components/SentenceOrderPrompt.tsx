import { Button, Group, Paper, Stack, Text } from "@mantine/core";
import { useEffect, useRef } from "react";
import type { Exercise } from "../api/models";

type TileMode = "use-all" | "with-spares";

function orderedBlockText(value: string, index: number) {
  if (value === "I") return value;
  return index === 0 ? value.charAt(0).toLocaleUpperCase("da-DK") + value.slice(1) : value;
}

function fixedPunctuationFor(prompt: object) {
  if (!("fixedPunctuation" in prompt) || !Array.isArray(prompt.fixedPunctuation)) return new Map<string, string>();
  return new Map(prompt.fixedPunctuation.flatMap((item) => {
    if (typeof item !== "object" || item === null || !("beforeBlockId" in item) || !("text" in item) ||
      typeof item.beforeBlockId !== "string" || typeof item.text !== "string") return [];
    return [[item.beforeBlockId, item.text] as const];
  }));
}

export default function SentenceOrderPrompt({ prompt, order, disabled, tileMode, onOrder }: {
  prompt: Exclude<Exercise, { formatId: "E014" | "E015" }>["prompt"];
  order: string[];
  disabled: boolean;
  tileMode?: TileMode;
  onOrder: (value: string[]) => void;
}) {
  const availableRefs = useRef(new Map<string, HTMLButtonElement>());
  const chosenRefs = useRef(new Map<string, HTMLButtonElement>());
  const focusTarget = useRef<{ id: string; area: "available" | "chosen" } | null>(null);
  const chosen = new Set(order);
  const available = prompt.blocks.filter((block) => !chosen.has(block.id));
  const blocksById = new Map(prompt.blocks.map((block) => [block.id, block]));
  const fixedPunctuation = fixedPunctuationFor(prompt);
  const tileExercise = tileMode != null;

  useEffect(() => {
    const target = focusTarget.current;
    if (!target) return;
    const refs = target.area === "available" ? availableRefs.current : chosenRefs.current;
    refs.get(target.id)?.focus();
    focusTarget.current = null;
  }, [order]);

  return (
    <Stack gap="sm">
      {prompt.cue && (
        <Text lang={prompt.cueLanguage ?? "en"} size="xl" fw={650}>{prompt.cue}</Text>
      )}
      {tileExercise && (
        <Text size="sm" c="dimmed">
          {tileMode === "use-all" ? "Use every word tile." : "One word tile will be left unused."}
        </Text>
      )}
      <Paper
        withBorder
        p="md"
        mih={70}
        aria-label={tileExercise ? "Your Danish sentence" : "Your sentence"}
        aria-live="polite"
      >
        <Group gap="xs">
          {order.length === 0 && (
            <Text c="dimmed">
              {tileExercise ? "Choose Danish words in sentence order." : "Choose the parts in sentence order."}
            </Text>
          )}
          {order.map((id, index) => {
            const punctuation = fixedPunctuation.get(order[index + 1] ?? "");
            const block = (
              <Button
                key={id}
                ref={(node) => { if (node) chosenRefs.current.set(id, node); else chosenRefs.current.delete(id); }}
                variant="light"
                color="gray"
                disabled={disabled}
                onClick={() => {
                  focusTarget.current = { id, area: "available" };
                  onOrder(order.filter((candidate) => candidate !== id));
                }}
                aria-label={`Remove ${blocksById.get(id)?.text ?? "part"} from position ${index + 1}`}
              >
                <span lang="da">{orderedBlockText(blocksById.get(id)?.text ?? "", index)}</span>
              </Button>
            );
            return punctuation ? (
              <Group key={id} gap={0} wrap="nowrap" maw="100%" data-fixed-punctuation-unit>
                {block}
                <Text component="span" lang="en" aria-label={`Fixed punctuation ${punctuation}`}>
                  {punctuation}
                </Text>
              </Group>
            ) : block;
          })}
          {order.length > 0 && <Text lang="da">{prompt.punctuation}</Text>}
        </Group>
      </Paper>
      <Group
        gap="xs"
        role="group"
        aria-label={tileExercise ? "Available Danish word tiles" : "Available sentence parts"}
      >
        {available.map((block) => (
          <Button
            key={block.id}
            ref={(node) => {
              if (node) availableRefs.current.set(block.id, node); else availableRefs.current.delete(block.id);
            }}
            variant="default"
            disabled={disabled}
            onClick={() => {
              const next = available.find((candidate) => candidate.id !== block.id);
              focusTarget.current = next ? { id: next.id, area: "available" } : { id: block.id, area: "chosen" };
              onOrder([...order, block.id]);
            }}
          >
            <span lang="da">{block.text}</span>
          </Button>
        ))}
      </Group>
    </Stack>
  );
}
