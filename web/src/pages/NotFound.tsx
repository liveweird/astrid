import { Button, Stack, Text, Title } from "@mantine/core";
import { Link as RouterLink } from "react-router-dom";

export default function NotFound() {
  return (
    <Stack gap="sm" maw={560}>
      <Title order={1}>Page not found</Title>
      <Text c="dimmed">This address does not point to a lesson or practice page.</Text>
      <Button component={RouterLink} to="/" w="fit-content">Go to Lessons</Button>
    </Stack>
  );
}
