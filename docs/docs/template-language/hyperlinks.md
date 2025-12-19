---
sidebar_position: 9
---

# Hyperlinks

Inserting clickable links into documents.

## Basic Syntax

Use `{rewrite $link(url)}...{end}` to create a hyperlink:

```
{rewrite $link("https://example.com")}Click here{end}
```

## Dynamic URLs

Use variables for the URL:

```
{rewrite $link(product.url)}{product.name}{end}
```

## URL Types

Hyperlinks work with various URL schemes:

```
{rewrite $link("https://example.com")}Website{end}
{rewrite $link("mailto:contact@example.com")}Email us{end}
{rewrite $link("tel:+1234567890")}Call us{end}
```

## Link Text Formatting

The text between `{rewrite ...}` and `{end}` can include formatting and expressions:

```
{rewrite $link(user.profileUrl)}View {user.name}'s profile{end}
```
