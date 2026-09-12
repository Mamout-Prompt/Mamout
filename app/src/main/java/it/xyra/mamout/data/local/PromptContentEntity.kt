package it.xyra.mamout.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

/**
 * Represents the actual content or template text associated with a specific prompt.
 *
 * This entity has a one-to-one relationship with [PromptEntity]: [promptId] is both
 * the primary key of this table and a foreign key to [PromptEntity.id], so a given
 * prompt can have at most one content row. This is also what makes `INSERT OR REPLACE`
 * (see [PromptDao.upsertPromptContent]) behave as a true upsert: a conflict on the
 * primary key replaces the existing row instead of creating a duplicate.
 *
 * If a parent [PromptEntity] is deleted, its corresponding content is removed
 * automatically (CASCADE).
 *
 * @property promptId The ID of the parent [PromptEntity]; also this row's primary key.
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
    ]
)
class PromptContentEntity(
    @PrimaryKey
    val promptId: Long,
    val templateText: String
)