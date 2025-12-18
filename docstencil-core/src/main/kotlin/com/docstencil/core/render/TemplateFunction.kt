package com.docstencil.core.render

import com.docstencil.core.parser.model.Expr
import com.docstencil.core.scanner.model.TemplateToken

/**
 * Runtime representation of a lambda function with closure capture.
 *
 * This class represents user-defined lambda functions in templates.
 * Lambdas are defined with syntax: (x, y) => expression
 */
class TemplateFunction(
    val params: List<TemplateToken>,
    val body: Expr,
    val closure: Environment,
) {
    val arity: Int = params.size

    override fun toString(): String =
        "<lambda(${params.joinToString(", ") { it.lexeme }})>"
}
