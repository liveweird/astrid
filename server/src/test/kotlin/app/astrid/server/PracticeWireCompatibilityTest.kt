package app.astrid.server

import app.astrid.server.infra.json.wireJson
import app.astrid.server.practice.PracticeItemDto
import app.astrid.server.practice.PracticeItemPayload
import app.astrid.server.practice.PracticePlanPayload
import app.astrid.server.practice.PracticeSessionResponse
import app.astrid.server.practice.PracticeSummaryDto
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlinx.serialization.encodeToString

class PracticeWireCompatibilityTest {
    @Test
    fun `legacy plan payload remains byte stable when assessment mode is absent`() {
        val payload = PracticePlanPayload(
            title = "Legacy",
            contentRevision = "pilot-3",
            generatorRevision = "generator-3",
            evaluationRevision = "evaluation-3",
            prerequisiteSkillIds = listOf("G001"),
            introduction = emptyList(),
            items = listOf(
                PracticeItemPayload(
                    position = 0,
                    targetLabel = "Word order",
                    answerExposureKeys = listOf("grammar:G005"),
                    promptExposureKeys = emptyList(),
                    lessonExposureKeys = emptyList(),
                    hintExposureKeys = emptyList(),
                    feedbackExposureKeys = emptyList(),
                ),
            ),
        )

        assertEquals(
            """{"title":"Legacy","contentRevision":"pilot-3","generatorRevision":"generator-3",""" +
                """"evaluationRevision":"evaluation-3","prerequisiteSkillIds":["G001"],"introduction":[],""" +
                """"items":[{"position":0,"targetLabel":"Word order","answerExposureKeys":["grammar:G005"],""" +
                """"promptExposureKeys":[],"lessonExposureKeys":[],"hintExposureKeys":[],""" +
                """"feedbackExposureKeys":[]}]}""",
            wireJson.encodeToString(payload),
        )
    }

    @Test
    fun `legacy practice item and response omit new optional fields`() {
        val item = PracticeItemDto(
            position = 0,
            exerciseId = "00000000-0000-0000-0000-000000000001",
            formatId = "E003",
            targetSkillId = "G005",
            lessonOpened = false,
            hintOpened = false,
            attemptIds = emptyList(),
            attemptCount = 0,
        )
        val encoded = wireJson.encodeToString(item)

        assertFalse("assessmentMode" in encoded)
        assertFalse("followUpPlanRevision" in wireJson.encodeToString(legacyResponse(item)))
    }

    private fun legacyResponse(item: PracticeItemDto) = PracticeSessionResponse(
        id = "00000000-0000-0000-0000-000000000002",
        ownerSessionId = "00000000-0000-0000-0000-000000000003",
        focusId = "reading-basics",
        title = "Legacy",
        planRevision = "practice-1",
        seed = 0,
        contentRevision = "pilot-3",
        generatorRevision = "generator-3",
        evaluationRevision = "evaluation-3",
        sessionExpiresAt = 1,
        state = "active",
        cursor = 0,
        itemCount = 1,
        prerequisiteSkillIds = emptyList(),
        introduction = emptyList(),
        introductionOpened = false,
        items = listOf(item),
        summary = PracticeSummaryDto(1, 0, 0, 0, 0, false, emptyList()),
    )
}
