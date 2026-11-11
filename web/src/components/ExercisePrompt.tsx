import { Button, Group, Paper, Stack, Text, Textarea, TextInput, VisuallyHidden } from "@mantine/core";
import { useLayoutEffect, useRef, useState, type Ref, type RefObject } from "react";
import type { Exercise, GapSelection, MatchingPair } from "../api/models";
import classes from "../theme.module.css";
import SentenceOrderPrompt from "./SentenceOrderPrompt";
import MatchingPrompt from "./MatchingPrompt";
import GapPrompt from "./GapPrompt";

type Props = {
  exercise: Exercise;
  text: string;
  order: string[];
  disabled: boolean;
  onText: (value: string) => void;
  onOrder: (value: string[]) => void;
  matchingPairs?: MatchingPair[];
  matchingFocusRef?: Ref<HTMLDivElement>;
  onMatchingPairs?: (value: MatchingPair[]) => void;
  gapSelections?: GapSelection[];
  gapFocusRef?: Ref<HTMLDivElement>;
  onGapSelections?: (value: GapSelection[]) => void;
};

const DANISH_CHARACTERS = ["æ", "ø", "å"] as const;

type DanishInputElement = HTMLInputElement | HTMLTextAreaElement;

function hasReadingTarget(exercise: Exercise) {
  return "readingTarget" in exercise && Boolean(exercise.readingTarget);
}

function hasLexicalTarget(exercise: Exercise) {
  return "lexicalTarget" in exercise && Boolean(exercise.lexicalTarget);
}

function choiceGroupLabel(formatId: Exercise["formatId"]) {
  return formatId === "E020"
    ? "Danish standalone reference word choices"
    : formatId === "E028" ? "English modal meaning choices"
    : formatId === "E026" ? "Danish verb after kan choices"
    : formatId === "E022" ? "Danish present verb choices"
    : "Danish sentence with answer choices";
}

function choiceGroupLanguage(formatId: Exercise["formatId"]) {
  return ["E020", "E028"].includes(formatId) ? "en" : "da";
}

function responseGroupLabel(formatId: Exercise["formatId"]) {
  return formatId === "E021"
    ? "Danish standalone reference word response"
    : formatId === "E027" ? "Danish verb after kan response"
    : "Danish sentence cloze";
}

function DanishCharacterButtons({ disabled, insertCharacter }: {
  disabled: boolean;
  insertCharacter: (character: string) => void;
}) {
  return (
    <Group gap="xs" aria-label="Insert a Danish character">
      {DANISH_CHARACTERS.map((character) => (
        <Button
          key={character}
          type="button"
          size="compact-sm"
          variant="default"
          disabled={disabled}
          aria-label={`Insert ${character}`}
          onClick={() => insertCharacter(character)}
        >
          <span lang="da">{character}</span>
        </Button>
      ))}
    </Group>
  );
}

function useDanishCharacterInsertion(
  inputRef: RefObject<DanishInputElement | null>,
  text: string,
  onText: (value: string) => void,
) {
  const pendingCaret = useRef<number | null>(null);
  const [caretRequest, setCaretRequest] = useState(0);

  useLayoutEffect(() => {
    const caret = pendingCaret.current;
    if (caret === null) return;
    inputRef.current?.focus();
    inputRef.current?.setSelectionRange(caret, caret);
    pendingCaret.current = null;
  }, [caretRequest, inputRef, text]);

  return (character: string) => {
    const input = inputRef.current;
    const selectionStart = input?.selectionStart ?? text.length;
    const selectionEnd = input?.selectionEnd ?? selectionStart;
    pendingCaret.current = selectionStart + character.length;
    onText(`${text.slice(0, selectionStart)}${character}${text.slice(selectionEnd)}`);
    setCaretRequest((current) => current + 1);
  };
}

function DanishTextInput({
  label, text, disabled, onText,
}: {
  label: string;
  text: string;
  disabled: boolean;
  onText: (value: string) => void;
}) {
  const inputRef = useRef<HTMLInputElement>(null);
  const insertCharacter = useDanishCharacterInsertion(inputRef, text, onText);

  return (
    <Stack gap="xs">
      <TextInput
        ref={inputRef}
        label={label}
        description="Enter one word"
        value={text}
        disabled={disabled}
        onChange={(event) => onText(event.currentTarget.value)}
        autoComplete="off"
        autoCapitalize="none"
        lang="da"
        maw={220}
      />
      <DanishCharacterButtons disabled={disabled} insertCharacter={insertCharacter} />
    </Stack>
  );
}

function DanishSentenceInput({ text, disabled, onText, label }: {
  text: string;
  disabled: boolean;
  onText: (value: string) => void;
  label: string;
}) {
  const inputRef = useRef<HTMLTextAreaElement>(null);
  const insertCharacter = useDanishCharacterInsertion(inputRef, text, onText);
  return (
    <Stack gap="xs">
      <Textarea
        ref={inputRef}
        label={label}
        description="Write one complete Danish statement"
        value={text}
        disabled={disabled}
        onChange={(event) => onText(event.currentTarget.value)}
        autoComplete="off"
        autoCapitalize="sentences"
        lang="da"
        rows={3}
      />
      <DanishCharacterButtons disabled={disabled} insertCharacter={insertCharacter} />
    </Stack>
  );
}

export default function ExercisePrompt({
  exercise, text, order, disabled, onText, onOrder,
  matchingPairs = [], matchingFocusRef, onMatchingPairs = () => undefined,
  gapSelections = [], gapFocusRef, onGapSelections = () => undefined,
}: Props) {
  if (exercise.formatId === "E014") {
    return (
      <MatchingPrompt
        key={exercise.id}
        columns={exercise.prompt.matchingColumns}
        pairs={matchingPairs}
        disabled={disabled}
        focusRef={matchingFocusRef}
        onPairs={onMatchingPairs}
      />
    );
  }
  if (exercise.formatId === "E015") {
    return (
      <GapPrompt
        key={exercise.id}
        prompt={exercise.prompt.gapPrompt}
        selections={gapSelections}
        disabled={disabled}
        focusRef={gapFocusRef}
        onSelections={onGapSelections}
      />
    );
  }
  const { prompt, formatId } = exercise;
  const inputLabel = "inputLabel" in prompt ? prompt.inputLabel : undefined;
  const replacementWord = "replacementWord" in prompt ? prompt.replacementWord : undefined;
  const passage = "passage" in prompt ? prompt.passage : undefined;
  const tileMode = "tileMode" in prompt ? prompt.tileMode : undefined;
  if (hasReadingTarget(exercise)) {
    return (
      <Stack gap="md">
        <Stack gap="xs" role="group" aria-label="Reading passage" lang="da">
          {passage?.map((sentence, index) => <Text key={`${index}-${sentence}`}>{sentence}</Text>)}
        </Stack>
        {prompt.cue && <Text lang="en" size="lg" fw={650}>{prompt.cue}</Text>}
        <Group gap="xs" role="group" aria-label="Answer choices">
          {prompt.choices.map((choice) => (
            <Button
              key={choice}
              variant={text === choice ? "filled" : "default"}
              disabled={disabled}
              onClick={() => onText(choice)}
              aria-pressed={text === choice}
            >
              <span lang="en">{choice}</span>
            </Button>
          ))}
        </Group>
      </Stack>
    );
  }

  if (formatId === "E009") {
    return (
      <Stack gap="md">
        {prompt.cue && <Text lang="en" size="xl" fw={650}>{prompt.cue}</Text>}
        <DanishSentenceInput
          label={inputLabel ?? "Danish sentence"}
          text={text}
          disabled={disabled}
          onText={onText}
        />
      </Stack>
    );
  }

  if (hasLexicalTarget(exercise)) {
    return (
      <Stack gap="sm">
        {prompt.cue && <Text lang={prompt.cueLanguage ?? undefined} size="xl" fw={650}>{prompt.cue}</Text>}
        {prompt.choices.length > 0 ? (
          <Group gap="xs" role="group" aria-label="Answer choices">
            {prompt.choices.map((choice) => (
              <Button
                key={choice}
                variant={text === choice ? "filled" : "default"}
                disabled={disabled}
                onClick={() => onText(choice)}
                aria-pressed={text === choice}
              >
                <span lang={prompt.answerLanguage ?? undefined}>{choice}</span>
              </Button>
            ))}
          </Group>
        ) : (
          <DanishTextInput
            label={inputLabel ?? "Danish singular headword"}
            text={text}
            disabled={disabled}
            onText={onText}
          />
        )}
      </Stack>
    );
  }

  if (["E003", "E024", "E025", "E029"].includes(formatId) || tileMode) {
    return (
      <SentenceOrderPrompt prompt={prompt} order={order} disabled={disabled} tileMode={tileMode} onOrder={onOrder} />
    );
  }

  if (prompt.choices.length > 0) {
    return (
      <Stack gap="sm">
        {prompt.cue && <Text lang={prompt.cueLanguage ?? "en"} size="lg" fw={650}>{prompt.cue}</Text>}
        <Group
          gap="sm"
          role="group"
          aria-label={choiceGroupLabel(formatId)}
          lang={choiceGroupLanguage(formatId)}
        >
          {prompt.before && <Text>{prompt.before}</Text>}
          <Group gap="xs" aria-label="Answer choices">
            {prompt.choices.map((choice) => (
              <Button
                key={choice}
                variant={text === choice ? "filled" : "default"}
                disabled={disabled}
                onClick={() => onText(choice)}
                aria-pressed={text === choice}
              >
                <span lang={prompt.answerLanguage ?? "da"}>{choice}</span>
              </Button>
            ))}
          </Group>
          {prompt.after && <Text>{prompt.after}</Text>}
        </Group>
      </Stack>
    );
  }

  if (replacementWord) {
    return (
      <Stack gap="sm">
        <BoxedRepairSentence before={prompt.before} word={replacementWord} after={prompt.after} />
        <DanishTextInput
          label={inputLabel ?? "Missing word"}
          text={text}
          disabled={disabled}
          onText={onText}
        />
      </Stack>
    );
  }

  return (
    <Stack gap="sm">
      {prompt.cue && <Text lang={prompt.cueLanguage ?? "en"} size="lg" fw={650}>{prompt.cue}</Text>}
      <Group
        gap="xs"
        align="end"
        wrap="wrap"
        role="group"
        aria-label={responseGroupLabel(formatId)}
      >
        {prompt.before && <Text lang="da" pb={9}>{prompt.before}</Text>}
        <DanishTextInput
          label={inputLabel ?? "Missing word"}
          text={text}
          disabled={disabled}
          onText={onText}
        />
        {prompt.after && <Text lang="da" pb={9}>{prompt.after}</Text>}
      </Group>
    </Stack>
  );
}

function BoxedRepairSentence({ before, word, after }: { before: string; word: string; after: string }) {
  return (
    <Paper withBorder p="md" role="group" aria-label="Danish sentence to repair">
      <Text size="sm" fw={650} mb="xs">Word to replace</Text>
      <Text lang="da" className={classes.repairSentence}>
        {before}
        <VisuallyHidden lang="en">Word to replace: </VisuallyHidden>
        <span className={classes.replacementWord}>{word}</span>
        {after}
      </Text>
    </Paper>
  );
}
