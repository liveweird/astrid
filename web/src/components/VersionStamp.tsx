import { Text, type TextProps } from "@mantine/core";
import { APP_VERSION } from "../changelog/version";

export default function VersionStamp({
  href,
  compact,
  ...props
}: TextProps & { href?: string; compact?: boolean }) {
  const time = __APP_COMMIT_TIME__.slice(0, 16).replace("T", " ");
  const timeSuffix = time ? ` · ${time}` : "";
  const full = `v${APP_VERSION} · ${__APP_COMMIT__}${timeSuffix}`;
  const content = compact ? `v${APP_VERSION}` : full;
  const title = compact ? full : "Build version";
  const common = {
    td: "none" as const,
    size: "xs" as const,
    c: "dimmed",
    title,
    ...props,
  };
  if (href) return (
    <Text component="a" href={href} {...common}>
      {content}
    </Text>
  );
  return (
    <Text
      component="span"
      {...common}
    >
      {content}
    </Text>
  );
}
