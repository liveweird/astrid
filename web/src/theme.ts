import { AppShell, createTheme, type CSSVariablesResolver, type MantineColorsTuple, NavLink } from "@mantine/core";
import classes from "./theme.module.css";

const astrid: MantineColorsTuple = [
  "#fff0f1", "#ffdfe3", "#ffbcc4", "#fa8998", "#ee5b70",
  "#d93650", "#b21f36", "#96192d", "#7c1728", "#681623",
];

const font = "'Inter Variable', -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif";

export const theme = createTheme({
  primaryColor: "astrid",
  primaryShade: { light: 7, dark: 6 },
  autoContrast: true,
  respectReducedMotion: true,
  defaultRadius: "md",
  colors: { astrid },
  fontFamily: font,
  headings: {
    fontFamily: font,
    fontWeight: "650",
    sizes: {
      h1: { fontSize: "1.625rem", lineHeight: "1.3" },
      h2: { fontSize: "1.3rem", lineHeight: "1.35" },
      h3: { fontSize: "1.1rem", lineHeight: "1.4" },
    },
  },
  components: {
    AppShell: AppShell.extend({
      classNames: { main: classes.appMain, header: classes.appHeader, navbar: classes.appNavbar },
    }),
    NavLink: NavLink.extend({ classNames: { root: classes.navLink } }),
  },
});

export const cssVariablesResolver: CSSVariablesResolver = () => ({
  variables: {},
  light: { "--mantine-color-dimmed": "#5f6b76" },
  dark: { "--mantine-color-dimmed": "#a3a3a3" },
});
