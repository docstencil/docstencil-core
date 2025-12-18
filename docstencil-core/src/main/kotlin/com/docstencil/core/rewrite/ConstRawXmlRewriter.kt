package com.docstencil.core.rewrite

import com.docstencil.core.render.model.XmlOutputToken
import com.docstencil.core.scanner.model.XmlInputToken.ExpansionTarget

private const val TOC_RAW_XML = """<w:p w:rsidR="00095C65" w:rsidRDefault="00095C65">
   <w:r>
      <w:rPr>
         <w:b />
         <w:bCs />
         <w:noProof />
      </w:rPr>
      <w:fldChar w:fldCharType="begin" />
   </w:r>
   <w:r>
      <w:rPr>
         <w:b />
         <w:bCs />
         <w:noProof />
      </w:rPr>
      <w:instrText xml:space="preserve"> TOC \o "1-3" \h \z \u </w:instrText>
   </w:r>
   <w:r>
      <w:rPr>
         <w:b />
         <w:bCs />
         <w:noProof />
      </w:rPr>
      <w:fldChar w:fldCharType="separate" />
   </w:r>
   <w:r>
      <w:rPr>
         <w:noProof />
      </w:rPr>
      <w:t>No table of contents entries found.</w:t>
   </w:r>
   <w:r>
      <w:rPr>
         <w:b />
         <w:bCs />
         <w:noProof />
      </w:rPr>
      <w:fldChar w:fldCharType="end" />
   </w:r>
</w:p>"""

class ConstRawXmlRewriter(private val rawXml: String) : XmlStreamRewriter {
    class Factory(
        private val rawXml: String,
        private val defaultTarget: ExpansionTarget,
    ) : XmlStreamRewriterFactory {
        override fun defaultTarget(): ExpansionTarget {
            return defaultTarget
        }

        override fun getRewriterFactoryFn(): Any {
            return { ConstRawXmlRewriter(rawXml) }
        }
    }

    companion object {
        fun createDefaultTocFactory() = Factory(TOC_RAW_XML, ExpansionTarget.Tag("w:p"))
    }

    override fun rewrite(wrappedXml: List<XmlOutputToken>): List<XmlOutputToken> {
        return XmlOutputToken.fromString(
            rawXml,
        )
    }
}
