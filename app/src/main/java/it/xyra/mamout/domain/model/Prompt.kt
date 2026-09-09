package it.xyra.mamout.domain.model

/**
 * Domain model representing lightweight prompt metadata without heavy content text.
 *
 * @property id The unique identifier of the prompt.
 * @property title The display title of the prompt.
 * @property description A brief summary or description of the prompt.
 */
data class Prompt(
    val id: Long,
    val title: String,
    val description: String
)
