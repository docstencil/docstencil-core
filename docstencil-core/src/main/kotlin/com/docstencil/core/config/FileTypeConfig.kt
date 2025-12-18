package com.docstencil.core.config

import com.docstencil.core.api.OfficeFileType


data class FileTypeConfig(
    val fileType: OfficeFileType,

    /**
     * XML tags that contain text content where placeholders can appear.
     */
    val tagsText: List<String>,

    /**
     * XML tags that define document structure.
     */
    val tagsStructure: List<String>,

    /**
     * Tags that can be deleted when template expressions are moved out of them.
     */
    val tagsDeletable: List<String> = emptyList(),

    /**
     * Tag used for raw XML insertion.
     */
    val defaultTagRawXmlExpansion: String = "w:p",

    /**
     * Expansion rules for certain tags
     */
    val expansionRules: List<ExpansionRule> = emptyList(),

    /**
     * Structural invariants that ensure Office XML schema compliance.
     */
    val repairRules: List<RepairRule> = emptyList(),

    /**
     * Maps user-friendly keywords (e.g., "inline", "block") to actual XML tag names.
     */
    val tagNicknames: Map<String, String> = emptyMap(),

    /**
     * Defines how manual line breaks can be inserted.
     */
    val lineBreaks: LineBreakConfig,

    /**
     * Default XML namespaces to ensure on document root elements.
     * Maps namespace prefix to namespace URI.
     * These will be merged with any existing namespaces (existing ones preserved).
     */
    val defaultNamespaces: Map<String, String> = emptyMap(),

    /**
     * Fallback paths for when _rels/.rels or [Content_Types].xml are missing or incomplete.
     */
    val fallbackPaths: FallbackPathsConfig,
) {
    data class FallbackPathsConfig(
        val mainDocument: String,
        val targetFilePrefixes: List<String>,
    )

    data class ExpansionRule(
        val contains: String,
        val notContains: String? = null,
        val expand: String,
    )

    data class LineBreakConfig(
        val tag: String,
        val tagsToEscape: List<String>,
    )

    /**
     * Configuration rule for structural validation.
     *
     * These rules define Office XML schema requirements that must be enforced to prevent
     * document corruption.
     *
     * @property tag The parent tag to check (e.g., "w:tc" for table cell)
     * @property shouldContain List of required child tags (e.g., ["w:p"] for paragraph).
     *                         If ANY of these tags is found, the parent is considered valid.
     * @property action The action to take when required children are missing:
     *                  - Insert: Inserts default XML content (e.g., "<w:p></w:p>")
     *                  - Drop: Removes the entire tag and its contents
     *                  - DropWithParent: Removes the parent tag and all contents
     */
    data class RepairRule(
        val tag: String,
        val shouldContain: List<String>,
        val action: Action,
    ) {
        sealed class Action

        data class Insert(val value: String) : Action()
        data object Drop : Action()
        data class DropWithParent(val parentTag: String) : Action()
    }

    companion object {
        fun docx(): FileTypeConfig {
            return FileTypeConfig(
                fileType = OfficeFileType.DOCX,
                tagsText = listOf(
                    // Core Word text tags
                    "w:t",
                    "a:t",
                    "m:t",

                    // Document properties
                    "Company",
                    "HyperlinkBase",
                    "Manager",
                    "cp:category",
                    "cp:keywords",
                    "dc:creator",
                    "dc:description",
                    "dc:subject",
                    "dc:title",
                    "cp:contentStatus",

                    // Variant types
                    "vt:lpstr",
                    "vt:lpwstr",
                ),
                tagsStructure = listOf(
                    // Document structure
                    "w:document",
                    "w:body",

                    // Paragraph and run
                    "w:p",
                    "w:r",
                    "w:br",
                    "w:t",

                    // Properties
                    "w:rPr",
                    "w:pPr",
                    "w:spacing",

                    // Table
                    "w:tbl",
                    "w:tr",
                    "w:tc",

                    // Headers/Footers
                    "w:hdr",
                    "w:ftr",

                    // Structured document tags
                    "w:sdt",
                    "w:sdtContent",

                    // Drawing
                    "w:drawing",

                    // Section
                    "w:sectPr",
                    "w:type",
                    "w:headerReference",
                    "w:footerReference",

                    // Bookmarks and comments
                    "w:bookmarkStart",
                    "w:bookmarkEnd",
                    "w:commentRangeStart",
                    "w:commentRangeEnd",
                    "w:commentReference",

                    // Proofing
                    "w:proofState",
                ),
                tagsDeletable = listOf(
                    "w:p",
                    "w:r",
                    "w:t",
                    "w:br",
                    "w:bookmarkStart",
                    "w:bookmarkEnd",
                    "w:rPr",
                    "w:pPr",
                    "w:pStyle",
                ),
                expansionRules = listOf(
                    ExpansionRule(contains = "w:p", expand = "w:p"),
                    ExpansionRule(contains = "w:tc", notContains = "w:tbl", expand = "w:tr"),
                ),
                defaultTagRawXmlExpansion = "w:p",
                repairRules = listOf(
                    RepairRule(
                        tag = "w:sdtContent",
                        shouldContain = listOf("w:p", "w:r", "w:commentRangeStart", "w:sdt"),
                        action = RepairRule.Insert("<w:p></w:p>"),
                    ),
                    RepairRule(
                        tag = "w:tc",
                        shouldContain = listOf("w:p"),
                        action = RepairRule.Insert("<w:p></w:p>"),
                    ),
                    RepairRule(
                        tag = "w:tr",
                        shouldContain = listOf("w:tc"),
                        action = RepairRule.Drop,
                    ),
                    RepairRule(
                        tag = "w:tbl",
                        shouldContain = listOf("w:tr"),
                        action = RepairRule.Drop,
                    ),
                ),
                tagNicknames = mapOf(
                    "inline" to "w:r",
                    "block" to "w:p",
                ),
                lineBreaks = LineBreakConfig("w:br", listOf("w:t")),
                fallbackPaths = FallbackPathsConfig(
                    mainDocument = "word/document.xml",
                    targetFilePrefixes = listOf(
                        "word/document.xml",
                        "word/header",
                        "word/footer",
                        "word/footnotes.xml",
                        "word/endnotes.xml",
                        "word/comments.xml",
                        "docProps/core.xml",
                        "docProps/app.xml",
                        "docProps/custom.xml",
                    ),
                ),
                defaultNamespaces = mapOf(
                    "w" to "http://schemas.openxmlformats.org/wordprocessingml/2006/main",
                    "r" to "http://schemas.openxmlformats.org/officeDocument/2006/relationships",
                    "mc" to "http://schemas.openxmlformats.org/markup-compatibility/2006",
                    "wp" to "http://schemas.openxmlformats.org/drawingml/2006/wordprocessingDrawing",
                    "a" to "http://schemas.openxmlformats.org/drawingml/2006/main",
                    "pic" to "http://schemas.openxmlformats.org/drawingml/2006/picture",
                    "c" to "http://schemas.openxmlformats.org/drawingml/2006/chart",
                    "dgm" to "http://schemas.openxmlformats.org/drawingml/2006/diagram",
                    "o" to "urn:schemas-microsoft-com:office:office",
                    "v" to "urn:schemas-microsoft-com:vml",
                    "m" to "http://schemas.openxmlformats.org/officeDocument/2006/math",
                    "wps" to "http://schemas.microsoft.com/office/word/2010/wordprocessingShape",
                    "wpg" to "http://schemas.microsoft.com/office/word/2010/wordprocessingGroup",
                    "wpc" to "http://schemas.microsoft.com/office/word/2010/wordprocessingCanvas",
                    "wne" to "http://schemas.microsoft.com/office/word/2006/wordml",
                    "w10" to "urn:schemas-microsoft-com:office:word",
                    "w14" to "http://schemas.microsoft.com/office/word/2010/wordml",
                    "w15" to "http://schemas.microsoft.com/office/word/2012/wordml",
                    "w16" to "http://schemas.microsoft.com/office/word/2018/wordml",
                    "w16cex" to "http://schemas.microsoft.com/office/word/2018/wordml/cex",
                    "w16cid" to "http://schemas.microsoft.com/office/word/2016/wordml/cid",
                    "w16se" to "http://schemas.microsoft.com/office/word/2015/wordml/symex",
                    "wp14" to "http://schemas.microsoft.com/office/word/2010/wordprocessingDrawing",
                    "a14" to "http://schemas.microsoft.com/office/drawing/2010/main",
                    "a15" to "http://schemas.microsoft.com/office/drawing/2012/main",
                    "a16" to "http://schemas.microsoft.com/office/drawing/2014/main",
                    "asvg" to "http://schemas.microsoft.com/office/drawing/2016/SVG/main",
                ),
            )
        }
    }
}
