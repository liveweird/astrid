import { Button, Group, Paper, Stack, Text, TextInput, VisuallyHidden } from "@mantine/core";
import { useEffect, useLayoutEffect, useRef, useState } from "react";
import type { Exercise } from "../api/models";
import classes from "../theme.module.css";

type Props = {
  exercise: Exercise;
  text: string;
  order: string[];
  disabled: boolean;
  onText: (value: string) => void;
  onOrder: (value: string[]) => void;
};

function orderedBlockText(value: string, index: number) {
  if (value === "I") return value;
  const first = index === 0 ? value.charAt(0).toLocaleUpperCase("da-DK") : value.charAt(0).toLocaleLowerCase("da-DK");
  return first + value.slice(1);
}

const DANISH_CHARACTERS = ["æ", "ø", "å"] as const;

function DanishTextInput({
  label, text, disabled, onText,
}: {
  label: string;
  text: string;
  disabled: boolean;
  onText: (value: string) => void;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const pendingCaret = useRef<number | null>(null);
  const [caretRequest, setCaretRequest] = useState(0);

  useLayoutEffect(() => {
    const caret = pendingCaret.current;
    if (caret === null) return;
    inputRef.current?.focus();
    inputRef.current?.setSelectionRange(caret, caret);
    pendingCaret.current = null;
  }, [caretRequest, text]);

  const insertCharacter = (character: string) => {
    const input = inputRef.current;
    const selectionStart = input?.selectionStart ?? text.length;
    const selectionEnd = input?.selectionEnd ?? selectionStart;
    pendingCaret.current = selectionStart + character.length;
    onText(`${text.slice(0, selectionStart)}${character}${text.slice(selectionEnd)}`);
    setCaretRequest((current) => current + 1);
  };

  return (
    <Stack gap="xs">
      <TextInput
        ref={inputRef}
        label={label}
        description="Enter one word"
        value={text}
        disabled={disabled}
        onChange={(event) => onText(event.currentTarget.value)}
        autoComplete="off"
        lang="da"
        maw={220}
      />
      <Group gap="xs" aria-label="Insert a Danish character">
        {DANISH_CHARACTERS.map((character) => (
          <Button
            key={character}
            type="button"
            size="compact-sm"
            variant="default"
            disabled={disabled}
            aria-label={`Insert ${character}`}
            onClick={() => insertCharacter(character)}
          >
            <span lang="da">{character}</span>
          </Button>
        ))}
      </Group>
    </Stack>
  );
}

export default function ExercisePrompt({ exercise, text, order, disabled, onText, onOrder }: Props) {
  const { prompt, formatId } = exercise;
  const { inputLabel, replacementWord } = prompt;
  const availableRefs = useRef(new Map<string, HTMLButtonElement>());
  const chosenRefs = useRef(new Map<string, HTMLButtonElement>());
  const focusTarget = useRef<{ id: string; area: "available" | "chosen" } | null>(null);

  useEffect(() => {
    const target = focusTarget.current;
    if (!target) return;
    const refs = target.area === "available" ? availableRefs.current : chosenRefs.current;
    refs.get(target.id)?.focus();
    focusTarget.current = null;
  }, [order]);

  if (formatId === "E003") {
    const chosen = new Set(order);
    const available = prompt.blocks.filter((block) => !chosen.has(block.id));
    const blocksById = new Map(prompt.blocks.map((block) => [block.id, block]));
    return (
      <Stack gap="sm">
        <Paper withBorder p="md" mih={70} aria-label="Your sentence" aria-live="polite">
          <Group gap="xs">
            {order.length === 0 && <Text c="dimmed">Choose the parts in sentence order.</Text>}
            {order.map((id, index) => (
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
            ))}
            {order.length > 0 && <Text lang="da">{prompt.punctuation}</Text>}
          </Group>
        </Paper>
        <Group gap="xs" aria-label="Available sentence parts">
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

  if (prompt.choices.length > 0) {
    return (
      <Group gap="sm" aria-label="Danish sentence with answer choices" lang="da">
        {prompt.before && <Text>{prompt.before}</Text>}
        <Group gap="xs" aria-label="Answer choices">
          {prompt.choices.map((choice) => (
            <Button
              key={choice}
              variant={text === choice ? "filled" : "default"}
              disabled={disabled}
              onClick={() => onText(choice)}
              aria-pressed={text === choice}
            >
              {choice}
            </Button>
          ))}
        </Group>
        {prompt.after && <Text>{prompt.after}</Text>}
      </Group>
    );
  }

  if (replacementWord) {
    return (
      <Stack gap="sm">
        <BoxedRepairSentence before={prompt.before} word={replacementWord} after={prompt.after} />
        <DanishTextInput
          label={inputLabel ?? "Missing word"}
          text={text}
          disabled={disabled}
          onText={onText}
        />
      </Stack>
    );
  }

  return (
    <Group gap="xs" align="end" wrap="wrap" aria-label="Danish sentence cloze">
      {prompt.before && <Text lang="da" pb={9}>{prompt.before}</Text>}
      <DanishTextInput
        label={inputLabel ?? "Missing word"}
        text={text}
        disabled={disabled}
        onText={onText}
      />
      {prompt.after && <Text lang="da" pb={9}>{prompt.after}</Text>}
    </Group>
  );
}

function BoxedRepairSentence({ before, word, after }: { before: string; word: string; after: string }) {
  return (
    <Paper withBorder p="md" role="group" aria-label="Danish sentence to repair">
      <Text size="sm" fw={650} mb="xs">Word to replace</Text>
      <Text lang="da" className={classes.repairSentence}>
        {before}
        <VisuallyHidden lang="en">Word to replace: </VisuallyHidden>
        <span className={classes.replacementWord}>{word}</span>
        {after}
      </Text>
    </Paper>
  );
}
