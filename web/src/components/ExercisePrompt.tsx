import { Button, Group, Paper, Stack, Text, TextInput } from "@mantine/core";
import type { Exercise } from "../api/models";

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

export default function ExercisePrompt({ exercise, text, order, disabled, onText, onOrder }: Props) {
  const { prompt, formatId } = exercise;
  if (formatId === "E003") {
    const chosen = new Set(order);
    const available = prompt.blocks.filter((block) => !chosen.has(block.id));
    const blocksById = new Map(prompt.blocks.map((block) => [block.id, block]));
    return (
      <Stack gap="sm">
        <Paper withBorder p="md" mih={70} aria-label="Your sentence">
          <Group gap="xs">
            {order.length === 0 && <Text c="dimmed">Choose the parts in sentence order.</Text>}
            {order.map((id, index) => (
              <Button
                key={id}
                variant="light"
                color="gray"
                disabled={disabled}
                onClick={() => onOrder(order.filter((candidate) => candidate !== id))}
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
            <Button key={block.id} variant="default" disabled={disabled} onClick={() => onOrder([...order, block.id])}>
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

  return (
    <Group gap="xs" align="end" wrap="nowrap" aria-label="Danish sentence cloze">
      {prompt.before && <Text lang="da" pb={9}>{prompt.before}</Text>}
      <TextInput
        label="Missing word"
        value={text}
        disabled={disabled}
        onChange={(event) => onText(event.currentTarget.value)}
        autoComplete="off"
        lang="da"
        maw={220}
      />
      {prompt.after && <Text lang="da" pb={9}>{prompt.after}</Text>}
    </Group>
  );
}
