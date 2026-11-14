import { Alert, Box, Button, Code, PasswordInput, SimpleGrid, Stack, Text } from "@mantine/core";
import { useRef, useState } from "react";
import { inspectExercise } from "../api/client";
import type { InspectorDetails } from "../api/models";

function VocabularyEvidence({ details }: { details: InspectorDetails }) {
  const source = details.vocabularySource;
  if (!source) return null;
  const p3Source = source as typeof source & {
    scene?: { id: string; text: string; exposureKeys: string[] };
    contrastId?: string;
    spatialMeanings?: {
      form: string;
      classification: string;
      lexicalSource: { entryId: string; senseId: string };
    }[];
  };
  const answerLanguage = ["E031", "E039", "E045", "E050"].includes(details.exercise.formatId)
    ? "en" : details.exercise.formatId === "E051" ? undefined : "da";
  return (
    <Stack gap="sm" style={{ gridColumn: "1 / -1" }}>
      <Text size="sm">Vocabulary packet task<br /><Code>{source.taskId} · {source.operation}</Code></Text>
      {p3Source.scene && (
        <Box>
          <Text size="sm">Required English scene<br /><Code>{p3Source.scene.id}</Code></Text>
          <Text size="sm" lang="en">{p3Source.scene.text}</Text>
        </Box>
      )}
      {p3Source.contrastId && <Text size="sm">Spatial contrast<br /><Code>{p3Source.contrastId}</Code></Text>}
      {p3Source.spatialMeanings?.map((meaning) => (
        <Text size="sm" key={`${meaning.lexicalSource.entryId}:${meaning.lexicalSource.senseId}:${meaning.form}`}>
          Spatial meaning<br /><Code><span lang="da">{meaning.form}</span> · {meaning.classification} · {meaning.lexicalSource.entryId} · {meaning.lexicalSource.senseId}</Code>
        </Text>
      ))}
      {source.lexicalSources.map((lexical) => (
        <Box key={`${lexical.entryId}:${lexical.senseId}`}>
          <Text size="sm"><Code>{lexical.entryId} · {lexical.senseId} · {lexical.kind}</Code></Text>
          <Text size="sm" lang="da">
            {[lexical.forms.infinitive, lexical.forms.present, lexical.forms.bundle, lexical.forms.form].filter(Boolean).join(" → ")}
          </Text>
          <Text size="sm" lang="en">{lexical.gloss}</Text>
          <Text size="sm">{lexical.selectedUse}</Text>
        </Box>
      ))}
      {source.expectedAnswer && <Text size="sm">Expected answer<br /><Code lang={answerLanguage}>{source.expectedAnswer}</Code></Text>}
      {source.supportFacts.map((fact) => {
        const p3Fact = fact as typeof fact & {
          actionText?: string;
          complementRole?: string;
          complementSource?: { entryId: string; senseId: string };
          constructionId?: string;
          sceneId?: string;
        };
        return <Box key={fact.factId}>
          <Text size="sm">Supporting fact<br /><Code>{fact.factId}: {fact.referentId} · {fact.predicateId} · {fact.valueRole} · {fact.valueId}</Code></Text>
          <Text size="sm" lang="da">{fact.sentence}</Text>
          <Text size="sm" lang="en">{fact.valueText}</Text>
          {p3Fact.actionText && <Text size="sm">Action<br /><Code lang="en">{p3Fact.actionText}</Code></Text>}
          {p3Fact.complementRole && <Text size="sm">Complement<br /><Code>{p3Fact.complementRole}{p3Fact.complementSource ? ` · ${p3Fact.complementSource.entryId} · ${p3Fact.complementSource.senseId}` : ""}</Code></Text>}
          {p3Fact.constructionId && <Text size="sm">Construction<br /><Code>{p3Fact.constructionId}</Code></Text>}
          {p3Fact.sceneId && <Text size="sm">Required scene<br /><Code>{p3Fact.sceneId}</Code></Text>}
        </Box>
      })}
      {source.matchingPairs.map((pair) => <Text size="sm" key={pair.leftId}>
        <Code>{pair.leftId} → {pair.rightId}: {pair.lexicalSource.entryId} · {pair.lexicalSource.senseId}</Code>
      </Text>)}
      {source.tileOccurrences.map((tile) => <Text size="sm" key={tile.blockId}>
        <Code>{tile.blockId}: {tile.lexicalId} · <span lang="da">{tile.form}</span></Code>
      </Text>)}
      {source.acceptedOrders.map((order) => <Text size="sm" key={order.join(":")}>
        Accepted occurrence order<br /><Code>{order.join(" → ")}</Code>
      </Text>)}
    </Stack>
  );
}

function InspectorEvidence({ details }: { details: InspectorDetails }) {
  if (details.vocabularySource) return <VocabularyEvidence details={details} />;
  if (details.gapSource) {
    return <Text size="sm" style={{ gridColumn: "1 / -1" }}>Correct gap evidence<br />
      {details.gapSource.slots.map((slot) => <Code key={slot.slotId} display="block">
        {slot.slotId}: {slot.expectedChoiceId} · {slot.lexicalId} · <span lang="da">{slot.expectedForm}</span> / <span lang="da">{slot.alternativeForm}</span>
      </Code>)}
    </Text>;
  }
  if (details.matchingSource) {
    return <Text size="sm" style={{ gridColumn: "1 / -1" }}>Correct matching evidence<br />
      {details.matchingSource.pairs.map((pair) => <Code key={pair.leftId} display="block">
        {pair.leftId} → {pair.rightId}: {pair.lexicalSource.entryId} · {pair.lexicalSource.senseId} · <span lang="da">{pair.lexicalSource.article} {pair.lexicalSource.headword}</span> · <span lang="en">{pair.lexicalSource.gloss}</span>
      </Code>)}
    </Text>;
  }
  if (details.readingSource) {
    return <>
      <Text size="sm">Question relation<br /><Code>{details.readingSource.questionSubjectId} · {details.readingSource.questionVerbId}</Code></Text>
      <Text size="sm">Answer evidence<br /><Code>{details.readingSource.answerNounId} · {details.readingSource.answer}</Code></Text>
      <Text size="sm" style={{ gridColumn: "1 / -1" }}>Passage propositions<br />
        {details.readingSource.propositions.map((proposition) => <Code key={`${proposition.sentenceId}-${proposition.subjectId}-${proposition.verbId}-${proposition.objectId}`} display="block">
          {proposition.sentenceId}: {proposition.subjectId} · {proposition.verbId} · {proposition.objectId}
        </Code>)}
      </Text>
    </>;
  }
  if (details.lexicalSource) {
    return <>
      <Text size="sm">Entry and sense<br /><Code>{details.lexicalSource.entryId} · {details.lexicalSource.senseId}</Code></Text>
      <Text size="sm">Headword<br /><Code lang="da">{details.lexicalSource.headword}</Code></Text>
      <Text size="sm">Gloss<br /><Code lang="en">{details.lexicalSource.gloss}</Code></Text>
      <Text size="sm">Article<br /><Code lang="da">{details.lexicalSource.article}</Code></Text>
    </>;
  }
  if (details.tileSource) {
    return <>
      <Text size="sm">Canonical sentence<br /><Code lang="da">{details.canonicalSentence}</Code></Text>
      <Text size="sm">Identity<br /><Code>{details.canonicalSentenceId}</Code></Text>
      <Text size="sm" style={{ gridColumn: "1 / -1" }}>Tile identities<br />
        {details.tileSource.tokens.map((token) => <Code key={token.blockId} display="block">
          {token.blockId}: {token.lexicalId} · {token.form}
        </Code>)}
      </Text>
      <Text size="sm" style={{ gridColumn: "1 / -1" }}>Licensed orders<br />
        {details.tileSource.licensedOrders.map((order) => <Code key={`${order.kind}-${order.blockIds.join("-")}`} display="block">
          {order.kind}: {order.blockIds.join(" → ")}
        </Code>)}
      </Text>
      {details.tileSource.distractors.length > 0 && <Text size="sm" style={{ gridColumn: "1 / -1" }}>Distractors<br />
        {details.tileSource.distractors.map((distractor) => <Code key={distractor.blockId} display="block">
          {distractor.blockId}: {distractor.rationale}
        </Code>)}
      </Text>}
    </>;
  }
  return <>
    <Text size="sm">Canonical sentence<br /><Code lang="da">{details.canonicalSentence}</Code></Text>
    <Text size="sm">Identity<br /><Code>{details.canonicalSentenceId}</Code></Text>
    <Text size="sm">Construction<br /><Code>{details.constructionId}</Code></Text>
    <Text size="sm">Roles<br /><Code>{details.clauseRoles?.join(", ")}</Code></Text>
    <Text size="sm">Lexical IDs<br /><Code>{details.lexicalIds?.join(", ")}</Code></Text>
  </>;
}

export default function InspectorPanel({ exerciseId }: { exerciseId: string }) {
  const [key, setKey] = useState("");
  const [result, setResult] = useState<{ exerciseId: string; details: InspectorDetails } | null>(null);
  const [failure, setFailure] = useState<{ exerciseId: string; message: string } | null>(null);
  const [loading, setLoading] = useState(false);
  const requestVersion = useRef(0);
  const details = result?.exerciseId === exerciseId ? result.details : null;
  const error = failure?.exerciseId === exerciseId ? failure.message : "";

  const inspect = async () => {
    const version = ++requestVersion.current;
    const requestedExerciseId = exerciseId;
    setLoading(true);
    setFailure(null);
    try {
      const inspected = await inspectExercise(requestedExerciseId, key);
      if (requestVersion.current === version) setResult({ exerciseId: requestedExerciseId, details: inspected });
    } catch {
      if (requestVersion.current === version) {
        setResult(null);
        setFailure({
          exerciseId: requestedExerciseId,
          message: "Inspector access was refused. Check the in-memory key and server configuration.",
        });
      }
    } finally {
      if (requestVersion.current === version) setLoading(false);
    }
  };

  return (
    <Stack gap="sm">
      <Text size="sm">Private generation evidence. The key stays in this tab’s memory.</Text>
      <PasswordInput label="Inspector key" value={key} onChange={(event) => setKey(event.currentTarget.value)} />
      <Button variant="default" loading={loading} disabled={key.length < 32} onClick={() => void inspect()}>
        Inspect this exercise
      </Button>
      {error && <Alert color="red">{error}</Alert>}
      {details && (
        <SimpleGrid cols={{ base: 1, sm: 2 }} spacing="xs">
          <InspectorEvidence details={details} />
          <Text size="sm">Reproduction<br /><Code>seed {details.seed}, index {details.index}</Code></Text>
        </SimpleGrid>
      )}
    </Stack>
  );
}
