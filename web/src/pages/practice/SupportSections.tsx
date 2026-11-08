import { Accordion, Box, SimpleGrid, Stack, Text, Title } from "@mantine/core";
import { IconBook2 } from "@tabler/icons-react";
import { useState } from "react";
import type { PracticeSession } from "../../api/models";

export type PracticeSupportKind = "introduction" | "lesson" | "hint";

type SupportSectionsProps = {
  session: PracticeSession;
  disabled: boolean;
  introductionContentRef: React.RefObject<HTMLParagraphElement | null>;
  lessonContentRef: React.RefObject<HTMLParagraphElement | null>;
  onSupport: (kind: PracticeSupportKind, position?: number) => void;
};

export default function SupportSections({ session, disabled, introductionContentRef, lessonContentRef, onSupport }: SupportSectionsProps) {
  const exercise = session.currentExercise;
  const item = session.items.find((candidate) => candidate.position === session.cursor);
  const [introductionDisclosure, setIntroductionDisclosure] = useState({
    value: session.introductionOpened ? "introduction" : null as string | null,
    userCollapsed: false,
  });
  const [lessonDisclosure, setLessonDisclosure] = useState({
    position: item?.position,
    value: item?.lessonOpened ? "lesson" : null as string | null,
    userCollapsed: false,
  });
  const introductionValue = session.introductionOpened && !introductionDisclosure.userCollapsed
    ? "introduction" : introductionDisclosure.value;
  const lessonValue = lessonDisclosure.position !== item?.position
    ? item?.lessonOpened ? "lesson" : null
    : item?.lessonOpened && !lessonDisclosure.userCollapsed ? "lesson" : lessonDisclosure.value;
  return (
    <Stack gap="sm">
      {session.introduction.length > 0 && (
        <Accordion
          variant="contained"
          transitionDuration={0}
          value={introductionValue}
          onChange={(value) => {
            if (value === "introduction" && !session.introductionOpened) onSupport("introduction");
            else setIntroductionDisclosure({ value, userCollapsed: value === null && session.introductionOpened });
          }}
        >
          <Accordion.Item value="introduction">
            <Accordion.Control disabled={disabled} icon={<IconBook2 size={18} />}>Session introduction</Accordion.Control>
            <Accordion.Panel>
              {session.introductionOpened ? (
                <SimpleGrid minColWidth="min(100%, 18rem)">
                  {session.introduction.map((section, index) => (
                    <Box key={`${section.title}-${section.body}`}>
                      <Title order={3} size="h4">{section.title}</Title>
                      <Text
                        ref={index === 0 ? introductionContentRef : undefined}
                        data-practice-focus={index === 0 ? "introduction" : undefined}
                        tabIndex={index === 0 ? -1 : undefined}
                        size="sm"
                        mt={4}
                      >{section.body}</Text>
                    </Box>
                  ))}
                </SimpleGrid>
              ) : <Text size="sm">Saving support before it is shown…</Text>}
            </Accordion.Panel>
          </Accordion.Item>
        </Accordion>
      )}
      {exercise && exercise.introduction.length > 0 && item && (
        <Accordion
          variant="contained"
          transitionDuration={0}
          value={lessonValue}
          onChange={(value) => {
            if (value === "lesson" && !item.lessonOpened) onSupport("lesson", item.position);
            else setLessonDisclosure({
              position: item.position, value, userCollapsed: value === null && item.lessonOpened,
            });
          }}
        >
          <Accordion.Item value="lesson">
            <Accordion.Control disabled={disabled}>Learn this first</Accordion.Control>
            <Accordion.Panel>
              {item.lessonOpened ? exercise.introduction.map((section, index) => (
                <Box key={`${section.title}-${section.body}`} mb="sm">
                  <Title order={3} size="h4">{section.title}</Title>
                  <Text
                    ref={index === 0 ? lessonContentRef : undefined}
                    data-practice-focus={index === 0 ? "lesson" : undefined}
                    tabIndex={index === 0 ? -1 : undefined}
                    size="sm"
                    mt={4}
                  >{section.body}</Text>
                </Box>
              )) : <Text size="sm">Saving support before it is shown…</Text>}
            </Accordion.Panel>
          </Accordion.Item>
        </Accordion>
      )}
    </Stack>
  );
}
