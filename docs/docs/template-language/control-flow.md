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

Use `@"tag"` to specify which XML element a `if` and `for` block should repeat:

```
|----------------------------------------------------------------------|
| {@"w:tr" for row in rows}{@"w:tc" for cell in row} {cell} {end}{end} |
|----------------------------------------------------------------------|
```

Here you can see a single table cell with two `for` loops. The first for loop wraps the table row, the second `for` loop wraps the table cell.


This targets the table cell (`w:tc`) element, useful when the default element detection isn't what you need.
