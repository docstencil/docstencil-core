package com.docstencil.core.parser.model

import com.docstencil.core.render.Environment
import com.docstencil.core.scanner.model.TemplateToken


interface ExprVisitor<R> {
    fun visitAssignExpr(expr: AssignExpr): R
    fun visitBinaryExpr(expr: BinaryExpr): R
    fun visitCallExpr(expr: CallExpr): R
    fun visitCaseExpr(expr: CaseExpr): R
    fun visitGetExpr(expr: GetExpr): R
    fun visitGroupingExpr(expr: GroupingExpr): R
    fun visitLambdaExpr(expr: LambdaExpr): R
    fun visitLiteralExpr(expr: LiteralExpr): R
    fun visitLogicalExpr(expr: LogicalExpr): R
    fun visitOptionalGetExpr(expr: OptionalGetExpr): R
    fun visitPipeExpr(expr: PipeExpr): R
    fun visitSetExpr(expr: SetExpr): R
    fun visitUnaryExpr(expr: UnaryExpr): R
    fun visitVariableExpr(expr: VariableExpr): R
}

interface ExprWithEnvVisitor<R> {
    fun visitAssignExpr(expr: AssignExpr, env: Environment): R
    fun visitBinaryExpr(expr: BinaryExpr, env: Environment): R
    fun visitCallExpr(expr: CallExpr, env: Environment): R
    fun visitCaseExpr(expr: CaseExpr, env: Environment): R
    fun visitGetExpr(expr: GetExpr, env: Environment): R
    fun visitGroupingExpr(expr: GroupingExpr, env: Environment): R
    fun visitLambdaExpr(expr: LambdaExpr, env: Environment): R
    fun visitLiteralExpr(expr: LiteralExpr, env: Environment): R
    fun visitLogicalExpr(expr: LogicalExpr, env: Environment): R
    fun visitOptionalGetExpr(expr: OptionalGetExpr, env: Environment): R
    fun visitPipeExpr(expr: PipeExpr, env: Environment): R
    fun visitSetExpr(expr: SetExpr, env: Environment): R
    fun visitUnaryExpr(expr: UnaryExpr, env: Environment): R
    fun visitVariableExpr(expr: VariableExpr, env: Environment): R
}


abstract class Expr {
    abstract fun <R> accept(visitor: ExprVisitor<R>): R
    abstract fun <R> accept(visitor: ExprWithEnvVisitor<R>, env: Environment): R
}

data class AssignExpr(val name: TemplateToken, val value: Expr) : Expr() {
    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitAssignExpr(this)
    }

    override fun <R> accept(visitor: ExprWithEnvVisitor<R>, env: Environment): R {
        return visitor.visitAssignExpr(this, env)
    }
}

data class BinaryExpr(val left: Expr, val operator: TemplateToken, val right: Expr) : Expr() {
    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitBinaryExpr(this)
    }

    override fun <R> accept(visitor: ExprWithEnvVisitor<R>, env: Environment): R {
        return visitor.visitBinaryExpr(this, env)
    }
}

data class CallExpr(val callee: Expr, val paren: TemplateToken, val args: List<Expr>) : Expr() {
    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitCallExpr(this)
    }

    override fun <R> accept(visitor: ExprWithEnvVisitor<R>, env: Environment): R {
        return visitor.visitCallExpr(this, env)
    }
}

data class CaseExpr(
    val branches: List<WhenBranch>,
    val elseBranch: Expr?,
) : Expr() {
    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitCaseExpr(this)
    }

    override fun <R> accept(visitor: ExprWithEnvVisitor<R>, env: Environment): R {
        return visitor.visitCaseExpr(this, env)
    }
}

data class GetExpr(val obj: Expr, val name: TemplateToken) : Expr() {
    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitGetExpr(this)
    }

    override fun <R> accept(visitor: ExprWithEnvVisitor<R>, env: Environment): R {
        return visitor.visitGetExpr(this, env)
    }
}

data class GroupingExpr(val expr: Expr) : Expr() {
    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitGroupingExpr(this)
    }

    override fun <R> accept(visitor: ExprWithEnvVisitor<R>, env: Environment): R {
        return visitor.visitGroupingExpr(this, env)
    }
}

data class LambdaExpr(val params: List<TemplateToken>, val body: Expr) : Expr() {
    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitLambdaExpr(this)
    }

    override fun <R> accept(visitor: ExprWithEnvVisitor<R>, env: Environment): R {
        return visitor.visitLambdaExpr(this, env)
    }
}

data class LiteralExpr(val value: Any?) : Expr() {
    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitLiteralExpr(this)
    }

    override fun <R> accept(visitor: ExprWithEnvVisitor<R>, env: Environment): R {
        return visitor.visitLiteralExpr(this, env)
    }
}

data class LogicalExpr(val left: Expr, val operator: TemplateToken, val right: Expr) :
    Expr() {
    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitLogicalExpr(this)
    }

    override fun <R> accept(visitor: ExprWithEnvVisitor<R>, env: Environment): R {
        return visitor.visitLogicalExpr(this, env)
    }
}

data class OptionalGetExpr(val obj: Expr, val name: TemplateToken) : Expr() {
    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitOptionalGetExpr(this)
    }

    override fun <R> accept(visitor: ExprWithEnvVisitor<R>, env: Environment): R {
        return visitor.visitOptionalGetExpr(this, env)
    }
}

data class PipeExpr(val left: Expr, val operator: TemplateToken, val right: Expr) : Expr() {
    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitPipeExpr(this)
    }

    override fun <R> accept(visitor: ExprWithEnvVisitor<R>, env: Environment): R {
        return visitor.visitPipeExpr(this, env)
    }
}

data class SetExpr(val obj: Expr, val name: TemplateToken, val value: Expr) : Expr() {
    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitSetExpr(this)
    }

    override fun <R> accept(visitor: ExprWithEnvVisitor<R>, env: Environment): R {
        return visitor.visitSetExpr(this, env)
    }
}

data class UnaryExpr(val operator: TemplateToken, val right: Expr) : Expr() {
    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitUnaryExpr(this)
    }

    override fun <R> accept(visitor: ExprWithEnvVisitor<R>, env: Environment): R {
        return visitor.visitUnaryExpr(this, env)
    }
}

data class VariableExpr(val name: TemplateToken) : Expr() {
    override fun <R> accept(visitor: ExprVisitor<R>): R {
        return visitor.visitVariableExpr(this)
    }

    override fun <R> accept(visitor: ExprWithEnvVisitor<R>, env: Environment): R {
        return visitor.visitVariableExpr(this, env)
    }
}

data class WhenBranch(
    val whenKeyword: TemplateToken,
    val condition: Expr,
    val thenKeyword: TemplateToken,
    val branch: Expr,
)

