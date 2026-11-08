import { Typography } from "@mantine/core";
import ReactMarkdown from "react-markdown";
import remarkGemoji from "remark-gemoji";
import remarkGfm from "remark-gfm";
import classes from "../theme.module.css";

export default function MarkdownView({ children }: { children: string }) {
  return (
    <Typography className={classes.markdown}>
      <ReactMarkdown remarkPlugins={[remarkGfm, remarkGemoji]}>{children}</ReactMarkdown>
    </Typography>
  );
}
