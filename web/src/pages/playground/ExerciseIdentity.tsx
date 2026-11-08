import { Badge, Box, Group, Text } from "@mantine/core";
import type { Catalog, Exercise } from "../../api/models";

function lexicalTargetLabel(format: Catalog["formats"][number] | undefined) {
  const task = format?.lexicalTask;
  if (!task) return null;
  if (task.facet === "meaning" && task.direction === "da-en") return "Meaning · Danish → English";
  if (task.facet === "meaning" && task.direction === "en-da") return "Meaning · English → Danish";
  if (task.facet === "singular-headword" && task.direction === "en-da") {
    return "Singular headword · English → Danish";
  }
  return "Vocabulary target";
}

function readingTargetLabel(format: Catalog["formats"][number] | undefined) {
  if (format?.readingTask?.facet === "participant-action-complement") {
    return "Reading · participant, action and object";
  }
  return null;
}

export default function ExerciseIdentity({ catalog, exercise, format }: {
  catalog: Catalog | null;
  exercise: Exercise | null;
  format: Catalog["formats"][number] | undefined;
}) {
  const skill = catalog?.skills.find((candidate) => candidate.id === format?.targetSkillId);
  const lexicalLabel = lexicalTargetLabel(format);
  const readingLabel = readingTargetLabel(format);
  const lexicalTargetCount = exercise?.formatId === "E014" ? exercise.lexicalTargets.length : 0;
  return (
    <Group justify="space-between" align="start">
      <Box>
        <Text fw={650}>{format?.title ?? "Exercise"}</Text>
        {skill && <Text size="sm" c="dimmed">Target: {skill.title}</Text>}
        {lexicalLabel && <Text size="sm" c="dimmed">Target: {lexicalLabel}</Text>}
        {readingLabel && <Text size="sm" c="dimmed">Task: {readingLabel}</Text>}
      </Box>
      {lexicalTargetCount > 1
        ? <Badge color="astrid" variant="light">Four lexical meanings</Badge>
        : exercise?.targetSkillId && <Badge color="astrid" variant="light">{exercise.targetSkillId}</Badge>}
    </Group>
  );
}
