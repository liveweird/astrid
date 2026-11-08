import { Button } from "@mantine/core";

export type RecoveryRequired = "engine" | "session" | null;

export default function ErrorRecoveryActions({ recoveryRequired, hasExercise }: {
  recoveryRequired: RecoveryRequired; hasExercise: boolean;
}) {
  if (recoveryRequired === "session") {
    return <Button color="red" variant="light" onClick={() => window.location.reload()}>Reconnect session</Button>;
  }
  if (recoveryRequired === "engine") {
    return <Button color="red" variant="light" onClick={() => window.location.reload()}>Reload engine</Button>;
  }
  if (!hasExercise) {
    return <Button color="red" variant="light" onClick={() => window.location.reload()}>Try connection again</Button>;
  }
  return null;
}
