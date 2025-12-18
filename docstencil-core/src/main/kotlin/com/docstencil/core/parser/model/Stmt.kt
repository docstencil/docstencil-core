package com.docstencil.core.parser.model

import com.docstencil.core.render.Environment
import com.docstencil.core.scanner.model.TemplateToken
import com.docstencil.core.scanner.model.XmlInputToken

interface StmtVisitor<T> {
    fun visitExpressionStmt(stmt: ExpressionStmt): T
    fun visitBlockStmt(stmt: BlockStmt): T
    fun visitDoStmt(stmt: DoStmt): T
    fun visitIfStmt(stmt: IfStmt): T
    fun visitInsertStmt(stmt: InsertStmt): T
    fun visitRewriteStmt(stmt: RewriteStmt): T
    fun visitForStmt(stmt: ForStmt): T
    fun visitVarStmt(stmt: VarStmt): T
    fun visitVerbatimStmt(stmt: VerbatimStmt): T
}

interface StmtWithEnvVisitor<T> {
    fun visitExpressionStmt(stmt: ExpressionStmt, env: Environment): T
    fun visitBlockStmt(stmt: BlockStmt, env: Environment): T
    fun visitDoStmt(stmt: DoStmt, env: Environment): T
    fun visitIfStmt(stmt: IfStmt, env: Environment): T
    fun visitInsertStmt(stmt: InsertStmt, env: Environment): T
    fun visitRewriteStmt(stmt: RewriteStmt, env: Environment): T
    fun visitForStmt(stmt: ForStmt, env: Environment): T
    fun visitVarStmt(stmt: VarStmt, env: Environment): T
    fun visitVerbatimStmt(stmt: VerbatimStmt, env: Environment): T
}

abstract class Stmt {
    abstract fun <T> accept(visitor: StmtVisitor<T>): T
    abstract fun <T> accept(visitor: StmtWithEnvVisitor<T>, env: Environment): T
}

data class ExpressionStmt(val expr: Expr) : Stmt() {
    override fun <T> accept(visitor: StmtVisitor<T>): T {
        return visitor.visitExpressionStmt(this)
    }

    override fun <T> accept(visitor: StmtWithEnvVisitor<T>, env: Environment): T {
        return visitor.visitExpressionStmt(this, env)
    }
}

data class BlockStmt(val stmts: List<Stmt>) : Stmt() {
    override fun <T> accept(visitor: StmtVisitor<T>): T {
        return visitor.visitBlockStmt(this)
    }

    override fun <T> accept(visitor: StmtWithEnvVisitor<T>, env: Environment): T {
        return visitor.visitBlockStmt(this, env)
    }
}

data class DoStmt(val expr: Expr) : Stmt() {
    override fun <T> accept(visitor: StmtVisitor<T>): T {
        return visitor.visitDoStmt(this)
    }

    override fun <T> accept(visitor: StmtWithEnvVisitor<T>, env: Environment): T {
        return visitor.visitDoStmt(this, env)
    }
}

data class ForStmt(val loopVarName: TemplateToken, val iterable: Expr, val body: BlockStmt) :
    Stmt() {
    override fun <T> accept(visitor: StmtVisitor<T>): T {
        return visitor.visitForStmt(this)
    }

    override fun <T> accept(visitor: StmtWithEnvVisitor<T>, env: Environment): T {
        return visitor.visitForStmt(this, env)
    }
}

data class IfStmt(val condition: Expr, val thenBranch: BlockStmt) : Stmt() {
    override fun <T> accept(visitor: StmtVisitor<T>): T {
        return visitor.visitIfStmt(this)
    }

    override fun <T> accept(visitor: StmtWithEnvVisitor<T>, env: Environment): T {
        return visitor.visitIfStmt(this, env)
    }
}

data class InsertStmt(val insertKeyword: TemplateToken, val expression: Expr, val body: BlockStmt) :
    Stmt() {
    override fun <T> accept(visitor: StmtVisitor<T>): T {
        return visitor.visitInsertStmt(this)
    }

    override fun <T> accept(visitor: StmtWithEnvVisitor<T>, env: Environment): T {
        return visitor.visitInsertStmt(this, env)
    }
}

data class RewriteStmt(
    val rewriteKeyword: TemplateToken,
    val expression: Expr,
    val body: BlockStmt,
) : Stmt() {
    override fun <T> accept(visitor: StmtVisitor<T>): T {
        return visitor.visitRewriteStmt(this)
    }

    override fun <T> accept(visitor: StmtWithEnvVisitor<T>, env: Environment): T {
        return visitor.visitRewriteStmt(this, env)
    }
}

data class VarStmt(val name: TemplateToken, val initializer: Expr) : Stmt() {
    override fun <T> accept(visitor: StmtVisitor<T>): T {
        return visitor.visitVarStmt(this)
    }

    override fun <T> accept(visitor: StmtWithEnvVisitor<T>, env: Environment): T {
        return visitor.visitVarStmt(this, env)
    }
}

data class VerbatimStmt(val inputToken: XmlInputToken) : Stmt() {
    override fun <T> accept(visitor: StmtVisitor<T>): T {
        return visitor.visitVerbatimStmt(this)
    }

    override fun <T> accept(visitor: StmtWithEnvVisitor<T>, env: Environment): T {
        return visitor.visitVerbatimStmt(this, env)
    }
}
