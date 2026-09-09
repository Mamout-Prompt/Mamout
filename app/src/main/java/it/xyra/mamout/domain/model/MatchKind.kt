package it.xyra.mamout.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class MatchKind {
    @SerialName("insertion") INSERTION,
    @SerialName("edit") EDIT
}