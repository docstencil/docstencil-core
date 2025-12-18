---
sidebar_position: 1
---

# Installation

Add DocStencil to your project using Maven or Gradle.

## Requirements

- Java 8 or higher

## Maven

```xml
<dependency>
    <groupId>com.docstencil</groupId>
    <artifactId>docstencil-core</artifactId>
    <version>0.1.2</version>
</dependency>
```

## Gradle

```kotlin
implementation("com.docstencil:docstencil-core:0.1.2")
```

## Pro Modules

For extended features (images, footnotes, endnotes), add the pro module:

**Maven:**
```xml
<dependency>
    <groupId>com.docstencil</groupId>
    <artifactId>docstencil-docx-pro</artifactId>
    <version>0.1.2</version>
</dependency>
```

**Gradle:**
```kotlin
implementation("com.docstencil:docstencil-docx-pro:0.1.2")
```
