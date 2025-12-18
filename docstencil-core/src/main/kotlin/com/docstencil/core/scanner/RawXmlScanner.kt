package com.docstencil.core.scanner

import com.docstencil.core.api.OfficeTemplateOptions
import com.docstencil.core.error.TemplaterException
import com.docstencil.core.scanner.model.TagPartType
import com.docstencil.core.scanner.model.XmlRawInputToken

class RawXmlScanner(
    private val delimiters: OfficeTemplateOptions.Delimiters?,
    private val templateTagNames: Set<String>,
) {
    private class Cursor(
        private val rawXml: String,
        private val textTagNames: Set<String>,
        private var idx: Int = 0,
        private var isInsideTextTag: Boolean = false,
    ) {
        fun nextToken(): XmlRawInputToken? {
            val token = parseNextToken(isInsideTextTag)
            isInsideTextTag = updateIsInsideTextTag(token, isInsideTextTag)
            return token
        }

        private fun parseNextToken(isInsideTextTag: Boolean): XmlRawInputToken? {
            // We are done.
            if (rawXml.length <= idx) {
                return null
            }
            // Handle optional XML preamble.
            if (idx == 0 && rawXml.startsWith("<?")) {
                val nextTagEnd = rawXml.indexOf(">")
                idx = nextTagEnd + 1
                @Suppress("ReplaceSubstringWithTake")
                return XmlRawInputToken.Verbatim.fromRawText(0, rawXml.substring(0, idx))
            }

            val nextTagStart = rawXml.indexOf("<", idx)

            // Handle content at the end of the XML file.
            if (nextTagStart == -1) {
                val remainingText = rawXml.substring(idx)
                val currPartStartIdx = idx
                idx = rawXml.length
                return XmlRawInputToken.Content.fromRawText(currPartStartIdx, remainingText, false)
            }

            if (nextTagStart == idx) {
                // We are parsing a tag part.
                val nextTagEnd = rawXml.indexOf(">", idx)
                val rawText = rawXml.substring(idx, nextTagEnd + 1)
                idx = nextTagEnd + 1
                return XmlRawInputToken.TagPart.fromRawText(nextTagStart, rawText, textTagNames)
            } else {
                // We are parsing content.
                val rawText = rawXml.substring(idx, nextTagStart)
                val currPartStartIdx = idx
                idx = nextTagStart
                return XmlRawInputToken.Content.fromRawText(
                    currPartStartIdx,
                    rawText,
                    isInsideTextTag,
                )
            }
        }

        private fun updateIsInsideTextTag(
            token: XmlRawInputToken?,
            isInsideTextTag: Boolean,
        ): Boolean {
            if (token == null) {
                // `null` indicates that the end of the document.
                // Ending inside a tag indicates a malformed XML document.
                if (isInsideTextTag) {
                    throw TemplaterException.XmlError(
                        "XML document ended while one of the following tags is still open: " +
                                "${textTagNames.joinToString(", ")}}",
                        rawXml.length - 1,
                    )
                }
                return false
            }

            val isTextTagPart =
                token is XmlRawInputToken.TagPart && textTagNames.contains(token.tagName)
            if (isTextTagPart && token.tagPartType == TagPartType.OPENING) {
                if (isInsideTextTag) {
                    throw TemplaterException.XmlError(
                        "XML document opened a text tag while a previous text tag is still unclosed: ${token.tagName}}",
                        token.position,
                    )
                }
                return true
            }
            if (isTextTagPart && token.tagPartType == TagPartType.CLOSING) {
                if (!isInsideTextTag) {
                    throw TemplaterException.XmlError(
                        "XML document closes tag that was never opened: ${token.tagName}}",
                        token.position,
                    )
                }
                return false
            }
            return isInsideTextTag
        }
    }

    fun scan(rawXml: String): List<XmlRawInputToken> {
        // Tokens as found in the XML files.
        val rawTokens = mutableListOf<XmlRawInputToken>()

        val cursor = Cursor(rawXml, templateTagNames)
        while (true) {
            val token = cursor.nextToken() ?: break
            rawTokens.add(token)
        }

        // Tokens split in a way that each content part is either:
        //  - 100% verbatim content or
        //  - 100% template expression or
        return splitContentPartsAtDelimiters(rawTokens)
    }

    /**
     * Split content tokens at possible delimiter positions.
     * This allows a mapping between the AST and the flat list of tokens.
     */
    private fun splitContentPartsAtDelimiters(rawTokens: List<XmlRawInputToken>): List<XmlRawInputToken> {
        if (rawTokens.isEmpty()) {
            return listOf()
        }

        val tokens = mutableListOf<XmlRawInputToken>()

        val delimiterPosIter = delimiterPositions(rawTokens).sorted().iterator()
        var delimiterPos = if (delimiterPosIter.hasNext()) {
            delimiterPosIter.next()
        } else {
            Int.MAX_VALUE
        }

        val rawTokensIter = rawTokens.iterator()
        var rawToken = rawTokensIter.next()
        var rawTokenStartPos = 0

        while (true) {
            // Tokens not relevant for templating are passed through.
            if (!rawToken.canContainTemplateExpressionText()) {
                tokens.add(rawToken)
                if (!rawTokensIter.hasNext()) {
                    break
                }
                rawToken = rawTokensIter.next()
                continue
            }

            // Next split occurs in a future token.
            if (delimiterPos >= rawTokenStartPos + rawToken.logicalText.length) {
                tokens.add(rawToken)
                if (!rawTokensIter.hasNext()) {
                    break
                }
                rawTokenStartPos += rawToken.logicalText.length
                rawToken = rawTokensIter.next()
                continue
            }

            // Split occurs at the beginning of token and therefore already exists.
            if (delimiterPos == rawTokenStartPos) {
                delimiterPos = if (delimiterPosIter.hasNext()) {
                    delimiterPosIter.next()
                } else {
                    Int.MAX_VALUE
                }
                continue
            }

            // Split must occur inside the current token.
            // Split the token.
            val part1Length = delimiterPos - rawTokenStartPos
            val part1 = rawToken.logicalText.substring(0, part1Length)
            val part2 = rawToken.logicalText.substring(part1Length)

            // Finalize the first half.
            tokens.add(XmlRawInputToken.Content.fromRawText(rawTokenStartPos, part1, true))
            // Keep the second half (there might be more splits coming).
            rawTokenStartPos += part1Length
            rawToken = XmlRawInputToken.Content.fromRawText(rawTokenStartPos, part2, true)

            // Advance the pos iterator.
            delimiterPos = if (delimiterPosIter.hasNext()) {
                delimiterPosIter.next()
            } else {
                Int.MAX_VALUE
            }
        }

        return tokens
    }

    /**
     * Returns a list of positions where we need to split content parts to have delimiters split off
     * from verbatim content parts and template expression content parts.
     * As a side effect, we also split off verbatim content parts from template content parts.
     *
     * The indices are character offsets of the string representing all raw test in content parts
     * in the document, complete ignoring tag parts.
     *
     * Example:
     *  `[Tag("{"), Content("0{2"), Tag("nothing in here matters"), Content("3}")]`
     *  --> `Set(1, 2, 4, 5)`
     *
     * The function returns beginning and end indices of delimiters to be able to handle
     * multi-character delimiters.
     */
    private fun delimiterPositions(rawTokens: List<XmlRawInputToken>): Set<Int> {
        if (delimiters == null) {
            return emptySet()
        }

        val positions = mutableSetOf<Int>()

        val rawContentText = XmlRawInputToken.extractTextContent(rawTokens)

        for (delimiter in listOf(delimiters.open, delimiters.close)) {
            var currIdx = 0
            while (currIdx < rawContentText.length) {
                val nextDelimiterPosition = rawContentText.indexOf(delimiter, currIdx)
                if (nextDelimiterPosition == -1) {
                    break
                }
                positions.add(nextDelimiterPosition)
                positions.add(nextDelimiterPosition + delimiter.length)
                currIdx = nextDelimiterPosition + delimiter.length
            }
        }

        return positions
    }
}
