package it.xyra.mamout.domain.model

/**
 * Domain wrapper model used during search operations to supply [templateText] for filtering.
 *
 * @property prompt The lightweight prompt domain object.
 * @property templateText The raw template text used for search matching.
 */
data class PromptSearchable(
    val prompt: Prompt,
    val templateText: String
)
