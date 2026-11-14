import { Center, Stack, Text, ThemeIcon } from "@mantine/core";
import type { Icon } from "@tabler/icons-react";

/** Narrowly adapted from Flow's shared EmptyState: same icon, label and optional-hint shape. */
export default function EmptyState({ icon: IconComponent, label, hint }: {
  icon: Icon;
  label: string;
  hint?: string;
}) {
  return (
    <Center py={48}>
      <Stack align="center" gap="sm">
        <ThemeIcon variant="light" color="gray" size={64} radius="xl">
          <IconComponent size={32} stroke={1.2} aria-hidden="true" />
        </ThemeIcon>
        <Text c="dimmed" size="sm" ta="center">{label}</Text>
        {hint && <Text c="dimmed" size="xs" ta="center">{hint}</Text>}
      </Stack>
    </Center>
  );
}
