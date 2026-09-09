package it.xyra.mamout.data.local

/**
 * Database projection combining prompt metadata with template text specifically for search indexing.
 *
 * @property id The unique identifier matching [PromptEntity.id].
 * @property title The title of the prompt.
 * @property description A brief summary or description of the prompt.
 * @property templateText The raw template content joined from [PromptContentEntity].
 */
data class PromptSearchableDb(
    val id: Long,
    val title: String,
    val description: String,
    val templateText: String
)
