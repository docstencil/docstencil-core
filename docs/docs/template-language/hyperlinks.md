---
sidebar_position: 9
---

# Hyperlinks

Inserting clickable links into documents.

## Basic Syntax

Use `{insert $link(url)}...{end}` to create a hyperlink:

```
{insert $link("https://example.com")}Click here{end}
```

## Dynamic URLs

Use variables for the URL:

```
{insert $link(product.url)}{product.name}{end}
```

## URL Types

Hyperlinks work with various URL schemes:

```
{insert $link("https://example.com")}Website{end}
{insert $link("mailto:contact@example.com")}Email us{end}
{insert $link("tel:+1234567890")}Call us{end}
```

## Link Text Formatting

The text between `{insert ...}` and `{end}` can include formatting and expressions:

```
{insert $link(user.profileUrl)}View {user.name}'s profile{end}
```

## Links in Loops

Generate multiple links from data:

```
{for link in navigation}
{insert $link(link.url)}{link.title}{end}
{end}
```
