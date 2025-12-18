package com.docstencil.core.rewrite

import com.docstencil.core.render.model.XmlOutputToken

interface XmlStreamRewriter {
    fun rewrite(wrappedXml: List<XmlOutputToken>): List<XmlOutputToken>
}
