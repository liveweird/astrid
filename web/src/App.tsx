import {
  ActionIcon, AppShell, Box, Burger, Divider, Group, Indicator, Loader, NavLink, ScrollArea, Text, Tooltip,
  useComputedColorScheme, useMantineColorScheme,
} from "@mantine/core";
import { useDisclosure, useMediaQuery } from "@mantine/hooks";
import {
  IconBook2, IconFlask, IconHistory, IconLayoutSidebarLeftCollapse, IconLayoutSidebarLeftExpand, IconMoon, IconSun,
} from "@tabler/icons-react";
import { lazy, Suspense, useEffect, useState } from "react";
import { Link as RouterLink, Route, Routes, useLocation, useNavigationType } from "react-router-dom";
import BrandMark from "./components/BrandMark";
import ChangelogErrorBoundary from "./components/ChangelogErrorBoundary";
import VersionStamp from "./components/VersionStamp";
import { useChangelogUnseen } from "./hooks/useChangelogSeen";
import Playground from "./pages/playground/Playground";
import Practice from "./pages/practice/Practice";
import classes from "./theme.module.css";

const Changelog = lazy(() => import("./pages/Changelog"));

function storedRail() {
  try {
    return localStorage.getItem("astrid.viewSettings.appShell.navCollapsed") === "true";
  } catch {
    return false;
  }
}

function persistRail(collapsed: boolean) {
  try {
    localStorage.setItem("astrid.viewSettings.appShell.navCollapsed", String(collapsed));
  } catch {
    // View settings remain usable for this page when browser storage is unavailable.
  }
}

interface NavigationProps {
  changelog: boolean;
  changelogUnseen: boolean;
  close: () => void;
  practice: boolean;
  rail: boolean;
}

function PrimaryNavigation({ changelog, changelogUnseen, close, practice, rail }: NavigationProps) {
  const playgroundLink = {
    label: rail ? undefined : "Engine playground",
    leftSection: <IconFlask size={19} stroke={1.6} />,
    "aria-label": "Engine playground",
    className: rail ? classes.railLink : undefined,
    onClick: close,
  };
  return (
    <>
      <AppShell.Section grow component={ScrollArea}>
        {!rail && <Text size="xs" fw={650} c="dimmed" px="sm" py="xs">Learn Danish</Text>}
        {practice || changelog ? (
          <NavLink {...playgroundLink} component={RouterLink} to="/" />
        ) : (
          // Already on the playground: a plain fragment link moves focus to the main landmark.
          <NavLink {...playgroundLink} active href="#main-content" />
        )}
        <NavLink
          component={RouterLink}
          to="/practice"
          active={practice}
          label={rail ? undefined : "Practice"}
          leftSection={<IconBook2 size={19} stroke={1.6} />}
          aria-label="Practice"
          className={rail ? classes.railLink : undefined}
          onClick={close}
        />
        <NavLink
          component={RouterLink}
          to="/changelog"
          active={changelog}
          label={rail ? undefined : "Changelog"}
          leftSection={(
            <Indicator
              disabled={!changelogUnseen}
              size={7}
              color="red"
              offset={2}
              title={changelogUnseen ? "What's new" : undefined}
            >
              <IconHistory size={19} stroke={1.6} />
            </Indicator>
          )}
          aria-label="Changelog"
          className={rail ? classes.railLink : undefined}
          onClick={close}
        />
      </AppShell.Section>
      <AppShell.Section>
        <Divider mb="xs" />
        <Box ta="center" py={4}>
          <VersionStamp href="/changelog" compact={rail} className={classes.versionStamp} />
        </Box>
      </AppShell.Section>
    </>
  );
}

/**
 * Practice recovers its run once, on mount, from the URL and browser storage. Its own URL binding
 * (`?session=<id>`) replaces the entry and must keep the mounted run; a pushed navigation to
 * /practice (the nav link) starts recovery afresh, as the former full page load did. Fragment
 * links (skip link) arrive as POP and keep the run too.
 */
function PracticeRoute() {
  const { key } = useLocation();
  const navigationType = useNavigationType();
  const [mount, setMount] = useState({ location: key, instance: key });
  if (mount.location !== key) {
    setMount({ location: key, instance: navigationType === "PUSH" ? key : mount.instance });
  }
  return <Practice key={mount.instance} />;
}

function RouteContent() {
  return (
    <Routes>
      <Route path="/" element={<Playground />} />
      <Route path="/practice" element={<PracticeRoute />} />
      <Route
        path="/changelog"
        element={(
          <ChangelogErrorBoundary>
            <Suspense fallback={<Group justify="center" py="xl"><Loader aria-label="Loading changelog" /></Group>}>
              <Changelog />
            </Suspense>
          </ChangelogErrorBoundary>
        )}
      />
      {/* Unknown paths keep today's fallback: the playground, without a redirect. */}
      <Route path="*" element={<Playground />} />
    </Routes>
  );
}

export default function App() {
  const [opened, { toggle, close }] = useDisclosure();
  const [collapsed, setCollapsed] = useState(storedRail);
  const desktop = useMediaQuery("(min-width: 48em)");
  const { setColorScheme } = useMantineColorScheme();
  const colorScheme = useComputedColorScheme("light", { getInitialValueInEffect: false });
  const rail = collapsed && (desktop || !opened);
  const path = useLocation().pathname;
  const practice = path === "/practice";
  const changelog = path === "/changelog";
  const changelogUnseen = useChangelogUnseen();
  useEffect(() => {
    if (desktop) close();
  }, [close, desktop]);
  const changeRail = () => {
    const next = !collapsed;
    setCollapsed(next);
    persistRail(next);
  };

  return (
    <AppShell
      header={{ height: "3rem" }}
      navbar={{ width: { base: 240, sm: rail ? 64 : 240 }, breakpoint: "sm", collapsed: { mobile: !opened } }}
      padding="md"
    >
      <AppShell.Header>
        <a href="#main-content" className="skip-link">
          {changelog ? "Skip to main content" : "Skip to learning exercise"}
        </a>
        <Group h="3rem" px={{ base: "xs", sm: "md" }} justify="space-between" wrap="nowrap">
          <Group gap="sm" wrap="nowrap">
            <Burger
              opened={opened}
              onClick={toggle}
              hiddenFrom="sm"
              size="sm"
              aria-label="Toggle navigation"
              aria-expanded={opened}
              aria-controls="primary-navigation"
            />
            <Tooltip label={collapsed ? "Expand navigation" : "Collapse navigation"} position="right">
              <ActionIcon
                visibleFrom="sm"
                variant="subtle"
                color="gray"
                onClick={changeRail}
                aria-label={collapsed ? "Expand navigation" : "Collapse navigation"}
                aria-expanded={!collapsed}
                aria-controls="primary-navigation"
              >
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

      <AppShell.Navbar id="primary-navigation" p="xs" data-mobile-opened={opened || undefined}>
        <PrimaryNavigation
          changelog={changelog}
          changelogUnseen={changelogUnseen}
          close={close}
          practice={practice}
          rail={rail}
        />
      </AppShell.Navbar>

      <AppShell.Main id="main-content" tabIndex={-1}>
        <RouteContent />
      </AppShell.Main>
    </AppShell>
  );
}
