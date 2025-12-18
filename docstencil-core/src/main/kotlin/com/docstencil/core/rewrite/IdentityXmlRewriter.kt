package com.docstencil.core.rewrite

import com.docstencil.core.render.model.XmlOutputToken
import com.docstencil.core.scanner.model.XmlInputToken

class IdentityXmlRewriter : XmlStreamRewriter {
    class Factory : XmlStreamRewriterFactory {
        override fun defaultTarget(): XmlInputToken.ExpansionTarget {
            return XmlInputToken.ExpansionTarget.None()
        }

        override fun getRewriterFactoryFn(): Any {
            return { IdentityXmlRewriter() }
        }
    }

    override fun rewrite(wrappedXml: List<XmlOutputToken>): List<XmlOutputToken> {
        return wrappedXml
    }
}
