---
sidebar_position: 2
---

# Control Flow

Conditional rendering and loops in templates.

## Conditionals

Use `{if}...{end}` to conditionally render content:

```
{if user.premium}
Premium member benefits apply.
{end}
```

## For Loops

Use `{for ... in ...}...{end}` to iterate over collections:

```
{for item in items}
- {item.name}: {item.price}
{end}
```

## Case Expressions

Use `case` for multi-branch conditionals:

```
{case
  when status == "pending" then "Awaiting review"
  when status == "approved" then "Ready to ship"
  when status == "shipped" then "On the way"
  else "Unknown"
end}
```

Single-line form:

```
{case when score >= 90 then "A" when score >= 80 then "B" else "C" end}
```

## Element Targeting with @

Use `@"tag"` to specify which XML element an `if` or `for` block should wrap:

```
|----------------------------------------------------------------------|
| {@"w:tr" for row in rows}{@"w:tc" for cell in row} {cell} {end}{end} |
|----------------------------------------------------------------------|
```

This example shows a single table cell containing two `for` loops:
- `@"w:tr"` makes the outer loop repeat the entire table row
- `@"w:tc"` makes the inner loop repeat each table cell within the row

This is useful when you need to control exactly which XML element gets repeated, rather than relying on automatic detection.
