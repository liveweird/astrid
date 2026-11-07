import { Group, Image, Text } from "@mantine/core";
import classes from "../theme.module.css";

export default function BrandMark({ compact = false }: { compact?: boolean }) {
  return (
    <Group gap="xs" wrap="nowrap">
      <Image src="/astrid-mark.svg" alt="" w={28} h={28} />
      {!compact && (
        <Text fw={650} size="md" className={classes.narrowBrandLabel}>
          Astrid
        </Text>
      )}
    </Group>
  );
}
