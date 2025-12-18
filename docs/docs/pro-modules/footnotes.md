---
sidebar_position: 12
---

# Footnotes

:::info Preview Feature
This module is currently in preview. [Request access](https://docs.google.com/forms/d/e/1FAIpQLSfhK4WRdPvCurk1B3MI4t1vDgfZg4GIaet8Y7LfypOqSovW_w/viewform?usp=dialog) to try it out.
:::

Adding footnotes to documents using the docstencil-docx-pro module.

## Adding the Module

```java
OfficeTemplateOptions options = new OfficeTemplateOptions()
    .withModule(new FootnoteModule());
```

```kotlin
val options = OfficeTemplateOptions()
    .withModule(FootnoteModule())
```

## Basic Syntax

Use `$footnote` to insert a footnote reference with its content:

```
This claim requires citation{insert $footnote("Source: Annual Report 2024")}*{end}.
```

The placeholder (`*`) becomes a footnote reference number, and the text appears at the bottom of the page.

## Dynamic Content

Use variables for footnote text:

```
{insert $footnote(source.citation)}*{end}
```

## Footnotes in Loops

Generate footnotes from data:

```
{for fact in facts}
{fact.statement}{insert $footnote(fact.source)}*{end}
{end}
```

## Formatting Within Footnotes

Footnote text can include expressions:

```
{insert $footnote("See " + reference.title + ", page " + reference.page)}*{end}
```

## Multiple Footnotes

Each footnote is automatically numbered:

```
First point{insert $footnote("First source")}*{end} and
second point{insert $footnote("Second source")}*{end}.
```

Produces footnotes numbered 1, 2, etc. at the page bottom.
