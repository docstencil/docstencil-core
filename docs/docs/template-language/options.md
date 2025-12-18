---
sidebar_position: 10
---

# Template Options

Configuring template rendering behavior with `OfficeTemplateOptions`.

## Parallel Processing

Enable parallel execution for `{for}` loops and `$map` operations:

```java
OfficeTemplateOptions options = new OfficeTemplateOptions()
    .parallel(true);

OfficeTemplate template = OfficeTemplate.fromFile("Template.docx", options);
```

```kotlin
val options = OfficeTemplateOptions()
    .parallel(true)

val template = OfficeTemplate.fromFile("Template.docx", options)
```

### Performance Benefits

Parallel mode can significantly speed up rendering when:
- Processing large collections
- Each iteration involves expensive operations
- Running on multi-core systems

### Thread Safety Requirement

When using parallel mode, all objects in your data map must be thread-safe:

- **Immutable objects** — Safe by default (data classes, records, strings, numbers)
- **Read-only access** — Safe if you only read properties
- **Mutable shared state** — Must use synchronization or thread-safe collections

```kotlin
// Safe: immutable data class
data class Product(val name: String, val price: Double)

// Safe: read-only list
val products: List<Product> = listOf(...)

// Unsafe: mutable list being modified during iteration
val results = mutableListOf<String>()  // Don't share this in parallel mode
```

If your data objects have side effects or mutable state, keep parallel mode disabled (the default).
