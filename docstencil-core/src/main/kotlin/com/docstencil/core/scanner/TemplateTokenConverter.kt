package com.docstencil.core.scanner

import com.docstencil.core.scanner.model.ContentIdx
import com.docstencil.core.scanner.model.TemplateToken
import com.docstencil.core.scanner.model.TemplateTokenType
import com.docstencil.core.scanner.model.XmlInputToken

class TemplateTokenConverter {
    fun convert(rawTokens: List<XmlInputToken>): List<TemplateToken> {
        return rawTokens
            .flatMap {
                when (it) {
                    is XmlInputToken.Sentinel -> listOf(
                        TemplateToken(
                            TemplateTokenType.SENTINEL,
                            "",
                            it,
                            null,
                            ContentIdx(0),
                        ),
                    )

                    is XmlInputToken.Raw -> listOf(
                        TemplateToken(
                            TemplateTokenType.VERBATIM,
                            it.xml.xmlString,
                            it,
                            null,
                            ContentIdx(0),
                        ),
                    )

                    is XmlInputToken.TemplateGroup -> it.tokens.tokens
                }
            }
            .toList()
    }
}
