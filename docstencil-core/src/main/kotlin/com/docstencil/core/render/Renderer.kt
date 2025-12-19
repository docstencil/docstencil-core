package com.docstencil.core.render

import com.docstencil.core.error.TemplaterException
import com.docstencil.core.parser.model.*
import com.docstencil.core.render.model.XmlOutputToken
import com.docstencil.core.rewrite.XmlStreamRewriter
import com.docstencil.core.scanner.model.TemplateToken
import com.docstencil.core.scanner.model.TemplateTokenType
import com.docstencil.core.scanner.model.XmlInputToken
import java.util.*
import java.util.stream.Collectors


class Renderer(
    private val globals: Environment,
    private val parallelRendering: Boolean = false,
) {
    class Interpreter private constructor(
        private val program: List<Stmt>,
        private val data: Map<String, Any>,
        private val globalEnv: Environment,
        private val locals: Map<Expr, Int>,
        private val parallelRendering: Boolean,
    ) : ExprWithEnvVisitor<Any?>, StmtWithEnvVisitor<List<XmlOutputToken>> {
        companion object {
            val resolver = Resolver()

            fun create(
                program: List<Stmt>,
                globals: Environment,
                data: Map<String, Any>,
                parallelRendering: Boolean = false,
            ): Interpreter {
                val globalSymbols = globals.definedVars() + data.keys
                val locals = resolver.resolve(program, globalSymbols)
                return Interpreter(
                    program,
                    data,
                    globals,
                    locals,
                    parallelRendering,
                )
            }
        }

        private val binaryOpHelper = BinaryOpInterpreterHelper()
        private val nativeObjectHelper = NativeObjectHelper()
        private val propertyAccessHelper = PropertyAccessHelper()

        val env: Environment get() = globalEnv

        fun run(): List<XmlOutputToken> {
            data.forEach { (key, value) -> globalEnv.define(key, value) }
            return program.flatMap { execute(it, globalEnv) }
        }

        private fun execute(stmt: Stmt, env: Environment): List<XmlOutputToken> {
            return stmt.accept(this, env)
        }

        private fun evaluate(expr: Expr, env: Environment): Any? {
            return expr.accept(this, env)
        }

        override fun visitBinaryExpr(expr: BinaryExpr, env: Environment): Any {
            val leftVal = evaluate(expr.left, env)
            val rightVal = evaluate(expr.right, env)

            return when (expr.operator.type) {
                TemplateTokenType.PLUS -> binaryOpHelper.performArithmetic(
                    leftVal,
                    rightVal,
                    expr.operator,
                    BinaryOpInterpreterHelper.ArithmeticOp.ADD,
                )

                TemplateTokenType.MINUS -> binaryOpHelper.performArithmetic(
                    leftVal,
                    rightVal,
                    expr.operator,
                    BinaryOpInterpreterHelper.ArithmeticOp.SUBTRACT,
                )

                TemplateTokenType.STAR -> binaryOpHelper.performArithmetic(
                    leftVal,
                    rightVal,
                    expr.operator,
                    BinaryOpInterpreterHelper.ArithmeticOp.MULTIPLY,
                )

                TemplateTokenType.SLASH -> binaryOpHelper.performArithmetic(
                    leftVal,
                    rightVal,
                    expr.operator,
                    BinaryOpInterpreterHelper.ArithmeticOp.DIVIDE,
                )

                TemplateTokenType.PERCENT -> binaryOpHelper.performArithmetic(
                    leftVal,
                    rightVal,
                    expr.operator,
                    BinaryOpInterpreterHelper.ArithmeticOp.MODULO,
                )

                TemplateTokenType.GREATER -> binaryOpHelper.compareValues(
                    leftVal,
                    rightVal,
                    expr.operator,
                    BinaryOpInterpreterHelper.ComparisonOp.GREATER,
                )

                TemplateTokenType.GREATER_EQUAL -> binaryOpHelper.compareValues(
                    leftVal,
                    rightVal,
                    expr.operator,
                    BinaryOpInterpreterHelper.ComparisonOp.GREATER_EQUAL,
                )

                TemplateTokenType.LESS -> binaryOpHelper.compareValues(
                    leftVal,
                    rightVal,
                    expr.operator,
                    BinaryOpInterpreterHelper.ComparisonOp.LESS,
                )

                TemplateTokenType.LESS_EQUAL -> binaryOpHelper.compareValues(
                    leftVal,
                    rightVal,
                    expr.operator,
                    BinaryOpInterpreterHelper.ComparisonOp.LESS_EQUAL,
                )

                TemplateTokenType.BANG_EQUAL -> !isEqual(leftVal, rightVal)

                TemplateTokenType.EQUAL_EQUAL -> isEqual(leftVal, rightVal)

                else -> throw TemplaterException.RuntimeError(
                    "Unknown binary operator: ${expr.operator.type}",
                    expr.operator,
                )
            }
        }

        override fun visitCallExpr(expr: CallExpr, env: Environment): Any? {
            val callee = evaluate(expr.callee, env)

            val args = mutableListOf<Any?>()
            for (arg in expr.args) {
                args.add(evaluate(arg, env))
            }

            // Handle TemplateFunction
            if (callee is TemplateFunction) {
                if (args.size != callee.arity) {
                    throw TemplaterException.RuntimeError(
                        "Expected ${callee.arity} arguments but got ${args.size}.",
                        expr.paren,
                    )
                }
                return callTemplateFunction(callee, args, env)
            }

            if (!nativeObjectHelper.isCallable(callee)) {
                throw TemplaterException.RuntimeError(
                    "Can only call functions and classes.",
                    expr.paren,
                )
            }

            if (!nativeObjectHelper.isValidArity(callee!!, args.size)) {
                val arity = nativeObjectHelper.getArity(callee)
                throw TemplaterException.RuntimeError(
                    "Expected $arity arguments but got ${args.size}.",
                    expr.paren,
                )
            }

            try {
                return nativeObjectHelper.call(callee, wrapTemplateFunctionsInArgs(args, env))
            } catch (e: TemplaterException.DeepRuntimeError) {
                throw TemplaterException.RuntimeError(
                    e.message ?: "Error calling function",
                    expr.paren,
                    e.cause,
                )
            }
        }

        /**
         * Passing template functions to native functions would not work because they cannot
         * evaluate them. Therefore, we wrap template functions to make them look like native
         * functions to other native functions.
         */
        private fun wrapTemplateFunctionsInArgs(args: List<Any?>, env: Environment): List<Any?> {
            return args.map { arg ->
                if (arg != null && arg is TemplateFunction) {
                    if (arg.arity == 0) {
                        {
                            callTemplateFunction(
                                arg,
                                wrapTemplateFunctionsInArgs(listOf(), env),
                                env,
                            )
                        }
                    } else if (arg.arity == 1) {
                        { a1: Any? ->
                            callTemplateFunction(
                                arg,
                                wrapTemplateFunctionsInArgs(listOf(a1), env),
                                env,
                            )
                        }
                    } else if (arg.arity == 2) {
                        val f = { a1: Any?, a2: Any? ->
                            callTemplateFunction(
                                arg,
                                wrapTemplateFunctionsInArgs(listOf(a1, a2), env),
                                env,
                            )
                        }
                        f
                    } else if (arg.arity == 3) {
                        val f = { a1: Any?, a2: Any?, a3: Any? ->
                            callTemplateFunction(
                                arg,
                                wrapTemplateFunctionsInArgs(listOf(a1, a2, a3), env),
                                env,
                            )
                        }
                        f
                    } else {
                        null
                    }
                } else {
                    arg
                }
            }
        }

        private fun callTemplateFunction(
            fn: TemplateFunction,
            args: List<Any?>,
            env: Environment,
        ): Any? {
            val callEnv = Environment(fn.closure)
            for (i in fn.params.indices) {
                callEnv.define(fn.params[i].lexeme, args[i])
            }
            return evaluate(fn.body, callEnv)
        }

        override fun visitCaseExpr(expr: CaseExpr, env: Environment): Any? {
            for (branch in expr.branches) {
                val condition = evaluate(branch.condition, env)
                if (isTruthy(condition)) {
                    return evaluate(branch.branch, env)
                }
            }

            return if (expr.elseBranch != null) {
                evaluate(expr.elseBranch, env)
            } else {
                null
            }
        }

        override fun visitGetExpr(expr: GetExpr, env: Environment): Any? {
            val v = evaluate(expr.obj, env)
            val obj = v ?: throw TemplaterException.RuntimeError(
                "Cannot get property '${expr.name.lexeme}' of null.",
                expr.name,
            )

            return propertyAccessHelper.getProperty(obj, expr.name)
        }

        override fun visitOptionalGetExpr(expr: OptionalGetExpr, env: Environment): Any? {
            val obj = evaluate(expr.obj, env) ?: return null

            return propertyAccessHelper.getProperty(obj, expr.name)
        }

        override fun visitGroupingExpr(expr: GroupingExpr, env: Environment): Any? {
            return evaluate(expr.expr, env)
        }

        override fun visitLambdaExpr(expr: LambdaExpr, env: Environment): Any? {
            // Capture current environment as closure
            return TemplateFunction(expr.params, expr.body, env)
        }

        override fun visitLiteralExpr(expr: LiteralExpr, env: Environment): Any? {
            return expr.value
        }

        override fun visitUnaryExpr(expr: UnaryExpr, env: Environment): Any {
            val rawVal = expr.right.accept(this, env)

            return when (expr.operator.type) {
                TemplateTokenType.MINUS -> {
                    when (rawVal) {
                        is Byte -> -rawVal
                        is Short -> -rawVal
                        is Int -> -rawVal
                        is Long -> -rawVal
                        is Float -> -rawVal
                        is Double -> -rawVal
                        else -> throw TemplaterException.RuntimeError(
                            "Operand must be a number.",
                            expr.operator,
                        )
                    }
                }

                TemplateTokenType.BANG -> !isTruthy(rawVal)

                else -> throw TemplaterException.FatalError(
                    "Unknown unary operator: ${expr.operator.type}",
                    expr.operator,
                )
            }
        }

        override fun visitVariableExpr(expr: VariableExpr, env: Environment): Any? {
            return lookUpVariable(expr.name, expr, env)
        }

        private fun lookUpVariable(name: TemplateToken, expr: Expr, env: Environment): Any? {
            val distance = locals[expr]
            return if (distance != null) {
                env.getAt(distance, name.lexeme)
            } else {
                globalEnv.get(name)
            }
        }

        override fun visitLogicalExpr(expr: LogicalExpr, env: Environment): Any? {
            val left = evaluate(expr.left, env)

            if (expr.operator.type == TemplateTokenType.QUESTION_QUESTION) {
                if (left != null) return left
                return evaluate(expr.right, env)
            } else if (expr.operator.type == TemplateTokenType.OR) {
                if (isTruthy(left)) return left
            } else {
                if (!isTruthy(left)) return left
            }

            return evaluate(expr.right, env)
        }

        override fun visitPipeExpr(expr: PipeExpr, env: Environment): Any? {
            val leftValue = evaluate(expr.left, env)

            // If right side is a CallExpr, prepend leftValue as first argument.
            if (expr.right is CallExpr) {
                val callExpr = expr.right
                val callee = evaluate(callExpr.callee, env)

                // Evaluate args and prepend leftValue.
                val args = mutableListOf(leftValue)
                for (arg in callExpr.args) {
                    args.add(evaluate(arg, env))
                }

                if (callee is TemplateFunction) {
                    if (args.size != callee.arity) {
                        throw TemplaterException.RuntimeError(
                            "Expected ${callee.arity} arguments but got ${args.size}.",
                            expr.operator,
                        )
                    }
                    return callTemplateFunction(callee, args, env)
                }

                if (!nativeObjectHelper.isCallable(callee)) {
                    throw TemplaterException.RuntimeError(
                        "Right side of pipe must be callable.",
                        expr.operator,
                    )
                }

                if (!nativeObjectHelper.isValidArity(callee!!, args.size)) {
                    val arity = nativeObjectHelper.getArity(callee)
                    throw TemplaterException.RuntimeError(
                        "Expected $arity arguments but got ${args.size}.",
                        expr.operator,
                    )
                }

                try {
                    return nativeObjectHelper.call(callee, wrapTemplateFunctionsInArgs(args, env))
                } catch (e: TemplaterException.DeepRuntimeError) {
                    throw TemplaterException.RuntimeError(
                        e.message ?: "Error calling piped function",
                        expr.operator,
                        e.cause,
                    )
                }
            }

            // Otherwise, right side must be a callable value.
            val rightCallable = evaluate(expr.right, env)

            if (rightCallable is TemplateFunction) {
                if (rightCallable.arity < 1) {
                    throw TemplaterException.RuntimeError(
                        "Pipe operator requires function with at least 1 parameter, got ${rightCallable.arity}.",
                        expr.operator,
                    )
                }
                return callTemplateFunction(rightCallable, listOf(leftValue), env)
            }

            if (!nativeObjectHelper.isCallable(rightCallable)) {
                throw TemplaterException.RuntimeError(
                    "Right side of pipe must be callable.",
                    expr.operator,
                )
            }

            val arity = nativeObjectHelper.getArity(rightCallable!!)
            if (arity < 1) {
                throw TemplaterException.RuntimeError(
                    "Pipe operator requires function with at least 1 parameter, got $arity.",
                    expr.operator,
                )
            }

            try {
                return nativeObjectHelper.call(
                    rightCallable,
                    wrapTemplateFunctionsInArgs(listOf(leftValue), env),
                )
            } catch (e: TemplaterException.DeepRuntimeError) {
                throw TemplaterException.RuntimeError(
                    e.message ?: "Error calling piped function",
                    expr.operator,
                    e.cause,
                )
            }
        }

        override fun visitSetExpr(expr: SetExpr, env: Environment): Any? {
            val obj = evaluate(expr.obj, env) ?: throw TemplaterException.RuntimeError(
                "Cannot set property '${expr.name.lexeme}' on null.",
                expr.name,
            )

            val value = evaluate(expr.value, env)
            propertyAccessHelper.setProperty(obj, expr.name, value)

            return value
        }

        override fun visitAssignExpr(expr: AssignExpr, env: Environment): Any? {
            val value = evaluate(expr.value, env)

            val distance = locals[expr]
            if (distance != null) {
                env.assignAt(distance, expr.name, value)
            } else {
                globalEnv.assign(expr.name, value)
            }

            return value
        }

        private fun isTruthy(bool: Any?): Boolean {
            if (bool == null) {
                return false
            }
            if (bool is Boolean && !bool) {
                return false
            }
            if (bool == "") {
                return false
            }
            if (bool == 0 || bool == 0.0) {
                return false
            }
            if (bool is Collection<*> && bool.isEmpty()) {
                return false
            }

            return true
        }

        private fun isEqual(a: Any?, b: Any?): Boolean {
            return Objects.equals(a, b)
        }

        override fun visitExpressionStmt(
            stmt: ExpressionStmt,
            env: Environment,
        ): List<XmlOutputToken> {
            // Null should stay null. `null` does not render unless a setting was added.
            val result = evaluate(stmt.expr, env)?.toString()
            return listOf(XmlOutputToken.Content(result, true))
        }

        override fun visitVarStmt(stmt: VarStmt, env: Environment): List<XmlOutputToken> {
            env.define(stmt.name.lexeme, evaluate(stmt.initializer, env))
            return listOf()
        }

        override fun visitBlockStmt(stmt: BlockStmt, env: Environment): List<XmlOutputToken> {
            val blockEnv = Environment(env)
            return stmt.stmts.flatMap { execute(it, blockEnv) }
        }

        override fun visitIfStmt(stmt: IfStmt, env: Environment): List<XmlOutputToken> {
            return if (isTruthy(evaluate(stmt.condition, env))) {
                execute(stmt.thenBranch, env)
            } else {
                listOf()
            }
        }

        override fun visitInsertStmt(stmt: InsertStmt, env: Environment): List<XmlOutputToken> {
            val rewriter = evaluate(stmt.expression, env) ?: return execute(stmt.body, env)
            if (rewriter !is XmlStreamRewriter) {
                throw TemplaterException.RuntimeError(
                    "Need instance of `XmlStreamRewriter` for `insert` statement.",
                    stmt.insertKeyword,
                )
            }

            val rawEval = execute(stmt.body, env)
            return rewriter.rewrite(rawEval)
        }

        override fun visitRewriteStmt(stmt: RewriteStmt, env: Environment): List<XmlOutputToken> {
            val rewriter = evaluate(stmt.expression, env) ?: return execute(stmt.body, env)
            if (rewriter !is XmlStreamRewriter) {
                throw TemplaterException.RuntimeError(
                    "Need instance of `XmlStreamRewriter` for `rewrite` statement.",
                    stmt.rewriteKeyword,
                )
            }

            val rawEval = execute(stmt.body, env)
            return rewriter.rewrite(rawEval)
        }

        override fun visitForStmt(stmt: ForStmt, env: Environment): List<XmlOutputToken> {
            val iterable = evaluate(stmt.iterable, env)
            if (iterable !is Iterable<*>) {
                throw TemplaterException.RuntimeError(
                    "'for' loop expression cannot be iterated over as it does not implement Iterable: ${iterable?.javaClass ?: "null"}",
                    stmt.loopVarName,
                )
            }

            if (parallelRendering) {
                val list = iterable as? List<*> ?: iterable.toList()
                return list
                    .parallelStream()
                    .flatMap { loopVal ->
                        val iterationEnv = Environment(env)
                        iterationEnv.define(stmt.loopVarName.lexeme, loopVal)
                        execute(stmt.body, iterationEnv).stream()
                    }
                    .collect(Collectors.toList())
            } else {
                val xmlTokens = mutableListOf<XmlOutputToken>()
                for (loopVal in iterable) {
                    val iterationEnv = Environment(env)
                    iterationEnv.define(stmt.loopVarName.lexeme, loopVal)
                    xmlTokens.addAll(execute(stmt.body, iterationEnv))
                }
                return xmlTokens
            }
        }

        override fun visitVerbatimStmt(stmt: VerbatimStmt, env: Environment): List<XmlOutputToken> {
            return when (val inputToken = stmt.inputToken) {
                is XmlInputToken.Raw -> listOf(XmlOutputToken.from(inputToken.xml))
                is XmlInputToken.Sentinel -> listOf(XmlOutputToken.Sentinel())
                is XmlInputToken.TemplateGroup -> throw TemplaterException.FatalError(
                    "VerbatimStmt cannot contain TemplateGroup.",
                    null,
                )
            }
        }

        override fun visitDoStmt(stmt: DoStmt, env: Environment): List<XmlOutputToken> {
            // Evaluate the expression for its side effects and ignore the output.
            evaluate(stmt.expr, env)
            return listOf()
        }
    }

    fun render(program: List<Stmt>, data: Map<String, Any>): List<XmlOutputToken> {
        return Interpreter.create(program, globals, data, parallelRendering).run()
    }

    fun createInterpreterForTesting(program: List<Stmt>): Interpreter {
        return Interpreter.create(program, globals, emptyMap(), parallelRendering)
    }
}
