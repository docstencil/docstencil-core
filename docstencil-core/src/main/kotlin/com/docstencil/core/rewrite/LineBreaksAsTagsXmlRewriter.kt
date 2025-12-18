package com.docstencil.core.rewrite

import com.docstencil.core.config.FileTypeConfig
import com.docstencil.core.render.model.XmlOutputToken
import com.docstencil.core.scanner.model.XmlInputToken

class LineBreaksAsTagsXmlRewriter private constructor(
    private val arg: Any?,
    private val lineBreakRawXml: String,
) : XmlStreamRewriter {
    class Factory(private val config: FileTypeConfig) : XmlStreamRewriterFactory {
        override fun defaultTarget(): XmlInputToken.ExpansionTarget {
            return XmlInputToken.ExpansionTarget.None()
        }

        override fun getRewriterFactoryFn(): Any {
            val tokens = mutableListOf<String>()
            for (tag in config.lineBreaks.tagsToEscape) {
                tokens.add("</$tag>")
            }
            tokens.add("<${config.lineBreaks.tag}/>")
            for (tag in config.lineBreaks.tagsToEscape.reversed()) {
                tokens.add("<$tag>")
            }
            val lineBreakRawXml = tokens.joinToString("")
            return { arg: Any? -> LineBreaksAsTagsXmlRewriter(arg, lineBreakRawXml) }
        }
    }

    override fun rewrite(wrappedXml: List<XmlOutputToken>): List<XmlOutputToken> {
        val s = arg?.toString() ?: ""
        return XmlOutputToken.fromString(
            s.replace("\n", lineBreakRawXml),
        )
    }
}
