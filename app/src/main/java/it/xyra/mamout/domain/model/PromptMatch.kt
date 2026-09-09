package it.xyra.mamout.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Represents a match found within a prompt template.
 *
 * @property startMarker Starting index of the match.
 * @property endMarker Ending index of the match.
 * @property matchKind The type of match (insertion or edit).
 * @property originalText The original text if it was an edit.
 * @property fieldType The expected UI field type for this match.
 * @property values Optional list of values for selection fields.
 */
@Serializable
data class PromptMatch(
    @SerialName("start_marker") val startMarker: Int,
    @SerialName("end_marker") val endMarker: Int,
    @SerialName("match_kind") val matchKind: MatchKind = MatchKind.EDIT,
    @SerialName("original_text") val originalText: String? = null,
    @SerialName("field_type") val fieldType: FieldType = FieldType.SMALL_TEXT,
    val values: List<String>? = null
)

/**
 * Wrapper for the metadata response containing all prompt matches.
 *
 * @property matches List of [PromptMatch] objects.
 */
@Serializable
data class MetaPromptResponse(
    val matches: List<PromptMatch>
)
