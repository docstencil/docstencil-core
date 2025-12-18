---
sidebar_position: 13
---

# Endnotes

:::info Preview Feature
This module is currently in preview. [Request access](https://docs.google.com/forms/d/e/1FAIpQLSfhK4WRdPvCurk1B3MI4t1vDgfZg4GIaet8Y7LfypOqSovW_w/viewform?usp=dialog) to try it out.
:::

Adding endnotes to documents using the docstencil-docx-pro module.

## Adding the Module

```java
OfficeTemplateOptions options = new OfficeTemplateOptions()
    .withModule(new EndnoteModule());
```

```kotlin
val options = OfficeTemplateOptions()
    .withModule(EndnoteModule())
```

## Basic Syntax

Use `$endnote` to insert an endnote reference:

```
This requires a reference{insert $endnote("See appendix for details")}*{end}.
```

The placeholder becomes an endnote reference number, and the text appears at the end of the document.

## Dynamic Content

Use variables for endnote text:

```
{insert $endnote(reference.fullCitation)}*{end}
```

## Endnotes vs Footnotes

| Feature | Footnotes | Endnotes |
|---------|-----------|----------|
| Location | Bottom of each page | End of document |
| Best for | Brief citations, asides | Detailed references, bibliography |
| Module | `FootnoteModule` | `EndnoteModule` |

## Endnotes in Loops

Generate endnotes from a collection:

```
{for citation in citations}
{citation.text}{insert $endnote(citation.source)}*{end}
{end}
```

## Combined Usage

You can use both footnotes and endnotes in the same document by adding both modules:

```java
OfficeTemplateOptions options = new OfficeTemplateOptions()
    .withModule(new FootnoteModule())
    .withModule(new EndnoteModule());
```
