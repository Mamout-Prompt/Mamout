package it.xyra.mamout.domain.parser

/**
 * Represents the UI control types available for dynamic input fields.
 */
enum class InputType(val key: String) {
    /** Multiline text field for longer text entries. */
    TEXT("text"),

    /** Single-line text field for short text entries. */
    SMALL_TEXT("smallText"),

    /** Dropdown menu for selecting from a predefined list of options. */
    OPTIONS("options");

    companion object {
        /**
         * Returns all valid string keys for input types.
         */
        val allKeys: List<String> = values().map { it.key }

        /**
         * Finds an [InputType] by its string key, defaulting to [TEXT].
         */
        fun fromKey(key: String?): InputType = values().find { it.key == key } ?: TEXT
    }
}
