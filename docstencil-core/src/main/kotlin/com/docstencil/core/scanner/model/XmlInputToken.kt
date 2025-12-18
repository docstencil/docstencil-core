package com.docstencil.core.scanner.model

import com.docstencil.core.error.TemplaterException


/**
 * An XML token that groups parsed template expressions. This is useful for rewriting token order
 * during condition and loop expansion.
 */
sealed class XmlInputToken {
    enum class ExpansionDirection {
        LEFT, RIGHT,
    }

    sealed class ExpansionTarget(open val value: String?) {
        data class None(override val value: String? = null) : ExpansionTarget(value)

        data class Auto(override val value: String = "auto") : ExpansionTarget(value)

        data class Tag(override val value: String) : ExpansionTarget(value)

        data class Outermost(override val value: String? = null) : ExpansionTarget(value)
    }

    data class Raw(val xml: XmlRawInputToken) : XmlInputToken()

    // Is left behind by moved `TemplateGroups` to be able to clean up empty tags.
    data class Sentinel(val ignore: Boolean = true) : XmlInputToken()

    data class TemplateGroup(
        val tokens: TemplateTokenGroup,
        val expansionDirection: ExpansionDirection,
        var expansionTarget: ExpansionTarget,
        val requiresPartner: Boolean,
        var partner: TemplateGroup? = null,
    ) : XmlInputToken() {
        // Override methods to exclude partner (circular reference).
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is TemplateGroup) return false
            if (tokens != other.tokens) return false
            if (expansionDirection != other.expansionDirection) return false
            if (expansionTarget != other.expansionTarget) return false
            if (requiresPartner != other.requiresPartner) return false
            return true
        }

        override fun hashCode(): Int {
            var result = tokens.hashCode()
            result = 31 * result + expansionDirection.hashCode()
            result = 31 * result + expansionTarget.hashCode()
            result = 31 * result + requiresPartner.hashCode()
            return result
        }

        override fun toString(): String {
            return "TemplateGroup(requiresPartner=$requiresPartner, expansionTarget=$expansionTarget, expansionDirection=$expansionDirection, tokens=$tokens)"
        }

        companion object {
            fun create(
                group: TemplateTokenGroup,
                nicknamesToTags: Map<String, String> = mapOf(),
            ): TemplateGroup {
                // 'END' tokens expand to right, everything else expands to the left.
                val expansionDirection =
                    if (group.tokens.size >= 2 &&
                        group.tokens[0].type == TemplateTokenType.DELIMITER_OPEN &&
                        group.tokens[1].type == TemplateTokenType.END
                    ) {
                        ExpansionDirection.RIGHT
                    } else {
                        ExpansionDirection.LEFT
                    }

                val hasAtPrefix =
                    group.tokens.size >= 4 &&
                            group.tokens[0].type == TemplateTokenType.DELIMITER_OPEN &&
                            group.tokens[1].type == TemplateTokenType.AT &&
                            group.tokens[2].type == TemplateTokenType.STRING

                val hasDoPrefix =
                    group.tokens.size >= 3 &&
                            group.tokens[0].type == TemplateTokenType.DELIMITER_OPEN &&
                            (group.tokens[1].type == TemplateTokenType.DO ||
                                    (hasAtPrefix && group.tokens.size >= 4 && group.tokens[3].type == TemplateTokenType.DO))

                val hasVarPrefix =
                    group.tokens.size >= 3 &&
                            group.tokens[0].type == TemplateTokenType.DELIMITER_OPEN &&
                            group.tokens[1].type == TemplateTokenType.VAR

                val targetTagName: String? = if (hasAtPrefix) {
                    group.tokens[2].literal as String
                } else {
                    null
                }

                val expansionTarget =
                    when {
                        hasDoPrefix -> ExpansionTarget.Outermost()
                        hasVarPrefix -> ExpansionTarget.Outermost()
                        hasAtPrefix && targetTagName != null -> {
                            val resolvedTagName = resolveTagName(targetTagName, nicknamesToTags)
                            ExpansionTarget.Tag(resolvedTagName)
                        }

                        else -> ExpansionTarget.Auto()
                    }

                // 'FOR', 'IF', 'INSERT', 'REWRITE' and 'END' tokens require that their template
                // expressions are paired.
                val requiresPartner =
                    when {
                        hasDoPrefix -> false

                        // FOR, IF, INSERT, REWRITE at position 1 (without @ prefix)
                        group.tokens.size >= 4 &&
                                group.tokens[0].type == TemplateTokenType.DELIMITER_OPEN &&
                                (group.tokens[1].type == TemplateTokenType.FOR ||
                                        group.tokens[1].type == TemplateTokenType.IF ||
                                        group.tokens[1].type == TemplateTokenType.INSERT ||
                                        group.tokens[1].type == TemplateTokenType.REWRITE) -> true

                        // FOR, IF, INSERT, REWRITE at position 3 (with @ prefix: {@"tag" for ...})
                        hasAtPrefix &&
                                group.tokens.size >= 6 &&
                                (group.tokens[3].type == TemplateTokenType.FOR ||
                                        group.tokens[3].type == TemplateTokenType.IF ||
                                        group.tokens[3].type == TemplateTokenType.INSERT ||
                                        group.tokens[3].type == TemplateTokenType.REWRITE) -> true

                        // END token
                        group.tokens.size >= 2 &&
                                group.tokens[0].type == TemplateTokenType.DELIMITER_OPEN &&
                                group.tokens[1].type == TemplateTokenType.END -> true

                        else -> false
                    }

                val containsPartnerTag =
                    group.tokens.any {
                        it.type == TemplateTokenType.FOR ||
                                it.type == TemplateTokenType.IF ||
                                it.type == TemplateTokenType.INSERT ||
                                it.type == TemplateTokenType.REWRITE
                    }
                if (!requiresPartner && containsPartnerTag) {
                    throw TemplaterException.ParseError(
                        "Loops and conditions must be placed at the start of a template " +
                                "expression or after the initial '@' command.",
                        group.tokens.firstOrNull(),
                    )
                }

                return TemplateGroup(group, expansionDirection, expansionTarget, requiresPartner)
            }

            private fun resolveTagName(
                rawTagName: String,
                nicknamesToTags: Map<String, String>,
            ): String {
                return nicknamesToTags[rawTagName] ?: rawTagName
            }
        }
    }
}
