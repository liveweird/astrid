import {
  Accordion, Alert, Anchor, Badge, Box, Button, Checkbox, Group, List, Paper, Select, Stack, Text, TextInput, Title,
} from "@mantine/core";
import { IconAlertCircle, IconBook2, IconSearch } from "@tabler/icons-react";
import { useMemo, useState } from "react";
import { Link as RouterLink } from "react-router-dom";
import type { VocabularyEntry, VocabularySense, VocabularyTeachingClaim } from "../../api/models";
import EmptyState from "../../components/EmptyState";
import classes from "../../theme.module.css";
import { filterVocabulary, hasRecordedTeaching, SITUATIONS, type SituationId } from "./vocabularyFilters";
import { useVocabularyContext } from "./useVocabularyContext";
import { useVocabularyEntries } from "./useVocabularyEntries";

function claimFor(sense: VocabularySense, entry: VocabularyEntry, claims: VocabularyTeachingClaim[]) {
  return claims.find((claim) => claim.entryId === entry.entryId && claim.senseId === sense.senseId);
}

function reviewLabel(reviewState: string) {
  return reviewState === "draft-awaiting-teaching-review" || reviewState === "draft-not-teacher-reviewed"
    ? "Draft — awaiting teaching review" : reviewState.replaceAll("-", " ");
}

function SenseDetails({ entry, sense, claims }: {
  entry: VocabularyEntry;
  sense: VocabularySense;
  claims: VocabularyTeachingClaim[];
}) {
  const claim = claimFor(sense, entry, claims);
  return (
    <Stack component="section" aria-label={sense.meaning} gap="sm" className={classes.vocabularySense}>
      <Group gap="xs" align="center">
        <Text lang="en" fw={650}>{sense.meaning}</Text>
        {claim?.meaningRecorded && <Badge color="gray" variant="light">Meaning in saved lesson teaching</Badge>}
      </Group>
      <Box>
        <Text size="xs" fw={650} c="dimmed">Active forms and bundles</Text>
        <Group gap="xs" mt={4} align="stretch">
          {sense.activeForms.map((form) => (
            <Paper key={form.formId} withBorder px="sm" py={6} className={classes.vocabularyForm}>
              <Text lang="da" fw={650}>{form.text}</Text>
              <Text size="xs" c="dimmed">{form.label.replaceAll("-", " ")}</Text>
              {claim?.formIds.includes(form.formId) && (
                <Badge color="gray" variant="outline" size="xs" mt={4}>In saved lesson teaching</Badge>
              )}
            </Paper>
          ))}
        </Group>
      </Box>
      {sense.restrictions.length > 0 && (
        <Box>
          <Text size="xs" fw={650} c="dimmed">Use notes</Text>
          <List size="sm" mt={4}>{sense.restrictions.map((restriction) => <List.Item key={restriction}>{restriction}</List.Item>)}</List>
        </Box>
      )}
      <Box>
        <Text size="xs" fw={650} c="dimmed">Authored examples</Text>
        {sense.examples.length > 0 ? (
          <Stack component="ul" className={classes.vocabularyExampleList} gap="xs" mt={4}>
            {sense.examples.map((example) => (
              <Box component="li" key={example.exampleId}>
                <Text lang="da">{example.danish}</Text>
                {example.english && <Text lang="en" size="sm" c="dimmed">{example.english}</Text>}
                {example.scene && <Text lang="en" size="xs" c="dimmed">Scene: {example.scene}</Text>}
              </Box>
            ))}
          </Stack>
        ) : <Text size="sm" c="dimmed" mt={4}>{sense.exampleNote ?? "No authored example is available for this selected use."}</Text>}
      </Box>
      {sense.lessons.length > 0 && (
        <Box>
          <Text size="xs" fw={650} c="dimmed">Related lessons</Text>
          <Group gap="xs" mt={4}>
            {sense.lessons.map((lesson) => (
              <Anchor key={lesson.focusId} component={RouterLink} to={`/#lesson-${lesson.focusId}`} size="sm">
                {lesson.title}
              </Anchor>
            ))}
          </Group>
        </Box>
      )}
      <Group gap="xs">
        <Badge color="gray" variant="light">{reviewLabel(sense.reviewState)}</Badge>
        {sense.sourceUrls.map((url, index) => (
          <Anchor key={url} href={url} target="_blank" rel="noreferrer" size="xs">Source {index + 1}</Anchor>
        ))}
      </Group>
    </Stack>
  );
}

function EntryCard({ entry, claims }: { entry: VocabularyEntry; claims: VocabularyTeachingClaim[] }) {
  const recorded = hasRecordedTeaching(entry, claims);
  const focalText = entry.senses[0]?.activeForms[0]?.text ?? entry.headword;
  return (
    <Paper component="li" withBorder className={classes.vocabularyCard}>
      <Accordion variant="unstyled">
        <Accordion.Item value={entry.entryId}>
          <Accordion.Control>
            <Group justify="space-between" align="start" wrap="nowrap" className={classes.vocabularyCardHeading}>
              <Box>
                <Title component="span" order={3} size="h3" lang="da">{focalText}</Title>
                {focalText !== entry.headword && (
                  <Text size="xs" c="dimmed">Headword: <span lang="da">{entry.headword}</span></Text>
                )}
                <Text lang="en" size="sm" c="dimmed">{entry.senses.map((sense) => sense.meaning).join("; ")}</Text>
              </Box>
              {recorded && <Badge color="gray" variant="light">In saved lesson teaching</Badge>}
            </Group>
          </Accordion.Control>
          <Accordion.Panel>
            <Stack gap="lg">
              {entry.senses.map((sense) => <SenseDetails key={sense.senseId} entry={entry} sense={sense} claims={claims} />)}
            </Stack>
          </Accordion.Panel>
        </Accordion.Item>
      </Accordion>
    </Paper>
  );
}

export default function Vocabulary() {
  const catalog = useVocabularyEntries();
  const personal = useVocabularyContext(catalog.data?.libraryRevision);
  const [search, setSearch] = useState("");
  const [situation, setSituation] = useState<SituationId>("all");
  const [savedOnly, setSavedOnly] = useState(false);
  const claims = useMemo(() => personal.context?.items ?? [], [personal.context]);
  const entries = useMemo(() => filterVocabulary(
    catalog.data?.items ?? [], search, situation, savedOnly, claims,
  ), [catalog.data?.items, claims, savedOnly, search, situation]);
  const groups = SITUATIONS.slice(1).map((group) => ({
    ...group,
    entries: entries.filter((entry) => situation === "all" ? entry.situationIds[0] === group.value : group.value === situation),
  })).filter((group) => group.entries.length > 0);

  return (
    <Stack gap="xl" maw={960} className={classes.vocabularyLibrary}>
      <Box className={classes.lessonsIntro}>
        <Title order={1}>Vocabulary</Title>
        <Text c="dimmed" mt="xs" maw={720}>
          Explore Danish words and phrases by situation, with forms, meanings and examples. Browsing does not save lesson teaching or change mixed review.
        </Text>
      </Box>

      <Paper withBorder p="md">
        <Stack gap="md">
          <Group align="end" grow className={classes.vocabularyToolbar}>
            <TextInput label="Search vocabulary" placeholder="Danish or English" value={search}
              onChange={(event) => setSearch(event.currentTarget.value)} leftSection={<IconSearch size={16} />} />
            <Select label="Situation" value={situation} data={[...SITUATIONS]}
              onChange={(value) => setSituation((value ?? "all") as SituationId)} allowDeselect={false} />
          </Group>
          <Group justify="space-between" align="center">
            <Checkbox label="In saved lesson teaching" checked={savedOnly}
              onChange={(event) => setSavedOnly(event.currentTarget.checked)} />
            <Button variant="default" disabled={!catalog.data} loading={personal.loading} onClick={() => void personal.load()}>
              Load saved lesson teaching
            </Button>
          </Group>
          {personal.context && (
            <Text role="status" size="sm" c="dimmed">
              {claims.length} {claims.length === 1 ? "selected use has" : "selected uses have"} recorded teaching in this browser session. Open a word to see which meanings and forms were included.
            </Text>
          )}
          {personal.message && <Alert color="blue" icon={<IconAlertCircle size={18} />} role="status">{personal.message}</Alert>}
        </Stack>
      </Paper>

      {catalog.isPending && <Text role="status">Loading vocabulary…</Text>}
      {catalog.isError && (
        <Alert color="red" icon={<IconAlertCircle size={18} />} title="Vocabulary unavailable" role="alert">
          The reference library could not be loaded. <Button variant="subtle" size="compact-sm" onClick={() => void catalog.refetch()}>Try again</Button>
        </Alert>
      )}
      {catalog.data && (
        <>
          <Text role="status" size="sm" c="dimmed">{entries.length} of {catalog.data.items.length} entries shown</Text>
          {groups.length === 0 ? (
            <EmptyState icon={IconBook2} label="No vocabulary matches these filters."
              hint="Change the search, situation or saved-teaching filter." />
          ) : groups.map((group) => (
            <Box component="section" aria-labelledby={`vocabulary-${group.value}`} key={group.value}>
              <Title id={`vocabulary-${group.value}`} order={2} size="h2" mb="md">{group.label}</Title>
              <Stack component="ul" className={classes.vocabularyList} gap="sm">
                {group.entries.map((entry) => <EntryCard key={entry.entryId} entry={entry} claims={claims} />)}
              </Stack>
            </Box>
          ))}
        </>
      )}
    </Stack>
  );
}
