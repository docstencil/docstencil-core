<p align="center">
  <h1 align="center">DocStencil</h1>
  <p align="center">
    <strong>Word Document Template Engine for Java & Kotlin</strong><br>
    <em>Generate DOCX files with mail merge, loops, and conditionals</em>
  </p>
</p>

<p align="center">
  <a href="https://central.sonatype.com/artifact/com.docstencil/docstencil-core"><img src="https://img.shields.io/maven-central/v/com.docstencil/docstencil-core" alt="Maven Central"></a>
  <a href="docstencil-core/LICENSE"><img src="https://img.shields.io/badge/License-Apache%202.0%20%2F%20MIT-blue.svg" alt="License"></a>
</p>

<p align="center">
  <a href="https://docstencil.com/docs">Documentation</a> •
  <a href="https://docstencil.com/docs/real-world-example">Examples</a> •
  <a href="https://github.com/docstencil/docstencil-core/issues">Report Bug</a>
</p>

---

## What is DocStencil?

DocStencil is an open-source document generation library for Java and Kotlin. It works like mail merge for Word documents — design your templates in Microsoft Word using familiar `{placeholder}` syntax, then render them with your data to produce professional DOCX files.

**Use cases:**
- **Invoices & Receipts** — Generate billing documents with line items, totals, and customer details
- **Contracts & Agreements** — Create legal documents with dynamic clauses and signatures
- **Reports** — Build data-driven reports with tables, charts, and formatted numbers

Templates can be edited by anyone familiar with Word. For most use cases no programming knowledge is required. Your team can update document layouts, styles, and content without touching code.

## Why DocStencil?

| | DocStencil                    | docx4j | Apache POI |
|---|-------------------------------|---|---|
| **Template-based** | Yes! Use Word as your editor  | Requires XML knowledge | No template support |
| **API complexity** | 3 lines of code               | Complex, verbose API | Complex, verbose API |
| **Kotlin-native** | Yes, with Java interop        | Java only | Java only |
| **Dependencies** | Minimal (only kotlin-reflect) | Heavy (100+ MB) | Moderate |

DocStencil focuses on simplicity: you design templates in Word, not in code. Compare the "Hello World" example above to [docx4j](https://www.docx4java.org/trac/docx4j) or [Apache POI](https://poi.apache.org/components/document/) — DocStencil requires no XML manipulation and no complex object models.

## Key Features

- **Placeholder replacement**: Use simple placeholders: `{name}`
- **Loops & Conditionals**: Generate dynamic tables and show/hide sections with `{for}` and `{if}`
- **Nested Data**: Access complex data structures with dot notation and function calling: `{customer.address.city}`, `{customerService.get(invoice.getCustomerId()).getName()}`
- **Formatting**: Format dates and numbers with built-in functions: `{$format(date, "MMMM dd, yyyy")}`
- **Preserves Styles**: Your Word styles, fonts, and layouts stay intact
- **Parallel Evaluation**: Template expressions can be evaluated in parallel; ideal for large templates that fetch data lazily

## Quickstart

### Requirements

- **JDK 8** or higher

### Installation

<details>
<summary><b>Maven</b></summary>

```xml
<dependency>
    <groupId>com.docstencil</groupId>
    <artifactId>docstencil-core</artifactId>
    <version>0.1.4</version>
</dependency>
```

</details>

<details>
<summary><b>Gradle (Kotlin)</b></summary>

```kotlin
implementation("com.docstencil:docstencil-core:0.1.4")
```

</details>

<details>
<summary><b>Gradle (Groovy)</b></summary>

```groovy
implementation 'com.docstencil:docstencil-core:0.1.4'
```

</details>

### Hello World

**1. Create a Word template:**

![Template showing Hello {name}! with a surrounding if clause](docs/static/img/hello_in.png)

```
{if name == "world"}
Hello {name}!
{end}
```

**2. Render with your data:**

<details open>
<summary><b>Kotlin</b></summary>

```kotlin
import com.docstencil.core.api.OfficeTemplate

fun main() {
    val template = OfficeTemplate.fromFile("template.docx")
    val result = template.render(mapOf("name" to "world"))
    result.writeToFile("Output.docx")
}
```

</details>

<details>
<summary><b>Java</b></summary>

```java
import com.docstencil.core.api.OfficeTemplate;
import java.util.Map;

public class Main {
    public static void main(String[] args) {
        var template = OfficeTemplate.fromFile("template.docx");
        var result = template.render(Map.of("name", "world"));
        result.writeToFile("Output.docx");
    }
}
```

</details>

**3. Result:**

![Output showing Hello World!](docs/static/img/hello_out.png)

```
Hello World!
```

## Template Syntax

### Variables

Access nested properties in maps, POJOs with getters, records, and data classes with dot notation:

```
Ship to: {customer.address.street}, {customer.address.city}
```

### Loops

Repeat content for each item in a list or table row:

```
{for item in items}
- {item.name}: ${item.price}
{end}
```

| Product | Quantity | Price |
|---------|----------|-------|
| {for line in orderLines}{line.product} | {line.qty} | ${line.price}{end} |

### Conditionals

Show content based on conditions:

```
{if invoice.subtotal >= 100 and !user.registered}
Use code COUPON10 to get 10% off your next order!
{end}
```

### Other features

DocStencil has a rich and expressive templating language that supports:

- Formatting of dates and numbers
- Pipe notation with lambdas
- Inserting raw XML
- Inserting hyperlinks
- and much more ...

## FAQ

<details>
<summary><b>Can I generate PDFs?</b></summary>

DocStencil generates DOCX files. To convert to PDF, you can use LibreOffice in headless mode, or a library like [documents4j](https://github.com/documents4j/documents4j).
</details>

<details>
<summary><b>Does it work with Spring Boot?</b></summary>

Yes. DocStencil is a plain Java/Kotlin library with no framework dependencies. Add it to your project and use `OfficeTemplate` from any Spring component.
</details>

<details>
<summary><b>Can non-developers edit templates?</b></summary>

Yes, that's a core design goal. Templates are regular Word documents. Anyone who knows Word can edit the layout, styles, and text. Developers only need to ensure the `{placeholder}` names match the data model.
</details>

<details>
<summary><b>What's the difference between DocStencil and docx4j?</b></summary>

docx4j is a low-level library for manipulating OOXML documents. It's powerful but requires understanding Word's XML structure. DocStencil is a template engine: You write templates in Word, not code. See the [comparison table](#why-docstencil) above.
</details>

## Documentation

For comprehensive guides and API reference, visit the **[Documentation](https://docstencil.com/docs)**.

- [Quickstart Guide](https://docstencil.com/docs)
- [Template Basics](https://docstencil.com/docs/basics)
- [Built-in Functions](https://docstencil.com/docs/template-language/builtin-functions)
- [Pro Features](https://docstencil.com/docs/pro-modules/images)

## License

The library is dual-licensed under the [Apache License 2.0](https://www.apache.org/licenses/LICENSE-2.0) and [MIT License](https://opensource.org/licenses/MIT). See [docstencil-core/LICENSE](docstencil-core/LICENSE) for details.
