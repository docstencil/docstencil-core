package com.docstencil.core.modules

import com.docstencil.core.api.OfficeTemplateOptions
import com.docstencil.core.api.model.OfficeTemplateData
import com.docstencil.core.builtin.BuiltinFormatter
import com.docstencil.core.builtin.EnumerationWrapper
import com.docstencil.core.builtin.TypeCaster
import com.docstencil.core.config.FileTypeConfig
import com.docstencil.core.error.TemplaterException
import com.docstencil.core.managers.ContentTypesManager
import com.docstencil.core.managers.RelationshipManager
import com.docstencil.core.render.Globals
import com.docstencil.core.render.NativeObjectHelper
import com.docstencil.core.rewrite.ConstRawXmlRewriter
import com.docstencil.core.rewrite.IdentityXmlRewriter
import com.docstencil.core.rewrite.LineBreaksAsTagsXmlRewriter
import com.docstencil.core.rewrite.RawXmlRewriter
import java.time.LocalDateTime
import java.util.*
import java.util.stream.Collectors

/**
 * Core module that registers essential archive file managers required for all Office documents.
 */
class CoreModule : TemplateModule {
    private val nativeObjectHelper = NativeObjectHelper()

    override fun registerGlobals(
        builder: Globals.Builder,
        options: OfficeTemplateOptions,
        config: FileTypeConfig,
    ) {
        val formatter = BuiltinFormatter(options.locale)
        val typeCaster = TypeCaster()

        builder
            .registerGlobal($$"$asByte", { n: Any? -> typeCaster.asByte(n) })
            .registerGlobal($$"$asShort", { n: Any? -> typeCaster.asShort(n) })
            .registerGlobal($$"$asInt", { n: Any? -> typeCaster.asInt(n) })
            .registerGlobal($$"$asLong", { n: Any? -> typeCaster.asLong(n) })
            .registerGlobal($$"$asFloat", { n: Any? -> typeCaster.asFloat(n) })
            .registerGlobal($$"$asDouble", { n: Any? -> typeCaster.asDouble(n) })
            .registerGlobal(
                $$"$enumerate", { iter: Iterable<Any?> -> EnumerationWrapper(iter) },
            )
            .registerGlobal($$"$filter", createFilterFunction(options.parallelRendering))
            .registerGlobal(
                $$"$filterNotNull", { iter: Iterable<Any?> -> iter.filterNotNull() },
            )
            .registerGlobal(
                $$"$format",
                VariableArityFunction(1, 2) { args ->
                    val value = args[0]
                    val pattern = args.getOrNull(1) as? String
                    formatter.format(value, pattern)
                },
            )
            .registerGlobal(
                $$"$formatDate",
                { date: Any?, pattern: String -> formatter.formatDate(date, pattern) },
            )
            .registerGlobal(
                $$"$formatNumber",
                { num: Number?, pattern: String -> formatter.formatNumber(num, pattern) },
            )
            .registerGlobal(
                $$"$join",
                { iter: Iterable<Any?>, sep: String ->
                    iter.joinToString(sep) { Objects.toString(it) }
                },
            )
            .registerGlobal($$"$map", createMapFunction(options.parallelRendering))
            .registerGlobal($$"$notNull", { x: Any? -> x != null })
            .registerGlobal($$"$now", { LocalDateTime.now() })
            .registerGlobal($$"$range", createRangeFunction())
            .registerGlobal($$"$reduce", createReduceFunction())
            .registerGlobal($$"$sum", createSumFunction())
            .registerGlobal($$"$lowercase") { s: String? -> s?.lowercase() }
            .registerGlobal($$"$uppercase") { s: String? -> s?.uppercase() }
            .registerGlobal(
                $$"$reverse",
                { c: Iterable<Any?> -> if (c is List) c.asReversed() else c.reversed() },
            )
            .registerRewriter($$"$identityRewriter", IdentityXmlRewriter.Factory())
            .registerRewriter($$"$toc", ConstRawXmlRewriter.createDefaultTocFactory())
            .registerRewriter(
                $$"$lineBreaksAsTags",
                LineBreaksAsTagsXmlRewriter.Factory(config),
            )
            .registerRewriter($$"$rawXml", RawXmlRewriter.Factory(config))
    }

    override fun registerManagers(files: OfficeTemplateData) {
        val relationshipManager = RelationshipManager.create(
            files.getFiles(),
            files.getDetectionResult(),
        )
        files.registerManager(RelationshipManager::class, relationshipManager)

        val contentTypesManager = ContentTypesManager.create(files.getFiles())
        files.registerManager(ContentTypesManager::class, contentTypesManager)
    }

    internal fun createFilterFunction(parallel: Boolean): (Any?, Any?) -> Any? {
        return { items: Any?, predicate: Any? ->
            if (items == null) emptyList()
            else if (items !is Iterable<*>) {
                throw TemplaterException.DeepRuntimeError(
                    $$"$filter: first argument must be iterable",
                    null,
                )
            } else if (predicate == null) {
                throw TemplaterException.DeepRuntimeError(
                    $$"$filter: predicate must not be null",
                    null,
                )
            } else {
                val list = items.toList()
                val filterPredicate = { item: Any? ->
                    when (val result = nativeObjectHelper.call(predicate, listOf(item))) {
                        null -> false
                        is Boolean -> result
                        else -> true
                    }
                }
                if (parallel && list.size > 1) {
                    list.parallelStream()
                        .filter { filterPredicate(it) }
                        .collect(Collectors.toList())
                } else {
                    list.filter(filterPredicate)
                }
            }
        }
    }

    internal fun createMapFunction(parallel: Boolean): (Any?, Any?) -> Any? {
        return { items: Any?, mapper: Any? ->
            if (items == null) emptyList()
            else if (items !is Iterable<*>) {
                throw TemplaterException.DeepRuntimeError(
                    $$"$map: first argument must be iterable",
                    null,
                )
            } else if (mapper == null) {
                throw TemplaterException.DeepRuntimeError(
                    $$"$map: mapper must not be null",
                    null,
                )
            } else {
                val list = items.toList()
                if (parallel && list.size > 1) {
                    list.parallelStream()
                        .map { item -> nativeObjectHelper.call(mapper, listOf(item)) }
                        .collect(Collectors.toList())
                } else {
                    list.map { item -> nativeObjectHelper.call(mapper, listOf(item)) }
                }
            }
        }
    }

    internal fun createReduceFunction(): VariableArityFunction {
        return VariableArityFunction(2, 3) { args ->
            val items = args.getOrNull(0)
            val reducer = args.getOrNull(1)
            val initial = args.getOrNull(2)

            if (items == null) null
            else if (items !is Iterable<*>) {
                throw TemplaterException.DeepRuntimeError(
                    $$"$reduce: first argument must be iterable",
                    null,
                )
            } else if (reducer == null) {
                throw TemplaterException.DeepRuntimeError(
                    $$"$reduce: reducer must not be null",
                    null,
                )
            } else {
                val list = items.toList()
                if (list.isEmpty()) {
                    initial
                } else {
                    var accumulator = initial ?: list[0]
                    val startIndex = if (initial == null) 1 else 0

                    for (i in startIndex until list.size) {
                        accumulator =
                            nativeObjectHelper.call(reducer, listOf(accumulator, list[i]))
                    }

                    accumulator
                }
            }
        }
    }

    internal fun createSumFunction(): (Iterable<*>?) -> Number {
        return { items ->
            items?.sumOf { item ->
                when (item) {
                    is Number -> item.toDouble()
                    else -> throw TemplaterException.DeepRuntimeError(
                        $$"$sum: can only sum numbers, got: " + (item?.javaClass ?: null),
                    )
                }
            }
                ?: 0
        }
    }

    internal fun createRangeFunction(): VariableArityFunction {
        return VariableArityFunction(1, 2) { args ->
            val firstArg = args[0]
            val secondArg = args.getOrNull(1)

            if (firstArg !is Int) {
                throw TemplaterException.DeepRuntimeError(
                    $$"$range: argument must be an integer",
                    null,
                )
            }

            val (fromIncl, toExcl) = if (secondArg == null) {
                0 to firstArg
            } else {
                if (secondArg !is Int) {
                    throw TemplaterException.DeepRuntimeError(
                        $$"$range: second argument must be an integer",
                        null,
                    )
                }
                firstArg to secondArg
            }

            IntRange(fromIncl, toExcl - 1).toList()
        }
    }
}

/**
 * Wrapper for variable-arity functions that can accept different numbers of arguments.
 * Used for functions like `$reduce` that have optional parameters.
 */
internal class VariableArityFunction(
    val minArity: Int,
    val maxArity: Int,
    val impl: (List<Any?>) -> Any?,
) : Function<Any?> {
    fun invoke(vararg args: Any?): Any? = impl(args.toList())
}
