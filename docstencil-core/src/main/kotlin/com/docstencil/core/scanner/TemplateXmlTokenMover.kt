package com.docstencil.core.scanner

import com.docstencil.core.error.TemplaterException
import com.docstencil.core.scanner.model.TagPartType
import com.docstencil.core.scanner.model.TemplateTokenType
import com.docstencil.core.scanner.model.XmlInputToken
import com.docstencil.core.scanner.model.XmlRawInputToken

class TemplateXmlTokenMover {
    data class ExpansionConfig(
        val indexDelta: Int,
        val targetTagPartTypes: Set<TagPartType>,
        val checkIndexForTarget: Int,
    ) {
        companion object {
            fun forLeft() = ExpansionConfig(
                indexDelta = -1,
                targetTagPartTypes = setOf(TagPartType.OPENING, TagPartType.SELF_CLOSING),
                checkIndexForTarget = 1,
            )

            fun forRight() = ExpansionConfig(
                indexDelta = 1,
                targetTagPartTypes = setOf(TagPartType.CLOSING, TagPartType.SELF_CLOSING),
                checkIndexForTarget = -1,
            )
        }
    }

    fun moveTokenToTarget(
        tokens: MutableList<XmlInputToken>,
        tokenToMoveIdx: Int,
        targetTagName: String,
        config: ExpansionConfig,
    ) {
        var currIdx = tokenToMoveIdx

        while (true) {
            val targetCheckIdx = currIdx + config.checkIndexForTarget
            val targetCheckToken = tokens.getOrNull(targetCheckIdx)

            if (isTargetTag(targetCheckToken, targetTagName, config.targetTagPartTypes)) {
                break
            }

            val candidateIdx = currIdx + config.indexDelta
            val candidateToken = tokens.getOrNull(candidateIdx)

            // Don't expand over template groups (would change control flow).
            if (isBlockingTemplateGroup(candidateToken)) {
                break
            }

            if (candidateToken == null) {
                val errorLocation = tokens[currIdx]
                val errorLocationToken = if (errorLocation is XmlInputToken.TemplateGroup) {
                    errorLocation.tokens.tokens.firstOrNull()
                } else {
                    null
                }
                throw TemplaterException.ParseError(
                    "Cannot expand tag further ${if (config.indexDelta < 0) "left" else "right"}.",
                    errorLocationToken,
                )
            }

            swapTokens(tokens, currIdx, candidateIdx)
            currIdx += config.indexDelta
        }
    }

    private fun isTargetTag(
        token: XmlInputToken?,
        tagName: String,
        allowedTagPartTypes: Set<TagPartType>,
    ): Boolean {
        return token is XmlInputToken.Raw &&
                token.xml is XmlRawInputToken.TagPart &&
                token.xml.tagName == tagName &&
                allowedTagPartTypes.contains(token.xml.tagPartType)
    }

    private fun isBlockingTemplateGroup(token: XmlInputToken?): Boolean {
        return token is XmlInputToken.TemplateGroup && token.tokens.tokens
            .any {
                it.type != TemplateTokenType.DELIMITER_OPEN &&
                        it.type != TemplateTokenType.DELIMITER_CLOSE &&
                        it.type != TemplateTokenType.NULL
            }
    }

    fun moveTokenToLeftmost(
        tokens: MutableList<XmlInputToken>,
        tokenToMoveIdx: Int,
    ) {
        var currIdx = tokenToMoveIdx

        while (true) {
            val candidateIdx = currIdx - 1

            // Stop condition 1: Reached beginning of document.
            if (candidateIdx < 0) {
                break
            }

            val candidateToken = tokens.getOrNull(candidateIdx)

            // Stop condition 2: Hit another template group (would change control flow).
            if (isBlockingTemplateGroup(candidateToken)) {
                break
            }

            swapTokens(tokens, currIdx, candidateIdx)
            currIdx = candidateIdx
        }
    }

    private fun swapTokens(
        tokens: MutableList<XmlInputToken>,
        idx1: Int,
        idx2: Int,
    ) {
        val temp = tokens[idx1]
        tokens[idx1] = tokens[idx2]
        tokens[idx2] = temp
    }
}
