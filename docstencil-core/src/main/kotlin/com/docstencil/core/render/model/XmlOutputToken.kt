package com.docstencil.core.render.model

import com.docstencil.core.scanner.RawXmlScanner
import com.docstencil.core.scanner.model.TagPartType
import com.docstencil.core.scanner.model.XmlRawInputToken
import com.docstencil.core.utils.DocUtils

sealed class XmlOutputToken(open val inputToken: XmlRawInputToken?) {
    companion object {
        // There are no more templates in the output.
        private val rawXmlScanner = RawXmlScanner(null, emptySet())

        fun fromString(rawXmlString: String): List<XmlOutputToken> {
            return rawXmlScanner.scan(rawXmlString).map {
                from(it)
            }
        }

        fun from(rawToken: XmlRawInputToken): XmlOutputToken {
            return when (rawToken) {
                is XmlRawInputToken.Verbatim -> Verbatim(rawToken)
                is XmlRawInputToken.TagPart -> TagPart(
                    rawToken.tagName,
                    rawToken.tagPartType,
                    rawToken.rawText,
                    rawToken,
                )

                is XmlRawInputToken.Content -> Content(
                    rawToken.text,
                    false,
                    rawToken,
                )
            }
        }
    }

    val xmlString: String
        get() {
            return when (this) {
                is Content -> DocUtils.escapeXmlString(this.text ?: "")
                is Verbatim -> this.inputToken.xmlString
                is TagPart -> this.rawText
                is Sentinel -> ""
            }
        }

    /**
     * For passing XML straight through without any changes.
     * Never use this for generated XML. Always use the Factory to convert strings into instances of
     * `TagPart` and `Content`.
     */
    data class Verbatim(
        override val inputToken: XmlRawInputToken,
    ) : XmlOutputToken(inputToken)

    /**
     * XML tag parts like `<foo>`, `</foo>` or `<foo/>`.
     */
    data class TagPart(
        val tagName: String,
        val tagPartType: TagPartType,
        val rawText: String,
        override val inputToken: XmlRawInputToken? = null,
    ) : XmlOutputToken(inputToken)

    /**
     * Content subject to escaping.
     */
    data class Content(
        val text: String?,
        val isGeneratedByTemplate: Boolean,
        override val inputToken: XmlRawInputToken? = null,
    ) : XmlOutputToken(inputToken)

    /**
     * Sentinel markers left behind by template expressions during merging.
     * Used to identify empty tags for post-rendering cleanup.
     */
    data class Sentinel(
        override val inputToken: XmlRawInputToken? = null,
    ) : XmlOutputToken(inputToken)
}
