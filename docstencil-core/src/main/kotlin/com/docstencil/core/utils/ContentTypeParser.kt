package com.docstencil.core.utils


object ContentTypeParser {
    data class ParseResult(
        val overrides: List<ContentType>,
        val defaults: List<DefaultExtension>,
    )

    fun parse(xml: String): ParseResult {
        val overrides = mutableListOf<ContentType>()
        val defaults = mutableListOf<DefaultExtension>()

        // Parse <Override> elements.
        // Example: <Override PartName="/word/document.xml" ContentType="application/vnd...+xml"/>
        val overridePattern =
            """<Override\s+PartName="([^"]+)"\s+ContentType="([^"]+)"\s*/?>""".toRegex()
        overridePattern.findAll(xml).forEach { match ->
            val partName = match.groupValues[1]
            val contentType = match.groupValues[2]
            overrides.add(ContentType(partName, contentType))
        }

        // Parse <Default> elements.
        // Example: <Default Extension="xml" ContentType="application/xml"/>
        val defaultPattern =
            """<Default\s+Extension="([^"]+)"\s+ContentType="([^"]+)"\s*/?>""".toRegex()
        defaultPattern.findAll(xml).forEach { match ->
            val extension = match.groupValues[1]
            val contentType = match.groupValues[2]
            defaults.add(DefaultExtension(extension, contentType))
        }

        return ParseResult(overrides, defaults)
    }
}

/**
 * Represents a content type override for a specific file in the Office document.
 * Corresponds to <Override> elements in [Content_Types].xml
 */
data class ContentType(
    val partName: String,
    val contentType: String,
)

/**
 * Represents a default content type for a file extension.
 * Corresponds to <Default> elements in [Content_Types].xml
 */
data class DefaultExtension(
    val extension: String,
    val contentType: String,
)
