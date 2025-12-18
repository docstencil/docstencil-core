---
sidebar_position: 8
---

# Comments

Adding comments to templates.

## Comment Syntax

Use `{/* ... */}` to add comments that won't appear in the output:

```
{/* This is a comment */}
Hello, {name}!
```

## Multi-line Comments

Comments can span multiple lines:

```
{/*
  This template generates an invoice.
  Last updated: 2024-03-15
*/}
```

## Behavior

Comments are completely stripped from the output. They render as empty, producing no content in the final document.

```
{/* Hidden note */}Visible text
```

Outputs: `Visible text`
