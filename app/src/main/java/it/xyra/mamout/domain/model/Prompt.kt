package it.xyra.mamout.domain.model

import kotlinx.serialization.Serializable

/**
 * Domain model representing lightweight prompt metadata without heavy content text.
 *
 * @property id The unique identifier of the prompt.
 * @property title The display title of the prompt.
 * @property description A brief summary or description of the prompt.
 */
@Serializable
data class Prompt(
    val id: Long,
    val title: String,
    val description: String,
    val lastModified: Long
)
