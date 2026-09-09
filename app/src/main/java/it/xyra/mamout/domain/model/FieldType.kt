package it.xyra.mamout.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class FieldType {
    @SerialName("text") TEXT,
    @SerialName("smallText") SMALL_TEXT,
    @SerialName("options") OPTIONS
}