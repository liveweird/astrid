import { Alert, Button, Center, Stack, Text } from "@mantine/core";
import { IconAlertTriangle } from "@tabler/icons-react";
import { Component, type ReactNode } from "react";

interface Props {
  children: ReactNode;
  reload?: () => void;
}

export default class ChangelogErrorBoundary extends Component<Props, { failed: boolean }> {
  state = { failed: false };

  static getDerivedStateFromError(): { failed: boolean } {
    return { failed: true };
  }

  render() {
    if (!this.state.failed) return this.props.children;
    return (
      <Center mih={200} p="xl">
        <Alert
          color="red"
          variant="light"
          icon={<IconAlertTriangle />}
          title="Changelog unavailable"
          maw={480}
        >
          <Stack gap="sm" align="flex-start">
            <Text size="sm">Astrid could not load the release notes. Reload to try again.</Text>
            <Button
              color="red"
              variant="light"
              onClick={this.props.reload ?? (() => window.location.reload())}
            >
              Reload changelog
            </Button>
          </Stack>
        </Alert>
      </Center>
    );
  }
}
