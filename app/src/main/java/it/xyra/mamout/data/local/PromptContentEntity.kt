package it.xyra.mamout.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Represents the actual content or template text associated with a specific prompt.
 *
 * This entity has a one-to-one relationship with [PromptEntity] and uses a foreign key
 * to ensure data integrity. If a parent [PromptEntity] is deleted, its corresponding
 * content will be removed automatically (CASCADE).
 *
 * @property idContent The unique identifier for this content entry.
 * @property promptId The ID of the parent [PromptEntity].
 * @property templateText The actual text content or template of the prompt.
 */
@Entity(
    tableName = "prompt_contents",
    foreignKeys = [
        ForeignKey(
            entity = PromptEntity::class,
            parentColumns = ["id"],
            childColumns = ["promptId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("promptId")]
)
class PromptContentEntity(
    @PrimaryKey(autoGenerate = true)
    val idContent: Long = 0,
    val promptId: Long,
    val templateText: String
)