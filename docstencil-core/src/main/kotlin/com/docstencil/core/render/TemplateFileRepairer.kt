package com.docstencil.core.render

import com.docstencil.core.api.OfficeFileType
import com.docstencil.core.config.FileTypeConfig
import com.docstencil.core.render.model.XmlOutputToken
import com.docstencil.core.scanner.model.TagPartType

/**
 * Repairs Office document XML to ensure valid structure after template rendering.
 */
class TemplateFileRepairer(
    private val fileType: OfficeFileType,
    private val repairRules: List<FileTypeConfig.RepairRule>,
) {
    /**
     * Repair tokens by applying all document-fixing phases.
     *
     * @param tokens List of rendered XML tokens
     * @return List of repaired tokens
     */
    fun repair(tokens: List<XmlOutputToken>): List<XmlOutputToken> {
        var processed = tokens

        if (fileType == OfficeFileType.DOCX) {
            processed = addEmptyParagraphAfterTables(processed)
        }

        if (repairRules.isNotEmpty()) {
            processed = applyStructuralRules(processed)
        }

        processed = ensureSpacePreserve(processed)
        processed = cleanupSpacePreserve(processed)

        return processed
    }

    /**
     * Insert empty paragraphs after tables to prevent document corruption.
     *
     * Word's XML schema has implicit requirements about what can follow tables.
     * When a table is followed by a run without a paragraph wrapper, Word requires
     * an empty paragraph to maintain document structure.
     */
    private fun addEmptyParagraphAfterTables(tokens: List<XmlOutputToken>): List<XmlOutputToken> {
        val result = mutableListOf<XmlOutputToken>()
        var lastTagPart: XmlOutputToken.TagPart? = null

        for (token in tokens) {
            // Check if we need to insert a paragraph before this token
            if (lastTagPart?.tagName == "w:tbl" &&
                lastTagPart.tagPartType == TagPartType.CLOSING
            ) {
                val needsParagraph = when (token) {
                    is XmlOutputToken.TagPart -> {
                        // Don't insert if next is paragraph, table, sectPr, or closing tag.
                        token.tagName != "w:p" &&
                                token.tagName != "w:tbl" &&
                                token.tagName != "w:sectPr" &&
                                token.tagPartType != TagPartType.CLOSING
                    }

                    is XmlOutputToken.Content -> {
                        // Content after table needs paragraph wrapper
                        token.text?.isNotBlank() == true
                    }

                    else -> false
                }

                if (needsParagraph) {
                    // Insert empty paragraph
                    result.add(
                        XmlOutputToken.TagPart(
                            tagName = "w:p",
                            tagPartType = TagPartType.SELF_CLOSING,
                            rawText = "<w:p/>",
                        ),
                    )
                }
            }

            result.add(token)

            // Track the last TagPart (skip whitespace-only content and bookmarks).
            when (token) {
                is XmlOutputToken.TagPart -> {
                    // Skip bookmarkEnd tags.
                    if (token.tagName != "w:bookmarkEnd") {
                        lastTagPart = token
                    }
                }

                is XmlOutputToken.Content -> {
                    // Only skip whitespace-only content.
                    if (token.text?.isNotBlank() == true) {
                        lastTagPart = null
                    }
                }

                else -> {}
            }
        }

        return result
    }

    /**
     * Apply structural validation rules to ensure valid Office XML.
     */
    private fun applyStructuralRules(tokens: List<XmlOutputToken>): List<XmlOutputToken> {
        var processed = tokens.toMutableList()

        for (rule in repairRules) {
            processed = applyRule(processed, rule).toMutableList()
        }

        return processed
    }

    private fun applyRule(
        tokens: MutableList<XmlOutputToken>,
        rule: FileTypeConfig.RepairRule,
    ): List<XmlOutputToken> {
        val (tag, shouldContain, action) = rule

        // State machine variables
        var collecting = mutableListOf<XmlOutputToken>()
        var startIndex = -1
        var foundRequiredChild = false

        val result = mutableListOf<XmlOutputToken>()
        var i = 0

        while (i < tokens.size) {
            val token = tokens[i]

            // STATE 1: Currently collecting content inside a tag
            if (startIndex != -1) {
                val isClosingTag = token is XmlOutputToken.TagPart &&
                        token.tagName == tag &&
                        token.tagPartType == TagPartType.CLOSING

                if (isClosingTag) {
                    // Found closing tag - decide what to do
                    if (foundRequiredChild) {
                        // Valid structure - add all collected tokens and closing tag
                        result.addAll(collecting)
                        result.add(token)
                    } else {
                        // Invalid structure - apply fix
                        when (action) {
                            is FileTypeConfig.RepairRule.DropWithParent -> {
                                // Search backwards in result to find parent opening tag
                                val parentStart = result.indexOfLast { t ->
                                    t is XmlOutputToken.TagPart &&
                                            t.tagName == action.parentTag &&
                                            t.tagPartType == TagPartType.OPENING
                                }

                                if (parentStart != -1) {
                                    // Remove everything from parent start
                                    while (result.size > parentStart) {
                                        result.removeLast()
                                    }
                                    // Skip forward to find parent closing tag
                                    var depth = 1
                                    i++
                                    while (i < tokens.size && depth > 0) {
                                        val t = tokens[i]
                                        if (t is XmlOutputToken.TagPart && t.tagName == action.parentTag) {
                                            when (t.tagPartType) {
                                                TagPartType.OPENING -> depth++
                                                TagPartType.CLOSING -> depth--
                                                TagPartType.SELF_CLOSING -> {}
                                            }
                                        }
                                        i++
                                    }
                                    i-- // Will be incremented at end of loop
                                }
                            }

                            is FileTypeConfig.RepairRule.Drop -> {
                                // Drop entire tag - don't add anything
                            }

                            is FileTypeConfig.RepairRule.Insert -> {
                                // Insert minimum valid content
                                // Add opening tag
                                val openingToken = collecting.firstOrNull()
                                if (openingToken != null) {
                                    result.add(openingToken)
                                }
                                // Add any properties (non-required-child tags between opening and closing)
                                for (j in 1 until collecting.size) {
                                    result.add(collecting[j])
                                }
                                // Add the default content
                                result.addAll(XmlOutputToken.fromString(action.value))
                                // Add closing tag
                                result.add(token)
                            }
                        }
                    }

                    // Reset state
                    startIndex = -1
                    collecting = mutableListOf()
                    foundRequiredChild = false
                } else {
                    // Still collecting
                    collecting.add(token)

                    // Check if we found a required child tag
                    if (!foundRequiredChild && token is XmlOutputToken.TagPart) {
                        if (shouldContain.contains(token.tagName) &&
                            (token.tagPartType == TagPartType.OPENING ||
                                    token.tagPartType == TagPartType.SELF_CLOSING)
                        ) {
                            foundRequiredChild = true
                        }
                    }
                }
                i++
                continue
            }

            // STATE 2: Not collecting - check if we should start
            if (token is XmlOutputToken.TagPart && token.tagName == tag) {
                when (token.tagPartType) {
                    TagPartType.SELF_CLOSING -> {
                        // Self-closing empty tag - handle immediately
                        when (action) {
                            is FileTypeConfig.RepairRule.Drop,
                            is FileTypeConfig.RepairRule.DropWithParent,
                                -> {
                                // Drop it - don't add to result
                            }

                            is FileTypeConfig.RepairRule.Insert -> {
                                // Replace with full tag containing default value
                                val openTag = token.rawText.replace("/>", ">")
                                result.add(
                                    XmlOutputToken.TagPart(
                                        tagName = tag,
                                        tagPartType = TagPartType.OPENING,
                                        rawText = openTag,
                                    ),
                                )
                                result.addAll(XmlOutputToken.fromString(action.value))
                                result.add(
                                    XmlOutputToken.TagPart(
                                        tagName = tag,
                                        tagPartType = TagPartType.CLOSING,
                                        rawText = "</$tag>",
                                    ),
                                )
                            }
                        }
                    }

                    TagPartType.OPENING -> {
                        // Start collecting
                        startIndex = i
                        collecting = mutableListOf(token)
                        foundRequiredChild = false
                    }

                    TagPartType.CLOSING -> {
                        // Unexpected closing tag - just add it
                        result.add(token)
                    }
                }
            } else {
                result.add(token)
            }

            i++
        }

        return result
    }

    /**
     * Ensure text tags with significant whitespace have xml:space="preserve".
     *
     * When rewriters inject closing tags like `</w:t></w:r>`, the preceding content
     * ends up in a `<w:t>` without the preserve attribute. This causes Word to
     * collapse leading/trailing whitespace. This pass adds the attribute where needed.
     */
    private fun ensureSpacePreserve(tokens: List<XmlOutputToken>): List<XmlOutputToken> {
        if (tokens.isEmpty()) {
            return tokens
        }

        val result = mutableListOf<XmlOutputToken>()

        for (i in tokens.indices) {
            val token = tokens[i]

            val isTextTokenStart = token is XmlOutputToken.TagPart &&
                    token.tagName == "w:t" &&
                    token.tagPartType == TagPartType.OPENING
            if (!isTextTokenStart) {
                result.add(token)
                continue
            }

            val alreadyHasSpacePreserve = token.rawText.contains("xml:space=\"preserve\"")
            if (alreadyHasSpacePreserve) {
                result.add(token)
                continue
            }

            val contentTokens = mutableListOf<XmlOutputToken.Content>()
            for (j in (i + 1)..<tokens.size) {
                val lookAheadToken = tokens[j]

                if (lookAheadToken is XmlOutputToken.Content) {
                    contentTokens.add(lookAheadToken)
                }

                val isClosingTag = lookAheadToken is XmlOutputToken.TagPart &&
                        lookAheadToken.tagName == "w:t" &&
                        lookAheadToken.tagPartType == TagPartType.CLOSING
                if (isClosingTag) {
                    break
                }
            }

            val hasSignificantWhitespace = contentTokens.any { content ->
                val text = content.text ?: return@any false
                text.startsWith(' ') || text.startsWith('\t') ||
                        text.endsWith(' ') || text.endsWith('\t')
            }

            val finalToken = if (hasSignificantWhitespace) {
                // Add xml:space="preserve" to the opening tag.
                val newRawText = token.rawText.replace("<w:t", "<w:t xml:space=\"preserve\"")
                XmlOutputToken.TagPart(
                    tagName = token.tagName,
                    tagPartType = token.tagPartType,
                    rawText = newRawText,
                )
            } else {
                token
            }

            result.add(finalToken)
        }

        return result
    }

    /**
     * Clean up empty space-preserve tags to optimize output.
     *
     * After template rendering, we may have text tags with space preservation attributes
     * but no actual content. Convert them to self-closing tags.
     */
    private fun cleanupSpacePreserve(tokens: List<XmlOutputToken>): List<XmlOutputToken> {
        if (tokens.size < 2) {
            return tokens
        }

        val result = mutableListOf<XmlOutputToken>()
        result.add(tokens[0])

        for (i in 1..<tokens.size) {
            val prevTag = result.last()
            val currTag = tokens[i]

            val prevIsOpeningTextTag =
                prevTag is XmlOutputToken.TagPart && prevTag.tagName == "w:t" && prevTag.tagPartType == TagPartType.OPENING
            val currIsClosingTextTag =
                currTag is XmlOutputToken.TagPart && currTag.tagName == "w:t" && currTag.tagPartType == TagPartType.CLOSING

            if (!prevIsOpeningTextTag || !currIsClosingTextTag) {
                result.add(currTag)
                continue
            }

            val prevHasSpacePreservingAttribute = prevTag.rawText.contains("xml:space=\"preserve\"")
            if (!prevHasSpacePreservingAttribute) {
                result.add(currTag)
                continue
            }

            // Replace previous opening text tag with a self-closing text tag.
            result[result.size - 1] = XmlOutputToken.TagPart(
                tagName = "w:t",
                tagPartType = TagPartType.SELF_CLOSING,
                rawText = "<w:t/>",
            )
        }

        return result
    }
}
