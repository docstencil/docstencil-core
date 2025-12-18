package com.docstencil.core.modules.hyperlink

import com.docstencil.core.error.TemplaterException
import com.docstencil.core.render.model.XmlOutputToken
import com.docstencil.core.rewrite.XmlStreamRewriter
import com.docstencil.core.rewrite.XmlStreamRewriterFactory
import com.docstencil.core.scanner.model.XmlInputToken

/**
 * Rewriter for wrapping content in Word hyperlinks.
 *
 * Wraps the body content in `<w:hyperlink>` elements that reference
 * external URLs via the relationships file.
 */
class HyperlinkRewriter(
    private val rId: String,
) : XmlStreamRewriter {
    class Factory(
        private val hyperlinkModule: HyperlinkModule,
    ) : XmlStreamRewriterFactory {
        override fun defaultTarget(): XmlInputToken.ExpansionTarget {
            return XmlInputToken.ExpansionTarget.None()
        }

        override fun getRewriterFactoryFn(): Any {
            return { url: Any? ->
                val urlString = url?.toString()
                    ?: throw TemplaterException.DeepRuntimeError("Hyperlink URL cannot be null.")
                val rId = hyperlinkModule.createHyperlinkRelationship(urlString)
                HyperlinkRewriter(rId)
            }
        }
    }

    override fun rewrite(wrappedXml: List<XmlOutputToken>): List<XmlOutputToken> {
        val prefix = XmlOutputToken.fromString(
            """</w:t></w:r><w:hyperlink r:id="$rId"><w:r><w:rPr><w:rStyle w:val="Hyperlink"/></w:rPr><w:t>""",
        )
        val suffix = XmlOutputToken.fromString(
            """</w:t></w:r></w:hyperlink><w:r><w:t>""",
        )
        return prefix + wrappedXml + suffix
    }
}
