package com.docstencil.core.scanner

import com.docstencil.core.scanner.model.TemplateToken
import com.docstencil.core.scanner.model.TemplateTokenGroup
import com.docstencil.core.scanner.model.TemplateTokenType
import com.docstencil.core.scanner.model.XmlInputToken

/**
 * Fixes template token groups by adding fake {end} tokens after INSERT statements.
 */
class TemplateTokenGroupFixer {
    private val helper = TemplateTokenGroupHelper()

    fun fix(tokens: List<XmlInputToken>): List<XmlInputToken> {
        val result = mutableListOf<XmlInputToken>()

        for (token in tokens) {
            result.add(token)

            if (token is XmlInputToken.TemplateGroup && helper.isInsertStatement(token.tokens)) {
                result.add(createFakeEndToken(token))
            }
        }

        return result
    }

    private fun createFakeEndToken(insertToken: XmlInputToken.TemplateGroup): XmlInputToken.TemplateGroup {
        val endIdx = insertToken.tokens.endExclusive
        val fakeTokens = listOf(
            TemplateToken(TemplateTokenType.DELIMITER_OPEN, "{", null, null, endIdx),
            TemplateToken(TemplateTokenType.END, "end", null, null, endIdx),
            TemplateToken(TemplateTokenType.DELIMITER_CLOSE, "}", null, null, endIdx),
        )

        val fakeGroup = TemplateTokenGroup(fakeTokens, endIdx, endIdx)
        return XmlInputToken.TemplateGroup.create(fakeGroup)
    }
}
