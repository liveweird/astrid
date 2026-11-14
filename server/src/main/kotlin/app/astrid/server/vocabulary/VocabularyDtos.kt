@file:OptIn(kotlinx.serialization.ExperimentalSerializationApi::class)

package app.astrid.server.vocabulary

import app.astrid.engine.VocabularyLibraryCatalog
import app.astrid.engine.VocabularyTeachingClaim
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.Serializable

@Serializable
data class VocabularyEntriesResponse(
    val libraryRevision: String,
    val contentRevision: String,
    val items: List<VocabularyEntryDto>,
)

@Serializable
data class VocabularyEntryDto(
    val entryId: String,
    val headword: String,
    val situationIds: List<String>,
    val senses: List<VocabularySenseDto>,
)

@Serializable
data class VocabularySenseDto(
    val senseId: String,
    val meaning: String,
    val activeForms: List<VocabularyFormDto>,
    val restrictions: List<String>,
    val examples: List<VocabularyExampleDto>,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val exampleNote: String? = null,
    val lessons: List<VocabularyLessonDto>,
    val sourceUrls: List<String>,
    val reviewState: String,
)

@Serializable
data class VocabularyFormDto(val formId: String, val label: String, val text: String)

@Serializable
data class VocabularyExampleDto(
    val exampleId: String,
    val danish: String,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val english: String? = null,
    @EncodeDefault(EncodeDefault.Mode.NEVER)
    val scene: String? = null,
)

@Serializable
data class VocabularyLessonDto(val focusId: String, val title: String)

@Serializable
data class VocabularyContextResponse(
    val libraryRevision: String,
    val ownerSessionId: String,
    val sessionExpiresAt: Long,
    val items: List<VocabularyTeachingClaimDto>,
)

@Serializable
data class VocabularyTeachingClaimDto(
    val entryId: String,
    val senseId: String,
    val meaningRecorded: Boolean,
    val formIds: List<String>,
)

internal fun VocabularyLibraryCatalog.toResponse() = VocabularyEntriesResponse(
    libraryRevision,
    contentRevision,
    items.map { entry ->
        VocabularyEntryDto(
            entry.entryId,
            entry.headword,
            entry.situationIds.toList(),
            entry.senses.map { sense ->
                VocabularySenseDto(
                    sense.senseId,
                    sense.meaning,
                    sense.activeForms.map { VocabularyFormDto(it.formId, it.label, it.text) },
                    sense.restrictions.toList(),
                    sense.examples.map { VocabularyExampleDto(it.exampleId, it.danish, it.english, it.scene) },
                    sense.exampleNote,
                    sense.lessons.map { VocabularyLessonDto(it.focusId, it.title) },
                    sense.sourceUrls.toList(),
                    sense.reviewState,
                )
            },
        )
    },
)

internal fun VocabularyTeachingClaim.toDto() = VocabularyTeachingClaimDto(
    entryId,
    senseId,
    meaningRecorded,
    formIds.toList(),
)
