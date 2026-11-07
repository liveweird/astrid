import { Typography } from "@mantine/core";
import ReactMarkdown from "react-markdown";
import remarkGemoji from "remark-gemoji";
import remarkGfm from "remark-gfm";

export default function MarkdownView({ children }: { children: string }) {
  return (
    <Typography>
      <ReactMarkdown remarkPlugins={[remarkGfm, remarkGemoji]}>{children}</ReactMarkdown>
    </Typography>
  );
}
