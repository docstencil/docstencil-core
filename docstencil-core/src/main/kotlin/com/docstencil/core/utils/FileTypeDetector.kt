package com.docstencil.core.utils

import com.docstencil.core.api.OfficeFileType
import com.docstencil.core.config.FileTypeConfig

object FileTypeDetector {
    val relationshipParser = RelationshipParser()

    /**
     * Result of file type detection.
     *
     * @property fileType The detected file type.
     * @property targetFiles List of all XML files that should be processed for placeholders.
     */
    data class DetectionResult(
        val fileType: OfficeFileType,
        val mainFile: String,
        val targetFiles: List<String>,
    )

    /**
     * Detect file type and discover all target files to process.
     *
     * @param files The complete ZIP file contents (file path → file bytes).
     * @return DetectionResult containing file type, targets, and metadata.
     */
    fun detect(
        files: Map<String, ByteArray>,
        fallbackConfig: FileTypeConfig.FallbackPathsConfig? = null,
    ): DetectionResult {
        val fileNamesByContentType = getFilesByContentType(files)
        val filesByRelType = getFilesByTargetRelType(files)

        val mainDocPath =
            relationshipParser.getMainDocumentPath(files, fallbackConfig)
                ?: throw IllegalStateException("Cannot find main document path")

        val targetFiles = mutableListOf<String>()
        targetFiles.addAll(getCommonAuxiliaryTargetFiles(fileNamesByContentType))
        targetFiles.addAll(getRelationshipTargetFiles(fileNamesByContentType, filesByRelType))

        if (targetFiles.isEmpty()) {
            targetFiles.addAll(getFallbackTargetFiles(files.keys))
        }

        return DetectionResult(
            fileType = OfficeFileType.DOCX,
            mainFile = mainDocPath,
            targetFiles = targetFiles.distinct(),
        )
    }

    private fun getCommonAuxiliaryTargetFiles(fileNamesByContentType: Map<String, List<String>>): List<String> {
        // Strip leading slash from paths (Content_Types.xml uses "/word/document.xml" format).
        return ContentTypeConstants.COMMON_TYPES
            .flatMap { fileNamesByContentType.getOrDefault(it, listOf()) }
            .map { it.removePrefix("/") }
    }

    private fun getRelationshipTargetFiles(
        fileNamesByContentType: Map<String, List<String>>, filesByRelType: Map<String, String>,
    ): List<String> {
        // Validate relationship type (if relationship exists)
        // This filters out files that have the correct content type but wrong relationship type
        // (e.g., files that look like main documents but aren't actually the main document)
        return ContentTypeConstants.DOCX_ALL_TYPES
            .flatMap { fileNamesByContentType.getOrDefault(it, listOf()) }
            .flatMap {
                val normalizedPath = it.removePrefix("/")

                val relType = filesByRelType[normalizedPath]
                val hasInvalidRelType =
                    relType != null && relType !in ContentTypeConstants.OFFICE_DOCUMENT_RELS

                if (hasInvalidRelType) {
                    listOf()
                } else {
                    listOf(normalizedPath)
                }
            }
    }

    private fun getFilesByContentType(files: Map<String, ByteArray>): Map<String, List<String>> {
        val contentTypesXmlBytes = files["[Content_Types].xml"] ?: return mapOf()
        val parseResult = ContentTypeParser.parse(contentTypesXmlBytes.toString(Charsets.UTF_8))
        return parseResult.overrides.groupBy({ it.contentType }, { it.partName })
    }

    private fun getFilesByTargetRelType(files: Map<String, ByteArray>): Map<String, String> {
        val relsXmlBytes = files["_rels/.rels"] ?: return mapOf()
        val parser = RelationshipParser()
        val relationships = parser.parse(relsXmlBytes.toString(Charsets.UTF_8))
        return relationships.associateBy({ it.target }, { it.type })
    }

    private fun getFallbackTargetFiles(files: Collection<String>): List<String> {
        return files.filter { path ->
            path.contains("word/document.xml") ||
                    path.contains("word/header") ||
                    path.contains("word/footer") ||
                    path.contains("word/footnotes.xml") ||
                    path.contains("word/endnotes.xml") ||
                    path.contains("word/comments.xml") ||
                    path.contains("docProps/core.xml") ||
                    path.contains("docProps/app.xml") ||
                    path.contains("docProps/custom.xml")
        }
    }

    /**
     * Get fallback target files by pattern matching using config.
     */
    private fun getFallbackTargetFiles(
        files: Collection<String>,
        fileTypeConfig: FileTypeConfig,
    ): List<String> {
        // Use patterns from config (separated from main document)
        val patterns = fileTypeConfig.fallbackPaths.targetFilePrefixes

        // Reuse the same pattern matching logic as getMainDocumentPath
        return files.filter { path ->
            patterns.any { pattern -> path.contains(pattern) }
        }
    }
}
