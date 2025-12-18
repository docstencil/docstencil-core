package com.docstencil.core.utils

/**
 * Office Open XML content type constants.
 *
 * These constants define the content types used in Office documents
 * as specified in the [Content_Types].xml file within DOCX/PPTX/XLSX archives.
 *
 * Reference: ECMA-376 Office Open XML File Formats specification
 */
object ContentTypeConstants {
    // ============================================================
    // Common/Auxiliary Content Types
    // ============================================================

    /**
     * Core document properties (title, author, created date, etc.)
     * Typically found in: docProps/core.xml
     */
    const val CORE_PROPERTIES = "application/vnd.openxmlformats-package.core-properties+xml"

    /**
     * Application-specific properties (template, total editing time, etc.)
     * Typically found in: docProps/app.xml
     */
    const val APP_PROPERTIES =
        "application/vnd.openxmlformats-officedocument.extended-properties+xml"

    /**
     * Custom document properties defined by the user
     * Typically found in: docProps/custom.xml
     */
    const val CUSTOM_PROPERTIES =
        "application/vnd.openxmlformats-officedocument.custom-properties+xml"

    /**
     * Word document settings (language, compatibility mode, etc.)
     * Typically found in: word/settings.xml
     */
    const val SETTINGS =
        "application/vnd.openxmlformats-officedocument.wordprocessingml.settings+xml"

    /**
     * Diagram data files (SmartArt diagrams)
     * Typically found in: word/diagrams/data*.xml
     */
    const val DIAGRAM_DATA =
        "application/vnd.openxmlformats-officedocument.drawingml.diagramData+xml"

    /**
     * Diagram drawing files (SmartArt visual representation)
     * Typically found in: word/diagrams/drawing*.xml
     */
    const val DIAGRAM_DRAWING = "application/vnd.ms-office.drawingml.diagramDrawing+xml"

    /**
     * All common/auxiliary content types that should be processed for placeholders
     */
    val COMMON_TYPES = listOf(
        CORE_PROPERTIES,
        APP_PROPERTIES,
        CUSTOM_PROPERTIES,
        SETTINGS,
        DIAGRAM_DATA,
        DIAGRAM_DRAWING,
    )

    // ============================================================
    // DOCX Content Types
    // ============================================================

    /**
     * Main Word document content
     * Typically found in: word/document.xml
     */
    const val DOCX_MAIN =
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"

    /**
     * Macro-enabled Word document (.docm)
     * Typically found in: word/document.xml
     */
    const val DOCX_MACRO = "application/vnd.ms-word.document.macroEnabled.main+xml"

    /**
     * Word template document (.dotx)
     * Typically found in: word/document.xml
     */
    const val DOTX_TEMPLATE =
        "application/vnd.openxmlformats-officedocument.wordprocessingml.template.main+xml"

    /**
     * Macro-enabled Word template (.dotm)
     * Typically found in: word/document.xml
     */
    const val DOTM_TEMPLATE = "application/vnd.ms-word.template.macroEnabledTemplate.main+xml"

    /**
     * Word header files
     * Typically found in: word/header*.xml
     */
    const val HEADER = "application/vnd.openxmlformats-officedocument.wordprocessingml.header+xml"

    /**
     * Word footer files
     * Typically found in: word/footer*.xml
     */
    const val FOOTER = "application/vnd.openxmlformats-officedocument.wordprocessingml.footer+xml"

    /**
     * Word footnotes
     * Typically found in: word/footnotes.xml
     */
    const val FOOTNOTES =
        "application/vnd.openxmlformats-officedocument.wordprocessingml.footnotes+xml"

    /**
     * Word endnotes
     * Typically found in: word/endnotes.xml
     */
    const val ENDNOTES =
        "application/vnd.openxmlformats-officedocument.wordprocessingml.endnotes+xml"

    /**
     * Word comments
     * Typically found in: word/comments.xml
     */
    const val COMMENTS =
        "application/vnd.openxmlformats-officedocument.wordprocessingml.comments+xml"

    /**
     * Main document types (different variants of word/document.xml)
     */
    val DOCX_MAIN_TYPES = listOf(
        DOCX_MAIN,
        DOCX_MACRO,
        DOTX_TEMPLATE,
        DOTM_TEMPLATE,
    )

    /**
     * All DOCX content types that should be processed
     */
    val DOCX_ALL_TYPES = listOf(
        HEADER,
        FOOTER,
        FOOTNOTES,
        ENDNOTES,
        COMMENTS,
    ) + DOCX_MAIN_TYPES

    // ============================================================
    // Office Document Relationship Types
    // ============================================================

    /**
     * Office Document relationship type (2006 schema)
     * Used in _rels/.rels to identify the main document file
     */
    const val OFFICE_DOCUMENT_REL_2006 =
        "http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument"

    /**
     * Office Document relationship type (PURL/OCLC schema)
     * Alternative URI used in some Office versions
     */
    const val OFFICE_DOCUMENT_REL_PURL =
        "http://purl.oclc.org/ooxml/officeDocument/relationships/officeDocument"

    /**
     * All valid Office Document relationship types
     * Files with these relationship types are considered main document files
     */
    val OFFICE_DOCUMENT_RELS = listOf(
        OFFICE_DOCUMENT_REL_2006,
        OFFICE_DOCUMENT_REL_PURL,
    )
}
