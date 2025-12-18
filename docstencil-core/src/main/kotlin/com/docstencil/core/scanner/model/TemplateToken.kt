package com.docstencil.core.scanner.model

import com.docstencil.core.error.TemplaterException


/**
 * This represents the index of a character in the raw string that is the concatenation of all
 * content parts where template expressions _could_ occur.
 */
@JvmInline
value class ContentIdx(val value: Int) {
    fun increment(): ContentIdx {
        return ContentIdx(value + 1)
    }

    fun decrement(): ContentIdx {
        return ContentIdx(value - 1)
    }

    fun add(other: Int): ContentIdx {
        return ContentIdx(value + other)
    }
}

data class TemplateToken(
    val type: TemplateTokenType,
    val lexeme: String,
    val xml: XmlInputToken?,
    val literal: Any?,
    val startIdx: ContentIdx,
) {
    init {
        if (type == TemplateTokenType.VERBATIM && xml == null) {
            throw TemplaterException.FatalError("VERBATIM needs to have input token set.")
        }
        if (type == TemplateTokenType.VERBATIM && xml !is XmlInputToken.Raw) {
            throw TemplaterException.FatalError("VERBATIM requires XmlInputToken.Raw.")
        }
        if (type == TemplateTokenType.SENTINEL && xml == null) {
            throw TemplaterException.FatalError("SENTINEL needs to have input token set.")
        }
        if (type == TemplateTokenType.SENTINEL && xml !is XmlInputToken.Sentinel) {
            throw TemplaterException.FatalError("SENTINEL requires XmlInputToken.Sentinel.")
        }
    }

    override fun toString(): String {
        if (xml != null) {
            return "$type $xml"
        }
        return "$type $lexeme $literal"
    }
}
