package com.docstencil.core.utils

import com.docstencil.core.config.FileTypeConfig


private const val OFFICE_DOCUMENT_REL_TYPE =
    "http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument"

class RelationshipParser {
    fun parse(xml: String): List<Relationship> {
        val relationships = mutableListOf<Relationship>()

        // Parse <Relationship> elements.
        // Example: <Relationship Id="rId1" Type="http://..." Target="word/document.xml"/>
        val pattern =
            """<Relationship\s+Id="([^"]+)"\s+Type="([^"]+)"\s+Target="([^"]+)"\s*/?>""".toRegex()

        pattern.findAll(xml).forEach { match ->
            val id = match.groupValues[1]
            val type = match.groupValues[2]
            val target = match.groupValues[3]
            relationships.add(Relationship(id, type, target))
        }

        return relationships
    }

    fun getMainDocumentPath(
        files: Map<String, ByteArray>,
        fallbackConfig: FileTypeConfig.FallbackPathsConfig? = null,
    ): String? {
        val relsPath = "_rels/.rels"
        val relsXml = files[relsPath]?.toString(Charsets.UTF_8)

        if (relsXml != null) {
            val relationships = parse(relsXml)
            return relationships.find { it.type == OFFICE_DOCUMENT_REL_TYPE }?.target
        }
        if (fallbackConfig == null) {
            return null
        }

        val fallbackPattern = fallbackConfig.mainDocument
        return files.keys.firstOrNull { path ->
            path.contains(fallbackPattern)
        }
    }
}

/**
 * Represents a relationship between files in the Office document.
 * Corresponds to <Relationship> elements in _rels/.rels files.
 */
data class Relationship(
    val id: String,
    val type: String,
    val target: String,
)
