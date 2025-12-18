package com.docstencil.core.render

import com.docstencil.core.error.TemplaterException
import com.docstencil.core.modules.VariableArityFunction
import com.docstencil.core.scanner.model.TemplateToken
import java.util.concurrent.ConcurrentHashMap
import java.util.function.*
import kotlin.reflect.*
import kotlin.reflect.full.allSuperclasses
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.reflect.jvm.isAccessible


class NativeObjectHelper {
    private val propertyCache = ConcurrentHashMap<Pair<KClass<*>, String>, KProperty1<out Any, *>>()
    private val callableCache = ConcurrentHashMap<Int, CallableMetadata>()

    fun isCallable(obj: Any?): Boolean {
        if (obj == null) return false

        @Suppress("RemoveRedundantQualifierName")
        return when (obj) {
            is VariableArityFunction -> true
            is Function<*> -> true

            // Java functional interfaces.
            is Supplier<*> -> true
            is java.util.function.Function<*, *> -> true
            is Consumer<*> -> true
            is Predicate<*> -> true
            is BiFunction<*, *, *> -> true
            is BiConsumer<*, *> -> true
            is BiPredicate<*, *> -> true

            else -> {
                // Generic fallback: check if it implements a Function interface
                obj::class.allSuperclasses.any {
                    it.qualifiedName?.startsWith("kotlin.jvm.functions.Function") == true
                }
            }
        }
    }

    /**
     * Check if the given number of arguments is valid for the callable object.
     * For most callables, this checks exact arity. For VariableArityFunction, it checks the range.
     */
    fun isValidArity(obj: Any, argsSize: Int): Boolean {
        return when (obj) {
            is VariableArityFunction -> argsSize in obj.minArity..obj.maxArity
            else -> argsSize == getArity(obj)
        }
    }

    fun getArity(obj: Any): Int {
        val cached = callableCache[System.identityHashCode(obj)]
        if (cached != null) return cached.arity

        @Suppress("RemoveRedundantQualifierName")
        return when (obj) {
            is VariableArityFunction -> obj.minArity  // Return minimum arity for error messages
            // Kotlin Function0-Function3.
            is Function0<*> -> 0
            is Function1<*, *> -> 1
            is Function2<*, *, *> -> 2
            is Function3<*, *, *, *> -> 3

            // Kotlin KFunction.
            is KFunction<*> -> obj.parameters.count { it.kind == KParameter.Kind.VALUE }

            // Java functional interfaces.
            is Supplier<*> -> 0
            is java.util.function.Function<*, *> -> 1
            is Consumer<*> -> 1
            is Predicate<*> -> 1
            is BiFunction<*, *, *> -> 2
            is BiConsumer<*, *> -> 2
            is BiPredicate<*, *> -> 2

            else -> {
                // Generic fallback: use reflection to find invoke() method.
                val invokeMethod = obj::class.memberFunctions
                    .find { it.name == "invoke" }
                    ?: throw IllegalArgumentException("Object is not callable: ${obj::class.simpleName}")

                invokeMethod.parameters.count { it.kind == KParameter.Kind.VALUE }
            }
        }
    }

    fun call(obj: Any, args: List<Any?>): Any? {
        if (obj is VariableArityFunction) {
            return obj.invoke(*args.toTypedArray())
        }
        if (args.any { it is TemplateFunction }) {
            throw TemplaterException.FatalError("TemplateFunction passed to NativeObjectHelper.")
        }

        val metadata = callableCache.getOrPut(System.identityHashCode(obj)) {
            getCallableMetadata(obj)
                ?: throw IllegalArgumentException("Object is not callable: ${obj::class.simpleName}")
        }

        try {
            return metadata.invoker(args)
        } catch (e: Exception) {
            // Unwrap reflection exceptions for cleaner error messages.
            val cause = e.cause ?: e
            throw TemplaterException.DeepRuntimeError(
                "Error calling function: ${cause.message}",
                cause,
            )
        }
    }

    fun hasProperty(obj: Any, propertyName: String): Boolean {
        val kClass = obj::class
        val cacheKey = kClass to propertyName

        // Check cache first.
        if (propertyCache.containsKey(cacheKey)) return true

        // Search for property.
        val property = kClass.memberProperties.find { it.name == propertyName }
        if (property != null) {
            propertyCache[cacheKey] = property
            return true
        }

        return false
    }

    /**
     * Get a property value from an object using reflection.
     *
     * Supports:
     * - Kotlin properties (val/var)
     * - Java bean getters (getName, getPropertyName)
     */
    fun getProperty(obj: Any, name: TemplateToken): Any? {
        val propertyName = name.lexeme
        val kClass = obj::class
        val cacheKey = kClass to propertyName

        // Try cache first.
        val cachedProperty = propertyCache[cacheKey]
        if (cachedProperty != null) {
            return invokeGetter(cachedProperty, obj, name)
        }

        // Find property via reflection.
        val property = kClass.memberProperties.find { it.name == propertyName }
        if (property != null) {
            propertyCache[cacheKey] = property
            return invokeGetter(property, obj, name)
        }

        // Fallback: try Java bean getter (getName, getPropertyName).
        val getterName = "get${propertyName.replaceFirstChar { it.uppercase() }}"
        val getter = kClass.memberFunctions.find {
            it.name == getterName && it.parameters.size == 1 // Only 'this' parameter
        }

        if (getter != null) {
            try {
                getter.isAccessible = true
                return getter.call(obj)
            } catch (e: Exception) {
                throw TemplaterException.RuntimeError(
                    "Error accessing property '$propertyName': ${e.message}",
                    name,
                )
            }
        }

        throw TemplaterException.RuntimeError(
            "Property '$propertyName' not found on ${kClass.simpleName}.",
            name,
        )
    }

    /**
     * Set a property value on an object using reflection.
     *
     * Supports:
     * - Kotlin mutable properties (var)
     * - Java bean setters (setName, setPropertyName)
     */
    fun setProperty(obj: Any, name: TemplateToken, value: Any?) {
        val propertyName = name.lexeme
        val kClass = obj::class
        val cacheKey = kClass to propertyName

        // Try cache first.
        val cachedProperty = propertyCache[cacheKey]
        if (cachedProperty != null) {
            if (cachedProperty !is KMutableProperty1) {
                throw TemplaterException.RuntimeError(
                    "Property '$propertyName' is read-only.",
                    name,
                )
            }
            invokeSetter(cachedProperty, obj, value, name)
            return
        }

        // Find property via reflection.
        val property = kClass.memberProperties.find { it.name == propertyName }

        if (property != null) {
            if (property !is KMutableProperty1) {
                throw TemplaterException.RuntimeError(
                    "Property '$propertyName' is read-only.",
                    name,
                )
            }
            propertyCache[cacheKey] = property
            invokeSetter(property, obj, value, name)
            return
        }

        // Fallback: try Java bean setter (setName, setPropertyName).
        val setterName = "set${propertyName.replaceFirstChar { it.uppercase() }}"
        val setter = kClass.memberFunctions.find {
            it.name == setterName && it.parameters.size == 2 // 'this' + value parameter
        }

        if (setter != null) {
            try {
                setter.isAccessible = true
                // Convert value if needed
                val param = setter.parameters[1] // First is 'this', second is the value
                val convertedValue = convertNumericType(value, param.type)
                setter.call(obj, convertedValue)
                return
            } catch (e: Exception) {
                throw TemplaterException.RuntimeError(
                    "Error setting property '$propertyName': ${e.message}",
                    name,
                )
            }
        }

        throw TemplaterException.RuntimeError(
            "Property '$propertyName' not found on ${kClass.simpleName}.",
            name,
        )
    }

    private fun getCallableMetadata(obj: Any): CallableMetadata? {
        @Suppress("RemoveRedundantQualifierName")
        return when (obj) {
            is Function0<*> -> CallableMetadata(0) { obj.invoke() }

            is KFunction<*> -> {
                val arity = obj.parameters.count { it.kind == KParameter.Kind.VALUE }
                val params = obj.parameters.filter { it.kind == KParameter.Kind.VALUE }

                CallableMetadata(arity) { args ->
                    obj.isAccessible = true
                    val convertedArgs = args.mapIndexed { index, arg ->
                        if (index < params.size) {
                            convertNumericType(arg, params[index].type)
                        } else arg
                    }
                    obj.call(*convertedArgs.toTypedArray())
                }
            }

            is Supplier<*> -> CallableMetadata(0) {
                @Suppress("UNCHECKED_CAST")
                (obj as Supplier<Any?>).get()
            }

            is java.util.function.Function<*, *> -> CallableMetadata(1) { args ->
                @Suppress("UNCHECKED_CAST")
                (obj as java.util.function.Function<Any?, Any?>).apply(args[0])
            }

            is Consumer<*> -> CallableMetadata(1) { args ->
                @Suppress("UNCHECKED_CAST")
                (obj as Consumer<Any?>).accept(args[0])
                null // Consumer returns void
            }

            is Predicate<*> -> CallableMetadata(1) { args ->
                @Suppress("UNCHECKED_CAST")
                (obj as Predicate<Any?>).test(args[0])
            }

            is BiFunction<*, *, *> -> CallableMetadata(2) { args ->
                @Suppress("UNCHECKED_CAST")
                (obj as BiFunction<Any?, Any?, Any?>).apply(args[0], args[1])
            }

            is BiConsumer<*, *> -> CallableMetadata(2) { args ->
                @Suppress("UNCHECKED_CAST")
                (obj as BiConsumer<Any?, Any?>).accept(args[0], args[1])
                null // BiConsumer returns void
            }

            is BiPredicate<*, *> -> CallableMetadata(2) { args ->
                @Suppress("UNCHECKED_CAST")
                (obj as BiPredicate<Any?, Any?>).test(args[0], args[1])
            }

            else -> {
                // Generic fallback: Try Kotlin reflection first, then Java reflection.
                val kotlinInvoke = obj::class.memberFunctions.find { it.name == "invoke" }

                if (kotlinInvoke != null) {
                    // Use Kotlin reflection (works for KFunction and some other types).
                    val arity = kotlinInvoke.parameters.count { it.kind == KParameter.Kind.VALUE }
                    val params = kotlinInvoke.parameters.filter { it.kind == KParameter.Kind.VALUE }

                    CallableMetadata(arity) { args ->
                        kotlinInvoke.isAccessible = true
                        val convertedArgs = args.mapIndexed { index, arg ->
                            if (index < params.size) {
                                convertNumericType(arg, params[index].type)
                            } else {
                                arg
                            }
                        }
                        kotlinInvoke.call(obj, *convertedArgs.toTypedArray())
                    }
                } else {
                    // Try Java reflection (works for lambdas where Kotlin reflection fails).
                    // Look for ALL invoke methods and prefer non-Object/non-bridge ones.
                    val invokeMethods = obj.javaClass.methods.filter { it.name == "invoke" }
                    if (invokeMethods.isEmpty()) return null

                    // Prefer non-bridge methods with specific types over erased Object types.
                    val javaInvoke = invokeMethods.firstOrNull {
                        !it.isBridge && it.parameterTypes.any { p -> p != Object::class.java }
                    } ?: invokeMethods.first()

                    val arity = javaInvoke.parameterCount

                    CallableMetadata(arity) { args ->
                        javaInvoke.isAccessible = true

                        // Try invoking with original args first.
                        try {
                            return@CallableMetadata javaInvoke.invoke(obj, *args.toTypedArray())
                        } catch (_: IllegalArgumentException) {
                            // Type mismatch, try converting numeric arguments.
                        } catch (e: java.lang.reflect.InvocationTargetException) {
                            if (e.cause is IllegalArgumentException) {
                                // Type mismatch, try converting.
                            } else {
                                throw e
                            }
                        }

                        // Use type-specific conversion.
                        val paramTypes = javaInvoke.parameterTypes
                        val convertedArgs = args.mapIndexed { index, arg ->
                            if (arg is Number && index < paramTypes.size) {
                                convertJavaNumericType(arg, paramTypes[index])
                            } else {
                                arg
                            }
                        }
                        javaInvoke.invoke(obj, *convertedArgs.toTypedArray())
                    }
                }
            }
        }
    }

    private fun invokeGetter(
        property: KProperty1<out Any, *>,
        obj: Any,
        name: TemplateToken,
    ): Any? {
        try {
            // Make accessible to handle private properties if needed
            property.isAccessible = true

            @Suppress("UNCHECKED_CAST")
            return (property as KProperty1<Any, *>).get(obj)
        } catch (e: Exception) {
            throw TemplaterException.RuntimeError(
                "Error getting property ${property.name}: ${e.message}",
                name,
            )
        }
    }

    private fun invokeSetter(
        property: KMutableProperty1<out Any, *>,
        obj: Any,
        value: Any?,
        name: TemplateToken,
    ) {
        try {
            property.isAccessible = true

            // Convert value to target type if needed
            val convertedValue = convertNumericType(value, property.returnType)

            @Suppress("UNCHECKED_CAST")
            (property as KMutableProperty1<Any, Any?>).set(obj, convertedValue)
        } catch (_: IllegalArgumentException) {
            // Type mismatch
            throw TemplaterException.RuntimeError(
                "Type mismatch setting property ${property.name}. " +
                        "Expected ${property.returnType}, got ${value?.let { it::class.simpleName }}.",
                name,
            )
        } catch (e: Exception) {
            throw TemplaterException.RuntimeError(
                "Error setting property ${property.name}: ${e.message}",
                name,
            )
        }
    }

    private fun convertNumericType(value: Any?, targetType: KType): Any? {
        if (value == null) return null

        // Get the classifier (the actual type).
        val classifier = targetType.classifier as? KClass<*> ?: return value

        // If value is already the target type, return as-is.
        if (classifier.isInstance(value)) return value

        // Handle numeric conversions.
        return when (classifier) {
            Int::class -> when (value) {
                is Number -> value.toInt()
                else -> value
            }

            Long::class -> when (value) {
                is Number -> value.toLong()
                else -> value
            }

            Float::class -> when (value) {
                is Number -> value.toFloat()
                else -> value
            }

            Double::class -> when (value) {
                is Number -> value.toDouble()
                else -> value
            }

            Byte::class -> when (value) {
                is Number -> value.toByte()
                else -> value
            }

            Short::class -> when (value) {
                is Number -> value.toShort()
                else -> value
            }

            // All other types: no conversion (strict typing).
            else -> value
        }
    }

    @Suppress("PLATFORM_CLASS_MAPPED_TO_KOTLIN")
    private fun convertJavaNumericType(value: Number, targetType: Class<*>): Any {
        return when (targetType) {
            Integer.TYPE, Integer::class.java -> value.toInt()
            java.lang.Long.TYPE, java.lang.Long::class.java -> value.toLong()
            java.lang.Float.TYPE, java.lang.Float::class.java -> value.toFloat()
            java.lang.Double.TYPE, java.lang.Double::class.java -> value.toDouble()
            java.lang.Byte.TYPE, java.lang.Byte::class.java -> value.toByte()
            java.lang.Short.TYPE, java.lang.Short::class.java -> value.toShort()
            else -> value
        }
    }
}

private data class CallableMetadata(
    val arity: Int,
    val invoker: (List<Any?>) -> Any?,
)
