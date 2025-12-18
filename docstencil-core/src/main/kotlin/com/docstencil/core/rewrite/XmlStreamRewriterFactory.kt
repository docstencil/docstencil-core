package com.docstencil.core.rewrite

import com.docstencil.core.scanner.model.XmlInputToken

interface XmlStreamRewriterFactory {
    fun defaultTarget(): XmlInputToken.ExpansionTarget

    /**
     * This is the function the user calls in the template: `{insert rewriterFactoryFn(...)}
     * The return type of this function must be `XmlStreamRewriter`.
     */
    fun getRewriterFactoryFn(): Any
}
