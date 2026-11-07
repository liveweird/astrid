import {
  ActionIcon, AppShell, Box, Burger, Divider, Group, NavLink, ScrollArea, Text, Tooltip,
  useComputedColorScheme, useMantineColorScheme,
} from "@mantine/core";
import { useDisclosure } from "@mantine/hooks";
import {
  IconFlask, IconLayoutSidebarLeftCollapse, IconLayoutSidebarLeftExpand, IconMoon, IconSun,
} from "@tabler/icons-react";
import { useState } from "react";
import Playground from "./Playground";
import BrandMark from "./components/BrandMark";
import classes from "./theme.module.css";

function storedRail() {
  return localStorage.getItem("astrid.viewSettings.appShell.navCollapsed") === "true";
}

export default function App() {
  const [opened, { toggle, close }] = useDisclosure();
  const [collapsed, setCollapsed] = useState(storedRail);
  const { setColorScheme } = useMantineColorScheme();
  const colorScheme = useComputedColorScheme("light", { getInitialValueInEffect: false });
  const rail = collapsed && !opened;
  const changeRail = () => {
    const next = !collapsed;
    setCollapsed(next);
    localStorage.setItem("astrid.viewSettings.appShell.navCollapsed", String(next));
  };

  return (
    <AppShell
      header={{ height: "3rem" }}
      navbar={{ width: { base: 240, sm: rail ? 64 : 240 }, breakpoint: "sm", collapsed: { mobile: !opened } }}
      padding="md"
    >
      <AppShell.Header>
        <a href="#main-content" className="skip-link">Skip to learning exercise</a>
        <Group h="3rem" px={{ base: "xs", sm: "md" }} justify="space-between" wrap="nowrap">
          <Group gap="sm" wrap="nowrap">
            <Burger opened={opened} onClick={toggle} hiddenFrom="sm" size="sm" aria-label="Toggle navigation" />
            <Tooltip label={collapsed ? "Expand navigation" : "Collapse navigation"} position="right">
              <ActionIcon visibleFrom="sm" variant="subtle" color="gray" onClick={changeRail} aria-label="Toggle navigation width">
                {collapsed ? <IconLayoutSidebarLeftExpand size={18} /> : <IconLayoutSidebarLeftCollapse size={18} />}
              </ActionIcon>
            </Tooltip>
            <BrandMark />
          </Group>
          <Tooltip label={colorScheme === "dark" ? "Use light theme" : "Use dark theme"}>
            <ActionIcon
              variant="subtle"
              color="gray"
              aria-label={colorScheme === "dark" ? "Use light theme" : "Use dark theme"}
              onClick={() => setColorScheme(colorScheme === "dark" ? "light" : "dark")}
            >
              {colorScheme === "dark" ? <IconSun size={19} /> : <IconMoon size={19} />}
            </ActionIcon>
          </Tooltip>
        </Group>
      </AppShell.Header>

      <AppShell.Navbar p="xs">
        <AppShell.Section grow component={ScrollArea}>
          {!rail && <Text size="xs" fw={650} c="dimmed" px="sm" py="xs">Learn Danish</Text>}
          <NavLink
            active
            label={rail ? undefined : "Engine playground"}
            leftSection={<IconFlask size={19} stroke={1.6} />}
            aria-label="Engine playground"
            className={rail ? classes.railLink : undefined}
            onClick={close}
          />
        </AppShell.Section>
        <AppShell.Section>
          <Divider mb="xs" />
          {rail ? <Box ta="center"><BrandMark compact /></Box> : (
            <Text size="xs" c="dimmed" ta="center" py={4}>Pilot · reading and writing</Text>
          )}
        </AppShell.Section>
      </AppShell.Navbar>

      <AppShell.Main id="main-content" tabIndex={-1}>
        <Playground />
      </AppShell.Main>
    </AppShell>
  );
}
