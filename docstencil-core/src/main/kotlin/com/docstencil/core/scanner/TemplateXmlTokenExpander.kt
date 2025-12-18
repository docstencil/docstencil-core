package com.docstencil.core.scanner

import com.docstencil.core.config.FileTypeConfig
import com.docstencil.core.error.TemplaterException
import com.docstencil.core.scanner.model.TagPartType
import com.docstencil.core.scanner.model.XmlInputToken
import com.docstencil.core.scanner.model.XmlRawInputToken

data class ContainedTagNames(
    val templateGroup: XmlInputToken.TemplateGroup,
    var seenTagNames: MutableSet<String> = mutableSetOf(),
)

class TemplateXmlTokenExpander(
    expansionRules: List<FileTypeConfig.ExpansionRule>,
    defaultTagRawXmlExpansion: String,
    rewriteFnsDefaultTargets: Map<String, XmlInputToken.ExpansionTarget>,
) {
    private val targeter = TemplateXmlTokenExpansionTargeter(
        expansionRules,
        defaultTagRawXmlExpansion,
        rewriteFnsDefaultTargets,
    )
    private val mover = TemplateXmlTokenMover()

    /**
     * Pairs loops and conditions and then expand tags.
     */
    fun expand(rawTokens: List<XmlInputToken>): List<XmlInputToken> {
        val tokens = rawTokens.toList()

        pair(tokens)
        setExpansionTargets(tokens)
        val expandedTokens = doExpand(tokens)
        checkTagsBalanced(expandedTokens)

        return expandedTokens
    }

    private fun pair(mergedTokens: List<XmlInputToken>) {
        val stack = mutableListOf<XmlInputToken.TemplateGroup>()

        for (token in mergedTokens) {
            // Skip tokens that don't require a partner.
            if (token !is XmlInputToken.TemplateGroup || !token.requiresPartner) {
                continue
            }

            if (token.expansionDirection == XmlInputToken.ExpansionDirection.LEFT) {
                // Left-expanding tokens appear before their partner.
                stack.add(token)
            } else {
                // Right-expanding tokens pair with the most current unpaired token.
                val leftToken = stack.lastOrNull() ?: throw TemplaterException.ParseError(
                    "Too many end tags.",
                    token = token.tokens.tokens.firstOrNull(),
                )
                leftToken.partner = token
                token.partner = leftToken
                token.expansionTarget = leftToken.expansionTarget
                stack.removeLast()
            }
        }

        if (stack.isNotEmpty()) {
            throw TemplaterException.ParseError(
                "Unpaired conditions and loops detected.",
                stack.firstOrNull()?.tokens?.tokens?.firstOrNull(),
            )
        }
    }

    /**
     * Infers the correct expansion for all expressions with their expansion mode set to "auto".
     */
    private fun setExpansionTargets(mergedTokens: List<XmlInputToken>) {
        val stack = mutableListOf<ContainedTagNames>()
        for (token in mergedTokens) {
            // Update contained tag names.
            val topOpenGroup = stack.lastOrNull()
            if (topOpenGroup != null && token is XmlInputToken.Raw && token.xml is XmlRawInputToken.TagPart) {
                topOpenGroup.seenTagNames.add(token.xml.tagName)
            }

            // Skip tokens that don't need expansion.
            if (token !is XmlInputToken.TemplateGroup) {
                continue
            }
            if (token.expansionTarget !is XmlInputToken.ExpansionTarget.Auto) {
                continue
            }
            // "auto" for unpaired tokens means no expansion.
            val tokenPartner = token.partner
            if (tokenPartner == null) {
                token.expansionTarget = XmlInputToken.ExpansionTarget.None()
                continue
            }

            // "left" tokens are put on the stack.
            if (token.expansionDirection == XmlInputToken.ExpansionDirection.LEFT) {
                stack.add(ContainedTagNames(token))
                continue
            }

            if (topOpenGroup == null) {
                throw TemplaterException.ParseError(
                    "Right template group with no matching left group found.",
                    token.tokens.tokens.firstOrNull(),
                )
            }
            // We found a "right" token and can set its expansion
            val expansionTarget: XmlInputToken.ExpansionTarget =
                targeter.calculateTarget(topOpenGroup, tokenPartner.tokens)
            token.expansionTarget = expansionTarget
            tokenPartner.expansionTarget = expansionTarget

            stack.removeLast()

            // The new stack top also has seen all the tags that the previous stack top has seen.
            val newTopOpenGroup = stack.lastOrNull()
            newTopOpenGroup?.seenTagNames?.addAll(topOpenGroup.seenTagNames)
        }
    }

    private fun doExpand(mergedTokens: List<XmlInputToken>): List<XmlInputToken> {
        val tokens = mergedTokens.toMutableList()

        // Expand left.
        for (idx in 0..<tokens.size) {
            val currToken = tokens[idx]
            if (currToken is XmlInputToken.TemplateGroup &&
                currToken.expansionDirection == XmlInputToken.ExpansionDirection.LEFT &&
                currToken.expansionTarget is XmlInputToken.ExpansionTarget.Tag
            ) {
                expandTokenLeft(tokens, idx, currToken.expansionTarget.value as String)
            }
        }

        // Expand right.
        for (idx in (tokens.size - 1) downTo 0) {
            val currToken = tokens[idx]
            if (currToken is XmlInputToken.TemplateGroup &&
                currToken.expansionDirection == XmlInputToken.ExpansionDirection.RIGHT &&
                currToken.expansionTarget is XmlInputToken.ExpansionTarget.Tag
            ) {
                expandTokenRight(tokens, idx, currToken.expansionTarget.value as String)
            }
        }

        // Expand leftmost.
        for (idx in 0..<tokens.size) {
            val currToken = tokens[idx]
            if (currToken is XmlInputToken.TemplateGroup &&
                currToken.expansionDirection == XmlInputToken.ExpansionDirection.LEFT &&
                currToken.expansionTarget is XmlInputToken.ExpansionTarget.Outermost
            ) {
                expandTokenLeftmost(tokens, idx)
            }
        }

        // There currently don't exist any use cases for a rightmost expansion.

        return tokens
    }

    private fun expandTokenLeftmost(tokens: MutableList<XmlInputToken>, idx: Int) {
        mover.moveTokenToLeftmost(tokens, idx)
    }

    private fun expandTokenLeft(tokens: MutableList<XmlInputToken>, idx: Int, tagName: String) {
        mover.moveTokenToTarget(
            tokens,
            idx,
            tagName,
            TemplateXmlTokenMover.ExpansionConfig.forLeft(),
        )
    }

    private fun expandTokenRight(tokens: MutableList<XmlInputToken>, idx: Int, tagName: String) {
        mover.moveTokenToTarget(
            tokens,
            idx,
            tagName,
            TemplateXmlTokenMover.ExpansionConfig.forRight(),
        )
    }

    private fun checkTagsBalanced(tokens: List<XmlInputToken>) {
        val openTagCountByToken = mutableMapOf<XmlInputToken.TemplateGroup, Int>()
        var openTagCount = 0
        for (token in tokens) {
            if (token is XmlInputToken.Raw &&
                token.xml is XmlRawInputToken.TagPart
            ) {
                if (token.xml.tagPartType == TagPartType.OPENING) {
                    ++openTagCount
                }
                if (token.xml.tagPartType == TagPartType.CLOSING) {
                    --openTagCount
                }
            }
            if (token is XmlInputToken.TemplateGroup &&
                token.partner != null
            ) {
                when (token.expansionDirection) {
                    XmlInputToken.ExpansionDirection.LEFT -> {
                        openTagCountByToken[token] = openTagCount
                    }

                    XmlInputToken.ExpansionDirection.RIGHT -> {
                        val leftCount: Int = openTagCountByToken[token.partner]
                            ?: throw TemplaterException.FatalError(
                                "No matching token count found.",
                                token.tokens.tokens.firstOrNull(),
                            )
                        if (leftCount != openTagCount) {
                            throw TemplaterException.ParseError(
                                "The XML tags between the beginning and end of the 'for' or 'if' statement are not balanced.",
                                token.tokens.tokens.firstOrNull(),
                            )
                        }
                    }
                }
            }
        }
    }
}
