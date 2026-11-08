import { Group, Image, Text, useComputedColorScheme } from "@mantine/core";
import classes from "../theme.module.css";

/** The theme-aware Astrid mark (light/dark SVG variants from public/) with its wordmark. */
export default function BrandMark({ compact = false }: { compact?: boolean }) {
  const colorScheme = useComputedColorScheme("light", { getInitialValueInEffect: false });
  const src = colorScheme === "dark" ? "/logo-dark.svg" : "/logo-light.svg";
  return (
    <Group gap="xs" wrap="nowrap">
      {/* alt="" — decorative: the adjacent "Astrid" wordmark (or the rail's labelled link) names the brand. */}
      <Image src={src} alt="" w={28} h={28} />
      {!compact && (
        <Text fw={650} size="md" className={classes.narrowBrandLabel}>
          Astrid
        </Text>
      )}
    </Group>
  );
}
