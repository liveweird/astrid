import { Group, Image, Text } from "@mantine/core";

export default function BrandMark({ compact = false }: { compact?: boolean }) {
  return (
    <Group gap="xs" wrap="nowrap">
      <Image src="/astrid-mark.svg" alt="" w={28} h={28} />
      {!compact && <Text fw={650} size="md">Astrid</Text>}
    </Group>
  );
}
