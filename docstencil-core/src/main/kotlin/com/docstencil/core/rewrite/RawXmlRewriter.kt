package com.docstencil.core.rewrite

import com.docstencil.core.config.FileTypeConfig
import com.docstencil.core.render.model.XmlOutputToken
import com.docstencil.core.scanner.model.XmlInputToken

class RawXmlRewriter(private val rawXml: String) : XmlStreamRewriter {
    class Factory(private val config: FileTypeConfig) : XmlStreamRewriterFactory {
        override fun defaultTarget(): XmlInputToken.ExpansionTarget {
            return XmlInputToken.ExpansionTarget.Tag(config.defaultTagRawXmlExpansion)
        }

        override fun getRewriterFactoryFn(): Any {
            return { rawXml: String -> RawXmlRewriter(rawXml) }
        }
    }

    override fun rewrite(wrappedXml: List<XmlOutputToken>): List<XmlOutputToken> {
        return XmlOutputToken.fromString(
            rawXml,

            )
    }
}
