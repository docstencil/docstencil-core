package com.docstencil.core.scanner

import com.docstencil.core.error.TemplaterException
import com.docstencil.core.scanner.model.ContentIdx
import com.docstencil.core.scanner.model.TemplateTokenGroup
import com.docstencil.core.scanner.model.XmlInputToken
import com.docstencil.core.scanner.model.XmlRawInputToken

class TemplateXmlTokenMerger(private val nicknamesToTags: Map<String, String>) {
    private class MergeCursor(
        private val xmlRawInputTokens: List<XmlRawInputToken>,
        private val templateTokenGroups: List<TemplateTokenGroup>,
        private val nicknamesToTags: Map<String, String>,
        private var currContentScannedViaRawXml: ContentIdx = ContentIdx(0),
        private var currContentScannedViaTemplateGroups: ContentIdx = ContentIdx(0),
        private var currRawXmlTokenIdx: Int = 0,
        private var currTemplateTokenGroupIdx: Int = 0,
    ) {
        fun isAtEnd(): Boolean {
            return currRawXmlToken() == null && currTemplateTokenGroup() == null
        }

        fun scan(): XmlInputToken {
            val currRawXmlToken = currRawXmlToken()
            val currTemplateGroup = currTemplateTokenGroup()

            if (currRawXmlToken == null && currTemplateGroup == null) {
                throw TemplaterException.FatalError("Merge cursor that merges raw XML tokens and template groups is at the end of the file.")
            }

            if (currRawXmlToken == null) {
                throw TemplaterException.FatalError("Merge cursor that merges raw XML tokens and template groups ran out of XML tokens before running out of template token groups.")
            }

            // Scan any non-template tokens.
            if (!currRawXmlToken.canContainTemplateExpressionText()) {
                ++currRawXmlTokenIdx
                return XmlInputToken.Raw(currRawXmlToken)
            }

            // Scan the current `TemplateTokenGroup` if it starts with the current `RawXmlToken`.
            // This always works properly because we split `RawXmlToken`s around delimiters.
            if (currTemplateGroup?.startInclusive == currContentScannedViaRawXml) {
                ++currTemplateTokenGroupIdx
                currContentScannedViaTemplateGroups = currTemplateGroup.endExclusive
                return XmlInputToken.TemplateGroup.create(currTemplateGroup, nicknamesToTags)
            }

            // Drop the `RawXmlToken` if its data is already contained within the template token
            // groups.
            // Otherwise, consume the token without emitting anything.
            val currContentToken = currRawXmlToken as XmlRawInputToken.Content
            ++currRawXmlTokenIdx
            currContentScannedViaRawXml =
                currContentScannedViaRawXml.add(currContentToken.text.length)

            return if (currContentScannedViaRawXml.value > currContentScannedViaTemplateGroups.value) {
                XmlInputToken.Raw(currRawXmlToken)
            } else {
                XmlInputToken.Sentinel()
            }
        }

        private fun currRawXmlToken(): XmlRawInputToken? {
            return xmlRawInputTokens.getOrNull(currRawXmlTokenIdx)
        }

        private fun currTemplateTokenGroup(): TemplateTokenGroup? {
            return templateTokenGroups.getOrNull(currTemplateTokenGroupIdx)
        }
    }

    /**
     * Merge the lexed `TemplateTokenGroup`s with the `RawXmlToken`s into a single list.
     */
    fun merge(
        rawTokens: List<XmlRawInputToken>,
        templateTokenGroups: List<TemplateTokenGroup>,
    ): List<XmlInputToken> {
        val cursor = MergeCursor(rawTokens, templateTokenGroups, nicknamesToTags)
        val tokens = mutableListOf<XmlInputToken>()

        while (!cursor.isAtEnd()) {
            val token = cursor.scan()
            tokens.add(token)
        }

        return tokens
    }
}
