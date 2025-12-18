package com.docstencil.core.scanner.model

import com.docstencil.core.error.TemplaterException
import com.docstencil.core.utils.DocUtils


/**
 * A raw XML token that is not aware of the templating language.
 *
 * @property position The position at which the token starts in the XML file
 */
sealed class XmlRawInputToken(open val position: Int) {
    companion object {
        fun extractTextContent(tokens: List<XmlRawInputToken>): String {
            return tokens.mapNotNull { token ->
                if (token is Content && token.isInsideTextTag) {
                    token.text
                } else {
                    null
                }
            }
                .joinToString("")
        }
    }

    fun canContainTemplateExpressionText(): Boolean {
        return this is Content && this.isInsideTextTag
    }

    val xmlString: String
        get() {
            return when (this) {
                is Content -> DocUtils.escapeXmlString(this.text)
                is Verbatim -> this.rawText
                is TagPart -> this.rawText
            }
        }

    val logicalText: String
        get() {
            return when (this) {
                is Content -> this.text
                is Verbatim -> this.rawText
                is TagPart -> this.rawText
            }
        }

    data class TagPart(
        val tagName: String,
        override val position: Int,
        val rawText: String,
        val tagPartType: TagPartType,
        val isTextTag: Boolean,
    ) : XmlRawInputToken(position) {
        companion object {
            fun fromRawText(position: Int, rawText: String, textTagNames: Set<String>): TagPart {
                val tagName = extractTagName(rawText)
                return TagPart(
                    tagName,
                    position,
                    rawText,
                    extractTagPartType(rawText, position),
                    textTagNames.contains(tagName),
                )
            }

            private fun extractTagName(rawText: String): String {
                val trimmedText = rawText.trim { it == '<' || it == '>' || it == '/' }
                return trimmedText.split(" ")[0]
            }

            private fun extractTagPartType(rawText: String, position: Int): TagPartType {
                return when {
                    rawText.startsWith("</") -> TagPartType.CLOSING
                    rawText.endsWith("/>") -> TagPartType.SELF_CLOSING
                    rawText.startsWith("<") -> TagPartType.OPENING
                    else -> throw TemplaterException.XmlError(
                        "Cannot determine whether a tag part is opening, closing or self-closing: $rawText",
                        position,
                    )
                }
            }
        }
    }

    data class Content(
        override val position: Int,
        val text: String,
        val isInsideTextTag: Boolean,
    ) : XmlRawInputToken(position) {
        companion object {
            fun fromRawText(position: Int, rawText: String, isInsideTextTag: Boolean): Content {
                return Content(position, DocUtils.unescapeXmlString(rawText), isInsideTextTag)
            }
        }
    }

    data class Verbatim(
        override val position: Int, val rawText: String,
    ) : XmlRawInputToken(position) {
        companion object {
            fun fromRawText(position: Int, rawText: String): Verbatim {
                return Verbatim(position, rawText)
            }
        }
    }
}
