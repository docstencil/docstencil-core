package com.docstencil.core.scanner

import com.docstencil.core.config.FileTypeConfig
import com.docstencil.core.scanner.model.TemplateTokenGroup
import com.docstencil.core.scanner.model.TemplateTokenType
import com.docstencil.core.scanner.model.XmlInputToken


class TemplateXmlTokenExpansionTargeter(
    private val expansionRules: List<FileTypeConfig.ExpansionRule>,
    private val defaultTagRawXmlExpansion: String,
    private val rewriteFnsDefaultTargets: Map<String, XmlInputToken.ExpansionTarget>,
) {
    val helper = TemplateTokenGroupHelper()

    fun calculateTarget(
        containedTagNames: ContainedTagNames,
        tokenGroup: TemplateTokenGroup,
    ): XmlInputToken.ExpansionTarget {
        if (helper.isInsertStatement(tokenGroup)) {
            return calcForInsert(tokenGroup)
        }
        return calcDefault(containedTagNames)
    }

    private fun calcForInsert(tokenGroup: TemplateTokenGroup): XmlInputToken.ExpansionTarget {
        for (token in tokenGroup.tokens) {
            if (token.type != TemplateTokenType.IDENTIFIER) {
                continue
            }
            return rewriteFnsDefaultTargets[token.lexeme] ?: continue
        }
        return XmlInputToken.ExpansionTarget.Tag(defaultTagRawXmlExpansion)
    }

    private fun calcDefault(containedTagNames: ContainedTagNames): XmlInputToken.ExpansionTarget {
        var expansionTarget: XmlInputToken.ExpansionTarget =
            XmlInputToken.ExpansionTarget.None()
        for (rule in expansionRules) {
            if (containedTagNames.seenTagNames.contains(rule.contains) &&
                !containedTagNames.seenTagNames.contains(rule.notContains)
            ) {
                expansionTarget = XmlInputToken.ExpansionTarget.Tag(rule.expand)
            }
        }
        return expansionTarget
    }
}
