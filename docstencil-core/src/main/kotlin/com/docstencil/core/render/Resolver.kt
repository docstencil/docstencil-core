package com.docstencil.core.render

import com.docstencil.core.error.TemplaterException
import com.docstencil.core.parser.model.*
import com.docstencil.core.scanner.model.TemplateToken


class Resolver {
    private class Runner : StmtVisitor<Unit>, ExprVisitor<Unit> {
        private val scopes = ArrayDeque<MutableMap<String, Boolean>>()
        private val locals = mutableMapOf<Expr, Int>()

        fun run(stmts: List<Stmt>, globalSymbols: Collection<String>): Map<Expr, Int> {
            // Resolve globals.
            val globalScope = mutableMapOf<String, Boolean>()
            globalSymbols.forEach { key ->
                globalScope[key] = true
            }
            scopes.add(globalScope)

            resolve(stmts)

            return locals
        }

        fun resolve(stmts: List<Stmt>) {
            for (stmt in stmts) {
                resolve(stmt)
            }
        }

        fun resolve(stmt: Stmt) {
            stmt.accept(this)
        }

        fun resolve(expr: Expr) {
            expr.accept(this)
        }

        private fun resolveLocal(expr: Expr, name: TemplateToken) {
            if (scopes.isEmpty()) {
                return
            }

            for (scopeIdx in (scopes.size - 1) downTo 0) {
                val scope = scopes[scopeIdx]
                if (scope.contains(name.lexeme)) {
                    val depth = scopes.size - 1 - scopeIdx
                    locals[expr] = depth
                    return
                }
            }

            throw TemplaterException.RuntimeError("Undefined variable '${name.lexeme}'.", name)
        }

        private fun beginScope() {
            scopes.addLast(mutableMapOf())
        }

        private fun endScope() {
            scopes.removeLast()
        }

        private fun declare(name: TemplateToken) {
            if (scopes.isEmpty()) return

            val scope = scopes.last()
            if (scope.contains(name.lexeme)) {
                throw TemplaterException.RuntimeError(
                    "A variable with that name already exists in this scope.",
                    name,
                )
            }

            scope[name.lexeme] = false
        }

        private fun define(name: TemplateToken) {
            if (scopes.isEmpty()) return

            scopes.last()[name.lexeme] = true
        }

        override fun visitExpressionStmt(stmt: ExpressionStmt) {
            resolve(stmt.expr)
        }

        override fun visitVarStmt(stmt: VarStmt) {
            declare(stmt.name)
            resolve(stmt.initializer)
            define(stmt.name)
        }

        override fun visitVerbatimStmt(stmt: VerbatimStmt) {
            // Do nothing.
        }

        override fun visitDoStmt(stmt: DoStmt) {
            resolve(stmt.expr)
        }

        override fun visitBlockStmt(stmt: BlockStmt) {
            beginScope()
            resolve(stmt.stmts)
            endScope()
        }

        override fun visitIfStmt(stmt: IfStmt) {
            resolve(stmt.condition)
            resolve(stmt.thenBranch)
        }

        override fun visitInsertStmt(stmt: InsertStmt) {
            resolve(stmt.expression)
            resolve(stmt.body)
        }

        override fun visitRewriteStmt(stmt: RewriteStmt) {
            resolve(stmt.expression)
            resolve(stmt.body)
        }

        override fun visitForStmt(stmt: ForStmt) {
            resolve(stmt.iterable)
            beginScope()
            declare(stmt.loopVarName)
            define(stmt.loopVarName)
            resolve(stmt.body)
            endScope()
        }

        override fun visitAssignExpr(expr: AssignExpr) {
            resolve(expr.value)
            resolveLocal(expr, expr.name)
        }

        override fun visitBinaryExpr(expr: BinaryExpr) {
            resolve(expr.left)
            resolve(expr.right)
        }

        override fun visitCallExpr(expr: CallExpr) {
            resolve(expr.callee)
            for (arg in expr.args) {
                resolve(arg)
            }
        }

        override fun visitCaseExpr(expr: CaseExpr) {
            for (branch in expr.branches) {
                resolve(branch.condition)
                resolve(branch.branch)
            }

            if (expr.elseBranch != null) {
                resolve(expr.elseBranch)
            }
        }

        override fun visitGetExpr(expr: GetExpr) {
            resolve(expr.obj)
        }

        override fun visitOptionalGetExpr(expr: OptionalGetExpr) {
            resolve(expr.obj)
        }

        override fun visitGroupingExpr(expr: GroupingExpr) {
            resolve(expr.expr)
        }

        override fun visitLambdaExpr(expr: LambdaExpr) {
            beginScope()

            for (param in expr.params) {
                declare(param)
                define(param)
            }

            resolve(expr.body)

            endScope()
        }

        override fun visitLiteralExpr(expr: LiteralExpr) {
            // Do nothing.
        }

        override fun visitUnaryExpr(expr: UnaryExpr) {
            resolve(expr.right)
        }

        override fun visitVariableExpr(expr: VariableExpr) {
            if (!scopes.isEmpty() && scopes.last()[expr.name.lexeme] == false) {
                throw TemplaterException.RuntimeError(
                    "Can't read local variable in its initializer.",
                    expr.name,
                )
            }

            resolveLocal(expr, expr.name)
        }

        override fun visitLogicalExpr(expr: LogicalExpr) {
            resolve(expr.left)
            resolve(expr.right)
        }

        override fun visitPipeExpr(expr: PipeExpr) {
            resolve(expr.left)
            resolve(expr.right)
        }

        override fun visitSetExpr(expr: SetExpr) {
            resolve(expr.value)
            resolve(expr.obj)
        }
    }

    fun resolve(stmts: List<Stmt>, globalSymbols: Collection<String>): Map<Expr, Int> {
        return Runner().run(stmts, globalSymbols)
    }
}
